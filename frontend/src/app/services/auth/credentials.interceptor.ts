import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  const isApiCall = req.url.startsWith(auth.apiBase);

  if (!isApiCall) return next(req);

  return next(req.clone({ withCredentials: true }));
};
