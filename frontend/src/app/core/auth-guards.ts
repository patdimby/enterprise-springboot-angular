import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthStore } from './auth-store';

/**
 * Gardes de route = vigies AVANT d'ouvrir une page.
 *
 * CanActivateFn = une fonction qui retourne :
 * - true → "tu peux entrer"
 * - un UrlTree (createUrlTree) → "va plutôt sur cette autre URL"
 *
 * authGuard : il faut être connecté. Sinon → /login, avec returnUrl
 * pour revenir ici après le mot de passe.
 *
 * adminGuard : il faut le rôle ADMIN. Sinon → /forbidden.
 *
 * Règle d'or : une garde Angular est un CONFORT (UX). La VRAIE sécurité
 * est dans Spring Security (un USER qui appelle /api/users reçoit 403).
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    return true;
  }
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  if (auth.isAdmin()) {
    return true;
  }
  return router.createUrlTree(['/forbidden']);
};
