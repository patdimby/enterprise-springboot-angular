import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth-interceptor';

/**
 * Configuration GLOBALE : la liste des outils disponibles partout dans l'app.
 *
 * Vocabulaire :
 * - "provider" = un service enregistré une fois, réutilisable partout
 *   (ex. HttpClient pour appeler l'API Java).
 * - "zoneless" = Angular 16+ n'utilise plus Zone.js pour détecter les
 *   changements d'écran. Il s'appuie sur les signals (voir auth-store.ts).
 * - "intercepteur HTTP" = un filtre qui s'exécute sur chaque appel API
 *   (ici : coller le token JWT, voir auth-interceptor.ts).
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
