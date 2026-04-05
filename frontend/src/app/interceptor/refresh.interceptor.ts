import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { AuthService } from '../services/auth/auth.service';
import { catchError, shareReplay, switchMap, throwError } from 'rxjs';

let refreshInFlight$: ReturnType<AuthService['refresh']> | null = null;

export const refreshInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const messages = inject(MessageService);

  // Nemoj pokušavati refreshati refresh/login/logout pozive
  const isAuthEndpoint =
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/refresh') ||
    req.url.includes('/auth/logout');

  return next(req).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse)) return throwError(() => err);

      // samo 401 i samo za "normalne" API pozive
      if (err.status !== 401 || isAuthEndpoint) {
        return throwError(() => err);
      }

      // ako nema refresh u tijeku, pokreni ga
      if (!refreshInFlight$) {
        refreshInFlight$ = auth.refresh().pipe(
          shareReplay(1)
        );
      }

      return refreshInFlight$.pipe(
        switchMap((ok) => {
            // reset single-flight
            refreshInFlight$ = null;

            if (!ok) {
                auth.setToken(null);
                messages.add({
                    severity: 'warn',
                    summary: 'Sesija je istekla',
                    detail: 'Molimo prijavite se ponovno.'
                });
                router.navigateByUrl('/login');
                return throwError(() => err);
            }

            // refresh uspio -> ponovi original request (sad bearerInterceptor dodaje novi token)
            const newToken = auth.getToken();
            if (!newToken) {
                router.navigateByUrl('/login');
                return throwError(() => err);
            }
            return next(req.clone({
                setHeaders: { Authorization: `Bearer ${newToken}` }
            }));
        }),
        catchError((e) => {
          refreshInFlight$ = null;
          return throwError(() => e);
        })
      );
    })
  );
};