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

describe('Feed Store - Error Handling', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should expose error state', () => {
    const store = useFeedStore()
    expect(store.error).toBeNull()
  })

  it('should set error on fetchFeed failure', async () => {
    const store = useFeedStore()
    vi.mocked(feedApi.getFeed).mockRejectedValue({
      response: { data: { message: 'Server error' } },
    })

    await expect(store.fetchFeed(true)).rejects.toBeTruthy()
    expect(store.error).toBe('Server error')
    expect(store.loading).toBe(false)
  })

  it('should set fallback error message when no response message', async () => {
    const store = useFeedStore()
    vi.mocked(feedApi.getFeed).mockRejectedValue(new Error('Network error'))

    await expect(store.fetchFeed(true)).rejects.toBeTruthy()
    expect(store.error).toBe('Failed to load feed')
  })

  it('should clear error on successful fetchFeed', async () => {
    const store = useFeedStore()
    store.error = 'Previous error' as any

    vi.mocked(feedApi.getFeed).mockResolvedValue({
      data: { data: { content: [], last: true } },
    } as any)

    await store.fetchFeed(true)
    expect(store.error).toBeNull()
  })

  it('should set error on createPost failure', async () => {
    const store = useFeedStore()
    vi.mocked(feedApi.createPost).mockRejectedValue({
      response: { data: { message: 'Validation error' } },
    })

    await expect(store.createPost({ content: 'test' } as any)).rejects.toBeTruthy()
    expect(store.error).toBe('Validation error')
  })

  it('should set error on deletePost failure', async () => {
    const store = useFeedStore()
    store.posts = [{ id: '1' }] as any
    vi.mocked(feedApi.deletePost).mockRejectedValue({
      response: { data: { message: 'Forbidden' } },
    })

    await expect(store.deletePost('1')).rejects.toBeTruthy()
    expect(store.error).toBe('Forbidden')
    // Post should not be removed on error
    expect(store.posts).toHaveLength(1)
  })

  it('should set error on addComment failure', async () => {
    const store = useFeedStore()
    store.posts = [{ id: 'p1', commentCount: 2 }] as any
    vi.mocked(feedApi.addComment).mockRejectedValue({
      response: { data: { message: 'Too many requests' } },
    })

    await expect(store.addComment('p1', 'Nice!')).rejects.toBeTruthy()
    expect(store.error).toBe('Too many requests')
    // Comment count should not be incremented on error
    expect(store.posts[0].commentCount).toBe(2)
  })
})
