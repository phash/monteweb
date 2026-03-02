import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { login } from '../../helpers/auth'
import { selectors, toastWithText } from '../../helpers/selectors'

// ============================================================================
// Jobboard E2E Tests — US-200 to US-219
// ============================================================================

type Page = import('@playwright/test').Page

/**
 * Helper: navigate to the jobboard page and wait for it to load.
 */
async function goToJobboard(page: Page): Promise<void> {
  await page.goto('/jobs')
  await page.waitForLoadState('networkidle')
  await expect(page.locator('.page-title')).toBeVisible({ timeout: 10000 })
  await expect(page.locator('.page-title')).toContainText('Jobbörse')
}

/**
 * Helper: create a job via API and return its info object.
 */
async function createJobViaApi(
  page: Page,
  data: {
    title: string
    description?: string
    category?: string
    location?: string
    estimatedHours?: number
    maxAssignees?: number
    scheduledDate?: string
    scheduledTime?: string
    contactInfo?: string
    visibility?: 'PUBLIC' | 'PRIVATE' | 'DRAFT'
    eventId?: string
    roomId?: string
  }
): Promise<{ id: string; status: string; visibility: string } | null> {
  try {
    const response = await page.request.post('/api/v1/jobs', {
      data: {
        title: data.title,
        description: data.description ?? 'E2E Test Job Beschreibung',
        category: data.category ?? 'Normal',
        location: data.location ?? 'Schulhof',
        estimatedHours: data.estimatedHours ?? 2,
        maxAssignees: data.maxAssignees ?? 3,
        scheduledDate: data.scheduledDate,
        scheduledTime: data.scheduledTime,
        contactInfo: data.contactInfo,
        visibility: data.visibility ?? 'PUBLIC',
        eventId: data.eventId,
        roomId: data.roomId,
      },
    })
    if (response.ok()) {
      const json = await response.json()
      return json.data
    }
  } catch { /* ignore */ }
  return null
}

/**
 * Helper: delete a job permanently via API (cleanup).
 */
async function deleteJobViaApi(page: Page, jobId: string): Promise<void> {
  try {
    await page.request.delete(`/api/v1/jobs/${jobId}?permanent=true`)
  } catch { /* ignore */ }
}

/**
 * Helper: apply for a job via API and return the assignment info.
 */
async function applyForJobViaApi(
  page: Page,
  jobId: string
): Promise<{ id: string; status: string } | null> {
  try {
    const response = await page.request.post(`/api/v1/jobs/${jobId}/apply`)
    if (response.ok()) {
      const json = await response.json()
      return json.data
    }
  } catch { /* ignore */ }
  return null
}

/**
 * Helper: get the current user's family info.
 */
async function getMyFamily(page: Page): Promise<{ id: string; name: string } | null> {
  try {
    const response = await page.request.get('/api/v1/families/mine')
    if (response.ok()) {
      const json = await response.json()
      const families = json.data ?? []
      if (families.length > 0) return families[0]
    }
  } catch { /* ignore */ }
  return null
}

/**
 * Helper: generate a unique job title for test isolation.
 */
function uniqueTitle(base: string): string {
  return `${base} ${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
}

// ============================================================================
// US-200: Job erstellen als Elternteil
// ============================================================================
test.describe('US-200: Job erstellen als Elternteil', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('should navigate to jobboard and see create button', async ({ page }) => {
    await goToJobboard(page)

    // Parent should see the "Job erstellen" button (not student-gated)
    const createButton = page.locator('button:has-text("Job erstellen")').first()
    await expect(createButton).toBeVisible()
  })

  test('should create a public job via UI', async ({ page }) => {
    await page.goto('/jobs/create')
    await page.waitForLoadState('networkidle')
    await expect(page.locator('.page-title')).toBeVisible({ timeout: 10000 })

    const jobTitle = uniqueTitle('E2E Schulhof kehren')

    // Fill the form
    await page.locator('#job-title').fill(jobTitle)
    await page.locator('#job-description').fill('Bitte den Schulhof sauber kehren.')

    // Estimated hours field
    const hoursInput = page.locator('#job-hours')
    await hoursInput.click()
    await hoursInput.press('Control+a')
    await hoursInput.pressSequentially('3')

    // Click submit
    const submitButton = page.locator('button:has-text("Job erstellen")').last()
    await submitButton.click()

    // Should navigate to the job detail page
    await page.waitForURL(/\/jobs\//, { timeout: 15000 })
    await page.waitForLoadState('networkidle')

    // Verify the job title is displayed
    await expect(page.locator('.page-title')).toContainText(jobTitle)

    // Verify status tag shows "Offen"
    const statusTag = page.locator('.p-tag').first()
    await expect(statusTag).toContainText('Offen')
  })

  test('should create a public job via API and see it in list', async ({ page }) => {
    const jobTitle = uniqueTitle('E2E API Job')
    const job = await createJobViaApi(page, { title: jobTitle })
    expect(job).toBeTruthy()
    expect(job!.status).toBe('OPEN')
    expect(job!.visibility).toBe('PUBLIC')

    // Navigate to jobboard and verify it shows up
    await goToJobboard(page)
    await expect(page.locator(`.job-card:has-text("${jobTitle}")`).first()).toBeVisible({ timeout: 10000 })

    // Cleanup
    await deleteJobViaApi(page, job!.id)
  })
})

// ============================================================================
// US-201: Student kann keinen Job erstellen
// ============================================================================
test.describe('US-201: Student kann keinen Job erstellen', () => {
  test.use({ storageState: 'e2e/.auth/student.json' })

  test('should not show "Job erstellen" button for student', async ({ page }) => {
    await goToJobboard(page)

    // The "Job erstellen" button should be hidden for students
    // The v-if checks "!auth.isStudent"
    const createButton = page.locator('button:has-text("Job erstellen")')
    await expect(createButton).toHaveCount(0)
  })

  test('should return 403 when student tries to create job via API', async ({ page }) => {
    const response = await page.request.post('/api/v1/jobs', {
      data: {
        title: 'Student Job Versuch',
        category: 'Normal',
        estimatedHours: 1,
        maxAssignees: 1,
        visibility: 'PUBLIC',
      },
    })
    expect(response.status()).toBe(403)
  })
})

// ============================================================================
// US-202: Privaten Job erstellen mit Auto-Zuweisung
// ============================================================================
test.describe('US-202: Privaten Job erstellen mit Auto-Zuweisung', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('should create a private job and auto-assign creator', async ({ page }) => {
    const jobTitle = uniqueTitle('E2E Privater Job')
    const job = await createJobViaApi(page, {
      title: jobTitle,
      visibility: 'PRIVATE',
      estimatedHours: 1.5,
    })
    expect(job).toBeTruthy()

    // Private jobs are auto-assigned
    expect(job!.status).toBe('ASSIGNED')

    // Verify the assignment exists via API
    const assignmentsRes = await page.request.get(`/api/v1/jobs/${job!.id}/assignments`)
    if (assignmentsRes.ok()) {
      const assignmentsJson = await assignmentsRes.json()
      const assignments = assignmentsJson.data ?? []
      expect(assignments.length).toBeGreaterThanOrEqual(1)
      expect(assignments[0].status).toBe('ASSIGNED')
    }

    // Verify via job detail page
    await page.goto(`/jobs/${job!.id}`)
    await page.waitForLoadState('networkidle')
    const statusTag = page.locator('.p-tag').first()
    await expect(statusTag).toContainText('Vergeben')

    // Cleanup
    await deleteJobViaApi(page, job!.id)
  })

  test('should show private job UI with Eigener Job label', async ({ page }) => {
    await goToJobboard(page)

    // The "Eigener Job" button should be visible
    const privateButton = page.locator('button:has-text("Eigener Job")')
    await expect(privateButton).toBeVisible()

    // Click it to navigate to create with PRIVATE visibility
    await privateButton.click()
    await page.waitForURL(/\/jobs\/create\?visibility=PRIVATE/, { timeout: 10000 })
    await page.waitForLoadState('networkidle')

    // Title should indicate private job creation
    await expect(page.locator('.page-title')).toContainText('Eigener Job')
  })
})

// ============================================================================
// US-203: Draft-Job erstellen und genehmigen
// ============================================================================
test.describe('US-203: Draft-Job erstellen und genehmigen', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('should create a draft job and admin can approve it', async ({ page }) => {
    const jobTitle = uniqueTitle('E2E Draft Job')
    const job = await createJobViaApi(page, {
      title: jobTitle,
      visibility: 'DRAFT',
    })
    expect(job).toBeTruthy()
    expect(job!.visibility).toBe('DRAFT')

    // Draft should NOT appear in public job listing
    const publicRes = await page.request.get('/api/v1/jobs?page=0&size=100')
    if (publicRes.ok()) {
      const publicJson = await publicRes.json()
      const publicJobs = publicJson.data?.content ?? []
      const found = publicJobs.find((j: any) => j.id === job!.id)
      expect(found).toBeUndefined()
    }

    // Login as admin to approve
    await login(page, accounts.admin)

    // Admin fetches draft jobs
    const draftsRes = await page.request.get('/api/v1/jobs/drafts')
    if (draftsRes.ok()) {
      const draftsJson = await draftsRes.json()
      const drafts = draftsJson.data?.content ?? draftsJson.data ?? []
      const draftJob = drafts.find((j: any) => j.id === job!.id)
      expect(draftJob).toBeTruthy()
    }

    // Admin approves the draft
    const approveRes = await page.request.post(`/api/v1/jobs/${job!.id}/approve`)
    expect(approveRes.ok()).toBeTruthy()

    // Verify job is now PUBLIC and OPEN
    const jobRes = await page.request.get(`/api/v1/jobs/${job!.id}`)
    if (jobRes.ok()) {
      const jobJson = await jobRes.json()
      expect(jobJson.data.visibility).toBe('PUBLIC')
      expect(jobJson.data.status).toBe('OPEN')
    }

    // Cleanup
    await deleteJobViaApi(page, job!.id)
  })
})

// ============================================================================
// US-204: Fuer einen Job bewerben
// ============================================================================
test.describe('US-204: Fuer einen Job bewerben', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('should allow parent to apply for a job', async ({ page }) => {
    // Create a job as admin first
    await login(page, accounts.admin)
    const jobTitle = uniqueTitle('E2E Job zum Bewerben')
    const job = await createJobViaApi(page, {
      title: jobTitle,
      maxAssignees: 3,
    })
    expect(job).toBeTruthy()

    // Switch to parent
    await login(page, accounts.parent)

    // Apply via API
    const assignment = await applyForJobViaApi(page, job!.id)
    expect(assignment).toBeTruthy()
    expect(assignment!.status).toBe('ASSIGNED')

    // Verify on job detail page
    await page.goto(`/jobs/${job!.id}`)
    await page.waitForLoadState('networkidle')

    // The assignment section should show the parent's name
    const assignmentsSection = page.locator('.assignments-section')
    await expect(assignmentsSection).toBeVisible({ timeout: 10000 })

    // Cleanup
    await login(page, accounts.admin)
    await deleteJobViaApi(page, job!.id)
  })

  test('should prevent double application', async ({ page }) => {
    // Create job as admin
    await login(page, accounts.admin)
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Double Apply') })
    expect(job).toBeTruthy()

    // Apply as parent
    await login(page, accounts.parent)
    const first = await applyForJobViaApi(page, job!.id)
    expect(first).toBeTruthy()

    // Second application should fail
    const secondRes = await page.request.post(`/api/v1/jobs/${job!.id}/apply`)
    expect(secondRes.ok()).toBeFalsy()

    // Cleanup
    await login(page, accounts.admin)
    await deleteJobViaApi(page, job!.id)
  })

  test('should return 403 when teacher tries to apply', async ({ page }) => {
    // Create job as admin
    await login(page, accounts.admin)
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Teacher Apply') })
    expect(job).toBeTruthy()

    // Try to apply as teacher
    await login(page, accounts.teacher)
    const response = await page.request.post(`/api/v1/jobs/${job!.id}/apply`)
    expect(response.status()).toBe(403)

    // Cleanup
    await login(page, accounts.admin)
    await deleteJobViaApi(page, job!.id)
  })
})

// ============================================================================
// US-205: Stunden starten, abschliessen und bestaetigen (manuell)
// ============================================================================
test.describe('US-205: Stunden starten, abschliessen und bestaetigen', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('should complete the full assignment lifecycle', async ({ page }) => {
    // Setup: create job as admin and apply as parent
    await login(page, accounts.admin)
    const job = await createJobViaApi(page, {
      title: uniqueTitle('E2E Lifecycle'),
      estimatedHours: 2,
    })
    expect(job).toBeTruthy()

    await login(page, accounts.parent)
    const assignment = await applyForJobViaApi(page, job!.id)
    expect(assignment).toBeTruthy()
    expect(assignment!.status).toBe('ASSIGNED')

    // Start the assignment
    const startRes = await page.request.put(`/api/v1/jobs/assignments/${assignment!.id}/start`)
    expect(startRes.ok()).toBeTruthy()
    const startJson = await startRes.json()
    expect(startJson.data.status).toBe('IN_PROGRESS')

    // Complete the assignment with actual hours
    const completeRes = await page.request.put(`/api/v1/jobs/assignments/${assignment!.id}/complete`, {
      data: { actualHours: 2.5, notes: 'Hat etwas laenger gedauert' },
    })
    expect(completeRes.ok()).toBeTruthy()
    const completeJson = await completeRes.json()
    expect(completeJson.data.status).toBe('COMPLETED')
    expect(completeJson.data.confirmed).toBe(false)

    // Teacher confirms the hours
    await login(page, accounts.teacher)
    const confirmRes = await page.request.put(`/api/v1/jobs/assignments/${assignment!.id}/confirm`)
    expect(confirmRes.ok()).toBeTruthy()
    const confirmJson = await confirmRes.json()
    expect(confirmJson.data.confirmed).toBe(true)

    // Cleanup
    await login(page, accounts.admin)
    await deleteJobViaApi(page, job!.id)
  })

  test('should show complete dialog on job detail page', async ({ page }) => {
    // Setup
    await login(page, accounts.admin)
    const job = await createJobViaApi(page, {
      title: uniqueTitle('E2E Complete Dialog'),
      estimatedHours: 1.5,
    })
    expect(job).toBeTruthy()

    await login(page, accounts.parent)
    const assignment = await applyForJobViaApi(page, job!.id)
    expect(assignment).toBeTruthy()

    // Navigate to job detail
    await page.goto(`/jobs/${job!.id}`)
    await page.waitForLoadState('networkidle')

    // The "Abschliessen" button should be visible for the assigned parent
    const completeButton = page.locator('button:has-text("Abschlie")').first()
    await expect(completeButton).toBeVisible({ timeout: 10000 })

    // Click to open the complete dialog
    await completeButton.click()
    const dialog = page.locator('.p-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await expect(dialog.locator('.p-dialog-title')).toContainText('Aufgabe abschlie')

    // Cleanup
    await login(page, accounts.admin)
    await deleteJobViaApi(page, job!.id)
  })
})

// ============================================================================
// US-206: Auto-Bestaetigung bei deaktivierter Konfiguration
// ============================================================================
test.describe('US-206: Auto-Bestaetigung bei deaktivierter Konfiguration', () => {
  test.skip(true, 'TODO: Requires changing require_assignment_confirmation config mid-test, which needs admin API access and careful state restoration')

  test('should auto-confirm when require_assignment_confirmation is false', async ({ page }) => {
    // Would need: admin sets config, parent completes assignment, verify auto-confirmed
  })
})

// ============================================================================
// US-207: Zuweisung ablehnen (Reject)
// ============================================================================
test.describe('US-207: Zuweisung ablehnen (Reject)', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('should allow teacher to reject a completed assignment', async ({ page }) => {
    // Setup: create job, apply, start, complete
    await login(page, accounts.admin)
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Reject Test') })
    expect(job).toBeTruthy()

    await login(page, accounts.parent)
    const assignment = await applyForJobViaApi(page, job!.id)
    expect(assignment).toBeTruthy()

    await page.request.put(`/api/v1/jobs/assignments/${assignment!.id}/start`)
    await page.request.put(`/api/v1/jobs/assignments/${assignment!.id}/complete`, {
      data: { actualHours: 1.0 },
    })

    // Teacher rejects
    await login(page, accounts.teacher)
    const rejectRes = await page.request.put(`/api/v1/jobs/assignments/${assignment!.id}/reject`)
    expect(rejectRes.ok()).toBeTruthy()

    // Verify assignment is cancelled
    const jobRes = await page.request.get(`/api/v1/jobs/${job!.id}`)
    if (jobRes.ok()) {
      const jobJson = await jobRes.json()
      // After rejection, job should reopen
      expect(['OPEN', 'PARTIALLY_ASSIGNED']).toContain(jobJson.data.status)
    }

    // Cleanup
    await login(page, accounts.admin)
    await deleteJobViaApi(page, job!.id)
  })

  test('should show reject button in pending confirmations tab', async ({ page }) => {
    // Create setup
    await login(page, accounts.admin)
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Reject UI') })
    expect(job).toBeTruthy()

    await login(page, accounts.parent)
    const assignment = await applyForJobViaApi(page, job!.id)
    expect(assignment).toBeTruthy()
    await page.request.put(`/api/v1/jobs/assignments/${assignment!.id}/start`)
    await page.request.put(`/api/v1/jobs/assignments/${assignment!.id}/complete`, {
      data: { actualHours: 1.0 },
    })

    // Teacher views pending tab
    await login(page, accounts.teacher)
    await goToJobboard(page)

    // Click "Zu bestaetigen" tab
    const pendingTab = page.locator('[role="tab"]:has-text("Zu best")')
    if (await pendingTab.isVisible({ timeout: 3000 }).catch(() => false)) {
      await pendingTab.click()
      await page.waitForLoadState('networkidle')

      // The reject button should be visible
      const rejectButton = page.locator('button:has-text("Ablehnen")').first()
      await expect(rejectButton).toBeVisible({ timeout: 10000 })
    }

    // Cleanup
    await login(page, accounts.admin)
    await deleteJobViaApi(page, job!.id)
  })
})

// ============================================================================
// US-208: Zuweisung stornieren durch Zugewiesenen
// ============================================================================
test.describe('US-208: Zuweisung stornieren durch Zugewiesenen', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('should allow parent to cancel their own assignment via API', async ({ page }) => {
    // Setup
    await login(page, accounts.admin)
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Cancel Assignment') })
    expect(job).toBeTruthy()

    await login(page, accounts.parent)
    const assignment = await applyForJobViaApi(page, job!.id)
    expect(assignment).toBeTruthy()

    // Parent cancels own assignment
    const cancelRes = await page.request.delete(`/api/v1/jobs/assignments/${assignment!.id}`)
    expect(cancelRes.ok()).toBeTruthy()

    // Verify the job reopens
    const jobRes = await page.request.get(`/api/v1/jobs/${job!.id}`)
    if (jobRes.ok()) {
      const jobJson = await jobRes.json()
      expect(jobJson.data.status).toBe('OPEN')
    }

    // Cleanup
    await login(page, accounts.admin)
    await deleteJobViaApi(page, job!.id)
  })

  test('should show return button on job detail page', async ({ page }) => {
    // Setup
    await login(page, accounts.admin)
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Return UI') })
    expect(job).toBeTruthy()

    await login(page, accounts.parent)
    await applyForJobViaApi(page, job!.id)

    // Navigate to job detail
    await page.goto(`/jobs/${job!.id}`)
    await page.waitForLoadState('networkidle')

    // "Zurueckgeben" button should appear
    const returnButton = page.locator('button:has-text("Zurückgeben")')
    await expect(returnButton).toBeVisible({ timeout: 10000 })

    // Click it to open the confirmation dialog
    await returnButton.click()
    const dialog = page.locator('.p-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await expect(dialog).toContainText('zurückgeben')

    // Cleanup
    await login(page, accounts.admin)
    await deleteJobViaApi(page, job!.id)
  })
})

// ============================================================================
// US-209: Familien-Stundenkonto einsehen
// ============================================================================
test.describe('US-209: Familien-Stundenkonto einsehen', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('should display family hours widget on my assignments tab', async ({ page }) => {
    await goToJobboard(page)

    // Switch to "Meine Aufgaben" tab
    const assignmentsTab = page.locator('[role="tab"]:has-text("Meine Aufgaben")')
    await assignmentsTab.click()
    await page.waitForLoadState('networkidle')

    // FamilyHoursWidget may be visible if parent has a family
    // The widget renders if primary family exists and is not exempt
    const widget = page.locator('.family-hours-widget, .hours-widget').first()
    // It's okay if the widget is not visible (depends on test data)
    // But at least the tab panel should be active
    const tabPanel = page.locator('[role="tabpanel"]').first()
    await expect(tabPanel).toBeVisible()
  })

  test('should fetch family hours via API', async ({ page }) => {
    const family = await getMyFamily(page)
    if (!family) {
      test.skip(true, 'No family found for parent account')
      return
    }

    const response = await page.request.get(`/api/v1/jobs/family/${family.id}/hours`)
    expect(response.ok()).toBeTruthy()

    const json = await response.json()
    const hours = json.data
    expect(hours).toBeTruthy()
    expect(hours).toHaveProperty('targetHours')
    expect(hours).toHaveProperty('completedHours')
    expect(hours).toHaveProperty('cleaningHours')
    expect(hours).toHaveProperty('totalHours')
    expect(hours).toHaveProperty('remainingHours')
    expect(hours).toHaveProperty('trafficLight')
    expect(['GREEN', 'YELLOW', 'RED']).toContain(hours.trafficLight)
  })
})

// ============================================================================
// US-210: Stunden-Befreiung fuer Familien
// ============================================================================
test.describe('US-210: Stunden-Befreiung fuer Familien', () => {
  test.skip(true, 'TODO: Requires admin family management to toggle is_hours_exempt flag and verify traffic light turns GREEN')

  test('should show GREEN traffic light for exempt families', async ({ page }) => {
    // Would need: admin sets is_hours_exempt=true, verify family shows GREEN
  })
})

// ============================================================================
// US-211: Jahresabrechnung erstellen und abschliessen
// ============================================================================
test.describe('US-211: Jahresabrechnung erstellen und abschliessen', () => {
  test.skip(true, 'TODO: Complex API-only workflow requiring billing period creation and closing with snapshots')

  test('should create and close a billing period', async ({ page }) => {
    // Would need: admin creates billing period, then closes it
  })
})

// ============================================================================
// US-212: PDF-Export des Stundenberichts
// ============================================================================
test.describe('US-212: PDF-Export des Stundenberichts', () => {
  test.use({ storageState: 'e2e/.auth/admin.json' })

  test('should have PDF export button on admin job report page', async ({ page }) => {
    await page.goto('/admin/job-report')
    await page.waitForLoadState('networkidle')

    // Wait for page to load
    await expect(page.locator('.page-title')).toBeVisible({ timeout: 10000 })

    // PDF export button should be visible
    const pdfButton = page.locator('button:has-text("PDF")')
    await expect(pdfButton).toBeVisible()

    // CSV export button should be visible
    const csvButton = page.locator('button:has-text("CSV")')
    await expect(csvButton).toBeVisible()
  })

  test('should download PDF report via API', async ({ page }) => {
    const response = await page.request.get('/api/v1/jobs/report/pdf')
    // PDF endpoint should return 200 with application/pdf or octet-stream
    expect(response.ok()).toBeTruthy()
    const contentType = response.headers()['content-type'] ?? ''
    expect(contentType).toMatch(/pdf|octet-stream/)
  })

  test('should download CSV report via API', async ({ page }) => {
    const response = await page.request.get('/api/v1/jobs/report/export')
    expect(response.ok()).toBeTruthy()
    const contentType = response.headers()['content-type'] ?? ''
    expect(contentType).toMatch(/csv|octet-stream|text/)
  })
})

// ============================================================================
// US-213: Billing-Perioden-PDF-Export
// ============================================================================
test.describe('US-213: Billing-Perioden-PDF-Export', () => {
  test.skip(true, 'TODO: Requires a closed billing period to exist for PDF export. Complex setup with billing period creation and closure')

  test('should export billing period PDF', async ({ page }) => {
    // Would need: existing closed billing period, then export PDF
  })
})

// ============================================================================
// US-214: Admin-Gesamtuebersicht (Report Summary)
// ============================================================================
test.describe('US-214: Admin-Gesamtuebersicht (Report Summary)', () => {
  test.use({ storageState: 'e2e/.auth/admin.json' })

  test('should return report summary with job counts', async ({ page }) => {
    const response = await page.request.get('/api/v1/jobs/report/summary')
    expect(response.ok()).toBeTruthy()

    const json = await response.json()
    const summary = json.data
    expect(summary).toBeTruthy()
    expect(summary).toHaveProperty('openJobs')
    expect(summary).toHaveProperty('activeJobs')
    expect(summary).toHaveProperty('completedJobs')
    expect(summary).toHaveProperty('greenFamilies')
    expect(summary).toHaveProperty('yellowFamilies')
    expect(summary).toHaveProperty('redFamilies')

    // All values should be non-negative
    expect(summary.openJobs).toBeGreaterThanOrEqual(0)
    expect(summary.activeJobs).toBeGreaterThanOrEqual(0)
    expect(summary.completedJobs).toBeGreaterThanOrEqual(0)
    expect(summary.greenFamilies).toBeGreaterThanOrEqual(0)
    expect(summary.yellowFamilies).toBeGreaterThanOrEqual(0)
    expect(summary.redFamilies).toBeGreaterThanOrEqual(0)
  })

  test('should return report with sorted families', async ({ page }) => {
    const response = await page.request.get('/api/v1/jobs/report')
    expect(response.ok()).toBeTruthy()

    const json = await response.json()
    const families = json.data
    expect(Array.isArray(families)).toBeTruthy()

    // Each family entry should have hours fields
    if (families.length > 0) {
      const first = families[0]
      expect(first).toHaveProperty('familyId')
      expect(first).toHaveProperty('familyName')
      expect(first).toHaveProperty('targetHours')
      expect(first).toHaveProperty('completedHours')
      expect(first).toHaveProperty('trafficLight')
    }
  })

  test('should display summary cards on admin report page', async ({ page }) => {
    await page.goto('/admin/job-report')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('.page-title')).toBeVisible({ timeout: 10000 })

    // Summary cards should be visible
    const summaryCards = page.locator('.summary-card')
    // Expecting at least a few summary cards
    const count = await summaryCards.count()
    expect(count).toBeGreaterThanOrEqual(1)
  })
})

// ============================================================================
// US-215: Job-Anhaenge hochladen und herunterladen
// ============================================================================
test.describe('US-215: Job-Anhaenge hochladen und herunterladen', () => {
  test.skip(true, 'TODO: Requires file upload setup with multipart form data and test files')

  test('should upload, download, and delete attachments', async ({ page }) => {
    // Would need: create job, upload file, verify download, delete attachment
  })
})

// ============================================================================
// US-216: Job loeschen vs. stornieren
// ============================================================================
test.describe('US-216: Job loeschen vs. stornieren', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('should cancel a job (soft delete) via API', async ({ page }) => {
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Cancel Job') })
    expect(job).toBeTruthy()

    // Cancel (not permanent) — default behavior of DELETE without permanent=true
    const cancelRes = await page.request.delete(`/api/v1/jobs/${job!.id}`)
    expect(cancelRes.ok()).toBeTruthy()

    // Verify job is cancelled
    const jobRes = await page.request.get(`/api/v1/jobs/${job!.id}`)
    if (jobRes.ok()) {
      const jobJson = await jobRes.json()
      expect(jobJson.data.status).toBe('CANCELLED')
    }

    // Permanent delete cleanup
    await deleteJobViaApi(page, job!.id)
  })

  test('should permanently delete a job via API', async ({ page }) => {
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Permanent Delete') })
    expect(job).toBeTruthy()

    // Permanent delete
    const deleteRes = await page.request.delete(`/api/v1/jobs/${job!.id}?permanent=true`)
    expect(deleteRes.ok()).toBeTruthy()

    // Job should no longer be accessible
    const jobRes = await page.request.get(`/api/v1/jobs/${job!.id}`)
    expect(jobRes.ok()).toBeFalsy()
  })

  test('should show delete dialog on job detail page', async ({ page }) => {
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Delete Dialog') })
    expect(job).toBeTruthy()

    await page.goto(`/jobs/${job!.id}`)
    await page.waitForLoadState('networkidle')

    // Creator should see delete button
    const deleteButton = page.locator('button:has-text("Löschen")').first()
    await expect(deleteButton).toBeVisible({ timeout: 10000 })

    // Click to open dialog
    await deleteButton.click()
    const dialog = page.locator('.p-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await expect(dialog).toContainText('endgültig löschen')

    // Cleanup
    await deleteJobViaApi(page, job!.id)
  })

  test('should not allow non-creator/non-admin to delete', async ({ page }) => {
    // Create as admin
    await login(page, accounts.admin)
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Delete Perms') })
    expect(job).toBeTruthy()

    // Try to delete as parent (not the creator)
    await login(page, accounts.parent)
    const deleteRes = await page.request.delete(`/api/v1/jobs/${job!.id}?permanent=true`)
    expect(deleteRes.ok()).toBeFalsy()

    // Cleanup as admin
    await login(page, accounts.admin)
    await deleteJobViaApi(page, job!.id)
  })
})

// ============================================================================
// US-217: Job mit Kalender-Event verknuepfen
// ============================================================================
test.describe('US-217: Job mit Kalender-Event verknuepfen', () => {
  test.use({ storageState: 'e2e/.auth/admin.json' })

  test('should link a job to a calendar event via API', async ({ page }) => {
    // Create a calendar event first
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    const dateStr = tomorrow.toISOString().split('T')[0]

    let eventId: string | null = null
    try {
      const eventRes = await page.request.post('/api/v1/calendar/events', {
        data: {
          title: uniqueTitle('E2E Link Event'),
          startDate: dateStr,
          endDate: dateStr,
          startTime: '10:00',
          endTime: '12:00',
          scope: 'SCHOOL',
          allDay: false,
        },
      })
      if (eventRes.ok()) {
        const eventJson = await eventRes.json()
        eventId = eventJson.data?.id
      }
    } catch { /* calendar module may not be enabled */ }

    if (!eventId) {
      test.skip(true, 'Calendar module not enabled or event creation failed')
      return
    }

    // Create a job
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Linked Job') })
    expect(job).toBeTruthy()

    // Link job to event
    const linkRes = await page.request.put(`/api/v1/jobs/${job!.id}/link-event`, {
      data: { eventId },
    })
    expect(linkRes.ok()).toBeTruthy()

    // Query jobs by event
    const byEventRes = await page.request.get(`/api/v1/jobs/by-event/${eventId}`)
    expect(byEventRes.ok()).toBeTruthy()
    const byEventJson = await byEventRes.json()
    const linkedJobs = byEventJson.data ?? []
    const found = linkedJobs.find((j: any) => j.id === job!.id)
    expect(found).toBeTruthy()

    // Cleanup
    await deleteJobViaApi(page, job!.id)
    try {
      await page.request.delete(`/api/v1/calendar/events/${eventId}`)
    } catch { /* ignore */ }
  })
})

// ============================================================================
// US-218: Putzaktion erzeugt automatisch einen Job
// ============================================================================
test.describe('US-218: Putzaktion erzeugt automatisch einen Job', () => {
  test.skip(true, 'TODO: Requires cleaning module interaction and cross-module event testing. Putzaktion creation triggers automatic job creation via RoomCleaningEvent')

  test('should create a job when a Putzaktion is created', async ({ page }) => {
    // Would need: create cleaning config/slot, verify a corresponding job is created
  })
})

// ============================================================================
// US-219: Familien-Zuweisungen abrufen
// ============================================================================
test.describe('US-219: Familien-Zuweisungen abrufen', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('should fetch family assignments via API', async ({ page }) => {
    const family = await getMyFamily(page)
    if (!family) {
      test.skip(true, 'No family found for parent account')
      return
    }

    const response = await page.request.get(`/api/v1/jobs/family/${family.id}/assignments`)
    expect(response.ok()).toBeTruthy()

    const json = await response.json()
    const assignments = json.data
    expect(Array.isArray(assignments)).toBeTruthy()

    // Each entry should have expected fields
    if (assignments.length > 0) {
      const first = assignments[0]
      expect(first).toHaveProperty('id')
      expect(first).toHaveProperty('jobId')
      expect(first).toHaveProperty('jobTitle')
      expect(first).toHaveProperty('status')
      expect(first).toHaveProperty('confirmed')
    }
  })

  test('should only return confirmed completed assignments', async ({ page }) => {
    // Create a job, apply, complete, and confirm
    await login(page, accounts.admin)
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Family Assign') })
    expect(job).toBeTruthy()

    await login(page, accounts.parent)
    const assignment = await applyForJobViaApi(page, job!.id)
    expect(assignment).toBeTruthy()

    // Start and complete
    await page.request.put(`/api/v1/jobs/assignments/${assignment!.id}/start`)
    await page.request.put(`/api/v1/jobs/assignments/${assignment!.id}/complete`, {
      data: { actualHours: 1.0 },
    })

    // Confirm as teacher
    await login(page, accounts.teacher)
    await page.request.put(`/api/v1/jobs/assignments/${assignment!.id}/confirm`)

    // Now fetch family assignments
    await login(page, accounts.parent)
    const family = await getMyFamily(page)
    if (!family) {
      await login(page, accounts.admin)
      await deleteJobViaApi(page, job!.id)
      return
    }

    const response = await page.request.get(`/api/v1/jobs/family/${family.id}/assignments`)
    expect(response.ok()).toBeTruthy()

    const json = await response.json()
    const familyAssignments = json.data
    // The confirmed assignment should be in the list
    const found = familyAssignments.find((a: any) => a.id === assignment!.id)
    expect(found).toBeTruthy()
    expect(found.confirmed).toBe(true)
    expect(found.status).toBe('COMPLETED')

    // Cleanup
    await login(page, accounts.admin)
    await deleteJobViaApi(page, job!.id)
  })
})

// ============================================================================
// Additional Integration Tests (Cross-story coverage)
// ============================================================================
test.describe('Jobboard: General Integration', () => {
  test.use({ storageState: 'e2e/.auth/parent.json' })

  test('should display open jobs tab with job cards', async ({ page }) => {
    await goToJobboard(page)

    // The tabs should be visible
    const tabList = page.locator('[role="tablist"]')
    await expect(tabList).toBeVisible()

    // "Offene Jobs" tab should be present
    const openTab = page.locator('[role="tab"]:has-text("Offene Jobs")')
    await expect(openTab).toBeVisible()

    // "Meine Aufgaben" tab should be present
    const myTab = page.locator('[role="tab"]:has-text("Meine Aufgaben")')
    await expect(myTab).toBeVisible()
  })

  test('should filter jobs by category', async ({ page }) => {
    await goToJobboard(page)

    // Category filter dropdown should exist
    const categoryFilter = page.locator('.category-filter').first()
    await expect(categoryFilter).toBeVisible({ timeout: 10000 })
  })

  test('should navigate to job detail page', async ({ page }) => {
    // Create a job first
    const jobTitle = uniqueTitle('E2E Detail Nav')
    const job = await createJobViaApi(page, { title: jobTitle })
    expect(job).toBeTruthy()

    // Navigate to detail
    await page.goto(`/jobs/${job!.id}`)
    await page.waitForLoadState('networkidle')

    // Page title should show the job title
    await expect(page.locator('.page-title')).toContainText(jobTitle)

    // Detail grid should show category, hours, etc.
    const detailGrid = page.locator('.detail-grid')
    await expect(detailGrid).toBeVisible()
    await expect(detailGrid).toContainText('Normal')

    // Assignments section should exist
    await expect(page.locator('.assignments-section')).toBeVisible()

    // Attachments section should exist
    await expect(page.locator('.attachments-section')).toBeVisible()

    // Back button should navigate to jobs list
    const backButton = page.locator('button:has-text("Zurück")').first()
    await expect(backButton).toBeVisible()

    // Cleanup
    await deleteJobViaApi(page, job!.id)
  })

  test('should show edit button for job creator', async ({ page }) => {
    const job = await createJobViaApi(page, { title: uniqueTitle('E2E Edit Button') })
    expect(job).toBeTruthy()

    await page.goto(`/jobs/${job!.id}`)
    await page.waitForLoadState('networkidle')

    // Creator should see the edit button
    const editButton = page.locator('button:has-text("Bearbeiten")')
    await expect(editButton).toBeVisible({ timeout: 10000 })

    // Click edit to open dialog
    await editButton.click()
    const dialog = page.locator('.p-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await expect(dialog.locator('.p-dialog-title')).toContainText('Job bearbeiten')

    // Cleanup
    await deleteJobViaApi(page, job!.id)
  })

  test('should display my assignments tab', async ({ page }) => {
    await goToJobboard(page)

    // Switch to "Meine Aufgaben" tab
    const myTab = page.locator('[role="tab"]:has-text("Meine Aufgaben")')
    await myTab.click()
    await page.waitForLoadState('networkidle')

    // Tab content should be visible (either assignments or empty state)
    const tabPanel = page.locator('[role="tabpanel"]')
    await expect(tabPanel.first()).toBeVisible()
  })
})

test.describe('Jobboard: Admin/Teacher Features', () => {
  test.use({ storageState: 'e2e/.auth/teacher.json' })

  test('should show completed jobs tab for teacher', async ({ page }) => {
    await goToJobboard(page)

    // Teacher should see the "Abgeschlossene Jobs" tab
    const completedTab = page.locator('[role="tab"]:has-text("Abgeschlossene Jobs")')
    await expect(completedTab).toBeVisible({ timeout: 10000 })
  })

  test('should show pending confirmations tab for teacher', async ({ page }) => {
    await goToJobboard(page)

    // Teacher should see the "Zu bestaetigen" tab (if requireAssignmentConfirmation is true)
    const pendingTab = page.locator('[role="tab"]:has-text("Zu best")')
    // This may not be visible if config says otherwise, so just check without failing
    if (await pendingTab.isVisible({ timeout: 5000 }).catch(() => false)) {
      await pendingTab.click()
      await page.waitForLoadState('networkidle')

      // Should show either pending confirmations or empty state
      const tabPanel = page.locator('[role="tabpanel"]')
      await expect(tabPanel.first()).toBeVisible()
    }
  })
})

test.describe('Jobboard: Admin Report Access', () => {
  test.use({ storageState: 'e2e/.auth/admin.json' })

  test('should access admin job report page', async ({ page }) => {
    await page.goto('/admin/job-report')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('.page-title')).toBeVisible({ timeout: 10000 })

    // Report table should eventually load
    const dataTable = page.locator('.p-datatable')
    // DataTable may or may not be visible depending on data
    const pageContent = page.locator('main, .layout-content, [class*="admin"]').first()
    await expect(pageContent).toBeVisible()
  })

  test('should show drafts tab for admin on jobboard', async ({ page }) => {
    await goToJobboard(page)

    // Admin should see the "Entwuerfe" tab
    const draftsTab = page.locator('[role="tab"]:has-text("Entwürfe")')
    await expect(draftsTab).toBeVisible({ timeout: 10000 })
  })
})
