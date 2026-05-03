import { SortDirection } from "../../config/app-types";
import { TaskStatusEnum } from "../enum/TaskStatusEnum";

export interface ITaskQueryParams {
  page: number;
  size: number;
  search?: string | null;
  status?: TaskStatusEnum | null;
  enabled?: boolean | null;
  shared?: boolean | null;
  excludeCurrentUser?: boolean | null;
  sortBy?: string;
  sortDir?: SortDirection;
}
