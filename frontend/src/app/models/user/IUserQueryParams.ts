import { SortDirection } from "../../config/app-types";
import { RoleEnum } from "../enum/RoleEnum";

export interface IUserQueryParams {
    page: number;
    size: number;
    email?: string | null;
    username?: string | null;
    search?: string | null;
    role?: RoleEnum | null;
    enabled?: boolean | null;
    sortBy?: string;
    sortDirection?: SortDirection;
}
