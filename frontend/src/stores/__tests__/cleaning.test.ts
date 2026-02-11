import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCleaningStore } from '@/stores/cleaning'

vi.mock('@/api/cleaning.api', () => ({
  getUpcomingSlots: vi.fn(),
  getMySlots: vi.fn(),
  getSlotById: vi.fn(),
  registerForSlot: vi.fn(),
  unregisterFromSlot: vi.fn(),
  checkIn: vi.fn(),
  checkOut: vi.fn(),
  getConfigs: vi.fn(),
  createConfig: vi.fn(),
  generateSlots: vi.fn(),
  cancelSlot: vi.fn(),
  getDashboard: vi.fn(),
  offerSwap: vi.fn(),
}))

import * as cleaningApi from '@/api/cleaning.api'

describe('Cleaning Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useCleaningStore()
    expect(store.upcomingSlots).toEqual([])
    expect(store.mySlots).toEqual([])
    expect(store.currentSlot).toBeNull()
    expect(store.configs).toEqual([])
    expect(store.loading).toBe(false)
  })

  it('should load upcoming slots with pagination', async () => {
    const store = useCleaningStore()
    const mockSlots = [{ id: 's1', date: '2025-03-01' }, { id: 's2', date: '2025-03-02' }]

    vi.mocked(cleaningApi.getUpcomingSlots).mockResolvedValue({
      data: { data: { content: mockSlots, totalPages: 5 } },
    } as any)

    await store.loadUpcomingSlots(2)

    expect(store.upcomingSlots).toHaveLength(2)
    expect(store.totalPages).toBe(5)
    expect(store.currentPage).toBe(2)
  })

  it('should load my slots', async () => {
    const store = useCleaningStore()
    const mockSlots = [{ id: 's1' }, { id: 's2' }]

    vi.mocked(cleaningApi.getMySlots).mockResolvedValue({
      data: { data: mockSlots },
    } as any)

    await store.loadMySlots()

    expect(store.mySlots).toHaveLength(2)
  })

  it('should load single slot', async () => {
    const store = useCleaningStore()
    const mockSlot = { id: 's1', date: '2025-03-01', registrations: [] }

    vi.mocked(cleaningApi.getSlotById).mockResolvedValue({
      data: { data: mockSlot },
    } as any)

    await store.loadSlot('s1')

    expect(store.currentSlot).toEqual(mockSlot)
  })

  it('should register for slot and update currentSlot', async () => {
    const store = useCleaningStore()
    const updatedSlot = { id: 's1', registrations: [{ userId: 'u1' }] }

    vi.mocked(cleaningApi.registerForSlot).mockResolvedValue({
      data: { data: updatedSlot },
    } as any)

    const result = await store.registerForSlot('s1')

    expect(result).toEqual(updatedSlot)
    expect(store.currentSlot).toEqual(updatedSlot)
  })

  it('should check in and update currentSlot', async () => {
    const store = useCleaningStore()
    const updatedSlot = { id: 's1', checkedIn: true }

    vi.mocked(cleaningApi.checkIn).mockResolvedValue({
      data: { data: updatedSlot },
    } as any)

    const result = await store.checkIn('s1', 'qr-token-123')

    expect(result).toEqual(updatedSlot)
    expect(store.currentSlot).toEqual(updatedSlot)
  })

  it('should check out and update currentSlot', async () => {
    const store = useCleaningStore()
    const updatedSlot = { id: 's1', checkedOut: true }

    vi.mocked(cleaningApi.checkOut).mockResolvedValue({
      data: { data: updatedSlot },
    } as any)

    const result = await store.checkOut('s1')

    expect(result).toEqual(updatedSlot)
    expect(store.currentSlot).toEqual(updatedSlot)
  })

  it('should load configs (admin)', async () => {
    const store = useCleaningStore()
    const mockConfigs = [{ id: 'c1', sectionId: 'sec1', dayOfWeek: 'MONDAY' }]

    vi.mocked(cleaningApi.getConfigs).mockResolvedValue({
      data: { data: mockConfigs },
    } as any)

    await store.loadConfigs('sec1')

    expect(store.configs).toHaveLength(1)
    expect(store.configs[0].id).toBe('c1')
  })
})
