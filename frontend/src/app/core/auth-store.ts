import { Injectable, computed, signal } from '@angular/core';

import { LoginResponse } from './models';

const STORAGE_KEY = 'enterprise.auth';

/**
 * ÉTAT D'AUTHENTIFICATION — le cœur de la session côté navigateur.
 *
 * <p><b>Pourquoi des signals ?</b> C'est le système d'état réactif d'Angular
 * moderne : quand {@link currentUser} change, tous les composants qui le
 * lisent se redessinent tout seuls (et l'app est "zoneless", donc c'est LA
 * façon de déclencher le change detection).</p>
 *
 * <p><b>Pourquoi localStorage ?</b> Pour rester connecté après un
 * rafraîchissement de page (F5). Simple et suffisant pour ce projet ;
 * en production on préférerait des cookies httpOnly + refresh tokens.</p>
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  /** L'utilisateur connecté (ou null). Signal privé : on le modifie via les méthodes. */
  private readonly _user = signal<LoginResponse | null>(this.readFromStorage());

  /** Lecture publique réactive : les templates et guards l'utilisent. */
  readonly currentUser = this._user.asReadonly();

  /** Valeurs dérivées, recalculées automatiquement quand _user change. */
  readonly isLoggedIn = computed(() => this._user() !== null);
  readonly roles = computed(() => this._user()?.roles ?? []);
  readonly isAdmin = computed(() => this.roles().includes('ADMIN'));

  /** Mémorise la session (signal + localStorage). */
  setSession(session: LoginResponse): void {
    this._user.set(session);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  /** Déconnexion : on efface partout. */
  logout(): void {
    this._user.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  /** Au démarrage du service : restaure la session d'une exécution précédente. */
  private readFromStorage(): LoginResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as LoginResponse) : null;
    } catch {
      return null; // JSON corrompu → on repart de zéro, sans planter l'app.
    }
  }
}
