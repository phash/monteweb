import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDiscussionsStore } from '@/stores/discussions'

vi.mock('@/api/discussions.api', () => ({
  discussionsApi: {
    getThreads: vi.fn(),
    getThread: vi.fn(),
    createThread: vi.fn(),
    archiveThread: vi.fn(),
    deleteThread: vi.fn(),
    getReplies: vi.fn(),
    addReply: vi.fn(),
  },
}))

import { discussionsApi } from '@/api/discussions.api'

describe('Discussions Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useDiscussionsStore()
    expect(store.threads).toEqual([])
    expect(store.currentThread).toBeNull()
    expect(store.replies).toEqual([])
    expect(store.totalThreads).toBe(0)
    expect(store.loading).toBe(false)
  })

  it('should fetch threads with pagination', async () => {
    const store = useDiscussionsStore()
    const mockThreads = [
      { id: 't1', title: 'Thread 1', replyCount: 3 },
      { id: 't2', title: 'Thread 2', replyCount: 0 },
    ]

    vi.mocked(discussionsApi.getThreads).mockResolvedValue({
      data: { data: { content: mockThreads, totalElements: 15 } },
    } as any)

    await store.fetchThreads('room1', 'ACTIVE', 0)

    expect(store.threads).toHaveLength(2)
    expect(store.totalThreads).toBe(15)
  })

  it('should fetch single thread', async () => {
    const store = useDiscussionsStore()
    const mockThread = { id: 't1', title: 'Thread 1', replyCount: 5 }

    vi.mocked(discussionsApi.getThread).mockResolvedValue({
      data: { data: mockThread },
    } as any)

    await store.fetchThread('room1', 't1')

    expect(store.currentThread).toEqual(mockThread)
  })

  it('should create thread and prepend to list', async () => {
    const store = useDiscussionsStore()
    store.threads = [{ id: 't-old', title: 'Old' }] as any
    const newThread = { id: 't-new', title: 'New Thread', replyCount: 0 }

    vi.mocked(discussionsApi.createThread).mockResolvedValue({
      data: { data: newThread },
    } as any)

    const result = await store.createThread('room1', 'New Thread', 'Content')

    expect(result.id).toBe('t-new')
    expect(store.threads[0].id).toBe('t-new')
    expect(store.threads).toHaveLength(2)
  })

  it('should archive thread and update in list and currentThread', async () => {
    const store = useDiscussionsStore()
    const archivedThread = { id: 't1', title: 'Thread 1', status: 'ARCHIVED', replyCount: 3 }
    store.threads = [{ id: 't1', title: 'Thread 1', status: 'ACTIVE', replyCount: 3 }] as any
    store.currentThread = { id: 't1', title: 'Thread 1', status: 'ACTIVE', replyCount: 3 } as any

    vi.mocked(discussionsApi.archiveThread).mockResolvedValue({
      data: { data: archivedThread },
    } as any)

    await store.archiveThread('room1', 't1')

    expect(store.threads[0].status).toBe('ARCHIVED')
    expect(store.currentThread?.status).toBe('ARCHIVED')
  })

  it('should delete thread and remove from list', async () => {
    const store = useDiscussionsStore()
    store.threads = [{ id: 't1' }, { id: 't2' }] as any
    store.currentThread = { id: 't1' } as any

    vi.mocked(discussionsApi.deleteThread).mockResolvedValue({} as any)

    await store.deleteThread('room1', 't1')

    expect(store.threads).toHaveLength(1)
    expect(store.threads[0].id).toBe('t2')
    expect(store.currentThread).toBeNull()
  })

  it('should fetch replies', async () => {
    const store = useDiscussionsStore()
    const mockReplies = [
      { id: 'r1', content: 'Reply 1' },
      { id: 'r2', content: 'Reply 2' },
    ]

    vi.mocked(discussionsApi.getReplies).mockResolvedValue({
      data: { data: { content: mockReplies } },
    } as any)

    await store.fetchReplies('room1', 't1')

    expect(store.replies).toHaveLength(2)
  })

  it('should add reply and increment replyCount', async () => {
    const store = useDiscussionsStore()
    const newReply = { id: 'r-new', content: 'New reply' }
    store.threads = [{ id: 't1', replyCount: 2 }] as any
    store.currentThread = { id: 't1', replyCount: 2 } as any

    vi.mocked(discussionsApi.addReply).mockResolvedValue({
      data: { data: newReply },
    } as any)

    const result = await store.addReply('room1', 't1', 'New reply')

    expect(result.id).toBe('r-new')
    expect(store.replies).toHaveLength(1)
    expect(store.threads[0].replyCount).toBe(3)
    expect(store.currentThread?.replyCount).toBe(3)
  })
})
