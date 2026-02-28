package com.codeevaluation.core.service;

import com.codeevaluation.core.service.dto.RunResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.InternalServerErrorException;
import org.jboss.logging.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@ApplicationScoped
public class CppDockerSandboxService {
    private static final Logger LOG = Logger.getLogger(CppDockerSandboxService.class);

    // DVA image-a
    private static final String IMAGE_COMPILE = "cpp-compile:latest";
    private static final String IMAGE_RUN     = "cpp-run:latest";

    private static final String CPUS = "1.0";
    private static final String MEMORY = "256m";

    // compile treba više procesa (g++, as, ld...)
    private static final long COMPILE_PIDS_LIMIT = 64;
    // run može biti stroži
    private static final long RUN_PIDS_LIMIT = 32;

    public RunResult compileAndRun(String cppSource, int timeoutSec) {
        Duration runTimeout = Duration.ofSeconds(Math.max(1, Math.min(timeoutSec, 30)));
        Duration compileTimeout = Duration.ofSeconds(Math.min(20, runTimeout.getSeconds()));

        Path dir = null;
        try {
            // VAŽNO: na Windowsu je bolje da ovo bude negdje gdje Docker Desktop ima file sharing
            // Ako ti java.io.tmpdir nije sharean, prebaci na user.home
            dir = Files.createTempDirectory("cpp-job-");
            Path execDir = dir.resolve("exec");
            Files.createDirectories(execDir);

            Path main = dir.resolve("main.cpp");
            Files.writeString(main, cppSource, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // 1) COMPILE
            RunResult compileRes = runDocker(buildCompileCmd(dir, execDir), compileTimeout);
            compileRes.phase = "compile";
            if (compileRes.exitCode != 0) {
                return compileRes; // compile error
            }

            // 2) RUN
            RunResult runRes = runDocker(buildRunCmd(execDir), runTimeout);
            runRes.phase = "run";
            return runRes;

        } catch (IOException e) {
            LOG.error("Docker execution failed", e);
            throw new InternalServerErrorException("Docker execution failed: " + e.getMessage());
        } finally {
            if (dir != null) {
                try { deleteRecursive(dir); } catch (Exception ignored) {}
            }
        }
    }

    private List<String> buildCompileCmd(Path dir, Path execDir) {
        // compile: /bin/sh -c "g++ ..."
        String inner = "g++ -O2 -std=c++20 /work/main.cpp -o /runexec/a.out";

        return new ArrayList<>(List.of(
                "docker", "run", "--rm",
                "--name", "cpp-compile-" + UUID.randomUUID(),

                "--network", "none",

                "--cpus", CPUS,
                "--memory", MEMORY,
                "--pids-limit", String.valueOf(COMPILE_PIDS_LIMIT),

                "--read-only",
                "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges",

                "--user", "10001:10001",

                // tmpfs za /tmp (g++ povremeno koristi temp)
                "--tmpfs", "/tmp:rw,nosuid,nodev,size=128m",

                "--ulimit", "core=0",
                "--ulimit", "fsize=1048576",
                "--ulimit", "nofile=128:128",

                "-v", dir.toAbsolutePath() + ":/work:ro",
                "-v", execDir.toAbsolutePath() + ":/runexec:rw",

                IMAGE_COMPILE,

                // pokreni komandu kroz sh (compile image nema entrypoint)
                "/bin/sh", "-c", inner
        ));
    }

    private List<String> buildRunCmd(Path execDir) {
        // run: distroless image već ima ENTRYPOINT /runexec/a.out
        return new ArrayList<>(List.of(
                "docker", "run", "--rm",
                "--name", "cpp-run-" + UUID.randomUUID(),

                "--network", "none",

                "--cpus", CPUS,
                "--memory", MEMORY,
                "--pids-limit", String.valueOf(RUN_PIDS_LIMIT),

                "--read-only",
                "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges",

                "--user", "10001:10001",

                "--tmpfs", "/tmp:rw,nosuid,nodev,size=64m",

                "--ulimit", "core=0",
                "--ulimit", "fsize=1048576",
                "--ulimit", "nofile=128:128",

                // mount binarku read-only
                "-v", execDir.resolve("a.out").toAbsolutePath() + ":/runexec/a.out:ro",

                IMAGE_RUN
                // nema shell-a, nema komandi; ENTRYPOINT pokreće a.out
        ));
    }

    private RunResult runDocker(List<String> cmd, Duration timeout) {
        long start = System.nanoTime();
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);

        try {
            Process p = pb.start();

            ExecutorService es = Executors.newFixedThreadPool(2);
            Future<String> outF = es.submit(() -> readLimited(p.getInputStream(), 200_000));
            Future<String> errF = es.submit(() -> readLimited(p.getErrorStream(), 200_000));

            boolean finished = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                p.destroyForcibly();
                es.shutdownNow();
                return RunResult.timeout(timeout);
            }

            int exit = p.exitValue();
            String stdout = getFuture(outF);
            String stderr = getFuture(errF);
            es.shutdown();

            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            if (!stdout.isBlank()) LOG.infof("docker stdout:\n%s", stdout);
            if (!stderr.isBlank()) LOG.warnf("docker stderr:\n%s", stderr);

            return new RunResult(exit, ms, stdout, stderr);

        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to start docker: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException("Interrupted");
        }
    }

    private static String readLimited(InputStream is, int maxChars) throws IOException {
        try (Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int total = 0;
            int n;
            while ((n = r.read(buf)) != -1) {
                int take = Math.min(n, maxChars - total);
                if (take > 0) sb.append(buf, 0, take);
                total += take;
                if (total >= maxChars) {
                    sb.append("\n[output truncated]\n");
                    break;
                }
            }
            return sb.toString();
        }
    }

    private static String getFuture(Future<String> f) {
        try { return f.get(2, TimeUnit.SECONDS); }
        catch (Exception e) { return ""; }
    }

    private static void deleteRecursive(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }
    }
}