import { AuthStore } from './auth-store';

/**
 * TESTS UNITAIRES du magasin d'état d'authentification.
 * Aucune dépendance externe : on teste les signals et localStorage.
 */
describe('AuthStore', () => {
  /** Session d'exemple réutilisée par les tests. */
  const session = {
    token: 'jwt-test',
    type: 'Bearer',
    id: 7,
    email: 'jane@example.com',
    fullName: 'Jane Doe',
    roles: ['USER'],
  };

  beforeEach(() => localStorage.clear());

  it('démarre déconnecté quand localStorage est vide', () => {
    const store = new AuthStore();
    expect(store.currentUser()).toBeNull();
    expect(store.isLoggedIn()).toBe(false);
  });

  it('setSession expose l\u2019utilisateur et persiste dans localStorage', () => {
    const store = new AuthStore();
    store.setSession(session);

    expect(store.isLoggedIn()).toBe(true);
    expect(store.currentUser()?.email).toBe('jane@example.com');
    // Le JSON a bien été écrit (clé enterprise.auth).
    expect(localStorage.getItem('enterprise.auth')).toContain('jane@example.com');
  });

  it('roles() et isAdmin() dérivent correctement des rôles', () => {
    const store = new AuthStore();
    store.setSession({ ...session, roles: ['ADMIN'] });
    expect(store.roles()).toEqual(['ADMIN']);
    expect(store.isAdmin()).toBe(true);

    store.setSession({ ...session, roles: ['USER'] });
    expect(store.isAdmin()).toBe(false);
  });

  it('logout vide le signal ET localStorage', () => {
    const store = new AuthStore();
    store.setSession(session);
    store.logout();

    expect(store.currentUser()).toBeNull();
    expect(localStorage.getItem('enterprise.auth')).toBeNull();
  });

  it('restaure la session d\u2019une exécution précédente', () => {
    // Simule une page précédente qui avait rangé la session.
    localStorage.setItem('enterprise.auth', JSON.stringify(session));
    const store = new AuthStore();

    expect(store.isLoggedIn()).toBe(true);
    expect(store.currentUser()?.id).toBe(7);
  });

  it('tolère un localStorage corrompu (repart déconnecté)', () => {
    localStorage.setItem('enterprise.auth', '{pas-du-json');
    const store = new AuthStore();

    expect(store.currentUser()).toBeNull();
  });
});
