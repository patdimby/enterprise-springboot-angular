import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthStore } from './auth-store';

/**
 * INTERCEPTEUR HTTP — une fonction exécutée sur CHAQUE requête sortante.
 *
 * <p>Fait deux choses :</p>
 * <ol>
 *   <li>ajoute l'en-tête {@code Authorization: Bearer <token>} si l'utilisateur
 *       est connecté (c'est ce que le JwtAuthenticationFilter côté Java attend) ;</li>
 *   <li>en cas de 401 ("token expiré/invalide"), déconnecte proprement et
 *       renvoie au /login — on ne reste pas coincé sur une page cassée.</li>
 * </ol>
 *
 * <p>C'est une "Functional Interceptor" : la forme moderne (une simple
 * fonction {@code HttpInterceptorFn} au lieu d'une classe), enregistrée
 * dans app.config.ts via provideHttpClient(withInterceptors([...])).</p>
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  // 1) Clone la requête en y ajoutant l'en-tête si un token existe.
  //    (clone() = copie immuable : on ne modifie jamais la requête d'origine.)
  const session = auth.currentUser();
  const request = session
    ? req.clone({ setHeaders: { Authorization: `Bearer ${session.token}` } })
    : req;

  // 2) Laisse partir la requête, mais surveille la réponse.
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        // Token absent, expiré ou falsifié → session nettoyée + retour au login.
        auth.logout();
        router.navigateByUrl('/login');
      }
      // On relance l'erreur : les composants pourront l'afficher à leur tour.
      return throwError(() => error);
    }),
  );
};
