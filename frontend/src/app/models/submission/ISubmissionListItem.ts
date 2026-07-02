import { SubmissionStatusEnum } from "../enum/SubmissionStatusEnum";
import { IUserResponse } from "../user/IUserResponse";

export interface ISubmissionListItem {
    id: number;
    assignmentId: number;
    taskId: number;
    user: IUserResponse;
    status: SubmissionStatusEnum;
    code: string;
    finalScore: number;
    submittedAt: string;
}
