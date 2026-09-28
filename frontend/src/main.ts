/**
 * POINT D'ENTRÉE de l'application Angular.
 *
 * Le navigateur charge d'abord index.html, qui contient <app-root>.
 * Ce fichier démarre ensuite Angular et "branche" le composant racine App
 * dans cette balise, avec la configuration globale (app.config.ts).
 *
 * Analogie : c'est le bouton "ON" de l'application.
 */
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig).catch(err => console.error(err));
