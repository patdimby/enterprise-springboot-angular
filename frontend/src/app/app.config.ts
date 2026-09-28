import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth-interceptor';

/**
 * CONFIGURATION GLOBALE de l'application — la liste des "providers",
 * c'est-à-dire des services/outils mis à disposition de toute l'app.
 *
 * <ul>
 *   <li>{@code provideZonelessChangeDetection()} : la détection de changements
 *       SANS Zone.js, basée sur les signals — plus rapide, recommandée par
 *       Angular (défaut sur les nouveaux projets) ;</li>
 *   <li>{@code provideRouter(...)} : le routeur + transition animée entre
 *       pages + liaison des params d'URL vers les inputs de composant ;</li>
 *   <li>{@code provideHttpClient(withInterceptors(...))} : HttpClient avec
 *       notre intercepteur JWT ;</li>
 *   <li>{@code provideAnimationsAsync()} : animations Angular Material.</li>
 * </ul>
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes, withViewTransitions(), withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
  ],
};
