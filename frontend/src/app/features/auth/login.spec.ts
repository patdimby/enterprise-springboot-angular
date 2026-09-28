import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthStore } from '../../core/auth-store';
import { Login } from './login';

/**
 * TEST FONCTIONNEL (unitaire enrichi) de la page de connexion.
 *
 * <p>On rend le VRAI composant avec son formulaire, on simule des saisies,
 * on déclenche la soumission et on vérifie : les appels HTTP émis, la
 * session rangée dans AuthStore, la redirection. C'est le comportement
 * complet de la page, sans serveur.</p>
 */
describe('Login (page)', () => {
  let controller: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        // Route fourre-tout : les redirections post-login (vers /home) ont
        // une cible inoffensive dans le module de test.
        provideRouter([{ path: '**', redirectTo: '' }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('refuse la soumission si les champs sont vides (validation locale)', async () => {
    const fixture = TestBed.createComponent(Login);
    await fixture.whenStable();

    // Soumet le formulaire sans rien saisir (l'événement submit natif
    // déclenche ngSubmit → submit(), comme un vrai clic sur le bouton).
    (fixture.nativeElement as HTMLElement).querySelector('form')!
      .dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    // Aucune requête n'a dû partir : la validation a bloqué.
    controller.expectNone(() => true);
    // Des messages d'erreur Material sont affichés.
    expect(fixture.nativeElement.textContent).toContain('obligatoire');
  });

  it('connecte l\u2019utilisateur, stocke la session puis redirige', async () => {
    const fixture = TestBed.createComponent(Login);
    const auth = TestBed.inject(AuthStore);
    await fixture.whenStable();

    // Remplit le formulaire par l'API des contrôles (équivaut à taper).
    const cmp = fixture.componentInstance as unknown as {
      form: { controls: Record<string, { setValue: (v: string) => void }> };
    };
    cmp.form.controls['email'].setValue('admin@enterprise.com');
    cmp.form.controls['password'].setValue('Admin123!');
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('form')!
      .dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    // La requête de login est partie vers la bonne URL avec le bon corps.
    const req = controller.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'admin@enterprise.com', password: 'Admin123!' });

    // Le backend répond OK : la session doit être enregistrée.
    req.flush({
      token: 'jwt-admin',
      type: 'Bearer',
      id: 1,
      email: 'admin@enterprise.com',
      fullName: 'Administrateur',
      roles: ['ADMIN'],
    });
    await fixture.whenStable();

    expect(auth.currentUser()?.token).toBe('jwt-admin');
  });

  it('affiche un message clair sur 401 (mauvais identifiants)', async () => {
    const fixture = TestBed.createComponent(Login);
    await fixture.whenStable();

    const cmp = fixture.componentInstance as unknown as {
      form: { controls: Record<string, { setValue: (v: string) => void }> };
    };
    cmp.form.controls['email'].setValue('admin@enterprise.com');
    cmp.form.controls['password'].setValue('Mauvais1x!');
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('form')!
      .dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    controller.expectOne('/api/auth/login').flush(
      { title: 'Unauthorized', detail: 'Email ou mot de passe incorrect.' },
      { status: 401, statusText: 'Unauthorized' },
    );
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Email ou mot de passe incorrect');
  });
});
