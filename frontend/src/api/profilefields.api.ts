import client from './client'
import type { ApiResponse } from '@/types/api'
import type {
  ProfileFieldDefinition,
  CreateProfileFieldRequest,
  UpdateProfileFieldRequest,
} from '@/types/profilefields'

export const profileFieldsApi = {
  // User endpoints
  getDefinitions() {
    return client.get<ApiResponse<ProfileFieldDefinition[]>>('/profile-fields')
  },
  getMyValues() {
    return client.get<ApiResponse<Record<string, string>>>('/profile-fields/me')
  },
  updateMyValues(values: Record<string, string>) {
    return client.put<ApiResponse<Record<string, string>>>('/profile-fields/me', values)
  },

  // Admin endpoints
  listAllDefinitions() {
    return client.get<ApiResponse<ProfileFieldDefinition[]>>('/admin/profile-fields')
  },
  createDefinition(data: CreateProfileFieldRequest) {
    return client.post<ApiResponse<ProfileFieldDefinition>>('/admin/profile-fields', data)
  },
  updateDefinition(id: string, data: UpdateProfileFieldRequest) {
    return client.put<ApiResponse<ProfileFieldDefinition>>(`/admin/profile-fields/${id}`, data)
  },
  deleteDefinition(id: string) {
    return client.delete<ApiResponse<void>>(`/admin/profile-fields/${id}`)
  },
}
