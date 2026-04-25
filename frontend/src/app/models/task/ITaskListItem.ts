import { TaskStatusEnum } from "../enum/TaskStatusEnum";
import { IUserResponse } from "../user/IUserResponse";

export interface ITaskListItem {
    id: number;
    title: string;
    status: TaskStatusEnum;
    enabled: boolean;
    shared: boolean;
    user: IUserResponse;
}
