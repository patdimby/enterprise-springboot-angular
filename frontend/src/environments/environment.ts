/**
 * Environnement de PRODUCTION (build `ng build`).
 *
 * En prod, le frontend est servi par nginx (Docker) qui proxifie /api
 * vers le backend Spring Boot : on appelle donc la même origine.
 * Voir frontend/nginx.conf et docker-compose.yml.
 */
export const environment = {
  production: true,
  // Toutes les requêtes partent vers "/api..." sur le MÊME domaine.
  apiUrl: '/api',
};
