import { IAssignmentRunTestResult } from "./IAssignmentRunTestResult";
import { IRunResult } from "./IRunResult";

export interface IAssignmentRunResponse {
    assignmentId: number;
    assignmentName: string;
    compile: IRunResult;
    results: IAssignmentRunTestResult[];
    passedCount: number;
    totalCount: number;
}
