import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterOutlet } from '@angular/router';
import { map } from 'rxjs/operators';

import { AuthStore } from './core/auth-store';

/**
 * COMPOSANT RACINE : la "coquille" autour de toutes les pages.
 *
 * Il affiche :
 * 1. la barre du haut (toolbar) ;
 * 2. le menu de gauche (sidenav) ;
 * 3. au centre, un <router-outlet> : Angular y place la page de l'URL actuelle.
 *
 * Vocabulaire d'un composant Angular :
 * - @Component({...}) = "ceci est un morceau d'écran" (template HTML + logique TS).
 * - selector: 'app-root' = le nom de la balise dans index.html.
 * - inject(Service) = "donne-moi le service partagé" (injection de dépendances).
 * - signal() = une petite boîte de valeur. Quand on change la valeur, l'écran
 *   se met à jour tout seul.
 * - computed() = une valeur CALCULÉE à partir d'autres signals.
 */
@Component({
  imports: [
    RouterOutlet,
    RouterLink,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatTooltipModule,
  ],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  // inject() remplace l'ancien constructeur(private auth: AuthStore).
  // Angular nous donne l'UNIQUE instance AuthStore de toute l'application.
  private readonly auth = inject(AuthStore);
  private readonly breakpoints = inject(BreakpointObserver);

  /** Utilisateur connecté, ou null si personne n'est loggé. */
  protected readonly user = this.auth.currentUser;
  protected readonly isAdmin = this.auth.isAdmin;

  /**
   * Vrai sur un écran étroit (téléphone).
   * toSignal() transforme un Observable RxJS en signal Angular.
   */
  protected readonly isHandset = toSignal(
    this.breakpoints.observe(Breakpoints.Handset).pipe(map(state => state.matches)),
    { initialValue: false },
  );

  /** Menu latéral ouvert ou fermé. */
  protected readonly sidenavOpen = signal(true);

  /** Thème sombre : on relit le choix précédent dans localStorage. */
  protected readonly darkMode = signal<boolean>(localStorage.getItem('enterprise.dark') === 'true');

  /** Initiales affichées (ex. "Jean Dupont" → "JD"). */
  protected readonly initials = computed(() => {
    const name = this.user()?.fullName ?? '';
    return (
      name
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((part: string) => part[0]?.toUpperCase())
        .join('') || '?'
    );
  });

  constructor() {
    // Au démarrage, on applique le thème déjà choisi (sinon le body reste clair).
    document.body.classList.toggle('dark-mode', this.darkMode());
  }

  /** Bascule clair / sombre et mémorise le choix. */
  protected toggleDarkMode(): void {
    const next = !this.darkMode();
    this.darkMode.set(next);
    localStorage.setItem('enterprise.dark', String(next));
    document.body.classList.toggle('dark-mode', next);
  }

  /** Vide la session puis le template renvoie vers l'accueil via les liens. */
  protected logout(): void {
    this.auth.logout();
  }

  /** Sur téléphone, on ferme le menu après un clic sur un lien. */
  protected onNavNavigate(): void {
    if (this.isHandset()) {
      this.sidenavOpen.set(false);
    }
  }
}
