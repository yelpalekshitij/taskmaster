export const environment = {
  production: true,
  apiUrl: '/api',
  keycloak: {
    authority: 'http://localhost:8180/realms/taskmaster-admin',
    redirectUrl: window.location.origin + '/callback',
    postLogoutRedirectUri: window.location.origin + '/login',
    clientId: 'taskmaster-admin-frontend',
    scope: 'openid profile email',
    responseType: 'code',
    silentRenew: true,
    useRefreshToken: true,
  }
};
