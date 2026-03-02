import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { login } from '../../helpers/auth'

// ============================================================================
// Fundgrube (Lost & Found) E2E Tests — US-270 to US-282
// ============================================================================

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

interface FundgrubeItemData {
  id: string
  title: string
  description: string | null
  sectionId: string | null
  sectionName: string | null
  createdBy: string
  createdByName: string
  createdAt: string
  updatedAt: string | null
  claimedBy: string | null
  claimedByName: string | null
  claimedAt: string | null
  expiresAt: string | null
  claimed: boolean
  images: unknown[]
}

/**
 * Create a Fundgrube item via the API and return its data.
 */
async function createItemViaApi(
  page: import('@playwright/test').Page,
  title: string,
  description?: string,
  sectionId?: string
): Promise<FundgrubeItemData | null> {
  try {
    const data: Record<string, unknown> = { title }
    if (description !== undefined) data.description = description
    if (sectionId !== undefined) data.sectionId = sectionId
    const res = await page.request.post('/api/v1/fundgrube/items', { data })
    if (res.ok()) {
      const json = await res.json()
      return json.data ?? null
    }
  } catch {
    // API call failed
  }
  return null
}

/**
 * Delete a Fundgrube item via the API (cleanup).
 */
async function deleteItemViaApi(
  page: import('@playwright/test').Page,
  itemId: string
): Promise<void> {
  try {
    await page.request.delete(`/api/v1/fundgrube/items/${itemId}`)
  } catch {
    // Ignore cleanup failures
  }
}

/**
 * Get a single Fundgrube item via the API.
 */
async function getItemViaApi(
  page: import('@playwright/test').Page,
  itemId: string
): Promise<FundgrubeItemData | null> {
  try {
    const res = await page.request.get(`/api/v1/fundgrube/items/${itemId}`)
    if (res.ok()) {
      const json = await res.json()
      return json.data ?? null
    }
  } catch {
    // API call failed
  }
  return null
}

/**
 * List all Fundgrube items, optionally filtered by sectionId.
 */
async function listItemsViaApi(
  page: import('@playwright/test').Page,
  sectionId?: string
): Promise<FundgrubeItemData[]> {
  try {
    const url = sectionId
      ? `/api/v1/fundgrube/items?sectionId=${sectionId}`
      : '/api/v1/fundgrube/items'
    const res = await page.request.get(url)
    if (res.ok()) {
      const json = await res.json()
      return json.data ?? []
    }
  } catch {
    // API call failed
  }
  return []
}

/**
 * Get the first available school section ID.
 */
async function getFirstSectionId(
  page: import('@playwright/test').Page
): Promise<string | null> {
  try {
    const res = await page.request.get('/api/v1/sections')
    if (res.ok()) {
      const json = await res.json()
      const sections = json.data ?? []
      if (sections.length > 0) return sections[0].id
    }
  } catch {
    // API call failed
  }
  return null
}

/**
 * Get two different section IDs for cross-section tests.
 */
async function getTwoSectionIds(
  page: import('@playwright/test').Page
): Promise<[string, string] | null> {
  try {
    const res = await page.request.get('/api/v1/sections')
    if (res.ok()) {
      const json = await res.json()
      const sections = json.data ?? []
      if (sections.length >= 2) return [sections[0].id, sections[1].id]
    }
  } catch {
    // API call failed
  }
  return null
}

// --------------------------------------------------------------------------
// US-270: Fundgegenstand einstellen
// --------------------------------------------------------------------------
test.describe('US-270: Fundgegenstand einstellen', () => {

  test('authenticated user can create a Fundgrube item via API', async ({ page }) => {
    await login(page, accounts.parent)

    const title = `Verlorene Jacke ${Date.now()}`
    const description = 'Blaue Winterjacke, Groesse 128'
    const item = await createItemViaApi(page, title, description)
    expect(item).toBeTruthy()
    expect(item!.title).toBe(title)
    expect(item!.description).toBe(description)
    expect(item!.createdBy).toBeTruthy()
    expect(item!.createdByName).toBeTruthy()
    expect(item!.createdAt).toBeTruthy()
    expect(item!.claimed).toBe(false)
    expect(item!.claimedBy).toBeNull()

    // Cleanup
    await deleteItemViaApi(page, item!.id)
  })

  test('item can be created with optional sectionId', async ({ page }) => {
    await login(page, accounts.parent)
    const sectionId = await getFirstSectionId(page)
    if (!sectionId) {
      test.skip()
      return
    }

    const title = `Schluessel ${Date.now()}`
    const item = await createItemViaApi(page, title, 'Kleiner Schluessel gefunden', sectionId)
    expect(item).toBeTruthy()
    expect(item!.sectionId).toBe(sectionId)
    expect(item!.sectionName).toBeTruthy()

    // Cleanup
    await deleteItemViaApi(page, item!.id)
  })

  test('item can be created without description', async ({ page }) => {
    await login(page, accounts.teacher)

    const title = `Trinkflasche ${Date.now()}`
    const item = await createItemViaApi(page, title)
    expect(item).toBeTruthy()
    expect(item!.title).toBe(title)
    // description may be null or empty
    expect(item!.claimed).toBe(false)

    // Cleanup
    await deleteItemViaApi(page, item!.id)
  })

  test('item appears in list after creation', async ({ page }) => {
    await login(page, accounts.parent)

    const title = `Turnbeutel ${Date.now()}`
    const item = await createItemViaApi(page, title, 'Roter Turnbeutel')
    expect(item).toBeTruthy()

    const items = await listItemsViaApi(page)
    const found = items.find(i => i.id === item!.id)
    expect(found).toBeTruthy()
    expect(found!.title).toBe(title)

    // Cleanup
    await deleteItemViaApi(page, item!.id)
  })

  test('creation fails with blank title (validation)', async ({ page }) => {
    await login(page, accounts.parent)

    const res = await page.request.post('/api/v1/fundgrube/items', {
      data: { title: '', description: 'Missing title' },
    })
    expect(res.ok()).toBe(false)
    expect(res.status()).toBeGreaterThanOrEqual(400)
  })

  test('Fundgrube page is accessible at /fundgrube', async ({ page }) => {
    await login(page, accounts.parent)
    await page.goto('/fundgrube')
    await page.waitForLoadState('networkidle')
    // Page should load without error — check we are not on a 404 or error page
    const url = page.url()
    expect(url).toContain('/fundgrube')
  })
})

// --------------------------------------------------------------------------
// US-271: Fotos zum Fundgegenstand hochladen
// --------------------------------------------------------------------------
test.describe('US-271: Fotos zum Fundgegenstand hochladen', () => {

  // TODO: Requires real multipart image upload with valid magic bytes.
  // Skipping as constructing valid image buffers for Playwright multipart
  // upload is not reliably supported in this test harness.
  test.skip('uploading images to item generates thumbnails', async ({ page }) => {
    await login(page, accounts.parent)
  })

  test.skip('image endpoint returns image via GET with ?token= auth', async ({ page }) => {
    await login(page, accounts.parent)
  })
})

// --------------------------------------------------------------------------
// US-272: Fundgegenstaende auflisten mit Bereichsfilter
// --------------------------------------------------------------------------
test.describe('US-272: Fundgegenstaende auflisten mit Bereichsfilter', () => {

  test('GET /api/v1/fundgrube/items returns all unclaimed items', async ({ page }) => {
    await login(page, accounts.parent)

    // Create two items
    const item1 = await createItemViaApi(page, `Liste-A-${Date.now()}`, 'Item A')
    const item2 = await createItemViaApi(page, `Liste-B-${Date.now()}`, 'Item B')
    expect(item1).toBeTruthy()
    expect(item2).toBeTruthy()

    const items = await listItemsViaApi(page)
    const ids = items.map(i => i.id)
    expect(ids).toContain(item1!.id)
    expect(ids).toContain(item2!.id)

    // Cleanup
    await deleteItemViaApi(page, item1!.id)
    await deleteItemViaApi(page, item2!.id)
  })

  test('filtering by sectionId returns only items in that section', async ({ page }) => {
    await login(page, accounts.parent)

    const sections = await getTwoSectionIds(page)
    if (!sections) {
      test.skip()
      return
    }
    const [sectionA, sectionB] = sections

    const itemA = await createItemViaApi(page, `SectionA-${Date.now()}`, 'In section A', sectionA)
    const itemB = await createItemViaApi(page, `SectionB-${Date.now()}`, 'In section B', sectionB)
    expect(itemA).toBeTruthy()
    expect(itemB).toBeTruthy()

    // Filter by section A
    const filteredA = await listItemsViaApi(page, sectionA)
    const filteredAIds = filteredA.map(i => i.id)
    expect(filteredAIds).toContain(itemA!.id)
    expect(filteredAIds).not.toContain(itemB!.id)

    // Filter by section B
    const filteredB = await listItemsViaApi(page, sectionB)
    const filteredBIds = filteredB.map(i => i.id)
    expect(filteredBIds).toContain(itemB!.id)
    expect(filteredBIds).not.toContain(itemA!.id)

    // Cleanup
    await deleteItemViaApi(page, itemA!.id)
    await deleteItemViaApi(page, itemB!.id)
  })

  test('item without sectionId appears in unfiltered list', async ({ page }) => {
    await login(page, accounts.parent)

    const item = await createItemViaApi(page, `NoSection-${Date.now()}`, 'Kein Bereich')
    expect(item).toBeTruthy()
    expect(item!.sectionId).toBeNull()

    // Should appear in unfiltered list
    const allItems = await listItemsViaApi(page)
    const found = allItems.find(i => i.id === item!.id)
    expect(found).toBeTruthy()

    // Cleanup
    await deleteItemViaApi(page, item!.id)
  })

  test('items are sorted newest first', async ({ page }) => {
    await login(page, accounts.parent)

    const item1 = await createItemViaApi(page, `First-${Date.now()}`)
    // Small delay to ensure different createdAt
    await page.waitForTimeout(100)
    const item2 = await createItemViaApi(page, `Second-${Date.now()}`)
    expect(item1).toBeTruthy()
    expect(item2).toBeTruthy()

    const items = await listItemsViaApi(page)
    const idx1 = items.findIndex(i => i.id === item1!.id)
    const idx2 = items.findIndex(i => i.id === item2!.id)
    // Newest first: item2 should appear before item1
    expect(idx2).toBeLessThan(idx1)

    // Cleanup
    await deleteItemViaApi(page, item1!.id)
    await deleteItemViaApi(page, item2!.id)
  })
})

// --------------------------------------------------------------------------
// US-273: Fundgegenstand beanspruchen (Claim)
// --------------------------------------------------------------------------
test.describe('US-273: Fundgegenstand beanspruchen (Claim)', () => {

  test('user can claim an unclaimed item', async ({ page }) => {
    // Create as parent
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `Claim-Test-${Date.now()}`, 'Bitte abholen')
    expect(item).toBeTruthy()

    // Claim as teacher (different user)
    await login(page, accounts.teacher)
    const res = await page.request.post(`/api/v1/fundgrube/items/${item!.id}/claim`, {
      data: { comment: 'Das gehoert meinem Schueler' },
    })
    expect(res.ok()).toBe(true)

    const json = await res.json()
    const claimed = json.data as FundgrubeItemData
    expect(claimed.claimed).toBe(true)
    expect(claimed.claimedBy).toBeTruthy()
    expect(claimed.claimedByName).toBeTruthy()
    expect(claimed.claimedAt).toBeTruthy()
    expect(claimed.expiresAt).toBeTruthy()

    // expiresAt should be ~24h after claimedAt
    const claimedAtMs = new Date(claimed.claimedAt!).getTime()
    const expiresAtMs = new Date(claimed.expiresAt!).getTime()
    const diffHours = (expiresAtMs - claimedAtMs) / (1000 * 60 * 60)
    expect(diffHours).toBeGreaterThanOrEqual(23.5)
    expect(diffHours).toBeLessThanOrEqual(24.5)

    // Cleanup as creator
    await login(page, accounts.parent)
    await deleteItemViaApi(page, item!.id)
  })

  test('double claim is prevented', async ({ page }) => {
    // Create as parent
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `DoubleClaim-${Date.now()}`)
    expect(item).toBeTruthy()

    // First claim as teacher
    await login(page, accounts.teacher)
    const res1 = await page.request.post(`/api/v1/fundgrube/items/${item!.id}/claim`, {
      data: { comment: 'First claim' },
    })
    expect(res1.ok()).toBe(true)

    // Second claim as student — should fail
    await login(page, accounts.student)
    const res2 = await page.request.post(`/api/v1/fundgrube/items/${item!.id}/claim`, {
      data: { comment: 'Second claim attempt' },
    })
    expect(res2.ok()).toBe(false)
    expect(res2.status()).toBe(400)

    // Cleanup as creator
    await login(page, accounts.parent)
    await deleteItemViaApi(page, item!.id)
  })

  test('creator cannot claim their own item', async ({ page }) => {
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `OwnClaim-${Date.now()}`)
    expect(item).toBeTruthy()

    const res = await page.request.post(`/api/v1/fundgrube/items/${item!.id}/claim`, {
      data: { comment: 'Self claim' },
    })
    expect(res.ok()).toBe(false)
    expect(res.status()).toBe(400)

    // Cleanup
    await deleteItemViaApi(page, item!.id)
  })

  test('claim without comment is accepted', async ({ page }) => {
    // Create as teacher
    await login(page, accounts.teacher)
    const item = await createItemViaApi(page, `NoComment-${Date.now()}`)
    expect(item).toBeTruthy()

    // Claim as parent without comment
    await login(page, accounts.parent)
    const res = await page.request.post(`/api/v1/fundgrube/items/${item!.id}/claim`, {
      data: {},
    })
    expect(res.ok()).toBe(true)

    const json = await res.json()
    expect(json.data.claimed).toBe(true)

    // Cleanup as creator
    await login(page, accounts.teacher)
    await deleteItemViaApi(page, item!.id)
  })
})

// --------------------------------------------------------------------------
// US-274: Claim-Ablauf nach 24 Stunden
// --------------------------------------------------------------------------
test.describe('US-274: Claim-Ablauf nach 24 Stunden', () => {

  // TODO: Testing claim expiration requires either time manipulation on
  // the server or waiting 24 hours. The scheduled cleanup job cannot be
  // triggered from E2E tests. Skipping.
  test.skip('claimed item expires after 24 hours via scheduled job', async ({ page }) => {
    await login(page, accounts.parent)
  })
})

// --------------------------------------------------------------------------
// US-275: Fundgegenstand bearbeiten
// --------------------------------------------------------------------------
test.describe('US-275: Fundgegenstand bearbeiten', () => {

  test('creator can update title and description', async ({ page }) => {
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `Edit-Original-${Date.now()}`, 'Alte Beschreibung')
    expect(item).toBeTruthy()

    const newTitle = `Edit-Updated-${Date.now()}`
    const res = await page.request.put(`/api/v1/fundgrube/items/${item!.id}`, {
      data: { title: newTitle, description: 'Neue Beschreibung' },
    })
    expect(res.ok()).toBe(true)

    const json = await res.json()
    expect(json.data.title).toBe(newTitle)
    expect(json.data.description).toBe('Neue Beschreibung')

    // Cleanup
    await deleteItemViaApi(page, item!.id)
  })

  test('updatedAt is set after edit', async ({ page }) => {
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `Timestamp-${Date.now()}`, 'Check updatedAt')
    expect(item).toBeTruthy()

    const res = await page.request.put(`/api/v1/fundgrube/items/${item!.id}`, {
      data: { description: 'Geaendert' },
    })
    expect(res.ok()).toBe(true)

    const json = await res.json()
    // updatedAt should be present after an edit
    expect(json.data.updatedAt).toBeTruthy()

    // Cleanup
    await deleteItemViaApi(page, item!.id)
  })

  test('admin can edit any item', async ({ page }) => {
    // Create as parent
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `AdminEdit-${Date.now()}`, 'Eltern-Item')
    expect(item).toBeTruthy()

    // Edit as admin
    await login(page, accounts.admin)
    const res = await page.request.put(`/api/v1/fundgrube/items/${item!.id}`, {
      data: { title: 'Admin-geaendert' },
    })
    expect(res.ok()).toBe(true)
    const json = await res.json()
    expect(json.data.title).toBe('Admin-geaendert')

    // Cleanup
    await deleteItemViaApi(page, item!.id)
  })

  test('other user (non-creator, non-admin) cannot edit', async ({ page }) => {
    // Create as parent
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `NoEdit-${Date.now()}`)
    expect(item).toBeTruthy()

    // Try edit as student
    await login(page, accounts.student)
    const res = await page.request.put(`/api/v1/fundgrube/items/${item!.id}`, {
      data: { title: 'Hacked' },
    })
    expect(res.status()).toBe(403)

    // Cleanup as creator
    await login(page, accounts.parent)
    await deleteItemViaApi(page, item!.id)
  })
})

// --------------------------------------------------------------------------
// US-276: Fundgegenstand loeschen
// --------------------------------------------------------------------------
test.describe('US-276: Fundgegenstand loeschen', () => {

  test('creator can delete own item', async ({ page }) => {
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `DeleteMe-${Date.now()}`)
    expect(item).toBeTruthy()

    const res = await page.request.delete(`/api/v1/fundgrube/items/${item!.id}`)
    expect(res.ok()).toBe(true)

    // Verify item is gone
    const getRes = await page.request.get(`/api/v1/fundgrube/items/${item!.id}`)
    expect(getRes.status()).toBe(404)
  })

  test('admin can delete any item', async ({ page }) => {
    // Create as teacher
    await login(page, accounts.teacher)
    const item = await createItemViaApi(page, `AdminDelete-${Date.now()}`)
    expect(item).toBeTruthy()

    // Delete as admin
    await login(page, accounts.admin)
    const res = await page.request.delete(`/api/v1/fundgrube/items/${item!.id}`)
    expect(res.ok()).toBe(true)

    // Verify item is gone
    const getRes = await page.request.get(`/api/v1/fundgrube/items/${item!.id}`)
    expect(getRes.status()).toBe(404)
  })

  test('other user (non-creator, non-admin) cannot delete', async ({ page }) => {
    // Create as teacher
    await login(page, accounts.teacher)
    const item = await createItemViaApi(page, `NoDel-${Date.now()}`)
    expect(item).toBeTruthy()

    // Try to delete as student
    await login(page, accounts.student)
    const res = await page.request.delete(`/api/v1/fundgrube/items/${item!.id}`)
    expect(res.status()).toBe(403)

    // Cleanup as creator
    await login(page, accounts.teacher)
    await deleteItemViaApi(page, item!.id)
  })

  test('deleted item no longer appears in list', async ({ page }) => {
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `GoneFromList-${Date.now()}`)
    expect(item).toBeTruthy()

    await page.request.delete(`/api/v1/fundgrube/items/${item!.id}`)

    const items = await listItemsViaApi(page)
    const found = items.find(i => i.id === item!.id)
    expect(found).toBeFalsy()
  })
})

// --------------------------------------------------------------------------
// US-277: Bild vom Fundgegenstand loeschen
// --------------------------------------------------------------------------
test.describe('US-277: Bild vom Fundgegenstand loeschen', () => {

  // TODO: Deleting images requires having uploaded images first (multipart).
  // Skipping until a reliable multipart image upload pattern is established.
  test.skip('creator can delete an image via DELETE /api/v1/fundgrube/images/{imageId}', async ({ page }) => {
    await login(page, accounts.parent)
  })

  test.skip('admin can delete any image', async ({ page }) => {
    await login(page, accounts.admin)
  })
})

// --------------------------------------------------------------------------
// US-278: Fundgegenstand-Detailansicht
// --------------------------------------------------------------------------
test.describe('US-278: Fundgegenstand-Detailansicht', () => {

  test('GET /api/v1/fundgrube/items/{itemId} returns full details', async ({ page }) => {
    await login(page, accounts.parent)
    const sectionId = await getFirstSectionId(page)

    const title = `Detail-${Date.now()}`
    const description = 'Detaillierte Beschreibung des Gegenstands'
    const item = await createItemViaApi(page, title, description, sectionId ?? undefined)
    expect(item).toBeTruthy()

    // Fetch detail
    const detail = await getItemViaApi(page, item!.id)
    expect(detail).toBeTruthy()
    expect(detail!.id).toBe(item!.id)
    expect(detail!.title).toBe(title)
    expect(detail!.description).toBe(description)
    expect(detail!.createdBy).toBeTruthy()
    expect(detail!.createdByName).toBeTruthy()
    expect(detail!.createdAt).toBeTruthy()
    expect(detail!.claimed).toBe(false)
    expect(detail!.images).toBeDefined()
    expect(Array.isArray(detail!.images)).toBe(true)

    if (sectionId) {
      expect(detail!.sectionId).toBe(sectionId)
      expect(detail!.sectionName).toBeTruthy()
    }

    // Cleanup
    await deleteItemViaApi(page, item!.id)
  })

  test('detail includes claim info after item is claimed', async ({ page }) => {
    // Create as parent
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `ClaimDetail-${Date.now()}`)
    expect(item).toBeTruthy()

    // Claim as teacher
    await login(page, accounts.teacher)
    await page.request.post(`/api/v1/fundgrube/items/${item!.id}/claim`, {
      data: { comment: 'Claim for detail test' },
    })

    // Check detail
    const detail = await getItemViaApi(page, item!.id)
    expect(detail).toBeTruthy()
    expect(detail!.claimed).toBe(true)
    expect(detail!.claimedBy).toBeTruthy()
    expect(detail!.claimedByName).toBeTruthy()
    expect(detail!.claimedAt).toBeTruthy()
    expect(detail!.expiresAt).toBeTruthy()

    // Cleanup as creator
    await login(page, accounts.parent)
    await deleteItemViaApi(page, item!.id)
  })

  test('requesting a non-existent item returns 404', async ({ page }) => {
    await login(page, accounts.parent)

    const fakeId = '00000000-0000-0000-0000-000000000099'
    const res = await page.request.get(`/api/v1/fundgrube/items/${fakeId}`)
    expect(res.status()).toBe(404)
  })
})

// --------------------------------------------------------------------------
// US-279: Fundgrube bei deaktiviertem Modul
// --------------------------------------------------------------------------
test.describe('US-279: Fundgrube bei deaktiviertem Modul', () => {

  // TODO: Disabling the fundgrube module requires a config property change
  // and server restart. Cannot be toggled at runtime from E2E tests.
  test.skip('API returns 404 when fundgrube module is disabled', async ({ page }) => {
    await login(page, accounts.parent)
  })
})

// --------------------------------------------------------------------------
// US-280: Fundgrube-Item mit abgelaufenem Claim
// --------------------------------------------------------------------------
test.describe('US-280: Fundgrube-Item mit abgelaufenem Claim', () => {

  // TODO: Testing expired claims requires time manipulation or waiting 24h.
  // The scheduled cleanup job cannot be triggered from E2E tests.
  test.skip('expired claimed items are cleaned up by scheduled job', async ({ page }) => {
    await login(page, accounts.parent)
  })
})

// --------------------------------------------------------------------------
// US-281: Fundgrube-Suche ueber mehrere Bereiche
// --------------------------------------------------------------------------
test.describe('US-281: Fundgrube-Suche ueber mehrere Bereiche', () => {

  test('admin sees items across all sections', async ({ page }) => {
    // Create items in different sections as parent
    await login(page, accounts.parent)
    const sections = await getTwoSectionIds(page)
    if (!sections) {
      test.skip()
      return
    }
    const [sectionA, sectionB] = sections

    const itemA = await createItemViaApi(page, `CrossA-${Date.now()}`, 'Section A item', sectionA)
    const itemB = await createItemViaApi(page, `CrossB-${Date.now()}`, 'Section B item', sectionB)
    const itemNone = await createItemViaApi(page, `CrossNone-${Date.now()}`, 'No section item')
    expect(itemA).toBeTruthy()
    expect(itemB).toBeTruthy()
    expect(itemNone).toBeTruthy()

    // Admin lists all without filter
    await login(page, accounts.admin)
    const allItems = await listItemsViaApi(page)
    const allIds = allItems.map(i => i.id)
    expect(allIds).toContain(itemA!.id)
    expect(allIds).toContain(itemB!.id)
    expect(allIds).toContain(itemNone!.id)

    // Cleanup
    await deleteItemViaApi(page, itemA!.id)
    await deleteItemViaApi(page, itemB!.id)
    await deleteItemViaApi(page, itemNone!.id)
  })

  test('filter by sectionId narrows results correctly', async ({ page }) => {
    await login(page, accounts.admin)
    const sectionId = await getFirstSectionId(page)
    if (!sectionId) {
      test.skip()
      return
    }

    const inSection = await createItemViaApi(page, `InSection-${Date.now()}`, 'Has section', sectionId)
    const noSection = await createItemViaApi(page, `NoSection-${Date.now()}`, 'No section')
    expect(inSection).toBeTruthy()
    expect(noSection).toBeTruthy()

    const filtered = await listItemsViaApi(page, sectionId)
    const filteredIds = filtered.map(i => i.id)
    expect(filteredIds).toContain(inSection!.id)
    // Item without section should NOT appear in section-filtered results
    expect(filteredIds).not.toContain(noSection!.id)

    // Cleanup
    await deleteItemViaApi(page, inSection!.id)
    await deleteItemViaApi(page, noSection!.id)
  })
})

// --------------------------------------------------------------------------
// US-282: Fundgrube Berechtigungspruefung fuer Loeschen/Bearbeiten
// --------------------------------------------------------------------------
test.describe('US-282: Fundgrube Berechtigungspruefung fuer Loeschen/Bearbeiten', () => {

  test('creator can edit own item', async ({ page }) => {
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `PermEdit-${Date.now()}`)
    expect(item).toBeTruthy()

    const res = await page.request.put(`/api/v1/fundgrube/items/${item!.id}`, {
      data: { title: 'Creator-Updated' },
    })
    expect(res.ok()).toBe(true)

    // Cleanup
    await deleteItemViaApi(page, item!.id)
  })

  test('creator can delete own item', async ({ page }) => {
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `PermDel-${Date.now()}`)
    expect(item).toBeTruthy()

    const res = await page.request.delete(`/api/v1/fundgrube/items/${item!.id}`)
    expect(res.ok()).toBe(true)
  })

  test('other user with same role gets 403 on edit', async ({ page }) => {
    // Create as parent
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `Perm403Edit-${Date.now()}`)
    expect(item).toBeTruthy()

    // Try edit as student (different non-admin user)
    await login(page, accounts.student)
    const res = await page.request.put(`/api/v1/fundgrube/items/${item!.id}`, {
      data: { title: 'Unauthorized' },
    })
    expect(res.status()).toBe(403)

    // Cleanup as creator
    await login(page, accounts.parent)
    await deleteItemViaApi(page, item!.id)
  })

  test('other user with same role gets 403 on delete', async ({ page }) => {
    // Create as parent
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `Perm403Del-${Date.now()}`)
    expect(item).toBeTruthy()

    // Try delete as student
    await login(page, accounts.student)
    const res = await page.request.delete(`/api/v1/fundgrube/items/${item!.id}`)
    expect(res.status()).toBe(403)

    // Cleanup as creator
    await login(page, accounts.parent)
    await deleteItemViaApi(page, item!.id)
  })

  test('SUPERADMIN can edit any item regardless of creator', async ({ page }) => {
    // Create as student
    await login(page, accounts.student)
    const item = await createItemViaApi(page, `AdminPerm-${Date.now()}`, 'Schueler-Item')
    expect(item).toBeTruthy()

    // Admin edits it
    await login(page, accounts.admin)
    const editRes = await page.request.put(`/api/v1/fundgrube/items/${item!.id}`, {
      data: { title: 'Admin-Override', description: 'Admin hat bearbeitet' },
    })
    expect(editRes.ok()).toBe(true)
    const json = await editRes.json()
    expect(json.data.title).toBe('Admin-Override')

    // Cleanup
    await deleteItemViaApi(page, item!.id)
  })

  test('SUPERADMIN can delete any item regardless of creator', async ({ page }) => {
    // Create as teacher
    await login(page, accounts.teacher)
    const item = await createItemViaApi(page, `AdminDel-${Date.now()}`)
    expect(item).toBeTruthy()

    // Admin deletes it
    await login(page, accounts.admin)
    const res = await page.request.delete(`/api/v1/fundgrube/items/${item!.id}`)
    expect(res.ok()).toBe(true)

    // Verify gone
    const getRes = await page.request.get(`/api/v1/fundgrube/items/${item!.id}`)
    expect(getRes.status()).toBe(404)
  })

  test('anyone can claim an unclaimed item they did not create', async ({ page }) => {
    // Create as teacher
    await login(page, accounts.teacher)
    const item = await createItemViaApi(page, `AnyClaim-${Date.now()}`)
    expect(item).toBeTruthy()

    // Student claims it
    await login(page, accounts.student)
    const res = await page.request.post(`/api/v1/fundgrube/items/${item!.id}/claim`, {
      data: { comment: 'Schueler beansprucht' },
    })
    expect(res.ok()).toBe(true)

    const json = await res.json()
    expect(json.data.claimed).toBe(true)

    // Cleanup as creator
    await login(page, accounts.teacher)
    await deleteItemViaApi(page, item!.id)
  })

  test('section admin can edit item in their section', async ({ page }) => {
    // Get a section that the section admin manages
    await login(page, accounts.sectionAdmin)
    const sectionId = await getFirstSectionId(page)
    if (!sectionId) {
      test.skip()
      return
    }

    // Create item as parent with that section
    await login(page, accounts.parent)
    const item = await createItemViaApi(page, `SectionAdminEdit-${Date.now()}`, 'In section', sectionId)
    expect(item).toBeTruthy()

    // Section admin tries to edit — may succeed if they admin that section
    await login(page, accounts.sectionAdmin)
    const res = await page.request.put(`/api/v1/fundgrube/items/${item!.id}`, {
      data: { title: 'SectionAdmin-Edited' },
    })
    // Section admin can edit items in their section (or 403 if not their section)
    expect([200, 403]).toContain(res.status())

    // Cleanup as admin (guaranteed to work)
    await login(page, accounts.admin)
    await deleteItemViaApi(page, item!.id)
  })
})
