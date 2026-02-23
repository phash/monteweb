import client from './client'
import type { ApiResponse } from '@/types/api'

export interface SearchResult {
  id: string
  type: 'USER' | 'ROOM' | 'POST' | 'EVENT'
  title: string
  subtitle: string | null
  snippet: string | null
  url: string | null
  timestamp: string | null
}

export type SearchType = 'ALL' | 'USER' | 'ROOM' | 'POST' | 'EVENT'

export const searchApi = {
  search(q: string, type: SearchType = 'ALL', limit = 20) {
    return client.get<ApiResponse<SearchResult[]>>('/search', {
      params: { q, type, limit },
    })
  },
}
