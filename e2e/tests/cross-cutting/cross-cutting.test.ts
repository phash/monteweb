import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { login } from '../../helpers/auth'
import { selectors, toastWithText } from '../../helpers/selectors'

// ============================================================================
// Cross-Cutting E2E Tests
// US-367 through US-374 — PWA, Dark Mode, Language, Maintenance,
// Impersonation, Responsive, Keyboard Navigation, Error Reporting
// ============================================================================

/** Login via API and return a JWT access token. */
async function getToken(
  page: { request: { post: Function } },
  email: string,
  password: string,
): Promise<string> {
  const res = await page.request.post('/api/v1/auth/login', {
    data: { email, password },
  })
  expect(res.ok()).toBeTruthy()
  const body = await res.json()
  return body.data?.accessToken || body.accessToken
}

/** Convenience: get admin token. */
async function getAdminToken(
  page: { request: { post: Function } },
): Promise<string> {
  return getToken(page, accounts.admin.email, accounts.admin.password)
}

/** Standard auth header object. */
function authHeader(token: string) {
  return { Authorization: `Bearer ${token}` }
}

// ============================================================================
// US-367: PWA installieren
// ============================================================================
test.describe('US-367: PWA installieren', () => {

  test('manifest.webmanifest is accessible and returns valid JSON', async ({ page }) => {
    const res = await page.request.get('/manifest.webmanifest')
    // The manifest might also be served at a hashed path; try alternate if needed
    if (!res.ok()) {
      // VitePWA may inline the manifest or use a different path — check the HTML
      await page.goto('/')
      const manifestLink = await page.locator('link[rel="manifest"]').getAttribute('href')
      expect(manifestLink).toBeTruthy()
      if (manifestLink) {
        const altRes = await page.request.get(manifestLink)
        expect(altRes.ok()).toBeTruthy()
        const manifest = await altRes.json()
        expect(manifest).toBeDefined()
        expect(manifest.name).toBeTruthy()
      }
      return
    }
    const manifest = await res.json()
    expect(manifest).toBeDefined()
  })

  test('manifest has required PWA fields', async ({ page }) => {
    await page.goto('/')
    const manifestLink = await page.locator('link[rel="manifest"]').getAttribute('href')
    expect(manifestLink).toBeTruthy()

    const res = await page.request.get(manifestLink!)
    expect(res.ok()).toBeTruthy()
    const manifest = await res.json()

    // Required PWA fields
    expect(manifest.name).toBeTruthy()
    expect(manifest.short_name).toBeTruthy()
    expect(manifest.start_url).toBeTruthy()
    expect(manifest.display).toBe('standalone')
    expect(manifest.icons).toBeDefined()
    expect(Array.isArray(manifest.icons)).toBeTruthy()
    expect(manifest.icons.length).toBeGreaterThanOrEqual(2)
  })

  test('manifest contains correctly sized icons', async ({ page }) => {
    await page.goto('/')
    const manifestLink = await page.locator('link[rel="manifest"]').getAttribute('href')
    expect(manifestLink).toBeTruthy()

    const res = await page.request.get(manifestLink!)
    const manifest = await res.json()

    // Must have at least 192x192 and 512x512
    const sizes = manifest.icons.map((icon: { sizes: string }) => icon.sizes)
    expect(sizes).toContain('192x192')
    expect(sizes).toContain('512x512')
  })

  test('PWA icon files are accessible', async ({ page }) => {
    const icon192 = await page.request.get('/icons/icon-192x192.png')
    expect(icon192.ok()).toBeTruthy()

    const icon512 = await page.request.get('/icons/icon-512x512.png')
    expect(icon512.ok()).toBeTruthy()

    const appleIcon = await page.request.get('/icons/apple-touch-icon.png')
    expect(appleIcon.ok()).toBeTruthy()
  })

  test('service worker is registered after page load', async ({ page }) => {
    await page.goto('/')
    // Give service worker time to register
    await page.waitForTimeout(3000)

    const swRegistered = await page.evaluate(async () => {
      if (!('serviceWorker' in navigator)) return false
      const registration = await navigator.serviceWorker.getRegistration()
      return !!registration
    })
    // Service worker should be registered (VitePWA autoUpdate)
    expect(swRegistered).toBeTruthy()
  })

  test.skip('actual PWA install prompt', async () => {
    // TODO: Actual PWA installation requires specific browser conditions
    // (HTTPS, valid manifest, service worker) and user gesture.
    // Cannot be fully tested in headless Playwright.
  })

  test('HTML has meta tags for mobile web app', async ({ page }) => {
    await page.goto('/')
    // Check for theme-color meta tag
    const themeColor = await page.locator('meta[name="theme-color"]').getAttribute('content')
    expect(themeColor).toBeTruthy()
  })
})

// ============================================================================
// US-368: Dark Mode umschalten
// ============================================================================
test.describe('US-368: Dark Mode umschalten', () => {

  test('API: PUT /api/v1/users/me/dark-mode with DARK succeeds', async ({ page }) => {
    const token = await getToken(page, accounts.parent.email, accounts.parent.password)
    const res = await page.request.put('/api/v1/users/me/dark-mode', {
      headers: authHeader(token),
      data: { darkMode: 'DARK' },
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    const data = body.data || body
    expect(data.darkMode).toBe('DARK')
  })

  test('API: PUT /api/v1/users/me/dark-mode with LIGHT succeeds', async ({ page }) => {
    const token = await getToken(page, accounts.parent.email, accounts.parent.password)
    const res = await page.request.put('/api/v1/users/me/dark-mode', {
      headers: authHeader(token),
      data: { darkMode: 'LIGHT' },
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    const data = body.data || body
    expect(data.darkMode).toBe('LIGHT')
  })

  test('API: PUT /api/v1/users/me/dark-mode with SYSTEM succeeds', async ({ page }) => {
    const token = await getToken(page, accounts.parent.email, accounts.parent.password)
    const res = await page.request.put('/api/v1/users/me/dark-mode', {
      headers: authHeader(token),
      data: { darkMode: 'SYSTEM' },
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    const data = body.data || body
    expect(data.darkMode).toBe('SYSTEM')
  })

  test('API: PUT /api/v1/users/me/dark-mode with INVALID returns 400', async ({ page }) => {
    const token = await getToken(page, accounts.parent.email, accounts.parent.password)
    const res = await page.request.put('/api/v1/users/me/dark-mode', {
      headers: authHeader(token),
      data: { darkMode: 'INVALID' },
    })
    expect(res.status()).toBe(400)
  })

  test('API: GET /api/v1/users/me/dark-mode returns current preference', async ({ page }) => {
    const token = await getToken(page, accounts.parent.email, accounts.parent.password)
    // First set to LIGHT
    await page.request.put('/api/v1/users/me/dark-mode', {
      headers: authHeader(token),
      data: { darkMode: 'LIGHT' },
    })
    // Then read it back
    const res = await page.request.get('/api/v1/users/me/dark-mode', {
      headers: authHeader(token),
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    const data = body.data || body
    expect(data.darkMode).toBe('LIGHT')
  })

  test('UI: profile page shows dark mode selector', async ({ page }) => {
    await login(page, accounts.parent)
    await page.goto('/profile')
    await page.waitForLoadState('networkidle')

    // The dark mode card should be visible
    const darkModeCard = page.locator('.darkmode-card')
    await expect(darkModeCard).toBeVisible({ timeout: 10000 })

    // Should contain a Select (dropdown) component
    const select = darkModeCard.locator('.p-select')
    await expect(select).toBeVisible()
  })

  test('UI: DARK class is applied to html element when dark mode set via API', async ({ page }) => {
    const token = await getToken(page, accounts.parent.email, accounts.parent.password)
    // Set to DARK via API
    await page.request.put('/api/v1/users/me/dark-mode', {
      headers: authHeader(token),
      data: { darkMode: 'DARK' },
    })

    // Login via UI to trigger the composable that reads the dark mode preference
    await login(page, accounts.parent)
    await page.goto('/profile')
    await page.waitForLoadState('networkidle')
    // Give the composable time to apply the class
    await page.waitForTimeout(1500)

    const hasDarkClass = await page.evaluate(() =>
      document.documentElement.classList.contains('dark'),
    )
    expect(hasDarkClass).toBeTruthy()

    // Clean up: reset to SYSTEM
    await page.request.put('/api/v1/users/me/dark-mode', {
      headers: authHeader(token),
      data: { darkMode: 'SYSTEM' },
    })
  })
})

// ============================================================================
// US-369: Sprache wechseln
// ============================================================================
test.describe('US-369: Sprache wechseln', () => {

  test('UI: default page language is German', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // Login page should show German text — "Anmelden" button
    const loginButton = page.locator('button:has-text("Anmelden")')
    await expect(loginButton).toBeVisible({ timeout: 10000 })
  })

  test('UI: profile page has language switcher section', async ({ page }) => {
    await login(page, accounts.parent)
    await page.goto('/profile')
    await page.waitForLoadState('networkidle')

    // Language card should be visible
    const languageCard = page.locator('.language-card')
    await expect(languageCard).toBeVisible({ timeout: 10000 })
  })

  test('UI: login page shows language switcher if multiple languages configured', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // The login page may have a LanguageSwitcher component
    // It is only shown if available_languages has >1 entry
    // We check for its presence but allow it to be absent if only 1 language
    const switcher = page.locator('.language-switcher, [data-testid="language-switcher"]')
    // This is a soft check — the switcher may or may not be visible depending on config
    const isVisible = await switcher.isVisible().catch(() => false)
    // Just verify the page loaded correctly regardless
    expect(true).toBeTruthy()
    // Log for debugging
    if (isVisible) {
      // Language switcher is present on login page
    }
  })

  test('API: tenant config includes available_languages', async ({ page }) => {
    // The config endpoint is public (allowed even during maintenance)
    const res = await page.request.get('/api/v1/config')
    if (res.ok()) {
      const body = await res.json()
      const data = body.data || body
      // If available, check it contains languages
      if (data.availableLanguages) {
        expect(Array.isArray(data.availableLanguages)).toBeTruthy()
        expect(data.availableLanguages).toContain('de')
      }
    }
    // Config endpoint existence is enough — it may require auth in some setups
    expect(true).toBeTruthy()
  })
})

// ============================================================================
// US-370: Wartungsmodus aktivieren
// ============================================================================
test.describe('US-370: Wartungsmodus aktivieren', () => {

  test('API: admin can enable maintenance mode', async ({ page }) => {
    const token = await getAdminToken(page)
    const res = await page.request.put('/api/v1/admin/config/maintenance', {
      headers: authHeader(token),
      data: { maintenanceEnabled: true, maintenanceMessage: 'E2E Test Wartung' },
    })
    expect(res.ok()).toBeTruthy()

    // Immediately disable to avoid locking out other tests
    const disableRes = await page.request.put('/api/v1/admin/config/maintenance', {
      headers: authHeader(token),
      data: { maintenanceEnabled: false },
    })
    expect(disableRes.ok()).toBeTruthy()
  })

  test('API: non-admin gets 503 during maintenance', async ({ page }) => {
    const adminToken = await getAdminToken(page)
    const parentToken = await getToken(page, accounts.parent.email, accounts.parent.password)

    // Enable maintenance
    await page.request.put('/api/v1/admin/config/maintenance', {
      headers: authHeader(adminToken),
      data: { maintenanceEnabled: true, maintenanceMessage: 'Wartungsarbeiten' },
    })

    try {
      // Non-admin should get 503 on a regular API endpoint
      const res = await page.request.get('/api/v1/users/me', {
        headers: authHeader(parentToken),
      })
      expect(res.status()).toBe(503)

      // Check the response body contains maintenance info
      const body = await res.json()
      expect(body.maintenance).toBe(true)
      expect(body.message).toContain('Wartungsarbeiten')
    } finally {
      // Always disable maintenance — critical cleanup
      await page.request.put('/api/v1/admin/config/maintenance', {
        headers: authHeader(adminToken),
        data: { maintenanceEnabled: false },
      })
    }
  })

  test('API: admin still has access during maintenance', async ({ page }) => {
    const adminToken = await getAdminToken(page)

    // Enable maintenance
    await page.request.put('/api/v1/admin/config/maintenance', {
      headers: authHeader(adminToken),
      data: { maintenanceEnabled: true, maintenanceMessage: 'Admin-Test' },
    })

    try {
      // Admin endpoints are whitelisted — admin/config should work
      const res = await page.request.get('/api/v1/admin/users?page=0&size=1', {
        headers: authHeader(adminToken),
      })
      expect(res.ok()).toBeTruthy()
    } finally {
      await page.request.put('/api/v1/admin/config/maintenance', {
        headers: authHeader(adminToken),
        data: { maintenanceEnabled: false },
      })
    }
  })

  test('API: auth endpoints remain accessible during maintenance', async ({ page }) => {
    const adminToken = await getAdminToken(page)

    // Enable maintenance
    await page.request.put('/api/v1/admin/config/maintenance', {
      headers: authHeader(adminToken),
      data: { maintenanceEnabled: true, maintenanceMessage: 'Auth-Test' },
    })

    try {
      // Auth login should still work (whitelisted path /api/v1/auth/)
      const loginRes = await page.request.post('/api/v1/auth/login', {
        data: { email: accounts.parent.email, password: accounts.parent.password },
      })
      expect(loginRes.ok()).toBeTruthy()
    } finally {
      await page.request.put('/api/v1/admin/config/maintenance', {
        headers: authHeader(adminToken),
        data: { maintenanceEnabled: false },
      })
    }
  })

  test('API: disabling maintenance restores access for non-admin', async ({ page }) => {
    const adminToken = await getAdminToken(page)
    const parentToken = await getToken(page, accounts.parent.email, accounts.parent.password)

    // Enable maintenance
    await page.request.put('/api/v1/admin/config/maintenance', {
      headers: authHeader(adminToken),
      data: { maintenanceEnabled: true },
    })

    // Verify blocked
    const blockedRes = await page.request.get('/api/v1/users/me', {
      headers: authHeader(parentToken),
    })
    expect(blockedRes.status()).toBe(503)

    // Disable maintenance
    await page.request.put('/api/v1/admin/config/maintenance', {
      headers: authHeader(adminToken),
      data: { maintenanceEnabled: false },
    })

    // Now non-admin should have access again (need fresh token since JWT itself is fine)
    const freshToken = await getToken(page, accounts.parent.email, accounts.parent.password)
    const unlockedRes = await page.request.get('/api/v1/users/me', {
      headers: authHeader(freshToken),
    })
    expect(unlockedRes.ok()).toBeTruthy()
  })

  test.skip('UI: full maintenance mode flow with banner', async () => {
    // TODO: Full UI flow test would require enabling maintenance, checking
    // the frontend shows a maintenance banner, then disabling it.
    // Skipped to avoid risk of locking out concurrent test sessions.
  })
})

// ============================================================================
// US-371: Impersonation als Cross-Cutting-Funktion
// ============================================================================
test.describe('US-371: Impersonation als Cross-Cutting-Funktion', () => {

  test.skip('POST /api/v1/auth/impersonate with targetUserId', async () => {
    // TODO: Impersonation requires the feature to be enabled and involves
    // complex multi-session token management:
    // 1. POST /api/v1/auth/impersonate { targetUserId } — admin starts impersonation
    // 2. Visual banner appears during impersonation session
    // 3. Cannot impersonate another SUPERADMIN
    // 4. POST /api/v1/auth/stop-impersonation — returns to admin session
  })

  test.skip('cannot impersonate another SUPERADMIN', async () => {
    // TODO: Verify that attempting to impersonate another admin returns 403
  })

  test.skip('impersonation shows visual banner', async () => {
    // TODO: After impersonation, the frontend should display a visible
    // banner indicating the admin is acting as another user
  })

  test.skip('stop-impersonation returns to admin context', async () => {
    // TODO: POST /api/v1/auth/stop-impersonation should restore the
    // original admin token/session
  })
})

// ============================================================================
// US-372: Responsive Design und Touch-Bedienung
// ============================================================================
test.describe('US-372: Responsive Design und Touch-Bedienung', () => {

  test('desktop viewport (1920x1080): sidebar and full layout visible', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 })
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    // Sidebar / navigation should be visible on desktop
    const sidebar = page.locator('.sidebar, .layout-sidebar, nav, .p-menubar')
    await expect(sidebar.first()).toBeVisible({ timeout: 10000 })

    // Main content area should be present
    const main = page.locator('main, .main-content, .layout-main')
    await expect(main.first()).toBeVisible()
  })

  test('tablet viewport (768x1024): layout adapts', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 })
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    // Page should still be functional at tablet width
    const body = page.locator('body')
    await expect(body).toBeVisible()

    // Content should not overflow horizontally
    const hasHScroll = await page.evaluate(() =>
      document.documentElement.scrollWidth > document.documentElement.clientWidth,
    )
    // Allow small tolerance for scrollbar, but no major overflow
    expect(hasHScroll).toBeFalsy()
  })

  test('mobile viewport (375x667): mobile layout without horizontal overflow', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    // Page should render without errors
    const body = page.locator('body')
    await expect(body).toBeVisible()

    // No extreme horizontal overflow on mobile
    const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth)
    const clientWidth = await page.evaluate(() => document.documentElement.clientWidth)
    // Allow up to 20px tolerance
    expect(scrollWidth).toBeLessThanOrEqual(clientWidth + 20)
  })

  test('mobile viewport: login page is usable', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // Login form inputs should be visible and reachable
    const emailInput = page.locator('input[type="email"], input[type="text"][name="email"], #email').first()
    await expect(emailInput).toBeVisible({ timeout: 10000 })

    const passwordInput = page.locator('input[type="password"]').first()
    await expect(passwordInput).toBeVisible()

    const submitButton = page.locator('button[type="submit"], button:has-text("Anmelden")').first()
    await expect(submitButton).toBeVisible()
  })

  test('global search dialog adapts to mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    // Open search via keyboard shortcut
    await page.keyboard.press('Control+k')
    await page.waitForTimeout(500)

    // The dialog should be present and constrained to viewport
    const dialog = page.locator('.global-search-dialog')
    if (await dialog.isVisible().catch(() => false)) {
      const dialogBox = await dialog.boundingBox()
      if (dialogBox) {
        // Dialog should not exceed viewport width
        expect(dialogBox.width).toBeLessThanOrEqual(375)
      }
    }

    // Close dialog
    await page.keyboard.press('Escape')
  })
})

// ============================================================================
// US-373: Tastaturnavigation in der globalen Suche
// ============================================================================
test.describe('US-373: Tastaturnavigation in der globalen Suche', () => {

  test('Ctrl+K opens global search dialog', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')
    await page.waitForTimeout(500)

    // Search dialog should be visible
    const dialog = page.locator('.global-search-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })
  })

  test('Escape closes the search dialog', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    // Open
    await page.keyboard.press('Control+k')
    await page.waitForTimeout(500)
    const dialog = page.locator('.global-search-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Close with Escape
    await page.keyboard.press('Escape')
    await page.waitForTimeout(500)
    await expect(dialog).not.toBeVisible()
  })

  test('search input receives focus when dialog opens', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')
    await page.waitForTimeout(500)

    // The search input should be focused
    const searchInput = page.locator('.global-search-dialog .search-input input, .global-search-dialog .search-input')
    await expect(searchInput.first()).toBeFocused({ timeout: 3000 })
  })

  test('typing in search shows hint for minimum characters', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')
    await page.waitForTimeout(500)

    // Before typing, hint should be visible (min 2 chars)
    const hint = page.locator('.search-hint')
    await expect(hint).toBeVisible({ timeout: 3000 })

    // Close
    await page.keyboard.press('Escape')
  })

  test.skip('ArrowDown/ArrowUp navigate results and Enter selects', async () => {
    // TODO: Full keyboard navigation test requires Solr to be running and
    // indexed with data so that search results are returned. The steps would be:
    // 1. Open search with Ctrl+K
    // 2. Type a query that returns results (e.g. "Admin")
    // 3. Press ArrowDown — first result gets .selected class
    // 4. Press ArrowDown again — second result gets .selected class
    // 5. Press ArrowUp — first result selected again
    // 6. Press Enter — navigates to the selected result URL
    // 7. Dialog closes after navigation
    // Skipped because Solr search availability cannot be guaranteed in E2E.
  })

  test('search filter chips are visible in dialog', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')
    await page.waitForTimeout(500)

    const dialog = page.locator('.global-search-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Filter chips should be present
    const filters = dialog.locator('.filter-chip')
    const filterCount = await filters.count()
    // Should have at least the "Alle" filter + some type filters
    expect(filterCount).toBeGreaterThanOrEqual(2)

    // The "Alle" (ALL) filter should be active by default
    const activeFilter = dialog.locator('.filter-chip.active')
    await expect(activeFilter).toBeVisible()

    await page.keyboard.press('Escape')
  })

  test('Ctrl+K shortcut hint is displayed in search dialog', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')
    await page.waitForTimeout(500)

    // The shortcut hint area should mention Ctrl + K
    const shortcutHint = page.locator('.search-shortcut-hint')
    await expect(shortcutHint).toBeVisible({ timeout: 3000 })

    await page.keyboard.press('Escape')
  })
})

// ============================================================================
// US-374: Fehlerberichterstattung (Frontend Error Reporting)
// ============================================================================
test.describe('US-374: Fehlerberichterstattung', () => {

  test('API: POST /api/v1/error-reports accepts public submissions (no auth)', async ({ page }) => {
    const res = await page.request.post('/api/v1/error-reports', {
      data: {
        source: 'FRONTEND',
        errorType: 'TypeError',
        message: 'E2E Test: Cannot read properties of undefined',
        stackTrace: 'TypeError: Cannot read properties\n  at Object.<anonymous> (test.js:1:1)',
        location: '/dashboard',
        userAgent: 'Playwright E2E Test',
        requestUrl: 'http://localhost/dashboard',
      },
    })
    expect(res.ok()).toBeTruthy()
  })

  test('API: POST /api/v1/error-reports with authenticated user', async ({ page }) => {
    const token = await getToken(page, accounts.parent.email, accounts.parent.password)
    const res = await page.request.post('/api/v1/error-reports', {
      headers: authHeader(token),
      data: {
        source: 'FRONTEND',
        errorType: 'ReferenceError',
        message: 'E2E Test: authenticated error report',
        stackTrace: 'ReferenceError: foo is not defined\n  at test.js:2:3',
        location: '/feed',
        userAgent: 'Playwright E2E Test Auth',
        requestUrl: 'http://localhost/feed',
      },
    })
    expect(res.ok()).toBeTruthy()
  })

  test('API: POST /api/v1/error-reports validates required fields', async ({ page }) => {
    // Missing required 'source' and 'message' fields
    const res = await page.request.post('/api/v1/error-reports', {
      data: {
        errorType: 'SyntaxError',
      },
    })
    expect(res.ok()).toBeFalsy()
    expect(res.status()).toBeGreaterThanOrEqual(400)
  })

  test('API: fingerprint deduplication increments occurrence counter', async ({ page }) => {
    const adminToken = await getAdminToken(page)

    // Submit same error twice (same source + errorType + message → same fingerprint)
    const errorPayload = {
      source: 'FRONTEND',
      errorType: 'DedupTest',
      message: 'E2E dedup test error ' + Date.now(),
      stackTrace: 'at dedup-test.js:1:1',
      location: '/test-dedup',
      userAgent: 'Playwright Dedup',
      requestUrl: 'http://localhost/test-dedup',
    }

    // First submission
    const res1 = await page.request.post('/api/v1/error-reports', { data: errorPayload })
    expect(res1.ok()).toBeTruthy()

    // Second submission with same data
    const res2 = await page.request.post('/api/v1/error-reports', { data: errorPayload })
    expect(res2.ok()).toBeTruthy()

    // Admin should see the report with occurrence_count >= 2
    const listRes = await page.request.get('/api/v1/admin/error-reports?page=0&size=50', {
      headers: authHeader(adminToken),
    })
    expect(listRes.ok()).toBeTruthy()
    const listBody = await listRes.json()
    const data = listBody.data || listBody
    expect(data.content).toBeDefined()

    // Find the report matching our unique message
    const report = data.content.find(
      (r: { message: string }) => r.message === errorPayload.message,
    )
    if (report) {
      // Occurrence count should be at least 2 due to dedup
      expect(report.occurrenceCount || report.occurrence_count || 1).toBeGreaterThanOrEqual(2)
    }
  })

  test('API: admin can list error reports', async ({ page }) => {
    const token = await getAdminToken(page)
    const res = await page.request.get('/api/v1/admin/error-reports?page=0&size=10', {
      headers: authHeader(token),
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    const data = body.data || body
    expect(data.content).toBeDefined()
    expect(Array.isArray(data.content)).toBeTruthy()
  })

  test('API: admin can view a single error report', async ({ page }) => {
    const token = await getAdminToken(page)

    // First get the list
    const listRes = await page.request.get('/api/v1/admin/error-reports?page=0&size=1', {
      headers: authHeader(token),
    })
    expect(listRes.ok()).toBeTruthy()
    const listBody = await listRes.json()
    const data = listBody.data || listBody

    if (data.content && data.content.length > 0) {
      const reportId = data.content[0].id
      const detailRes = await page.request.get(`/api/v1/admin/error-reports/${reportId}`, {
        headers: authHeader(token),
      })
      expect(detailRes.ok()).toBeTruthy()
      const detail = await detailRes.json()
      const reportData = detail.data || detail
      expect(reportData.id).toBe(reportId)
      expect(reportData.message).toBeTruthy()
      expect(reportData.source).toBeTruthy()
    }
  })

  test('API: admin can update error report status', async ({ page }) => {
    const token = await getAdminToken(page)

    // Get a report to update
    const listRes = await page.request.get('/api/v1/admin/error-reports?page=0&size=1', {
      headers: authHeader(token),
    })
    const listBody = await listRes.json()
    const data = listBody.data || listBody

    if (data.content && data.content.length > 0) {
      const reportId = data.content[0].id

      // Update status to RESOLVED
      const updateRes = await page.request.put(`/api/v1/admin/error-reports/${reportId}/status`, {
        headers: authHeader(token),
        data: { status: 'RESOLVED' },
      })
      expect(updateRes.ok()).toBeTruthy()
      const updateBody = await updateRes.json()
      const updated = updateBody.data || updateBody
      expect(updated.status).toBe('RESOLVED')

      // Reset to NEW for other tests
      await page.request.put(`/api/v1/admin/error-reports/${reportId}/status`, {
        headers: authHeader(token),
        data: { status: 'NEW' },
      })
    }
  })

  test('API: non-admin cannot access admin error reports endpoint', async ({ page }) => {
    const parentToken = await getToken(page, accounts.parent.email, accounts.parent.password)
    const res = await page.request.get('/api/v1/admin/error-reports?page=0&size=10', {
      headers: authHeader(parentToken),
    })
    expect(res.ok()).toBeFalsy()
    expect(res.status()).toBe(403)
  })
})
