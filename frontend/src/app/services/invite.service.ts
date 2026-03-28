import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { IInviteQueryParams } from "../models/invite/IInviteQueryParams";
import { Observable } from "rxjs";
import { IPagedResponse } from "../models/IPagedResponse";
import { IInviteResponse } from "../models/invite/IInviteResponse";
import { InviteQueryParamEnum } from "../models/enum/InviteQueryParamEnum";
import { SortDirection } from "../config/app-types";
import { IInviteCreate } from "../models/invite/IInviteCreate";

@Injectable({ providedIn: 'root' })
export class InviteService {
    private http = inject(HttpClient);
    private readonly baseUrl = '/api/invites';

    getInvites(params: IInviteQueryParams): Observable<IPagedResponse<IInviteResponse>> {
        const defaultSortDirection: SortDirection = 'desc';
        let httpParams = new HttpParams()
            .set(InviteQueryParamEnum.PAGE, params.page)
            .set(InviteQueryParamEnum.SIZE, params.size)
            .set(InviteQueryParamEnum.SORT_DIRECTION, params.sortDirection ?? defaultSortDirection);

        if (params.sortBy) httpParams = httpParams.set(InviteQueryParamEnum.SORT_BY, params.sortBy);
        if (params.email) httpParams = httpParams.set(InviteQueryParamEnum.EMAIL, params.email);
        if (params.status) httpParams = httpParams.set(InviteQueryParamEnum.STATUS, params.status);
        if (params.role) httpParams = httpParams.set(InviteQueryParamEnum.ROLE, params.role);
        if (params.createdByAdminId) httpParams = httpParams.set(InviteQueryParamEnum.CREATED_BY_ADMIN_ID, params.createdByAdminId);

        return this.http.get<IPagedResponse<IInviteResponse>>(this.baseUrl, { params: httpParams });
    }

    revokeInvite(id: number): Observable<void> {
        return this.http.post<void>(`${this.baseUrl}/${id}/revoke`, {});
    }

    createInvite(payload: IInviteCreate): Observable<IInviteResponse> {
        return this.http.post<IInviteResponse>(this.baseUrl, payload);
    }
}
