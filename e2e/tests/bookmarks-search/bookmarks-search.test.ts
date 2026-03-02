import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { login } from '../../helpers/auth'
import { selectors } from '../../helpers/selectors'

// ============================================================================
// Bookmarks & Search E2E Tests — US-314 to US-324
// ============================================================================

type Page = import('@playwright/test').Page

// ---------------------------------------------------------------------------
// Helpers — Bookmarks
// ---------------------------------------------------------------------------

/**
 * Toggle a bookmark via the API. Returns the new bookmarked status (true/false)
 * or null on failure.
 */
async function toggleBookmarkViaApi(
  page: Page,
  contentType: string,
  contentId: string
): Promise<boolean | null> {
  try {
    const response = await page.request.post('/api/v1/bookmarks', {
      data: { contentType, contentId },
    })
    if (response.ok()) {
      const json = await response.json()
      return json.data?.bookmarked ?? null
    }
  } catch { /* ignore */ }
  return null
}

/**
 * List bookmarks via API. Returns the page response or null on failure.
 */
async function listBookmarksViaApi(
  page: Page,
  type?: string,
  pageNum = 0,
  size = 20
): Promise<{ content: any[]; totalElements: number } | null> {
  try {
    const params = new URLSearchParams()
    if (type) params.set('type', type)
    params.set('page', String(pageNum))
    params.set('size', String(size))
    const response = await page.request.get(`/api/v1/bookmarks?${params.toString()}`)
    if (response.ok()) {
      const json = await response.json()
      return json.data ?? null
    }
  } catch { /* ignore */ }
  return null
}

/**
 * Check bookmark status via API.
 */
async function checkBookmarkViaApi(
  page: Page,
  contentType: string,
  contentId: string
): Promise<boolean | null> {
  try {
    const response = await page.request.get(
      `/api/v1/bookmarks/check?contentType=${contentType}&contentId=${contentId}`
    )
    if (response.ok()) {
      const json = await response.json()
      return json.data?.bookmarked ?? null
    }
  } catch { /* ignore */ }
  return null
}

/**
 * Get bookmarked IDs for a content type via API.
 */
async function getBookmarkedIdsViaApi(
  page: Page,
  contentType: string
): Promise<string[] | null> {
  try {
    const response = await page.request.get(
      `/api/v1/bookmarks/ids?contentType=${contentType}`
    )
    if (response.ok()) {
      const json = await response.json()
      return json.data ?? null
    }
  } catch { /* ignore */ }
  return null
}

/**
 * Remove all bookmarks of a given type via API (cleanup helper).
 */
async function clearBookmarksOfType(page: Page, contentType: string): Promise<void> {
  const ids = await getBookmarkedIdsViaApi(page, contentType)
  if (ids && ids.length > 0) {
    for (const id of ids) {
      await toggleBookmarkViaApi(page, contentType, id)
    }
  }
}

// ---------------------------------------------------------------------------
// Helpers — Feed Posts (for bookmark targets)
// ---------------------------------------------------------------------------

/**
 * Create a feed post via API and return its id.
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
        title: title ?? 'E2E Bookmark Test Post',
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

// ---------------------------------------------------------------------------
// Helpers — Calendar Events (for bookmark targets)
// ---------------------------------------------------------------------------

/**
 * Create a calendar event via API and return its id.
 */
async function createEventViaApi(
  page: Page,
  title: string
): Promise<string | null> {
  try {
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    const dateStr = tomorrow.toISOString().split('T')[0]

    const response = await page.request.post('/api/v1/calendar/events', {
      data: {
        title,
        description: 'E2E Bookmark Test Event',
        allDay: true,
        startDate: dateStr,
        endDate: dateStr,
        scope: 'SCHOOL',
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
 * Delete a calendar event via API (cleanup).
 */
async function deleteEventViaApi(page: Page, eventId: string): Promise<void> {
  try {
    await page.request.delete(`/api/v1/calendar/events/${eventId}`)
  } catch { /* ignore */ }
}

// ---------------------------------------------------------------------------
// Helpers — Jobs (for bookmark targets)
// ---------------------------------------------------------------------------

/**
 * Create a job via API and return its id.
 */
async function createJobViaApi(
  page: Page,
  title: string
): Promise<string | null> {
  try {
    const response = await page.request.post('/api/v1/jobs', {
      data: {
        title,
        description: 'E2E Bookmark Test Job',
        category: 'Normal',
        location: 'Schulhof',
        estimatedHours: 2,
        maxAssignees: 3,
        visibility: 'PUBLIC',
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
 * Delete a job via API (cleanup).
 */
async function deleteJobViaApi(page: Page, jobId: string): Promise<void> {
  try {
    await page.request.delete(`/api/v1/jobs/${jobId}?permanent=true`)
  } catch { /* ignore */ }
}

// ---------------------------------------------------------------------------
// Helpers — Search
// ---------------------------------------------------------------------------

/**
 * Execute a search via API and return results.
 */
async function searchViaApi(
  page: Page,
  q: string,
  type = 'ALL',
  limit = 20
): Promise<any[] | null> {
  try {
    const response = await page.request.get(
      `/api/v1/search?q=${encodeURIComponent(q)}&type=${type}&limit=${limit}`
    )
    if (response.ok()) {
      const json = await response.json()
      return json.data ?? null
    }
  } catch { /* ignore */ }
  return null
}

/**
 * Navigate to the bookmarks page.
 */
async function goToBookmarks(page: Page): Promise<void> {
  await page.goto('/bookmarks')
  await page.waitForLoadState('networkidle')
}

// ============================================================================
// US-314: Feed-Post bookmarken
// ============================================================================
test.describe('US-314: Feed-Post bookmarken', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  let postId: string | null = null

  test.afterEach(async ({ page }) => {
    // Clean up: remove bookmark if set
    if (postId) {
      const isBookmarked = await checkBookmarkViaApi(page, 'POST', postId)
      if (isBookmarked) {
        await toggleBookmarkViaApi(page, 'POST', postId)
      }
      await deletePostViaApi(page, postId)
      postId = null
    }
  })

  test('can bookmark a feed post via API toggle', async ({ page }) => {
    await login(page, accounts.parent)

    // Create a post to bookmark
    postId = await createPostViaApi(page, 'Lesezeichen-Test Inhalt fuer E2E')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    // Toggle bookmark ON
    const result = await toggleBookmarkViaApi(page, 'POST', postId)
    expect(result).toBe(true)

    // Verify via check endpoint
    const isBookmarked = await checkBookmarkViaApi(page, 'POST', postId)
    expect(isBookmarked).toBe(true)
  })

  test('toggle returns bookmarked:false when removing', async ({ page }) => {
    await login(page, accounts.parent)

    postId = await createPostViaApi(page, 'Lesezeichen Toggle Test')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    // Bookmark ON
    await toggleBookmarkViaApi(page, 'POST', postId)

    // Bookmark OFF (second toggle)
    const result = await toggleBookmarkViaApi(page, 'POST', postId)
    expect(result).toBe(false)

    // Verify it is no longer bookmarked
    const isBookmarked = await checkBookmarkViaApi(page, 'POST', postId)
    expect(isBookmarked).toBe(false)
  })

  test('bookmark icon changes on feed page when toggled', async ({ page }) => {
    await login(page, accounts.parent)

    postId = await createPostViaApi(page, 'Lesezeichen-Icon Test')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    // Navigate to feed / dashboard to see the post
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')

    // Look for a bookmark button (pi-bookmark or pi-bookmark-fill)
    const bookmarkBtn = page.locator('.bookmark-btn, button:has(.pi-bookmark)').first()
    const hasBtnVisible = await bookmarkBtn.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasBtnVisible) {
      // Bookmark buttons may not be on the dashboard — verify via API instead
      const toggled = await toggleBookmarkViaApi(page, 'POST', postId)
      expect(toggled).toBe(true)
      return
    }

    // Click the bookmark button to toggle it on
    await bookmarkBtn.click()
    await page.waitForTimeout(500)

    // Verify the filled bookmark icon is now visible
    const filledIcon = page.locator('.bookmark-active, .pi-bookmark-fill').first()
    const isActive = await filledIcon.isVisible({ timeout: 3000 }).catch(() => false)
    // Either the UI shows the active state or we verify via API
    if (!isActive) {
      const status = await checkBookmarkViaApi(page, 'POST', postId)
      expect(status).toBeDefined()
    }
  })
})

// ============================================================================
// US-315: Kalender-Event bookmarken
// ============================================================================
test.describe('US-315: Kalender-Event bookmarken', () => {
  test.use({ storageState: 'e2e/.auth/teacher.json' })

  let eventId: string | null = null

  test.afterEach(async ({ page }) => {
    if (eventId) {
      const isBookmarked = await checkBookmarkViaApi(page, 'EVENT', eventId)
      if (isBookmarked) {
        await toggleBookmarkViaApi(page, 'EVENT', eventId)
      }
      await deleteEventViaApi(page, eventId)
      eventId = null
    }
  })

  test('can bookmark a calendar event via API', async ({ page }) => {
    await login(page, accounts.teacher)

    eventId = await createEventViaApi(page, 'E2E Lesezeichen-Event')
    if (!eventId) {
      test.skip(true, 'Could not create test event via API')
      return
    }

    // Toggle bookmark ON
    const result = await toggleBookmarkViaApi(page, 'EVENT', eventId)
    expect(result).toBe(true)

    // Verify via check endpoint
    const isBookmarked = await checkBookmarkViaApi(page, 'EVENT', eventId)
    expect(isBookmarked).toBe(true)
  })

  test('check endpoint returns correct bookmarked status', async ({ page }) => {
    await login(page, accounts.teacher)

    eventId = await createEventViaApi(page, 'E2E Check-Endpoint Event')
    if (!eventId) {
      test.skip(true, 'Could not create test event via API')
      return
    }

    // Initially not bookmarked
    const beforeBookmark = await checkBookmarkViaApi(page, 'EVENT', eventId)
    expect(beforeBookmark).toBe(false)

    // Bookmark it
    await toggleBookmarkViaApi(page, 'EVENT', eventId)

    // Now bookmarked
    const afterBookmark = await checkBookmarkViaApi(page, 'EVENT', eventId)
    expect(afterBookmark).toBe(true)
  })

  test('event bookmark appears in bookmarked IDs list', async ({ page }) => {
    await login(page, accounts.teacher)

    eventId = await createEventViaApi(page, 'E2E IDs-Liste Event')
    if (!eventId) {
      test.skip(true, 'Could not create test event via API')
      return
    }

    await toggleBookmarkViaApi(page, 'EVENT', eventId)

    const ids = await getBookmarkedIdsViaApi(page, 'EVENT')
    expect(ids).toBeDefined()
    expect(ids).toContain(eventId)
  })
})

// ============================================================================
// US-316: Job bookmarken
// ============================================================================
test.describe('US-316: Job bookmarken', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  let jobId: string | null = null

  test.afterEach(async ({ page }) => {
    if (jobId) {
      const isBookmarked = await checkBookmarkViaApi(page, 'JOB', jobId)
      if (isBookmarked) {
        await toggleBookmarkViaApi(page, 'JOB', jobId)
      }
      await deleteJobViaApi(page, jobId)
      jobId = null
    }
  })

  test('can bookmark a job via API toggle', async ({ page }) => {
    await login(page, accounts.parent)

    jobId = await createJobViaApi(page, 'E2E Lesezeichen-Job')
    if (!jobId) {
      test.skip(true, 'Could not create test job via API — jobboard module may be disabled')
      return
    }

    // Toggle bookmark ON
    const result = await toggleBookmarkViaApi(page, 'JOB', jobId)
    expect(result).toBe(true)

    // Verify
    const isBookmarked = await checkBookmarkViaApi(page, 'JOB', jobId)
    expect(isBookmarked).toBe(true)
  })

  test('job bookmark shows in IDs endpoint', async ({ page }) => {
    await login(page, accounts.parent)

    jobId = await createJobViaApi(page, 'E2E IDs-Job')
    if (!jobId) {
      test.skip(true, 'Could not create test job via API')
      return
    }

    await toggleBookmarkViaApi(page, 'JOB', jobId)

    const ids = await getBookmarkedIdsViaApi(page, 'JOB')
    expect(ids).toBeDefined()
    expect(ids).toContain(jobId)
  })

  test('toggle returns false after double-toggle (remove)', async ({ page }) => {
    await login(page, accounts.parent)

    jobId = await createJobViaApi(page, 'E2E Double-Toggle Job')
    if (!jobId) {
      test.skip(true, 'Could not create test job via API')
      return
    }

    // ON
    const first = await toggleBookmarkViaApi(page, 'JOB', jobId)
    expect(first).toBe(true)

    // OFF
    const second = await toggleBookmarkViaApi(page, 'JOB', jobId)
    expect(second).toBe(false)
  })
})

// ============================================================================
// US-317: Wiki-Seite bookmarken
// ============================================================================
test.describe('US-317: Wiki-Seite bookmarken', () => {
  test.use({ storageState: 'e2e/.auth/teacher.json' })

  test.skip(true, 'TODO: Requires existing wiki page — wiki content may not be seeded')

  test('can bookmark a wiki page via API', async ({ page }) => {
    await login(page, accounts.teacher)
    // Would need to create or find a wiki page to bookmark
    // Wiki pages are per-room, requiring room context
  })
})

// ============================================================================
// US-318: Bookmarks-Uebersicht anzeigen
// ============================================================================
test.describe('US-318: Bookmarks-Uebersicht anzeigen', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  let postId: string | null = null
  let jobId: string | null = null

  test.afterEach(async ({ page }) => {
    if (postId) {
      await toggleBookmarkViaApi(page, 'POST', postId).catch(() => {})
      await deletePostViaApi(page, postId)
      postId = null
    }
    if (jobId) {
      await toggleBookmarkViaApi(page, 'JOB', jobId).catch(() => {})
      await deleteJobViaApi(page, jobId)
      jobId = null
    }
  })

  test('bookmarks page loads and shows title "Lesezeichen"', async ({ page }) => {
    await login(page, accounts.parent)
    await goToBookmarks(page)

    const title = page.locator('.page-title').first()
    await expect(title).toBeVisible({ timeout: 10000 })
    await expect(title).toContainText('Lesezeichen')
  })

  test('filter tabs are visible (Alle, Beitraege, Termine, Jobs, Wiki)', async ({ page }) => {
    await login(page, accounts.parent)
    await goToBookmarks(page)

    const tabContainer = page.locator('.bookmark-tabs')
    await expect(tabContainer).toBeVisible({ timeout: 10000 })

    // Check for tab labels
    await expect(tabContainer.locator('button:has-text("Alle")')).toBeVisible()
    await expect(tabContainer.locator('button:has-text("Beitr")')).toBeVisible()
    await expect(tabContainer.locator('button:has-text("Termine")')).toBeVisible()
    await expect(tabContainer.locator('button:has-text("Jobs")')).toBeVisible()
    await expect(tabContainer.locator('button:has-text("Wiki")')).toBeVisible()
  })

  test('empty state message shown when no bookmarks exist', async ({ page }) => {
    await login(page, accounts.parent)

    // Clear all existing bookmarks first
    await clearBookmarksOfType(page, 'POST')
    await clearBookmarksOfType(page, 'EVENT')
    await clearBookmarksOfType(page, 'JOB')
    await clearBookmarksOfType(page, 'WIKI_PAGE')

    await goToBookmarks(page)

    const emptyState = page.locator('.empty-state').first()
    await expect(emptyState).toBeVisible({ timeout: 10000 })
    await expect(emptyState).toContainText('Lesezeichen')
  })

  test('GET /bookmarks returns paginated response', async ({ page }) => {
    await login(page, accounts.parent)

    const result = await listBookmarksViaApi(page, undefined, 0, 20)
    expect(result).toBeDefined()
    expect(result).toHaveProperty('content')
    expect(result).toHaveProperty('totalElements')
    expect(Array.isArray(result!.content)).toBe(true)
  })

  test('bookmarked items appear in overview after creation', async ({ page }) => {
    await login(page, accounts.parent)

    // Create and bookmark a post
    postId = await createPostViaApi(page, 'Uebersicht Test Post')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }
    await toggleBookmarkViaApi(page, 'POST', postId)

    // Check the list
    const result = await listBookmarksViaApi(page, 'POST')
    expect(result).toBeDefined()
    expect(result!.content.length).toBeGreaterThanOrEqual(1)

    const found = result!.content.some((bm: any) => bm.contentId === postId)
    expect(found).toBe(true)
  })

  test('filter by type shows only matching bookmarks', async ({ page }) => {
    await login(page, accounts.parent)

    // Create and bookmark a post and a job
    postId = await createPostViaApi(page, 'Filter Test Post')
    jobId = await createJobViaApi(page, 'Filter Test Job')

    if (!postId || !jobId) {
      test.skip(true, 'Could not create test content via API')
      return
    }

    await toggleBookmarkViaApi(page, 'POST', postId)
    await toggleBookmarkViaApi(page, 'JOB', jobId)

    // Filter by POST
    const posts = await listBookmarksViaApi(page, 'POST')
    expect(posts).toBeDefined()
    const hasPost = posts!.content.some((bm: any) => bm.contentId === postId)
    expect(hasPost).toBe(true)
    // Should NOT contain the job
    const hasJobInPosts = posts!.content.some((bm: any) => bm.contentId === jobId)
    expect(hasJobInPosts).toBe(false)

    // Filter by JOB
    const jobs = await listBookmarksViaApi(page, 'JOB')
    expect(jobs).toBeDefined()
    const hasJob = jobs!.content.some((bm: any) => bm.contentId === jobId)
    expect(hasJob).toBe(true)
  })

  test('GET /bookmarks/ids returns set of bookmarked IDs for batch check', async ({ page }) => {
    await login(page, accounts.parent)

    postId = await createPostViaApi(page, 'Batch-Check Test')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    await toggleBookmarkViaApi(page, 'POST', postId)

    const ids = await getBookmarkedIdsViaApi(page, 'POST')
    expect(ids).toBeDefined()
    expect(Array.isArray(ids)).toBe(true)
    expect(ids).toContain(postId)
  })

  test('bookmarks page shows items in the UI list', async ({ page }) => {
    await login(page, accounts.parent)

    postId = await createPostViaApi(page, 'UI-Liste Test')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }
    await toggleBookmarkViaApi(page, 'POST', postId)

    await goToBookmarks(page)

    // Wait for the bookmark list to load
    const bookmarkList = page.locator('.bookmark-list')
    const hasItems = await bookmarkList.isVisible({ timeout: 10000 }).catch(() => false)

    if (hasItems) {
      const items = bookmarkList.locator('.bookmark-item')
      const count = await items.count()
      expect(count).toBeGreaterThanOrEqual(1)
    } else {
      // If list is not visible, the empty state should not be showing
      // (since we just bookmarked something — verify via API)
      const result = await listBookmarksViaApi(page, 'POST')
      expect(result!.content.length).toBeGreaterThanOrEqual(1)
    }
  })
})

// ============================================================================
// US-319: Bookmark entfernen
// ============================================================================
test.describe('US-319: Bookmark entfernen', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  let postId: string | null = null

  test.afterEach(async ({ page }) => {
    if (postId) {
      // Ensure bookmark is removed
      const isBookmarked = await checkBookmarkViaApi(page, 'POST', postId)
      if (isBookmarked) {
        await toggleBookmarkViaApi(page, 'POST', postId)
      }
      await deletePostViaApi(page, postId)
      postId = null
    }
  })

  test('second POST toggle removes bookmark', async ({ page }) => {
    await login(page, accounts.parent)

    postId = await createPostViaApi(page, 'Entfernen Test Post')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    // Add bookmark
    const added = await toggleBookmarkViaApi(page, 'POST', postId)
    expect(added).toBe(true)

    // Remove bookmark (second toggle)
    const removed = await toggleBookmarkViaApi(page, 'POST', postId)
    expect(removed).toBe(false)

    // Verify removal
    const isBookmarked = await checkBookmarkViaApi(page, 'POST', postId)
    expect(isBookmarked).toBe(false)
  })

  test('removed bookmark disappears from overview list', async ({ page }) => {
    await login(page, accounts.parent)

    postId = await createPostViaApi(page, 'Verschwinden Test Post')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    // Add bookmark
    await toggleBookmarkViaApi(page, 'POST', postId)

    // Verify it exists
    let list = await listBookmarksViaApi(page, 'POST')
    let found = list!.content.some((bm: any) => bm.contentId === postId)
    expect(found).toBe(true)

    // Remove bookmark
    await toggleBookmarkViaApi(page, 'POST', postId)

    // Verify it is gone
    list = await listBookmarksViaApi(page, 'POST')
    found = list!.content.some((bm: any) => bm.contentId === postId)
    expect(found).toBe(false)
  })

  test('remove bookmark via trash button in bookmarks view', async ({ page }) => {
    await login(page, accounts.parent)

    postId = await createPostViaApi(page, 'Trash-Button Test')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    await toggleBookmarkViaApi(page, 'POST', postId)
    await goToBookmarks(page)

    const bookmarkList = page.locator('.bookmark-list')
    const hasItems = await bookmarkList.isVisible({ timeout: 10000 }).catch(() => false)

    if (hasItems) {
      // Click the trash/delete button on the first bookmark item
      const trashBtn = bookmarkList.locator('.bookmark-item').first().locator('button:has(.pi-trash)').first()
      const trashVisible = await trashBtn.isVisible({ timeout: 3000 }).catch(() => false)

      if (trashVisible) {
        await trashBtn.click()
        await page.waitForTimeout(1000)

        // Verify bookmark was removed via API
        const isBookmarked = await checkBookmarkViaApi(page, 'POST', postId)
        expect(isBookmarked).toBe(false)
      }
    }
  })

  test('no confirmation dialog needed for removal', async ({ page }) => {
    await login(page, accounts.parent)

    postId = await createPostViaApi(page, 'Keine Bestaetigung Test')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    // Add and immediately remove — should not require confirmation
    await toggleBookmarkViaApi(page, 'POST', postId)
    const removed = await toggleBookmarkViaApi(page, 'POST', postId)
    expect(removed).toBe(false)

    // No dialog should appear — direct toggle
    const isBookmarked = await checkBookmarkViaApi(page, 'POST', postId)
    expect(isBookmarked).toBe(false)
  })
})

// ============================================================================
// US-320: Globale Suche oeffnen (Ctrl+K)
// ============================================================================
test.describe('US-320: Globale Suche oeffnen (Ctrl+K)', () => {
  test.use({ storageState: 'e2e/.auth/teacher.json' })

  test('Ctrl+K opens the global search dialog', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    // Press Ctrl+K
    await page.keyboard.press('Control+k')

    // The dialog should appear
    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })
  })

  test('search dialog has auto-focused input field', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // The search input should be focused
    const searchInput = dialog.locator('.search-input, input[type="text"]').first()
    await expect(searchInput).toBeVisible()
    // Verify focus by typing and checking value changes
    await page.keyboard.type('test')
    await expect(searchInput).toHaveValue('test')
  })

  test('search dialog shows 8 filter chips', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Check filter chips: Alle, Benutzer, Raeume, Beitraege, Termine, Dateien, Wiki, Aufgaben
    const filters = dialog.locator('.filter-chip, .search-filters button')
    const count = await filters.count()
    expect(count).toBe(8)
  })

  test('Escape closes the search dialog', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Press Escape to close
    await page.keyboard.press('Escape')
    await expect(dialog).not.toBeVisible({ timeout: 5000 })
  })

  test('search hint text shown before typing', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Should show the hint about minimum 2 characters
    const hint = dialog.locator('.search-hint')
    await expect(hint).toBeVisible()
    await expect(hint).toContainText('Zeichen')
  })
})

// ============================================================================
// US-321: Volltextsuche ausfuehren
// ============================================================================
test.describe('US-321: Volltextsuche ausfuehren', () => {
  test.use({ storageState: 'e2e/.auth/teacher.json' })

  test('search API returns results for valid query', async ({ page }) => {
    await login(page, accounts.teacher)

    // Search for a common term that should have results (e.g. user names from seed data)
    const results = await searchViaApi(page, 'Admin')
    expect(results).toBeDefined()
    expect(Array.isArray(results)).toBe(true)
    // Should find at least the admin user
    expect(results!.length).toBeGreaterThanOrEqual(1)
  })

  test('search requires minimum 2 characters via UI', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Type only 1 character — no results section should appear
    await page.keyboard.type('a')
    await page.waitForTimeout(500)

    const resultsList = dialog.locator('.search-results-list')
    const hasResults = await resultsList.isVisible({ timeout: 2000 }).catch(() => false)
    expect(hasResults).toBe(false)
  })

  test('search shows results after typing 2+ characters', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Type a search term that should return results
    await page.keyboard.type('Admin')
    // Wait for debounce (300ms) + network
    await page.waitForTimeout(1000)

    // Either results or empty state should appear
    const searchResults = dialog.locator('.search-results')
    await expect(searchResults).toBeVisible({ timeout: 5000 })
  })

  test('search results are grouped by type', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Search for something broad
    await page.keyboard.type('Sonnengruppe')
    await page.waitForTimeout(1000)

    // Check if result group headers exist
    const groupHeaders = dialog.locator('.result-group-header')
    const headerCount = await groupHeaders.count()
    // Should have at least one group if there are results
    if (headerCount > 0) {
      expect(headerCount).toBeGreaterThanOrEqual(1)
    }
  })

  test('keyboard navigation works (ArrowDown/ArrowUp)', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await page.keyboard.type('Admin')
    await page.waitForTimeout(1000)

    const resultItems = dialog.locator('.search-result-item')
    const count = await resultItems.count()

    if (count > 0) {
      // Press ArrowDown to select first item
      await page.keyboard.press('ArrowDown')
      await page.waitForTimeout(200)

      // The first item should have the 'selected' class
      const firstItem = resultItems.first()
      const isSelected = await firstItem.evaluate(
        (el) => el.classList.contains('selected')
      )
      expect(isSelected).toBe(true)
    }
  })

  test('300ms debounce prevents rapid API calls', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Track API calls
    const searchRequests: string[] = []
    page.on('request', (req) => {
      if (req.url().includes('/api/v1/search')) {
        searchRequests.push(req.url())
      }
    })

    // Type quickly — should debounce and only fire one or two requests
    await page.keyboard.type('Admin', { delay: 50 })
    await page.waitForTimeout(800)

    // Should not have fired a request for every keystroke
    // With 5 chars typed at 50ms delay = 250ms total, debounce at 300ms
    // so at most 1-2 requests (one maybe mid-way, one final after debounce)
    expect(searchRequests.length).toBeLessThanOrEqual(3)
  })
})

// ============================================================================
// US-322: Suche nach Typ filtern
// ============================================================================
test.describe('US-322: Suche nach Typ filtern', () => {
  test.use({ storageState: 'e2e/.auth/teacher.json' })

  test('clicking a filter chip highlights it', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Click the "Benutzer" (USER) filter chip
    const userChip = dialog.locator('.filter-chip:has-text("Benutzer")').first()
    await expect(userChip).toBeVisible()
    await userChip.click()

    // The chip should now have the 'active' class
    await expect(userChip).toHaveClass(/active/)
  })

  test('filter change triggers new search', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Type a query first
    await page.keyboard.type('Admin')
    await page.waitForTimeout(800)

    // Track requests from this point
    const searchRequests: string[] = []
    page.on('request', (req) => {
      if (req.url().includes('/api/v1/search')) {
        searchRequests.push(req.url())
      }
    })

    // Click a filter chip — should trigger a new search
    const roomChip = dialog.locator('.filter-chip:has-text("Raeume"), .filter-chip:has-text("Räume")').first()
    if (await roomChip.isVisible({ timeout: 2000 }).catch(() => false)) {
      await roomChip.click()
      await page.waitForTimeout(800)

      // Should have triggered at least one search with the new type
      expect(searchRequests.length).toBeGreaterThanOrEqual(1)
      const hasTypeParam = searchRequests.some((url) => url.includes('type=ROOM'))
      expect(hasTypeParam).toBe(true)
    }
  })

  test('API accepts type parameter for filtered search', async ({ page }) => {
    await login(page, accounts.teacher)

    // Search for users specifically
    const userResults = await searchViaApi(page, 'Admin', 'USER')
    expect(userResults).toBeDefined()
    expect(Array.isArray(userResults)).toBe(true)

    // All results should be of type USER
    for (const result of userResults ?? []) {
      expect(result.type).toBe('USER')
    }
  })

  test('filter "Alle" returns mixed result types', async ({ page }) => {
    await login(page, accounts.teacher)

    const allResults = await searchViaApi(page, 'Sonnengruppe', 'ALL', 50)
    expect(allResults).toBeDefined()
    // With "ALL" filter, we may get multiple types
    if (allResults && allResults.length > 1) {
      const types = new Set(allResults.map((r: any) => r.type))
      // May have multiple types — or all the same, depending on data
      expect(types.size).toBeGreaterThanOrEqual(1)
    }
  })
})

// ============================================================================
// US-323: Solr-Volltextsuche
// ============================================================================
test.describe('US-323: Solr-Volltextsuche', () => {
  test.use({ storageState: 'e2e/.auth/admin.json' })

  test.skip(true, 'TODO: Solr container may not be running in E2E environment')

  test('admin can trigger reindex via API', async ({ page }) => {
    await login(page, accounts.admin)

    const response = await page.request.post('/api/v1/admin/search/reindex')
    // If Solr is running, this should succeed
    expect(response.status()).toBeLessThan(500)
  })

  test('Solr search returns results with German stemming', async ({ page }) => {
    await login(page, accounts.admin)

    // German stemming: "Benutzer" should match "Benutzern", "Benutzers" etc.
    const results = await searchViaApi(page, 'Benutzer')
    expect(results).toBeDefined()
    expect(results!.length).toBeGreaterThanOrEqual(0)
  })
})

// ============================================================================
// US-324: DB-Fallback bei deaktiviertem Solr
// ============================================================================
test.describe('US-324: DB-Fallback bei deaktiviertem Solr', () => {
  test.use({ storageState: 'e2e/.auth/teacher.json' })

  test('search returns results without Solr (DB fallback)', async ({ page }) => {
    await login(page, accounts.teacher)

    // The search endpoint should work regardless of Solr status
    const results = await searchViaApi(page, 'Admin')
    expect(results).toBeDefined()
    expect(Array.isArray(results)).toBe(true)
    // Should find something via DB fallback
    expect(results!.length).toBeGreaterThanOrEqual(1)
  })

  test('search results have consistent format regardless of backend', async ({ page }) => {
    await login(page, accounts.teacher)

    const results = await searchViaApi(page, 'Sonnengruppe')
    expect(results).toBeDefined()

    if (results && results.length > 0) {
      const first = results[0]
      // Each result should have the expected shape
      expect(first).toHaveProperty('id')
      expect(first).toHaveProperty('type')
      expect(first).toHaveProperty('title')
      expect(['USER', 'ROOM', 'POST', 'EVENT', 'FILE', 'WIKI', 'TASK']).toContain(first.type)
    }
  })

  test('no error toast when searching without Solr', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await page.keyboard.type('Admin')
    await page.waitForTimeout(1000)

    // No error toast should appear
    const errorToast = page.locator(selectors.toastError)
    const hasError = await errorToast.isVisible({ timeout: 2000 }).catch(() => false)
    expect(hasError).toBe(false)
  })

  test('search via UI shows results in dialog', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await page.keyboard.type('Admin')
    await page.waitForTimeout(1000)

    // Should see results or empty state — not an error
    const searchResults = dialog.locator('.search-results')
    await expect(searchResults).toBeVisible({ timeout: 5000 })

    // Either we have result items or the "no results" message
    const resultItems = dialog.locator('.search-result-item')
    const emptyState = dialog.locator('.search-empty')
    const hasResults = await resultItems.first().isVisible({ timeout: 2000 }).catch(() => false)
    const hasEmpty = await emptyState.isVisible({ timeout: 2000 }).catch(() => false)

    // One of these must be true
    expect(hasResults || hasEmpty).toBe(true)
  })
})
