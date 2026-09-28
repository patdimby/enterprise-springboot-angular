import { Routes } from '@angular/router';

import { adminGuard, authGuard } from './core/auth-guards';

/**
 * Table des ROUTES = quelle page afficher pour quelle URL.
 *
 * Exemples :
 *   /home      → composant Home
 *   /login     → composant Login
 *   /users     → composant Users (seulement si connecté ET admin)
 *
 * Vocabulaire :
 * - "loadComponent" = chargement PARESSEUX (lazy) : le fichier de la page
 *   n'est téléchargé que quand on visite l'URL. L'app démarre plus vite.
 * - "canActivate" = GARDE : une fonction qui dit oui/non avant d'ouvrir la page.
 * - "title" = texte de l'onglet du navigateur.
 * - path: '**' = "n'importe quelle autre URL" (filet 404) → on renvoie à l'accueil.
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
  { path: '**', redirectTo: 'home' },
];
