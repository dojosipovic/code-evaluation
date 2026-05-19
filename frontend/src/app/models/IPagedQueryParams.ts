import { SortDirection } from "../config/app-types";

export interface IPagedQueryParams {
    page: number;
    size: number;
    search?: string | null;
    sortBy?: string;
    sortDirection?: SortDirection;
}
