import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useFamilyStore } from '@/stores/family'

vi.mock('@/api/family.api', () => ({
  familyApi: {
    getMine: vi.fn(),
    create: vi.fn(),
    join: vi.fn(),
  },
}))

import { familyApi } from '@/api/family.api'

describe('Family Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useFamilyStore()
    expect(store.families).toEqual([])
    expect(store.hasFamily).toBe(false)
    expect(store.primaryFamily).toBeNull()
    expect(store.loading).toBe(false)
  })

  it('should fetch families', async () => {
    const store = useFamilyStore()
    const mockFamilies = [
      { id: 'f1', name: 'Müller Family' },
      { id: 'f2', name: 'Schmidt Family' },
    ]

    vi.mocked(familyApi.getMine).mockResolvedValue({
      data: { data: mockFamilies },
    } as any)

    await store.fetchFamilies()

    expect(store.families).toHaveLength(2)
  })

  it('should return primaryFamily as first element', async () => {
    const store = useFamilyStore()

    vi.mocked(familyApi.getMine).mockResolvedValue({
      data: { data: [{ id: 'f1', name: 'Primary' }, { id: 'f2', name: 'Other' }] },
    } as any)

    await store.fetchFamilies()

    expect(store.primaryFamily).toEqual({ id: 'f1', name: 'Primary' })
  })

  it('should set hasFamily to true after fetch', async () => {
    const store = useFamilyStore()

    vi.mocked(familyApi.getMine).mockResolvedValue({
      data: { data: [{ id: 'f1', name: 'Family' }] },
    } as any)

    await store.fetchFamilies()

    expect(store.hasFamily).toBe(true)
  })

  it('should create family and append to list', async () => {
    const store = useFamilyStore()
    const newFamily = { id: 'f-new', name: 'New Family' }

    vi.mocked(familyApi.create).mockResolvedValue({
      data: { data: newFamily },
    } as any)

    const result = await store.createFamily('New Family')

    expect(result.id).toBe('f-new')
    expect(store.families).toHaveLength(1)
    expect(store.hasFamily).toBe(true)
  })

  it('should join family and append to list', async () => {
    const store = useFamilyStore()
    const joinedFamily = { id: 'f-joined', name: 'Joined Family' }

    vi.mocked(familyApi.join).mockResolvedValue({
      data: { data: joinedFamily },
    } as any)

    const result = await store.joinFamily('INVITE-CODE-123')

    expect(result.id).toBe('f-joined')
    expect(store.families).toHaveLength(1)
  })
})
