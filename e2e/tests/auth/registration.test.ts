import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'

// ============================================================================
// Auth Module E2E Tests — Registration & Terms
// US-004, US-005, US-006, US-007, US-021
// ============================================================================

// All registration tests start unauthenticated
test.use({ storageState: { cookies: [], origins: [] } })

/**
 * Navigate to the registration form by clicking the "Registrieren" toggle link.
 */
async function goToRegistrationForm(page: import('@playwright/test').Page) {
  await page.goto('/')
  await expect(page.locator('h1')).toContainText('MonteWeb')

  // Click the toggle link to switch from login to register mode
  // The login page has: "Noch kein Konto?" + link "Registrieren"
  const registerLink = page.locator('a:has-text("Registrieren")').first()
  await registerLink.click()

  // Wait for registration form fields to appear (firstName is only visible in register mode)
  await expect(page.locator('#firstName')).toBeVisible({ timeout: 5000 })
}

test.describe('US-004: Registration with valid data', () => {
  test('new user can register and sees approval message', async ({ page }) => {
    await goToRegistrationForm(page)

    // Generate unique email to avoid conflicts
    const uniqueEmail = `e2e.${Date.now()}@monteweb.local`

    // Fill registration form
    await page.locator('#firstName').fill('E2E')
    await page.locator('#lastName').fill('Testuser')
    await page.locator('#email, input[type="email"]').first().fill(uniqueEmail)

    // PrimeVue Password component — fill the password input
    const passwordInput = page.locator('#password, input[type="password"]').first()
    await passwordInput.fill('SecurePass123!')

    // Phone is optional, but fill it for completeness
    await page.locator('#phone').fill('+49 170 1234567')

    // Accept terms checkbox — PrimeVue Checkbox
    const termsCheckbox = page.locator('#acceptTerms, .terms-checkbox input[type="checkbox"], .terms-checkbox .p-checkbox-box').first()
    await termsCheckbox.click()

    // Submit the form
    const submitButton = page.locator('button[type="submit"], button:has-text("Registrieren")').first()
    await submitButton.click()

    // Should see success message about admin approval
    // The i18n key "auth.pendingApprovalSuccess" renders:
    // "Registrierung erfolgreich! Ihr Konto muss zunachst von einem Administrator freigeschaltet werden."
    const successMessage = page.locator('.p-message-success, [data-pc-severity="success"]')
    await expect(successMessage).toBeVisible({ timeout: 10000 })
    await expect(successMessage).toContainText('Registrierung erfolgreich')
  })
})

test.describe('US-005: Registration without accepting terms', () => {
  test('form submission fails without terms acceptance', async ({ page }) => {
    await goToRegistrationForm(page)

    const uniqueEmail = `e2e.noterms.${Date.now()}@monteweb.local`

    // Fill all required fields
    await page.locator('#firstName').fill('NoTerms')
    await page.locator('#lastName').fill('User')
    await page.locator('#email, input[type="email"]').first().fill(uniqueEmail)

    const passwordInput = page.locator('#password, input[type="password"]').first()
    await passwordInput.fill('SecurePass123!')

    // Do NOT check the terms checkbox

    // Submit the form
    const submitButton = page.locator('button[type="submit"], button:has-text("Registrieren")').first()
    await submitButton.click()

    // Should show error about terms acceptance
    // i18n key "auth.termsRequired": "Sie muessen den Nutzungsbedingungen zustimmen."
    const errorMessage = page.locator('.p-message-error, [data-pc-severity="error"]')
    await expect(errorMessage).toBeVisible({ timeout: 10000 })
    await expect(errorMessage).toContainText('Nutzungsbedingungen')

    // Should still be on registration page (form was not submitted)
    await expect(page.locator('#firstName')).toBeVisible()
  })
})

test.describe('US-006: Registration with existing email', () => {
  test('duplicate email shows error message', async ({ page }) => {
    await goToRegistrationForm(page)

    // Use an existing email
    await page.locator('#firstName').fill('Duplicate')
    await page.locator('#lastName').fill('Email')
    await page.locator('#email, input[type="email"]').first().fill('lehrer@monteweb.local')

    const passwordInput = page.locator('#password, input[type="password"]').first()
    await passwordInput.fill('SecurePass123!')

    // Accept terms
    const termsCheckbox = page.locator('#acceptTerms, .terms-checkbox input[type="checkbox"], .terms-checkbox .p-checkbox-box').first()
    await termsCheckbox.click()

    // Submit
    const submitButton = page.locator('button[type="submit"], button:has-text("Registrieren")').first()
    await submitButton.click()

    // Should show error about existing email or generic registration error
    const errorMessage = page.locator('.p-message-error, [data-pc-severity="error"]')
    await expect(errorMessage).toBeVisible({ timeout: 10000 })

    // Should still be on registration page
    await expect(page.locator('#firstName')).toBeVisible()
  })
})

test.describe('US-007: User approval by admin', () => {
  // Depends on US-004 creating a pending user and then an admin approving it.
  // This requires a multi-step flow: register -> login as admin -> approve -> login as new user.
  test.skip('admin approves pending user registration', async ({ page }) => {
    // Depends on US-004 creating a pending user + admin flow
    // Steps would be:
    // 1. Register a new user (US-004)
    // 2. Login as admin
    // 3. Navigate to admin users
    // 4. Find and approve the pending user
    // 5. Logout, login as the new user
    // 6. Verify access is granted
  })
})

test.describe('US-021: Terms of Service at registration', () => {
  test('terms link exists on registration form', async ({ page }) => {
    await goToRegistrationForm(page)

    // The terms link text comes from i18n: "Nutzungsbedingungen"
    const termsLink = page.locator('.terms-link, a:has-text("Nutzungsbedingungen")').first()
    await expect(termsLink).toBeVisible()
  })

  test('terms link opens in new tab (target="_blank")', async ({ page }) => {
    await goToRegistrationForm(page)

    // Check the terms link has target="_blank"
    // The LoginView uses <router-link to="/terms" target="_blank" class="terms-link">
    const termsLink = page.locator('.terms-link, a:has-text("Nutzungsbedingungen")').first()

    const target = await termsLink.getAttribute('target')
    expect(target).toBe('_blank')
  })

  test('terms link points to /terms route', async ({ page }) => {
    await goToRegistrationForm(page)

    const termsLink = page.locator('.terms-link, a:has-text("Nutzungsbedingungen")').first()
    const href = await termsLink.getAttribute('href')
    expect(href).toContain('/terms')
  })
})
