import { IUserResponse } from "../user/IUserResponse";

export interface IGroupResponse {
    id: number;
    name: string;
    description: string;
    createdAt: string;
    memberCount: number;
    owner: IUserResponse;
}
