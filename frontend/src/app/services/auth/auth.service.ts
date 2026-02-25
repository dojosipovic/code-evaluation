import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { ILoginRequest } from '../../models/ILoginRequest';
import { ILoginResponse } from '../../models/ILoginResponse';
import { catchError, map, of, tap } from 'rxjs';
import { IRefreshResponse } from '../../models/IRefreshResponse';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly tokenKey = 'access_token';
  private _token = signal<string | null>(this.readToken());
  private http = inject(HttpClient);

  readonly apiBase = '';

  token = computed(() => this._token());
  isAuthenticated = computed(() => !!this.token());

  login(req: ILoginRequest, remember: boolean) {
    return this.http.post<ILoginResponse>(`${this.apiBase}/auth/login`, req)
      .pipe(
        tap((res) => {
          this.setToken(res.accessToken, remember);
          this._token.set(res.accessToken);
        }),
        map(() => true),
        catchError(() => of(false))
      );
  }

  getToken(): string | null {
    return this._token();
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

  refresh() {
    return this.http
      .post<IRefreshResponse>(`${this.apiBase}/auth/refresh`, {}, { withCredentials: true })
      .pipe(
        tap(res => this.setToken(res.accessToken)),
        map(() => true),
        catchError(() => of(false))
      );
  }

  logout() {
    return this.http.post(`${this.apiBase}/auth/logout`, {}, { withCredentials: true }).pipe(
      tap(() => this.setToken(null)),
      map(() => true),
      catchError(() => {
        this.setToken(null);
        return of(false);
      })
    );
  }

  private readToken(): string | null {
    return localStorage.getItem(this.tokenKey) ?? sessionStorage.getItem(this.tokenKey);
  }
}
