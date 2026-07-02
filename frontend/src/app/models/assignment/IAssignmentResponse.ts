import { ISubmissionResponse } from "../submission/ISubmissionResponse";
import { ITaskResponse } from "../task/ITaskResponse";
import { IUserResponse } from "../user/IUserResponse";

export interface IAssignmentResponse {
    id: number;
    groupId: number;
    name: string;
    startsAt: string;
    endsAt: string;
    points: number;
    submission: ISubmissionResponse;
    task: ITaskResponse;
    createdBy: IUserResponse;
}
