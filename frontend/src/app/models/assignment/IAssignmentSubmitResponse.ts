import { SubmissionStatusEnum } from "../enum/SubmissionStatusEnum";
import { IUserResponse } from "../user/IUserResponse";

export interface IAssignmentSubmitResponse {
    id: number;
    assignmentId: number;
    submitter: IUserResponse;
    status: SubmissionStatusEnum;
    finalScore: number;
    submittedAt: string;
    code: string;
}
