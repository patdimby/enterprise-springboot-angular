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
 * Page de CONNEXION.
 *
 * Formulaire RÉACTIF (Reactive Forms) :
 * - on décrit les champs et les règles EN TypeScript (pas dans le HTML) ;
 * - le HTML est relié via [formGroup] et formControlName.
 *
 * Parcours :
 * 1. L'utilisateur clique sur "Se connecter".
 * 2. Si le formulaire est invalide, on marque les champs "touchés"
 *    pour afficher les messages d'erreur Material.
 * 3. Sinon POST /api/auth/login.
 * 4. Succès → AuthStore.setSession (token + profil) → on navigue vers
 *    returnUrl (la page demandée avant le login) ou /home.
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
  // ActivatedRoute = infos sur l'URL actuelle (ex. ?returnUrl=/users).
  private readonly route = inject(ActivatedRoute);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected readonly hidePassword = signal(true);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    // getRawValue() = { email, password } même si un champ est disabled.
    this.authApi.login(this.form.getRawValue()).subscribe({
      next: session => {
        this.auth.setSession(session);
        // Bonne pratique : plus aucun message d'erreur ni spinner résiduel
        // — on navigue et le composant est détruit par le routeur.
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/home';
        // Petit garde-fou : on refuse un returnUrl externe (open redirect).
        this.router.navigateByUrl(returnUrl.startsWith('/') ? returnUrl : '/home');
      },
      error: (err: HttpErrorResponse) => {
        // reset complet de l'état : sans ça, un second essai resterait
        // bloqué avec le spinner actif (bug classique des signaux).
        this.loading.set(false);
        this.error.set(this.humanMessage(err));
      },
    });
  }

  /** Transforme un code HTTP en phrase lisible. */
  private humanMessage(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'Impossible de joindre le serveur. Le backend est-il démarré ?';
    }
    if (err.status === 401) {
      return 'Email ou mot de passe incorrect.';
    }
    return err.error?.detail ?? 'Erreur inattendue. Réessayez.';
  }
}
