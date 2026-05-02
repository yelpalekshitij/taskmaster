import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { switchMap, take } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Never attach tokens to Keycloak's own endpoints — it triggers CORS preflight
  if (req.url.startsWith(environment.keycloak.authority)) {
    return next(req);
  }

  const oidcService = inject(OidcSecurityService);
  return oidcService.getAccessToken().pipe(
    take(1),
    switchMap(token => {
      if (token) {
        return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
      }
      return next(req);
    })
  );
};
