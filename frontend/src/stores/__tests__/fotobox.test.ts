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

  // ==================== Initial State ====================

  it('should start with empty state', () => {
    const store = useFotoboxStore()
    expect(store.threads).toEqual([])
    expect(store.currentThread).toBeNull()
    expect(store.images).toEqual([])
    expect(store.settings).toBeNull()
    expect(store.loading).toBe(false)
  })

  // ==================== Settings ====================

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

  it('should handle settings fetch failure gracefully', async () => {
    const store = useFotoboxStore()

    vi.mocked(fotoboxApi.getSettings).mockRejectedValue(new Error('Network error'))

    await store.fetchSettings('room1')

    expect(store.settings).toBeNull()
  })

  it('should propagate settings update error', async () => {
    const store = useFotoboxStore()

    vi.mocked(fotoboxApi.updateSettings).mockRejectedValue(new Error('Forbidden'))

    await expect(store.updateSettings('room1', { enabled: true })).rejects.toThrow('Forbidden')
  })

  // ==================== Threads ====================

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

  it('should set loading true during fetchThreads and reset after', async () => {
    const store = useFotoboxStore()

    let resolvePromise: Function
    vi.mocked(fotoboxApi.getThreads).mockReturnValue(
      new Promise((resolve) => { resolvePromise = resolve }) as any,
    )

    const promise = store.fetchThreads('room1')
    expect(store.loading).toBe(true)

    resolvePromise!({ data: { data: [] } })
    await promise

    expect(store.loading).toBe(false)
  })

  it('should reset loading on fetchThreads error', async () => {
    const store = useFotoboxStore()

    vi.mocked(fotoboxApi.getThreads).mockRejectedValue(new Error('Error'))

    try {
      await store.fetchThreads('room1')
    } catch {
      // expected
    }

    expect(store.loading).toBe(false)
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

  it('should set loading true during fetchImages and reset after', async () => {
    const store = useFotoboxStore()

    let resolvePromise: Function
    vi.mocked(fotoboxApi.getThreadImages).mockReturnValue(
      new Promise((resolve) => { resolvePromise = resolve }) as any,
    )

    const promise = store.fetchImages('room1', 't1')
    expect(store.loading).toBe(true)

    resolvePromise!({ data: { data: [] } })
    await promise

    expect(store.loading).toBe(false)
  })

  it('should reset loading on fetchImages error', async () => {
    const store = useFotoboxStore()

    vi.mocked(fotoboxApi.getThreadImages).mockRejectedValue(new Error('Error'))

    try {
      await store.fetchImages('room1', 't1')
    } catch {
      // expected
    }

    expect(store.loading).toBe(false)
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

  it('should create thread with empty threads list', async () => {
    const store = useFotoboxStore()
    const newThread = { id: 't-first', title: 'First Thread', imageCount: 0 }

    vi.mocked(fotoboxApi.createThread).mockResolvedValue({
      data: { data: newThread },
    } as any)

    await store.createThread('room1', { title: 'First Thread' })

    expect(store.threads).toHaveLength(1)
    expect(store.threads[0].id).toBe('t-first')
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

  it('should not clear currentThread when deleting different thread', async () => {
    const store = useFotoboxStore()
    store.threads = [{ id: 't1' }, { id: 't2' }] as any
    store.currentThread = { id: 't2' } as any

    vi.mocked(fotoboxApi.deleteThread).mockResolvedValue({} as any)

    await store.deleteThread('room1', 't1')

    expect(store.currentThread).not.toBeNull()
    expect(store.currentThread!.id).toBe('t2')
  })

  it('should handle deleting last thread', async () => {
    const store = useFotoboxStore()
    store.threads = [{ id: 't1' }] as any
    store.currentThread = { id: 't1' } as any

    vi.mocked(fotoboxApi.deleteThread).mockResolvedValue({} as any)

    await store.deleteThread('room1', 't1')

    expect(store.threads).toHaveLength(0)
    expect(store.currentThread).toBeNull()
  })

  // ==================== Images ====================

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

  it('should upload images with caption', async () => {
    const store = useFotoboxStore()
    store.threads = [{ id: 't1', imageCount: 0 }] as any
    const newImages = [{ id: 'i1', threadId: 't1', caption: 'My caption' }]

    vi.mocked(fotoboxApi.uploadImages).mockResolvedValue({
      data: { data: newImages },
    } as any)

    const files = [new File([''], 'photo.jpg')] as File[]
    await store.uploadImages('room1', 't1', files, 'My caption')

    expect(fotoboxApi.uploadImages).toHaveBeenCalledWith('room1', 't1', files, 'My caption')
  })

  it('should not update imageCount for unmatched thread', async () => {
    const store = useFotoboxStore()
    store.threads = [{ id: 't2', imageCount: 3 }] as any
    store.currentThread = null
    const newImages = [{ id: 'i1', threadId: 't1' }]

    vi.mocked(fotoboxApi.uploadImages).mockResolvedValue({
      data: { data: newImages },
    } as any)

    const files = [new File([''], 'photo.jpg')] as File[]
    await store.uploadImages('room1', 't1', files)

    expect(store.threads[0].imageCount).toBe(3)
  })

  it('should return uploaded images from uploadImages', async () => {
    const store = useFotoboxStore()
    store.threads = [{ id: 't1', imageCount: 0 }] as any
    const newImages = [{ id: 'i1', threadId: 't1' }, { id: 'i2', threadId: 't1' }]

    vi.mocked(fotoboxApi.uploadImages).mockResolvedValue({
      data: { data: newImages },
    } as any)

    const result = await store.uploadImages('room1', 't1', [new File([''], 'photo.jpg')])

    expect(result).toHaveLength(2)
    expect(result[0].id).toBe('i1')
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

  it('should not update thread count when deleting image from unknown thread', async () => {
    const store = useFotoboxStore()
    store.images = [{ id: 'i1', threadId: 't-unknown' }] as any
    store.threads = [{ id: 't1', imageCount: 5 }] as any

    vi.mocked(fotoboxApi.deleteImage).mockResolvedValue({} as any)

    await store.deleteImage('i1')

    expect(store.threads[0].imageCount).toBe(5)
  })

  it('should not update currentThread count for different thread', async () => {
    const store = useFotoboxStore()
    store.images = [{ id: 'i1', threadId: 't2' }] as any
    store.threads = [{ id: 't1', imageCount: 3 }, { id: 't2', imageCount: 2 }] as any
    store.currentThread = { id: 't1', imageCount: 3 } as any

    vi.mocked(fotoboxApi.deleteImage).mockResolvedValue({} as any)

    await store.deleteImage('i1')

    expect(store.currentThread?.imageCount).toBe(3)
    expect(store.threads[1].imageCount).toBe(1)
  })

  it('should handle deleting last image in thread', async () => {
    const store = useFotoboxStore()
    store.images = [{ id: 'i1', threadId: 't1' }] as any
    store.threads = [{ id: 't1', imageCount: 1 }] as any
    store.currentThread = { id: 't1', imageCount: 1 } as any

    vi.mocked(fotoboxApi.deleteImage).mockResolvedValue({} as any)

    await store.deleteImage('i1')

    expect(store.images).toHaveLength(0)
    expect(store.threads[0].imageCount).toBe(0)
    expect(store.currentThread?.imageCount).toBe(0)
  })

  it('should handle deleting non-existent image gracefully', async () => {
    const store = useFotoboxStore()
    store.images = [{ id: 'i1', threadId: 't1' }] as any
    store.threads = [{ id: 't1', imageCount: 1 }] as any

    vi.mocked(fotoboxApi.deleteImage).mockResolvedValue({} as any)

    await store.deleteImage('i-nonexistent')

    // Image count should not change since the image wasn't in the local list
    expect(store.threads[0].imageCount).toBe(1)
    expect(store.images).toHaveLength(1)
  })

  // ==================== Error Propagation ====================

  it('should propagate createThread error', async () => {
    const store = useFotoboxStore()

    vi.mocked(fotoboxApi.createThread).mockRejectedValue(new Error('Forbidden'))

    await expect(store.createThread('room1', { title: 'Fail' })).rejects.toThrow('Forbidden')
  })

  it('should propagate deleteThread error', async () => {
    const store = useFotoboxStore()

    vi.mocked(fotoboxApi.deleteThread).mockRejectedValue(new Error('Not found'))

    await expect(store.deleteThread('room1', 't1')).rejects.toThrow('Not found')
  })

  it('should propagate uploadImages error', async () => {
    const store = useFotoboxStore()
    store.threads = [{ id: 't1', imageCount: 0 }] as any

    vi.mocked(fotoboxApi.uploadImages).mockRejectedValue(new Error('File too large'))

    await expect(
      store.uploadImages('room1', 't1', [new File([''], 'big.jpg')]),
    ).rejects.toThrow('File too large')
  })

  it('should propagate deleteImage error', async () => {
    const store = useFotoboxStore()

    vi.mocked(fotoboxApi.deleteImage).mockRejectedValue(new Error('Forbidden'))

    await expect(store.deleteImage('i1')).rejects.toThrow('Forbidden')
  })

  it('should propagate fetchThread error', async () => {
    const store = useFotoboxStore()

    vi.mocked(fotoboxApi.getThread).mockRejectedValue(new Error('Not found'))

    await expect(store.fetchThread('room1', 't1')).rejects.toThrow('Not found')
  })

  // ==================== State Consistency ====================

  it('should append uploaded images to existing list', async () => {
    const store = useFotoboxStore()
    store.images = [{ id: 'i-existing', threadId: 't1' }] as any
    store.threads = [{ id: 't1', imageCount: 1 }] as any
    const newImages = [{ id: 'i-new', threadId: 't1' }]

    vi.mocked(fotoboxApi.uploadImages).mockResolvedValue({
      data: { data: newImages },
    } as any)

    await store.uploadImages('room1', 't1', [new File([''], 'new.jpg')])

    expect(store.images).toHaveLength(2)
    expect(store.images[0].id).toBe('i-existing')
    expect(store.images[1].id).toBe('i-new')
  })

  it('should replace threads list on fetch', async () => {
    const store = useFotoboxStore()
    store.threads = [{ id: 't-old', title: 'Old' }] as any

    vi.mocked(fotoboxApi.getThreads).mockResolvedValue({
      data: { data: [{ id: 't-new', title: 'New' }] },
    } as any)

    await store.fetchThreads('room1')

    expect(store.threads).toHaveLength(1)
    expect(store.threads[0].id).toBe('t-new')
  })

  it('should replace images list on fetchImages', async () => {
    const store = useFotoboxStore()
    store.images = [{ id: 'i-old', threadId: 't1' }] as any

    vi.mocked(fotoboxApi.getThreadImages).mockResolvedValue({
      data: { data: [{ id: 'i-new', threadId: 't1' }] },
    } as any)

    await store.fetchImages('room1', 't1')

    expect(store.images).toHaveLength(1)
    expect(store.images[0].id).toBe('i-new')
  })
})
