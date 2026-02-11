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

describe('Feed Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useFeedStore()
    expect(store.posts).toEqual([])
    expect(store.banners).toEqual([])
    expect(store.hasMore).toBe(true)
    expect(store.loading).toBe(false)
  })

  it('should fetch feed posts', async () => {
    const store = useFeedStore()
    const mockPosts = [
      { id: '1', content: 'Post 1', pinned: false, commentCount: 0 },
      { id: '2', content: 'Post 2', pinned: false, commentCount: 1 },
    ]

    vi.mocked(feedApi.getFeed).mockResolvedValue({
      data: { data: { content: mockPosts, last: false } },
    } as any)

    await store.fetchFeed(true)

    expect(store.posts).toHaveLength(2)
    expect(store.hasMore).toBe(true)
  })

  it('should set hasMore=false when last page', async () => {
    const store = useFeedStore()

    vi.mocked(feedApi.getFeed).mockResolvedValue({
      data: { data: { content: [{ id: '1' }], last: true } },
    } as any)

    await store.fetchFeed(true)

    expect(store.hasMore).toBe(false)
  })

  it('should append posts on next page', async () => {
    const store = useFeedStore()

    vi.mocked(feedApi.getFeed)
      .mockResolvedValueOnce({
        data: { data: { content: [{ id: '1' }], last: false } },
      } as any)
      .mockResolvedValueOnce({
        data: { data: { content: [{ id: '2' }], last: true } },
      } as any)

    await store.fetchFeed(true)
    await store.fetchFeed()

    expect(store.posts).toHaveLength(2)
  })

  it('should create post and prepend', async () => {
    const store = useFeedStore()
    const newPost = { id: 'new', content: 'New Post' }

    vi.mocked(feedApi.createPost).mockResolvedValue({
      data: { data: newPost },
    } as any)

    const result = await store.createPost({ content: 'New Post' } as any)

    expect(result.id).toBe('new')
    expect(store.posts[0].id).toBe('new')
  })

  it('should delete post', async () => {
    const store = useFeedStore()
    store.posts = [{ id: '1' }, { id: '2' }] as any

    vi.mocked(feedApi.deletePost).mockResolvedValue({} as any)

    await store.deletePost('1')

    expect(store.posts).toHaveLength(1)
    expect(store.posts[0].id).toBe('2')
  })

  it('should toggle pin on post', async () => {
    const store = useFeedStore()
    store.posts = [{ id: '1', pinned: false }] as any

    vi.mocked(feedApi.pinPost).mockResolvedValue({} as any)

    await store.pinPost('1')

    expect(store.posts[0].pinned).toBe(true)
  })

  it('should fetch banners', async () => {
    const store = useFeedStore()
    const mockBanners = [{ id: 'b1', message: 'Banner 1' }]

    vi.mocked(feedApi.getBanners).mockResolvedValue({
      data: { data: mockBanners },
    } as any)

    await store.fetchBanners()

    expect(store.banners).toHaveLength(1)
  })

  it('should add comment and increment count', async () => {
    const store = useFeedStore()
    store.posts = [{ id: 'p1', commentCount: 2 }] as any

    vi.mocked(feedApi.addComment).mockResolvedValue({
      data: { data: { id: 'c1', content: 'Nice!' } },
    } as any)

    await store.addComment('p1', 'Nice!')

    expect(store.comments).toHaveLength(1)
    expect(store.posts[0].commentCount).toBe(3)
  })
})
