import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useFundgrubeStore } from '@/stores/fundgrube'

vi.mock('@/api/fundgrube.api', () => ({
  fundgrubeApi: {
    listItems: vi.fn(),
    getItem: vi.fn(),
    createItem: vi.fn(),
    updateItem: vi.fn(),
    deleteItem: vi.fn(),
    claimItem: vi.fn(),
    uploadImages: vi.fn(),
    deleteImage: vi.fn(),
    imageUrl: vi.fn((id: string) => `/api/v1/fundgrube/images/${id}`),
    thumbnailUrl: vi.fn((id: string) => `/api/v1/fundgrube/images/${id}/thumbnail`),
  },
}))

import { fundgrubeApi } from '@/api/fundgrube.api'

const mockItem = {
  id: 'item-1',
  title: 'Lost bag',
  description: 'Blue backpack',
  sectionId: 'sec-1',
  sectionName: 'Sonnengruppe',
  createdBy: 'user-1',
  createdByName: 'Anna Müller',
  createdAt: '2025-01-01T10:00:00Z',
  updatedAt: '2025-01-01T10:00:00Z',
  claimedBy: null,
  claimedByName: null,
  claimedAt: null,
  expiresAt: null,
  claimed: false,
  images: [],
}

const mockImage = {
  id: 'img-1',
  itemId: 'item-1',
  originalFilename: 'photo.jpg',
  imageUrl: '/api/v1/fundgrube/images/img-1',
  thumbnailUrl: '/api/v1/fundgrube/images/img-1/thumbnail',
  fileSize: 12345,
  contentType: 'image/jpeg',
}

describe('Fundgrube Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  // ==================== Initial State ====================

  it('should start with empty state', () => {
    const store = useFundgrubeStore()
    expect(store.items).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.activeSectionId).toBeNull()
  })

  // ==================== fetchItems ====================

  it('should fetch all items without section filter', async () => {
    const store = useFundgrubeStore()
    vi.mocked(fundgrubeApi.listItems).mockResolvedValue({
      data: { data: [mockItem] },
    } as any)

    await store.fetchItems()

    expect(fundgrubeApi.listItems).toHaveBeenCalledWith(undefined)
    expect(store.items).toHaveLength(1)
    expect(store.items[0]).toEqual(mockItem)
    expect(store.loading).toBe(false)
    expect(store.activeSectionId).toBeNull()
  })

  it('should fetch items filtered by section', async () => {
    const store = useFundgrubeStore()
    vi.mocked(fundgrubeApi.listItems).mockResolvedValue({
      data: { data: [mockItem] },
    } as any)

    await store.fetchItems('sec-1')

    expect(fundgrubeApi.listItems).toHaveBeenCalledWith('sec-1')
    expect(store.activeSectionId).toBe('sec-1')
  })

  it('should set loading to false even if fetch fails', async () => {
    const store = useFundgrubeStore()
    vi.mocked(fundgrubeApi.listItems).mockRejectedValue(new Error('Network error'))

    await expect(store.fetchItems()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // ==================== createItem ====================

  it('should create an item and prepend to list', async () => {
    const store = useFundgrubeStore()
    store.items = [{ ...mockItem, id: 'item-existing' } as any]

    const newItem = { ...mockItem, id: 'item-new', title: 'Lost keys' }
    vi.mocked(fundgrubeApi.createItem).mockResolvedValue({
      data: { data: newItem },
    } as any)

    const result = await store.createItem({ title: 'Lost keys' })

    expect(result).toEqual(newItem)
    expect(store.items[0]).toEqual(newItem)
    expect(store.items).toHaveLength(2)
  })

  // ==================== updateItem ====================

  it('should update an item in place', async () => {
    const store = useFundgrubeStore()
    store.items = [{ ...mockItem }]

    const updated = { ...mockItem, title: 'Updated title' }
    vi.mocked(fundgrubeApi.updateItem).mockResolvedValue({
      data: { data: updated },
    } as any)

    const result = await store.updateItem('item-1', { title: 'Updated title' })

    expect(result).toEqual(updated)
    expect(store.items[0]?.title).toBe('Updated title')
  })

  it('should not crash if item not found during update', async () => {
    const store = useFundgrubeStore()
    store.items = []

    const updated = { ...mockItem }
    vi.mocked(fundgrubeApi.updateItem).mockResolvedValue({
      data: { data: updated },
    } as any)

    await store.updateItem('item-1', { title: 'Title' })
    expect(store.items).toHaveLength(0)
  })

  // ==================== deleteItem ====================

  it('should delete an item from the list', async () => {
    const store = useFundgrubeStore()
    store.items = [{ ...mockItem }]

    vi.mocked(fundgrubeApi.deleteItem).mockResolvedValue({ data: { data: null } } as any)

    await store.deleteItem('item-1')

    expect(store.items).toHaveLength(0)
    expect(fundgrubeApi.deleteItem).toHaveBeenCalledWith('item-1')
  })

  // ==================== claimItem ====================

  it('should update item after claim', async () => {
    const store = useFundgrubeStore()
    const claimed = { ...mockItem, claimed: true, claimedBy: 'user-2', claimedByName: 'Max Muster' }
    store.items = [{ ...mockItem }]

    vi.mocked(fundgrubeApi.claimItem).mockResolvedValue({
      data: { data: claimed },
    } as any)

    const result = await store.claimItem('item-1', { comment: 'This is mine' })

    expect(result).toEqual(claimed)
    expect(store.items[0]?.claimed).toBe(true)
    expect(store.items[0]?.claimedByName).toBe('Max Muster')
  })

  it('should claim without comment', async () => {
    const store = useFundgrubeStore()
    store.items = [{ ...mockItem }]

    vi.mocked(fundgrubeApi.claimItem).mockResolvedValue({
      data: { data: { ...mockItem, claimed: true } },
    } as any)

    await store.claimItem('item-1')

    expect(fundgrubeApi.claimItem).toHaveBeenCalledWith('item-1', {})
  })

  // ==================== uploadImages ====================

  it('should upload images and append to item', async () => {
    const store = useFundgrubeStore()
    store.items = [{ ...mockItem, images: [] }]

    vi.mocked(fundgrubeApi.uploadImages).mockResolvedValue({
      data: { data: [mockImage] },
    } as any)

    const file = new File(['data'], 'photo.jpg', { type: 'image/jpeg' })
    const result = await store.uploadImages('item-1', [file])

    expect(result).toEqual([mockImage])
    expect(store.items[0]?.images).toHaveLength(1)
    expect(store.items[0]?.images[0]).toEqual(mockImage)
  })

  it('should not crash when uploading to a non-existent item', async () => {
    const store = useFundgrubeStore()
    store.items = []

    vi.mocked(fundgrubeApi.uploadImages).mockResolvedValue({
      data: { data: [mockImage] },
    } as any)

    const file = new File(['data'], 'photo.jpg', { type: 'image/jpeg' })
    await store.uploadImages('item-x', [file])
    // Should not throw
  })

  // ==================== deleteImage ====================

  it('should delete an image from item', async () => {
    const store = useFundgrubeStore()
    store.items = [{ ...mockItem, images: [{ ...mockImage }] }]

    vi.mocked(fundgrubeApi.deleteImage).mockResolvedValue({ data: { data: null } } as any)

    await store.deleteImage('item-1', 'img-1')

    expect(store.items[0]?.images).toHaveLength(0)
    expect(fundgrubeApi.deleteImage).toHaveBeenCalledWith('img-1')
  })

  it('should not crash when deleting image from non-existent item', async () => {
    const store = useFundgrubeStore()
    store.items = []

    vi.mocked(fundgrubeApi.deleteImage).mockResolvedValue({ data: { data: null } } as any)

    await store.deleteImage('item-x', 'img-1')
    // Should not throw
  })
})
