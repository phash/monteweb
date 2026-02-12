import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useFotoboxStore } from '@/stores/fotobox'

vi.mock('@/api/fotobox.api', () => ({
  fotoboxApi: {
    getSettings: vi.fn(),
    updateSettings: vi.fn(),
    getThreads: vi.fn(),
    getThread: vi.fn(),
    getThreadImages: vi.fn(),
    createThread: vi.fn(),
    updateThread: vi.fn(),
    deleteThread: vi.fn(),
    uploadImages: vi.fn(),
    updateImage: vi.fn(),
    deleteImage: vi.fn(),
    imageUrl: vi.fn((id: string) => `/api/v1/fotobox/images/${id}`),
    thumbnailUrl: vi.fn((id: string) => `/api/v1/fotobox/images/${id}/thumbnail`),
  },
}))

import { fotoboxApi } from '@/api/fotobox.api'

describe('Fotobox Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useFotoboxStore()
    expect(store.threads).toEqual([])
    expect(store.currentThread).toBeNull()
    expect(store.images).toEqual([])
    expect(store.settings).toBeNull()
    expect(store.loading).toBe(false)
  })

  it('should fetch settings', async () => {
    const store = useFotoboxStore()
    const mockSettings = {
      enabled: true,
      defaultPermission: 'POST_IMAGES',
      maxImagesPerThread: null,
      maxFileSizeMb: 10,
    }

    vi.mocked(fotoboxApi.getSettings).mockResolvedValue({
      data: { data: mockSettings },
    } as any)

    await store.fetchSettings('room1')

    expect(store.settings).toEqual(mockSettings)
    expect(fotoboxApi.getSettings).toHaveBeenCalledWith('room1')
  })

  it('should update settings', async () => {
    const store = useFotoboxStore()
    const updatedSettings = {
      enabled: true,
      defaultPermission: 'CREATE_THREADS',
      maxImagesPerThread: 50,
      maxFileSizeMb: 20,
    }

    vi.mocked(fotoboxApi.updateSettings).mockResolvedValue({
      data: { data: updatedSettings },
    } as any)

    await store.updateSettings('room1', { enabled: true })

    expect(store.settings).toEqual(updatedSettings)
  })

  it('should fetch threads', async () => {
    const store = useFotoboxStore()
    const mockThreads = [
      { id: 't1', title: 'Thread 1', imageCount: 5 },
      { id: 't2', title: 'Thread 2', imageCount: 0 },
    ]

    vi.mocked(fotoboxApi.getThreads).mockResolvedValue({
      data: { data: mockThreads },
    } as any)

    await store.fetchThreads('room1')

    expect(store.threads).toHaveLength(2)
    expect(store.threads[0].title).toBe('Thread 1')
  })

  it('should fetch single thread', async () => {
    const store = useFotoboxStore()
    const mockThread = { id: 't1', title: 'Thread 1', imageCount: 5 }

    vi.mocked(fotoboxApi.getThread).mockResolvedValue({
      data: { data: mockThread },
    } as any)

    await store.fetchThread('room1', 't1')

    expect(store.currentThread).toEqual(mockThread)
  })

  it('should fetch thread images', async () => {
    const store = useFotoboxStore()
    const mockImages = [
      { id: 'i1', threadId: 't1', originalFilename: 'photo1.jpg' },
      { id: 'i2', threadId: 't1', originalFilename: 'photo2.png' },
    ]

    vi.mocked(fotoboxApi.getThreadImages).mockResolvedValue({
      data: { data: mockImages },
    } as any)

    await store.fetchImages('room1', 't1')

    expect(store.images).toHaveLength(2)
  })

  it('should create thread and prepend to list', async () => {
    const store = useFotoboxStore()
    store.threads = [{ id: 't-old', title: 'Old' }] as any
    const newThread = { id: 't-new', title: 'New Thread', imageCount: 0 }

    vi.mocked(fotoboxApi.createThread).mockResolvedValue({
      data: { data: newThread },
    } as any)

    const result = await store.createThread('room1', { title: 'New Thread' })

    expect(result.id).toBe('t-new')
    expect(store.threads[0].id).toBe('t-new')
    expect(store.threads).toHaveLength(2)
  })

  it('should delete thread and remove from list', async () => {
    const store = useFotoboxStore()
    store.threads = [{ id: 't1' }, { id: 't2' }] as any
    store.currentThread = { id: 't1' } as any

    vi.mocked(fotoboxApi.deleteThread).mockResolvedValue({} as any)

    await store.deleteThread('room1', 't1')

    expect(store.threads).toHaveLength(1)
    expect(store.threads[0].id).toBe('t2')
    expect(store.currentThread).toBeNull()
  })

  it('should upload images and update counts', async () => {
    const store = useFotoboxStore()
    store.threads = [{ id: 't1', imageCount: 2 }] as any
    store.currentThread = { id: 't1', imageCount: 2 } as any
    const newImages = [
      { id: 'i-new1', threadId: 't1' },
      { id: 'i-new2', threadId: 't1' },
    ]

    vi.mocked(fotoboxApi.uploadImages).mockResolvedValue({
      data: { data: newImages },
    } as any)

    const files = [new File([''], 'photo.jpg')] as File[]
    await store.uploadImages('room1', 't1', files)

    expect(store.images).toHaveLength(2)
    expect(store.threads[0].imageCount).toBe(4)
    expect(store.currentThread?.imageCount).toBe(4)
  })

  it('should delete image and update counts', async () => {
    const store = useFotoboxStore()
    store.images = [
      { id: 'i1', threadId: 't1' },
      { id: 'i2', threadId: 't1' },
    ] as any
    store.threads = [{ id: 't1', imageCount: 2 }] as any
    store.currentThread = { id: 't1', imageCount: 2 } as any

    vi.mocked(fotoboxApi.deleteImage).mockResolvedValue({} as any)

    await store.deleteImage('i1')

    expect(store.images).toHaveLength(1)
    expect(store.images[0].id).toBe('i2')
    expect(store.threads[0].imageCount).toBe(1)
    expect(store.currentThread?.imageCount).toBe(1)
  })

  it('should handle settings fetch failure gracefully', async () => {
    const store = useFotoboxStore()

    vi.mocked(fotoboxApi.getSettings).mockRejectedValue(new Error('Network error'))

    await store.fetchSettings('room1')

    expect(store.settings).toBeNull()
  })
})
