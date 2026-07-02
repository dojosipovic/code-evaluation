import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { ConfigService } from "./config.service";
import { Observable } from "rxjs";
import { ISubmissionResponse } from "../models/submission/ISubmissionResponse";

@Injectable({ providedIn: 'root' })
export class SubmissionService {

    private http = inject(HttpClient);
    private config = inject(ConfigService);

    private get baseUrl(): string {
        return `${this.config.apiUrl}/api/submissions`;
    }

    getSubmission(submissionId: number): Observable<ISubmissionResponse> {
        return this.http.get<ISubmissionResponse>(`${this.baseUrl}/${submissionId}`);
    }

}
