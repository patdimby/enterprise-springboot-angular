import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

/**
 * Page "accès refusé".
 * adminGuard envoie ici un utilisateur connecté qui n'est pas ADMIN.
 * Le template est INLINE (dans le .ts) car la page est très petite.
 */
@Component({
  imports: [RouterLink, MatCardModule, MatIconModule],
  selector: 'app-forbidden',
  template: `
    <div class="auth-wrap">
      <mat-card class="auth-card center">
        <mat-card-content>
          <mat-icon class="big">lock</mat-icon>
          <h2>Accès refusé</h2>
          <p>Votre compte n'a pas les droits nécessaires pour cette page.</p>
          <a matButton routerLink="/home">Retour à l'accueil</a>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: `
    .center { text-align: center; padding: 2rem 1rem; }
    .big { font-size: 48px; width: 48px; height: 48px; }
    h2 { margin: 0.5rem 0; }
  `,
})
export class Forbidden {}
