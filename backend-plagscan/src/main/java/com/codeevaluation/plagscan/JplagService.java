package com.codeevaluation.plagscan;

import com.codeevaluation.plagscan.dto.ScanRequest;
import com.codeevaluation.plagscan.dto.FilePayload;
import com.codeevaluation.plagscan.model.ClusterResult;
import com.codeevaluation.plagscan.model.PairResult;
import com.codeevaluation.plagscan.model.PlagResult;
import de.jplag.JPlag;
import de.jplag.JPlagComparison;
import de.jplag.JPlagResult;
import de.jplag.Submission;
import de.jplag.clustering.Cluster;
import de.jplag.clustering.ClusteringResult;
import de.jplag.cpp.CPPLanguage;
import de.jplag.exceptions.ExitException;
import de.jplag.options.JPlagOptions;
import de.jplag.reporting.reportobject.ReportObjectFactory;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Comparator;

@ApplicationScoped
public class JplagService {

    private final Decoder b64 = Base64.getDecoder();

    public PlagResult analyze(ScanRequest request) {
        String runId = UUID.randomUUID().toString();
        Path rootDir = null;

        try {
            rootDir = Files.createTempDirectory("jplag-root-" + runId + "-");

            for (FilePayload s : request.getSubmissions()) {
                String id = s.getId();

                Path submissionDir = rootDir.resolve(id);
                Files.createDirectories(submissionDir);

                Path target = submissionDir.resolve("main.cpp");

                byte[] content = b64.decode(s.getContentBase64());

                Files.write(target, content, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }

            Path baseCodeDir = null;
            if (request.getBaseCode() != null
                    && request.getBaseCode().files() != null
                    && !request.getBaseCode().files().isEmpty()) {
                baseCodeDir = rootDir.resolve("BaseCode");
                Files.createDirectories(baseCodeDir);

                int i = 0;
                for (FilePayload f : request.getBaseCode().files()) {
                    Path target = baseCodeDir.resolve("base" + (i++) + ".cpp");
                    byte[] content = b64.decode(f.getContentBase64());
                    Files.write(target, content, StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING);
                }
            }

            var language = new CPPLanguage();
            var roots = Set.of(rootDir.toFile());

            JPlagOptions options = new JPlagOptions(language, roots, Set.of())
                    .withSimilarityThreshold(request.getMinSimilarity())
                    .withMinimumTokenMatch(8);

            if (baseCodeDir != null) {
                options = options.withBaseCodeSubmissionDirectory(baseCodeDir.toFile());
            }

            JPlagResult result = JPlag.run(options);

            Path reportFile = rootDir.resolve("report.plag");
            ReportObjectFactory reportFactory = new ReportObjectFactory(reportFile.toFile());
            reportFactory.createAndSaveReport(result);
            String fixed =
                    """
                    {
                        "language": "cpp",
                        "baseCodeSubmissionDirectory": "BaseCode",
                        "submissionDirectories": [
                          "."
                        ],
                      "clusteringOptions": {
                            "similarityMetric": "AVG",
                            "spectralKernelBandwidth": 20.0,
                            "spectralGaussianProcessVariance": 0.0025000000000000005,
                            "spectralMinRuns": 5,
                            "spectralMaxRuns": 50,
                            "spectralMaxKMeansIterationPerRun": 200,
                            "agglomerativeThreshold": 0.2,
                            "preprocessor": "CUMULATIVE_DISTRIBUTION_FUNCTION",
                            "enabled": true,
                            "algorithm": "SPECTRAL",
                            "agglomerativeInterClusterSimilarity": "AVERAGE",
                            "preprocessorThreshold": 0.2,
                            "preprocessorPercentile": 0.5
                        }
                    }
                    """;

            URI uri = URI.create("jar:" + reportFile.toUri());

            try (FileSystem zipFs = FileSystems.newFileSystem(uri, Map.of("create", "true"))) {
                Path optionsJson = zipFs.getPath("/options.json");
                Files.writeString(optionsJson, fixed, StandardOpenOption.CREATE);
            }
            byte[] bytes = Files.readAllBytes(reportFile);
            String base64 = Base64.getEncoder().encodeToString(bytes);

            List<ClusterResult> clusters = null;
            if (request.isIncludeClusters()) {
                clusters = extractClusters(result);
            }

            List<PairResult> pairs = extractPairs(result, request.getMinSimilarity());

            return new PlagResult(runId, request.getMinSimilarity(), pairs, clusters, base64);
        } catch (IOException | ExitException e) {
            throw new InternalServerErrorException("JPlag analiza nije uspjela", e);
        } finally {
            safeDeleteRecursive(rootDir);
        }
    }

    private List<PairResult> extractPairs(JPlagResult result, double minSimilarity) {
        Collection<JPlagComparison> comps = result.getAllComparisons();

        List<PairResult> out = new ArrayList<>();
        for (JPlagComparison c : comps) {

            double sim = c.similarity(); // ili getSimilarity()
            if (sim > 1.0) {
                sim = sim / 100.0;
            }
            if (sim < minSimilarity) {
                continue;
            }

            String a = c.firstSubmission().getName();  // ili getDisplayName()
            String b = c.secondSubmission().getName();

            PairResult p = new PairResult(a, b, sim);
            out.add(p);
        }

        out.sort(Comparator.comparingDouble(PairResult::getSimilarity).reversed());
        return out;
    }

    private void safeDeleteRecursive(Path dir) {
        if (dir == null) {
            return;
        }

        if (!Files.exists(dir)) {
            return;
        }

        try (var paths = Files.walk(dir)) {
            paths
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // ignore
                        }
                    });
        } catch (IOException e) {
            // opcionalno: log
        }
    }

    private String stripCppExt(String name) {
        if (name == null) {
            return null;
        }
        if (name.endsWith(".cpp")) {
            return name.substring(0, name.length() - 4);
        }
        if (name.endsWith(".cc")) {
            return name.substring(0, name.length() - 3);
        }
        if (name.endsWith(".cxx")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    private List<ClusterResult> extractClusters(JPlagResult result) {
        List<ClusteringResult<Submission>> clusteringResults = result.getClusteringResult();
        if (clusteringResults == null) {
            return List.of();
        }

        int clusterId = 0;
        List<ClusterResult> out = new ArrayList<>();
        for (ClusteringResult<Submission> cr : clusteringResults) {
            List<Cluster<Submission>> clusters = (List<Cluster<Submission>>) cr.getClusters();
            if (clusters == null) {
                continue;
            }

            for (Cluster<de.jplag.Submission> c : clusters) {
                if (c.getMembers().size() < 2) {
                    continue;
                }

                List<String> members = c.getMembers().stream()
                        .map(m -> stripCppExt(m.getName()))
                        .toList();

                ClusterResult dto = new ClusterResult(
                        ++clusterId,
                        c.getAverageSimilarity(),
                        members
                );

                out.add(dto);
            }
        }
        return out;
    }
}
