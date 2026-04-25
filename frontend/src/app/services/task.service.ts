import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { ITaskQueryParams } from "../models/task/ITaskQueryParams";
import { Observable } from "rxjs";
import { IPagedResponse } from "../models/IPagedResponse";
import { ITaskListItem } from "../models/task/ITaskListItem";
import { TaskQueryParamEnum } from "../models/enum/TaskQueryParamEnum";

@Injectable({ providedIn: 'root' })
export class TaskService {
    private http = inject(HttpClient);
    private readonly baseUrl = '/api/tasks';

    getTasks(params: ITaskQueryParams): Observable<IPagedResponse<ITaskListItem>> {
        return this.http.get<IPagedResponse<ITaskListItem>>(this.baseUrl, {
            params: this.buildParams(params)
        });
    }

    enableTask(id: number): Observable<void> {
        return this.http.patch<void>(`${this.baseUrl}/${id}`, { enabled: true });
    }

    disableTask(id: number): Observable<void> {
        return this.http.patch<void>(`${this.baseUrl}/${id}`, { enabled: false });
    }

    publishTask(id: number): Observable<void> {
        return this.http.post<void>(`${this.baseUrl}/${id}/publish`, {});
    }

    deleteTask(id: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${id}`);
    }

    private buildParams(params: ITaskQueryParams): HttpParams {
    let httpParams = new HttpParams()
      .set('page', params.page)
      .set('size', params.size);

    if (params.search) {
      httpParams = httpParams.set(TaskQueryParamEnum.SEARCH, params.search);
    }

    if (params.status) {
      httpParams = httpParams.set(TaskQueryParamEnum.STATUS, params.status);
    }

    if (params.enabled !== null && params.enabled !== undefined) {
      httpParams = httpParams.set(TaskQueryParamEnum.ENABLED, params.enabled);
    }

    if (params.shared !== null && params.shared !== undefined) {
      httpParams = httpParams.set(TaskQueryParamEnum.SHARED, params.shared);
    }

    if (params.excludeCurrentUser !== null && params.excludeCurrentUser !== undefined) {
      httpParams = httpParams.set(TaskQueryParamEnum.EXCLUDE_CURRENT_USER, params.excludeCurrentUser);
    }

    if (params.sortBy) {
      httpParams = httpParams.set(TaskQueryParamEnum.SORT_BY, params.sortBy);
    }

    if (params.sortDir) {
      httpParams = httpParams.set(TaskQueryParamEnum.SORT_DIRECTION, params.sortDir);
    }

    return httpParams;
  }
}
