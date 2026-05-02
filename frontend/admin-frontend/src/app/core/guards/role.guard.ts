import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { map, take } from 'rxjs/operators';

function parseRoles(token: string): string[] {
  if (!token) return [];
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return payload?.realm_access?.roles || [];
  } catch {
    return [];
  }
}

export const masterAdminGuard: CanActivateFn = () => {
  const oidcService = inject(OidcSecurityService);
  const router = inject(Router);
  return oidcService.getAccessToken().pipe(
    take(1),
    map(token => parseRoles(token).includes('MASTER_ADMIN') || router.createUrlTree(['/dashboard']))
  );
};

export const tenantAdminGuard: CanActivateFn = () => {
  const oidcService = inject(OidcSecurityService);
  const router = inject(Router);
  return oidcService.getAccessToken().pipe(
    take(1),
    map(token => {
      const roles = parseRoles(token);
      return (roles.includes('TENANT_ADMIN') || roles.includes('MASTER_ADMIN')) || router.createUrlTree(['/dashboard']);
    })
  );
};

export const anyAdminGuard: CanActivateFn = () => {
  const oidcService = inject(OidcSecurityService);
  const router = inject(Router);
  return oidcService.getAccessToken().pipe(
    take(1),
    map(token => {
      const roles = parseRoles(token);
      return ['MASTER_ADMIN', 'TENANT_ADMIN'].some(r => roles.includes(r)) || router.createUrlTree(['/unauthorized']);
    })
  );
};
