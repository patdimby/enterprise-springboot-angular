import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';

import { adminGuard, authGuard } from './auth-guards';
import { AuthStore } from './auth-store';

/**
 * TESTS UNITAIRES des gardes de route.
 * On fabrique de fausses snapshots de route (simples objets) et on vérifie
 * que la garde renvoie true ou une UrlTree de redirection.
 */
describe('Guards de route', () => {
  /** Session utilitaire. */
  const login = (roles: string[]) =>
    TestBed.inject(AuthStore).setSession({
      token: 'jwt-test',
      type: 'Bearer',
      id: 1,
      email: 'jane@example.com',
      fullName: 'Jane Doe',
      roles,
    });

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'login', redirectTo: '' }, { path: 'forbidden', redirectTo: '' }])],
    }).compileComponents();
  });

  /** Fausses snapshots : suffisant pour les gardes. */
  const route = {} as ActivatedRouteSnapshot;
  const state = (url: string) => ({ url }) as RouterStateSnapshot;

  it('authGuard laisse passer un utilisateur connecté', () => {
    login(['USER']);
    const result = TestBed.runInInjectionContext(() => authGuard(route, state('/users')));
    expect(result).toBe(true);
  });

  it('authGuard redirige un visiteur vers /login avec returnUrl', () => {
    const result = TestBed.runInInjectionContext(() => authGuard(route, state('/users'))) as UrlTree;
    expect(result.toString()).toContain('login');
    expect(result.toString()).toContain('returnUrl');
  });

  it('adminGuard laisse passer un ADMIN', () => {
    login(['ADMIN']);
    const result = TestBed.runInInjectionContext(() => adminGuard(route, state('/users')));
    expect(result).toBe(true);
  });

  it('adminGuard envoie un simple USER vers /forbidden', () => {
    login(['USER']);
    const result = TestBed.runInInjectionContext(() => adminGuard(route, state('/users'))) as UrlTree;
    expect(result.toString()).toContain('forbidden');
  });
});
