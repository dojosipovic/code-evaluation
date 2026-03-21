import { InviteStatusEnum } from "../enum/InviteStatusEnum";
import { RoleEnum } from "../enum/RoleEnum";

export interface IInviteResponse {
  id: number;
  email: string;
  role: RoleEnum;
  status: InviteStatusEnum;
  expiresAt: string;
  createdAt: string;
}
