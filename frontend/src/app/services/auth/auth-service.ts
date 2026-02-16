import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { ILoginRequest } from '../../models/ILoginRequest';
import { ILoginResponse } from '../../models/ILoginResponse';
import { catchError, map, of, tap } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly tokenKey = 'access_token';
  private readonly apiBase = 'http://localhost:8080'

  private _token = signal<string | null>(this.readToken());
  token = computed(() => this._token());
  isAuthenticated = computed(() => !!this.token());

  constructor(private http: HttpClient) {}

  login(req: ILoginRequest) {
    return this.http.post<ILoginResponse>(`${this.apiBase}/auth/login`, req)
      .pipe(
        tap((res) => {
          this.writeToken(res.accessToken);
          this._token.set(res.accessToken);
        }),
        map(() => true),
        catchError(() => of(false))
      );
  }

  logout() {
    sessionStorage.removeItem(this.tokenKey);
    this._token.set(null);
  }

  getToken(): string | null {
    return this._token();
  }

  private writeToken(token: string) {
    sessionStorage.setItem(this.tokenKey, token);
  }

  private readToken(): string | null {
    return localStorage.getItem(this.tokenKey) ?? sessionStorage.getItem(this.tokenKey);
  }
}
