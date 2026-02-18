import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useJobboardStore } from '@/stores/jobboard'
import type { FamilyHoursInfo } from '@/types/jobboard'

// Mock the API module
vi.mock('@/api/jobboard.api', () => ({
  jobboardApi: {
    listJobs: vi.fn(),
    getJob: vi.fn(),
    createJob: vi.fn(),
    updateJob: vi.fn(),
    cancelJob: vi.fn(),
    getCategories: vi.fn(),
    applyForJob: vi.fn(),
    getAssignments: vi.fn(),
    getMyAssignments: vi.fn(),
    startAssignment: vi.fn(),
    completeAssignment: vi.fn(),
    confirmAssignment: vi.fn(),
    cancelAssignment: vi.fn(),
    getFamilyHours: vi.fn(),
    getReport: vi.fn(),
    getReportSummary: vi.fn(),
    exportCsv: vi.fn(),
    exportPdf: vi.fn(),
    linkEvent: vi.fn(),
  },
}))

import { jobboardApi } from '@/api/jobboard.api'

const mockedApi = vi.mocked(jobboardApi)

// ─── Helpers ────────────────────────────────────────────────────────────

function makeFamilyHours(overrides: Partial<FamilyHoursInfo> = {}): FamilyHoursInfo {
  return {
    familyId: 'fam-1',
    familyName: 'Familie Müller',
    targetHours: 30,
    completedHours: 0,
    pendingHours: 0,
    cleaningHours: 0,
    totalHours: 0,
    remainingHours: 30,
    trafficLight: 'RED',
    targetCleaningHours: 3,
    remainingCleaningHours: 3,
    cleaningTrafficLight: 'RED',
    hoursExempt: false,
    ...overrides,
  }
}

function wrapResponse<T>(data: T) {
  return { data: { data, success: true, timestamp: new Date().toISOString() } }
}

// ─── Tests ──────────────────────────────────────────────────────────────

describe('Jobboard Store — Stundenverrechnung (Fix #37)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  // ── Test 1: Normale Stunden nach Bestätigung ──────────────────────

  it('sollte normale Stunden korrekt laden (completedHours)', async () => {
    // Szenario: Familie hat 5h normale Stunden und 1.5h Reinigung
    const hours = makeFamilyHours({
      completedHours: 5,
      cleaningHours: 1.5,
      totalHours: 6.5,
      remainingHours: 23.5,
      trafficLight: 'RED',
    })
    mockedApi.getFamilyHours.mockResolvedValue(wrapResponse(hours) as any)

    const store = useJobboardStore()
    await store.fetchFamilyHours('fam-1')

    expect(store.familyHours).not.toBeNull()
    expect(store.familyHours!.completedHours).toBe(5)
    expect(store.familyHours!.cleaningHours).toBe(1.5)
    expect(store.familyHours!.totalHours).toBe(6.5)
  })

  // ── Test 2: Reinigungsstunden getrennt von normalen Stunden ───────

  it('sollte Reinigungsstunden getrennt von normalen Stunden anzeigen', async () => {
    // Szenario: 2h normal + 3h Reinigung = 5h gesamt
    const hours = makeFamilyHours({
      completedHours: 2,
      cleaningHours: 3,
      totalHours: 5,
      remainingHours: 25,
      remainingCleaningHours: 0,
      cleaningTrafficLight: 'GREEN',
      trafficLight: 'RED',
    })
    mockedApi.getFamilyHours.mockResolvedValue(wrapResponse(hours) as any)

    const store = useJobboardStore()
    await store.fetchFamilyHours('fam-1')

    // Normale und Reinigungsstunden müssen getrennt sein
    expect(store.familyHours!.completedHours).toBe(2)
    expect(store.familyHours!.cleaningHours).toBe(3)
    // Gesamt = Normal + Reinigung
    expect(store.familyHours!.totalHours).toBe(
      store.familyHours!.completedHours + store.familyHours!.cleaningHours
    )
  })

  // ── Test 3: Ampelfarben korrekt nach Prozent ──────────────────────

  it('sollte korrekte Ampelfarben je nach Zielerreichung anzeigen', async () => {
    // 75% von 30h = 22.5h → GREEN
    const greenHours = makeFamilyHours({
      targetHours: 30,
      completedHours: 20,
      cleaningHours: 5,
      totalHours: 25,
      remainingHours: 5,
      trafficLight: 'GREEN',
    })
    mockedApi.getFamilyHours.mockResolvedValue(wrapResponse(greenHours) as any)

    const store = useJobboardStore()
    await store.fetchFamilyHours('fam-1')
    expect(store.familyHours!.trafficLight).toBe('GREEN')

    // 40-74% → YELLOW
    const yellowHours = makeFamilyHours({
      targetHours: 30,
      completedHours: 10,
      cleaningHours: 2,
      totalHours: 12,
      trafficLight: 'YELLOW',
    })
    mockedApi.getFamilyHours.mockResolvedValue(wrapResponse(yellowHours) as any)
    await store.fetchFamilyHours('fam-1')
    expect(store.familyHours!.trafficLight).toBe('YELLOW')

    // <40% → RED
    const redHours = makeFamilyHours({
      targetHours: 30,
      completedHours: 2,
      cleaningHours: 0,
      totalHours: 2,
      trafficLight: 'RED',
    })
    mockedApi.getFamilyHours.mockResolvedValue(wrapResponse(redHours) as any)
    await store.fetchFamilyHours('fam-1')
    expect(store.familyHours!.trafficLight).toBe('RED')
  })

  // ── Test 4: Befreite Familien zeigen 0 Stunden ────────────────────

  it('sollte bei befreiten Familien (hoursExempt) alle Stunden als 0 anzeigen', async () => {
    const exemptHours = makeFamilyHours({
      targetHours: 0,
      completedHours: 0,
      pendingHours: 0,
      cleaningHours: 0,
      totalHours: 0,
      remainingHours: 0,
      trafficLight: 'GREEN',
      targetCleaningHours: 0,
      remainingCleaningHours: 0,
      cleaningTrafficLight: 'GREEN',
      hoursExempt: true,
    })
    mockedApi.getFamilyHours.mockResolvedValue(wrapResponse(exemptHours) as any)

    const store = useJobboardStore()
    await store.fetchFamilyHours('fam-1')

    expect(store.familyHours!.hoursExempt).toBe(true)
    expect(store.familyHours!.completedHours).toBe(0)
    expect(store.familyHours!.cleaningHours).toBe(0)
    expect(store.familyHours!.totalHours).toBe(0)
    expect(store.familyHours!.trafficLight).toBe('GREEN')
  })

  // ── Test 5: Job-Bestätigung aktualisiert Assignment-Liste ─────────

  it('sollte nach confirmAssignment den Status in der Liste aktualisieren', async () => {
    const store = useJobboardStore()

    // Vorher: unbestätigtes Assignment in der Liste
    store.myAssignments = [
      {
        id: 'assign-1',
        jobId: 'job-1',
        jobTitle: 'Hochbeete bepflanzen',
        userId: 'user-1',
        userName: 'Anna Müller',
        familyId: 'fam-1',
        familyName: 'Familie Müller',
        status: 'COMPLETED',
        actualHours: 2,
        confirmed: false,
        confirmedBy: null,
        confirmedAt: null,
        notes: null,
        assignedAt: '2026-02-18T10:00:00Z',
        completedAt: '2026-02-18T12:00:00Z',
      },
    ]

    // API gibt bestätigtes Assignment zurück
    mockedApi.confirmAssignment.mockResolvedValue(
      wrapResponse({
        ...store.myAssignments[0],
        confirmed: true,
        confirmedBy: 'admin-id',
        confirmedAt: '2026-02-18T12:30:00Z',
      }) as any
    )

    await store.confirmAssignment('assign-1')

    expect(store.myAssignments[0].confirmed).toBe(true)
    expect(store.myAssignments[0].confirmedBy).toBe('admin-id')
  })

  // ── Test 6: CompleteAssignment speichert actualHours ───────────────

  it('sollte nach completeAssignment die tatsächlichen Stunden speichern', async () => {
    const store = useJobboardStore()

    store.myAssignments = [
      {
        id: 'assign-2',
        jobId: 'job-2',
        jobTitle: 'Turnhalle wischen',
        userId: 'user-1',
        userName: 'Anna Müller',
        familyId: 'fam-1',
        familyName: 'Familie Müller',
        status: 'IN_PROGRESS',
        actualHours: null,
        confirmed: false,
        confirmedBy: null,
        confirmedAt: null,
        notes: null,
        assignedAt: '2026-02-18T10:00:00Z',
        completedAt: null,
      },
    ]

    mockedApi.completeAssignment.mockResolvedValue(
      wrapResponse({
        ...store.myAssignments[0],
        status: 'COMPLETED' as const,
        actualHours: 1.5,
        notes: 'Turnhalle blitzt',
        completedAt: '2026-02-18T11:30:00Z',
      }) as any
    )

    await store.completeAssignment('assign-2', 1.5, 'Turnhalle blitzt')

    expect(mockedApi.completeAssignment).toHaveBeenCalledWith('assign-2', 1.5, 'Turnhalle blitzt')
    expect(store.myAssignments[0].status).toBe('COMPLETED')
    expect(store.myAssignments[0].actualHours).toBe(1.5)
    expect(store.myAssignments[0].notes).toBe('Turnhalle blitzt')
  })

  // ── Test 7: Getrennte Ampel für Reinigungsstunden ─────────────────

  it('sollte getrennte Ampelfarben für normale und Reinigungsstunden haben', async () => {
    // Szenario: Normal=RED (wenig Stunden), Reinigung=GREEN (Ziel erreicht)
    const hours = makeFamilyHours({
      targetHours: 30,
      completedHours: 3,
      cleaningHours: 3,
      totalHours: 6,
      remainingHours: 24,
      trafficLight: 'RED',
      targetCleaningHours: 3,
      remainingCleaningHours: 0,
      cleaningTrafficLight: 'GREEN',
    })
    mockedApi.getFamilyHours.mockResolvedValue(wrapResponse(hours) as any)

    const store = useJobboardStore()
    await store.fetchFamilyHours('fam-1')

    // Unterschiedliche Ampeln für verschiedene Stunden-Typen
    expect(store.familyHours!.trafficLight).toBe('RED')
    expect(store.familyHours!.cleaningTrafficLight).toBe('GREEN')
  })

  // ── Test 8: Report enthält alle Familien mit korrekter Trennung ────

  it('sollte im Report alle Familien mit getrennten Stunden auflisten', async () => {
    const families: FamilyHoursInfo[] = [
      makeFamilyHours({
        familyId: 'fam-1',
        familyName: 'Familie Müller',
        completedHours: 10,
        cleaningHours: 2,
        totalHours: 12,
        trafficLight: 'YELLOW',
      }),
      makeFamilyHours({
        familyId: 'fam-2',
        familyName: 'Familie Schmidt',
        completedHours: 25,
        cleaningHours: 3,
        totalHours: 28,
        trafficLight: 'GREEN',
      }),
      makeFamilyHours({
        familyId: 'fam-3',
        familyName: 'Familie Weber',
        completedHours: 0,
        cleaningHours: 0,
        totalHours: 0,
        trafficLight: 'RED',
        hoursExempt: false,
      }),
    ]
    const summary: ReportSummary = {
      openJobs: 5,
      activeJobs: 3,
      completedJobs: 8,
      greenFamilies: 1,
      yellowFamilies: 1,
      redFamilies: 1,
    }
    mockedApi.getReport.mockResolvedValue(wrapResponse(families) as any)
    mockedApi.getReportSummary.mockResolvedValue(wrapResponse(summary) as any)

    const store = useJobboardStore()
    await store.fetchReport()

    expect(store.report).toHaveLength(3)
    // Prüfe, dass Normal- und Reinigungsstunden pro Familie korrekt sind
    expect(store.report[0].completedHours).toBe(10)
    expect(store.report[0].cleaningHours).toBe(2)
    expect(store.report[1].completedHours).toBe(25)
    expect(store.report[1].cleaningHours).toBe(3)
    // Report-Summary zählt Ampelfarben
    expect(store.reportSummary!.greenFamilies).toBe(1)
    expect(store.reportSummary!.yellowFamilies).toBe(1)
    expect(store.reportSummary!.redFamilies).toBe(1)
  })

  // ── Test 9: pendingHours werden vor Bestätigung angezeigt ─────────

  it('sollte pendingHours (unbestätigte Stunden) korrekt anzeigen', async () => {
    const hours = makeFamilyHours({
      completedHours: 5,
      pendingHours: 3,
      cleaningHours: 1,
      totalHours: 6,
      remainingHours: 24,
    })
    mockedApi.getFamilyHours.mockResolvedValue(wrapResponse(hours) as any)

    const store = useJobboardStore()
    await store.fetchFamilyHours('fam-1')

    // pendingHours = erledigte, aber noch nicht bestätigte Stunden
    expect(store.familyHours!.pendingHours).toBe(3)
    // completedHours = nur bestätigte
    expect(store.familyHours!.completedHours).toBe(5)
    // totalHours enthält NICHT pending (nur confirmed + cleaning)
    expect(store.familyHours!.totalHours).toBe(6)
  })

  // ── Test 10: API-Fehler setzt familyHours auf null ────────────────

  it('sollte bei API-Fehler familyHours auf null setzen', async () => {
    mockedApi.getFamilyHours.mockRejectedValue(new Error('Network error'))

    const store = useJobboardStore()
    await store.fetchFamilyHours('fam-1')

    expect(store.familyHours).toBeNull()
  })
})

// Import type for report summary
import type { ReportSummary } from '@/types/jobboard'
