import { SortDirection } from "../../config/app-types";
import { SubmissionStatusEnum } from "../enum/SubmissionStatusEnum";

export interface ISubmissionQueryParams {
    page: number;
    size: number;
    search?: string | null;
    sortBy?: string;
    sortDir?: SortDirection;
    
    assignmentId?: number | null;
    userId?: number | null;
    status?: SubmissionStatusEnum | null;
    submittedAfter?: string | null;
    submittedBefore?: string | null;
}
