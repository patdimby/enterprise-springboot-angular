import { Routes } from '@angular/router';

import { adminGuard, authGuard } from './core/auth-guards';

/**
 * TABLE DES ROUTES — quel composant afficher pour quelle URL.
 *
 * <p>Les gardes (guards) protègent les pages réservées ; le "title" est
 * affiché dans l'onglet du navigateur ; la page admin est chargée en LAZY
 * (loadComponent) : son code n'est téléchargé que si on la visite.</p>
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  {
    path: 'home',
    title: 'Accueil — Enterprise',
    loadComponent: () => import('./features/home/home').then(m => m.Home),
  },
  {
    path: 'login',
    title: 'Connexion — Enterprise',
    loadComponent: () => import('./features/auth/login').then(m => m.Login),
  },
  {
    path: 'register',
    title: 'Inscription — Enterprise',
    loadComponent: () => import('./features/auth/register').then(m => m.Register),
  },
  {
    path: 'users',
    title: 'Utilisateurs — Enterprise',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./features/users/users').then(m => m.Users),
  },
  {
    path: 'forbidden',
    title: 'Accès refusé — Enterprise',
    loadComponent: () => import('./features/forbidden/forbidden').then(m => m.Forbidden),
  },
  // Route fourre-tout : URL inconnue → page 404.
  { path: '**', redirectTo: 'home' },
];
