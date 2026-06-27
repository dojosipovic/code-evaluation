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
import org.jboss.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class JplagService {

    private static final Logger LOG = Logger.getLogger(JplagService.class);
    private static final Duration ASYNC_CLEANUP_DELAY = Duration.ofSeconds(2);
    private final Decoder b64 = Base64.getDecoder();
    private final Path workRoot = resolveWorkRoot();

    public PlagResult analyze(ScanRequest request) {
        String runId = UUID.randomUUID().toString();
        Path rootDir = null;

        try {
            LOG.infov("Starting plagiarism analysis runId={0}, submissions={1}, minSimilarity={2}, includeClusters={3}",
                    runId, request.getSubmissions().size(), request.getMinSimilarity(), request.isIncludeClusters());
            Files.createDirectories(workRoot);
            rootDir = Files.createTempDirectory(workRoot, "jplag-root-" + runId + "-");

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
            byte[] bytes = Files.readAllBytes(reportFile);
            String base64 = Base64.getEncoder().encodeToString(bytes);

            List<ClusterResult> clusters = null;
            if (request.isIncludeClusters()) {
                clusters = extractClusters(result);
            }

            List<PairResult> pairs = extractPairs(result, request.getMinSimilarity());

            LOG.infov("Finished plagiarism analysis runId={0}, pairs={1}, clusters={2}",
                    runId, pairs.size(), clusters == null ? 0 : clusters.size());
            return new PlagResult(runId, request.getMinSimilarity(), pairs, clusters, base64);
        } catch (IOException | ExitException e) {
            LOG.errorv(e, "Plagiarism analysis failed runId={0}", runId);
            throw new InternalServerErrorException("JPlag analiza nije uspjela", e);
        } catch (RuntimeException e) {
            LOG.errorv(e, "Unexpected plagiarism analysis failure runId={0}", runId);
            throw e;
        } finally {
            if (!safeDeleteRecursive(rootDir)) {
                scheduleAsyncCleanup(rootDir);
            }
            safeDeleteIfEmpty(workRoot);
        }
    }

    private List<PairResult> extractPairs(JPlagResult result, double minSimilarity) {
        Collection<JPlagComparison> comps = result.getAllComparisons();

        List<PairResult> out = new ArrayList<>();
        for (JPlagComparison c : comps) {
            String a = c.firstSubmission().getName();  // ili getDisplayName()
            String b = c.secondSubmission().getName();

            double simFromFirst = normalizeSimilarity(c.similarityOfFirst());
            if (simFromFirst >= minSimilarity) {
                out.add(new PairResult(a, b, simFromFirst));
            }

            double simFromSecond = normalizeSimilarity(c.similarityOfSecond());
            if (simFromSecond >= minSimilarity) {
                out.add(new PairResult(b, a, simFromSecond));
            }
        }

        out.sort(Comparator.comparingDouble(PairResult::getSimilarity).reversed());
        return out;
    }

    private double normalizeSimilarity(double similarity) {
        if (similarity > 1.0) {
            return similarity / 100.0;
        }
        return similarity;
    }

    private Path resolveWorkRoot() {
        Path preferred = Path.of(System.getProperty("user.dir"), ".jplag-work")
                .toAbsolutePath()
                .normalize();
        if (isWritableDirectory(preferred.getParent())) {
            return preferred;
        }

        Path fallback = Path.of(System.getProperty("java.io.tmpdir"), "jplag-work")
                .toAbsolutePath()
                .normalize();
        LOG.infov("Using fallback JPlag work directory {0} instead of {1}", fallback, preferred);
        return fallback;
    }

    private boolean isWritableDirectory(Path dir) {
        if (dir == null) {
            return false;
        }
        try {
            Files.createDirectories(dir);
            return Files.isWritable(dir);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean safeDeleteRecursive(Path dir) {
        if (dir == null) {
            return true;
        }

        for (int attempt = 1; attempt <= 5; attempt++) {
            if (!Files.exists(dir)) {
                return true;
            }

            try (var paths = Files.walk(dir)) {
                paths
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                                // retry on next attempt
                            }
                        });
            } catch (IOException ignored) {
                // retry below
            }

            if (!Files.exists(dir)) {
                return true;
            }

            try {
                Thread.sleep(100L * attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (Files.exists(dir)) {
            LOG.warnv("Failed to fully delete JPlag work directory {0}", dir);
            return false;
        }
        return true;
    }

    private void scheduleAsyncCleanup(Path dir) {
        if (dir == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(ASYNC_CLEANUP_DELAY.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (safeDeleteRecursive(dir)) {
                safeDeleteIfEmpty(workRoot);
                LOG.infov("Asynchronous cleanup removed JPlag work directory {0}", dir);
            } else {
                LOG.warnv("Asynchronous cleanup also failed for JPlag work directory {0}", dir);
            }
        });
    }

    private void safeDeleteIfEmpty(Path dir) {
        if (dir == null) {
            return;
        }

        try {
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {
            // ignore if not empty or temporarily unavailable
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
                if (c.getMembers().size() < 3) {
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
