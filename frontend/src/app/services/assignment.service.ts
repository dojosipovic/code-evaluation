import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { ConfigService } from "./config.service";
import { Observable } from "rxjs";
import { IAssignmentResponse } from "../models/assignment/IAssignmentResponse";
import { IAssignmentRun } from "../models/assignment/IAssignmentRun";
import { IAssignmentRunResponse } from "../models/assignment/IAssignmentRunResponse";

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

    runAssignment(assignmentId: number, payload: IAssignmentRun): Observable<IAssignmentRunResponse> {
        return this.http.post<IAssignmentRunResponse>(`${this.baseUrl}/${assignmentId}/run`, payload);
    }

}
