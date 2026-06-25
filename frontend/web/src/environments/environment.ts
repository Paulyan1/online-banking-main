export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  wsUrl: 'http://localhost:8080/ws',
  keycloak: {
    issuer: 'http://localhost:8180/realms/online-banking',
    clientId: 'online-banking-angular',
    redirectUri: window.location.origin,
    scope: 'openid profile email'
  }
};
