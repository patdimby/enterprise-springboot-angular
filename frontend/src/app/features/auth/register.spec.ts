import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Register } from './register';

/**
 * TEST FONCTIONNEL de la page d'inscription : validation des mots de passe,
 * appel register (+ 409 email pris), puis enchaînement automatique login.
 */
describe('Register (page)', () => {
  let controller: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [provideRouter([{ path: '**', redirectTo: '' }]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  /** Remplit le formulaire avec des données valides (ou des overrides). */
  async function fillAndSubmit(overrides: Record<string, string> = {}) {
    const fixture = TestBed.createComponent(Register);
    await fixture.whenStable();
    const cmp = fixture.componentInstance as unknown as {
      form: { controls: Record<string, { setValue: (v: string) => void }> };
    };
    const data = {
      fullName: 'Jane Doe',
      email: 'jane@example.com',
      password: 'Password123!',
      confirm: 'Password123!',
      ...overrides,
    };
    for (const [key, value] of Object.entries(data)) {
      cmp.form.controls[key].setValue(value);
    }
    fixture.detectChanges();
    (fixture.nativeElement as HTMLElement).querySelector('form')!
      .dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    return fixture;
  }

  it('bloque la soumission si les mots de passe diffèrent', async () => {
    await fillAndSubmit({ confirm: 'Autre1x!' });
    // Aucune requête : le validateur de groupe a bloqué.
    controller.expectNone(() => true);
  });

  it('envoie register puis login automatiquement', async () => {
    await fillAndSubmit();

    // 1) POST register d'abord.
    const registerReq = controller.expectOne('/api/auth/register');
    expect(registerReq.request.method).toBe('POST');
    expect(registerReq.request.body).toEqual({
      email: 'jane@example.com',
      password: 'Password123!',
      fullName: 'Jane Doe',
    });
    registerReq.flush({ id: 10, email: 'jane@example.com' });

    // 2) Puis POST login automatique.
    const loginReq = controller.expectOne('/api/auth/login');
    expect(loginReq.request.body).toEqual({ email: 'jane@example.com', password: 'Password123!' });
    loginReq.flush({
      token: 'jwt-new',
      type: 'Bearer',
      id: 10,
      email: 'jane@example.com',
      fullName: 'Jane Doe',
      roles: ['USER'],
    });
  });

  it('signale un email déjà pris (409)', async () => {
    const fixture = await fillAndSubmit();

    controller.expectOne('/api/auth/register').flush(
      { title: 'Conflict', detail: 'Email déjà utilisé.' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('déjà utilisé');
  });
});
