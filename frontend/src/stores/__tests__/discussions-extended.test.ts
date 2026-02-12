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

describe('Discussions Store - Extended', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('fetchThreads loading state', () => {
    it('should reset loading even on error', async () => {
      const store = useDiscussionsStore()

      vi.mocked(discussionsApi.getThreads).mockRejectedValue(new Error('Error'))

      try {
        await store.fetchThreads('room-1')
      } catch {
        // Expected
      }

      expect(store.loading).toBe(false)
    })
  })

  describe('archiveThread edge cases', () => {
    it('should not update currentThread when archiving a different thread', async () => {
      const store = useDiscussionsStore()
      store.threads = [
        { id: 't1', status: 'ACTIVE' },
        { id: 't2', status: 'ACTIVE' },
      ] as any
      store.currentThread = { id: 't2', status: 'ACTIVE' } as any

      vi.mocked(discussionsApi.archiveThread).mockResolvedValue({
        data: { data: { id: 't1', status: 'ARCHIVED' } },
      } as any)

      await store.archiveThread('room-1', 't1')

      expect(store.threads[0].status).toBe('ARCHIVED')
      expect(store.currentThread!.status).toBe('ACTIVE') // Unchanged
    })

    it('should handle archiving thread not in list', async () => {
      const store = useDiscussionsStore()
      store.threads = [] as any
      store.currentThread = null

      vi.mocked(discussionsApi.archiveThread).mockResolvedValue({
        data: { data: { id: 't-unknown', status: 'ARCHIVED' } },
      } as any)

      // Should not throw
      await store.archiveThread('room-1', 't-unknown')
    })
  })

  describe('deleteThread edge cases', () => {
    it('should not clear currentThread when deleting a different thread', async () => {
      const store = useDiscussionsStore()
      store.threads = [{ id: 't1' }, { id: 't2' }] as any
      store.currentThread = { id: 't2' } as any

      vi.mocked(discussionsApi.deleteThread).mockResolvedValue({} as any)

      await store.deleteThread('room-1', 't1')

      expect(store.currentThread).not.toBeNull()
      expect(store.currentThread!.id).toBe('t2')
    })
  })

  describe('addReply edge cases', () => {
    it('should not crash when thread not in threads list', async () => {
      const store = useDiscussionsStore()
      store.threads = [] as any
      store.currentThread = null

      vi.mocked(discussionsApi.addReply).mockResolvedValue({
        data: { data: { id: 'r1', content: 'Reply' } },
      } as any)

      const result = await store.addReply('room-1', 't-unknown', 'Reply')

      expect(result.id).toBe('r1')
      expect(store.replies).toHaveLength(1)
    })

    it('should not increment currentThread replyCount for different thread', async () => {
      const store = useDiscussionsStore()
      store.threads = [
        { id: 't1', replyCount: 5 },
        { id: 't2', replyCount: 3 },
      ] as any
      store.currentThread = { id: 't2', replyCount: 3 } as any

      vi.mocked(discussionsApi.addReply).mockResolvedValue({
        data: { data: { id: 'r1', content: 'Reply to t1' } },
      } as any)

      await store.addReply('room-1', 't1', 'Reply to t1')

      expect(store.threads[0].replyCount).toBe(6) // t1 incremented
      expect(store.currentThread!.replyCount).toBe(3) // t2 unchanged
    })
  })

  describe('fetchReplies loading state', () => {
    it('should set loading during fetch', async () => {
      const store = useDiscussionsStore()

      let resolveGet!: Function
      vi.mocked(discussionsApi.getReplies).mockReturnValue(
        new Promise((resolve) => { resolveGet = resolve }) as any
      )

      const promise = store.fetchReplies('room-1', 't1')
      expect(store.loading).toBe(true)

      resolveGet({ data: { data: { content: [] } } })
      await promise

      expect(store.loading).toBe(false)
    })

    it('should reset loading even on error', async () => {
      const store = useDiscussionsStore()

      vi.mocked(discussionsApi.getReplies).mockRejectedValue(new Error('Error'))

      try {
        await store.fetchReplies('room-1', 't1')
      } catch {
        // Expected
      }

      expect(store.loading).toBe(false)
    })
  })

  describe('createThread with audience parameter', () => {
    it('should pass audience to API', async () => {
      const store = useDiscussionsStore()

      vi.mocked(discussionsApi.createThread).mockResolvedValue({
        data: { data: { id: 't-new', title: 'New', replyCount: 0 } },
      } as any)

      await store.createThread('room-1', 'New Thread', 'Content here', 'PARENTS_ONLY')

      expect(discussionsApi.createThread).toHaveBeenCalledWith(
        'room-1', 'New Thread', 'Content here', 'PARENTS_ONLY'
      )
    })

    it('should work without optional parameters', async () => {
      const store = useDiscussionsStore()

      vi.mocked(discussionsApi.createThread).mockResolvedValue({
        data: { data: { id: 't-new', title: 'New', replyCount: 0 } },
      } as any)

      await store.createThread('room-1', 'New Thread')

      expect(discussionsApi.createThread).toHaveBeenCalledWith(
        'room-1', 'New Thread', undefined, undefined
      )
    })
  })
})
