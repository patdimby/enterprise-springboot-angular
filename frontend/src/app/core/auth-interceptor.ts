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
 * 2. Si le serveur répond 401 (token expiré / invalide), on déconnecte
 *    et on renvoie vers /login — l'utilisateur n'est pas coincé.
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
      if (error instanceof HttpErrorResponse && error.status === 401) {
        auth.logout();
        router.navigateByUrl('/login');
      }
      // On relance l'erreur pour que le composant puisse afficher un message.
      return throwError(() => error);
    }),
  );
};
