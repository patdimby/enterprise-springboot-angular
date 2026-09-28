import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { authInterceptor } from './auth-interceptor';
import { AuthStore } from './auth-store';

/**
 * TESTS UNITAIRES de l'intercepteur HTTP JWT.
 *
 * <p>provideHttpClientTesting remplace le transport réseau : on émet des
 * requêtes via HttpClient, puis on les inspecte avec HttpTestingController
 * ("expectOne") — on vérifie l'en-tête ajouté et le comportement sur 401.</p>
 */
describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let auth: AuthStore;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      providers: [
        // Route fourre-tout : la redirection /login de l'intercepteur
        // (après un 401) a une cible inoffensive dans le module de test.
        provideRouter([{ path: '**', redirectTo: '' }]),
        // On teste le VRAI intercepteur, branché sur le faux transport :
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthStore);
  });

  afterEach(() => {
    // Aucune requête en attente ne doit survivre à un test.
    controller.verify();
  });

  it('ajoute Authorization: Bearer <token> quand une session existe', () => {
    auth.setSession({
      token: 'jwt-valide',
      type: 'Bearer',
      id: 1,
      email: 'jane@example.com',
      fullName: 'Jane',
      roles: ['USER'],
    });

    let result: string | undefined;
    http.get('/api/auth/me').subscribe(body => (result = (body as { email: string }).email));

    const req = controller.expectOne('/api/auth/me');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-valide');

    req.flush({ email: 'jane@example.com' });
    expect(result).toBe('jane@example.com');
  });

  it('n\u2019ajoute pas d\u2019en-tête quand on est déconnecté', () => {
    http.get('/api/auth/login').subscribe();

    const req = controller.expectOne('/api/auth/login');
    expect(req.request.headers.get('Authorization')).toBeNull();

    req.flush({});
  });

  it('déconnecte et redirige vers /login après un 401', () => {
    auth.setSession({
      token: 'jwt-expiré',
      type: 'Bearer',
      id: 1,
      email: 'jane@example.com',
      fullName: 'Jane',
      roles: ['USER'],
    });

    const errors: HttpErrorResponse[] = [];
    http.get('/api/users').subscribe({ error: err => errors.push(err as HttpErrorResponse) });

    controller.expectOne('/api/users').flush({ title: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    // L'erreur 401 est bien relancée au composant…
    expect(errors.length).toBe(1);
    expect(errors[0].status).toBe(401);
    // …et la session a été vidée par l'intercepteur.
    expect(auth.currentUser()).toBeNull();
  });

  it('laisse passer les autres erreurs (403, 500) sans déconnexion', () => {
    auth.setSession({
      token: 'jwt-user',
      type: 'Bearer',
      id: 1,
      email: 'jane@example.com',
      fullName: 'Jane',
      roles: ['USER'],
    });

    const errors: HttpErrorResponse[] = [];
    http.get('/api/users').subscribe({ error: err => errors.push(err as HttpErrorResponse) });

    controller.expectOne('/api/users').flush({}, { status: 403, statusText: 'Forbidden' });

    expect(errors[0].status).toBe(403);
    // Toujours connecté : le 403 n'est pas un problème de token.
    expect(auth.currentUser()).not.toBeNull();
  });
});
