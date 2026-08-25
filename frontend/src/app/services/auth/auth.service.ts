import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { ILoginRequest } from '../../models/auth/ILoginRequest';
import { ILoginResponse } from '../../models/auth/ILoginResponse';
import { catchError, map, Observable, of, tap } from 'rxjs';
import { IRefreshResponse } from '../../models/auth/IRefreshResponse';
import { AppRole } from '../../config/app-types';
import { IRegisterRequest } from '../../models/auth/IRegisterRequest';
import { IUserResponse } from '../../models/user/IUserResponse';
import { ConfigService } from '../config.service';
import { IPlagScanTokenResponse } from '../../models/auth/IPlagScanTokenResponse';
import {
  ITotpSetupResponse,
  ITwoFactorSettings,
  IWebAuthnOptionsResponse
} from '../../models/auth/ITwoFactor';

export interface JwtPayload {
  sub?: string;
  email?: string;
  groups?: string[];
  exp?: number;
};

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly tokenKey = 'access_token';
  private readonly http = inject(HttpClient);
  private readonly config = inject(ConfigService);

  get apiBase(): string {
    return this.config.apiUrl;
  }

  private _token = signal<string | null>(this.readToken());

  token = computed(() => this._token());
  isAuthenticated = computed(() => !!this.token());

  payload = computed<JwtPayload | null>(() => this.parseToken(this.token()));
  roles = computed<AppRole[]>(() => {
    const roles = this.payload()?.groups ?? [];
    return roles as AppRole[];
  });

  username = computed<string>(() => {
    return this.payload()?.sub ?? "";
  });

  isAdmin = computed(() => this.roles().includes('ADMIN'));
  isProf = computed(() => this.roles().includes('PROF'));
  isUser = computed(() => this.roles().includes('STUDENT'));

  primaryRole = computed<AppRole | null>(() => {
    if (this.isAdmin()) return 'ADMIN';
    if (this.isProf()) return 'PROF';
    if (this.isUser()) return 'STUDENT';
    return null;
  });

  isExpired = computed(() => {
    const exp = this.payload()?.exp;
    if (!exp) return false;
    return Date.now() >= exp * 1000;
  });

  login(req: ILoginRequest, remember: boolean): Observable<ILoginResponse | null> {
    return this.http.post<ILoginResponse>(`${this.apiBase}/api/auth/login`, req).pipe(
      tap((res) => {
        if (res.status === 'AUTHENTICATED' && res.accessToken) {
          this.setToken(res.accessToken, remember);
        }
      }),
      catchError(() => of(null))
    );
  }

  verifyTotpLogin(twoFactorToken: string, code: string, remember: boolean) {
    return this.http.post<ILoginResponse>(`${this.apiBase}/api/auth/2fa/totp/verify`, {
      twoFactorToken,
      code
    }).pipe(
      tap((res) => {
        if (res.accessToken) {
          this.setToken(res.accessToken, remember);
        }
      }),
      map((res) => !!res.accessToken),
      catchError(() => of(false))
    );
  }

  startSecondFactorWebAuthn(twoFactorToken: string): Observable<IWebAuthnOptionsResponse> {
    return this.http.post<IWebAuthnOptionsResponse>(`${this.apiBase}/api/auth/2fa/webauthn/options`, {
      twoFactorToken
    });
  }

  finishSecondFactorWebAuthn(
    twoFactorToken: string,
    token: string,
    responseJson: string,
    remember: boolean
  ) {
    return this.http.post<ILoginResponse>(
      `${this.apiBase}/api/auth/2fa/webauthn/verify/${encodeURIComponent(twoFactorToken)}`,
      { token, responseJson }
    ).pipe(
      tap((res) => {
        if (res.accessToken) {
          this.setToken(res.accessToken, remember);
        }
      }),
      map((res) => !!res.accessToken),
      catchError(() => of(false))
    );
  }

  startPasskeyLogin(): Observable<IWebAuthnOptionsResponse> {
    return this.http.post<IWebAuthnOptionsResponse>(`${this.apiBase}/api/auth/passkey/options`, {});
  }

  finishPasskeyLogin(token: string, responseJson: string, remember: boolean) {
    return this.http.post<ILoginResponse>(`${this.apiBase}/api/auth/passkey/verify`, {
      token,
      responseJson
    }).pipe(
      tap((res) => {
        if (res.accessToken) {
          this.setToken(res.accessToken, remember);
        }
      }),
      map((res) => !!res.accessToken),
      catchError(() => of(false))
    );
  }

  refresh() {
    return this.http
      .post<IRefreshResponse>(`${this.apiBase}/api/auth/refresh`, {}, { withCredentials: true })
      .pipe(
        tap((res) => this.setToken(res.accessToken)),
        map(() => true),
        catchError(() => of(false))
      );
  }

  getPlagScanToken(assignmentId: number): Observable<IPlagScanTokenResponse> {
    return this.http.post<IPlagScanTokenResponse>(`${this.apiBase}/api/auth/plagscan-token/${assignmentId}`, {});
  }

  logout() {
    return this.http.post(`${this.apiBase}/api/auth/logout`, {}, { withCredentials: true }).pipe(
      tap(() => this.setToken(null)),
      map(() => true),
      catchError(() => {
        this.setToken(null);
        return of(false);
      })
    );
  }

  logoutEverywhere() {
    return this.http.post(`${this.apiBase}/api/auth/logout-everywhere`, {}, { withCredentials: true }).pipe(
      tap(() => this.setToken(null)),
      map(() => true),
      catchError(() => {
        this.setToken(null);
        return of(false);
      })
    );
  }

  getToken(): string | null {
    return this._token();
  }

  hasRole(role: AppRole): boolean {
    return this.roles().includes(role);
  }

  hasAnyRole(requiredRoles: AppRole[]): boolean {
    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }

    const userRoles = this.roles();
    return requiredRoles.some(role => userRoles.includes(role));
  }

  setToken(token: string | null, remember?: boolean) {
    if (!token) {
      localStorage.removeItem(this.tokenKey);
      sessionStorage.removeItem(this.tokenKey);
      this._token.set(null);
      return;
    }

    const currentlyInLocal = !!localStorage.getItem(this.tokenKey);
    const useLocal = remember ?? currentlyInLocal;

    const store = useLocal ? localStorage : sessionStorage;
    store.setItem(this.tokenKey, token);
    (useLocal ? sessionStorage : localStorage).removeItem(this.tokenKey);

    this._token.set(token);
  }

  private readToken(): string | null {
    return localStorage.getItem(this.tokenKey) ?? sessionStorage.getItem(this.tokenKey);
  }

  private parseToken(token: string | null): JwtPayload | null {
    if (!token) return null;

    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;

      const payload = parts[1];
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const json = atob(normalized);
      return JSON.parse(json) as JwtPayload;
    } catch {
      return null;
    }
  }

  register(payload: IRegisterRequest): Observable<IUserResponse> {
    return this.http.post<IUserResponse>(`${this.apiBase}/api/auth/register`, payload);
  }

  getTwoFactorSettings(): Observable<ITwoFactorSettings> {
    return this.http.get<ITwoFactorSettings>(`${this.apiBase}/api/auth/2fa/settings`);
  }

  startTotpSetup(): Observable<ITotpSetupResponse> {
    return this.http.post<ITotpSetupResponse>(`${this.apiBase}/api/auth/2fa/totp/setup`, {});
  }

  confirmTotpSetup(code: string): Observable<void> {
    return this.http.post<void>(`${this.apiBase}/api/auth/2fa/totp/confirm`, { code });
  }

  disableTotp(): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/api/auth/2fa/totp`);
  }

  startWebAuthnRegistration(): Observable<IWebAuthnOptionsResponse> {
    return this.http.post<IWebAuthnOptionsResponse>(
      `${this.apiBase}/api/auth/2fa/webauthn/register/options`,
      {}
    );
  }

  finishWebAuthnRegistration(token: string, responseJson: string): Observable<void> {
    return this.http.post<void>(`${this.apiBase}/api/auth/2fa/webauthn/register/verify`, {
      token,
      responseJson
    });
  }

  deleteWebAuthnCredential(credentialId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/api/auth/2fa/webauthn/${credentialId}`);
  }
}
