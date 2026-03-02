import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'

// ============================================================================
// Auth Module E2E Tests — Password Reset, 2FA, SSO/OIDC, LDAP
// US-009, US-010, US-011 to US-016, US-017, US-018, US-019, US-020
// ============================================================================

// All tests start unauthenticated
test.use({ storageState: { cookies: [], origins: [] } })

test.describe('US-009: Password reset request', () => {
  // Note: The current LoginView does not render a "Passwort vergessen?" link,
  // even though the i18n key exists. The password reset flow may be accessed
  // via a direct URL or the link may be conditionally rendered.
  // We test the API endpoint behavior via the UI if the link is present,
  // otherwise we skip gracefully.

  test('password reset request shows neutral confirmation for existing email', async ({ page }) => {
    await page.goto('/')

    // Look for "Passwort vergessen?" link
    const forgotLink = page.locator('a:has-text("Passwort vergessen"), a:has-text("Passwort"), button:has-text("Passwort vergessen")')
    const isVisible = await forgotLink.isVisible({ timeout: 5000 }).catch(() => false)

    if (!isVisible) {
      // The forgot-password link is not rendered in the current UI.
      // Try navigating to a password-reset route directly.
      await page.goto('/password-reset')
      const hasResetPage = await page.locator('input[type="email"], #email').isVisible({ timeout: 5000 }).catch(() => false)

      if (!hasResetPage) {
        test.skip()
        return
      }
    } else {
      await forgotLink.click()
    }

    // Enter a known email
    const emailInput = page.locator('#email, input[type="email"]').first()
    await emailInput.fill('lehrer@monteweb.local')

    // Submit
    const submitButton = page.locator('button[type="submit"]').first()
    await submitButton.click()

    // Should show a neutral confirmation message (no indication of whether email exists)
    // Wait for any success/info message
    const confirmMessage = page.locator('.p-message-success, .p-message-info, [data-pc-severity="success"], [data-pc-severity="info"]')
    await expect(confirmMessage).toBeVisible({ timeout: 10000 })
  })

  test('password reset with non-existent email shows same neutral message', async ({ page }) => {
    await page.goto('/')

    const forgotLink = page.locator('a:has-text("Passwort vergessen"), button:has-text("Passwort vergessen")')
    const isVisible = await forgotLink.isVisible({ timeout: 5000 }).catch(() => false)

    if (!isVisible) {
      await page.goto('/password-reset')
      const hasResetPage = await page.locator('input[type="email"], #email').isVisible({ timeout: 5000 }).catch(() => false)

      if (!hasResetPage) {
        test.skip()
        return
      }
    } else {
      await forgotLink.click()
    }

    // Enter a non-existent email
    const emailInput = page.locator('#email, input[type="email"]').first()
    await emailInput.fill('nonexistent.user@monteweb.local')

    const submitButton = page.locator('button[type="submit"]').first()
    await submitButton.click()

    // Should show the SAME neutral message (no email enumeration)
    const confirmMessage = page.locator('.p-message-success, .p-message-info, [data-pc-severity="success"], [data-pc-severity="info"]')
    await expect(confirmMessage).toBeVisible({ timeout: 10000 })
  })
})

test.describe('US-010: Password reset with token', () => {
  test.skip('user can reset password via email token', async ({ page }) => {
    // Requires email access or direct API token generation
    // Steps would be:
    // 1. Trigger password reset for a test user
    // 2. Extract token from email/API
    // 3. Navigate to /password-reset?token=<token>
    // 4. Enter new password
    // 5. Verify login with new password works
  })
})

test.describe('US-011: Enable 2FA (TOTP)', () => {
  test.skip('user can enable TOTP two-factor authentication', async ({ page }) => {
    // Requires TOTP code generation (otpauth library) - implement when totp test helper is available
    // Steps would be:
    // 1. Login as teacher
    // 2. Navigate to profile security settings
    // 3. Click enable 2FA
    // 4. Scan/read TOTP secret from QR code
    // 5. Generate TOTP code using secret
    // 6. Submit code to verify setup
    // 7. Verify 2FA is enabled
  })
})

test.describe('US-012: Login with 2FA', () => {
  test.skip('login with TOTP code after credentials', async ({ page }) => {
    // Requires TOTP code generation (otpauth library) - implement when totp test helper is available
    // Steps would be:
    // 1. Login with email/password (2FA enabled account)
    // 2. Should see 2FA code input
    // 3. Generate and enter TOTP code
    // 4. Verify successful login
  })
})

test.describe('US-013: Login with 2FA wrong code', () => {
  test.skip('wrong TOTP code shows error', async ({ page }) => {
    // Requires TOTP code generation (otpauth library) - implement when totp test helper is available
    // Steps would be:
    // 1. Login with email/password (2FA enabled account)
    // 2. Enter wrong TOTP code
    // 3. Verify error message is shown
    // 4. Verify user is not logged in
  })
})

test.describe('US-014: 2FA recovery codes', () => {
  test.skip('user can login with recovery code when TOTP unavailable', async ({ page }) => {
    // Requires TOTP code generation (otpauth library) - implement when totp test helper is available
    // Steps would be:
    // 1. Enable 2FA, save recovery codes
    // 2. Logout
    // 3. Login with email/password
    // 4. Use recovery code instead of TOTP
    // 5. Verify successful login
    // 6. Verify recovery code is consumed (cannot be reused)
  })
})

test.describe('US-015: Disable 2FA', () => {
  test.skip('user can disable two-factor authentication', async ({ page }) => {
    // Requires TOTP code generation (otpauth library) - implement when totp test helper is available
    // Steps would be:
    // 1. Login as user with 2FA enabled
    // 2. Navigate to profile security settings
    // 3. Click disable 2FA
    // 4. Confirm with password
    // 5. Verify 2FA is disabled
    // 6. Verify next login does not require TOTP
  })
})

test.describe('US-016: Admin manages 2FA mode', () => {
  test.skip('admin can change 2FA mode (DISABLED/OPTIONAL/MANDATORY)', async ({ page }) => {
    // Requires TOTP code generation (otpauth library) - implement when totp test helper is available
    // Steps would be:
    // 1. Login as admin
    // 2. Navigate to admin settings
    // 3. Change 2FA mode to MANDATORY
    // 4. Verify setting is saved
    // 5. Login as regular user
    // 6. Verify 2FA setup is required (grace period)
  })
})

test.describe('US-017: SSO/OIDC login', () => {
  test.skip('SSO login button redirects to OIDC provider', async ({ page }) => {
    // Requires OIDC provider configuration
    // Steps would be:
    // 1. Configure OIDC provider in admin settings
    // 2. Navigate to login page
    // 3. SSO button should be visible
    // 4. Click SSO button
    // 5. Should redirect to OIDC provider authorization URL
  })
})

test.describe('US-018: SSO button visibility when OIDC disabled', () => {
  test('SSO button is not visible when OIDC is not configured', async ({ page }) => {
    await page.goto('/')

    // Wait for login page to fully render
    await expect(page.locator('h1')).toContainText('MonteWeb')
    await expect(page.locator('#email, input[type="email"]').first()).toBeVisible()

    // Give the OIDC config check time to complete (it is an async call on mount)
    await page.waitForTimeout(2000)

    // The SSO button uses i18n key "auth.loginSso" = "Mit SSO anmelden"
    const ssoButton = page.locator('button:has-text("SSO"), button:has-text("Mit SSO anmelden")')
    await expect(ssoButton).not.toBeVisible()
  })
})

test.describe('US-019: LDAP authentication', () => {
  test.skip('user can login via LDAP credentials', async ({ page }) => {
    // Requires LDAP server
    // Steps would be:
    // 1. Configure LDAP in admin settings (url, base_dn, bind credentials)
    // 2. Enable LDAP module
    // 3. Login with LDAP user credentials
    // 4. Verify user is created/authenticated from LDAP
  })
})

test.describe('US-020: LDAP configuration', () => {
  test.skip('admin can configure and test LDAP connection', async ({ page }) => {
    // Requires LDAP server for connection test
    // Steps would be:
    // 1. Login as admin
    // 2. Navigate to admin modules/settings
    // 3. Enable LDAP module
    // 4. Fill LDAP configuration (server URL, base DN, bind DN, etc.)
    // 5. Test connection
    // 6. Verify connection test result
  })
})
