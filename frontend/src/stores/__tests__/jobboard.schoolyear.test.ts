import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useJobboardStore } from '@/stores/jobboard'

vi.mock('@/api/jobboard.api', () => ({
  jobboardApi: {
    getSchoolYears: vi.fn(),
    getFamilyHours: vi.fn(),
  },
}))

import { jobboardApi } from '@/api/jobboard.api'

describe('Jobboard store — school years', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchSchoolYears stores list and selects the active period by default', async () => {
    const store = useJobboardStore()
    vi.mocked(jobboardApi.getSchoolYears).mockResolvedValue({
      data: { data: [
        { id: 'p2', name: 'Schuljahr 2025/2026', startDate: '2025-09-01', endDate: '2026-08-31', active: true },
        { id: 'p1', name: 'Schuljahr 2024/2025', startDate: '2024-09-01', endDate: '2025-08-31', active: false },
      ] },
    } as any)

    await store.fetchSchoolYears()

    expect(store.schoolYears).toHaveLength(2)
    expect(store.selectedPeriodId).toBe('p2')
  })

  it('fetchFamilyHours passes the selected periodId', async () => {
    const store = useJobboardStore()
    store.selectedPeriodId = 'p1'
    vi.mocked(jobboardApi.getFamilyHours).mockResolvedValue({
      data: { data: { familyId: 'f1', familyName: 'X', completedHours: 3 } },
    } as any)

    await store.fetchFamilyHours('f1', store.selectedPeriodId)

    expect(jobboardApi.getFamilyHours).toHaveBeenCalledWith('f1', 'p1')
    expect(store.familyHours?.completedHours).toBe(3)
  })
})
