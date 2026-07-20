import { SortDirection } from "../../config/app-types";

export interface IPlagScanClusterQueryParams {
    page: number;
    size: number;
    search?: string | null;
    sortBy?: string;
    sortDir?: SortDirection;
    assignmentId?: number | null;
}
