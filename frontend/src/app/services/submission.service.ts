import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { ConfigService } from "./config.service";
import { Observable } from "rxjs";
import { ISubmissionDetailResponse } from "../models/submission/ISubmissionDetailResponse";
import { ISubmissionQueryParams } from "../models/submission/ISubmissionQueryParams";
import { SortDirection } from "../config/app-types";
import { IPagedResponse } from "../models/IPagedResponse";
import { ISubmissionListItem } from "../models/submission/ISubmissionListItem";
import { SubmissionQueryParamEnum } from "../models/enum/SubmissionQueryParamEnum";

@Injectable({ providedIn: 'root' })
export class SubmissionService {

    private http = inject(HttpClient);
    private config = inject(ConfigService);

    private get baseUrl(): string {
        return `${this.config.apiUrl}/api/submissions`;
    }

    getSubmission(submissionId: number): Observable<ISubmissionDetailResponse> {
        return this.http.get<ISubmissionDetailResponse>(`${this.baseUrl}/${submissionId}`);
    }

    getSubmissions(params: ISubmissionQueryParams): Observable<IPagedResponse<ISubmissionListItem>> {
        const defaultSortDirection: SortDirection = 'desc';
        let httpParams = new HttpParams()
            .set(SubmissionQueryParamEnum.PAGE, params.page)
            .set(SubmissionQueryParamEnum.SIZE, params.size)
            .set(SubmissionQueryParamEnum.SORT_DIRECTION, params.sortDir ?? defaultSortDirection);

        if (params.search) httpParams = httpParams.set(SubmissionQueryParamEnum.SEARCH, params.search);
        if (params.sortBy) httpParams = httpParams.set(SubmissionQueryParamEnum.SORT_BY, params.sortBy);

        if (params.assignmentId) httpParams = httpParams.set(SubmissionQueryParamEnum.ASSIGNMENT_ID, params.assignmentId);
        if (params.userId) httpParams = httpParams.set(SubmissionQueryParamEnum.USER_ID, params.userId);
        if (params.status) httpParams = httpParams.set(SubmissionQueryParamEnum.STATUS, params.status);
        if (params.submittedAfter) httpParams = httpParams.set(SubmissionQueryParamEnum.SUBMITTED_AFTER, params.submittedAfter);
        if (params.submittedBefore) httpParams = httpParams.set(SubmissionQueryParamEnum.SUBMITTED_BEFORE, params.submittedBefore);

        return this.http.get<IPagedResponse<ISubmissionListItem>>(`${this.baseUrl}`, { params: httpParams });
    }

}
