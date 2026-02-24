import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/bookmarks.api', () => ({
  bookmarksApi: {
    toggle: vi.fn().mockResolvedValue({ data: { data: { bookmarked: true } } }),
    list: vi.fn().mockResolvedValue({
      data: {
        data: {
          content: [
            { id: 'b1', userId: 'u1', contentType: 'POST', contentId: 'p1', createdAt: '2026-01-01T00:00:00Z' },
          ],
          totalElements: 1,
          totalPages: 1,
          number: 0,
        },
      },
    }),
    check: vi.fn().mockResolvedValue({ data: { data: { bookmarked: true } } }),
    getBookmarkedIds: vi.fn().mockResolvedValue({ data: { data: ['p1', 'p2'] } }),
  },
}))

import { useBookmarkStore } from '@/stores/bookmarks'
import { bookmarksApi } from '@/api/bookmarks.api'

describe('bookmarks store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useBookmarkStore()
    expect(store.bookmarks).toEqual([])
    expect(store.totalElements).toBe(0)
    expect(store.loading).toBe(false)
  })

  describe('fetchBookmarks', () => {
    it('should load bookmarks and set state', async () => {
      const store = useBookmarkStore()
      await store.fetchBookmarks()
      expect(bookmarksApi.list).toHaveBeenCalledWith({ type: undefined, page: 0, size: 20 })
      expect(store.bookmarks).toHaveLength(1)
      expect(store.totalElements).toBe(1)
    })

    it('should filter by type', async () => {
      const store = useBookmarkStore()
      await store.fetchBookmarks('EVENT', 1, 10)
      expect(bookmarksApi.list).toHaveBeenCalledWith({ type: 'EVENT', page: 1, size: 10 })
    })

    it('should set loading to true while fetching', async () => {
      const store = useBookmarkStore()
      const promise = store.fetchBookmarks()
      expect(store.loading).toBe(true)
      await promise
      expect(store.loading).toBe(false)
    })
  })

  describe('loadBookmarkedIds', () => {
    it('should load IDs for a content type', async () => {
      const store = useBookmarkStore()
      await store.loadBookmarkedIds('POST')
      expect(bookmarksApi.getBookmarkedIds).toHaveBeenCalledWith('POST')
      expect(store.isBookmarked('POST', 'p1')).toBe(true)
      expect(store.isBookmarked('POST', 'p2')).toBe(true)
      expect(store.isBookmarked('POST', 'p3')).toBe(false)
    })
  })

  describe('isBookmarked', () => {
    it('should return false when no IDs loaded', () => {
      const store = useBookmarkStore()
      expect(store.isBookmarked('POST', 'p1')).toBe(false)
    })
  })

  describe('toggle', () => {
    it('should call API and add to bookmarked IDs on bookmark', async () => {
      const store = useBookmarkStore()
      const result = await store.toggle('POST', 'new-post')
      expect(bookmarksApi.toggle).toHaveBeenCalledWith('POST', 'new-post')
      expect(result).toBe(true)
      expect(store.isBookmarked('POST', 'new-post')).toBe(true)
    })

    it('should remove from bookmarked IDs on unbookmark', async () => {
      vi.mocked(bookmarksApi.toggle).mockResolvedValueOnce({
        data: { data: { bookmarked: false } },
      } as any)
      const store = useBookmarkStore()
      // First add it
      store.bookmarkedIds['POST'] = new Set(['existing-post'])
      // Then toggle to remove
      const result = await store.toggle('POST', 'existing-post')
      expect(result).toBe(false)
      expect(store.isBookmarked('POST', 'existing-post')).toBe(false)
    })

    it('should return null on API error', async () => {
      vi.mocked(bookmarksApi.toggle).mockRejectedValueOnce(new Error('fail'))
      const store = useBookmarkStore()
      const result = await store.toggle('POST', 'fail-post')
      expect(result).toBeNull()
    })
  })
})
