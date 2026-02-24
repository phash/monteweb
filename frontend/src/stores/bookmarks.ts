import { defineStore } from 'pinia'
import { ref } from 'vue'
import { bookmarksApi } from '@/api/bookmarks.api'
import type { BookmarkInfo, BookmarkContentType } from '@/types/bookmark'

export const useBookmarkStore = defineStore('bookmarks', () => {
  const bookmarks = ref<BookmarkInfo[]>([])
  const totalElements = ref(0)
  const loading = ref(false)

  // Cache of bookmarked IDs per content type
  const bookmarkedIds = ref<Record<string, Set<string>>>({})

  async function fetchBookmarks(type?: BookmarkContentType, page = 0, size = 20) {
    loading.value = true
    try {
      const res = await bookmarksApi.list({ type, page, size })
      bookmarks.value = res.data.data.content
      totalElements.value = res.data.data.totalElements
    } finally {
      loading.value = false
    }
  }

  async function loadBookmarkedIds(contentType: BookmarkContentType) {
    try {
      const res = await bookmarksApi.getBookmarkedIds(contentType)
      bookmarkedIds.value[contentType] = new Set(res.data.data)
    } catch {
      // ignore
    }
  }

  function isBookmarked(contentType: BookmarkContentType, contentId: string): boolean {
    return bookmarkedIds.value[contentType]?.has(contentId) ?? false
  }

  async function toggle(contentType: BookmarkContentType, contentId: string) {
    try {
      const res = await bookmarksApi.toggle(contentType, contentId)
      const nowBookmarked = res.data.data.bookmarked

      if (!bookmarkedIds.value[contentType]) {
        bookmarkedIds.value[contentType] = new Set()
      }

      if (nowBookmarked) {
        bookmarkedIds.value[contentType].add(contentId)
      } else {
        bookmarkedIds.value[contentType].delete(contentId)
      }

      return nowBookmarked
    } catch {
      return null
    }
  }

  return {
    bookmarks,
    totalElements,
    loading,
    bookmarkedIds,
    fetchBookmarks,
    loadBookmarkedIds,
    isBookmarked,
    toggle,
  }
})
