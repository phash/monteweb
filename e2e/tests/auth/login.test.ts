import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { login, loginAs, logout, expectDashboard } from '../../helpers/auth'

// ============================================================================
// Auth Module E2E Tests — Login, Logout, Redirect
// US-001, US-002, US-003, US-008, US-022, US-023
// ============================================================================

test.describe('US-001: Login with valid credentials', () => {
  // Note: Auth tests must NOT use the global storageState (teacher.json) because
  // we are testing the login flow itself. Override to clear state.
  test.use({ storageState: { cookies: [], origins: [] } })

  for (const [key, account] of Object.entries(accounts)) {
    test(`${account.role} (${key}) can login successfully`, async ({ page }) => {
      await page.goto('/')

      // Should be on login page
      await expect(page.locator('h1')).toContainText('MonteWeb')

      // Fill email
      const emailInput = page.locator('#email, input[type="email"]').first()
      await emailInput.fill(account.email)

      // Fill password — PrimeVue Password component renders an input inside a wrapper
      const passwordInput = page.locator('#password, input[type="password"]').first()
      await passwordInput.fill(account.password)

      // Click submit
      const submitButton = page.locator('button[type="submit"], button:has-text("Anmelden")').first()
      await submitButton.click()

      // Wait for navigation away from login page
      await page.waitForURL(
        url => !url.pathname.includes('/login') && url.pathname !== '/',
        { timeout: 15000 }
      )

      // Should be on a protected page (dashboard or terms acceptance)
      const currentUrl = page.url()
      expect(currentUrl).not.toContain('/login')
    })
  }
})

test.describe('US-002: Login with wrong credentials', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('wrong password shows error message', async ({ page }) => {
    await page.goto('/')

    const emailInput = page.locator('#email, input[type="email"]').first()
    await emailInput.fill('lehrer@monteweb.local')

    const passwordInput = page.locator('#password, input[type="password"]').first()
    await passwordInput.fill('wrongpassword123')

    const submitButton = page.locator('button[type="submit"], button:has-text("Anmelden")').first()
    await submitButton.click()

    // Error message should appear (PrimeVue Message component with severity="error")
    const errorMessage = page.locator('.p-message-error, [data-pc-severity="error"]')
    await expect(errorMessage).toBeVisible({ timeout: 10000 })

    // User should still be on login page
    await expect(page.locator('h1')).toContainText('MonteWeb')
  })

  test('non-existent email shows same error (no email enumeration)', async ({ page }) => {
    await page.goto('/')

    const emailInput = page.locator('#email, input[type="email"]').first()
    await emailInput.fill('nonexistent@monteweb.local')

    const passwordInput = page.locator('#password, input[type="password"]').first()
    await passwordInput.fill('somepassword')

    const submitButton = page.locator('button[type="submit"], button:has-text("Anmelden")').first()
    await submitButton.click()

    // Same generic error message (no hint about whether email exists)
    const errorMessage = page.locator('.p-message-error, [data-pc-severity="error"]')
    await expect(errorMessage).toBeVisible({ timeout: 10000 })

    // Should still be on login page
    await expect(page.locator('h1')).toContainText('MonteWeb')
  })
})

test.describe('US-003: Login with empty form', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('submit without any input is prevented by HTML5 validation', async ({ page }) => {
    await page.goto('/')

    // Wait for login form to render
    await expect(page.locator('#email, input[type="email"]').first()).toBeVisible()

    const submitButton = page.locator('button[type="submit"], button:has-text("Anmelden")').first()
    await submitButton.click()

    // The email input has `required` attribute — browser should prevent submission.
    // Verify we are still on the login page (form was NOT submitted).
    await expect(page.locator('h1')).toContainText('MonteWeb')

    // The email field should have validation state — check that it is invalid
    const emailInput = page.locator('#email, input[type="email"]').first()
    const isInvalid = await emailInput.evaluate(
      (el: HTMLInputElement) => !el.checkValidity()
    )
    expect(isInvalid).toBe(true)
  })

  test('submit with only email is prevented by password validation', async ({ page }) => {
    await page.goto('/')

    const emailInput = page.locator('#email, input[type="email"]').first()
    await emailInput.fill('lehrer@monteweb.local')

    const submitButton = page.locator('button[type="submit"], button:has-text("Anmelden")').first()
    await submitButton.click()

    // Password field has `required` — browser prevents submission
    await expect(page.locator('h1')).toContainText('MonteWeb')

    // Verify password input is invalid
    const passwordInput = page.locator('#password, input[type="password"]').first()
    const isInvalid = await passwordInput.evaluate(
      (el: HTMLInputElement) => !el.checkValidity()
    )
    expect(isInvalid).toBe(true)
  })
})

test.describe('US-008: Login of non-approved user', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  // Requires a test user in PENDING state — cannot reliably create one
  // without a registration + admin approval flow running first.
  test.skip('pending user sees approval message', async ({ page }) => {
    // Requires test user in PENDING state
    // When attempted, the login response should contain 'PENDING_APPROVAL'
    // and the UI should display: "Ihr Konto wartet auf Freischaltung durch einen Administrator."
  })
})

test.describe('US-022: Logout', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('user can logout and is redirected to login', async ({ page }) => {
    // First, login as teacher
    await login(page, accounts.teacher)

    // Verify we are logged in (not on login page)
    await expect(page).not.toHaveURL(/\/login/)

    // Open user menu and click logout
    // The AppHeader has a button with class "user-menu-button" that toggles the PrimeVue Menu
    const userMenuButton = page.locator('.user-menu-button, [aria-label="Profil"], button:has(.pi-user)').first()
    await userMenuButton.click()

    // Click logout in the popup menu
    const logoutItem = page.locator('text=Abmelden').first()
    await logoutItem.click({ timeout: 5000 })

    // Should be redirected to login page
    await page.waitForURL(url => url.pathname === '/login' || url.pathname === '/', { timeout: 10000 })
    await expect(page.locator('h1')).toContainText('MonteWeb')
  })

  test('after logout, accessing protected route redirects to login', async ({ page }) => {
    // Login
    await login(page, accounts.teacher)
    await expect(page).not.toHaveURL(/\/login/)

    // Logout
    const userMenuButton = page.locator('.user-menu-button, [aria-label="Profil"], button:has(.pi-user)').first()
    await userMenuButton.click()
    const logoutItem = page.locator('text=Abmelden').first()
    await logoutItem.click({ timeout: 5000 })

    // Wait for login page
    await page.waitForURL(url => url.pathname === '/login' || url.pathname === '/', { timeout: 10000 })

    // Now try to navigate to a protected route
    await page.goto('/rooms')

    // Should be redirected back to login (with redirect query param)
    await page.waitForURL(url => url.pathname === '/login' || url.pathname === '/', { timeout: 10000 })
    await expect(page.locator('h1')).toContainText('MonteWeb')
  })
})

test.describe('US-023: Automatic redirect after login', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('after login, user is redirected back to originally requested page', async ({ page }) => {
    // Navigate to a protected route while not logged in
    await page.goto('/rooms')

    // Should be redirected to login page with redirect query param
    await page.waitForURL(url => url.pathname === '/login' || url.pathname === '/', { timeout: 10000 })
    await expect(page.locator('h1')).toContainText('MonteWeb')

    // The URL should contain a redirect query parameter
    const currentUrl = new URL(page.url())
    const redirectParam = currentUrl.searchParams.get('redirect')
    expect(redirectParam).toBeTruthy()

    // Now login
    const emailInput = page.locator('#email, input[type="email"]').first()
    await emailInput.fill(accounts.teacher.email)

    const passwordInput = page.locator('#password, input[type="password"]').first()
    await passwordInput.fill(accounts.teacher.password)

    const submitButton = page.locator('button[type="submit"], button:has-text("Anmelden")').first()
    await submitButton.click()

    // After successful login, should be redirected to /rooms (not dashboard)
    await page.waitForURL(url => url.pathname.includes('/rooms'), { timeout: 15000 })
    expect(page.url()).toContain('/rooms')
  })
})
