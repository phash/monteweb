import client from './client'
import type { ApiResponse } from '@/types/api'
import type {
  WikiPageResponse,
  WikiPageSummary,
  WikiPageVersionResponse,
  CreatePageRequest,
  UpdatePageRequest,
} from '@/types/wiki'

export const wikiApi = {
  // Page tree
  getPageTree(roomId: string) {
    return client.get<ApiResponse<WikiPageSummary[]>>(`/rooms/${roomId}/wiki`)
  },

  // Pages
  getPage(roomId: string, slug: string) {
    return client.get<ApiResponse<WikiPageResponse>>(`/rooms/${roomId}/wiki/pages/${slug}`)
  },

  createPage(roomId: string, data: CreatePageRequest) {
    return client.post<ApiResponse<WikiPageResponse>>(`/rooms/${roomId}/wiki/pages`, data)
  },

  updatePage(roomId: string, pageId: string, data: UpdatePageRequest) {
    return client.put<ApiResponse<WikiPageResponse>>(
      `/rooms/${roomId}/wiki/pages/${pageId}`,
      data,
    )
  },

  deletePage(roomId: string, pageId: string) {
    return client.delete<ApiResponse<void>>(`/rooms/${roomId}/wiki/pages/${pageId}`)
  },

  // Versions
  getVersions(roomId: string, pageId: string) {
    return client.get<ApiResponse<WikiPageVersionResponse[]>>(
      `/rooms/${roomId}/wiki/pages/${pageId}/versions`,
    )
  },

  getVersion(roomId: string, versionId: string) {
    return client.get<ApiResponse<WikiPageVersionResponse>>(
      `/rooms/${roomId}/wiki/versions/${versionId}`,
    )
  },

  // Search
  searchPages(roomId: string, query: string) {
    return client.get<ApiResponse<WikiPageSummary[]>>(`/rooms/${roomId}/wiki/search`, {
      params: { q: query },
    })
  },
}
