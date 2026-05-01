import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideAuth, LogLevel } from 'angular-auth-oidc-client';
import { provideApollo } from 'apollo-angular';
import { InMemoryCache } from '@apollo/client/core';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
    provideAuth({
      config: {
        authority: environment.keycloak.authority,
        redirectUrl: environment.keycloak.redirectUrl,
        clientId: environment.keycloak.clientId,
        scope: environment.keycloak.scope,
        responseType: environment.keycloak.responseType,
        silentRenew: environment.keycloak.silentRenew,
        useRefreshToken: environment.keycloak.useRefreshToken,
        logLevel: LogLevel.Warn,
        secureRoutes: [environment.apiUrl, environment.graphqlUrl],
      }
    }),
    provideApollo(() => ({
      cache: new InMemoryCache(),
      uri: environment.graphqlUrl,
    })),
    provideStore({}),
    provideEffects([]),
  ]
};
