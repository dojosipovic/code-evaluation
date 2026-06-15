import { TestResultEnum } from "../enum/TestResultEnum";
import { TestVisibilityEnum } from "../enum/TestVisibilityEnum";

export interface IAssignmentRunTestResult {
    index: number;
    input: string;
    expectedOutput: string;
    testVisibility: TestVisibilityEnum;
    exitCode: number;
    durationMs: number;
    stdout: string;
    stderr: string;
    timedOut: boolean;
    timeout: string;
    testResult: TestResultEnum;
}
