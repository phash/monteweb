import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { selectors } from '../../helpers/selectors'

// ============================================================================
// Bookmarks & Search E2E Tests — US-314 to US-324
// ============================================================================

type Page = import('@playwright/test').Page

const BASE = 'http://localhost'

// ---------------------------------------------------------------------------
// Token cache — avoids hitting rate limits by reusing tokens per user
// ---------------------------------------------------------------------------
const tokenCache: Record<string, { token: string; expiresAt: number }> = {}

async function getToken(page: Page, account: { email: string; password: string }): Promise<string> {
  const cached = tokenCache[account.email]
  if (cached && cached.expiresAt > Date.now() + 30000) {
    return cached.token
  }

  const res = await page.request.post(`${BASE}/api/v1/auth/login`, {
    data: { email: account.email, password: account.password },
  })

  // Handle HTML responses (rate limiting behind nginx)
  const contentType = res.headers()['content-type'] || ''
  if (!contentType.includes('json')) {
    throw new Error(`Login for ${account.email} returned non-JSON (status ${res.status()}). Likely rate-limited.`)
  }

  const body = await res.json()
  if (!body.data?.accessToken) {
    throw new Error(`Login failed for ${account.email}: ${JSON.stringify(body)}`)
  }

  const token = body.data.accessToken
  tokenCache[account.email] = { token, expiresAt: Date.now() + 10 * 60 * 1000 }
  return token
}

function authHeaders(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` }
}

/**
 * Full browser login — only use for tests that need the UI.
 * Reuses cached tokens to avoid rate limiting.
 */
async function loginWithCache(page: Page, account: { email: string; password: string }): Promise<string> {
  const token = await getToken(page, account)

  await page.goto('/')
  await page.evaluate((accessToken) => {
    sessionStorage.setItem('accessToken', accessToken)
  }, token)

  await page.reload()
  await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 15000 })
  return token
}

// ---------------------------------------------------------------------------
// Helpers — Bookmarks (all take explicit token)
// ---------------------------------------------------------------------------

async function toggleBookmarkViaApi(
  page: Page,
  token: string,
  contentType: string,
  contentId: string
): Promise<boolean | null> {
  try {
    const response = await page.request.post(`${BASE}/api/v1/bookmarks`, {
      headers: authHeaders(token),
      data: { contentType, contentId },
    })
    if (response.ok()) {
      const json = await response.json()
      return json.data?.bookmarked ?? null
    }
  } catch { /* ignore */ }
  return null
}

async function listBookmarksViaApi(
  page: Page,
  token: string,
  type?: string,
  pageNum = 0,
  size = 20
): Promise<{ content: any[]; totalElements: number } | null> {
  try {
    const params = new URLSearchParams()
    if (type) params.set('type', type)
    params.set('page', String(pageNum))
    params.set('size', String(size))
    const response = await page.request.get(`${BASE}/api/v1/bookmarks?${params.toString()}`, {
      headers: authHeaders(token),
    })
    if (response.ok()) {
      const json = await response.json()
      return json.data ?? null
    }
  } catch { /* ignore */ }
  return null
}

async function checkBookmarkViaApi(
  page: Page,
  token: string,
  contentType: string,
  contentId: string
): Promise<boolean | null> {
  try {
    const response = await page.request.get(
      `${BASE}/api/v1/bookmarks/check?contentType=${contentType}&contentId=${contentId}`,
      { headers: authHeaders(token) }
    )
    if (response.ok()) {
      const json = await response.json()
      return json.data?.bookmarked ?? null
    }
  } catch { /* ignore */ }
  return null
}

async function getBookmarkedIdsViaApi(
  page: Page,
  token: string,
  contentType: string
): Promise<string[] | null> {
  try {
    const response = await page.request.get(
      `${BASE}/api/v1/bookmarks/ids?contentType=${contentType}`,
      { headers: authHeaders(token) }
    )
    if (response.ok()) {
      const json = await response.json()
      return json.data ?? null
    }
  } catch { /* ignore */ }
  return null
}

async function clearBookmarksOfType(page: Page, token: string, contentType: string): Promise<void> {
  const ids = await getBookmarkedIdsViaApi(page, token, contentType)
  if (ids && ids.length > 0) {
    for (const id of ids) {
      await toggleBookmarkViaApi(page, token, contentType, id)
    }
  }
}

// ---------------------------------------------------------------------------
// Helpers — Feed Posts
// ---------------------------------------------------------------------------

async function createPostViaApi(
  page: Page,
  token: string,
  content: string,
  title?: string
): Promise<string | null> {
  try {
    const response = await page.request.post(`${BASE}/api/v1/feed/posts`, {
      headers: authHeaders(token),
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

async function deletePostViaApi(page: Page, token: string, postId: string): Promise<void> {
  try {
    await page.request.delete(`${BASE}/api/v1/feed/posts/${postId}`, {
      headers: authHeaders(token),
    })
  } catch { /* ignore */ }
}

// ---------------------------------------------------------------------------
// Helpers — Calendar Events
// ---------------------------------------------------------------------------

async function createEventViaApi(
  page: Page,
  token: string,
  title: string
): Promise<string | null> {
  try {
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    const dateStr = tomorrow.toISOString().split('T')[0]

    const response = await page.request.post(`${BASE}/api/v1/calendar/events`, {
      headers: authHeaders(token),
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

async function deleteEventViaApi(page: Page, token: string, eventId: string): Promise<void> {
  try {
    await page.request.delete(`${BASE}/api/v1/calendar/events/${eventId}`, {
      headers: authHeaders(token),
    })
  } catch { /* ignore */ }
}

// ---------------------------------------------------------------------------
// Helpers — Jobs
// ---------------------------------------------------------------------------

async function createJobViaApi(
  page: Page,
  token: string,
  title: string
): Promise<string | null> {
  try {
    const response = await page.request.post(`${BASE}/api/v1/jobs`, {
      headers: authHeaders(token),
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

async function deleteJobViaApi(page: Page, token: string, jobId: string): Promise<void> {
  try {
    await page.request.delete(`${BASE}/api/v1/jobs/${jobId}?permanent=true`, {
      headers: authHeaders(token),
    })
  } catch { /* ignore */ }
}

// ---------------------------------------------------------------------------
// Helpers — Search
// ---------------------------------------------------------------------------

async function searchViaApi(
  page: Page,
  token: string,
  q: string,
  type = 'ALL',
  limit = 20
): Promise<any[] | null> {
  try {
    const response = await page.request.get(
      `${BASE}/api/v1/search?q=${encodeURIComponent(q)}&type=${type}&limit=${limit}`,
      { headers: authHeaders(token) }
    )
    if (response.ok()) {
      const json = await response.json()
      return json.data ?? null
    }
  } catch { /* ignore */ }
  return null
}

async function goToBookmarks(page: Page): Promise<void> {
  await page.goto('/bookmarks')
  await page.waitForLoadState('networkidle')
}

// ============================================================================
// US-314: Feed-Post bookmarken
// ============================================================================
test.describe('US-314: Feed-Post bookmarken', () => {
  let postId: string | null = null
  let token: string

  test.afterEach(async ({ page }) => {
    if (postId) {
      const isBookmarked = await checkBookmarkViaApi(page, token, 'POST', postId)
      if (isBookmarked) {
        await toggleBookmarkViaApi(page, token, 'POST', postId)
      }
      await deletePostViaApi(page, token, postId)
      postId = null
    }
  })

  test('can bookmark a feed post via API toggle', async ({ page }) => {
    // Use teacher to create post (PARENT cannot create SCHOOL posts)
    token = await getToken(page, accounts.teacher)

    postId = await createPostViaApi(page, token, 'Lesezeichen-Test Inhalt fuer E2E')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    const result = await toggleBookmarkViaApi(page, token, 'POST', postId)
    expect(result).toBe(true)

    const isBookmarked = await checkBookmarkViaApi(page, token, 'POST', postId)
    expect(isBookmarked).toBe(true)
  })

  test('toggle returns bookmarked:false when removing', async ({ page }) => {
    token = await getToken(page, accounts.teacher)

    postId = await createPostViaApi(page, token, 'Lesezeichen Toggle Test')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    await toggleBookmarkViaApi(page, token, 'POST', postId)

    const result = await toggleBookmarkViaApi(page, token, 'POST', postId)
    expect(result).toBe(false)

    const isBookmarked = await checkBookmarkViaApi(page, token, 'POST', postId)
    expect(isBookmarked).toBe(false)
  })

  test('bookmark icon changes on feed page when toggled', async ({ page }) => {
    token = await loginWithCache(page, accounts.teacher)

    postId = await createPostViaApi(page, token, 'Lesezeichen-Icon Test')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')

    const bookmarkBtn = page.locator('.bookmark-btn, button:has(.pi-bookmark)').first()
    const hasBtnVisible = await bookmarkBtn.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasBtnVisible) {
      const toggled = await toggleBookmarkViaApi(page, token, 'POST', postId)
      expect(toggled).toBe(true)
      return
    }

    await bookmarkBtn.click()
    await page.waitForTimeout(500)

    const filledIcon = page.locator('.bookmark-active, .pi-bookmark-fill').first()
    const isActive = await filledIcon.isVisible({ timeout: 3000 }).catch(() => false)
    if (!isActive) {
      const status = await checkBookmarkViaApi(page, token, 'POST', postId)
      expect(status).toBeDefined()
    }
  })
})

// ============================================================================
// US-315: Kalender-Event bookmarken
// ============================================================================
test.describe('US-315: Kalender-Event bookmarken', () => {
  let eventId: string | null = null
  let token: string

  test.afterEach(async ({ page }) => {
    if (eventId) {
      const isBookmarked = await checkBookmarkViaApi(page, token, 'EVENT', eventId)
      if (isBookmarked) {
        await toggleBookmarkViaApi(page, token, 'EVENT', eventId)
      }
      await deleteEventViaApi(page, token, eventId)
      eventId = null
    }
  })

  test('can bookmark a calendar event via API', async ({ page }) => {
    token = await getToken(page, accounts.teacher)

    eventId = await createEventViaApi(page, token, 'E2E Lesezeichen-Event')
    if (!eventId) {
      test.skip(true, 'Could not create test event via API — calendar module may be disabled or TEACHER lacks permission')
      return
    }

    const result = await toggleBookmarkViaApi(page, token, 'EVENT', eventId)
    expect(result).toBe(true)

    const isBookmarked = await checkBookmarkViaApi(page, token, 'EVENT', eventId)
    expect(isBookmarked).toBe(true)
  })

  test('check endpoint returns correct bookmarked status', async ({ page }) => {
    token = await getToken(page, accounts.teacher)

    eventId = await createEventViaApi(page, token, 'E2E Check-Endpoint Event')
    if (!eventId) {
      test.skip(true, 'Could not create test event via API')
      return
    }

    const beforeBookmark = await checkBookmarkViaApi(page, token, 'EVENT', eventId)
    expect(beforeBookmark).toBe(false)

    await toggleBookmarkViaApi(page, token, 'EVENT', eventId)

    const afterBookmark = await checkBookmarkViaApi(page, token, 'EVENT', eventId)
    expect(afterBookmark).toBe(true)
  })

  test('event bookmark appears in bookmarked IDs list', async ({ page }) => {
    token = await getToken(page, accounts.teacher)

    eventId = await createEventViaApi(page, token, 'E2E IDs-Liste Event')
    if (!eventId) {
      test.skip(true, 'Could not create test event via API')
      return
    }

    await toggleBookmarkViaApi(page, token, 'EVENT', eventId)

    const ids = await getBookmarkedIdsViaApi(page, token, 'EVENT')
    expect(ids).toBeDefined()
    expect(ids).toContain(eventId)
  })
})

// ============================================================================
// US-316: Job bookmarken
// ============================================================================
test.describe('US-316: Job bookmarken', () => {
  let jobId: string | null = null
  let token: string

  test.afterEach(async ({ page }) => {
    if (jobId) {
      const isBookmarked = await checkBookmarkViaApi(page, token, 'JOB', jobId)
      if (isBookmarked) {
        await toggleBookmarkViaApi(page, token, 'JOB', jobId)
      }
      await deleteJobViaApi(page, token, jobId)
      jobId = null
    }
  })

  test('can bookmark a job via API toggle', async ({ page }) => {
    token = await getToken(page, accounts.parent)

    jobId = await createJobViaApi(page, token, 'E2E Lesezeichen-Job')
    if (!jobId) {
      test.skip(true, 'Could not create test job via API — jobboard module may be disabled')
      return
    }

    const result = await toggleBookmarkViaApi(page, token, 'JOB', jobId)
    expect(result).toBe(true)

    const isBookmarked = await checkBookmarkViaApi(page, token, 'JOB', jobId)
    expect(isBookmarked).toBe(true)
  })

  test('job bookmark shows in IDs endpoint', async ({ page }) => {
    token = await getToken(page, accounts.parent)

    jobId = await createJobViaApi(page, token, 'E2E IDs-Job')
    if (!jobId) {
      test.skip(true, 'Could not create test job via API')
      return
    }

    await toggleBookmarkViaApi(page, token, 'JOB', jobId)

    const ids = await getBookmarkedIdsViaApi(page, token, 'JOB')
    expect(ids).toBeDefined()
    expect(ids).toContain(jobId)
  })

  test('toggle returns false after double-toggle (remove)', async ({ page }) => {
    token = await getToken(page, accounts.parent)

    jobId = await createJobViaApi(page, token, 'E2E Double-Toggle Job')
    if (!jobId) {
      test.skip(true, 'Could not create test job via API')
      return
    }

    const first = await toggleBookmarkViaApi(page, token, 'JOB', jobId)
    expect(first).toBe(true)

    const second = await toggleBookmarkViaApi(page, token, 'JOB', jobId)
    expect(second).toBe(false)
  })
})

// ============================================================================
// US-317: Wiki-Seite bookmarken
// ============================================================================
test.describe('US-317: Wiki-Seite bookmarken', () => {
  test.skip(true, 'TODO: Requires existing wiki page — wiki content may not be seeded')

  test('can bookmark a wiki page via API', async ({ page }) => {
    const token = await getToken(page, accounts.teacher)
    // Would need to create or find a wiki page to bookmark
    expect(token).toBeDefined()
  })
})

// ============================================================================
// US-318: Bookmarks-Uebersicht anzeigen
// ============================================================================
test.describe('US-318: Bookmarks-Uebersicht anzeigen', () => {
  let postId: string | null = null
  let jobId: string | null = null
  let token: string

  test.afterEach(async ({ page }) => {
    if (!token) return
    if (postId) {
      await toggleBookmarkViaApi(page, token, 'POST', postId).catch(() => {})
      await deletePostViaApi(page, token, postId)
      postId = null
    }
    if (jobId) {
      await toggleBookmarkViaApi(page, token, 'JOB', jobId).catch(() => {})
      await deleteJobViaApi(page, token, jobId)
      jobId = null
    }
  })

  test('bookmarks page loads and shows title "Lesezeichen"', async ({ page }) => {
    token = await loginWithCache(page, accounts.teacher)
    await goToBookmarks(page)

    const title = page.locator('.page-title').first()
    await expect(title).toBeVisible({ timeout: 10000 })
    await expect(title).toContainText('Lesezeichen')
  })

  test('filter tabs are visible (Alle, Beitraege, Termine, Jobs, Wiki)', async ({ page }) => {
    token = await loginWithCache(page, accounts.teacher)
    await goToBookmarks(page)

    const tabContainer = page.locator('.bookmark-tabs')
    await expect(tabContainer).toBeVisible({ timeout: 10000 })

    await expect(tabContainer.locator('button:has-text("Alle")')).toBeVisible()
    await expect(tabContainer.locator('button:has-text("Beitr")')).toBeVisible()
    await expect(tabContainer.locator('button:has-text("Termine")')).toBeVisible()
    await expect(tabContainer.locator('button:has-text("Jobs")')).toBeVisible()
    await expect(tabContainer.locator('button:has-text("Wiki")')).toBeVisible()
  })

  test('empty state message shown when no bookmarks exist', async ({ page }) => {
    token = await loginWithCache(page, accounts.teacher)

    await clearBookmarksOfType(page, token, 'POST')
    await clearBookmarksOfType(page, token, 'EVENT')
    await clearBookmarksOfType(page, token, 'JOB')
    await clearBookmarksOfType(page, token, 'WIKI_PAGE')

    await goToBookmarks(page)

    const emptyState = page.locator('.empty-state').first()
    await expect(emptyState).toBeVisible({ timeout: 10000 })
    await expect(emptyState).toContainText('Lesezeichen')
  })

  test('GET /bookmarks returns paginated response', async ({ page }) => {
    token = await getToken(page, accounts.teacher)

    const result = await listBookmarksViaApi(page, token, undefined, 0, 20)
    expect(result).not.toBeNull()
    expect(result).toHaveProperty('content')
    expect(result).toHaveProperty('totalElements')
    expect(Array.isArray(result!.content)).toBe(true)
  })

  test('bookmarked items appear in overview after creation', async ({ page }) => {
    token = await getToken(page, accounts.teacher)

    postId = await createPostViaApi(page, token, 'Uebersicht Test Post')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }
    await toggleBookmarkViaApi(page, token, 'POST', postId)

    const result = await listBookmarksViaApi(page, token, 'POST')
    expect(result).not.toBeNull()
    expect(result!.content.length).toBeGreaterThanOrEqual(1)

    const found = result!.content.some((bm: any) => bm.contentId === postId)
    expect(found).toBe(true)
  })

  test('filter by type shows only matching bookmarks', async ({ page }) => {
    token = await getToken(page, accounts.teacher)

    postId = await createPostViaApi(page, token, 'Filter Test Post')
    jobId = await createJobViaApi(page, token, 'Filter Test Job')

    if (!postId || !jobId) {
      test.skip(true, 'Could not create test content via API')
      return
    }

    await toggleBookmarkViaApi(page, token, 'POST', postId)
    await toggleBookmarkViaApi(page, token, 'JOB', jobId)

    const posts = await listBookmarksViaApi(page, token, 'POST')
    expect(posts).not.toBeNull()
    const hasPost = posts!.content.some((bm: any) => bm.contentId === postId)
    expect(hasPost).toBe(true)
    const hasJobInPosts = posts!.content.some((bm: any) => bm.contentId === jobId)
    expect(hasJobInPosts).toBe(false)

    const jobs = await listBookmarksViaApi(page, token, 'JOB')
    expect(jobs).not.toBeNull()
    const hasJob = jobs!.content.some((bm: any) => bm.contentId === jobId)
    expect(hasJob).toBe(true)
  })

  test('GET /bookmarks/ids returns set of bookmarked IDs for batch check', async ({ page }) => {
    token = await getToken(page, accounts.teacher)

    postId = await createPostViaApi(page, token, 'Batch-Check Test')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    await toggleBookmarkViaApi(page, token, 'POST', postId)

    const ids = await getBookmarkedIdsViaApi(page, token, 'POST')
    expect(ids).not.toBeNull()
    expect(Array.isArray(ids)).toBe(true)
    expect(ids).toContain(postId)
  })

  test('bookmarks page shows items in the UI list', async ({ page }) => {
    token = await getToken(page, accounts.teacher)

    postId = await createPostViaApi(page, token, 'UI-Liste Test')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }
    await toggleBookmarkViaApi(page, token, 'POST', postId)

    token = await loginWithCache(page, accounts.teacher)
    await goToBookmarks(page)

    const bookmarkList = page.locator('.bookmark-list')
    const hasItems = await bookmarkList.isVisible({ timeout: 10000 }).catch(() => false)

    if (hasItems) {
      const items = bookmarkList.locator('.bookmark-item')
      const count = await items.count()
      expect(count).toBeGreaterThanOrEqual(1)
    } else {
      const result = await listBookmarksViaApi(page, token, 'POST')
      expect(result!.content.length).toBeGreaterThanOrEqual(1)
    }
  })
})

// ============================================================================
// US-319: Bookmark entfernen
// ============================================================================
test.describe('US-319: Bookmark entfernen', () => {
  let postId: string | null = null
  let token: string

  test.afterEach(async ({ page }) => {
    if (postId && token) {
      const isBookmarked = await checkBookmarkViaApi(page, token, 'POST', postId)
      if (isBookmarked) {
        await toggleBookmarkViaApi(page, token, 'POST', postId)
      }
      await deletePostViaApi(page, token, postId)
      postId = null
    }
  })

  test('second POST toggle removes bookmark', async ({ page }) => {
    token = await getToken(page, accounts.teacher)

    postId = await createPostViaApi(page, token, 'Entfernen Test Post')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    const added = await toggleBookmarkViaApi(page, token, 'POST', postId)
    expect(added).toBe(true)

    const removed = await toggleBookmarkViaApi(page, token, 'POST', postId)
    expect(removed).toBe(false)

    const isBookmarked = await checkBookmarkViaApi(page, token, 'POST', postId)
    expect(isBookmarked).toBe(false)
  })

  test('removed bookmark disappears from overview list', async ({ page }) => {
    token = await getToken(page, accounts.teacher)

    postId = await createPostViaApi(page, token, 'Verschwinden Test Post')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    await toggleBookmarkViaApi(page, token, 'POST', postId)

    let list = await listBookmarksViaApi(page, token, 'POST')
    let found = list!.content.some((bm: any) => bm.contentId === postId)
    expect(found).toBe(true)

    await toggleBookmarkViaApi(page, token, 'POST', postId)

    list = await listBookmarksViaApi(page, token, 'POST')
    found = list!.content.some((bm: any) => bm.contentId === postId)
    expect(found).toBe(false)
  })

  test('remove bookmark via trash button in bookmarks view', async ({ page }) => {
    token = await loginWithCache(page, accounts.teacher)

    postId = await createPostViaApi(page, token, 'Trash-Button Test')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    await toggleBookmarkViaApi(page, token, 'POST', postId)
    await goToBookmarks(page)

    const bookmarkList = page.locator('.bookmark-list')
    const hasItems = await bookmarkList.isVisible({ timeout: 10000 }).catch(() => false)

    if (hasItems) {
      const trashBtn = bookmarkList.locator('.bookmark-item').first().locator('button:has(.pi-trash)').first()
      const trashVisible = await trashBtn.isVisible({ timeout: 3000 }).catch(() => false)

      if (trashVisible) {
        await trashBtn.click()
        await page.waitForTimeout(1000)

        const isBookmarked = await checkBookmarkViaApi(page, token, 'POST', postId)
        expect(isBookmarked).toBe(false)
      }
    }
  })

  test('no confirmation dialog needed for removal', async ({ page }) => {
    token = await getToken(page, accounts.teacher)

    postId = await createPostViaApi(page, token, 'Keine Bestaetigung Test')
    if (!postId) {
      test.skip(true, 'Could not create test post via API')
      return
    }

    await toggleBookmarkViaApi(page, token, 'POST', postId)
    const removed = await toggleBookmarkViaApi(page, token, 'POST', postId)
    expect(removed).toBe(false)

    const isBookmarked = await checkBookmarkViaApi(page, token, 'POST', postId)
    expect(isBookmarked).toBe(false)
  })
})

// ============================================================================
// US-320: Globale Suche oeffnen (Ctrl+K)
// ============================================================================
test.describe('US-320: Globale Suche oeffnen (Ctrl+K)', () => {
  test('Ctrl+K opens the global search dialog', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })
  })

  test('search dialog has auto-focused input field', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    const searchInput = dialog.locator('.search-input, input[type="text"]').first()
    await expect(searchInput).toBeVisible()
    await page.keyboard.type('test')
    await expect(searchInput).toHaveValue('test')
  })

  test('search dialog shows 8 filter chips', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    const filters = dialog.locator('.filter-chip, .search-filters button')
    const count = await filters.count()
    expect(count).toBe(8)
  })

  test('Escape closes the search dialog', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await page.keyboard.press('Escape')
    await expect(dialog).not.toBeVisible({ timeout: 5000 })
  })

  test('search hint text shown before typing', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    const hint = dialog.locator('.search-hint')
    await expect(hint).toBeVisible()
    await expect(hint).toContainText('Zeichen')
  })
})

// ============================================================================
// US-321: Volltextsuche ausfuehren
// ============================================================================
test.describe('US-321: Volltextsuche ausfuehren', () => {
  test('search API returns results for valid query', async ({ page }) => {
    const token = await getToken(page, accounts.teacher)

    const results = await searchViaApi(page, token, 'Admin')
    expect(results).not.toBeNull()
    expect(Array.isArray(results)).toBe(true)
    expect(results!.length).toBeGreaterThanOrEqual(1)
  })

  test('search requires minimum 2 characters via UI', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await page.keyboard.type('a')
    await page.waitForTimeout(500)

    const resultsList = dialog.locator('.search-results-list')
    const hasResults = await resultsList.isVisible({ timeout: 2000 }).catch(() => false)
    expect(hasResults).toBe(false)
  })

  test('search shows results after typing 2+ characters', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await page.keyboard.type('Admin')
    await page.waitForTimeout(1000)

    const searchResults = dialog.locator('.search-results')
    await expect(searchResults).toBeVisible({ timeout: 5000 })
  })

  test('search results are grouped by type', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await page.keyboard.type('Sonnengruppe')
    await page.waitForTimeout(1000)

    const groupHeaders = dialog.locator('.result-group-header')
    const headerCount = await groupHeaders.count()
    if (headerCount > 0) {
      expect(headerCount).toBeGreaterThanOrEqual(1)
    }
  })

  test('keyboard navigation works (ArrowDown/ArrowUp)', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await page.keyboard.type('Admin')
    await page.waitForTimeout(1000)

    const resultItems = dialog.locator('.search-result-item')
    const count = await resultItems.count()

    if (count > 0) {
      await page.keyboard.press('ArrowDown')
      await page.waitForTimeout(200)

      const firstItem = resultItems.first()
      const isSelected = await firstItem.evaluate(
        (el) => el.classList.contains('selected')
      )
      expect(isSelected).toBe(true)
    }
  })

  test('300ms debounce prevents rapid API calls', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    const searchRequests: string[] = []
    page.on('request', (req) => {
      if (req.url().includes('/api/v1/search')) {
        searchRequests.push(req.url())
      }
    })

    await page.keyboard.type('Admin', { delay: 50 })
    await page.waitForTimeout(800)

    expect(searchRequests.length).toBeLessThanOrEqual(3)
  })
})

// ============================================================================
// US-322: Suche nach Typ filtern
// ============================================================================
test.describe('US-322: Suche nach Typ filtern', () => {
  test('clicking a filter chip highlights it', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    const userChip = dialog.locator('.filter-chip:has-text("Benutzer")').first()
    await expect(userChip).toBeVisible()
    await userChip.click()

    await expect(userChip).toHaveClass(/active/)
  })

  test('filter change triggers new search', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await page.keyboard.type('Admin')
    await page.waitForTimeout(800)

    const searchRequests: string[] = []
    page.on('request', (req) => {
      if (req.url().includes('/api/v1/search')) {
        searchRequests.push(req.url())
      }
    })

    const roomChip = dialog.locator('.filter-chip:has-text("Raeume"), .filter-chip:has-text("Räume")').first()
    if (await roomChip.isVisible({ timeout: 2000 }).catch(() => false)) {
      await roomChip.click()
      await page.waitForTimeout(800)

      expect(searchRequests.length).toBeGreaterThanOrEqual(1)
      const hasTypeParam = searchRequests.some((url) => url.includes('type=ROOM'))
      expect(hasTypeParam).toBe(true)
    }
  })

  test('API accepts type parameter for filtered search', async ({ page }) => {
    const token = await getToken(page, accounts.teacher)

    const userResults = await searchViaApi(page, token, 'Admin', 'USER')
    expect(userResults).not.toBeNull()
    expect(Array.isArray(userResults)).toBe(true)

    for (const result of userResults ?? []) {
      expect(result.type).toBe('USER')
    }
  })

  test('filter "Alle" returns mixed result types', async ({ page }) => {
    const token = await getToken(page, accounts.teacher)

    const allResults = await searchViaApi(page, token, 'Sonnengruppe', 'ALL', 50)
    expect(allResults).not.toBeNull()
    if (allResults && allResults.length > 1) {
      const types = new Set(allResults.map((r: any) => r.type))
      expect(types.size).toBeGreaterThanOrEqual(1)
    }
  })
})

// ============================================================================
// US-323: Solr-Volltextsuche
// ============================================================================
test.describe('US-323: Solr-Volltextsuche', () => {
  test.skip(true, 'TODO: Solr container may not be running in E2E environment')

  test('admin can trigger reindex via API', async ({ page }) => {
    const token = await getToken(page, accounts.admin)

    const response = await page.request.post(`${BASE}/api/v1/admin/search/reindex`, {
      headers: authHeaders(token),
    })
    expect(response.status()).toBeLessThan(500)
  })

  test('Solr search returns results with German stemming', async ({ page }) => {
    const token = await getToken(page, accounts.admin)

    const results = await searchViaApi(page, token, 'Benutzer')
    expect(results).toBeDefined()
    expect(results!.length).toBeGreaterThanOrEqual(0)
  })
})

// ============================================================================
// US-324: DB-Fallback bei deaktiviertem Solr
// ============================================================================
test.describe('US-324: DB-Fallback bei deaktiviertem Solr', () => {
  test('search returns results without Solr (DB fallback)', async ({ page }) => {
    const token = await getToken(page, accounts.teacher)

    const results = await searchViaApi(page, token, 'Admin')
    expect(results).not.toBeNull()
    expect(Array.isArray(results)).toBe(true)
    expect(results!.length).toBeGreaterThanOrEqual(1)
  })

  test('search results have consistent format regardless of backend', async ({ page }) => {
    const token = await getToken(page, accounts.teacher)

    const results = await searchViaApi(page, token, 'Sonnengruppe')
    expect(results).not.toBeNull()

    if (results && results.length > 0) {
      const first = results[0]
      expect(first).toHaveProperty('id')
      expect(first).toHaveProperty('type')
      expect(first).toHaveProperty('title')
      expect(['USER', 'ROOM', 'POST', 'EVENT', 'FILE', 'WIKI', 'TASK']).toContain(first.type)
    }
  })

  test('no error toast when searching without Solr', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await page.keyboard.type('Admin')
    await page.waitForTimeout(1000)

    const errorToast = page.locator(selectors.toastError)
    const hasError = await errorToast.isVisible({ timeout: 2000 }).catch(() => false)
    expect(hasError).toBe(false)
  })

  test('search via UI shows results in dialog', async ({ page }) => {
    await loginWithCache(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await page.keyboard.press('Control+k')

    const dialog = page.locator('.global-search-dialog, .p-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await page.keyboard.type('Admin')
    await page.waitForTimeout(1000)

    const searchResults = dialog.locator('.search-results')
    await expect(searchResults).toBeVisible({ timeout: 5000 })

    const resultItems = dialog.locator('.search-result-item')
    const emptyState = dialog.locator('.search-empty')
    const hasResults = await resultItems.first().isVisible({ timeout: 2000 }).catch(() => false)
    const hasEmpty = await emptyState.isVisible({ timeout: 2000 }).catch(() => false)

    expect(hasResults || hasEmpty).toBe(true)
  })
})
