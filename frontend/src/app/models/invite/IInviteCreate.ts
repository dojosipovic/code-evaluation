import { RoleEnum } from "../enum/RoleEnum";

export interface IInviteCreate {
    email: string;
    role: RoleEnum;
}