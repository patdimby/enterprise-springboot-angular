import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterOutlet } from '@angular/router';

import { AuthStore } from './core/auth-store';

/**
 * COMPOSANT RACINE — la "coquille" de l'application :
 * une barre supérieure (toolbar), un menu latéral (sidenav) et la zone
 * centrale où le routeur affiche la page courante (router-outlet).
 *
 * <p>Responsive : sur mobile (Breakpoints.Small), le sidenav passe en mode
 * "over" (il recouvre le contenu et se ferme après un clic) ; sur desktop,
 * il reste ouvert à côté du contenu (mode "side").</p>
 *
 * <p>Bouton lune/soleil : bascule le thème sombre en ajoutant la classe
 * "dark-mode" sur &lt;body&gt; (voir styles.scss).</p>
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
  private readonly auth = inject(AuthStore);

  /** Utilisateur connecté (ou null) — réactif via AuthStore. */
  protected readonly user = this.auth.currentUser;
  protected readonly isAdmin = this.auth.isAdmin;

  /** Vrai sur un écran étroit (< 960 px) : pilote le mode du sidenav. */
  protected readonly isHandset = signal(false);

  /** État d'ouverture du menu latéral. */
  protected readonly sidenavOpen = signal(true);

  /** Thème sombre actif ? Persisté dans localStorage pour rester entre visites. */
  protected readonly darkMode = signal<boolean>(localStorage.getItem('enterprise.dark') === 'true');

  /** Initiales affichées dans l'avatar de la toolbar (ex. "JD"). */
  protected readonly initials = computed(() => {
    const name = this.user()?.fullName ?? '';
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part: string) => part[0]?.toUpperCase())
      .join('') || '?';
  });

  /** Bascule clair/sombre et mémorise le choix. */
  protected toggleDarkMode(): void {
    const next = !this.darkMode();
    this.darkMode.set(next);
    localStorage.setItem('enterprise.dark', String(next));
    document.body.classList.toggle('dark-mode', next);
  }

  /** Déconnexion puis retour à l'accueil (le template appelle ceci). */
  protected logout(): void {
    this.auth.logout();
  }

  /** Ferme le sidenav sur mobile après une navigation. */
  protected onNavNavigate(): void {
    if (this.isHandset()) {
      this.sidenavOpen.set(false);
    }
  }
}
