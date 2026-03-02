import { type Page, type BrowserContext, expect } from '@playwright/test'
import { accounts, type TestAccount } from '../fixtures/test-accounts'

/**
 * Login via UI and return the page.
 */
export async function login(page: Page, account: TestAccount): Promise<void> {
  await page.goto('/')

  // Wait for login page to load
  await page.waitForSelector('input[type="email"], input[type="text"][name="email"], #email', { timeout: 10000 })

  // Fill email - try multiple selectors
  const emailInput = page.locator('input[type="email"], input[type="text"][name="email"], #email').first()
  await emailInput.fill(account.email)

  // Fill password
  const passwordInput = page.locator('input[type="password"]').first()
  await passwordInput.fill(account.password)

  // Click login button
  const loginButton = page.locator('button[type="submit"], button:has-text("Anmelden")').first()
  await loginButton.click()

  // Wait for navigation away from login page
  await page.waitForURL(url => !url.pathname.includes('/login') && url.pathname !== '/', { timeout: 15000 })
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
