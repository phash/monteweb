import { type Page, type BrowserContext, expect } from '@playwright/test'
import { accounts, type TestAccount } from '../fixtures/test-accounts'

const BASE = 'http://localhost'

/**
 * Login via API and inject token into sessionStorage.
 * This is more reliable than UI login with PrimeVue components.
 */
export async function login(page: Page, account: TestAccount): Promise<void> {
  // Step 1: Get tokens via API (with retry for rate limiting)
  // Rate limit: 10 logins/minute per IP. Wait for window reset if needed.
  let token: string | undefined
  for (let attempt = 0; attempt < 5; attempt++) {
    const res = await page.request.post(`${BASE}/api/v1/auth/login`, {
      data: { email: account.email, password: account.password },
    })
    const body = await res.json()

    if (body.data?.accessToken) {
      token = body.data.accessToken
      break
    }

    // Rate limited — wait for rate limit window to expire (60s window)
    if (res.status() === 429 || JSON.stringify(body).includes('Too many')) {
      const waitMs = attempt < 2 ? 5000 : 15000
      await page.waitForTimeout(waitMs)
      continue
    }

    throw new Error(`API login failed for ${account.email}: ${body.message || JSON.stringify(body)}`)
  }

  if (!token) {
    throw new Error(`API login failed for ${account.email} after 5 retries (rate limited)`)
  }

  // Step 2: Navigate to the app (this loads the SPA)
  await page.goto('/')

  // Step 3: Inject token into sessionStorage (where the auth store reads from)
  await page.evaluate((accessToken) => {
    sessionStorage.setItem('accessToken', accessToken)
  }, token)

  // Step 4: Reload so the app picks up the token from sessionStorage
  await page.reload()

  // Step 5: Wait for the app to load with authenticated state
  await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 15000 })
}

/**
 * Login via UI form (fallback for tests that specifically test the login UI).
 */
export async function loginViaUI(page: Page, account: TestAccount): Promise<void> {
  await page.goto('/')

  // Wait for login page to load
  await page.waitForSelector('input[type="email"], input[type="text"][name="email"], #email', { timeout: 10000 })

  // Fill email
  const emailInput = page.locator('input[type="email"], input[type="text"][name="email"], #email').first()
  await emailInput.fill(account.email)

  // Fill password — use the PrimeVue Password input
  const passwordInput = page.locator('input[type="password"]').first()
  await passwordInput.fill(account.password)

  // Click login button
  const loginButton = page.locator('button[type="submit"], button:has-text("Anmelden")').first()
  await loginButton.click()

  // Wait for navigation away from login page
  await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 15000 })
}

/**
 * Login and save storage state to a file for reuse.
 */
export async function loginAndSaveState(
  page: Page,
  context: BrowserContext,
  account: TestAccount,
  statePath: string
): Promise<void> {
  await login(page, account)
  await context.storageState({ path: statePath })
}

/**
 * Login as a specific role by key name.
 */
export async function loginAs(page: Page, role: keyof typeof accounts): Promise<void> {
  await login(page, accounts[role])
}

/**
 * Logout via UI.
 */
export async function logout(page: Page): Promise<void> {
  // Try clicking user menu / avatar / logout button
  const userMenu = page.locator('[data-testid="user-menu"], .user-menu, .avatar-menu').first()
  if (await userMenu.isVisible({ timeout: 3000 }).catch(() => false)) {
    await userMenu.click()
  }
  const logoutButton = page.locator('button:has-text("Abmelden"), a:has-text("Abmelden"), [data-testid="logout"]').first()
  await logoutButton.click()
  await page.waitForURL('**/', { timeout: 10000 })
}

/**
 * Check that we are on the dashboard (logged in).
 */
export async function expectDashboard(page: Page): Promise<void> {
  await expect(page.locator('.dashboard, [data-testid="dashboard"], main')).toBeVisible({ timeout: 10000 })
}
