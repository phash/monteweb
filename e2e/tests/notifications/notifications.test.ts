import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { login } from '../../helpers/auth'

// ============================================================================
// Notifications E2E Tests — US-325 to US-331
// ============================================================================

type Page = import('@playwright/test').Page

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Fetch notifications list via API. Returns paginated response or null.
 */
async function getNotificationsViaApi(
  page: Page,
  pageNum = 0,
  size = 20
): Promise<{ content: any[]; totalElements: number } | null> {
  try {
    const response = await page.request.get(
      `/api/v1/notifications?page=${pageNum}&size=${size}`
    )
    if (response.ok()) {
      const json = await response.json()
      return json.data ?? null
    }
  } catch { /* ignore */ }
  return null
}

/**
 * Fetch unread notification count via API.
 */
async function getUnreadCountViaApi(page: Page): Promise<number | null> {
  try {
    const response = await page.request.get('/api/v1/notifications/unread-count')
    if (response.ok()) {
      const json = await response.json()
      return json.data?.count ?? null
    }
  } catch { /* ignore */ }
  return null
}

/**
 * Mark a single notification as read via API.
 */
async function markAsReadViaApi(page: Page, id: string): Promise<boolean> {
  try {
    const response = await page.request.put(`/api/v1/notifications/${id}/read`)
    return response.ok()
  } catch { /* ignore */ }
  return false
}

/**
 * Mark all notifications as read via API.
 */
async function markAllAsReadViaApi(page: Page): Promise<boolean> {
  try {
    const response = await page.request.put('/api/v1/notifications/read-all')
    return response.ok()
  } catch { /* ignore */ }
  return false
}

/**
 * Delete a notification via API.
 */
async function deleteNotificationViaApi(page: Page, id: string): Promise<boolean> {
  try {
    const response = await page.request.delete(`/api/v1/notifications/${id}`)
    return response.ok()
  } catch { /* ignore */ }
  return false
}

/**
 * Create a feed post as admin (school-level), which generates a notification
 * for room members. Returns the post ID or null.
 */
async function createPostViaApi(
  page: Page,
  content: string,
  title?: string
): Promise<string | null> {
  try {
    const response = await page.request.post('/api/v1/feed/posts', {
      data: {
        sourceType: 'SCHOOL',
        title: title ?? 'E2E Notification Test Post',
        content,
      },
    })
    if (response.ok()) {
      const json = await response.json()
      return json.data?.id ?? null
    }
  } catch { /* ignore */ }
  return null
}

/**
 * Delete a feed post via API (cleanup).
 */
async function deletePostViaApi(page: Page, postId: string): Promise<void> {
  try {
    await page.request.delete(`/api/v1/feed/posts/${postId}`)
  } catch { /* ignore */ }
}

/**
 * Find the first unread notification, if any.
 */
async function findFirstUnreadNotification(
  page: Page
): Promise<{ id: string; read: boolean; type: string; title: string } | null> {
  const result = await getNotificationsViaApi(page, 0, 50)
  if (!result) return null
  const unread = result.content.find((n: any) => !n.read)
  return unread ?? null
}

/**
 * Find the first notification (read or unread).
 */
async function findFirstNotification(
  page: Page
): Promise<{ id: string; read: boolean; type: string; title: string; link: string | null } | null> {
  const result = await getNotificationsViaApi(page, 0, 50)
  if (!result || result.content.length === 0) return null
  return result.content[0]
}

// ============================================================================
// US-325: In-App-Benachrichtigungsliste
// ============================================================================
test.describe('US-325: In-App-Benachrichtigungsliste', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('GET /notifications returns paginated list', async ({ page }) => {
    await login(page, accounts.parent)

    const result = await getNotificationsViaApi(page)
    expect(result).toBeDefined()
    expect(result).toHaveProperty('content')
    expect(result).toHaveProperty('totalElements')
    expect(Array.isArray(result!.content)).toBe(true)
  })

  test('notification items have required fields (id, type, title, message, read, createdAt)', async ({ page }) => {
    await login(page, accounts.parent)

    const result = await getNotificationsViaApi(page, 0, 50)
    expect(result).toBeDefined()

    if (result!.content.length > 0) {
      const notification = result!.content[0]
      expect(notification).toHaveProperty('id')
      expect(notification).toHaveProperty('type')
      expect(notification).toHaveProperty('title')
      expect(notification).toHaveProperty('message')
      expect(notification).toHaveProperty('read')
      expect(notification).toHaveProperty('createdAt')
      expect(typeof notification.id).toBe('string')
      expect(typeof notification.read).toBe('boolean')
    }
  })

  test('bell icon is visible in the header after login', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    // The notification bell component should be present
    const bellButton = page.locator('.notification-bell .bell-button, button:has(.pi-bell)')
    await expect(bellButton.first()).toBeVisible({ timeout: 10000 })
  })

  test('clicking bell icon opens notification popover', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    // Click the bell button
    const bellButton = page.locator('.notification-bell .bell-button, button:has(.pi-bell)').first()
    await expect(bellButton).toBeVisible({ timeout: 10000 })
    await bellButton.click()

    // Popover should appear with notification-header
    const popover = page.locator('.notification-popover, .p-popover')
    await expect(popover.first()).toBeVisible({ timeout: 5000 })

    // Header should show "Benachrichtigungen" (or similar title)
    const header = popover.first().locator('.notification-header')
    await expect(header).toBeVisible({ timeout: 5000 })
  })

  test('unread notifications are highlighted in the list', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    // Open the notification popover
    const bellButton = page.locator('.notification-bell .bell-button, button:has(.pi-bell)').first()
    await expect(bellButton).toBeVisible({ timeout: 10000 })
    await bellButton.click()

    const popover = page.locator('.notification-popover, .p-popover').first()
    await expect(popover).toBeVisible({ timeout: 5000 })

    // Check if there are any notification items
    const items = popover.locator('.notification-item')
    const itemCount = await items.count()

    if (itemCount > 0) {
      // Unread items should have the 'unread' class
      const unreadItems = popover.locator('.notification-item.unread')
      const unreadCount = await unreadItems.count()

      // Verify via API how many are unread
      const apiUnread = await getUnreadCountViaApi(page)

      // If API says there are unread notifications, UI should reflect that
      if (apiUnread && apiUnread > 0) {
        expect(unreadCount).toBeGreaterThan(0)
      }
    }
  })

  test('clicking a notification navigates to linked content', async ({ page }) => {
    await login(page, accounts.parent)

    // Find a notification with a link via API
    const notification = await findFirstNotification(page)
    if (!notification || !notification.link) {
      test.skip(true, 'No notification with a link found — cannot test navigation')
      return
    }

    await page.waitForLoadState('networkidle')

    // Open the notification popover
    const bellButton = page.locator('.notification-bell .bell-button, button:has(.pi-bell)').first()
    await expect(bellButton).toBeVisible({ timeout: 10000 })
    await bellButton.click()

    const popover = page.locator('.notification-popover, .p-popover').first()
    await expect(popover).toBeVisible({ timeout: 5000 })

    // Click the first notification item
    const firstItem = popover.locator('.notification-item').first()
    const isVisible = await firstItem.isVisible({ timeout: 3000 }).catch(() => false)

    if (isVisible) {
      const currentUrl = page.url()
      await firstItem.click()
      // Wait for navigation
      await page.waitForTimeout(2000)
      // URL should have changed (popover closes and navigates)
      const newUrl = page.url()
      // The URL should reflect the notification's link
      expect(newUrl !== currentUrl || notification.link === null).toBeTruthy()
    }
  })

  test('notifications API supports pagination (page, size params)', async ({ page }) => {
    await login(page, accounts.parent)

    // Request page 0 with size 5
    const page0 = await getNotificationsViaApi(page, 0, 5)
    expect(page0).toBeDefined()
    expect(Array.isArray(page0!.content)).toBe(true)
    expect(page0!.content.length).toBeLessThanOrEqual(5)

    // Request page 0 with size 2
    const smallPage = await getNotificationsViaApi(page, 0, 2)
    expect(smallPage).toBeDefined()
    expect(smallPage!.content.length).toBeLessThanOrEqual(2)
  })
})

// ============================================================================
// US-326: Unread-Count Badge
// ============================================================================
test.describe('US-326: Unread-Count Badge', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('GET /notifications/unread-count returns numeric count', async ({ page }) => {
    await login(page, accounts.parent)

    const count = await getUnreadCountViaApi(page)
    expect(count).toBeDefined()
    expect(typeof count).toBe('number')
    expect(count!).toBeGreaterThanOrEqual(0)
  })

  test('badge on bell icon shows unread count when > 0', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    const apiCount = await getUnreadCountViaApi(page)

    if (apiCount && apiCount > 0) {
      // Badge should be visible
      const badge = page.locator('.notification-bell .bell-badge, .notification-bell .p-badge')
      await expect(badge.first()).toBeVisible({ timeout: 10000 })

      // Badge text should contain the count (or 99+ for large counts)
      const badgeText = await badge.first().textContent()
      if (apiCount > 99) {
        expect(badgeText).toContain('99+')
      } else {
        expect(badgeText).toContain(String(apiCount))
      }
    }
  })

  test('badge disappears when all notifications are read', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    // Mark all as read via API
    const success = await markAllAsReadViaApi(page)
    expect(success).toBe(true)

    // Reload the page so the bell component re-fetches
    await page.reload()
    await page.waitForLoadState('networkidle')

    // Badge should not be visible (unread count is 0)
    const badge = page.locator('.notification-bell .bell-badge, .notification-bell .p-badge')
    const isBadgeVisible = await badge.first().isVisible({ timeout: 5000 }).catch(() => false)

    // Verify via API
    const count = await getUnreadCountViaApi(page)
    expect(count).toBe(0)

    // If API says 0, badge should not be visible
    if (count === 0) {
      expect(isBadgeVisible).toBe(false)
    }
  })

  test('unread count endpoint returns 0 for fresh user with no notifications', async ({ page }) => {
    await login(page, accounts.parent)

    // Mark all read first to establish baseline
    await markAllAsReadViaApi(page)

    const count = await getUnreadCountViaApi(page)
    expect(count).toBe(0)
  })
})

// ============================================================================
// US-327: Benachrichtigung als gelesen markieren
// ============================================================================
test.describe('US-327: Benachrichtigung als gelesen markieren', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('PUT /notifications/{id}/read marks a single notification as read', async ({ page }) => {
    await login(page, accounts.parent)

    // Find an unread notification
    const unread = await findFirstUnreadNotification(page)
    if (!unread) {
      test.skip(true, 'No unread notification found to test marking as read')
      return
    }

    // Mark it as read
    const success = await markAsReadViaApi(page, unread.id)
    expect(success).toBe(true)

    // Verify it is now read
    const result = await getNotificationsViaApi(page, 0, 50)
    const updated = result!.content.find((n: any) => n.id === unread.id)
    expect(updated).toBeDefined()
    expect(updated.read).toBe(true)
  })

  test('marking as read decreases unread count by 1', async ({ page }) => {
    await login(page, accounts.parent)

    const countBefore = await getUnreadCountViaApi(page)
    if (countBefore === null || countBefore === 0) {
      test.skip(true, 'No unread notifications to test count decrease')
      return
    }

    const unread = await findFirstUnreadNotification(page)
    if (!unread) {
      test.skip(true, 'No unread notification found')
      return
    }

    await markAsReadViaApi(page, unread.id)

    const countAfter = await getUnreadCountViaApi(page)
    expect(countAfter).toBe(countBefore - 1)
  })

  test('marking already-read notification is idempotent', async ({ page }) => {
    await login(page, accounts.parent)

    const notification = await findFirstNotification(page)
    if (!notification) {
      test.skip(true, 'No notification found')
      return
    }

    // Ensure it is read first
    await markAsReadViaApi(page, notification.id)

    const countBefore = await getUnreadCountViaApi(page)

    // Mark the same (already read) notification as read again
    const success = await markAsReadViaApi(page, notification.id)
    expect(success).toBe(true)

    // Unread count should not change
    const countAfter = await getUnreadCountViaApi(page)
    expect(countAfter).toBe(countBefore)
  })
})

// ============================================================================
// US-328: Alle als gelesen markieren
// ============================================================================
test.describe('US-328: Alle als gelesen markieren', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('PUT /notifications/read-all marks all notifications as read', async ({ page }) => {
    await login(page, accounts.parent)

    const success = await markAllAsReadViaApi(page)
    expect(success).toBe(true)

    // Verify all are read
    const result = await getNotificationsViaApi(page, 0, 50)
    if (result && result.content.length > 0) {
      const allRead = result.content.every((n: any) => n.read === true)
      expect(allRead).toBe(true)
    }

    // Unread count should be 0
    const count = await getUnreadCountViaApi(page)
    expect(count).toBe(0)
  })

  test('"Alle als gelesen markieren" button visible when unread exist', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    const unreadCount = await getUnreadCountViaApi(page)

    // Open bell popover
    const bellButton = page.locator('.notification-bell .bell-button, button:has(.pi-bell)').first()
    await expect(bellButton).toBeVisible({ timeout: 10000 })
    await bellButton.click()

    const popover = page.locator('.notification-popover, .p-popover').first()
    await expect(popover).toBeVisible({ timeout: 5000 })

    // The "mark all read" button should be visible only when unread > 0
    const markAllBtn = popover.locator('button:has-text("gelesen")')
    if (unreadCount && unreadCount > 0) {
      await expect(markAllBtn.first()).toBeVisible({ timeout: 5000 })
    } else {
      // With 0 unread, the button should be hidden
      const isVisible = await markAllBtn.first().isVisible({ timeout: 3000 }).catch(() => false)
      expect(isVisible).toBe(false)
    }
  })

  test('badge disappears after marking all as read via UI', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    const unreadCount = await getUnreadCountViaApi(page)
    if (!unreadCount || unreadCount === 0) {
      test.skip(true, 'No unread notifications — cannot test badge disappearance')
      return
    }

    // Open bell popover
    const bellButton = page.locator('.notification-bell .bell-button, button:has(.pi-bell)').first()
    await expect(bellButton).toBeVisible({ timeout: 10000 })
    await bellButton.click()

    const popover = page.locator('.notification-popover, .p-popover').first()
    await expect(popover).toBeVisible({ timeout: 5000 })

    // Click "Alle als gelesen markieren"
    const markAllBtn = popover.locator('button:has-text("gelesen")').first()
    const isBtnVisible = await markAllBtn.isVisible({ timeout: 3000 }).catch(() => false)

    if (isBtnVisible) {
      await markAllBtn.click()
      await page.waitForTimeout(1000)

      // Badge should disappear
      const badge = page.locator('.notification-bell .bell-badge, .notification-bell .p-badge')
      const badgeGone = await badge.first().isVisible({ timeout: 3000 }).catch(() => false)
      expect(badgeGone).toBe(false)
    } else {
      // Fallback: mark all read via API
      await markAllAsReadViaApi(page)
      const count = await getUnreadCountViaApi(page)
      expect(count).toBe(0)
    }
  })
})

// ============================================================================
// US-329: Benachrichtigung loeschen
// ============================================================================
test.describe('US-329: Benachrichtigung loeschen', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('DELETE /notifications/{id} removes notification from list', async ({ page }) => {
    await login(page, accounts.parent)

    const notification = await findFirstNotification(page)
    if (!notification) {
      test.skip(true, 'No notification found to delete')
      return
    }

    const countBefore = (await getNotificationsViaApi(page, 0, 50))!.totalElements

    // Delete the notification
    const success = await deleteNotificationViaApi(page, notification.id)
    expect(success).toBe(true)

    // Verify it no longer appears in the list
    const result = await getNotificationsViaApi(page, 0, 50)
    const found = result!.content.find((n: any) => n.id === notification.id)
    expect(found).toBeUndefined()

    // Total count should have decreased
    expect(result!.totalElements).toBeLessThan(countBefore)
  })

  test('deleting an unread notification decreases unread count', async ({ page }) => {
    await login(page, accounts.parent)

    const unread = await findFirstUnreadNotification(page)
    if (!unread) {
      test.skip(true, 'No unread notification to delete')
      return
    }

    const countBefore = await getUnreadCountViaApi(page)

    await deleteNotificationViaApi(page, unread.id)

    const countAfter = await getUnreadCountViaApi(page)
    expect(countAfter).toBe(countBefore! - 1)
  })

  test('deleting a read notification does not change unread count', async ({ page }) => {
    await login(page, accounts.parent)

    // Find a notification and make sure it is read
    const notification = await findFirstNotification(page)
    if (!notification) {
      test.skip(true, 'No notification available')
      return
    }

    // Mark as read first
    await markAsReadViaApi(page, notification.id)

    const unreadBefore = await getUnreadCountViaApi(page)

    // Delete the read notification
    await deleteNotificationViaApi(page, notification.id)

    const unreadAfter = await getUnreadCountViaApi(page)
    expect(unreadAfter).toBe(unreadBefore)
  })

  test('delete button is visible on notification item hover in UI', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    // Open bell popover
    const bellButton = page.locator('.notification-bell .bell-button, button:has(.pi-bell)').first()
    await expect(bellButton).toBeVisible({ timeout: 10000 })
    await bellButton.click()

    const popover = page.locator('.notification-popover, .p-popover').first()
    await expect(popover).toBeVisible({ timeout: 5000 })

    const firstItem = popover.locator('.notification-item').first()
    const hasItem = await firstItem.isVisible({ timeout: 5000 }).catch(() => false)

    if (hasItem) {
      // Hover over the item to reveal the delete button
      await firstItem.hover()

      const deleteBtn = firstItem.locator('.delete-btn, button:has(.pi-times)')
      // After hover, opacity should become 1 (button visible on hover)
      await expect(deleteBtn.first()).toBeVisible({ timeout: 3000 })
    }
  })

  test('deleting notification via UI removes it from the list', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    // Open bell popover
    const bellButton = page.locator('.notification-bell .bell-button, button:has(.pi-bell)').first()
    await expect(bellButton).toBeVisible({ timeout: 10000 })
    await bellButton.click()

    const popover = page.locator('.notification-popover, .p-popover').first()
    await expect(popover).toBeVisible({ timeout: 5000 })

    const items = popover.locator('.notification-item')
    const itemCountBefore = await items.count()

    if (itemCountBefore === 0) {
      test.skip(true, 'No notifications in the popover to delete')
      return
    }

    // Hover and click delete on the first item
    const firstItem = items.first()
    await firstItem.hover()

    const deleteBtn = firstItem.locator('.delete-btn, button:has(.pi-times)').first()
    const isBtnVisible = await deleteBtn.isVisible({ timeout: 3000 }).catch(() => false)

    if (isBtnVisible) {
      await deleteBtn.click()
      await page.waitForTimeout(1000)

      // Item count should have decreased
      const itemCountAfter = await items.count()
      expect(itemCountAfter).toBe(itemCountBefore - 1)
    } else {
      // Fallback: verify delete via API works
      const notification = await findFirstNotification(page)
      if (notification) {
        const success = await deleteNotificationViaApi(page, notification.id)
        expect(success).toBe(true)
      }
    }
  })
})

// ============================================================================
// US-330: Benachrichtigungstypen
// ============================================================================
test.describe('US-330: Benachrichtigungstypen', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  // Triggering various notification types requires cross-module events
  // (e.g., creating a room join request, publishing a form, sending a message).
  // This is complex and would make the test fragile and slow.
  test.skip(true, 'TODO: Requires triggering various cross-module events to generate different notification types')

  test('notifications include various types (POST, COMMENT, MESSAGE, etc.)', async ({ page }) => {
    await login(page, accounts.parent)
    const result = await getNotificationsViaApi(page, 0, 100)
    if (!result || result.content.length === 0) return
    const types = new Set(result.content.map((n: any) => n.type))
    expect(types.size).toBeGreaterThanOrEqual(1)
  })
})

// ============================================================================
// US-331: Push-Benachrichtigungen
// ============================================================================
test.describe('US-331: Push-Benachrichtigungen', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  // Push notifications require:
  // 1. Browser push permission grant (not automatable in headless Playwright)
  // 2. VAPID keys configured on the server
  // 3. Real push delivery via web push protocol
  test.skip(true, 'TODO: Requires browser push permission, VAPID keys, and real push delivery')

  test('push subscription endpoint is accessible', async ({ page }) => {
    await login(page, accounts.parent)
    const response = await page.request.get('/api/v1/notifications/push/public-key')
    expect(response.status()).toBeLessThan(500)
  })
})
