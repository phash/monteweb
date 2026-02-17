import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBillingStore } from '@/stores/billing'

vi.mock('@/api/billing.api', () => ({
  billingApi: {
    listPeriods: vi.fn(),
    getActivePeriod: vi.fn(),
    createPeriod: vi.fn(),
    getReport: vi.fn(),
    closePeriod: vi.fn(),
    exportPdf: vi.fn(),
    exportCsv: vi.fn(),
  },
}))

import { billingApi } from '@/api/billing.api'

describe('Billing Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useBillingStore()
    expect(store.periods).toEqual([])
    expect(store.activePeriod).toBeNull()
    expect(store.report).toBeNull()
    expect(store.loading).toBe(false)
  })

  it('should fetch periods and active period', async () => {
    const store = useBillingStore()
    const mockPeriods = [
      { id: 'p1', name: 'Schuljahr 2024/2025', status: 'ACTIVE' },
      { id: 'p2', name: 'Schuljahr 2023/2024', status: 'CLOSED' },
    ]
    const mockActive = { id: 'p1', name: 'Schuljahr 2024/2025', status: 'ACTIVE' }

    vi.mocked(billingApi.listPeriods).mockResolvedValue({
      data: { data: mockPeriods },
    } as any)
    vi.mocked(billingApi.getActivePeriod).mockResolvedValue({
      data: { data: mockActive },
    } as any)

    await store.fetchPeriods()

    expect(store.periods).toHaveLength(2)
    expect(store.activePeriod).toEqual(mockActive)
  })

  it('should create a period', async () => {
    const store = useBillingStore()
    const newPeriod = { id: 'p-new', name: 'Schuljahr 2025/2026', status: 'ACTIVE' }

    vi.mocked(billingApi.createPeriod).mockResolvedValue({
      data: { data: newPeriod },
    } as any)

    const result = await store.createPeriod({
      name: 'Schuljahr 2025/2026',
      startDate: '2025-09-01',
      endDate: '2026-07-31',
    })

    expect(result.id).toBe('p-new')
    expect(store.activePeriod).toEqual(newPeriod)
    expect(store.periods[0]).toEqual(newPeriod)
  })

  it('should fetch report for a period', async () => {
    const store = useBillingStore()
    const mockReport = {
      period: { id: 'p1', name: 'Schuljahr 2024/2025' },
      families: [{ familyId: 'f1', familyName: 'Mueller', totalHours: 22.5 }],
      summary: { totalFamilies: 1, averageHours: 22.5, greenCount: 0, yellowCount: 0, redCount: 1 },
    }

    vi.mocked(billingApi.getReport).mockResolvedValue({
      data: { data: mockReport },
    } as any)

    await store.fetchReport('p1')

    expect(store.report).toEqual(mockReport)
    expect(store.report!.families).toHaveLength(1)
  })

  it('should handle null active period', async () => {
    const store = useBillingStore()

    vi.mocked(billingApi.listPeriods).mockResolvedValue({
      data: { data: [] },
    } as any)
    vi.mocked(billingApi.getActivePeriod).mockResolvedValue({
      data: { data: null },
    } as any)

    await store.fetchPeriods()

    expect(store.periods).toEqual([])
    expect(store.activePeriod).toBeNull()
  })

  it('should close a period and refresh data', async () => {
    const store = useBillingStore()
    const closedPeriod = { id: 'p1', name: 'Schuljahr', status: 'CLOSED' }

    vi.mocked(billingApi.closePeriod).mockResolvedValue({ data: { data: closedPeriod } } as any)
    vi.mocked(billingApi.listPeriods).mockResolvedValue({ data: { data: [closedPeriod] } } as any)
    vi.mocked(billingApi.getActivePeriod).mockResolvedValue({ data: { data: null } } as any)

    const result = await store.closePeriod('p1')

    expect(result).toEqual(closedPeriod)
    expect(billingApi.closePeriod).toHaveBeenCalledWith('p1')
    expect(billingApi.listPeriods).toHaveBeenCalled()
  })

  it('should close period and null report when no active period remains', async () => {
    const store = useBillingStore()
    store.report = { period: { id: 'p1' } } as any

    vi.mocked(billingApi.closePeriod).mockResolvedValue({ data: { data: {} } } as any)
    vi.mocked(billingApi.listPeriods).mockResolvedValue({ data: { data: [] } } as any)
    vi.mocked(billingApi.getActivePeriod).mockResolvedValue({ data: { data: null } } as any)

    await store.closePeriod('p1')

    expect(store.report).toBeNull()
  })

  it('should export PDF and trigger download', async () => {
    const store = useBillingStore()

    // Mock window.URL and document.createElement
    const mockUrl = 'blob:mock-url'
    const mockAnchor = { href: '', download: '', click: vi.fn() }
    vi.spyOn(window.URL, 'createObjectURL').mockReturnValue(mockUrl)
    vi.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => {})
    vi.spyOn(document, 'createElement').mockReturnValue(mockAnchor as any)

    vi.mocked(billingApi.exportPdf).mockResolvedValue({ data: new Blob(['pdf']) } as any)

    await store.exportPdf('p1')

    expect(billingApi.exportPdf).toHaveBeenCalledWith('p1')
    expect(mockAnchor.download).toBe('jahresabrechnung.pdf')
    expect(mockAnchor.click).toHaveBeenCalled()

    vi.restoreAllMocks()
  })

  it('should export CSV and trigger download', async () => {
    const store = useBillingStore()

    const mockUrl = 'blob:mock-csv'
    const mockAnchor = { href: '', download: '', click: vi.fn() }
    vi.spyOn(window.URL, 'createObjectURL').mockReturnValue(mockUrl)
    vi.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => {})
    vi.spyOn(document, 'createElement').mockReturnValue(mockAnchor as any)

    vi.mocked(billingApi.exportCsv).mockResolvedValue({ data: new Blob(['csv']) } as any)

    await store.exportCsv('p1')

    expect(billingApi.exportCsv).toHaveBeenCalledWith('p1')
    expect(mockAnchor.download).toBe('jahresabrechnung.csv')
    expect(mockAnchor.click).toHaveBeenCalled()

    vi.restoreAllMocks()
  })
})
