import { RoleEnum } from "../enum/RoleEnum";

export interface IGroupMember {
    id: number;
    username: string;
    firstname: string;
    lastname: string;
    email: string;
    role: RoleEnum;
    enabled: boolean;
    addedAt: string;
}
