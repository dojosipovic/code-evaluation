import { ISubmissionGradeRequest } from "../submission/ISubmissionGradeRequest";

export interface IAssignmentEvaluateRequest {
    submissions: ISubmissionGradeRequest[];
}
