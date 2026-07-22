import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AppConfig } from '../config/app-config.model';

@Injectable({ providedIn: 'root' })
export class ConfigService {
  private config?: AppConfig;
  private http = inject(HttpClient);

  load(): Promise<void> {
    return firstValueFrom(
      this.http.get<AppConfig>('config.json')
    ).then(config => {
      this.config = config;
    });
  }

  get value(): AppConfig {
    if (!this.config) {
      throw new Error('App config nije ucitan');
    }

    return this.config;
  }

  get apiUrl(): string {
    return this.value.apiUrl.replace(/\/+$/, '');
  }

  get plagScanReportUrl(): string {
    const configuredUrl = this.value.plagScanReportUrl?.trim();

    if (configuredUrl) {
      return configuredUrl.replace(/\/+$/, '');
    }

    return `${this.apiUrl}/api/plagscan/report`;
  }

  get plagScanReportViewerUrl(): string {
    const configuredUrl = this.value.plagScanReportViewerUrl?.trim();

    if (configuredUrl) {
      return configuredUrl.replace(/\/+$/, '');
    }

    return '';
  }
}
