import { defineConfig, devices } from '@playwright/test';

/**
 * Configuration Playwright pour les tests fonctionnels e2e.
 *
 * `webServer` démarre automatiquement `ng serve` avant les tests et attend
 * que http://localhost:4200 réponde : `npm run e2e` suffit, aucun serveur
 * manuel à lancer. Les appels /api sont interceptés dans les tests.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: {
    command: 'npm run start -- --port 4200',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
