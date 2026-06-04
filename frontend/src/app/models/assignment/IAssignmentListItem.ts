import { ITaskResponse } from "../task/ITaskResponse";
import { IUserResponse } from "../user/IUserResponse";

export interface IAssignmentListItem {
    id: number;
    name: string;
    startsAt: string;
    endsAt: string;
    points: number;
    task?: ITaskResponse;
    createdBy: IUserResponse;
}
