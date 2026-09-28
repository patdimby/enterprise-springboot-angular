import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { firstValueFrom } from 'rxjs';
import { UsersApi } from './users-api';

/**
 * TESTS UNITAIRES du client HTTP UsersApi.
 * On vérifie que chaque méthode construit la BONNE requête (URL, méthode,
 * paramètres) — pas besoin de serveur : le HttpTestingController capture.
 */
describe('UsersApi', () => {
  let api: UsersApi;
  let controller: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([])), provideHttpClientTesting()],
    }).compileComponents();
    api = TestBed.inject(UsersApi);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  /** Fausse page d'utilisateurs réutilisée. */
  const fakePage = {
    content: [
      { id: 1, email: 'a@x.co', fullName: 'A', enabled: true, roles: ['ADMIN'], createdAt: '2026-01-01T00:00:00Z' },
    ],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 20,
  };

  it('list() appelle GET /api/users avec page et size en paramètres', async () => {
    const promise = firstValueFrom(api.list(2, 50));

    const req = controller.expectOne(r => r.url === '/api/users' && r.params.get('page') === '2'
      && r.params.get('size') === '50');
    expect(req.request.method).toBe('GET');
    req.flush(fakePage);

    const page = (await promise) as { content: { email: string }[]; totalElements: number };
    expect(page.content[0].email).toBe('a@x.co');
    expect(page.totalElements).toBe(1);
  });

  it('updateRoles() envoie PATCH avec le corps des rôles', async () => {
    const promise = firstValueFrom(api.updateRoles(5, { roles: ['MANAGER'] }));

    const req = controller.expectOne('/api/users/5/roles');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ roles: ['MANAGER'] });
    req.flush({ id: 5 });
    await promise;
  });

  it('updateEnabled() envoie PATCH avec le drapeau', async () => {
    const promise = firstValueFrom(api.updateEnabled(5, { enabled: false }));

    const req = controller.expectOne('/api/users/5/enabled');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ enabled: false });
    req.flush({ id: 5 });
    await promise;
  });

  it('delete() envoie DELETE /api/users/{id}', async () => {
    const promise = firstValueFrom(api.delete(9));

    const req = controller.expectOne('/api/users/9');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    await promise;
  });
});
