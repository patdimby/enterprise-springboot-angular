import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { firstValueFrom } from 'rxjs';

import { ROLE_NAMES, UserResponse } from '../../core/models';
import { UsersApi } from '../../core/users-api';

/**
 * Page ADMIN : tableau paginé des comptes.
 *
 * Pagination CÔTÉ SERVEUR : changer de page relance GET /api/users?page=&size=.
 * Le champ "Rechercher" filtre seulement la page déjà chargée (côté navigateur).
 *
 * firstValueFrom(observable) = "attends la 1re valeur puis arrête-toi".
 * Ça permet d'écrire async/await au lieu de .subscribe().
 */
@Component({
  imports: [
    DatePipe,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  selector: 'app-users',
  styleUrl: './users.scss',
  templateUrl: './users.html',
})
export class Users {
  private readonly api = inject(UsersApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  /** Ordre des colonnes = ordre d'affichage dans le tableau Material. */
  protected readonly displayedColumns = ['id', 'email', 'fullName', 'enabled', 'roles', 'createdAt', 'actions'];

  protected readonly users = signal<UserResponse[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly filter = signal('');

  protected readonly filtered = computed(() => {
    const needle = this.filter().trim().toLowerCase();
    if (!needle) {
      return this.users();
    }
    return this.users().filter(
      u => u.email.toLowerCase().includes(needle) || (u.fullName ?? '').toLowerCase().includes(needle),
    );
  });

  protected pageIndex = 0;
  // protected (pas private) : le template HTML y a accès.
  protected pageSize = 10;

  constructor() {
    void this.load();
  }

  protected async load(): Promise<void> {
    this.loading.set(true);
    try {
      const page = await firstValueFrom(this.api.list(this.pageIndex, this.pageSize));
      this.users.set(page.content);
      this.total.set(page.totalElements);
    } catch {
      this.snackBar.open('Impossible de charger les utilisateurs.', 'Fermer', { duration: 5000 });
    } finally {
      this.loading.set(false);
    }
  }

  protected onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    void this.load();
  }

  protected async toggleEnabled(user: UserResponse): Promise<void> {
    try {
      await firstValueFrom(this.api.updateEnabled(user.id, { enabled: !user.enabled }));
      this.snackBar.open(`Compte ${user.enabled ? 'désactivé' : 'activé'} : ${user.email}`, undefined, {
        duration: 3000,
      });
      await this.load();
    } catch {
      this.snackBar.open('Échec de la modification.', 'Fermer', { duration: 5000 });
    }
  }

  protected async editRoles(user: UserResponse): Promise<void> {
    const ref = this.dialog.open(RolesDialog, { data: user, width: '420px' });
    // afterClosed() émet undefined si on clique Annuler.
    const roles = (await firstValueFrom(ref.afterClosed())) as string[] | undefined;
    if (roles) {
      try {
        await firstValueFrom(this.api.updateRoles(user.id, { roles }));
        this.snackBar.open(`Rôles mis à jour : ${user.email}`, undefined, { duration: 3000 });
        await this.load();
      } catch {
        this.snackBar.open('Échec de la mise à jour des rôles.', 'Fermer', { duration: 5000 });
      }
    }
  }

  protected async remove(user: UserResponse): Promise<void> {
    const confirmed = confirm(`Supprimer définitivement le compte ${user.email} ?`);
    if (!confirmed) {
      return;
    }
    try {
      await firstValueFrom(this.api.delete(user.id));
      this.snackBar.open(`Compte supprimé : ${user.email}`, undefined, { duration: 3000 });
      await this.load();
    } catch {
      this.snackBar.open('Échec de la suppression.', 'Fermer', { duration: 5000 });
    }
  }
}

interface DialogDataUser {
  id: number;
  email: string;
  fullName: string;
  roles: string[];
}

/**
 * Petite fenêtre (dialog) pour cocher les rôles.
 * MAT_DIALOG_DATA = l'objet passé dans dialog.open(..., { data: user }).
 */
@Component({
  imports: [FormsModule, MatCheckboxModule, MatButtonModule, MatDialogModule, MatChipsModule],
  template: `
    <h2 mat-dialog-title>
      Rôles de {{ data.fullName }}
      <small class="muted">{{ data.email }}</small>
    </h2>

    <mat-dialog-content>
      <p class="muted">Cochez les rôles à attribuer (remplace les rôles actuels) :</p>
      @for (role of allRoles; track role) {
        <mat-checkbox [(ngModel)]="selected[role]">{{ role }}</mat-checkbox>
      }
      <div class="chips">
        @for (role of chosen(); track role) {
          <mat-chip>{{ role }}</mat-chip>
        }
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button matButton mat-dialog-close>Annuler</button>
      <button matButton="filled" [mat-dialog-close]="chosen()" cdkFocusInitial>Enregistrer</button>
    </mat-dialog-actions>
  `,
  styles: `
    .muted { opacity: 0.7; font-weight: 400; display: block; }
    .chips { margin-top: 1rem; display: flex; gap: 0.5rem; }
    mat-checkbox { display: block; margin: 0.35rem 0; }
  `,
})
class RolesDialog {
  readonly data = inject<DialogDataUser>(MAT_DIALOG_DATA);
  protected readonly allRoles = ROLE_NAMES;

  readonly selected: Record<string, boolean> = Object.fromEntries(
    ROLE_NAMES.map(role => [role, this.data.roles.includes(role)]),
  );

  readonly chosen = () => ROLE_NAMES.filter(role => this.selected[role]);
}
