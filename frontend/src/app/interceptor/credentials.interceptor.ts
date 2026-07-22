import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth/auth.service';

export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  if (req.url.endsWith('config.json')) return next(req);

  const isApiCall = req.url.startsWith(auth.apiBase);

  if (!isApiCall) return next(req);

  return next(req.clone({ withCredentials: true }));
};
