import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { ConfigService } from "./config.service";
import { Observable } from "rxjs";
import { ISubmissionDetailResponse } from "../models/submission/ISubmissionDetailResponse";

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

}
