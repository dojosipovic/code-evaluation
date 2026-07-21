import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { IDashboard } from '../models/dashboard/IDashboard';
import { ConfigService } from './config.service';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);
  private config = inject(ConfigService);

  getDashboard(): Observable<IDashboard> {
    return this.http.get<IDashboard>(`${this.config.apiUrl}/api/dashboard`);
  }
}
