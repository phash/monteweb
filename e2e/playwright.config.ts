import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 0 : 0,
  workers: 1,
  reporter: [
    ['html', { open: 'never' }],
    ['list'],
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost',
    trace: process.env.CI ? 'off' : 'on-first-retry',
    screenshot: 'only-on-failure',
    video: process.env.CI ? 'off' : 'on-first-retry',
    locale: 'de-DE',
    timezoneId: 'Europe/Berlin',
  },
  projects: [
    {
      name: 'setup',
      testMatch: /global-setup\.ts/,
      teardown: 'teardown',
      timeout: 120000, // account provisioning may need retries
    },
    {
      name: 'teardown',
      testMatch: /global-teardown\.ts/,
    },
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        storageState: './auth-states/teacher.json',
      },
      dependencies: ['setup'],
    },
  ],
  expect: {
    timeout: 10000,
  },
  timeout: 15000,
})
