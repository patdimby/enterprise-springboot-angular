import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthStore } from '../../core/auth-store';
import { UserResponse } from '../../core/models';
import { Users } from './users';

/**
 * TEST FONCTIONNEL de la page d'administration des utilisateurs :
 * chargement de la liste, rendu du tableau, filtre local, actions
 * (activation/désactivation) — tout sans serveur, grâce au
 * HttpTestingController.
 */
describe('Users (page admin)', () => {
  let controller: HttpTestingController;

  /** Deux faux utilisateurs renvoyés par l'API. */
  const page = {
    content: [
      { id: 1, email: 'admin@x.co', fullName: 'Admin', enabled: true, roles: ['ADMIN'], createdAt: '2026-01-01T00:00:00Z' },
      { id: 2, email: 'bob@x.co', fullName: 'Bob', enabled: false, roles: ['USER'], createdAt: '2026-02-02T00:00:00Z' },
    ],
    totalElements: 2,
    totalPages: 1,
    number: 0,
    size: 10,
  };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Users],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // resetTestingModule remet le module de test à zéro (détruit les
    // composants créés, leurs injecteurs et abonnements) : sans ça, des
    // promises « traînardes » d'un test débordent sur le suivant.
    TestBed.resetTestingModule();
    controller.verify();
  });

  /** Crée la page et laisse la requête initiale aboutir. */
  async function renderPage() {
    // La page exige un ADMIN : on simule la session admin AVANT de créer
    // le composant (l'injection du store lit localStorage à l'instanciation).
    TestBed.inject(AuthStore).setSession({
      token: 'jwt-admin',
      type: 'Bearer',
      id: 1,
      email: 'admin@x.co',
      fullName: 'Admin',
      roles: ['ADMIN'],
    });
    const fixture = TestBed.createComponent(Users);
    // Détecte le premier rendu pour que la requête initiale parte.
    fixture.detectChanges();
    await fixture.whenStable();
    controller.expectOne(r => r.url === '/api/users' && r.params.get('page') === '0').flush(page);
    // Ré-affiche après réception des données (zoneless : le tick n'est pas
    // automatique hors d'un navigateur réel).
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }

  it('charge et affiche les utilisateurs dans le tableau', async () => {
    const fixture = await renderPage();
    const cmp = fixture.componentInstance as unknown as { users: () => UserResponse[] };

    // Le chargement a rempli le signal users (2 lignes)...
    expect(cmp.users().length).toBe(2);
    // ...et après un tick de plus, le DOM affiche les données.
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('admin@x.co');
    expect(fixture.nativeElement.textContent).toContain('bob@x.co');
  });

  it('envoie bien PATCH /enabled quand on bascule un compte', async () => {
    const fixture = await renderPage();
    const cmp = fixture.componentInstance as unknown as {
      toggleEnabled: (u: UserResponse) => Promise<void>;
      users: () => UserResponse[];
    };

    // Appel direct de l'action de la première ligne (admin@x.co, activé)
    // : plus robuste qu'un clic synthétique sur l'input Material interne.
    // La séquence HTTP de l'action est : PATCH /enabled, PUIS reload (GET).
    // On répond à chaque requête à son tour — sans quoi la promise de
    // l'action ne se termine jamais (deadlock de test).
    const action = cmp.toggleEnabled(cmp.users()[0]);
    fixture.detectChanges();

    // 1) Le PATCH part immédiatement : on y répond.
    controller.expectOne('/api/users/1/enabled')
      .flush({ id: 1, enabled: false }, { status: 200, statusText: 'OK' });

    // 2) Puis la liste est rechargée : on répond à ce 2e GET.
    await fixture.whenStable();
    controller.expectOne(r => r.url === '/api/users').flush(page);
    fixture.detectChanges();

    // 3) L'action est alors terminée, sans erreur.
    await action;
  });

  it('filtre les lignes localement (recherche)', async () => {
    const fixture = await renderPage();

    // Le champ recherche devient "bob" → seule la ligne de bob reste.
    const input = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('input[matinput]');
    input!.value = 'bob';
    input!.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('bob@x.co');
  });
});
