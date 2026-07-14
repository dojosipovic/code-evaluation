import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { SortDirection } from "../config/app-types";
import { PlagScanClusterQueryParamEnum } from "../models/enum/PlagScanClusterQueryParamEnum";
import { IPagedResponse } from "../models/IPagedResponse";
import { IPlagScanCluster } from "../models/plagscan/IPlagScanCluster";
import { IPlagScanClusterQueryParams } from "../models/plagscan/IPlagScanClusterQueryParams";
import { ConfigService } from "./config.service";

@Injectable({ providedIn: 'root' })
export class PlagScanService {

    private http = inject(HttpClient);
    private config = inject(ConfigService);

    private get baseUrl(): string {
        return `${this.config.apiUrl}/api/plagscan`;
    }

    getClusters(params: IPlagScanClusterQueryParams): Observable<IPagedResponse<IPlagScanCluster>> {
        const defaultSortDirection: SortDirection = 'desc';
        let httpParams = new HttpParams()
            .set(PlagScanClusterQueryParamEnum.PAGE, params.page)
            .set(PlagScanClusterQueryParamEnum.SIZE, params.size)
            .set(
                PlagScanClusterQueryParamEnum.SORT_DIRECTION,
                params.sortDir ?? defaultSortDirection
            );

        if (params.search) {
            httpParams = httpParams.set(PlagScanClusterQueryParamEnum.SEARCH, params.search);
        }

        if (params.sortBy) {
            httpParams = httpParams.set(PlagScanClusterQueryParamEnum.SORT_BY, params.sortBy);
        }

        if (params.assignmentId) {
            httpParams = httpParams.set(
                PlagScanClusterQueryParamEnum.ASSIGNMENT_ID,
                params.assignmentId
            );
        }

        return this.http.get<IPagedResponse<IPlagScanCluster>>(
            `${this.baseUrl}/clusters`,
            { params: httpParams }
        );
    }
}
