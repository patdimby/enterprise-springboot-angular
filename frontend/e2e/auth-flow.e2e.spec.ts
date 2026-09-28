import { expect, test, type Page } from '@playwright/test';

/**
 * TESTS FONCTIONNELS E2E — un VRAI navigateur (Chromium) piloté par
 * Playwright, face au VRAI frontend. Les appels HTTP vers /api/** sont
 * INTERCEPTÉS (page.route) et répondus avec des données de test : on teste
 * donc le parcours complet de l'interface sans backend allumé.
 *
 * Lancement : npm run e2e   (démarre ng serve puis Playwright)
 */

/** Session renvoyée par un faux POST /api/auth/login. */
const fakeSession = {
  token: 'jwt-e2e',
  type: 'Bearer',
  id: 1,
  email: 'admin@enterprise.com',
  fullName: 'Admin E2E',
  roles: ['ADMIN'],
};

/** Branche les interceptions HTTP pour toute la session de test. */
async function mockApi(page: Page): Promise<void> {
  await page.route('**/api/auth/login', route =>
    route.fulfill({ json: fakeSession }));
  await page.route('**/api/auth/me', route =>
    route.fulfill({ json: { id: 1, email: fakeSession.email, fullName: fakeSession.fullName, enabled: true, roles: ['ADMIN'], createdAt: '2026-01-01T00:00:00Z' } }));
  await page.route('**/actuator/health', route =>
    route.fulfill({ json: { status: 'UP' } }));
  await page.route('**/api/users**', route =>
    route.fulfill({ json: {
      content: [
        { id: 1, email: 'admin@enterprise.com', fullName: 'Admin E2E', enabled: true, roles: ['ADMIN'], createdAt: '2026-01-01T00:00:00Z' },
        { id: 2, email: 'bob@enterprise.com', fullName: 'Bob Martin', enabled: false, roles: ['USER'], createdAt: '2026-02-02T00:00:00Z' },
      ],
      totalElements: 2, totalPages: 1, number: 0, size: 10,
    } }));
}

test.describe('Parcours Connexion', () => {
  test.beforeEach(async ({ page }) => {
    await mockApi(page);
  });

  test('le formulaire de connexion est utilisable sur mobile comme sur desktop', async ({ page }) => {
    await page.goto('/login');

    // Champs présents et actifs.
    await expect(page.getByLabel(/email/i)).toBeVisible();
    await expect(page.getByLabel(/mot de passe/i)).toBeVisible();

    // Simulation "petit écran" : la carte reste visible et utilisable.
    await page.setViewportSize({ width: 375, height: 667 });
    await expect(page.getByRole('button', { name: /se connecter/i })).toBeVisible();
  });

  test('un login réussi mène à l\u2019accueil avec le nom de l\u2019utilisateur', async ({ page }) => {
    await page.goto('/login');

    await page.getByLabel(/email/i).fill('admin@enterprise.com');
    await page.getByLabel(/mot de passe/i).fill('Admin123!');
    await page.getByRole('button', { name: /se connecter/i }).click();

    // Redirection vers l'accueil et session visible.
    await expect(page).toHaveURL(/home/);
    await expect(page.getByText('Admin E2E')).toBeVisible();
  });

  test('la page admin affiche le tableau des utilisateurs', async ({ page }) => {
    // On injecte directement la session : la page /users est testée,
    // pas le login (déjà couvert ci-dessus).
    await page.addInitScript(() => {
      localStorage.setItem('enterprise.auth', JSON.stringify({
        token: 'jwt-e2e', type: 'Bearer', id: 1,
        email: 'admin@enterprise.com', fullName: 'Admin E2E', roles: ['ADMIN'],
      }));
    });

    await page.goto('/users');
    await expect(page.getByRole('table')).toBeVisible();
    await expect(page.getByText('bob@enterprise.com')).toBeVisible();
    await expect(page.getByText('ADMIN')).toBeVisible();
  });

  test('un utilisateur sans rôle ADMIN est redirigé vers Accès refusé', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('enterprise.auth', JSON.stringify({
        token: 'jwt-user', type: 'Bearer', id: 2,
        email: 'bob@enterprise.com', fullName: 'Bob Martin', roles: ['USER'],
      }));
    });

    await page.goto('/users');
    await expect(page.getByText('Accès refusé')).toBeVisible();
  });
});
