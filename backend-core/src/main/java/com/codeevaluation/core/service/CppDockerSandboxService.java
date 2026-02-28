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
    private static final String IMAGE = "cpp-sandbox:latest";

    // “default” sandbox limiti – prilagodi po potrebi
    private static final String CPUS = "1.0";
    private static final String MEMORY = "256m";
    private static final long PIDS_LIMIT = 4;
    private static final String SECCOMP_PROFILE = """
        {
          "defaultAction": "SCMP_ACT_ALLOW",
          "syscalls": [
            {
              "names": ["execve", "execveat"],
              "action": "SCMP_ACT_ERRNO"
            }
          ]
        }
        """;

    public RunResult compileAndRun(String cppSource, int timeoutSec) {
        Duration timeout = Duration.ofSeconds(Math.max(1, Math.min(timeoutSec, 30)));

        Path dir = null;
        try {
            dir = Files.createTempDirectory("cpp-job-");
            Path execDir = dir.resolve("exec");
            Files.createDirectories(execDir);
            Path seccomp = ensureSeccompProfile();

            Path main = dir.resolve("main.cpp");
            Files.writeString(main, cppSource, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Komanda unutar containera:
            // - kompajliraj u /tmp (jer je rootfs read-only)
            // - pokreni binarku
            // - koristi -O2 po želji; možeš i -O0 radi brzine
            String inner = String.join(" && ",
                    "g++ -O2 -std=c++20 /work/main.cpp -o /runexec/a.out",
                    "/runexec/a.out"
            );

            String containerName = "cpp-" + UUID.randomUUID();

            List<String> cmd = new ArrayList<>();
            cmd.addAll(List.of(
                    "docker", "run", "--rm",
                    "--name", containerName,

                    // 1) NO NETWORK
                    "--network", "none",

                    // 2) Resource limits
                    "--cpus", CPUS,
                    "--memory", MEMORY,
                    "--pids-limit", String.valueOf(PIDS_LIMIT),

                    // 3) Hardening
                    "--read-only",
                    "--cap-drop", "ALL",
                    "--security-opt", "no-new-privileges",

                    // (opcionalno) seccomp profil – Docker default je već ok; custom profil je dodatni korak
                    // "--security-opt", "seccomp=/work/seccomp.json",

                    // 4) Non-root user (iako image već ima USER, ovo dodatno forsira)
                    "--user", "10001:10001",

                    // 5) Dozvoli minimalno pisanje samo u /tmp (tmpfs)
                    "--tmpfs", "/tmp:rw,nosuid,nodev,size=64m",

                    // 6) Ulimit primjeri (spriječi npr. core dump, limit file size)
                    "--ulimit", "core=0",
                    "--ulimit", "fsize=1048576",

                    // Mount source code read-only
                    "-v", dir.toAbsolutePath() + ":/work:ro",

                    "-v", execDir.toAbsolutePath() + ":/runexec:rw",
                    //"--security-opt", "seccomp=" + seccomp.toAbsolutePath(),

                    IMAGE,

                    // entrypoint je bash -lc, pa proslijedi komandu
                    inner
            ));

            long start = System.nanoTime();
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);

            Process p = pb.start();

            ExecutorService es = Executors.newFixedThreadPool(2);
            Future<String> outF = es.submit(() -> readLimited(p.getInputStream(), 200_000));
            Future<String> errF = es.submit(() -> readLimited(p.getErrorStream(), 200_000));

            boolean finished = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                LOG.warnf("Timeout (%s). Killing docker run + container %s", timeout, containerName);
                p.destroyForcibly();
                safeKillContainer(containerName);
                es.shutdownNow();
                return RunResult.timeout(timeout);
            }

            int exit = p.exitValue();
            String stdout = getFuture(outF);
            String stderr = getFuture(errF);
            es.shutdown();

            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            if (!stdout.isBlank()) LOG.infof("C++ stdout:\n%s", stdout);
            if (!stderr.isBlank()) LOG.warnf("C++ stderr:\n%s", stderr);

            return new RunResult(exit, ms, stdout, stderr);

        } catch (IOException e) {
            LOG.error("Docker execution failed", e);
            throw new InternalServerErrorException("Docker execution failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException("Interrupted");
        } finally {
            if (dir != null) {
                try { deleteRecursive(dir); } catch (Exception ignored) {}
            }
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

    private void safeKillContainer(String name) {
        try {
            new ProcessBuilder("docker", "rm", "-f", name).start().waitFor(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
    }

    private static void deleteRecursive(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }
    }

    private Path ensureSeccompProfile() throws IOException {
        Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "cpp-sandbox");
        Files.createDirectories(dir);

        Path profile = dir.resolve("seccomp-noexec.json");

        if (!Files.exists(profile)) {
            Files.writeString(profile, SECCOMP_PROFILE);
        }

        return profile;
    }
}
