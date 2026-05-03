import { RoleEnum } from "../enum/RoleEnum";

export interface IUserResponse {
    id: number;
    username: string;
    firstname: string;
    lastname: string;
    email: string;
    role: RoleEnum;
    enabled: boolean;
}
