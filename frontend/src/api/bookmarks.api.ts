import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { BookmarkInfo, BookmarkContentType } from '@/types/bookmark'

export const bookmarksApi = {
  toggle(contentType: BookmarkContentType, contentId: string) {
    return client.post<ApiResponse<{ bookmarked: boolean }>>('/bookmarks', { contentType, contentId })
  },

  list(params?: { type?: BookmarkContentType; page?: number; size?: number }) {
    return client.get<ApiResponse<PageResponse<BookmarkInfo>>>('/bookmarks', { params })
  },

  check(contentType: BookmarkContentType, contentId: string) {
    return client.get<ApiResponse<{ bookmarked: boolean }>>('/bookmarks/check', {
      params: { contentType, contentId },
    })
  },

  getBookmarkedIds(contentType: BookmarkContentType) {
    return client.get<ApiResponse<string[]>>('/bookmarks/ids', {
      params: { contentType },
    })
  },
}
