export const environment = {
  production: true,
  apiUrl: '/api',
  graphqlUrl: '/graphql',
  keycloak: {
    authority: 'http://keycloak:8180/realms/taskmaster-app',
    redirectUrl: window.location.origin + '/callback',
    postLogoutRedirectUri: window.location.origin + '/login',
    clientId: 'taskmaster-user-frontend',
    scope: 'openid profile email offline_access',
    responseType: 'code',
    silentRenew: true,
    useRefreshToken: true,
  },
  fcm: {
    vapidKey: 'YOUR_VAPID_KEY_HERE'
  }
};
