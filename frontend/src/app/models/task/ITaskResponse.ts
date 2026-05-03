import { TaskStatusEnum } from "../enum/TaskStatusEnum";
import { IUserResponse } from "../user/IUserResponse";
import { IStarterCode } from "./IStarterCode";
import { ITestResponse } from "./ITestResponse";

export interface ITaskResponse {
    id: number;
    title: string;
    description: string;
    starterCode: IStarterCode;
    includeStarterCode: boolean;
    shared: boolean;
    enabled: boolean;
    status: TaskStatusEnum;
    tests: ITestResponse[];
    user: IUserResponse;
}
