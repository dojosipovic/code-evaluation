package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.RunBatchResponseDto;
import com.codeevaluation.core.api.dto.TestRunResult;
import com.codeevaluation.core.service.dto.RunResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.InternalServerErrorException;
import org.jboss.logging.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@ApplicationScoped
public class CppDockerSandboxService {
    private static final Logger LOG = Logger.getLogger(CppDockerSandboxService.class);

    private static final String IMAGE_COMPILE = "cpp-compile:latest";
    private static final String IMAGE_RUN     = "cpp-run:latest";

    private static final String CPUS = "1.0";
    private static final String MEMORY = "256m";

    private static final long COMPILE_PIDS_LIMIT = 64;
    private static final long RUN_PIDS_LIMIT = 32;

    private static final int MAX_OUT_CHARS = 200_000;

    public RunResult compileAndRun(String cppSource, String input, int timeoutSec) {
        Duration runTimeout = Duration.ofSeconds(Math.max(1, Math.min(timeoutSec, 30)));
        Duration compileTimeout = Duration.ofSeconds(Math.min(20, runTimeout.getSeconds()));

        String volume = "cpp-job-" + UUID.randomUUID();

        try {
            // 0) create per-job volume
            RunResult volCreate = runDockerRaw(
                    List.of("docker", "volume", "create", volume),
                    Duration.ofSeconds(5),
                    null
            );
            if (volCreate.exitCode != 0) {
                volCreate.phase = "volume-create";
                return volCreate;
            }

            // 1) COMPILE (stdin -> /tmp/main.cpp, output -> /runexec/a.out in volume)
            RunResult compileRes = runDockerRaw(
                    buildCompileCmd(volume),
                    compileTimeout,
                    cppSource
            );
            compileRes.phase = "compile";
            if (compileRes.exitCode != 0) return compileRes;

            // 2) RUN (distroless runs /runexec/a.out; volume mounted read-only)
            RunResult runRes = runDockerRaw(
                    buildRunCmd(volume),
                    runTimeout,
                    input
            );
            runRes.phase = "run";
            return runRes;

        } finally {
            // Best-effort cleanup
            try {
                runDockerRaw(List.of("docker", "volume", "rm", "-f", volume), Duration.ofSeconds(5), null);
            } catch (Exception ignored) {}
        }
    }

    public RunBatchResponseDto compileAndRunBatch(String cppSource, List<String> inputs, int timeoutSecPerTest) {
        Duration runTimeout = Duration.ofSeconds(Math.max(1, Math.min(timeoutSecPerTest, 30)));
        Duration compileTimeout = Duration.ofSeconds(Math.min(20, runTimeout.getSeconds()));

        String volume = "cpp-job-" + UUID.randomUUID();

        RunBatchResponseDto resp = new RunBatchResponseDto();
        resp.phase = "batch";
        resp.results = new ArrayList<>();

        try {
            RunResult volCreate = runDockerRaw(
                    List.of("docker", "volume", "create", volume),
                    Duration.ofSeconds(5),
                    null
            );
            if (volCreate.exitCode != 0) {
                RunResult fail = new RunResult(volCreate.exitCode, volCreate.durationMs, volCreate.stdout, volCreate.stderr);
                fail.phase = "volume-create";
                resp.compile = fail;
                return resp;
            }

            // compile once
            RunResult compileRes = runDockerRaw(buildCompileCmd(volume), compileTimeout, cppSource);
            compileRes.phase = "compile";
            resp.compile = compileRes;

            if (compileRes.exitCode != 0 || compileRes.timedOut) {
                return resp; // no test runs
            }

            // run per test input (each in a fresh container)
            for (int i = 0; i < inputs.size(); i++) {
                String in = inputs.get(i);
                RunResult r = runDockerRaw(buildRunCmd(volume), runTimeout, in); // send stdin for this test
                TestRunResult tr = new TestRunResult();
                tr.index = i;
                tr.exitCode = r.exitCode;
                tr.durationMs = r.durationMs;
                tr.stdout = r.stdout;
                tr.stderr = r.stderr;
                tr.timedOut = r.timedOut;
                tr.timeout = r.timeout;
                resp.results.add(tr);
            }

            return resp;

        } finally {
            try {
                runDockerRaw(List.of("docker", "volume", "rm", "-f", volume), Duration.ofSeconds(5), null);
            } catch (Exception ignored) {}
        }
    }

    private List<String> buildCompileCmd(String volume) {
        // NOTE: We run compile as root to avoid any perms issues on the volume,
        // but still keep hardening flags.
        String inner =
                "cat > /tmp/main.cpp && " +
                        "echo '=== first 80 lines ===' && sed -n '1,80p' /tmp/main.cpp && " +
                        "echo '=== end ===' && " +
                        "g++ -O2 -std=c++20 /tmp/main.cpp -o /runexec/a.out && " +
                        "chmod 755 /runexec/a.out";

        return new ArrayList<>(List.of(
                "docker", "run", "--rm", "-i",
                "--name", "cpp-compile-" + UUID.randomUUID(),

                "--network", "none",

                "--cpus", CPUS,
                "--memory", MEMORY,
                "--pids-limit", String.valueOf(COMPILE_PIDS_LIMIT),

                "--read-only",
                "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges",

                "--user", "0:0",

                "--tmpfs", "/tmp:rw,nosuid,nodev,size=128m",

                "--ulimit", "core=0",
                "--ulimit", "fsize=1048576",
                "--ulimit", "nofile=128:128",

                // per-job volume for artifacts
                "-v", volume + ":/runexec:rw",

                IMAGE_COMPILE,
                "/bin/sh", "-c", inner
        ));
    }

    private List<String> buildRunCmd(String volume) {
        return new ArrayList<>(List.of(
                "docker", "run", "--rm", "-i",
                "--name", "cpp-run-" + UUID.randomUUID(),

                "--network", "none",

                "--cpus", CPUS,
                "--memory", MEMORY,
                "--pids-limit", String.valueOf(RUN_PIDS_LIMIT),

                "--read-only",
                "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges",

                // distroless is nonroot already; keep explicit
                "--user", "10001:10001",

                "--tmpfs", "/tmp:rw,nosuid,nodev,size=64m",

                "--ulimit", "core=0",
                "--ulimit", "fsize=1048576",
                "--ulimit", "nofile=128:128",

                // same volume, read-only
                "-v", volume + ":/runexec:ro",

                IMAGE_RUN
        ));
    }

    private RunResult runDockerRaw(List<String> cmd, Duration timeout, String stdin) {
        long start = System.nanoTime();
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);

        try {
            Process p = pb.start();

            // Provide stdin (for compile we send C++ source), then close to signal EOF.
            try (OutputStream os = p.getOutputStream()) {
                if (stdin != null) os.write(stdin.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                LOG.debugf("stdin write/close failed (ignored): %s", e.getMessage());
            }

            ExecutorService es = Executors.newFixedThreadPool(2);
            Future<String> outF = es.submit(() -> readLimited(p.getInputStream(), MAX_OUT_CHARS));
            Future<String> errF = es.submit(() -> readLimited(p.getErrorStream(), MAX_OUT_CHARS));

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
}
