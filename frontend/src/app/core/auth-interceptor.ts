import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthStore } from './auth-store';

/**
 * Intercepteur HTTP = un "douanier" sur CHAQUE requête vers l'API.
 *
 * C'est une FONCTION (forme moderne Angular), enregistrée dans app.config.ts.
 *
 * Elle fait 2 choses :
 * 1. Ajoute l'en-tête Authorization: Bearer <token> si on est connecté.
 *    C'est exactement ce que le filtre Java JwtAuthenticationFilter attend.
 * 2. Si le serveur répond 401 sur une requête AUTHENTIFIÉE (token expiré ou
 *    invalide), on déconnecte et on renvoie vers /login — l'utilisateur
 *    n'est pas coincé.
 *
 * ⚠️ Subtilité : un 401 reçu sur POST /api/auth/login ne veut PAS dire
 * "token expiré" mais "mauvais mot de passe" — déconnecter l'utilisateur
 * serait faux (et effacerait sa session pour rien). On ignore donc les
 * endpoints publics d'authentification dans la logique de déconnexion.
 *
 * Important : on clone la requête (req.clone). Une requête HTTP est
 * immuable : on ne la modifie jamais, on en crée une copie.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  const session = auth.currentUser();
  const request = session
    ? req.clone({ setHeaders: { Authorization: `Bearer ${session.token}` } })
    : req;

  return next(request).pipe(
    catchError((error: unknown) => {
      // 401 = "token invalide" UNIQUEMENT si on avait un token à présenter.
      // Sans session (ou sur les routes publiques), c'est une erreur de
      // saisie classique : le composant gère, pas l'intercepteur.
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        session !== null
      ) {
        auth.logout();
        router.navigateByUrl('/login');
      }
      // On relance l'erreur pour que le composant puisse afficher un message.
      return throwError(() => error);
    }),
  );
};
