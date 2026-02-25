import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const bearerInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  if (req.url.includes('/auth/login') || req.url.includes('/auth/refresh') || req.url.includes('/auth/logout')) {
    return next(req);
  }
  
  if (!token) return next(req);

  return next(
    req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    })
  );
}
