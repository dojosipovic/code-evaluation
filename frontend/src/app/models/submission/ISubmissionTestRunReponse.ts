import { SubmissionTestRunStatusEnum } from "../enum/SubmissionTestRunStatusEnum";
import { ISubmissionTestResultResponse } from "./ISubmissionTestResultResponse";

export interface ISubmissionTestRunResponse {
    id: number;
    status: SubmissionTestRunStatusEnum;
    totalTests: number;
    passedTests: number;
    runtimeMs: number | null;
    memoryBytes: number | null;
    logOutput: string | null;
    createdAt: string;
    testResults: ISubmissionTestResultResponse[];
}
