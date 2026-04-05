import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { AuthService } from "./auth.service";
import { AppRole } from "../../config/app-types";

export const authGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredRoles = route.data?.['roles'] as AppRole[] | undefined;

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  return authService.hasAnyRole(requiredRoles)
    ? true
    : router.createUrlTree(['/dashboard']);

  // return auth.isAuthenticated() ? true : router.createUrlTree(['/login']);
};
