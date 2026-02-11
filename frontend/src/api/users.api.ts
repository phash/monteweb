import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { UserInfo } from '@/types/user'

export const usersApi = {
  getMe() {
    return client.get<ApiResponse<UserInfo>>('/users/me')
  },

  updateMe(data: { firstName?: string; lastName?: string; phone?: string }) {
    return client.put<ApiResponse<UserInfo>>('/users/me', data)
  },

  getById(id: string) {
    return client.get<ApiResponse<UserInfo>>(`/users/${id}`)
  },

  search(query: string, page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<UserInfo>>>('/users/search', {
      params: { q: query, page, size },
    })
  },

  // Admin
  list(params?: { page?: number; size?: number }) {
    return client.get<ApiResponse<PageResponse<UserInfo>>>('/admin/users', { params })
  },

  updateRole(id: string, role: string) {
    return client.put<ApiResponse<UserInfo>>(`/admin/users/${id}/roles`, { role })
  },

  setActive(id: string, active: boolean) {
    return client.put<ApiResponse<UserInfo>>(`/admin/users/${id}/status`, null, {
      params: { active },
    })
  },
}
