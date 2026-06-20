import { TestResultEnum } from "../enum/TestResultEnum";
import { TestVisibilityEnum } from "../enum/TestVisibilityEnum";

export interface IAssignmentRunTestResult {
    index: number;
    input: string;
    showExpectedOutput: boolean;
    expectedOutput: string | null;
    visibility: TestVisibilityEnum;
    exitCode: number;
    durationMs: number;
    stdout: string;
    stderr: string;
    timedOut: boolean;
    timeout: string | null;
    testResult: TestResultEnum;
}
