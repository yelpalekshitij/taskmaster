export const environment = {
  production: true,
  apiUrl: '/api',
  graphqlUrl: '/graphql',
  notificationGraphqlUrl: '/notification-graphql',
  keycloak: {
    authority: 'http://localhost:8180/realms/taskmaster-app',
    redirectUrl: window.location.origin + '/callback',
    postLogoutRedirectUri: window.location.origin + '/login',
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
