export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  keycloak: {
    authority: 'http://localhost:8180/realms/taskmaster-admin',
    redirectUrl: 'http://localhost:4201/callback',
    postLogoutRedirectUri: 'http://localhost:4201/login',
    clientId: 'taskmaster-admin-frontend',
    scope: 'openid profile email offline_access',
    responseType: 'code',
    silentRenew: true,
    useRefreshToken: true,
  }
};
