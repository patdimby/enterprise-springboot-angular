import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { startWith } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

import { AuthStore } from '../../core/auth-store';

/**
 * PAGE D'ACCUEIL — deux cartes :
 * <ul>
 *   <li>état de la session (connecté ou non, rôles) ;</li>
 *   <li>sonde de santé de l'API : un GET /actuator/health est envoyé
 *       périodiquement ; le rond vert/rouge montre l'état du backend.</li>
 * </ul>
 * C'est aussi une démonstration des "resource signals" : le résultat du
 * HTTP devient un signal automatiquement géré (loading / valeur / erreur).
 */
@Component({
  imports: [DatePipe, MatCardModule, MatChipsModule, MatIconModule],
  selector: 'app-home',
  styleUrl: './home.scss',
  templateUrl: './home.html',
})
export class Home {
  private readonly http = inject(HttpClient);
  protected readonly auth = inject(AuthStore);

  protected readonly user = this.auth.currentUser;
  protected readonly roles = this.auth.roles;

  /** Date de la dernière vérification de santé (pour l'afficher). */
  protected readonly lastCheck = signal<Date | null>(null);

  /** Statut renvoyé par /actuator/health : "UP", "DOWN" ou erreur. */
  protected readonly healthStatus = signal<'UP' | 'DOWN' | 'CHECKING'>('CHECKING');

  /** Couleur du point de statut (classe CSS calculée). */
  protected readonly healthClass = computed(() =>
    this.healthStatus() === 'UP' ? 'dot-ok' : this.healthStatus() === 'DOWN' ? 'dot-ko' : 'dot-wait',
  );

  constructor() {
    // Re-vérifie la santé toutes les 30 secondes. On utilise l'observable
    // interval de RxJS + takeUntilDestroyed : l'abonnement est AUTOMATIQUEMENT
    // résilié quand la page est détruite (pas de fuite mémoire, et les tests
    // se terminent proprement — un setInterval natif garderait le worker vivant).
    interval(30_000)
      .pipe(startWith(0), takeUntilDestroyed())
      .subscribe(() => this.checkHealth());
  }

  /** Interroge la sonde de santé publique du backend. */
  protected checkHealth(): void {
    this.healthStatus.set('CHECKING');
    this.http.get<{ status: string }>(`${environment.apiUrl.replace('/api', '')}/actuator/health`)
      .subscribe({
        next: res => {
          this.healthStatus.set(res.status === 'UP' ? 'UP' : 'DOWN');
          this.lastCheck.set(new Date());
        },
        error: () => {
          this.healthStatus.set('DOWN');
          this.lastCheck.set(new Date());
        },
      });
  }
}
