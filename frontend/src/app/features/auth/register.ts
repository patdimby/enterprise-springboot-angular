import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs/operators';

import { AuthApi } from '../../core/auth-api';
import { AuthStore } from '../../core/auth-store';

/**
 * Page d'INSCRIPTION.
 *
 * Le backend crée le compte (rôle USER) mais ne renvoie PAS de JWT.
 * Donc après register() on enchaîne avec login() : switchMap évite
 * d'imbriquer deux .subscribe() (anti-pattern RxJS : "callback hell").
 *
 * Le validateur passwordsMatch compare DEUX champs : il se pose sur le
 * GROUPE (tout le formulaire), pas sur un champ isolé.
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
  selector: 'app-register',
  styleUrl: './auth.scss',
  templateUrl: './register.html',
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly authApi = inject(AuthApi);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  protected readonly form = this.fb.nonNullable.group(
    {
      fullName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      confirm: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  protected readonly hidePassword = signal(true);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { fullName, email, password } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);

    this.authApi
      .register({ fullName, email, password })
      .pipe(switchMap(() => this.authApi.login({ email, password })))
      .subscribe({
        next: session => {
          this.auth.setSession(session);
          this.router.navigateByUrl('/home');
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          if (err.status === 409) {
            this.error.set('Cet email est déjà utilisé.');
          } else if (err.status === 400 && err.error?.errors) {
            const first = Object.values(err.error.errors)[0];
            this.error.set(String(first));
          } else if (err.status === 0) {
            this.error.set('Impossible de joindre le serveur.');
          } else {
            this.error.set(err.error?.detail ?? 'Erreur inattendue.');
          }
        },
      });
  }
}

/** null = OK. Un objet { passwordsMismatch: true } = erreur sur le groupe. */
function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirm')?.value;
  return password === confirm ? null : { passwordsMismatch: true };
}
