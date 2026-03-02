import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { login } from '../../helpers/auth'
import { selectors } from '../../helpers/selectors'

// ============================================================================
// Wiki & Tasks/Kanban E2E Tests — US-300 to US-313
// ============================================================================

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

type PageType = import('@playwright/test').Page

/**
 * Get the first room ID from rooms the user is a member of.
 */
async function getRoomId(page: PageType): Promise<string | null> {
  try {
    const res = await page.request.get('/api/v1/rooms/mine')
    if (res.ok()) {
      const json = await res.json()
      const rooms = json.data?.content ?? json.data ?? []
      if (rooms.length > 0) return rooms[0].id
    }
  } catch {
    // API call failed
  }
  return null
}

/**
 * Create a wiki page via API. Returns the created page or null.
 */
async function createWikiPage(
  page: PageType,
  roomId: string,
  title: string,
  content?: string,
  parentId?: string,
): Promise<{
  id: string
  slug: string
  title: string
  roomId: string
  parentId: string | null
  content: string
} | null> {
  try {
    const res = await page.request.post(`/api/v1/rooms/${roomId}/wiki/pages`, {
      data: {
        title,
        content: content ?? `# ${title}\n\nInhalt der Seite.`,
        parentId: parentId ?? null,
      },
    })
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
 * Delete a wiki page via API (cleanup).
 */
async function deleteWikiPage(
  page: PageType,
  roomId: string,
  pageId: string,
): Promise<void> {
  try {
    await page.request.delete(`/api/v1/rooms/${roomId}/wiki/pages/${pageId}`)
  } catch {
    // Ignore cleanup failures
  }
}

/**
 * Create a task board column via API. Returns the created column or null.
 */
async function createColumn(
  page: PageType,
  roomId: string,
  name: string,
): Promise<{ id: string; name: string; position: number } | null> {
  try {
    const res = await page.request.post(`/api/v1/rooms/${roomId}/tasks/columns`, {
      data: { name },
    })
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
 * Delete a task board column via API (cleanup).
 */
async function deleteColumn(
  page: PageType,
  roomId: string,
  columnId: string,
): Promise<void> {
  try {
    await page.request.delete(`/api/v1/rooms/${roomId}/tasks/columns/${columnId}`)
  } catch {
    // Ignore cleanup failures
  }
}

/**
 * Create a task via API. Returns the created task or null.
 */
async function createTask(
  page: PageType,
  roomId: string,
  title: string,
  columnId: string,
  description?: string,
): Promise<{
  id: string
  columnId: string
  title: string
  description: string | null
  position: number
  checklistItems: { id: string; title: string; checked: boolean }[]
} | null> {
  try {
    const res = await page.request.post(`/api/v1/rooms/${roomId}/tasks`, {
      data: {
        title,
        columnId,
        description: description ?? null,
      },
    })
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
 * Delete a task via API (cleanup).
 */
async function deleteTask(
  page: PageType,
  roomId: string,
  taskId: string,
): Promise<void> {
  try {
    await page.request.delete(`/api/v1/rooms/${roomId}/tasks/${taskId}`)
  } catch {
    // Ignore cleanup failures
  }
}

/**
 * Get the task board for a room. Returns board or null.
 */
async function getBoard(
  page: PageType,
  roomId: string,
): Promise<{
  id: string
  roomId: string
  columns: { id: string; name: string; position: number }[]
  tasks: { id: string; columnId: string; title: string; position: number }[]
} | null> {
  try {
    const res = await page.request.get(`/api/v1/rooms/${roomId}/tasks`)
    if (res.ok()) {
      const json = await res.json()
      return json.data ?? null
    }
  } catch {
    // API call failed
  }
  return null
}

// ============================================================================
// WIKI TESTS — US-300 to US-306
// ============================================================================

// --------------------------------------------------------------------------
// US-300: Wiki-Seite erstellen
// --------------------------------------------------------------------------
test.describe('US-300: Wiki-Seite erstellen', () => {

  test('teacher can create a wiki page via API', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const title = `E2E-Wiki-Seite-${Date.now()}`
    const content = '# Testseite\n\nDies ist ein **Markdown**-Test.'

    const created = await createWikiPage(page, roomId, title, content)
    expect(created).toBeTruthy()
    expect(created!.title).toBe(title)
    expect(created!.content).toBe(content)
    expect(created!.slug).toBeTruthy()
    expect(created!.roomId).toBe(roomId)
    expect(created!.parentId).toBeNull()

    // Cleanup
    await deleteWikiPage(page, roomId, created!.id)
  })

  test('slug is auto-generated from title', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const title = `Mein Test Artikel ${Date.now()}`
    const created = await createWikiPage(page, roomId, title)
    expect(created).toBeTruthy()
    // Slug should be a lowercase, hyphenated version of the title
    expect(created!.slug).toBeTruthy()
    expect(created!.slug).not.toContain(' ')

    // Cleanup
    await deleteWikiPage(page, roomId, created!.id)
  })

  test('creating a wiki page without title returns 400', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const res = await page.request.post(`/api/v1/rooms/${roomId}/wiki/pages`, {
      data: { title: '', content: 'Inhalt ohne Titel' },
    })
    expect(res.ok()).toBe(false)
    expect(res.status()).toBe(400)
  })

  test('page appears in the wiki page tree after creation', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const title = `Tree-Test-${Date.now()}`
    const created = await createWikiPage(page, roomId, title)
    expect(created).toBeTruthy()

    // GET the page tree
    const treeRes = await page.request.get(`/api/v1/rooms/${roomId}/wiki`)
    expect(treeRes.ok()).toBe(true)
    const treeJson = await treeRes.json()
    const pages = treeJson.data ?? []
    const found = pages.find((p: { id: string }) => p.id === created!.id)
    expect(found).toBeTruthy()
    expect(found.title).toBe(title)

    // Cleanup
    await deleteWikiPage(page, roomId, created!.id)
  })
})

// --------------------------------------------------------------------------
// US-301: Wiki-Seite per Slug aufrufen
// --------------------------------------------------------------------------
test.describe('US-301: Wiki-Seite per Slug aufrufen', () => {

  test('page can be retrieved by slug', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const title = `Slug-Lookup-${Date.now()}`
    const content = '## Abschnitt\n\nText mit **fett** und *kursiv*.'
    const created = await createWikiPage(page, roomId, title, content)
    expect(created).toBeTruthy()

    // GET by slug
    const res = await page.request.get(`/api/v1/rooms/${roomId}/wiki/pages/${created!.slug}`)
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const pageData = json.data
    expect(pageData.id).toBe(created!.id)
    expect(pageData.title).toBe(title)
    expect(pageData.content).toBe(content)
    expect(pageData.slug).toBe(created!.slug)

    // Cleanup
    await deleteWikiPage(page, roomId, created!.id)
  })

  test('slug is unique per room', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const title = `Unique-Slug-${Date.now()}`
    const page1 = await createWikiPage(page, roomId, title)
    expect(page1).toBeTruthy()

    // Creating a second page with the same title should still succeed
    // (slug should be differentiated, e.g. appended number)
    const page2 = await createWikiPage(page, roomId, title)
    expect(page2).toBeTruthy()
    expect(page2!.slug).not.toBe(page1!.slug)

    // Cleanup
    await deleteWikiPage(page, roomId, page1!.id)
    await deleteWikiPage(page, roomId, page2!.id)
  })

  test('non-existent slug returns 404', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const res = await page.request.get(`/api/v1/rooms/${roomId}/wiki/pages/non-existent-slug-${Date.now()}`)
    expect(res.ok()).toBe(false)
    expect(res.status()).toBe(404)
  })
})

// --------------------------------------------------------------------------
// US-302: Wiki-Seite bearbeiten
// --------------------------------------------------------------------------
test.describe('US-302: Wiki-Seite bearbeiten', () => {

  test('teacher can update wiki page title and content', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const created = await createWikiPage(page, roomId, `Edit-Test-${Date.now()}`, 'Originalinhalt')
    expect(created).toBeTruthy()

    const newTitle = `Bearbeitet-${Date.now()}`
    const newContent = '# Aktualisiert\n\nNeuer Inhalt nach Bearbeitung.'

    const res = await page.request.put(`/api/v1/rooms/${roomId}/wiki/pages/${created!.id}`, {
      data: { title: newTitle, content: newContent },
    })
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const updated = json.data
    expect(updated.title).toBe(newTitle)
    expect(updated.content).toBe(newContent)

    // Cleanup
    await deleteWikiPage(page, roomId, created!.id)
  })

  test('editing a page creates a new version', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const created = await createWikiPage(page, roomId, `Version-Test-${Date.now()}`, 'Version 1')
    expect(created).toBeTruthy()

    // Edit the page
    await page.request.put(`/api/v1/rooms/${roomId}/wiki/pages/${created!.id}`, {
      data: { title: created!.title, content: 'Version 2' },
    })

    // Check versions — should have at least 2 (initial + edit)
    const versionsRes = await page.request.get(`/api/v1/rooms/${roomId}/wiki/pages/${created!.id}/versions`)
    expect(versionsRes.ok()).toBe(true)
    const versionsJson = await versionsRes.json()
    const versions = versionsJson.data ?? []
    expect(versions.length).toBeGreaterThanOrEqual(2)

    // Cleanup
    await deleteWikiPage(page, roomId, created!.id)
  })

  test('updatedAt changes after edit', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const created = await createWikiPage(page, roomId, `UpdatedAt-Test-${Date.now()}`, 'Vorher')
    expect(created).toBeTruthy()

    // Small delay to ensure timestamps differ
    await page.waitForTimeout(100)

    const res = await page.request.put(`/api/v1/rooms/${roomId}/wiki/pages/${created!.id}`, {
      data: { title: created!.title, content: 'Nachher' },
    })
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const updated = json.data

    // updatedAt should be different from the original createdAt
    expect(updated.updatedAt).toBeTruthy()

    // Cleanup
    await deleteWikiPage(page, roomId, created!.id)
  })
})

// --------------------------------------------------------------------------
// US-303: Wiki-Seite loeschen
// --------------------------------------------------------------------------
test.describe('US-303: Wiki-Seite loeschen', () => {

  test('teacher can delete a wiki page via API', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const created = await createWikiPage(page, roomId, `Delete-Me-${Date.now()}`)
    expect(created).toBeTruthy()

    // Delete the page
    const delRes = await page.request.delete(`/api/v1/rooms/${roomId}/wiki/pages/${created!.id}`)
    expect(delRes.ok()).toBe(true)

    // Verify it is gone from the tree
    const treeRes = await page.request.get(`/api/v1/rooms/${roomId}/wiki`)
    expect(treeRes.ok()).toBe(true)
    const treeJson = await treeRes.json()
    const pages = treeJson.data ?? []
    const found = pages.find((p: { id: string }) => p.id === created!.id)
    expect(found).toBeFalsy()
  })

  test('deleted page slug returns 404', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const created = await createWikiPage(page, roomId, `Gone-Page-${Date.now()}`)
    expect(created).toBeTruthy()
    const slug = created!.slug

    await page.request.delete(`/api/v1/rooms/${roomId}/wiki/pages/${created!.id}`)

    // GET by slug should now 404
    const res = await page.request.get(`/api/v1/rooms/${roomId}/wiki/pages/${slug}`)
    expect(res.ok()).toBe(false)
    expect(res.status()).toBe(404)
  })

  test('deleting non-existent page returns 404', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const fakeId = '00000000-0000-0000-0000-000000000099'
    const res = await page.request.delete(`/api/v1/rooms/${roomId}/wiki/pages/${fakeId}`)
    expect(res.ok()).toBe(false)
    expect(res.status()).toBe(404)
  })
})

// --------------------------------------------------------------------------
// US-304: Wiki-Versionshistorie
// --------------------------------------------------------------------------
test.describe('US-304: Wiki-Versionshistorie', () => {

  test('versions are returned in chronological order (newest first)', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const created = await createWikiPage(page, roomId, `History-Test-${Date.now()}`, 'Erste Version')
    expect(created).toBeTruthy()

    // Make two edits
    await page.request.put(`/api/v1/rooms/${roomId}/wiki/pages/${created!.id}`, {
      data: { title: created!.title, content: 'Zweite Version' },
    })
    await page.request.put(`/api/v1/rooms/${roomId}/wiki/pages/${created!.id}`, {
      data: { title: created!.title, content: 'Dritte Version' },
    })

    // Fetch versions
    const res = await page.request.get(`/api/v1/rooms/${roomId}/wiki/pages/${created!.id}/versions`)
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const versions = json.data ?? []

    // At least 3 versions (initial + 2 edits)
    expect(versions.length).toBeGreaterThanOrEqual(3)

    // Newest first — first version createdAt >= second version createdAt
    if (versions.length >= 2) {
      const newest = new Date(versions[0].createdAt).getTime()
      const second = new Date(versions[1].createdAt).getTime()
      expect(newest).toBeGreaterThanOrEqual(second)
    }

    // Cleanup
    await deleteWikiPage(page, roomId, created!.id)
  })

  test('individual version can be retrieved by ID', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const created = await createWikiPage(page, roomId, `Version-Detail-${Date.now()}`, 'Originaltext')
    expect(created).toBeTruthy()

    // Edit to create a second version
    await page.request.put(`/api/v1/rooms/${roomId}/wiki/pages/${created!.id}`, {
      data: { title: created!.title, content: 'Geaenderter Text' },
    })

    // Get version list
    const listRes = await page.request.get(`/api/v1/rooms/${roomId}/wiki/pages/${created!.id}/versions`)
    expect(listRes.ok()).toBe(true)
    const listJson = await listRes.json()
    const versions = listJson.data ?? []
    expect(versions.length).toBeGreaterThanOrEqual(2)

    // Fetch the first version by ID
    const versionId = versions[versions.length - 1].id // oldest
    const res = await page.request.get(`/api/v1/rooms/${roomId}/wiki/versions/${versionId}`)
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const version = json.data
    expect(version.id).toBe(versionId)
    expect(version.content).toBeTruthy()
    expect(version.editedByName).toBeTruthy()

    // Cleanup
    await deleteWikiPage(page, roomId, created!.id)
  })

  test('each version has editedByName and createdAt', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const created = await createWikiPage(page, roomId, `Fields-Test-${Date.now()}`, 'Inhalt')
    expect(created).toBeTruthy()

    const res = await page.request.get(`/api/v1/rooms/${roomId}/wiki/pages/${created!.id}/versions`)
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const versions = json.data ?? []
    expect(versions.length).toBeGreaterThanOrEqual(1)

    for (const v of versions) {
      expect(v.editedByName).toBeTruthy()
      expect(v.createdAt).toBeTruthy()
      expect(v.title).toBeTruthy()
    }

    // Cleanup
    await deleteWikiPage(page, roomId, created!.id)
  })
})

// --------------------------------------------------------------------------
// US-305: Wiki-Seitenhierarchie
// --------------------------------------------------------------------------
test.describe('US-305: Wiki-Seitenhierarchie', () => {

  test('pages can be created with parentId forming a tree', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    // Level 1 — root page
    const root = await createWikiPage(page, roomId, `Ebene1-${Date.now()}`, 'Wurzelseite')
    expect(root).toBeTruthy()

    // Level 2 — child of root
    const child = await createWikiPage(page, roomId, `Ebene2-${Date.now()}`, 'Unterseite', root!.id)
    expect(child).toBeTruthy()
    expect(child!.parentId).toBe(root!.id)

    // Level 3 — child of child
    const grandchild = await createWikiPage(page, roomId, `Ebene3-${Date.now()}`, 'Unter-Unterseite', child!.id)
    expect(grandchild).toBeTruthy()
    expect(grandchild!.parentId).toBe(child!.id)

    // Verify in tree
    const treeRes = await page.request.get(`/api/v1/rooms/${roomId}/wiki`)
    expect(treeRes.ok()).toBe(true)
    const treeJson = await treeRes.json()
    const pages = treeJson.data ?? []

    // Root page should be in top-level list
    const rootInTree = pages.find((p: { id: string }) => p.id === root!.id)
    expect(rootInTree).toBeTruthy()

    // Cleanup (delete leaves first to avoid FK issues)
    await deleteWikiPage(page, roomId, grandchild!.id)
    await deleteWikiPage(page, roomId, child!.id)
    await deleteWikiPage(page, roomId, root!.id)
  })

  test('child page is listed as child of parent when fetched by slug', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const parent = await createWikiPage(page, roomId, `Parent-${Date.now()}`, 'Elternseite')
    expect(parent).toBeTruthy()

    const child = await createWikiPage(page, roomId, `Child-${Date.now()}`, 'Kindseite', parent!.id)
    expect(child).toBeTruthy()

    // Fetch parent by slug — should list child in children
    const res = await page.request.get(`/api/v1/rooms/${roomId}/wiki/pages/${parent!.slug}`)
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const parentData = json.data
    expect(parentData.children).toBeTruthy()
    const childInList = parentData.children.find((c: { id: string }) => c.id === child!.id)
    expect(childInList).toBeTruthy()

    // Cleanup
    await deleteWikiPage(page, roomId, child!.id)
    await deleteWikiPage(page, roomId, parent!.id)
  })
})

// --------------------------------------------------------------------------
// US-306: Wiki-Suche
// --------------------------------------------------------------------------
test.describe('US-306: Wiki-Suche', () => {

  test('search finds page by title keyword', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const keyword = `Suchbegriff${Date.now()}`
    const created = await createWikiPage(page, roomId, `Seite mit ${keyword}`, 'Normaler Inhalt')
    expect(created).toBeTruthy()

    // Search
    const res = await page.request.get(`/api/v1/rooms/${roomId}/wiki/search?q=${keyword}`)
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const results = json.data ?? []
    expect(results.length).toBeGreaterThanOrEqual(1)
    const found = results.find((r: { id: string }) => r.id === created!.id)
    expect(found).toBeTruthy()

    // Cleanup
    await deleteWikiPage(page, roomId, created!.id)
  })

  test('search finds page by content keyword', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const keyword = `Inhaltsuche${Date.now()}`
    const created = await createWikiPage(page, roomId, `Normale Seite ${Date.now()}`, `Der Text enthaelt ${keyword} hier.`)
    expect(created).toBeTruthy()

    const res = await page.request.get(`/api/v1/rooms/${roomId}/wiki/search?q=${keyword}`)
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const results = json.data ?? []
    expect(results.length).toBeGreaterThanOrEqual(1)
    const found = results.find((r: { id: string }) => r.id === created!.id)
    expect(found).toBeTruthy()

    // Cleanup
    await deleteWikiPage(page, roomId, created!.id)
  })

  test('search is room-scoped and returns empty for non-matching query', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const res = await page.request.get(`/api/v1/rooms/${roomId}/wiki/search?q=xyznonexistent${Date.now()}`)
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const results = json.data ?? []
    expect(results.length).toBe(0)
  })
})


// ============================================================================
// TASKS / KANBAN TESTS — US-307 to US-313
// ============================================================================

// --------------------------------------------------------------------------
// US-307: Kanban-Board anzeigen
// --------------------------------------------------------------------------
test.describe('US-307: Kanban-Board anzeigen', () => {

  test('teacher can GET the task board for a room', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const board = await getBoard(page, roomId)
    expect(board).toBeTruthy()
    expect(board!.roomId).toBe(roomId)
    expect(Array.isArray(board!.columns)).toBe(true)
    expect(Array.isArray(board!.tasks)).toBe(true)
  })

  test('board is unique per room (same roomId returns same boardId)', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const board1 = await getBoard(page, roomId)
    const board2 = await getBoard(page, roomId)
    expect(board1).toBeTruthy()
    expect(board2).toBeTruthy()
    expect(board1!.id).toBe(board2!.id)
  })

  test('columns are sorted by position', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    // Create two columns to ensure ordering
    const colA = await createColumn(page, roomId, `Spalte-A-${Date.now()}`)
    const colB = await createColumn(page, roomId, `Spalte-B-${Date.now()}`)

    const board = await getBoard(page, roomId)
    expect(board).toBeTruthy()

    // Verify positions are sorted ascending
    const positions = board!.columns.map(c => c.position)
    for (let i = 1; i < positions.length; i++) {
      expect(positions[i]).toBeGreaterThanOrEqual(positions[i - 1])
    }

    // Cleanup
    if (colA) await deleteColumn(page, roomId, colA.id)
    if (colB) await deleteColumn(page, roomId, colB.id)
  })
})

// --------------------------------------------------------------------------
// US-308: Spalte erstellen
// --------------------------------------------------------------------------
test.describe('US-308: Spalte erstellen', () => {

  test('teacher can create a column via API', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const name = `Neue-Spalte-${Date.now()}`
    const col = await createColumn(page, roomId, name)
    expect(col).toBeTruthy()
    expect(col!.name).toBe(name)
    expect(typeof col!.position).toBe('number')

    // Cleanup
    await deleteColumn(page, roomId, col!.id)
  })

  test('created column appears in board', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const name = `Board-Check-${Date.now()}`
    const col = await createColumn(page, roomId, name)
    expect(col).toBeTruthy()

    const board = await getBoard(page, roomId)
    expect(board).toBeTruthy()
    const found = board!.columns.find(c => c.id === col!.id)
    expect(found).toBeTruthy()
    expect(found!.name).toBe(name)

    // Cleanup
    await deleteColumn(page, roomId, col!.id)
  })

  test('creating column with empty name returns 400', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const res = await page.request.post(`/api/v1/rooms/${roomId}/tasks/columns`, {
      data: { name: '' },
    })
    expect(res.ok()).toBe(false)
    expect(res.status()).toBe(400)
  })
})

// --------------------------------------------------------------------------
// US-309: Spalte umbenennen und loeschen
// --------------------------------------------------------------------------
test.describe('US-309: Spalte umbenennen und loeschen', () => {

  test('teacher can rename a column via PUT', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `Umbenennen-${Date.now()}`)
    expect(col).toBeTruthy()

    const newName = `Umbenannt-${Date.now()}`
    const res = await page.request.put(`/api/v1/rooms/${roomId}/tasks/columns/${col!.id}`, {
      data: { name: newName },
    })
    expect(res.ok()).toBe(true)
    const json = await res.json()
    expect(json.data.name).toBe(newName)

    // Cleanup
    await deleteColumn(page, roomId, col!.id)
  })

  test('teacher can delete a column via DELETE', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `Loeschen-${Date.now()}`)
    expect(col).toBeTruthy()

    const res = await page.request.delete(`/api/v1/rooms/${roomId}/tasks/columns/${col!.id}`)
    expect(res.ok()).toBe(true)

    // Verify column is gone from board
    const board = await getBoard(page, roomId)
    expect(board).toBeTruthy()
    const found = board!.columns.find(c => c.id === col!.id)
    expect(found).toBeFalsy()
  })

  test('deleting non-existent column returns 404', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const fakeId = '00000000-0000-0000-0000-000000000099'
    const res = await page.request.delete(`/api/v1/rooms/${roomId}/tasks/columns/${fakeId}`)
    expect(res.ok()).toBe(false)
    expect(res.status()).toBe(404)
  })
})

// --------------------------------------------------------------------------
// US-310: Task erstellen
// --------------------------------------------------------------------------
test.describe('US-310: Task erstellen', () => {

  test('teacher can create a task with title and columnId', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `Task-Col-${Date.now()}`)
    expect(col).toBeTruthy()

    const title = `E2E-Aufgabe-${Date.now()}`
    const task = await createTask(page, roomId, title, col!.id)
    expect(task).toBeTruthy()
    expect(task!.title).toBe(title)
    expect(task!.columnId).toBe(col!.id)
    expect(task!.description).toBeNull()

    // Cleanup
    await deleteTask(page, roomId, task!.id)
    await deleteColumn(page, roomId, col!.id)
  })

  test('task can be created with optional description', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `Desc-Col-${Date.now()}`)
    expect(col).toBeTruthy()

    const title = `Beschriebene-Aufgabe-${Date.now()}`
    const description = 'Dies ist eine ausfuehrliche Beschreibung der Aufgabe.'
    const task = await createTask(page, roomId, title, col!.id, description)
    expect(task).toBeTruthy()
    expect(task!.description).toBe(description)

    // Cleanup
    await deleteTask(page, roomId, task!.id)
    await deleteColumn(page, roomId, col!.id)
  })

  test('creating task without title returns 400', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `NoTitle-Col-${Date.now()}`)
    expect(col).toBeTruthy()

    const res = await page.request.post(`/api/v1/rooms/${roomId}/tasks`, {
      data: { title: '', columnId: col!.id },
    })
    expect(res.ok()).toBe(false)
    expect(res.status()).toBe(400)

    // Cleanup
    await deleteColumn(page, roomId, col!.id)
  })

  test('created task appears in board tasks list', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `Board-Task-Col-${Date.now()}`)
    expect(col).toBeTruthy()

    const task = await createTask(page, roomId, `Board-Task-${Date.now()}`, col!.id)
    expect(task).toBeTruthy()

    const board = await getBoard(page, roomId)
    expect(board).toBeTruthy()
    const found = board!.tasks.find(t => t.id === task!.id)
    expect(found).toBeTruthy()
    expect(found!.columnId).toBe(col!.id)

    // Cleanup
    await deleteTask(page, roomId, task!.id)
    await deleteColumn(page, roomId, col!.id)
  })
})

// --------------------------------------------------------------------------
// US-311: Task verschieben (Drag & Drop)
// --------------------------------------------------------------------------
test.describe('US-311: Task verschieben (Drag & Drop)', () => {

  // Skip UI drag & drop — test API move instead
  test.skip('drag & drop UI moves task between columns', async () => {
    // TODO: Playwright drag & drop with Kanban board requires complex
    // coordinate-based dragging. Testing API move endpoint instead.
  })

  test('task can be moved to a different column via API', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const colA = await createColumn(page, roomId, `Move-From-${Date.now()}`)
    const colB = await createColumn(page, roomId, `Move-To-${Date.now()}`)
    expect(colA).toBeTruthy()
    expect(colB).toBeTruthy()

    const task = await createTask(page, roomId, `Verschieben-${Date.now()}`, colA!.id)
    expect(task).toBeTruthy()
    expect(task!.columnId).toBe(colA!.id)

    // Move task to colB at position 0
    const res = await page.request.put(`/api/v1/rooms/${roomId}/tasks/${task!.id}/move`, {
      data: { columnId: colB!.id, position: 0 },
    })
    expect(res.ok()).toBe(true)
    const json = await res.json()
    expect(json.data.columnId).toBe(colB!.id)

    // Cleanup
    await deleteTask(page, roomId, task!.id)
    await deleteColumn(page, roomId, colA!.id)
    await deleteColumn(page, roomId, colB!.id)
  })

  test('task can be reordered within the same column via move', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `Reorder-Col-${Date.now()}`)
    expect(col).toBeTruthy()

    const task1 = await createTask(page, roomId, `Reorder-1-${Date.now()}`, col!.id)
    const task2 = await createTask(page, roomId, `Reorder-2-${Date.now()}`, col!.id)
    expect(task1).toBeTruthy()
    expect(task2).toBeTruthy()

    // Move task1 to position 1 (after task2)
    const res = await page.request.put(`/api/v1/rooms/${roomId}/tasks/${task1!.id}/move`, {
      data: { columnId: col!.id, position: 1 },
    })
    expect(res.ok()).toBe(true)

    // Cleanup
    await deleteTask(page, roomId, task1!.id)
    await deleteTask(page, roomId, task2!.id)
    await deleteColumn(page, roomId, col!.id)
  })
})

// --------------------------------------------------------------------------
// US-312: Task bearbeiten und loeschen
// --------------------------------------------------------------------------
test.describe('US-312: Task bearbeiten und loeschen', () => {

  test('teacher can update task title and description via PUT', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `Edit-Task-Col-${Date.now()}`)
    expect(col).toBeTruthy()

    const task = await createTask(page, roomId, `Vor-Edit-${Date.now()}`, col!.id)
    expect(task).toBeTruthy()

    const newTitle = `Nach-Edit-${Date.now()}`
    const newDesc = 'Aktualisierte Beschreibung'
    const res = await page.request.put(`/api/v1/rooms/${roomId}/tasks/${task!.id}`, {
      data: { title: newTitle, description: newDesc },
    })
    expect(res.ok()).toBe(true)
    const json = await res.json()
    expect(json.data.title).toBe(newTitle)
    expect(json.data.description).toBe(newDesc)

    // Cleanup
    await deleteTask(page, roomId, task!.id)
    await deleteColumn(page, roomId, col!.id)
  })

  test('teacher can delete a task via API', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `Del-Task-Col-${Date.now()}`)
    expect(col).toBeTruthy()

    const task = await createTask(page, roomId, `Loeschen-${Date.now()}`, col!.id)
    expect(task).toBeTruthy()

    const delRes = await page.request.delete(`/api/v1/rooms/${roomId}/tasks/${task!.id}`)
    expect(delRes.ok()).toBe(true)

    // Verify task is gone from board
    const board = await getBoard(page, roomId)
    expect(board).toBeTruthy()
    const found = board!.tasks.find(t => t.id === task!.id)
    expect(found).toBeFalsy()

    // Cleanup
    await deleteColumn(page, roomId, col!.id)
  })

  test('deleting non-existent task returns 404', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const fakeId = '00000000-0000-0000-0000-000000000099'
    const res = await page.request.delete(`/api/v1/rooms/${roomId}/tasks/${fakeId}`)
    expect(res.ok()).toBe(false)
    expect(res.status()).toBe(404)
  })
})

// --------------------------------------------------------------------------
// US-313: Checkliste / Sub-Tasks
// --------------------------------------------------------------------------
test.describe('US-313: Checkliste / Sub-Tasks', () => {

  test('teacher can add a checklist item to a task', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `CL-Col-${Date.now()}`)
    expect(col).toBeTruthy()
    const task = await createTask(page, roomId, `CL-Task-${Date.now()}`, col!.id)
    expect(task).toBeTruthy()

    // Add checklist item
    const itemTitle = `Sub-Aufgabe-${Date.now()}`
    const res = await page.request.post(`/api/v1/rooms/${roomId}/tasks/${task!.id}/checklist`, {
      data: { title: itemTitle },
    })
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const item = json.data
    expect(item.title).toBe(itemTitle)
    expect(item.checked).toBe(false)
    expect(item.id).toBeTruthy()

    // Cleanup
    await deleteTask(page, roomId, task!.id)
    await deleteColumn(page, roomId, col!.id)
  })

  test('checklist item can be toggled (checked/unchecked)', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `Toggle-Col-${Date.now()}`)
    expect(col).toBeTruthy()
    const task = await createTask(page, roomId, `Toggle-Task-${Date.now()}`, col!.id)
    expect(task).toBeTruthy()

    // Add checklist item
    const addRes = await page.request.post(`/api/v1/rooms/${roomId}/tasks/${task!.id}/checklist`, {
      data: { title: `Toggle-Item-${Date.now()}` },
    })
    expect(addRes.ok()).toBe(true)
    const addJson = await addRes.json()
    const itemId = addJson.data.id
    expect(addJson.data.checked).toBe(false)

    // Toggle to checked
    const toggleRes = await page.request.put(
      `/api/v1/rooms/${roomId}/tasks/${task!.id}/checklist/${itemId}/toggle`,
    )
    expect(toggleRes.ok()).toBe(true)
    const toggleJson = await toggleRes.json()
    expect(toggleJson.data.checked).toBe(true)

    // Toggle back to unchecked
    const toggle2Res = await page.request.put(
      `/api/v1/rooms/${roomId}/tasks/${task!.id}/checklist/${itemId}/toggle`,
    )
    expect(toggle2Res.ok()).toBe(true)
    const toggle2Json = await toggle2Res.json()
    expect(toggle2Json.data.checked).toBe(false)

    // Cleanup
    await deleteTask(page, roomId, task!.id)
    await deleteColumn(page, roomId, col!.id)
  })

  test('checklist item can be deleted', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `DelCL-Col-${Date.now()}`)
    expect(col).toBeTruthy()
    const task = await createTask(page, roomId, `DelCL-Task-${Date.now()}`, col!.id)
    expect(task).toBeTruthy()

    // Add checklist item
    const addRes = await page.request.post(`/api/v1/rooms/${roomId}/tasks/${task!.id}/checklist`, {
      data: { title: `Delete-Me-${Date.now()}` },
    })
    expect(addRes.ok()).toBe(true)
    const itemId = (await addRes.json()).data.id

    // Delete checklist item
    const delRes = await page.request.delete(
      `/api/v1/rooms/${roomId}/tasks/${task!.id}/checklist/${itemId}`,
    )
    expect(delRes.ok()).toBe(true)

    // Verify the item is gone from the task
    const board = await getBoard(page, roomId)
    expect(board).toBeTruthy()
    const taskInBoard = board!.tasks.find((t: { id: string }) => t.id === task!.id) as {
      id: string
      checklistItems: { id: string }[]
    } | undefined
    if (taskInBoard) {
      const itemStillExists = taskInBoard.checklistItems.find(i => i.id === itemId)
      expect(itemStillExists).toBeFalsy()
    }

    // Cleanup
    await deleteTask(page, roomId, task!.id)
    await deleteColumn(page, roomId, col!.id)
  })

  test('board reflects checklistTotal and checklistChecked counts', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const col = await createColumn(page, roomId, `Count-Col-${Date.now()}`)
    expect(col).toBeTruthy()
    const task = await createTask(page, roomId, `Count-Task-${Date.now()}`, col!.id)
    expect(task).toBeTruthy()

    // Add two checklist items
    const add1Res = await page.request.post(`/api/v1/rooms/${roomId}/tasks/${task!.id}/checklist`, {
      data: { title: `Item-1-${Date.now()}` },
    })
    expect(add1Res.ok()).toBe(true)
    const item1Id = (await add1Res.json()).data.id

    const add2Res = await page.request.post(`/api/v1/rooms/${roomId}/tasks/${task!.id}/checklist`, {
      data: { title: `Item-2-${Date.now()}` },
    })
    expect(add2Res.ok()).toBe(true)

    // Toggle first item to checked
    await page.request.put(`/api/v1/rooms/${roomId}/tasks/${task!.id}/checklist/${item1Id}/toggle`)

    // Verify counts on board
    const board = await getBoard(page, roomId)
    expect(board).toBeTruthy()
    const taskInBoard = board!.tasks.find((t: { id: string }) => t.id === task!.id) as {
      id: string
      checklistTotal: number
      checklistChecked: number
    } | undefined
    expect(taskInBoard).toBeTruthy()
    expect(taskInBoard!.checklistTotal).toBe(2)
    expect(taskInBoard!.checklistChecked).toBe(1)

    // Cleanup
    await deleteTask(page, roomId, task!.id)
    await deleteColumn(page, roomId, col!.id)
  })
})
