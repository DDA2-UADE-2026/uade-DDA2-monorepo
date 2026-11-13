import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  workers: 1,
  retries: 0,
  timeout: 90_000,
  globalTimeout: 12 * 60_000,
  expect: { timeout: 15_000 },
  metadata: { scope: 'five-real-backend-browser-journeys', commit: process.env.GITHUB_SHA || 'local-working-tree' },
  reporter: [['list'], ['html', { open: 'never' }], ['junit', { outputFile: 'test-results/e2e-junit.xml' }]],
  use: {
    baseURL: 'http://localhost:15173',
    timezoneId: 'America/Argentina/Buenos_Aires',
    actionTimeout: 20_000,
    trace: 'retain-on-failure', screenshot: 'only-on-failure', video: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    { command: 'node ../scripts/testing/start-e2e-backend.mjs', url: 'http://localhost:18080/actuator/health', reuseExistingServer: false, timeout: 180_000 },
    { command: 'npm run dev -- --host 127.0.0.1 --port 15173 --strictPort', url: 'http://localhost:15173', reuseExistingServer: false, timeout: 60_000, env: { VITE_SERVER_URL: 'http://localhost:18080', VITE_CLIENT_URL: 'http://localhost:15173' } },
  ],
})
