/**
 * Environnement de DÉVELOPPEMENT (`ng serve`).
 *
 * Le dev-server Angular redirige /api vers http://localhost:8080
 * grâce à proxy.conf.json : pas de CORS à gérer, comme en production.
 */
export const environment = {
  production: false,
  apiUrl: '/api',
};
