import { Injectable, computed, signal } from '@angular/core';

import { LoginResponse } from './models';

const STORAGE_KEY = 'enterprise.auth';

/**
 * Mémoire de la SESSION dans le navigateur.
 *
 * Pourquoi un @Injectable({ providedIn: 'root' }) ?
 * → Angular crée UN SEUL AuthStore pour toute l'app (singleton).
 *   Login, toolbar, gardes de routes lisent TOUS le même objet.
 *
 * Pourquoi un signal ?
 * → Quand currentUser change (login / logout), tous les écrans qui le
 *   lisent se mettent à jour tout seuls.
 *
 * Pourquoi localStorage ?
 * → F5 ne déconnecte pas. En production, un cookie httpOnly + refresh
 *   token serait plus sûr (le JWT ne serait plus lisible en JavaScript).
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  // Signal PRIVÉ : on ne le modifie que via setSession / logout.
  private readonly _user = signal<LoginResponse | null>(this.readFromStorage());

  /** Version LECTURE SEULE pour les templates et les gardes. */
  readonly currentUser = this._user.asReadonly();

  /** true si quelqu'un est connecté. Recalculé dès que _user change. */
  readonly isLoggedIn = computed(() => this._user() !== null);
  readonly roles = computed(() => this._user()?.roles ?? []);
  readonly isAdmin = computed(() => this.roles().includes('ADMIN'));

  /** Après un login réussi : on mémorise le token + le profil. */
  setSession(session: LoginResponse): void {
    this._user.set(session);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  /** Déconnexion : on efface le signal ET le disque du navigateur. */
  logout(): void {
    this._user.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  /** Au premier chargement : on tente de restaurer la session d'hier. */
  private readFromStorage(): LoginResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as LoginResponse) : null;
    } catch {
      // JSON cassé → on ignore, l'app démarre déconnectée au lieu de planter.
      return null;
    }
  }
}
