import { ITaskBaseResponse } from "../task/ITaskBaseResponse";
import { IUserResponse } from "../user/IUserResponse";

export interface IAssignmentListItem {
    id: number;
    submissionId: number | null;
    name: string;
    startsAt: string;
    endsAt: string;
    points: number;
    requiresEvaluation: boolean | null;
    task?: ITaskBaseResponse;
    createdBy: IUserResponse;
}
