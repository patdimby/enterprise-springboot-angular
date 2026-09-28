import { HttpClient } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { interval } from 'rxjs';
import { startWith } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { AuthStore } from '../../core/auth-store';

/**
 * Page d'ACCUEIL : session + "est-ce que le backend Java répond ?".
 *
 * interval(30_000) = un Observable qui "tic" toutes les 30 secondes.
 * startWith(0) = on tic tout de suite au chargement (pas attendre 30 s).
 * takeUntilDestroyed() = quand on quitte la page, l'abonnement s'arrête
 * automatiquement (sinon fuite mémoire : le timer continuerait en coulisse).
 */
@Component({
  imports: [DatePipe, MatCardModule, MatChipsModule, MatIconModule],
  selector: 'app-home',
  styleUrl: './home.scss',
  templateUrl: './home.html',
})
export class Home {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthStore);

  protected readonly user = this.auth.currentUser;
  protected readonly roles = this.auth.roles;

  protected readonly lastCheck = signal<Date | null>(null);
  protected readonly healthStatus = signal<'UP' | 'DOWN' | 'CHECKING'>('CHECKING');
  protected readonly healthClass = computed(() =>
    this.healthStatus() === 'UP' ? 'dot-ok' : this.healthStatus() === 'DOWN' ? 'dot-ko' : 'dot-wait',
  );

  constructor() {
    interval(30_000)
      .pipe(startWith(0), takeUntilDestroyed())
      .subscribe(() => this.checkHealth());
  }

  /**
   * GET /actuator/health (route publique Spring Boot Actuator).
   * environment.apiUrl vaut "/api" → on retire "/api" pour viser le root.
   */
  protected checkHealth(): void {
    this.healthStatus.set('CHECKING');
    this.http
      .get<{ status: string }>(`${environment.apiUrl.replace('/api', '')}/actuator/health`)
      .pipe(takeUntilDestroyed(this.destroyRef))
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
