import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useFeedStore } from '@/stores/feed'

vi.mock('@/api/feed.api', () => ({
  feedApi: {
    getFeed: vi.fn(),
    getBanners: vi.fn(),
    createPost: vi.fn(),
    deletePost: vi.fn(),
    pinPost: vi.fn(),
    getComments: vi.fn(),
    addComment: vi.fn(),
  },
}))

import { feedApi } from '@/api/feed.api'

describe('Feed Store - Extended', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('fetchFeed early return', () => {
    it('should not fetch when hasMore is false and not resetting', async () => {
      const store = useFeedStore()
      store.hasMore = false

      await store.fetchFeed()

      expect(feedApi.getFeed).not.toHaveBeenCalled()
    })

    it('should fetch when hasMore is false but resetting', async () => {
      const store = useFeedStore()
      store.hasMore = false

      vi.mocked(feedApi.getFeed).mockResolvedValue({
        data: { data: { content: [], last: true } },
      } as any)

      await store.fetchFeed(true)

      expect(feedApi.getFeed).toHaveBeenCalled()
    })
  })

  describe('fetchFeed loading state', () => {
    it('should set loading during fetch', async () => {
      const store = useFeedStore()

      let resolveGet!: Function
      vi.mocked(feedApi.getFeed).mockReturnValue(
        new Promise((resolve) => { resolveGet = resolve }) as any
      )

      const promise = store.fetchFeed(true)
      expect(store.loading).toBe(true)

      resolveGet({ data: { data: { content: [], last: true } } })
      await promise

      expect(store.loading).toBe(false)
    })

    it('should reset loading even on error', async () => {
      const store = useFeedStore()

      vi.mocked(feedApi.getFeed).mockRejectedValue(new Error('Network'))

      try {
        await store.fetchFeed(true)
      } catch {
        // Expected
      }

      expect(store.loading).toBe(false)
    })
  })

  describe('fetchFeed pagination', () => {
    it('should increment page counter on each fetch', async () => {
      const store = useFeedStore()

      vi.mocked(feedApi.getFeed).mockResolvedValue({
        data: { data: { content: [{ id: '1' }], last: false } },
      } as any)

      await store.fetchFeed(true) // page 0
      expect(feedApi.getFeed).toHaveBeenCalledWith(0)

      await store.fetchFeed() // page 1
      expect(feedApi.getFeed).toHaveBeenCalledWith(1)
    })

    it('should reset page to 0 on fresh fetch', async () => {
      const store = useFeedStore()

      vi.mocked(feedApi.getFeed).mockResolvedValue({
        data: { data: { content: [], last: false } },
      } as any)

      await store.fetchFeed(true) // page 0
      await store.fetchFeed() // page 1
      await store.fetchFeed(true) // should reset to page 0

      const calls = vi.mocked(feedApi.getFeed).mock.calls
      expect(calls[0][0]).toBe(0)
      expect(calls[1][0]).toBe(1)
      expect(calls[2][0]).toBe(0)
    })
  })

  describe('fetchBanners error handling', () => {
    it('should set empty banners on error', async () => {
      const store = useFeedStore()
      store.banners = [{ id: 'b1' }] as any

      vi.mocked(feedApi.getBanners).mockRejectedValue(new Error('Error'))

      await store.fetchBanners()

      expect(store.banners).toEqual([])
    })
  })

  describe('fetchComments', () => {
    it('should populate comments from API', async () => {
      const store = useFeedStore()
      const mockComments = [
        { id: 'c1', content: 'Comment 1' },
        { id: 'c2', content: 'Comment 2' },
      ]

      vi.mocked(feedApi.getComments).mockResolvedValue({
        data: { data: { content: mockComments } },
      } as any)

      await store.fetchComments('post-1')

      expect(store.commentsByPost['post-1']).toHaveLength(2)
      expect(store.commentsByPost['post-1'][0].content).toBe('Comment 1')
    })
  })

  describe('pinPost toggling', () => {
    it('should toggle pinned from false to true', async () => {
      const store = useFeedStore()
      store.posts = [{ id: '1', pinned: false }] as any

      vi.mocked(feedApi.pinPost).mockResolvedValue({} as any)

      await store.pinPost('1')

      expect(store.posts[0].pinned).toBe(true)
    })

    it('should toggle pinned from true to false', async () => {
      const store = useFeedStore()
      store.posts = [{ id: '1', pinned: true }] as any

      vi.mocked(feedApi.pinPost).mockResolvedValue({} as any)

      await store.pinPost('1')

      expect(store.posts[0].pinned).toBe(false)
    })

    it('should not crash when post not found', async () => {
      const store = useFeedStore()
      store.posts = []

      vi.mocked(feedApi.pinPost).mockResolvedValue({} as any)

      await store.pinPost('nonexistent')
      // Should not throw
    })
  })

  describe('deletePost', () => {
    it('should not crash when post not found', async () => {
      const store = useFeedStore()
      store.posts = [{ id: '1' }] as any

      vi.mocked(feedApi.deletePost).mockResolvedValue({} as any)

      await store.deletePost('nonexistent')

      expect(store.posts).toHaveLength(1) // Unchanged
    })
  })

  describe('addComment for non-existent post', () => {
    it('should still add comment even if post not in list', async () => {
      const store = useFeedStore()
      store.posts = []

      vi.mocked(feedApi.addComment).mockResolvedValue({
        data: { data: { id: 'c1', content: 'Comment' } },
      } as any)

      await store.addComment('unknown-post', 'Comment')

      expect(store.commentsByPost['unknown-post']).toHaveLength(1)
      // No crash even though post not found
    })
  })
})
