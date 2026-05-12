import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { IGroupQueryParams } from "../models/group/IGroupQueryParams";
import { Observable } from "rxjs";
import { IPagedResponse } from "../models/IPagedResponse";
import { IGroupListItem } from "../models/group/IGroupListItem";
import { GroupQueryParamEnum } from "../models/enum/GroupQueryParamEnum";
import { SortDirection } from "../config/app-types";

@Injectable({ providedIn: 'root' })
export class GroupService {
    private http = inject(HttpClient);
    private readonly baseUrl = '/api/groups';

    getGroups(params: IGroupQueryParams): Observable<IPagedResponse<IGroupListItem>> {
        const defaultSortDirection: SortDirection = 'desc';
        let httpParams = new HttpParams()
            .set(GroupQueryParamEnum.PAGE, params.page)
            .set(GroupQueryParamEnum.SIZE, params.size)
            .set(GroupQueryParamEnum.SORT_DIRECTION, params.sortDirection ?? defaultSortDirection);

        if (params.search) httpParams = httpParams.set(GroupQueryParamEnum.SEARCH, params.search);
        if (params.sortBy) httpParams = httpParams.set(GroupQueryParamEnum.SORT_BY, params.sortBy);

        return this.http.get<IPagedResponse<IGroupListItem>>(this.baseUrl, { params: httpParams });
    }
}
