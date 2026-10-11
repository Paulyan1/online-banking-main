export const environment = {
  production: true,

  apiUrl: 'https://paulsbank.duckdns.org',

  wsUrl: 'https://paulsbank.duckdns.org/ws',

  keycloak: {
    issuer: 'https://paulsbank.duckdns.org/realms/onlinebanking',
    clientId: 'onlinebanking-angular',
    redirectUri: window.location.origin,
    scope: 'openid profile email'
  }
};