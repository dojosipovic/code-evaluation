import { RoleEnum } from "../enum/RoleEnum";

export interface IInviteValidate {
    valid: boolean;
    reason: string;
    email: string;
    role: RoleEnum
}
