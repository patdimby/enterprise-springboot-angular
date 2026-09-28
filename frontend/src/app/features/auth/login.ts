import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthApi } from '../../core/auth-api';
import { AuthStore } from '../../core/auth-store';

/**
 * PAGE DE CONNEXION — formulaire réactif (ReactiveForms) + Material.
 *
 * <p>Flux : validation locale (email bien formé, champ non vide) → appel
 * POST /api/auth/login via AuthApi → si OK, la session (token + profil)
 * est rangée dans AuthStore (signal + localStorage) → redirection vers la
 * page demandée avant le login (returnUrl) ou l'accueil.</p>
 *
 * <p>Les erreurs du backend (401 mauvais mot de passe, réseau indisponible…)
 * sont affichées sous le bouton, jamais de blocage silencieux.</p>
 */
@Component({
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  selector: 'app-login',
  styleUrl: './auth.scss',
  templateUrl: './login.html',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly authApi = inject(AuthApi);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  /** Formulaire : email + mot de passe, avec règles de validation. */
  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  /** Masque/affiche le mot de passe (icône œil). */
  protected readonly hidePassword = signal(true);

  /** Vrai pendant l'appel HTTP → le bouton affiche un spinner. */
  protected readonly loading = signal(false);

  /** Message d'erreur à afficher (401, réseau…). */
  protected readonly error = signal<string | null>(null);

  protected submit(): void {
    // Formulaire invalide → on montre les erreurs de champ et on s'arrête.
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.authApi.login(this.form.getRawValue()).subscribe({
      next: session => {
        this.auth.setSession(session); // mémorise le token + le profil
        // returnUrl = la page qu'on voulait visiter avant d'être renvoyé ici.
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/home';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(this.humanMessage(err));
      },
    });
  }

  /** Traduit l'erreur HTTP en phrase compréhensible pour l'utilisateur. */
  private humanMessage(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'Impossible de joindre le serveur. Le backend est-il démarré ?';
    }
    if (err.status === 401) {
      return 'Email ou mot de passe incorrect.';
    }
    // Le backend renvoie du RFC 7807 avec un champ "detail".
    return err.error?.detail ?? 'Erreur inattendue. Réessayez.';
  }
}
