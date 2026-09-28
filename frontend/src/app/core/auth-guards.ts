import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthStore } from './auth-store';

/**
 * GARDES DE ROUTE — les vigies qui décident si une page est accessible.
 *
 * <p>{@code authGuard} : interdit les pages réservées aux connectés
 * (ex. /users) et renvoie au /login sinon. Le paramètre "returnUrl" permet
 * de revenir à la page demandée APRÈS le login (petit confort).</p>
 *
 * <p>{@code adminGuard} : en plus d'être connecté, il faut le rôle ADMIN
 * pour /users (le backend le re-vérifie de toute façon : la garde est un
 * confort UX, la SÉCURITÉ reste côté serveur — règle d'or).</p>
 */
export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    return true; // Connecté : on passe.
  }
  // Pas connecté : on note la page souhaitée puis on redirige.
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  if (auth.isAdmin()) {
    return true; // ADMIN : on passe.
  }
  // Connecté mais pas admin → page "interdit" maison.
  return router.createUrlTree(['/forbidden']);
};
