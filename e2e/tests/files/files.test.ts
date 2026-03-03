import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { login } from '../../helpers/auth'
import { selectors, toastWithText } from '../../helpers/selectors'

// ============================================================================
// Files E2E Tests — US-140 to US-151
// ============================================================================

/**
 * Helper: navigate to the first room and click the "Dateien" tab.
 * Returns the room ID extracted from the URL, or null if no rooms/tab found.
 */
async function navigateToFilesTab(page: import('@playwright/test').Page): Promise<string | null> {
  await page.goto('/rooms')
  await page.waitForLoadState('networkidle')

  const roomCard = page.locator('.room-card').first()
  const hasRoom = await roomCard.isVisible({ timeout: 10000 }).catch(() => false)
  if (!hasRoom) return null

  await roomCard.click()
  await page.waitForURL(/\/rooms\//, { timeout: 10000 })
  await page.waitForLoadState('networkidle')

  // Extract room ID from URL
  const url = page.url()
  const match = url.match(/\/rooms\/([a-f0-9-]+)/)
  const roomId = match?.[1] ?? null

  // Click the "Dateien" tab
  const filesTab = page.locator('.p-tablist').locator('text=Dateien')
  const hasFilesTab = await filesTab.isVisible({ timeout: 5000 }).catch(() => false)
  if (!hasFilesTab) return null

  await filesTab.click()
  await page.waitForLoadState('networkidle')

  return roomId
}

/**
 * Helper: create a folder via API and return the folder info.
 */
async function createFolderViaApi(
  page: import('@playwright/test').Page,
  roomId: string,
  name: string,
  audience?: string,
  parentId?: string
): Promise<{ id: string; name: string } | null> {
  try {
    const res = await page.request.post(`/api/v1/rooms/${roomId}/files/folders`, {
      data: {
        name,
        parentId: parentId ?? null,
        audience: audience ?? null,
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
 * Helper: delete a folder via API (cleanup).
 */
async function deleteFolderViaApi(
  page: import('@playwright/test').Page,
  roomId: string,
  folderId: string
): Promise<void> {
  try {
    await page.request.delete(`/api/v1/rooms/${roomId}/files/folders/${folderId}`)
  } catch {
    // Ignore cleanup failures
  }
}

/**
 * Helper: list folders via API.
 */
async function listFoldersViaApi(
  page: import('@playwright/test').Page,
  roomId: string,
  parentId?: string
): Promise<{ id: string; name: string; audience?: string }[]> {
  try {
    const url = parentId
      ? `/api/v1/rooms/${roomId}/files/folders?parentId=${parentId}`
      : `/api/v1/rooms/${roomId}/files/folders`
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
 * Helper: list files via API.
 */
async function listFilesViaApi(
  page: import('@playwright/test').Page,
  roomId: string,
  folderId?: string
): Promise<{ id: string; originalName: string }[]> {
  try {
    const url = folderId
      ? `/api/v1/rooms/${roomId}/files?folderId=${folderId}`
      : `/api/v1/rooms/${roomId}/files`
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
 * Helper: get rooms the user is a member of, return the first room ID.
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
 * Uses the browse endpoint to get all rooms, then finds one not in /mine.
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

// --------------------------------------------------------------------------
// US-140: Upload file in room
// --------------------------------------------------------------------------
test.describe('US-140: Datei hochladen', () => {

  // TODO: File upload via Playwright requires inputFile() interaction with
  // PrimeVue FileUpload component, which uses a hidden <input type="file">.
  // Skipping until reliable file upload pattern is established.
  test.skip('teacher can upload a file via "Hochladen" button', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await navigateToFilesTab(page)
    expect(roomId).toBeTruthy()

    // The "Hochladen" button should be visible in the file-actions area
    const uploadBtn = page.locator('.file-actions').locator('text=Hochladen')
    await expect(uploadBtn).toBeVisible({ timeout: 5000 })
  })

  test('teacher sees "Hochladen" button in Files tab', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await navigateToFilesTab(page)
    if (!roomId) {
      test.skip()
      return
    }

    // The upload button should be present in the file-actions area
    const uploadBtn = page.locator('.file-actions .upload-btn, .file-actions button:has-text("Hochladen"), .file-actions .p-fileupload')
    await expect(uploadBtn.first()).toBeVisible({ timeout: 5000 })
  })
})

// --------------------------------------------------------------------------
// US-141: Download file
// --------------------------------------------------------------------------
test.describe('US-141: Datei herunterladen', () => {

  test('file download endpoint returns 200 for existing file via API', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    // List existing files in the room
    const files = await listFilesViaApi(page, roomId)
    if (files.length === 0) {
      // No files uploaded yet — skip gracefully
      test.skip()
      return
    }

    // Download via API — should return 200
    const res = await page.request.get(`/api/v1/rooms/${roomId}/files/${files[0].id}`)
    expect(res.status()).toBe(200)

    // Response should have a content-type header
    const contentType = res.headers()['content-type']
    expect(contentType).toBeTruthy()
  })

  test('files are displayed as clickable items in the list', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await navigateToFilesTab(page)
    if (!roomId) {
      test.skip()
      return
    }

    // Check if there are any file items (may be empty)
    const fileItems = page.locator('.file-item:not(.folder)')
    const count = await fileItems.count()

    if (count > 0) {
      // File items should have a file-name span
      const firstFile = fileItems.first()
      await expect(firstFile.locator('.file-name')).toBeVisible()
      // File items should be clickable (tabindex="0" or cursor:pointer)
      await expect(firstFile).toBeVisible()
    } else {
      // Empty state is acceptable
      const emptyState = page.locator('.empty-state, text=Noch keine Dateien vorhanden')
      await expect(emptyState).toBeVisible({ timeout: 5000 })
    }
  })
})

// --------------------------------------------------------------------------
// US-142: Create/manage folders
// --------------------------------------------------------------------------
test.describe('US-142: Ordner erstellen und verwalten', () => {

  test('teacher can open "Neuer Ordner" dialog', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await navigateToFilesTab(page)
    if (!roomId) {
      test.skip()
      return
    }

    // Click "Neuer Ordner" button
    const newFolderBtn = page.locator('button:has-text("Neuer Ordner")')
    await expect(newFolderBtn).toBeVisible({ timeout: 5000 })
    await newFolderBtn.click()

    // Dialog should appear
    const dialog = page.locator(selectors.dialog)
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await expect(dialog.locator('.p-dialog-title')).toContainText('Neuer Ordner')

    // Dialog should have folder name input and Sichtbarkeit dropdown (for leaders)
    const folderNameInput = dialog.locator('.folder-input').first()
    await expect(folderNameInput).toBeVisible()
  })

  test('teacher can create a folder via dialog', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await navigateToFilesTab(page)
    if (!roomId) {
      test.skip()
      return
    }

    const folderName = `E2E-Test-Ordner-${Date.now()}`

    // Click "Neuer Ordner"
    const newFolderBtn = page.locator('button:has-text("Neuer Ordner")')
    await newFolderBtn.click()

    const dialog = page.locator(selectors.dialog)
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Fill folder name
    const folderNameInput = dialog.locator('.p-inputtext').first()
    await folderNameInput.fill(folderName)

    // Click "Erstellen" button
    const createBtn = dialog.locator('button:has-text("Erstellen")')
    await createBtn.click()

    // Dialog should close
    await expect(dialog).not.toBeVisible({ timeout: 5000 })

    // New folder should appear in the list
    const folderItem = page.locator('.file-item.folder').locator(`text=${folderName}`)
    await expect(folderItem).toBeVisible({ timeout: 10000 })

    // Cleanup: delete the folder via API
    const folders = await listFoldersViaApi(page, roomId)
    const created = folders.find(f => f.name === folderName)
    if (created) {
      await deleteFolderViaApi(page, roomId, created.id)
    }
  })

  test('teacher can navigate into a folder via breadcrumb', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await navigateToFilesTab(page)
    if (!roomId) {
      test.skip()
      return
    }

    const folderName = `E2E-Nav-Ordner-${Date.now()}`

    // Create folder via API
    const folder = await createFolderViaApi(page, roomId, folderName)
    if (!folder) {
      test.skip()
      return
    }

    // Reload files tab
    const filesTab = page.locator('.p-tablist').locator('text=Dateien')
    await filesTab.click()
    await page.waitForLoadState('networkidle')

    // Click on the folder to navigate into it
    const folderItem = page.locator('.file-item.folder').filter({ hasText: folderName })
    await expect(folderItem).toBeVisible({ timeout: 10000 })
    await folderItem.click()
    await page.waitForLoadState('networkidle')

    // Breadcrumb should now show: Dateien > folderName
    const breadcrumb = page.locator('.breadcrumb')
    await expect(breadcrumb).toBeVisible({ timeout: 5000 })
    await expect(breadcrumb).toContainText('Dateien')
    await expect(breadcrumb).toContainText(folderName)

    // Navigate back by clicking "Dateien" in breadcrumb
    const rootCrumb = breadcrumb.locator('.breadcrumb-item').first()
    await rootCrumb.click()
    await page.waitForLoadState('networkidle')

    // Should be back at root — folder visible again
    await expect(page.locator('.file-item.folder').filter({ hasText: folderName })).toBeVisible({ timeout: 5000 })

    // Cleanup
    await deleteFolderViaApi(page, roomId, folder.id)
  })

  test('folder creation via API returns folder with correct name', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const folderName = `API-Ordner-${Date.now()}`
    const folder = await createFolderViaApi(page, roomId, folderName)
    expect(folder).toBeTruthy()
    expect(folder!.name).toBe(folderName)

    // Cleanup
    await deleteFolderViaApi(page, roomId, folder!.id)
  })
})

// --------------------------------------------------------------------------
// US-143: Folder audience Nur Eltern
// --------------------------------------------------------------------------
test.describe('US-143: Ordner-Sichtbarkeit Nur Eltern', () => {

  test('parent can see a PARENTS_ONLY folder', async ({ page }) => {
    // First, create the folder as teacher (who is LEADER)
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const folderName = `Nur-Eltern-${Date.now()}`
    const folder = await createFolderViaApi(page, roomId, folderName, 'PARENTS_ONLY')
    if (!folder) {
      test.skip()
      return
    }

    // Now login as parent and check folders via API
    await login(page, accounts.parent)
    const parentFolders = await listFoldersViaApi(page, roomId)
    const found = parentFolders.find(f => f.name === folderName)
    expect(found).toBeTruthy()

    // Cleanup as teacher
    await login(page, accounts.teacher)
    await deleteFolderViaApi(page, roomId, folder.id)
  })

  test('student cannot see a PARENTS_ONLY folder', async ({ page }) => {
    // Create the folder as teacher
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const folderName = `Nur-Eltern-Hidden-${Date.now()}`
    const folder = await createFolderViaApi(page, roomId, folderName, 'PARENTS_ONLY')
    if (!folder) {
      test.skip()
      return
    }

    // Login as student and check folders via API
    await login(page, accounts.student)
    const studentFolders = await listFoldersViaApi(page, roomId)
    const found = studentFolders.find(f => f.name === folderName)
    expect(found).toBeFalsy()

    // Cleanup as teacher
    await login(page, accounts.teacher)
    await deleteFolderViaApi(page, roomId, folder.id)
  })

  test('PARENTS_ONLY folder shows "Nur Eltern" tag in UI for parent', async ({ page }) => {
    // Create folder as teacher
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const folderName = `Eltern-Tag-${Date.now()}`
    const folder = await createFolderViaApi(page, roomId, folderName, 'PARENTS_ONLY')
    if (!folder) {
      test.skip()
      return
    }

    // Login as parent and navigate to files tab
    await login(page, accounts.parent)
    const parentRoomId = await navigateToFilesTab(page)
    if (!parentRoomId) {
      await login(page, accounts.teacher)
      await deleteFolderViaApi(page, roomId, folder.id)
      test.skip()
      return
    }

    // The folder should show a "Nur Eltern" tag
    const folderItem = page.locator('.file-item.folder').filter({ hasText: folderName })
    const hasFolder = await folderItem.isVisible({ timeout: 5000 }).catch(() => false)
    if (hasFolder) {
      const tag = folderItem.locator('.p-tag')
      await expect(tag).toBeVisible({ timeout: 3000 })
      await expect(tag).toContainText('Nur Eltern')
    }

    // Cleanup as teacher
    await login(page, accounts.teacher)
    await deleteFolderViaApi(page, roomId, folder.id)
  })
})

// --------------------------------------------------------------------------
// US-144: Folder audience Nur Schueler
// --------------------------------------------------------------------------
test.describe('US-144: Ordner-Sichtbarkeit Nur Schueler', () => {

  test('student can see a STUDENTS_ONLY folder', async ({ page }) => {
    // Create folder as teacher
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const folderName = `Nur-Schueler-${Date.now()}`
    const folder = await createFolderViaApi(page, roomId, folderName, 'STUDENTS_ONLY')
    if (!folder) {
      test.skip()
      return
    }

    // Login as student and check via API
    await login(page, accounts.student)
    const studentFolders = await listFoldersViaApi(page, roomId)
    const found = studentFolders.find(f => f.name === folderName)
    expect(found).toBeTruthy()

    // Cleanup as teacher
    await login(page, accounts.teacher)
    await deleteFolderViaApi(page, roomId, folder.id)
  })

  test('parent cannot see a STUDENTS_ONLY folder', async ({ page }) => {
    // Create folder as teacher
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const folderName = `Nur-Schueler-Hidden-${Date.now()}`
    const folder = await createFolderViaApi(page, roomId, folderName, 'STUDENTS_ONLY')
    if (!folder) {
      test.skip()
      return
    }

    // Login as parent and check via API
    await login(page, accounts.parent)
    const parentFolders = await listFoldersViaApi(page, roomId)
    const found = parentFolders.find(f => f.name === folderName)
    expect(found).toBeFalsy()

    // Cleanup as teacher
    await login(page, accounts.teacher)
    await deleteFolderViaApi(page, roomId, folder.id)
  })

  test('STUDENTS_ONLY folder shows "Nur Schueler" tag in UI for student', async ({ page }) => {
    // Create folder as teacher
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const folderName = `Schueler-Tag-${Date.now()}`
    const folder = await createFolderViaApi(page, roomId, folderName, 'STUDENTS_ONLY')
    if (!folder) {
      test.skip()
      return
    }

    // Login as student and navigate to files tab
    await login(page, accounts.student)
    const studentRoomId = await navigateToFilesTab(page)
    if (!studentRoomId) {
      await login(page, accounts.teacher)
      await deleteFolderViaApi(page, roomId, folder.id)
      test.skip()
      return
    }

    // The folder should show a "Nur Schueler" tag
    const folderItem = page.locator('.file-item.folder').filter({ hasText: folderName })
    const hasFolder = await folderItem.isVisible({ timeout: 5000 }).catch(() => false)
    if (hasFolder) {
      const tag = folderItem.locator('.p-tag')
      await expect(tag).toBeVisible({ timeout: 3000 })
      // The i18n key files.audienceStudents resolves to "Nur Schüler"
      await expect(tag).toContainText('Nur Sch')
    }

    // Cleanup as teacher
    await login(page, accounts.teacher)
    await deleteFolderViaApi(page, roomId, folder.id)
  })
})

// --------------------------------------------------------------------------
// US-145: Delete file
// --------------------------------------------------------------------------
test.describe('US-145: Datei loeschen', () => {

  // TODO: Deleting a file requires a file to exist. File upload via Playwright
  // is complex with PrimeVue FileUpload. Skip until upload is available.
  test.skip('teacher can delete a file via delete button', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await navigateToFilesTab(page)
    expect(roomId).toBeTruthy()

    // Find a file item with a delete button
    const fileItem = page.locator('.file-item:not(.folder)').first()
    await expect(fileItem).toBeVisible({ timeout: 5000 })

    const deleteBtn = fileItem.locator('button[aria-label="Löschen"], button .pi-trash')
    await deleteBtn.click()
  })

  test('delete file API returns 200 for valid file', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    // List existing files
    const files = await listFilesViaApi(page, roomId)
    if (files.length === 0) {
      // No files to delete — skip gracefully
      test.skip()
      return
    }

    // Delete via API
    const res = await page.request.delete(`/api/v1/rooms/${roomId}/files/${files[0].id}`)
    expect(res.status()).toBe(200)
  })
})

// --------------------------------------------------------------------------
// US-146: Delete folder
// --------------------------------------------------------------------------
test.describe('US-146: Ordner loeschen', () => {

  test('teacher can delete a folder via trash button in UI', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await navigateToFilesTab(page)
    if (!roomId) {
      test.skip()
      return
    }

    const folderName = `Delete-Me-${Date.now()}`

    // Create folder via API
    const folder = await createFolderViaApi(page, roomId, folderName)
    if (!folder) {
      test.skip()
      return
    }

    // Reload files tab to see the new folder
    const filesTab = page.locator('.p-tablist').locator('text=Dateien')
    await filesTab.click()
    await page.waitForLoadState('networkidle')

    // Find the folder item
    const folderItem = page.locator('.file-item.folder').filter({ hasText: folderName })
    await expect(folderItem).toBeVisible({ timeout: 10000 })

    // Click the trash button on the folder (uses @click.stop)
    const deleteBtn = folderItem.locator('button').filter({ has: page.locator('.pi-trash') })
    await deleteBtn.click()

    // Folder should disappear from the list
    await expect(folderItem).not.toBeVisible({ timeout: 10000 })
  })

  test('delete folder via API returns 200', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await getFirstRoomId(page)
    if (!roomId) {
      test.skip()
      return
    }

    const folderName = `API-Delete-${Date.now()}`
    const folder = await createFolderViaApi(page, roomId, folderName)
    if (!folder) {
      test.skip()
      return
    }

    const res = await page.request.delete(`/api/v1/rooms/${roomId}/files/folders/${folder.id}`)
    expect(res.status()).toBe(200)

    // Verify it is gone
    const folders = await listFoldersViaApi(page, roomId)
    const stillExists = folders.find(f => f.id === folder.id)
    expect(stillExists).toBeFalsy()
  })
})

// --------------------------------------------------------------------------
// US-147: WOPI/ONLYOFFICE "Online bearbeiten"
// --------------------------------------------------------------------------
test.describe('US-147: WOPI/ONLYOFFICE Online bearbeiten', () => {

  // TODO: Requires ONLYOFFICE Document Server running as a separate container.
  // The "Online bearbeiten" button only appears when WOPI is enabled AND the
  // file is an Office-type file (.docx, .xlsx, .pptx, etc.).
  test.skip('office file shows "Online bearbeiten" button when WOPI enabled', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await navigateToFilesTab(page)
    expect(roomId).toBeTruthy()

    // Would need an Office file uploaded and WOPI module enabled
    const editBtn = page.locator('button:has-text("Online bearbeiten")')
    await expect(editBtn).toBeVisible({ timeout: 5000 })
  })
})

// --------------------------------------------------------------------------
// US-148: WOPI disabled — no "Online bearbeiten"
// --------------------------------------------------------------------------
test.describe('US-148: WOPI deaktiviert', () => {

  test('no "Online bearbeiten" button visible when WOPI is off', async ({ page }) => {
    await login(page, accounts.teacher)
    const roomId = await navigateToFilesTab(page)
    if (!roomId) {
      test.skip()
      return
    }

    // Wait for files to load
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)

    // "Online bearbeiten" should NOT be visible (WOPI is not enabled by default)
    const editBtn = page.locator('button:has-text("Online bearbeiten")')
    await expect(editBtn).not.toBeVisible({ timeout: 3000 })
  })

  test('WOPI module is disabled by default in config', async ({ page }) => {
    await login(page, accounts.admin)

    // Check admin config via API
    const res = await page.request.get('/api/v1/admin/config')
    if (res.ok()) {
      const json = await res.json()
      const modules = json.data?.modules ?? {}
      // WOPI should be disabled by default (DB-managed toggle in modules JSONB)
      const wopiEnabled = modules.wopi ?? false
      expect(wopiEnabled).toBe(false)
    }
  })
})

// --------------------------------------------------------------------------
// US-149: ClamAV virus scan
// --------------------------------------------------------------------------
test.describe('US-149: ClamAV Virenscanner', () => {

  // TODO: Requires ClamAV server running (clamav container).
  // In production, uploads are scanned via INSTREAM protocol.
  test.skip('file upload triggers ClamAV scan when enabled', async ({ page }) => {
    await login(page, accounts.teacher)
    // Would need ClamAV container running and module enabled
  })
})

// --------------------------------------------------------------------------
// US-150: Files module disabled
// --------------------------------------------------------------------------
test.describe('US-150: Dateien-Modul deaktiviert', () => {

  // TODO: Disabling the files module globally would affect other tests.
  // This should be tested in isolation or with a dedicated test environment.
  test.skip('no "Dateien" tab when files module is disabled', async ({ page }) => {
    await login(page, accounts.teacher)
    // Would need to disable files module in admin config, then verify
    // the "Dateien" tab is not shown in room detail
  })

  test('files module is enabled by default', async ({ page }) => {
    await login(page, accounts.admin)

    const res = await page.request.get('/api/v1/admin/config')
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const modules = json.data?.modules ?? {}
    // Files module should be enabled by default
    expect(modules.files).toBe(true)
  })
})

// --------------------------------------------------------------------------
// US-151: Access restriction for non-members
// --------------------------------------------------------------------------
test.describe('US-151: Zugriffsbeschraenkung fuer Nicht-Mitglieder', () => {

  test('API returns 403 when non-member tries to list files', async ({ page }) => {
    // Login as student who is only in Sonnengruppe
    await login(page, accounts.student)

    // Find a room the student is NOT a member of
    const nonMemberRoomId = await findNonMemberRoomId(page)
    if (!nonMemberRoomId) {
      // All rooms visible or browse not available — try a fabricated UUID
      const fakeRoomId = '00000000-0000-0000-0000-000000000099'
      const res = await page.request.get(`/api/v1/rooms/${fakeRoomId}/files`)
      // Should be 403 or 404
      expect([403, 404]).toContain(res.status())
      return
    }

    // Try to list files in that room
    const res = await page.request.get(`/api/v1/rooms/${nonMemberRoomId}/files`)
    expect([401, 403]).toContain(res.status())
  })

  test('API returns 403 when non-member tries to list folders', async ({ page }) => {
    await login(page, accounts.student)

    const nonMemberRoomId = await findNonMemberRoomId(page)
    if (!nonMemberRoomId) {
      const fakeRoomId = '00000000-0000-0000-0000-000000000099'
      const res = await page.request.get(`/api/v1/rooms/${fakeRoomId}/files/folders`)
      expect([403, 404]).toContain(res.status())
      return
    }

    const res = await page.request.get(`/api/v1/rooms/${nonMemberRoomId}/files/folders`)
    expect([401, 403]).toContain(res.status())
  })

  test('API returns 403 when non-member tries to create a folder', async ({ page }) => {
    await login(page, accounts.student)

    const nonMemberRoomId = await findNonMemberRoomId(page)
    if (!nonMemberRoomId) {
      const fakeRoomId = '00000000-0000-0000-0000-000000000099'
      const res = await page.request.post(`/api/v1/rooms/${fakeRoomId}/files/folders`, {
        data: { name: 'Unauthorized', parentId: null, audience: null },
      })
      expect([403, 404]).toContain(res.status())
      return
    }

    const res = await page.request.post(`/api/v1/rooms/${nonMemberRoomId}/files/folders`, {
      data: { name: 'Unauthorized Folder', parentId: null, audience: null },
    })
    expect([401, 403]).toContain(res.status())
  })
})
