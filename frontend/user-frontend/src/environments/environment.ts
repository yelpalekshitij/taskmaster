export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  graphqlUrl: 'http://localhost:8080/graphql',
  keycloak: {
    authority: 'http://localhost:8180/realms/taskmaster-app',
    redirectUrl: 'http://localhost:4200',
    clientId: 'taskmaster-user-frontend',
    scope: 'openid profile email',
    responseType: 'code',
    silentRenew: true,
    useRefreshToken: true,
  },
  fcm: {
    vapidKey: 'YOUR_VAPID_KEY_HERE'
  }
};
