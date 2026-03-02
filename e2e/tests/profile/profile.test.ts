import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { login, loginAs } from '../../helpers/auth'

// ============================================================================
// Profile & Settings Module E2E Tests
// US-024 to US-036
// ============================================================================

test.describe('Profile & Settings', () => {

  // --------------------------------------------------------------------------
  // US-024: View profile
  // --------------------------------------------------------------------------
  test.describe('US-024: View profile', () => {
    test('displays profile page with user information sections', async ({ page }) => {
      // Login as teacher
      await login(page, accounts.teacher)

      // Navigate to profile
      await page.goto('/profile')
      await page.waitForLoadState('networkidle')

      // Verify profile page title is visible ("Mein Profil")
      await expect(page.locator('text=Mein Profil')).toBeVisible({ timeout: 10000 })

      // Verify name fields are present
      const firstNameInput = page.locator('#profile-firstName')
      await expect(firstNameInput).toBeVisible()
      await expect(firstNameInput).not.toHaveValue('')

      const lastNameInput = page.locator('#profile-lastName')
      await expect(lastNameInput).toBeVisible()
      await expect(lastNameInput).not.toHaveValue('')

      // Verify email field is present and disabled
      const emailInput = page.locator('#profile-email')
      await expect(emailInput).toBeVisible()
      await expect(emailInput).toBeDisabled()

      // Verify roles section is visible ("Meine Rollen")
      await expect(page.locator('.roles-card')).toBeVisible()
      await expect(page.locator('text=Meine Rollen')).toBeVisible()

      // Verify appearance/dark mode section is visible ("Erscheinungsbild")
      await expect(page.locator('.darkmode-card')).toBeVisible()
      await expect(page.locator('text=Erscheinungsbild')).toBeVisible()
    })
  })

  // --------------------------------------------------------------------------
  // US-025: Edit profile
  // --------------------------------------------------------------------------
  test.describe('US-025: Edit profile', () => {
    test('can edit first name, save, and verify persistence after reload', async ({ page }) => {
      // Login as teacher
      await login(page, accounts.teacher)

      // Navigate to profile
      await page.goto('/profile')
      await page.waitForLoadState('networkidle')
      await expect(page.locator('#profile-firstName')).toBeVisible({ timeout: 10000 })

      // Read original first name
      const firstNameInput = page.locator('#profile-firstName')
      const originalFirstName = await firstNameInput.inputValue()

      // Change first name to a test value
      const testFirstName = originalFirstName === 'TestChange' ? 'TestRevert' : 'TestChange'
      await firstNameInput.clear()
      await firstNameInput.fill(testFirstName)

      // Click save button (the first submit button in the profile form)
      const saveButton = page.locator('.profile-form button[type="submit"]').first()
      await saveButton.click()

      // Verify success message appears ("Profil gespeichert")
      await expect(page.locator('text=Profil gespeichert')).toBeVisible({ timeout: 10000 })

      // Reload and verify the name persisted
      await page.reload()
      await page.waitForLoadState('networkidle')
      await expect(page.locator('#profile-firstName')).toBeVisible({ timeout: 10000 })
      await expect(page.locator('#profile-firstName')).toHaveValue(testFirstName)

      // CLEANUP: Revert first name back to original
      await page.locator('#profile-firstName').clear()
      await page.locator('#profile-firstName').fill(originalFirstName)
      await saveButton.click()
      await expect(page.locator('text=Profil gespeichert')).toBeVisible({ timeout: 10000 })
    })
  })

  // --------------------------------------------------------------------------
  // US-026: Upload avatar
  // --------------------------------------------------------------------------
  test.describe('US-026: Upload avatar', () => {
    test.skip('upload avatar — requires file upload interaction', async () => {
      // Requires file upload interaction with AvatarUpload component
    })
  })

  // --------------------------------------------------------------------------
  // US-027: Remove avatar
  // --------------------------------------------------------------------------
  test.describe('US-027: Remove avatar', () => {
    test.skip('remove avatar — requires existing avatar', async () => {
      // Requires an existing uploaded avatar to remove
    })
  })

  // --------------------------------------------------------------------------
  // US-028: Dark mode toggle
  // --------------------------------------------------------------------------
  test.describe('US-028: Dark mode toggle', () => {
    test('can switch dark mode to Dark and back to System', async ({ page }) => {
      // Login as teacher
      await login(page, accounts.teacher)

      // Navigate to profile
      await page.goto('/profile')
      await page.waitForLoadState('networkidle')
      await expect(page.locator('.darkmode-card')).toBeVisible({ timeout: 10000 })

      // Find the dark mode select within the darkmode-card
      const darkModeSelect = page.locator('.darkmode-card .p-select').first()
      await expect(darkModeSelect).toBeVisible()

      // Click the select to open the dropdown
      await darkModeSelect.click()

      // Select "Dunkel" (Dark) option from the dropdown overlay
      const darkOption = page.locator('.p-select-overlay .p-select-option:has-text("Dunkel")').first()
      await darkOption.click()

      // Wait for the toast confirmation ("Erscheinungsbild gespeichert")
      await expect(page.locator('.p-toast-message')).toBeVisible({ timeout: 5000 })

      // Verify dark mode class is applied to the document
      const hasDarkClass = await page.evaluate(() => {
        return document.documentElement.classList.contains('p-dark') ||
               document.documentElement.classList.contains('dark') ||
               document.body.classList.contains('dark-mode') ||
               document.documentElement.getAttribute('data-theme') === 'dark'
      })
      expect(hasDarkClass).toBe(true)

      // Switch back to "Automatisch (System)"
      await darkModeSelect.click()
      const systemOption = page.locator('.p-select-overlay .p-select-option:has-text("Automatisch")').first()
      await systemOption.click()

      // Wait for the toast confirmation
      await expect(page.locator('.p-toast-message')).toBeVisible({ timeout: 5000 })
    })
  })

  // --------------------------------------------------------------------------
  // US-029: Language selection
  // --------------------------------------------------------------------------
  test.describe('US-029: Language selection', () => {
    test('can switch language to English and back to German', async ({ page }) => {
      // Login as teacher
      await login(page, accounts.teacher)

      // Navigate to profile
      await page.goto('/profile')
      await page.waitForLoadState('networkidle')
      await expect(page.locator('.language-card')).toBeVisible({ timeout: 10000 })

      // Find the language switcher select within the language-card
      // The LanguageSwitcher only renders if >1 language is available
      const langSelect = page.locator('.language-card .p-select, .language-card .lang-select').first()
      const langVisible = await langSelect.isVisible({ timeout: 5000 }).catch(() => false)

      if (!langVisible) {
        // Language switcher not visible — only one language configured
        test.skip()
        return
      }

      // Click the select to open the dropdown
      await langSelect.click()

      // Select "English" from the dropdown
      const enOption = page.locator('.p-select-overlay .p-select-option:has-text("English")').first()
      const enVisible = await enOption.isVisible({ timeout: 3000 }).catch(() => false)

      if (!enVisible) {
        // English not available in the dropdown
        test.skip()
        return
      }

      await enOption.click()

      // Wait for UI to re-render in English
      await page.waitForTimeout(1000)

      // Verify some text changed to English (e.g., "My Profile" instead of "Mein Profil")
      const pageText = await page.locator('body').textContent()
      expect(pageText).toContain('My Profile')

      // Switch back to German
      const langSelectAfter = page.locator('.language-card .p-select, .language-card .lang-select').first()
      await langSelectAfter.click()

      const deOption = page.locator('.p-select-overlay .p-select-option:has-text("Deutsch")').first()
      await deOption.click()

      // Verify German text is restored
      await page.waitForTimeout(1000)
      await expect(page.locator('text=Mein Profil')).toBeVisible({ timeout: 5000 })
    })
  })

  // --------------------------------------------------------------------------
  // US-030: Language not visible with single language
  // --------------------------------------------------------------------------
  test.describe('US-030: Language not visible with single language', () => {
    test.skip('language switcher hidden when only one language configured — requires single-language configuration', async () => {
      // Requires the tenant to be configured with only one available language.
      // The LanguageSwitcher component conditionally renders only when
      // availableLanguages.length > 1.
    })
  })

  // --------------------------------------------------------------------------
  // US-031: Custom profile fields
  // --------------------------------------------------------------------------
  test.describe('US-031: Custom profile fields', () => {
    test.skip('custom profile fields visible and editable — requires admin-defined profile fields', async () => {
      // Requires the profilefields module to be enabled and at least one
      // profile field definition to be created by an admin.
      // The section only renders when:
      //   profileFieldsModuleEnabled && profileFieldsStore.definitions.length > 0
    })
  })

  // --------------------------------------------------------------------------
  // US-032: Admin manages profile fields
  // --------------------------------------------------------------------------
  test.describe('US-032: Admin manages profile fields', () => {
    test('admin can navigate to profile fields management page', async ({ page }) => {
      // Login as admin
      await login(page, accounts.admin)

      // Navigate to admin profile fields page
      await page.goto('/admin/profile-fields')
      await page.waitForLoadState('networkidle')

      // Verify the page loaded — look for the page title
      // "Profilfelder verwalten"
      await expect(
        page.locator('text=Profilfelder verwalten')
      ).toBeVisible({ timeout: 10000 })

      // Verify the "Neues Feld" (New Field) button is present
      await expect(
        page.locator('button:has-text("Neues Feld")')
      ).toBeVisible()

      // Skip actual CRUD operations — they would create persistent state
      // that affects other tests. A full CRUD test would:
      // 1. Click "Neues Feld"
      // 2. Fill in fieldKey, labelDe, labelEn, fieldType
      // 3. Save and verify toast
      // 4. Edit the created field
      // 5. Delete the created field
    })
  })

  // --------------------------------------------------------------------------
  // US-033: Role switch
  // --------------------------------------------------------------------------
  test.describe('US-033: Role switch', () => {
    test.skip('user with multiple roles can switch active role — requires user with multiple roles', async () => {
      // Requires a test user that has been assigned multiple roles
      // (e.g., both TEACHER and PARENT). The role switcher card only
      // renders when auth.canSwitchRole is true.
    })
  })

  // --------------------------------------------------------------------------
  // US-034: Push notifications
  // --------------------------------------------------------------------------
  test.describe('US-034: Push notifications', () => {
    test.skip('push notification toggle — requires Push API and VAPID configuration', async () => {
      // Requires:
      // 1. VAPID keys configured on the backend (monteweb.push.enabled=true)
      // 2. Browser Push API support (not available in headless Playwright)
      // 3. Service Worker registration
      // The push card only renders when pushSupported is true.
    })
  })

  // --------------------------------------------------------------------------
  // US-035: Muted chats
  // --------------------------------------------------------------------------
  test.describe('US-035: Muted chats', () => {
    test.skip('muted chats section shows muted conversations — requires pre-muted conversations', async () => {
      // Requires:
      // 1. Messaging module enabled
      // 2. At least one conversation that has been muted by the test user
      // The muted chats section shows "Keine stummgeschalteten Chats"
      // when no conversations are muted.
    })
  })

  // --------------------------------------------------------------------------
  // US-036: Email digest config
  // --------------------------------------------------------------------------
  test.describe('US-036: Email digest config', () => {
    test.skip('email digest frequency selection — requires email configuration', async () => {
      // Requires:
      // 1. Email sending configured on the backend (monteweb.email.enabled=true)
      // 2. The digest card is always visible, but changing the frequency
      //    requires a working email backend to be meaningful.
    })
  })

})
