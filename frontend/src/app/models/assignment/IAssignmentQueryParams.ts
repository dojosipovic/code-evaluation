import { SortDirection } from "../../config/app-types";

export interface IAssignmentQueryParams {
  page: number;
  size: number;
  search?: string | null;
  sortBy?: string;
  sortDir?: SortDirection;

  groupId?: number | null;
  active?: boolean | null;
  submitted?: boolean | null;
  ungraded?: boolean | null;
}
