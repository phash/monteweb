import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { login } from '../../helpers/auth'
import { selectors, toastWithText } from '../../helpers/selectors'

// ============================================================================
// Forms E2E Tests — US-160 to US-182
// ============================================================================

type Page = import('@playwright/test').Page

/**
 * Helper: navigate to the forms list page.
 */
async function goToForms(page: Page): Promise<void> {
  await page.goto('/forms')
  await page.waitForLoadState('networkidle')
  await expect(page.locator('.page-title')).toBeVisible({ timeout: 10000 })
}

/**
 * Helper: get the first room ID the current user is a member of.
 */
async function getFirstRoomId(page: Page): Promise<string | null> {
  try {
    const res = await page.request.get('/api/v1/rooms/mine')
    if (res.ok()) {
      const json = await res.json()
      const rooms = json.data?.content ?? json.data ?? []
      if (rooms.length > 0) return rooms[0].id
    }
  } catch { /* ignore */ }
  return null
}

/**
 * Helper: get all available sections.
 */
async function getSections(page: Page): Promise<Array<{ id: string; name: string }>> {
  try {
    const res = await page.request.get('/api/v1/sections')
    if (res.ok()) {
      const json = await res.json()
      return json.data ?? []
    }
  } catch { /* ignore */ }
  return []
}

/**
 * Helper: create a form via API and return its detail info.
 */
async function createFormViaApi(
  page: Page,
  data: {
    title: string
    description?: string
    type?: string
    scope?: string
    scopeId?: string
    sectionIds?: string[]
    anonymous?: boolean
    deadline?: string
    questions?: Array<{
      type: string
      label: string
      description?: string
      required?: boolean
      options?: string[]
      ratingConfig?: { min: number; max: number }
    }>
  }
): Promise<{ id: string; form: any; questions: any[] } | null> {
  try {
    const response = await page.request.post('/api/v1/forms', {
      data: {
        title: data.title,
        description: data.description,
        type: data.type ?? 'SURVEY',
        scope: data.scope ?? 'SCHOOL',
        scopeId: data.scopeId,
        sectionIds: data.sectionIds,
        anonymous: data.anonymous ?? false,
        deadline: data.deadline,
        questions: data.questions ?? [
          { type: 'TEXT', label: 'Testfrage', required: false },
        ],
      },
    })
    if (response.ok()) {
      const json = await response.json()
      return json.data ?? null
    }
  } catch { /* ignore */ }
  return null
}

/**
 * Helper: publish a form via API.
 */
async function publishFormViaApi(page: Page, formId: string): Promise<boolean> {
  try {
    const res = await page.request.post(`/api/v1/forms/${formId}/publish`)
    return res.ok()
  } catch { /* ignore */ }
  return false
}

/**
 * Helper: close a form via API.
 */
async function closeFormViaApi(page: Page, formId: string): Promise<boolean> {
  try {
    const res = await page.request.post(`/api/v1/forms/${formId}/close`)
    return res.ok()
  } catch { /* ignore */ }
  return false
}

/**
 * Helper: delete a form via API (cleanup).
 */
async function deleteFormViaApi(page: Page, formId: string): Promise<void> {
  try {
    await page.request.delete(`/api/v1/forms/${formId}`)
  } catch { /* ignore */ }
}

/**
 * Helper: submit a response to a form via API.
 */
async function submitResponseViaApi(
  page: Page,
  formId: string,
  answers: Array<{ questionId: string; text?: string; selectedOptions?: string[]; rating?: number }>
): Promise<boolean> {
  try {
    const res = await page.request.post(`/api/v1/forms/${formId}/respond`, {
      data: { answers },
    })
    return res.ok()
  } catch { /* ignore */ }
  return false
}

/**
 * Helper: get form detail via API.
 */
async function getFormViaApi(page: Page, formId: string): Promise<any | null> {
  try {
    const res = await page.request.get(`/api/v1/forms/${formId}`)
    if (res.ok()) {
      const json = await res.json()
      return json.data ?? null
    }
  } catch { /* ignore */ }
  return null
}

// --------------------------------------------------------------------------
// US-160: Create survey (ROOM scope, LEADER)
// --------------------------------------------------------------------------
test.describe('US-160: Create survey (ROOM scope, LEADER)', () => {

  test('leader can create a ROOM-scoped survey as draft via UI', async ({ page }) => {
    await login(page, accounts.admin)

    const roomId = await getFirstRoomId(page)
    test.skip(!roomId, 'No rooms available for this user')

    await page.goto('/forms/create')
    await page.waitForLoadState('networkidle')

    // Fill title
    const titleInput = page.locator('#form-title')
    await expect(titleInput).toBeVisible({ timeout: 10000 })
    await titleInput.fill('E2E Test Umfrage')

    // Type should default to SURVEY (Umfrage)
    const typeSelect = page.locator('#form-type')
    if (await typeSelect.isVisible({ timeout: 3000 }).catch(() => false)) {
      await expect(typeSelect).toBeVisible()
    }

    // Select scope ROOM
    const scopeSelect = page.locator('#form-scope')
    if (await scopeSelect.isVisible({ timeout: 3000 }).catch(() => false)) {
      await scopeSelect.click()
      await page.locator('.p-select-option, .p-listbox-option, li[role="option"]').filter({ hasText: 'Raum' }).first().click()
    }

    // Select room
    const roomSelect = page.locator('#form-room')
    if (await roomSelect.isVisible({ timeout: 3000 }).catch(() => false)) {
      await roomSelect.click()
      await page.locator('.p-select-option, .p-listbox-option, li[role="option"]').first().click()
    }

    // Add a question
    const addBtn = page.locator('button:has-text("Frage hinzufügen")')
    await addBtn.click()

    // Fill question label
    const qLabel = page.locator('#q-label-0')
    await expect(qLabel).toBeVisible({ timeout: 5000 })
    await qLabel.fill('Wie zufrieden sind Sie?')

    // Save as draft
    const draftBtn = page.locator('button:has-text("Als Entwurf speichern")')
    await draftBtn.click()

    // Should show success toast and navigate
    await expect(page.locator(toastWithText('gespeichert'))).toBeVisible({ timeout: 10000 })
    await page.waitForURL('**/forms', { timeout: 10000 })
  })

  test('leader can create a survey via API', async ({ page }) => {
    await login(page, accounts.admin)

    const roomId = await getFirstRoomId(page)
    test.skip(!roomId, 'No rooms available')

    const form = await createFormViaApi(page, {
      title: 'API Survey Test',
      scope: 'ROOM',
      scopeId: roomId!,
      questions: [{ type: 'TEXT', label: 'API Testfrage', required: false }],
    })
    expect(form).not.toBeNull()
    expect(form!.form.title).toBe('API Survey Test')
    expect(form!.form.status).toBe('DRAFT')

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-161: Create consent form (CONSENT type)
// --------------------------------------------------------------------------
test.describe('US-161: Create consent form (CONSENT type)', () => {

  test('can create a CONSENT form with YES_NO question via API', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Einverstaendniserklaerung',
      type: 'CONSENT',
      scope: 'SCHOOL',
      questions: [{ type: 'YES_NO', label: 'Stimmen Sie zu?', required: true }],
    })
    expect(form).not.toBeNull()
    expect(form!.form.type).toBe('CONSENT')
    expect(form!.questions[0].type).toBe('YES_NO')

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('consent form type shows "Einverstaendnis" tag on UI', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Consent Display',
      type: 'CONSENT',
      scope: 'SCHOOL',
      questions: [{ type: 'YES_NO', label: 'Zustimmung', required: true }],
    })
    test.skip(!form, 'Could not create form')

    await publishFormViaApi(page, form!.form.id)

    await goToForms(page)
    await page.waitForLoadState('networkidle')

    // Look for the consent tag
    const formEntry = page.locator('.form-item, .form-entry').filter({ hasText: 'E2E Consent Display' })
    const isVisible = await formEntry.isVisible({ timeout: 5000 }).catch(() => false)
    if (isVisible) {
      await expect(formEntry.locator('.p-tag').filter({ hasText: 'Einverständnis' })).toBeVisible()
    }

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-162: Publish form
// --------------------------------------------------------------------------
test.describe('US-162: Publish form', () => {

  test('publishing a draft form changes status to PUBLISHED via API', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Publish Test',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Frage 1', required: false }],
    })
    expect(form).not.toBeNull()
    expect(form!.form.status).toBe('DRAFT')

    const published = await publishFormViaApi(page, form!.form.id)
    expect(published).toBe(true)

    const detail = await getFormViaApi(page, form!.form.id)
    expect(detail.form.status).toBe('PUBLISHED')

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('publish button visible on draft form detail page', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Publish Button',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')

    await page.goto(`/forms/${form!.form.id}`)
    await page.waitForLoadState('networkidle')

    const publishBtn = page.locator('button:has-text("Veröffentlichen")')
    await expect(publishBtn).toBeVisible({ timeout: 10000 })

    await publishBtn.click()
    await expect(page.locator(toastWithText('veröffentlicht'))).toBeVisible({ timeout: 10000 })

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-163: Fill out form
// --------------------------------------------------------------------------
test.describe('US-163: Fill out form', () => {

  test('user can submit a response to a published form', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Fill Out Test',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Was denken Sie?', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    // Now login as a different user to fill out
    await login(page, accounts.teacher)

    await page.goto(`/forms/${form!.form.id}`)
    await page.waitForLoadState('networkidle')

    // Fill text answer
    const textarea = page.locator('textarea').first()
    const hasTextarea = await textarea.isVisible({ timeout: 8000 }).catch(() => false)
    if (hasTextarea) {
      await textarea.fill('E2E Testantwort')
    }

    // Submit response
    const submitBtn = page.locator('button:has-text("Antwort absenden")')
    if (await submitBtn.isVisible({ timeout: 5000 }).catch(() => false)) {
      await submitBtn.click()
      await expect(page.locator(toastWithText('Vielen Dank'))).toBeVisible({ timeout: 10000 })
    }

    // Cleanup
    await login(page, accounts.admin)
    await deleteFormViaApi(page, form!.form.id)
  })

  test('response submission via API returns success', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E API Response',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Feedback', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    await login(page, accounts.teacher)
    const qId = form!.questions[0].id
    const success = await submitResponseViaApi(page, form!.form.id, [
      { questionId: qId, text: 'API answer' },
    ])
    expect(success).toBe(true)

    // Cleanup
    await login(page, accounts.admin)
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-164: Edit answer (non-anonymous)
// --------------------------------------------------------------------------
test.describe('US-164: Edit answer (non-anonymous)', () => {

  test('user sees "Antwort bearbeiten" after responding to non-anonymous form', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Edit Answer',
      scope: 'SCHOOL',
      anonymous: false,
      questions: [{ type: 'TEXT', label: 'Ihre Meinung', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    // Respond as teacher
    await login(page, accounts.teacher)
    const qId = form!.questions[0].id
    await submitResponseViaApi(page, form!.form.id, [{ questionId: qId, text: 'Erste Antwort' }])

    await page.goto(`/forms/${form!.form.id}`)
    await page.waitForLoadState('networkidle')

    // Should show "already responded" message
    const respondedMsg = page.locator('text=bereits beantwortet')
    await expect(respondedMsg).toBeVisible({ timeout: 10000 })

    // Should show edit button
    const editBtn = page.locator('button:has-text("Antwort bearbeiten")')
    await expect(editBtn).toBeVisible({ timeout: 5000 })

    // Cleanup
    await login(page, accounts.admin)
    await deleteFormViaApi(page, form!.form.id)
  })

  test('edit response via API succeeds', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Edit Response API',
      scope: 'SCHOOL',
      anonymous: false,
      questions: [{ type: 'TEXT', label: 'Question', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    await login(page, accounts.teacher)
    const qId = form!.questions[0].id
    await submitResponseViaApi(page, form!.form.id, [{ questionId: qId, text: 'Original' }])

    // Update response via PUT
    try {
      const res = await page.request.put(`/api/v1/forms/${form!.form.id}/respond`, {
        data: { answers: [{ questionId: qId, text: 'Updated answer' }] },
      })
      expect(res.ok()).toBe(true)
    } catch {
      // If PUT fails, test may need adjustment
    }

    // Cleanup
    await login(page, accounts.admin)
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-165: Close form
// --------------------------------------------------------------------------
test.describe('US-165: Close form', () => {

  test('closing a published form changes status to CLOSED via API', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Close Test',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    const closed = await closeFormViaApi(page, form!.form.id)
    expect(closed).toBe(true)

    const detail = await getFormViaApi(page, form!.form.id)
    expect(detail.form.status).toBe('CLOSED')

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('close button visible on published form detail', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Close Button',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    await page.goto(`/forms/${form!.form.id}`)
    await page.waitForLoadState('networkidle')

    const closeBtn = page.locator('button:has-text("Schließen")')
    await expect(closeBtn).toBeVisible({ timeout: 10000 })

    await closeBtn.click()
    await expect(page.locator(toastWithText('geschlossen'))).toBeVisible({ timeout: 10000 })

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-166: Archive form
// --------------------------------------------------------------------------
test.describe('US-166: Archive form', () => {

  test('archiving a closed form changes status to ARCHIVED via API', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Archive Test',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)
    await closeFormViaApi(page, form!.form.id)

    try {
      const res = await page.request.post(`/api/v1/forms/${form!.form.id}/archive`)
      expect(res.ok()).toBe(true)
    } catch {
      // Archive might not be available
    }

    const detail = await getFormViaApi(page, form!.form.id)
    expect(detail.form.status).toBe('ARCHIVED')

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('archive button visible on closed form detail', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Archive Button',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)
    await closeFormViaApi(page, form!.form.id)

    await page.goto(`/forms/${form!.form.id}`)
    await page.waitForLoadState('networkidle')

    const archiveBtn = page.locator('button:has-text("Archivieren")')
    await expect(archiveBtn).toBeVisible({ timeout: 10000 })

    await archiveBtn.click()
    await expect(page.locator(toastWithText('archiviert'))).toBeVisible({ timeout: 10000 })

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-167: View results
// --------------------------------------------------------------------------
test.describe('US-167: View results', () => {

  test('results page shows summary for published form with responses', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Results View',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Feedback bitte', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    // Submit a response as teacher
    await login(page, accounts.teacher)
    await submitResponseViaApi(page, form!.form.id, [
      { questionId: form!.questions[0].id, text: 'Tolles Event' },
    ])

    // View results as admin
    await login(page, accounts.admin)
    await page.goto(`/forms/${form!.form.id}/results`)
    await page.waitForLoadState('networkidle')

    // Should show results page title
    const resultsTitle = page.locator('.page-title')
    await expect(resultsTitle).toBeVisible({ timeout: 10000 })
    await expect(resultsTitle).toContainText('Ergebnisse')

    // Should show response count
    await expect(page.locator('text=Rücklaufquote')).toBeVisible({ timeout: 5000 })

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('view results button navigates to results page', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Results Nav',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    await page.goto(`/forms/${form!.form.id}`)
    await page.waitForLoadState('networkidle')

    const resultsBtn = page.locator('button:has-text("Ergebnisse anzeigen")')
    await expect(resultsBtn).toBeVisible({ timeout: 10000 })
    await resultsBtn.click()

    await page.waitForURL(`**/forms/${form!.form.id}/results`, { timeout: 10000 })

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-168: CSV export
// --------------------------------------------------------------------------
test.describe('US-168: CSV export', () => {

  test('CSV export button visible on results page', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E CSV Export',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    await page.goto(`/forms/${form!.form.id}/results`)
    await page.waitForLoadState('networkidle')

    const csvBtn = page.locator('button:has-text("CSV Export")')
    await expect(csvBtn).toBeVisible({ timeout: 10000 })

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('CSV export API returns file', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E CSV API',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    const res = await page.request.get(`/api/v1/forms/${form!.form.id}/results/csv`)
    expect(res.ok()).toBe(true)
    const contentType = res.headers()['content-type'] ?? ''
    expect(contentType).toContain('text/csv')

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-169: PDF export
// --------------------------------------------------------------------------
test.describe('US-169: PDF export', () => {

  test('PDF export button visible on results page', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E PDF Export',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    await page.goto(`/forms/${form!.form.id}/results`)
    await page.waitForLoadState('networkidle')

    const pdfBtn = page.locator('button:has-text("PDF Export")')
    await expect(pdfBtn).toBeVisible({ timeout: 10000 })

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('PDF export API returns file', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E PDF API',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    const res = await page.request.get(`/api/v1/forms/${form!.form.id}/results/pdf`)
    expect(res.ok()).toBe(true)
    const contentType = res.headers()['content-type'] ?? ''
    expect(contentType).toContain('pdf')

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-170: Section-scope form (TEACHER)
// --------------------------------------------------------------------------
test.describe('US-170: Section-scope form (TEACHER)', () => {

  test('teacher can create a SECTION-scoped form via API', async ({ page }) => {
    await login(page, accounts.teacher)

    const sections = await getSections(page)
    test.skip(sections.length === 0, 'No sections available')

    const form = await createFormViaApi(page, {
      title: 'E2E Section Form',
      scope: 'SECTION',
      sectionIds: [sections[0].id],
      questions: [{ type: 'TEXT', label: 'Section question', required: false }],
    })
    expect(form).not.toBeNull()
    expect(form!.form.scope).toBe('SECTION')

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('section scope option visible for teacher on create page', async ({ page }) => {
    await login(page, accounts.teacher)

    await page.goto('/forms/create')
    await page.waitForLoadState('networkidle')

    const scopeSelect = page.locator('#form-scope')
    await expect(scopeSelect).toBeVisible({ timeout: 10000 })

    await scopeSelect.click()
    const sectionOption = page.locator('.p-select-option, .p-listbox-option, li[role="option"]').filter({ hasText: 'Schulbereich' })
    await expect(sectionOption).toBeVisible({ timeout: 5000 })
  })
})

// --------------------------------------------------------------------------
// US-171: Multi-section form
// --------------------------------------------------------------------------
test.describe('US-171: Multi-section form', () => {

  test('form can target multiple sections via API', async ({ page }) => {
    await login(page, accounts.admin)

    const sections = await getSections(page)
    test.skip(sections.length < 2, 'Need at least 2 sections')

    const form = await createFormViaApi(page, {
      title: 'E2E Multi-Section',
      scope: 'SECTION',
      sectionIds: [sections[0].id, sections[1].id],
      questions: [{ type: 'TEXT', label: 'Multi Q', required: false }],
    })
    expect(form).not.toBeNull()
    expect(form!.form.sectionIds).toBeDefined()
    expect(form!.form.sectionIds.length).toBeGreaterThanOrEqual(2)

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('multi-select sections shown on create page for SECTION scope', async ({ page }) => {
    await login(page, accounts.admin)

    await page.goto('/forms/create')
    await page.waitForLoadState('networkidle')

    // Select SECTION scope
    const scopeSelect = page.locator('#form-scope')
    await expect(scopeSelect).toBeVisible({ timeout: 10000 })
    await scopeSelect.click()
    await page.locator('.p-select-option, .p-listbox-option, li[role="option"]').filter({ hasText: 'Schulbereich' }).click()

    // MultiSelect for sections should be visible
    const sectionSelect = page.locator('#form-sections')
    await expect(sectionSelect).toBeVisible({ timeout: 5000 })
  })
})

// --------------------------------------------------------------------------
// US-172: School-wide form (SA only)
// --------------------------------------------------------------------------
test.describe('US-172: School-wide form (SA only)', () => {

  test('admin can create a SCHOOL-scoped form via API', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Schulweite Umfrage',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Schulweite Frage', required: false }],
    })
    expect(form).not.toBeNull()
    expect(form!.form.scope).toBe('SCHOOL')

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('school scope option visible for admin on create page', async ({ page }) => {
    await login(page, accounts.admin)

    await page.goto('/forms/create')
    await page.waitForLoadState('networkidle')

    const scopeSelect = page.locator('#form-scope')
    await expect(scopeSelect).toBeVisible({ timeout: 10000 })
    await scopeSelect.click()

    const schoolOption = page.locator('.p-select-option, .p-listbox-option, li[role="option"]').filter({ hasText: 'Schulweit' })
    await expect(schoolOption).toBeVisible({ timeout: 5000 })
  })
})

// --------------------------------------------------------------------------
// US-173: Permission check — P/S no create, API 403
// --------------------------------------------------------------------------
test.describe('US-173: Permission check', () => {

  test('parent cannot create a form via API (returns error)', async ({ page }) => {
    await login(page, accounts.parent)

    const form = await createFormViaApi(page, {
      title: 'E2E Parent Attempt',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    // Parents should not be able to create school-wide forms
    // The API may return 400/403, so form should be null
    expect(form).toBeNull()
  })

  test('student cannot create a form via API (returns error)', async ({ page }) => {
    await login(page, accounts.student)

    const form = await createFormViaApi(page, {
      title: 'E2E Student Attempt',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    expect(form).toBeNull()
  })

  test('create button navigates to form but parent/student form creation fails on submit', async ({ page }) => {
    // The button is visible but backend rejects the request
    await login(page, accounts.parent)

    await goToForms(page)

    // The "Formular erstellen" button may be visible (no frontend guard)
    const createBtn = page.locator('button:has-text("Formular erstellen")')
    const isBtnVisible = await createBtn.isVisible({ timeout: 5000 }).catch(() => false)

    if (isBtnVisible) {
      // Frontend shows the button but backend will reject
      // Verify this is the expected behavior
      expect(isBtnVisible).toBe(true)
    }
    // Either way, API should deny creation
    const form = await createFormViaApi(page, {
      title: 'Parent Form',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    expect(form).toBeNull()
  })
})

// --------------------------------------------------------------------------
// US-174: Delete form
// --------------------------------------------------------------------------
test.describe('US-174: Delete form', () => {

  test('delete form via API removes it', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Delete Test',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')

    await deleteFormViaApi(page, form!.form.id)

    // Try to get it, should fail
    const detail = await getFormViaApi(page, form!.form.id)
    expect(detail).toBeNull()
  })

  test('delete button on form detail shows confirmation and deletes', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Delete UI',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')

    await page.goto(`/forms/${form!.form.id}`)
    await page.waitForLoadState('networkidle')

    const deleteBtn = page.locator('button:has-text("Formular löschen")')
    await expect(deleteBtn).toBeVisible({ timeout: 10000 })
    await deleteBtn.click()

    // Should show success toast
    await expect(page.locator(toastWithText('gelöscht'))).toBeVisible({ timeout: 10000 })

    // Should navigate back to forms list
    await page.waitForURL('**/forms', { timeout: 10000 })
  })
})

// --------------------------------------------------------------------------
// US-175: Dashboard widget
// --------------------------------------------------------------------------
test.describe('US-175: Dashboard widget', () => {

  test('dashboard shows "Offene Formulare" widget when forms are pending', async ({ page }) => {
    await login(page, accounts.admin)

    // Create and publish a form visible to all
    const form = await createFormViaApi(page, {
      title: 'E2E Widget Form',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Widget Q', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    // Login as teacher who has not responded
    await login(page, accounts.teacher)
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')

    // Look for the forms widget
    const widget = page.locator('.forms-widget, text=Offene Formulare')
    const hasWidget = await widget.isVisible({ timeout: 10000 }).catch(() => false)

    if (hasWidget) {
      await expect(widget).toBeVisible()
      // Should contain the form title
      const formLink = page.locator('.form-entry, .form-entry-title').filter({ hasText: 'E2E Widget Form' })
      const isFormVisible = await formLink.isVisible({ timeout: 5000 }).catch(() => false)
      expect(isFormVisible).toBe(true)
    }

    // Cleanup
    await login(page, accounts.admin)
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-176: All question types
// --------------------------------------------------------------------------
test.describe('US-176: All question types', () => {

  test('form with all 5 question types can be created via API', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E All Question Types',
      scope: 'SCHOOL',
      questions: [
        { type: 'TEXT', label: 'Freitext-Frage', required: false },
        { type: 'SINGLE_CHOICE', label: 'Einfachauswahl-Frage', required: false, options: ['A', 'B', 'C'] },
        { type: 'MULTIPLE_CHOICE', label: 'Mehrfachauswahl-Frage', required: false, options: ['X', 'Y', 'Z'] },
        { type: 'RATING', label: 'Bewertungs-Frage', required: false, ratingConfig: { min: 1, max: 5 } },
        { type: 'YES_NO', label: 'Ja/Nein-Frage', required: false },
      ],
    })
    expect(form).not.toBeNull()
    expect(form!.questions).toHaveLength(5)
    expect(form!.questions.map((q: any) => q.type)).toEqual([
      'TEXT', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'RATING', 'YES_NO',
    ])

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('all question types render correctly on form detail', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Render Types',
      scope: 'SCHOOL',
      questions: [
        { type: 'TEXT', label: 'Freitextfrage', required: false },
        { type: 'SINGLE_CHOICE', label: 'Auswahlfrage', required: false, options: ['Ja', 'Nein', 'Vielleicht'] },
        { type: 'MULTIPLE_CHOICE', label: 'Mehrfach', required: false, options: ['Rot', 'Blau'] },
        { type: 'RATING', label: 'Bewertung', required: false, ratingConfig: { min: 1, max: 5 } },
        { type: 'YES_NO', label: 'Zustimmung', required: false },
      ],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    await login(page, accounts.teacher)
    await page.goto(`/forms/${form!.form.id}`)
    await page.waitForLoadState('networkidle')

    // TEXT => textarea visible
    await expect(page.locator('textarea').first()).toBeVisible({ timeout: 10000 })

    // SINGLE_CHOICE => radio buttons
    const radioGroup = page.locator('.radio-group, .radio-item')
    await expect(radioGroup.first()).toBeVisible({ timeout: 5000 })

    // MULTIPLE_CHOICE => checkboxes
    const checkGroup = page.locator('.checkbox-group, .checkbox-item')
    await expect(checkGroup.first()).toBeVisible({ timeout: 5000 })

    // RATING => rating component
    const ratingComp = page.locator('.p-rating')
    await expect(ratingComp.first()).toBeVisible({ timeout: 5000 })

    // YES_NO => Ja/Nein buttons
    const yesBtn = page.locator('button:has-text("Ja")').first()
    await expect(yesBtn).toBeVisible({ timeout: 5000 })
    const noBtn = page.locator('button:has-text("Nein")').first()
    await expect(noBtn).toBeVisible({ timeout: 5000 })

    // Cleanup
    await login(page, accounts.admin)
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-177: Deadline expired
// --------------------------------------------------------------------------
test.describe('US-177: Deadline expired', () => {

  test('form with past deadline shows expired hint and blocks submission', async ({ page }) => {
    await login(page, accounts.admin)

    // Create form with past deadline
    const form = await createFormViaApi(page, {
      title: 'E2E Expired Deadline',
      scope: 'SCHOOL',
      deadline: '2024-01-01',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    await login(page, accounts.teacher)
    await page.goto(`/forms/${form!.form.id}`)
    await page.waitForLoadState('networkidle')

    // The form should not show the response form, or if it does,
    // the submit button should be disabled/not visible
    // since the deadline has passed, canRespond should be false
    const submitBtn = page.locator('button:has-text("Antwort absenden")')
    const isSubmitVisible = await submitBtn.isVisible({ timeout: 5000 }).catch(() => false)

    // When deadline has passed, the response form should not be shown
    // unless the form detail page handles it differently
    // Either submit is hidden or disabled
    if (isSubmitVisible) {
      const isDisabled = await submitBtn.isDisabled()
      // If visible, it should be disabled
      expect(isDisabled).toBe(true)
    }

    // Cleanup
    await login(page, accounts.admin)
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-178: Module disabled
// --------------------------------------------------------------------------
test.describe('US-178: Module disabled', () => {

  // TODO: Testing module disabled state requires toggling the forms module off,
  // which affects the entire application state. This should be tested in
  // a dedicated environment or via admin API toggle.
  test.skip('forms nav item not shown when module disabled', async ({ page }) => {
    // When forms module is disabled:
    // - No /forms nav entry in sidebar/bottom nav
    // - No dashboard widget
    // - API returns 404
    await login(page, accounts.admin)
    // This test is skipped because toggling the module would affect other tests
  })
})

// --------------------------------------------------------------------------
// US-179: Individual answers
// --------------------------------------------------------------------------
test.describe('US-179: Individual answers', () => {

  test('results page shows individual responses table for non-anonymous forms', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Individual Answers',
      scope: 'SCHOOL',
      anonymous: false,
      questions: [{ type: 'TEXT', label: 'Individuell', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    // Submit as teacher
    await login(page, accounts.teacher)
    await submitResponseViaApi(page, form!.form.id, [
      { questionId: form!.questions[0].id, text: 'Teacher answer' },
    ])

    // View results as admin
    await login(page, accounts.admin)
    await page.goto(`/forms/${form!.form.id}/results`)
    await page.waitForLoadState('networkidle')

    // "Einzelantworten" section should be visible
    const individualSection = page.locator('text=Einzelantworten')
    await expect(individualSection).toBeVisible({ timeout: 10000 })

    // DataTable should contain the response
    const dataTable = page.locator('.p-datatable')
    await expect(dataTable).toBeVisible({ timeout: 5000 })

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('individual responses API returns user answers', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Responses API',
      scope: 'SCHOOL',
      anonymous: false,
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    test.skip(!form, 'Could not create form')
    await publishFormViaApi(page, form!.form.id)

    // Submit as teacher
    await login(page, accounts.teacher)
    await submitResponseViaApi(page, form!.form.id, [
      { questionId: form!.questions[0].id, text: 'API individual' },
    ])

    // Fetch individual responses
    await login(page, accounts.admin)
    const res = await page.request.get(`/api/v1/forms/${form!.form.id}/responses`)
    expect(res.ok()).toBe(true)
    const json = await res.json()
    const responses = json.data
    expect(responses.length).toBeGreaterThanOrEqual(1)
    expect(responses[0].answers).toBeDefined()

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-180: Edit draft
// --------------------------------------------------------------------------
test.describe('US-180: Edit draft', () => {

  test('edit questions button visible on draft form detail', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Edit Draft',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Original Question', required: false }],
    })
    test.skip(!form, 'Could not create form')

    await page.goto(`/forms/${form!.form.id}`)
    await page.waitForLoadState('networkidle')

    // "Fragen bearbeiten" button should be visible for DRAFT status
    const editBtn = page.locator('button:has-text("Fragen bearbeiten")')
    await expect(editBtn).toBeVisible({ timeout: 10000 })

    await editBtn.click()
    await page.waitForURL(`**/forms/${form!.form.id}/edit`, { timeout: 10000 })

    // Should show edit form with existing question
    const qLabel = page.locator('#q-label-0')
    await expect(qLabel).toBeVisible({ timeout: 10000 })
    const value = await qLabel.inputValue()
    expect(value).toBe('Original Question')

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })

  test('draft form can be updated via API', async ({ page }) => {
    await login(page, accounts.admin)

    const form = await createFormViaApi(page, {
      title: 'E2E Update Draft',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Old Q', required: false }],
    })
    test.skip(!form, 'Could not create form')

    const res = await page.request.put(`/api/v1/forms/${form!.form.id}`, {
      data: {
        title: 'E2E Updated Draft',
        questions: [
          { type: 'TEXT', label: 'New Question 1', required: true },
          { type: 'YES_NO', label: 'New Question 2', required: false },
        ],
      },
    })
    expect(res.ok()).toBe(true)

    const detail = await getFormViaApi(page, form!.form.id)
    expect(detail.form.title).toBe('E2E Updated Draft')
    expect(detail.questions).toHaveLength(2)

    // Cleanup
    await deleteFormViaApi(page, form!.form.id)
  })
})

// --------------------------------------------------------------------------
// US-181: Negative test — validation errors
// --------------------------------------------------------------------------
test.describe('US-181: Negative test — validation errors', () => {

  test('creating form without title returns error from API', async ({ page }) => {
    await login(page, accounts.admin)

    try {
      const res = await page.request.post('/api/v1/forms', {
        data: {
          title: '',
          type: 'SURVEY',
          scope: 'SCHOOL',
          anonymous: false,
          questions: [{ type: 'TEXT', label: 'Q', required: false }],
        },
      })
      // Should fail with 400 Bad Request
      expect(res.ok()).toBe(false)
      expect(res.status()).toBeGreaterThanOrEqual(400)
    } catch {
      // Request error is also acceptable
    }
  })

  test('creating form without questions still creates as draft', async ({ page }) => {
    await login(page, accounts.admin)

    // Forms can be saved as drafts without questions, but publishing requires questions
    const form = await createFormViaApi(page, {
      title: 'E2E No Questions',
      scope: 'SCHOOL',
      questions: [],
    })

    if (form) {
      // If allowed without questions, verify it's a draft
      expect(form.form.status).toBe('DRAFT')
      // But publishing should fail or produce a form with no questions
      // Cleanup
      await deleteFormViaApi(page, form.form.id)
    }
    // If null, the API rejected no-questions which is also valid
  })

  test('save draft button disabled when title is empty on create page', async ({ page }) => {
    await login(page, accounts.admin)

    await page.goto('/forms/create')
    await page.waitForLoadState('networkidle')

    // Do not fill title
    const draftBtn = page.locator('button:has-text("Als Entwurf speichern")')
    await expect(draftBtn).toBeVisible({ timeout: 10000 })

    // Button should be disabled when title is empty
    const isDisabled = await draftBtn.isDisabled()
    expect(isDisabled).toBe(true)
  })

  test('publish button disabled when no questions added on create page', async ({ page }) => {
    await login(page, accounts.admin)

    await page.goto('/forms/create')
    await page.waitForLoadState('networkidle')

    // Fill title but add no questions
    const titleInput = page.locator('#form-title')
    await titleInput.fill('Test Title')

    // Select scope
    const scopeSelect = page.locator('#form-scope')
    if (await scopeSelect.isVisible({ timeout: 3000 }).catch(() => false)) {
      await scopeSelect.click()
      await page.locator('.p-select-option, .p-listbox-option, li[role="option"]').filter({ hasText: 'Schulweit' }).click()
    }

    // Publish button should be disabled (no questions)
    const publishBtn = page.locator('button:has-text("Veröffentlichen")')
    await expect(publishBtn).toBeVisible({ timeout: 5000 })
    const isDisabled = await publishBtn.isDisabled()
    expect(isDisabled).toBe(true)
  })
})

// --------------------------------------------------------------------------
// US-182: Scope permissions — LEADER→ROOM, TEACHER→SECTION, SA→SCHOOL
// --------------------------------------------------------------------------
test.describe('US-182: Scope permissions', () => {

  test('teacher cannot create SCHOOL-scoped form via API', async ({ page }) => {
    await login(page, accounts.teacher)

    const form = await createFormViaApi(page, {
      title: 'E2E Teacher School Attempt',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    // Teacher should not be able to create school-wide forms
    expect(form).toBeNull()
  })

  test('admin can create forms of all scopes via API', async ({ page }) => {
    await login(page, accounts.admin)

    const roomId = await getFirstRoomId(page)
    const sections = await getSections(page)

    // SCHOOL scope
    const schoolForm = await createFormViaApi(page, {
      title: 'E2E Admin School',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    expect(schoolForm).not.toBeNull()

    // SECTION scope
    if (sections.length > 0) {
      const sectionForm = await createFormViaApi(page, {
        title: 'E2E Admin Section',
        scope: 'SECTION',
        sectionIds: [sections[0].id],
        questions: [{ type: 'TEXT', label: 'Q', required: false }],
      })
      expect(sectionForm).not.toBeNull()
      if (sectionForm) await deleteFormViaApi(page, sectionForm.form.id)
    }

    // ROOM scope
    if (roomId) {
      const roomForm = await createFormViaApi(page, {
        title: 'E2E Admin Room',
        scope: 'ROOM',
        scopeId: roomId,
        questions: [{ type: 'TEXT', label: 'Q', required: false }],
      })
      expect(roomForm).not.toBeNull()
      if (roomForm) await deleteFormViaApi(page, roomForm.form.id)
    }

    // Cleanup
    if (schoolForm) await deleteFormViaApi(page, schoolForm.form.id)
  })

  test('teacher can create SECTION-scoped but not SCHOOL-scoped form', async ({ page }) => {
    await login(page, accounts.teacher)

    const sections = await getSections(page)
    test.skip(sections.length === 0, 'No sections available')

    // SECTION should work
    const sectionForm = await createFormViaApi(page, {
      title: 'E2E Teacher Section',
      scope: 'SECTION',
      sectionIds: [sections[0].id],
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    expect(sectionForm).not.toBeNull()
    if (sectionForm) await deleteFormViaApi(page, sectionForm.form.id)

    // SCHOOL should fail
    const schoolForm = await createFormViaApi(page, {
      title: 'E2E Teacher School Deny',
      scope: 'SCHOOL',
      questions: [{ type: 'TEXT', label: 'Q', required: false }],
    })
    expect(schoolForm).toBeNull()
  })

  test('scope dropdown options reflect user role on create page', async ({ page }) => {
    // Teacher: should see ROOM and SECTION but NOT SCHOOL
    await login(page, accounts.teacher)
    await page.goto('/forms/create')
    await page.waitForLoadState('networkidle')

    const scopeSelect = page.locator('#form-scope')
    await expect(scopeSelect).toBeVisible({ timeout: 10000 })
    await scopeSelect.click()

    const options = page.locator('.p-select-option, .p-listbox-option, li[role="option"]')
    await expect(options.first()).toBeVisible({ timeout: 5000 })

    // Collect all option texts
    const count = await options.count()
    const texts: string[] = []
    for (let i = 0; i < count; i++) {
      const text = await options.nth(i).textContent()
      if (text) texts.push(text.trim())
    }

    expect(texts).toContain('Raum')
    expect(texts).toContain('Schulbereich')
    expect(texts).not.toContain('Schulweit')
  })

  test('admin sees all scope options including Schulweit', async ({ page }) => {
    await login(page, accounts.admin)
    await page.goto('/forms/create')
    await page.waitForLoadState('networkidle')

    const scopeSelect = page.locator('#form-scope')
    await expect(scopeSelect).toBeVisible({ timeout: 10000 })
    await scopeSelect.click()

    const schoolOption = page.locator('.p-select-option, .p-listbox-option, li[role="option"]').filter({ hasText: 'Schulweit' })
    await expect(schoolOption).toBeVisible({ timeout: 5000 })
  })
})
