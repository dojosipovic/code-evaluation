export interface IRunResult {
    exitCode: number;
    durationMs: number;
    stdout: string;
    stderr: string;
    timedOut: boolean;
    timeout: string;
    phase: string;
}
