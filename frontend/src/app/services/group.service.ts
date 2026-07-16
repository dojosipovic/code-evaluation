import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { IPagedResponse } from "../models/IPagedResponse";
import { IGroupListItem } from "../models/group/IGroupListItem";
import { PagedQueryParamEnum } from "../models/enum/PagedQueryParamEnum";
import { SortDirection } from "../config/app-types";
import { IGroupCreate } from "../models/group/IGroupCreate";
import { IGroupResponse } from "../models/group/IGroupResponse";
import { IGroupUpdate } from "../models/group/IGroupUpdate";
import { IPagedQueryParams } from "../models/IPagedQueryParams";
import { IUserResponse } from "../models/user/IUserResponse";
import { IGroupMember } from "../models/group/IGroupMember";
import { ConfigService } from "./config.service";

@Injectable({ providedIn: 'root' })
export class GroupService {
    private http = inject(HttpClient);
    private config = inject(ConfigService);

    private get baseUrl(): string {
        return `${this.config.apiUrl}/api/groups`;
    }

    getGroups(params: IPagedQueryParams): Observable<IPagedResponse<IGroupListItem>> {
        const defaultSortDirection: SortDirection = 'desc';
        let httpParams = new HttpParams()
            .set(PagedQueryParamEnum.PAGE, params.page)
            .set(PagedQueryParamEnum.SIZE, params.size)
            .set(PagedQueryParamEnum.SORT_DIRECTION, params.sortDirection ?? defaultSortDirection);

        if (params.search) httpParams = httpParams.set(PagedQueryParamEnum.SEARCH, params.search);
        if (params.sortBy) httpParams = httpParams.set(PagedQueryParamEnum.SORT_BY, params.sortBy);

        return this.http.get<IPagedResponse<IGroupListItem>>(this.baseUrl, { params: httpParams });
    }

    createGroup(payload: IGroupCreate): Observable<IGroupResponse> {
        return this.http.post<IGroupResponse>(this.baseUrl, payload);
    }

    getGroup(id: number): Observable<IGroupResponse> {
        return this.http.get<IGroupResponse>(`${this.baseUrl}/${id}`);
    }

    updateGroup(id: number, payload: IGroupUpdate): Observable<IGroupResponse> {
        return this.http.put<IGroupResponse>(`${this.baseUrl}/${id}`, payload);
    }

    getMembers(id: number, params: IPagedQueryParams): Observable<IPagedResponse<IGroupMember>> {
        const defaultSortDirection: SortDirection = 'desc';
        let httpParams = new HttpParams()
            .set(PagedQueryParamEnum.PAGE, params.page)
            .set(PagedQueryParamEnum.SIZE, params.size)
            .set(PagedQueryParamEnum.SORT_DIRECTION, params.sortDirection ?? defaultSortDirection);

        if (params.search) httpParams = httpParams.set(PagedQueryParamEnum.SEARCH, params.search);
        if (params.sortBy) httpParams = httpParams.set(PagedQueryParamEnum.SORT_BY, params.sortBy);

        return this.http.get<IPagedResponse<IGroupMember>>(`${this.baseUrl}/${id}/members`, { params: httpParams });
    }

    getNonMembers(id: number, params: IPagedQueryParams): Observable<IPagedResponse<IUserResponse>> {
        const defaultSortDirection: SortDirection = 'desc';
        let httpParams = new HttpParams()
            .set(PagedQueryParamEnum.PAGE, params.page)
            .set(PagedQueryParamEnum.SIZE, params.size)
            .set(PagedQueryParamEnum.SORT_DIRECTION, params.sortDirection ?? defaultSortDirection);

        if (params.search) httpParams = httpParams.set(PagedQueryParamEnum.SEARCH, params.search);
        if (params.sortBy) httpParams = httpParams.set(PagedQueryParamEnum.SORT_BY, params.sortBy);

        return this.http.get<IPagedResponse<IUserResponse>>(`${this.baseUrl}/${id}/non-members`, { params: httpParams });
    }

    removeMember(groupId: number, userId: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${groupId}/members/${userId}`);
    }

    addMember(groupId: number, userId: number): Observable<void> {
        return this.http.post<void>(`${this.baseUrl}/${groupId}/members/${userId}`, {});
    }

}
