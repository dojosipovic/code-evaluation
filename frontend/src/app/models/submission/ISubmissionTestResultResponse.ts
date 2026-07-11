import { TestResultEnum } from "../enum/TestResultEnum";
import { TestVisibilityEnum } from "../enum/TestVisibilityEnum";

export interface ISubmissionTestResultResponse {
    id: number;
    taskTestId: number;
    result: TestResultEnum;
    visibility: TestVisibilityEnum;
    testInput: string | null;
    expectedOutput: string | null;
    showExpectedOutput: boolean;
    actualOutput: string | null;
    runtimeMs: number | null;
    errorOutput: string | null;
}
