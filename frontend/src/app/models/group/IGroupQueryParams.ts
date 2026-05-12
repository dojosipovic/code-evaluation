import { SortDirection } from "../../config/app-types";

export interface IGroupQueryParams {
    page: number;
    size: number;
    search?: string | null;
    sortBy?: string;
    sortDirection?: SortDirection;
}
