import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { map, take } from 'rxjs/operators';

export const masterAdminGuard: CanActivateFn = () => {
  const oidcService = inject(OidcSecurityService);
  const router = inject(Router);

  return oidcService.getUserData().pipe(
    take(1),
    map(userData => {
      const roles: string[] = userData?.realm_access?.roles || [];
      if (roles.includes('MASTER_ADMIN')) {
        return true;
      }
      return router.createUrlTree(['/dashboard']);
    })
  );
};

export const tenantAdminGuard: CanActivateFn = () => {
  const oidcService = inject(OidcSecurityService);
  const router = inject(Router);

  return oidcService.getUserData().pipe(
    take(1),
    map(userData => {
      const roles: string[] = userData?.realm_access?.roles || [];
      if (roles.includes('TENANT_ADMIN') || roles.includes('MASTER_ADMIN')) {
        return true;
      }
      return router.createUrlTree(['/dashboard']);
    })
  );
};

export const anyAdminGuard: CanActivateFn = () => {
  const oidcService = inject(OidcSecurityService);
  const router = inject(Router);

  return oidcService.getUserData().pipe(
    take(1),
    map(userData => {
      const roles: string[] = userData?.realm_access?.roles || [];
      const adminRoles = ['MASTER_ADMIN', 'TENANT_ADMIN'];
      if (adminRoles.some(role => roles.includes(role))) {
        return true;
      }
      return router.createUrlTree(['/unauthorized']);
    })
  );
};
