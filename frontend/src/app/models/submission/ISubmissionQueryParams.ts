import { SortDirection } from "../../config/app-types";
import { SubmissionStatusEnum } from "../enum/SubmissionStatusEnum";

export interface ISubmissionQueryParams {
    page: number;
    size: number;
    search?: string | null;
    sortBy?: string;
    sortDir?: SortDirection;
    
    assignmentId: number;
    userId: number;
    status: SubmissionStatusEnum;
    submittedAfter: string;
    submittedBefore: string;
}
