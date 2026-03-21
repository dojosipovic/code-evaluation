import { SortDirection } from "../../config/app-types";
import { InviteStatusEnum } from "../enum/InviteStatusEnum";
import { RoleEnum } from "../enum/RoleEnum";

export interface IInviteQueryParams {
    page: number;
    size: number;
    email?: string | null;
    status?: InviteStatusEnum | null;
    role?: RoleEnum | null;
    createdByAdminId?: string | null;
    sortBy?: string;
    sortDirection?: SortDirection;
}
