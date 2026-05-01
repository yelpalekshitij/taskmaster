export const environment = {
  production: true,
  apiUrl: '/api',
  keycloak: {
    authority: 'http://keycloak:8180/realms/taskmaster-admin',
    redirectUrl: window.location.origin,
    clientId: 'taskmaster-admin-frontend',
    scope: 'openid profile email',
    responseType: 'code',
    silentRenew: true,
    useRefreshToken: true,
  }
};
