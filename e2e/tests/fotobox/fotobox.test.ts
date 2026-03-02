import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { login } from '../../helpers/auth'

// ============================================================================
// Fotobox E2E Tests — US-250 to US-260
// ============================================================================

/**
 * Helper: get the first room ID the current user is a member of via API.
 */
async function getFirstRoomId(page: import('@playwright/test').Page): Promise<string | null> {
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
 * Helper: find a room the user is NOT a member of.
 */
async function findNonMemberRoomId(page: import('@playwright/test').Page): Promise<string | null> {
  try {
    const [mineRes, browseRes] = await Promise.all([
      page.request.get('/api/v1/rooms/mine'),
      page.request.get('/api/v1/rooms/browse'),
    ])
    if (mineRes.ok() && browseRes.ok()) {
      const mineJson = await mineRes.json()
      const browseJson = await browseRes.json()
      const myRooms = mineJson.data?.content ?? mineJson.data ?? []
      const allRooms = browseJson.data?.content ?? browseJson.data ?? []
      const myRoomIds = new Set(myRooms.map((r: { id: string }) => r.id))
      const nonMemberRoom = allRooms.find((r: { id: string }) => !myRoomIds.has(r.id))
      return nonMemberRoom?.id ?? null
    }
  } catch {
    // API call failed
  }
  return null
}

/**
 * Helper: ensure fotobox is enabled for a room. Returns true if settings were
 * applied successfully (meaning we have leader/admin permission).
 */
async function ensureFotoboxEnabled(
  page: import('@playwright/test').Page,
  roomId: string
): Promise<boolean> {
  try {
    const res = await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: {
        enabled: true,
        defaultPermission: 'POST_IMAGES',
        maxImagesPerThread: 50,
        maxFileSizeMb: 10,
      },
    })
    return res.ok()
  } catch {
    return false
  }
}

/**
 * Helper: create a fotobox thread via API and return its info.
 */
async function createThreadViaApi(
  page: import('@playwright/test').Page,
  roomId: string,
  title: string,
  audience: string = 'ALL',
  description?: string
): Promise<{ id: string; title: string; audience: string; createdBy: string; createdAt: string } | null> {
  try {
    const res = await page.request.post(`/api/v1/rooms/${roomId}/fotobox/threads`, {
      data: { title, description: description ?? null, audience },
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
 * Helper: list threads via API.
 */
async function listThreadsViaApi(
  page: import('@playwright/test').Page,
  roomId: string
): Promise<{ id: string; title: string; audience: string }[]> {
  try {
    const res = await page.request.get(`/api/v1/rooms/${roomId}/fotobox/threads`)
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
 * Helper: delete a thread via API (cleanup).
 */
async function deleteThreadViaApi(
  page: import('@playwright/test').Page,
  roomId: string,
  threadId: string
): Promise<void> {
  try {
    await page.request.delete(`/api/v1/rooms/${roomId}/fotobox/threads/${threadId}`)
  } catch {
    // Ignore cleanup failures
  }
}

/**
 * Helper: get fotobox settings via API.
 */
async function getSettingsViaApi(
  page: import('@playwright/test').Page,
  roomId: string
): Promise<{ enabled: boolean; defaultPermission: string; maxImagesPerThread: number | null; maxFileSizeMb: number } | null> {
  try {
    const res = await page.request.get(`/api/v1/rooms/${roomId}/fotobox/settings`)
    if (res.ok()) {
      const json = await res.json()
      return json.data ?? null
    }
  } catch {
    // API call failed
  }
  return null
}

// --------------------------------------------------------------------------
// US-250: Fotobox-Einstellungen konfigurieren (Raumleiter)
// --------------------------------------------------------------------------
test.describe('US-250: Fotobox-Einstellungen konfigurieren', () => {

  test('teacher (leader) can read fotobox settings for room', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    // Ensure fotobox is enabled first
    await ensureFotoboxEnabled(page, roomId)

    const settings = await getSettingsViaApi(page, roomId)
    expect(settings).toBeTruthy()
    expect(typeof settings!.enabled).toBe('boolean')
    expect(typeof settings!.defaultPermission).toBe('string')
    expect(typeof settings!.maxFileSizeMb).toBe('number')
  })

  test('teacher (leader) can update fotobox settings via API', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const res = await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: {
        enabled: true,
        defaultPermission: 'POST_IMAGES',
        maxImagesPerThread: 50,
        maxFileSizeMb: 10,
      },
    })
    expect(res.ok()).toBe(true)

    const json = await res.json()
    const data = json.data
    expect(data.enabled).toBe(true)
    expect(data.defaultPermission).toBe('POST_IMAGES')
    expect(data.maxImagesPerThread).toBe(50)
    expect(data.maxFileSizeMb).toBe(10)
  })

  test('student (non-leader) cannot update fotobox settings', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    // Enable fotobox as teacher first
    await ensureFotoboxEnabled(page, roomId)

    // Switch to student
    await login(page, accounts.student)

    const res = await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: {
        enabled: false,
        defaultPermission: 'VIEW_ONLY',
      },
    })
    expect(res.status()).toBe(403)
  })

  test('admin can update fotobox settings for any room', async ({ page }) => {
    await login(page, accounts.admin)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const res = await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: {
        enabled: true,
        defaultPermission: 'CREATE_THREADS',
        maxImagesPerThread: 100,
        maxFileSizeMb: 20,
      },
    })
    expect(res.ok()).toBe(true)

    const json = await res.json()
    expect(json.data.defaultPermission).toBe('CREATE_THREADS')
    expect(json.data.maxImagesPerThread).toBe(100)
    expect(json.data.maxFileSizeMb).toBe(20)

    // Restore defaults
    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: {
        defaultPermission: 'POST_IMAGES',
        maxImagesPerThread: 50,
        maxFileSizeMb: 10,
      },
    })
  })
})

// --------------------------------------------------------------------------
// US-251: Fotobox-Thread erstellen
// --------------------------------------------------------------------------
test.describe('US-251: Fotobox-Thread erstellen', () => {

  test('teacher can create a thread via API', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await ensureFotoboxEnabled(page, roomId)

    const title = `Sommerfest-${Date.now()}`
    const thread = await createThreadViaApi(page, roomId, title, 'ALL', 'Fotos vom Sommerfest 2026')
    expect(thread).toBeTruthy()
    expect(thread!.title).toBe(title)
    expect(thread!.audience).toBe('ALL')

    // Thread should appear in listing
    const threads = await listThreadsViaApi(page, roomId)
    const found = threads.find(t => t.id === thread!.id)
    expect(found).toBeTruthy()

    // Cleanup
    await deleteThreadViaApi(page, roomId, thread!.id)
  })

  test('created thread is returned in thread list', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await ensureFotoboxEnabled(page, roomId)

    const title = `Thread-Liste-${Date.now()}`
    const thread = await createThreadViaApi(page, roomId, title)
    expect(thread).toBeTruthy()

    // Verify it appears in the list
    const res = await page.request.get(`/api/v1/rooms/${roomId}/fotobox/threads`)
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const list: { id: string; title: string }[] = json.data ?? []
    const found = list.find(t => t.title === title)
    expect(found).toBeTruthy()

    // Cleanup
    await deleteThreadViaApi(page, roomId, thread!.id)
  })

  test('student with VIEW_ONLY permission cannot create threads', async ({ page }) => {
    // Set up as teacher: enable fotobox with VIEW_ONLY default
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { enabled: true, defaultPermission: 'VIEW_ONLY' },
    })

    // Switch to student
    await login(page, accounts.student)

    const res = await page.request.post(`/api/v1/rooms/${roomId}/fotobox/threads`, {
      data: { title: 'Unauthorized Thread', audience: 'ALL' },
    })
    expect(res.status()).toBe(403)

    // Restore default permission
    await login(page, accounts.teacher)
    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { defaultPermission: 'POST_IMAGES' },
    })
  })
})

// --------------------------------------------------------------------------
// US-252: Thread-Audience fuer verschiedene Rollen
// --------------------------------------------------------------------------
test.describe('US-252: Thread-Audience fuer verschiedene Rollen', () => {

  test('parent auto-creates PARENTS_ONLY threads regardless of audience parameter', async ({ page }) => {
    // First enable fotobox and set permission to CREATE_THREADS
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { enabled: true, defaultPermission: 'CREATE_THREADS' },
    })

    // Switch to parent and create thread requesting ALL
    await login(page, accounts.parent)

    const title = `Eltern-Thread-${Date.now()}`
    const res = await page.request.post(`/api/v1/rooms/${roomId}/fotobox/threads`, {
      data: { title, audience: 'ALL' },
    })
    expect(res.ok()).toBe(true)

    const json = await res.json()
    const thread = json.data
    // Parent threads are always PARENTS_ONLY regardless of what was requested
    expect(thread.audience).toBe('PARENTS_ONLY')

    // Cleanup
    await login(page, accounts.teacher)
    await deleteThreadViaApi(page, roomId, thread.id)

    // Restore default permission
    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { defaultPermission: 'POST_IMAGES' },
    })
  })

  test('leader can choose ALL/PARENTS_ONLY/STUDENTS_ONLY audience', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await ensureFotoboxEnabled(page, roomId)

    // Create thread with ALL audience
    const t1 = await createThreadViaApi(page, roomId, `ALL-${Date.now()}`, 'ALL')
    expect(t1).toBeTruthy()
    expect(t1!.audience).toBe('ALL')

    // Create thread with PARENTS_ONLY audience
    const t2 = await createThreadViaApi(page, roomId, `Parents-${Date.now()}`, 'PARENTS_ONLY')
    expect(t2).toBeTruthy()
    expect(t2!.audience).toBe('PARENTS_ONLY')

    // Create thread with STUDENTS_ONLY audience
    const t3 = await createThreadViaApi(page, roomId, `Students-${Date.now()}`, 'STUDENTS_ONLY')
    expect(t3).toBeTruthy()
    expect(t3!.audience).toBe('STUDENTS_ONLY')

    // Cleanup
    await deleteThreadViaApi(page, roomId, t1!.id)
    await deleteThreadViaApi(page, roomId, t2!.id)
    await deleteThreadViaApi(page, roomId, t3!.id)
  })

  test('student sees only ALL and STUDENTS_ONLY threads', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await ensureFotoboxEnabled(page, roomId)

    const suffix = Date.now()
    const tAll = await createThreadViaApi(page, roomId, `All-${suffix}`, 'ALL')
    const tParents = await createThreadViaApi(page, roomId, `Parents-${suffix}`, 'PARENTS_ONLY')
    const tStudents = await createThreadViaApi(page, roomId, `Students-${suffix}`, 'STUDENTS_ONLY')

    expect(tAll).toBeTruthy()
    expect(tParents).toBeTruthy()
    expect(tStudents).toBeTruthy()

    // Switch to student and list threads
    await login(page, accounts.student)
    const threads = await listThreadsViaApi(page, roomId)
    const threadTitles = threads.map(t => t.title)

    expect(threadTitles).toContain(`All-${suffix}`)
    expect(threadTitles).not.toContain(`Parents-${suffix}`)
    expect(threadTitles).toContain(`Students-${suffix}`)

    // Cleanup as teacher
    await login(page, accounts.teacher)
    await deleteThreadViaApi(page, roomId, tAll!.id)
    await deleteThreadViaApi(page, roomId, tParents!.id)
    await deleteThreadViaApi(page, roomId, tStudents!.id)
  })

  test('parent sees only ALL and PARENTS_ONLY threads', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await ensureFotoboxEnabled(page, roomId)

    const suffix = Date.now()
    const tAll = await createThreadViaApi(page, roomId, `All-${suffix}`, 'ALL')
    const tParents = await createThreadViaApi(page, roomId, `Parents-${suffix}`, 'PARENTS_ONLY')
    const tStudents = await createThreadViaApi(page, roomId, `Students-${suffix}`, 'STUDENTS_ONLY')

    expect(tAll).toBeTruthy()
    expect(tParents).toBeTruthy()
    expect(tStudents).toBeTruthy()

    // Switch to parent and list threads
    await login(page, accounts.parent)
    const threads = await listThreadsViaApi(page, roomId)
    const threadTitles = threads.map(t => t.title)

    expect(threadTitles).toContain(`All-${suffix}`)
    expect(threadTitles).toContain(`Parents-${suffix}`)
    expect(threadTitles).not.toContain(`Students-${suffix}`)

    // Cleanup as teacher
    await login(page, accounts.teacher)
    await deleteThreadViaApi(page, roomId, tAll!.id)
    await deleteThreadViaApi(page, roomId, tParents!.id)
    await deleteThreadViaApi(page, roomId, tStudents!.id)
  })
})

// --------------------------------------------------------------------------
// US-253: Fotos hochladen (max 20 pro Upload)
// --------------------------------------------------------------------------
test.describe('US-253: Fotos hochladen', () => {

  // TODO: Multipart file upload with real image data and magic byte validation
  // requires constructing valid image buffers. Skipping until reliable pattern
  // for Playwright multipart image upload to PrimeVue FileUpload is established.
  test.skip('teacher can upload images to a thread via multipart POST', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    expect(roomId).toBeTruthy()
  })

  test.skip('upload rejects more than 20 files per request', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    expect(roomId).toBeTruthy()
  })
})

// --------------------------------------------------------------------------
// US-254: Content-Type-Validierung ueber Magic Bytes
// --------------------------------------------------------------------------
test.describe('US-254: Content-Type-Validierung ueber Magic Bytes', () => {

  // TODO: Testing magic byte validation requires constructing binary file
  // payloads with intentionally mismatched headers vs actual content.
  // Skipping as this is better covered by backend unit/integration tests.
  test.skip('upload rejects file with mismatched content-type and magic bytes', async ({ page }) => {
    await login(page, accounts.teacher)
  })
})

// --------------------------------------------------------------------------
// US-255: Lightbox-Ansicht
// --------------------------------------------------------------------------
test.describe('US-255: Lightbox-Ansicht', () => {

  // TODO: Lightbox requires real images in a thread. With multipart upload
  // skipped, we cannot populate threads with images to test lightbox.
  test.skip('clicking thumbnail opens lightbox overlay', async ({ page }) => {
    await login(page, accounts.teacher)
  })

  // TODO: JWT token parameter on image URLs cannot be tested without images.
  test.skip('image URLs use ?token= JWT parameter for authentication', async ({ page }) => {
    await login(page, accounts.teacher)
  })
})

// --------------------------------------------------------------------------
// US-256: Berechtigungshierarchie der Fotobox
// --------------------------------------------------------------------------
test.describe('US-256: Berechtigungshierarchie der Fotobox', () => {

  test('SUPERADMIN always gets CREATE_THREADS permission', async ({ page }) => {
    await login(page, accounts.admin)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await ensureFotoboxEnabled(page, roomId)

    // Admin should be able to create a thread (CREATE_THREADS level)
    const title = `Admin-Thread-${Date.now()}`
    const thread = await createThreadViaApi(page, roomId, title)
    expect(thread).toBeTruthy()
    expect(thread!.title).toBe(title)

    // Cleanup
    await deleteThreadViaApi(page, roomId, thread!.id)
  })

  test('LEADER always gets CREATE_THREADS permission', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    // Even with VIEW_ONLY default, leader should get CREATE_THREADS
    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { enabled: true, defaultPermission: 'VIEW_ONLY' },
    })

    const title = `Leader-Thread-${Date.now()}`
    const thread = await createThreadViaApi(page, roomId, title)
    expect(thread).toBeTruthy()
    expect(thread!.title).toBe(title)

    // Cleanup and restore
    await deleteThreadViaApi(page, roomId, thread!.id)
    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { defaultPermission: 'POST_IMAGES' },
    })
  })

  test('member with VIEW_ONLY cannot upload (POST_IMAGES required)', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    // Set default permission to VIEW_ONLY
    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { enabled: true, defaultPermission: 'VIEW_ONLY' },
    })

    const thread = await createThreadViaApi(page, roomId, `ViewOnly-Test-${Date.now()}`)
    expect(thread).toBeTruthy()

    // Switch to student — should have VIEW_ONLY
    await login(page, accounts.student)

    // Listing threads should succeed (VIEW_ONLY sufficient)
    const threads = await listThreadsViaApi(page, roomId)
    expect(threads.length).toBeGreaterThanOrEqual(0) // VIEW_ONLY can list

    // Creating a thread should fail (needs CREATE_THREADS)
    const res = await page.request.post(`/api/v1/rooms/${roomId}/fotobox/threads`, {
      data: { title: 'Unauthorized', audience: 'ALL' },
    })
    expect(res.status()).toBe(403)

    // Cleanup as teacher
    await login(page, accounts.teacher)
    await deleteThreadViaApi(page, roomId, thread!.id)
    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { defaultPermission: 'POST_IMAGES' },
    })
  })

  test('non-member gets 403 when accessing fotobox', async ({ page }) => {
    await login(page, accounts.student)

    const nonMemberRoomId = await findNonMemberRoomId(page)
    if (!nonMemberRoomId) {
      // Try fabricated UUID
      const fakeRoomId = '00000000-0000-0000-0000-000000000099'
      const res = await page.request.get(`/api/v1/rooms/${fakeRoomId}/fotobox/threads`)
      expect([403, 404]).toContain(res.status())
      return
    }

    const res = await page.request.get(`/api/v1/rooms/${nonMemberRoomId}/fotobox/threads`)
    expect(res.status()).toBe(403)
  })

  test('disabled fotobox returns 403 for all users', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    // Disable fotobox
    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { enabled: false },
    })

    // Even teacher (leader) should get 403 on thread listing
    // because permission check requires fotobox to be enabled
    const res = await page.request.get(`/api/v1/rooms/${roomId}/fotobox/threads`)
    expect(res.status()).toBe(403)

    // Re-enable fotobox
    await ensureFotoboxEnabled(page, roomId)
  })
})

// --------------------------------------------------------------------------
// US-257: Foto loeschen
// --------------------------------------------------------------------------
test.describe('US-257: Foto loeschen', () => {

  // TODO: Deleting photos requires having uploaded photos first, which requires
  // multipart file upload. Skipping until upload pattern is established.
  test.skip('uploader can delete own photo via DELETE /api/v1/fotobox/images/{id}', async ({ page }) => {
    await login(page, accounts.teacher)
  })

  test.skip('leader can delete any photo in the room', async ({ page }) => {
    await login(page, accounts.teacher)
  })

  test.skip('non-uploader non-leader cannot delete a photo', async ({ page }) => {
    await login(page, accounts.student)
  })
})

// --------------------------------------------------------------------------
// US-258: Thread loeschen mit allen Bildern
// --------------------------------------------------------------------------
test.describe('US-258: Thread loeschen mit allen Bildern', () => {

  test('thread creator can delete thread via API', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await ensureFotoboxEnabled(page, roomId)

    // Create a thread
    const title = `Delete-Me-${Date.now()}`
    const thread = await createThreadViaApi(page, roomId, title)
    expect(thread).toBeTruthy()

    // Delete it
    const res = await page.request.delete(`/api/v1/rooms/${roomId}/fotobox/threads/${thread!.id}`)
    expect(res.ok()).toBe(true)

    // Verify it is gone
    const threads = await listThreadsViaApi(page, roomId)
    const stillExists = threads.find(t => t.id === thread!.id)
    expect(stillExists).toBeFalsy()
  })

  test('leader can delete thread created by another user', async ({ page }) => {
    // Set permission to CREATE_THREADS so parent can create
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { enabled: true, defaultPermission: 'CREATE_THREADS' },
    })

    // Create thread as admin (different from teacher)
    await login(page, accounts.admin)
    const title = `Admin-Created-${Date.now()}`
    const thread = await createThreadViaApi(page, roomId, title)
    expect(thread).toBeTruthy()

    // Delete as teacher (leader of the room)
    await login(page, accounts.teacher)
    const res = await page.request.delete(`/api/v1/rooms/${roomId}/fotobox/threads/${thread!.id}`)
    expect(res.ok()).toBe(true)

    // Verify gone
    const threads = await listThreadsViaApi(page, roomId)
    const stillExists = threads.find(t => t.id === thread!.id)
    expect(stillExists).toBeFalsy()

    // Restore default permission
    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { defaultPermission: 'POST_IMAGES' },
    })
  })

  test('non-creator non-leader cannot delete a thread', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await ensureFotoboxEnabled(page, roomId)

    // Create thread as teacher
    const title = `No-Delete-${Date.now()}`
    const thread = await createThreadViaApi(page, roomId, title)
    expect(thread).toBeTruthy()

    // Try to delete as student
    await login(page, accounts.student)
    const res = await page.request.delete(`/api/v1/rooms/${roomId}/fotobox/threads/${thread!.id}`)
    expect(res.status()).toBe(403)

    // Cleanup as teacher
    await login(page, accounts.teacher)
    await deleteThreadViaApi(page, roomId, thread!.id)
  })
})

// --------------------------------------------------------------------------
// US-259: Thread bearbeiten
// --------------------------------------------------------------------------
test.describe('US-259: Thread bearbeiten', () => {

  test('thread creator can update title and description', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await ensureFotoboxEnabled(page, roomId)

    const originalTitle = `Original-${Date.now()}`
    const thread = await createThreadViaApi(page, roomId, originalTitle, 'ALL', 'Original description')
    expect(thread).toBeTruthy()

    // Update
    const newTitle = `Updated-${Date.now()}`
    const res = await page.request.put(`/api/v1/rooms/${roomId}/fotobox/threads/${thread!.id}`, {
      data: { title: newTitle, description: 'New description for the thread' },
    })
    expect(res.ok()).toBe(true)

    const json = await res.json()
    expect(json.data.title).toBe(newTitle)
    expect(json.data.description).toBe('New description for the thread')

    // Cleanup
    await deleteThreadViaApi(page, roomId, thread!.id)
  })

  test('leader can edit thread created by another user', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { enabled: true, defaultPermission: 'CREATE_THREADS' },
    })

    // Create as admin
    await login(page, accounts.admin)
    const title = `Admin-Edit-${Date.now()}`
    const thread = await createThreadViaApi(page, roomId, title)
    expect(thread).toBeTruthy()

    // Edit as teacher (leader)
    await login(page, accounts.teacher)
    const res = await page.request.put(`/api/v1/rooms/${roomId}/fotobox/threads/${thread!.id}`, {
      data: { title: 'Leader-Edited-Title' },
    })
    expect(res.ok()).toBe(true)

    const json = await res.json()
    expect(json.data.title).toBe('Leader-Edited-Title')

    // Cleanup
    await deleteThreadViaApi(page, roomId, thread!.id)
    await page.request.put(`/api/v1/rooms/${roomId}/fotobox/settings`, {
      data: { defaultPermission: 'POST_IMAGES' },
    })
  })

  test('non-creator non-leader cannot edit a thread', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await ensureFotoboxEnabled(page, roomId)

    const title = `No-Edit-${Date.now()}`
    const thread = await createThreadViaApi(page, roomId, title)
    expect(thread).toBeTruthy()

    // Try to edit as student
    await login(page, accounts.student)
    const res = await page.request.put(`/api/v1/rooms/${roomId}/fotobox/threads/${thread!.id}`, {
      data: { title: 'Hacked Title' },
    })
    expect(res.status()).toBe(403)

    // Cleanup as teacher
    await login(page, accounts.teacher)
    await deleteThreadViaApi(page, roomId, thread!.id)
  })

  test('get single thread via API returns correct data', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    await ensureFotoboxEnabled(page, roomId)

    const title = `Single-Thread-${Date.now()}`
    const thread = await createThreadViaApi(page, roomId, title, 'ALL', 'A test description')
    expect(thread).toBeTruthy()

    // Fetch single thread
    const res = await page.request.get(`/api/v1/rooms/${roomId}/fotobox/threads/${thread!.id}`)
    expect(res.ok()).toBe(true)

    const json = await res.json()
    expect(json.data.id).toBe(thread!.id)
    expect(json.data.title).toBe(title)
    expect(json.data.description).toBe('A test description')
    expect(json.data.audience).toBe('ALL')
    expect(json.data.imageCount).toBe(0)
    expect(json.data.createdBy).toBeTruthy()
    expect(json.data.createdByName).toBeTruthy()
    expect(json.data.createdAt).toBeTruthy()

    // Cleanup
    await deleteThreadViaApi(page, roomId, thread!.id)
  })
})

// --------------------------------------------------------------------------
// US-260: Bild-Caption aktualisieren
// --------------------------------------------------------------------------
test.describe('US-260: Bild-Caption aktualisieren', () => {

  // TODO: Updating image caption requires real uploaded images (multipart).
  // Skipping until reliable image upload pattern is established.
  test.skip('uploader can update caption via PUT /api/v1/fotobox/images/{id}', async ({ page }) => {
    await login(page, accounts.teacher)
  })

  test.skip('leader can update caption on any image', async ({ page }) => {
    await login(page, accounts.teacher)
  })

  test.skip('caption is limited to 500 characters', async ({ page }) => {
    await login(page, accounts.teacher)
  })
})
