import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { ConfigService } from "./config.service";
import { Observable } from "rxjs";
import { IPagedResponse } from "../models/IPagedResponse";
import { AssignmentQueryParamEnum } from "../models/enum/AssignmentQueryParamEnum";
import { SortDirection } from "../config/app-types";
import { IAssignmentListItem } from "../models/assignment/IAssignmentListItem";
import { IAssignmentQueryParams } from "../models/assignment/IAssignmentQueryParams";
import { IAssignmentCreate } from "../models/assignment/IAssignmentCreate";
import { IAssignmentResponse } from "../models/assignment/IAssignmentResponse";
import { IAssignmentRun } from "../models/assignment/IAssignmentRun";
import { IAssignmentRunResponse } from "../models/assignment/IAssignmentRunResponse";
import { IAssignmentSubmit } from "../models/assignment/IAssignmentSubmit";
import { IAssignmentSubmitResponse } from "../models/assignment/IAssignmentSubmitResponse";
import { IAssignmentEvaluateRequest } from "../models/assignment/IAssignmentEvaluateRequest";
import { ISubmissionResponse } from "../models/submission/ISubmissionResponse";

@Injectable({ providedIn: 'root' })
export class AssignmentService {
    private http = inject(HttpClient);
    private config = inject(ConfigService);

    private get baseUrl(): string {
        return `${this.config.apiUrl}/api/assignments`;
    }

    getAssignment(assignmentId: number): Observable<IAssignmentResponse> {
        return this.http.get<IAssignmentResponse>(`${this.baseUrl}/${assignmentId}`);
    }

    getAssignments(params: IAssignmentQueryParams): Observable<IPagedResponse<IAssignmentListItem>> {
        return this.http.get<IPagedResponse<IAssignmentListItem>>(this.baseUrl, {
            params: this.buildParams(params)
        });
    }

    createAssignment(payload: IAssignmentCreate): Observable<IAssignmentResponse> {
        return this.http.post<IAssignmentResponse>(`${this.baseUrl}`, payload);
    }

    runAssignment(assignmentId: number, payload: IAssignmentRun): Observable<IAssignmentRunResponse> {
        return this.http.post<IAssignmentRunResponse>(`${this.baseUrl}/${assignmentId}/run`, payload);
    }

    submitAssignment(assignmentId: number, payload: IAssignmentSubmit): Observable<IAssignmentSubmitResponse> {
        return this.http.post<IAssignmentSubmitResponse>(`${this.baseUrl}/${assignmentId}/submit`, payload);
    }

    evaluateAssignment(assignmentId: number, payload: IAssignmentEvaluateRequest): Observable<ISubmissionResponse[]> {
        return this.http.post<ISubmissionResponse[]>(`${this.baseUrl}/${assignmentId}/evaluate`, payload);
    }

    private buildParams(params: IAssignmentQueryParams): HttpParams {
        const defaultSortDirection: SortDirection = 'desc';
        let httpParams = new HttpParams()
            .set(AssignmentQueryParamEnum.PAGE, params.page)
            .set(AssignmentQueryParamEnum.SIZE, params.size)
            .set(AssignmentQueryParamEnum.SORT_DIRECTION, params.sortDir ?? defaultSortDirection);

        if (params.search) {
            httpParams = httpParams.set(AssignmentQueryParamEnum.SEARCH, params.search);
        }

        if (params.sortBy) {
            httpParams = httpParams.set(AssignmentQueryParamEnum.SORT_BY, params.sortBy);
        }

        if (params.groupId !== null && params.groupId !== undefined) {
            httpParams = httpParams.set(AssignmentQueryParamEnum.GROUP_ID, params.groupId);
        }

        if (params.active !== null && params.active !== undefined) {
            httpParams = httpParams.set(AssignmentQueryParamEnum.ACTIVE, params.active);
        }

        if (params.submitted !== null && params.submitted !== undefined) {
            httpParams = httpParams.set(AssignmentQueryParamEnum.SUBMITTED, params.submitted);
        }

        if (params.ungraded !== null && params.ungraded !== undefined) {
            httpParams = httpParams.set(AssignmentQueryParamEnum.UNGRADED, params.ungraded);
        }

        return httpParams;
    }
}
