import { RoleEnum } from "../enum/RoleEnum";

export interface IUserResponse {
    id: number;
    username: string;
    email: string;
    role: RoleEnum;
}
