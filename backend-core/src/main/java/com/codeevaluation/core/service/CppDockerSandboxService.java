package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.run.RunBatchResponseDto;
import com.codeevaluation.core.api.dto.run.TestRunResult;
import com.codeevaluation.core.service.dto.RunResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jboss.logging.Logger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

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
            if (volCreate.getExitCode() != 0) {
                volCreate.setPhase("volume-create");
                return volCreate;
            }

            // 1) COMPILE (stdin -> /tmp/main.cpp, output -> /runexec/a.out in volume)
            RunResult compileRes = runDockerRaw(
                    buildCompileCmd(volume),
                    compileTimeout,
                    cppSource
            );
            compileRes.setPhase("compile");
            if (compileRes.getExitCode() != 0) {
                return compileRes;
            }

            // 2) RUN (distroless runs /runexec/a.out; volume mounted read-only)
            RunResult runRes = runDockerRaw(
                    buildRunCmd(volume),
                    runTimeout,
                    input
            );
            runRes.setPhase("run");
            return runRes;

        } finally {
            // Best-effort cleanup
            try {
                runDockerRaw(
                        List.of("docker", "volume", "rm", "-f", volume), Duration.ofSeconds(5),
                        null
                );
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    public RunBatchResponseDto compileAndRunBatchParallel(
            String cppSource,
            List<String> inputs,
            int timeoutSecPerTest,
            int maxParallel // npr. 4 ili 8
    ) {
        Duration runTimeout = Duration.ofSeconds(Math.max(1, Math.min(timeoutSecPerTest, 30)));
        Duration compileTimeout = Duration.ofSeconds(Math.min(20, runTimeout.getSeconds()));

        String volume = "cpp-job-" + UUID.randomUUID();

        RunBatchResponseDto resp = new RunBatchResponseDto();
        resp.setPhase("batch");
        resp.setResults(new ArrayList<>());

        // fail-fast
        if (cppSource == null || cppSource.isBlank()) {
            RunResult c = new RunResult(1, 0, "", "Empty source code");
            c.setPhase("compile");
            resp.setCompile(c);
            return resp;
        }
        if (inputs == null) {
            inputs = List.of("");
        }

        // limit paralelizma
        int threads = Math.max(1, Math.min(maxParallel, inputs.size()));
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        try {
            // 0) volume create
            RunResult volCreate = runDockerRaw(
                    List.of("docker", "volume", "create", volume),
                    Duration.ofSeconds(5),
                    null
            );
            if (volCreate.getExitCode() != 0) {
                RunResult fail = new RunResult(
                        volCreate.getExitCode(),
                        volCreate.getDurationMs(),
                        volCreate.getStdout(),
                        volCreate.getStderr()
                );
                fail.setPhase("volume-create");
                resp.setCompile(fail);
                return resp;
            }

            // 1) compile jednom
            RunResult compileRes = runDockerRaw(buildCompileCmd(volume), compileTimeout, cppSource);
            compileRes.setPhase("compile");
            resp.setCompile(compileRes);

            if (compileRes.getExitCode() != 0 || compileRes.isTimedOut()) {
                return resp; // nema runova
            }

            // 2) paralelni runovi
            List<Future<TestRunResult>> futures = new ArrayList<>(inputs.size());

            for (int i = 0; i < inputs.size(); i++) {
                final int idx = i;
                final String in = inputs.get(i) == null ? "" : inputs.get(i);

                futures.add(pool.submit(() -> {
                    RunResult r = runDockerRaw(buildRunCmd(volume), runTimeout, in);

                    TestRunResult tr = new TestRunResult();
                    tr.setIndex(idx);
                    tr.setExitCode(r.getExitCode());
                    tr.setDurationMs(r.getDurationMs());
                    tr.setStdout(r.getStdout());
                    tr.setStderr(r.getStderr());
                    tr.setTimedOut(r.isTimedOut());
                    tr.setTimeout(r.getTimeout());

                    if (r.getExitCode() >= 128) {
                        int signal = r.getExitCode() - 128;
                        tr.setStderr("Runtime error (signal " + signal + ")");
                    }
                    return tr;
                }));
            }

            // pokupi rezultate (redoslijed očuvamo po indexu)
            TestRunResult[] ordered = new TestRunResult[inputs.size()];

            for (Future<TestRunResult> f : futures) {
                try {
                    TestRunResult tr = f.get(runTimeout.toMillis() + 2000, TimeUnit.MILLISECONDS);
                    ordered[tr.getIndex()] = tr;
                } catch (TimeoutException te) {
                    // ako neka Future zapne duže od očekivanog, označi timeout
                    TestRunResult tr = new TestRunResult();
                    tr.setIndex(findFirstEmpty(ordered));
                    tr.setExitCode(-1);
                    tr.setDurationMs(runTimeout.toMillis());
                    tr.setStdout("");
                    tr.setStderr("");
                    tr.setTimedOut(true);
                    tr.setTimeout(runTimeout.toString());
                    ordered[tr.getIndex()] = tr;
                } catch (Exception e) {
                    // bilo koja greška u workeru
                    TestRunResult tr = new TestRunResult();
                    tr.setIndex(findFirstEmpty(ordered));
                    tr.setExitCode(1);
                    tr.setDurationMs(0);
                    tr.setStdout("");
                    tr.setStderr("Internal error: " + e.getMessage());
                    tr.setTimedOut(false);
                    tr.setTimeout(null);
                    ordered[tr.getIndex()] = tr;
                }
            }

            resp.setResults(Arrays.asList(ordered));
            return resp;

        } finally {
            pool.shutdownNow();
            try {
                runDockerRaw(
                        List.of("docker", "volume", "rm", "-f", volume), Duration.ofSeconds(5),
                        null
                );
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    private static int findFirstEmpty(TestRunResult[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == null) {
                return i;
            }
        }
        return 0;
    }

    public RunBatchResponseDto compileAndRunBatch(
            String cppSource, List<String> inputs, int timeoutSecPerTest) {
        Duration runTimeout = Duration.ofSeconds(Math.max(1, Math.min(timeoutSecPerTest, 30)));
        Duration compileTimeout = Duration.ofSeconds(Math.min(20, runTimeout.getSeconds()));

        String volume = "cpp-job-" + UUID.randomUUID();

        RunBatchResponseDto resp = new RunBatchResponseDto();
        resp.setPhase("batch");
        resp.setResults(new ArrayList<>());

        try {
            RunResult volCreate = runDockerRaw(
                    List.of("docker", "volume", "create", volume),
                    Duration.ofSeconds(5),
                    null
            );
            if (volCreate.getExitCode() != 0) {
                RunResult fail = new RunResult(
                        volCreate.getExitCode(),
                        volCreate.getDurationMs(),
                        volCreate.getStdout(),
                        volCreate.getStderr()
                );
                fail.setPhase("volume-create");
                resp.setCompile(fail);
                return resp;
            }

            // compile once
            RunResult compileRes = runDockerRaw(buildCompileCmd(volume), compileTimeout, cppSource);
            compileRes.setPhase("compile");
            resp.setCompile(compileRes);

            if (compileRes.getExitCode() != 0 || compileRes.isTimedOut()) {
                return resp; // no test runs
            }

            // run per test input (each in a fresh container)
            for (int i = 0; i < inputs.size(); i++) {
                String in = inputs.get(i);
                RunResult r = runDockerRaw(buildRunCmd(volume), runTimeout, in);
                TestRunResult tr = new TestRunResult();
                tr.setIndex(i);
                tr.setExitCode(r.getExitCode());
                tr.setDurationMs(r.getDurationMs());
                tr.setStdout(r.getStdout());
                tr.setStderr(r.getStderr());
                tr.setTimedOut(r.isTimedOut());
                tr.setTimeout(r.getTimeout());
                resp.getResults().add(tr);
            }

            return resp;

        } finally {
            try {
                runDockerRaw(
                        List.of("docker", "volume", "rm", "-f", volume),
                        Duration.ofSeconds(5),
                        null);
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    private List<String> buildCompileCmd(String volume) {
        // NOTE: We run compile as root to avoid any perms issues on the volume,
        // but still keep hardening flags.
        String inner = "cat > /tmp/main.cpp && "
                + "echo '=== first 80 lines ===' && sed -n '1,80p' /tmp/main.cpp && "
                + "echo '=== end ===' && "
                + "g++ -O2 -std=c++20 /tmp/main.cpp -o /runexec/a.out && "
                + "chmod 755 /runexec/a.out";

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
                if (stdin != null) {
                    os.write(stdin.getBytes(StandardCharsets.UTF_8));
                }
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

            if (!stdout.isBlank()) {
                LOG.infof("docker stdout:\n%s", stdout);
            }
            if (!stderr.isBlank()) {
                LOG.warnf("docker stderr:\n%s", stderr);
            }

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
                if (take > 0) {
                    sb.append(buf, 0, take);
                }
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
        try {
            return f.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }
}
