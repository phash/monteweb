import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { UserInfo, LoginResponse } from '@/types/user'
import type { RoomInfo } from '@/types/room'
import type { FamilyInfo } from '@/types/family'

export const usersApi = {
  getMe() {
    return client.get<ApiResponse<UserInfo>>('/users/me')
  },

  switchActiveRole(role: string) {
    return client.put<ApiResponse<LoginResponse>>('/users/me/active-role', { role })
  },

  updateMe(data: { firstName?: string; lastName?: string; phone?: string }) {
    return client.put<ApiResponse<UserInfo>>('/users/me', data)
  },

  uploadAvatar(file: File) {
    const form = new FormData()
    form.append('file', file)
    return client.post<ApiResponse<UserInfo>>('/users/me/avatar', form)
  },

  removeAvatar() {
    return client.delete<ApiResponse<UserInfo>>('/users/me/avatar')
  },

  getById(id: string) {
    return client.get<ApiResponse<UserInfo>>(`/users/${id}`)
  },

  search(query: string, page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<UserInfo>>>('/users/search', {
      params: { q: query, page, size },
    })
  },

  // Directory (authenticated users)
  directory(params?: { page?: number; size?: number; role?: string; sectionId?: string; roomId?: string; q?: string }) {
    return client.get<ApiResponse<PageResponse<UserInfo>>>('/users/directory', { params })
  },

  // Admin
  list(params?: { page?: number; size?: number; role?: string; active?: boolean; search?: string }) {
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

  adminUpdateProfile(id: string, data: { email?: string; firstName?: string; lastName?: string; phone?: string }) {
    return client.put<ApiResponse<UserInfo>>(`/admin/users/${id}/profile`, data)
  },

  getUserRooms(id: string) {
    return client.get<ApiResponse<RoomInfo[]>>(`/admin/users/${id}/rooms`)
  },

  getUserFamilies(id: string) {
    return client.get<ApiResponse<FamilyInfo[]>>(`/admin/users/${id}/families`)
  },

  addUserToFamily(userId: string, familyId: string, role: string) {
    return client.post<ApiResponse<void>>(`/admin/users/${userId}/families/${familyId}`, { role })
  },

  removeUserFromFamily(userId: string, familyId: string) {
    return client.delete<ApiResponse<void>>(`/admin/users/${userId}/families/${familyId}`)
  },

  addSpecialRole(userId: string, role: string) {
    return client.post<ApiResponse<UserInfo>>(`/admin/users/${userId}/special-roles`, { role })
  },

  removeSpecialRole(userId: string, role: string) {
    return client.delete<ApiResponse<UserInfo>>(`/admin/users/${userId}/special-roles/${encodeURIComponent(role)}`)
  },

  findBySpecialRole(role: string) {
    return client.get<ApiResponse<UserInfo[]>>('/admin/users/search-special', {
      params: { role },
    })
  },

  updateAssignedRoles(userId: string, roles: string[]) {
    return client.put<ApiResponse<UserInfo>>(`/admin/users/${userId}/assigned-roles`, { roles })
  },

  // DSGVO / GDPR (self-service)
  requestDeletion() {
    return client.delete('/users/me')
  },

  cancelDeletion() {
    return client.post('/users/me/cancel-deletion')
  },

  getDeletionStatus() {
    return client.get('/users/me/deletion-status')
  },

  exportMyData() {
    return client.get('/users/me/data-export')
  },

  // DSGVO / GDPR (admin)
  adminExportUserData(userId: string) {
    return client.get(`/admin/users/${userId}/data-export`)
  },

  adminRequestDeletion(userId: string) {
    return client.delete(`/admin/users/${userId}`)
  },

  adminCancelDeletion(userId: string) {
    return client.post(`/admin/users/${userId}/cancel-deletion`)
  },

  adminGetDeletionStatus(userId: string) {
    return client.get(`/admin/users/${userId}/deletion-status`)
  },
}
