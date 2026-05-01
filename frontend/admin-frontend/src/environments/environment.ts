export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  keycloak: {
    authority: 'http://localhost:8180/realms/taskmaster-admin',
    redirectUrl: 'http://localhost:4201',
    clientId: 'taskmaster-admin-frontend',
    scope: 'openid profile email',
    responseType: 'code',
    silentRenew: true,
    useRefreshToken: true,
  }
};
