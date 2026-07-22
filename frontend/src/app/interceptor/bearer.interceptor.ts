import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth/auth.service';

export const bearerInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();
  const pathname = getPathname(req.url);

  const skipsBearer =
    pathname.endsWith('/auth/login') ||
    pathname.endsWith('/auth/refresh') ||
    pathname.endsWith('/auth/logout');

  if (skipsBearer) {
    return next(req);
  }

  if (!token) return next(req);

  return next(
    req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    })
  );
}

function getPathname(url: string): string {
  try {
    return new URL(url).pathname;
  } catch {
    return url.split('?')[0];
  }
}
