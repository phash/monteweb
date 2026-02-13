import client from './client'
import type { ApiResponse } from '@/types/api'
import type { UserInfo } from '@/types/user'
import type { RoomInfo } from '@/types/room'

export interface SectionInfo {
  id: string
  name: string
  slug: string
  description: string
  sortOrder: number
  active: boolean
}

export interface CreateSectionRoomRequest {
  name: string
  description?: string
  type: string
  sectionId: string
}

export const sectionAdminApi = {
  getMySections() {
    return client.get<ApiResponse<SectionInfo[]>>('/section-admin/my-sections')
  },

  getSectionUsers(sectionId: string) {
    return client.get<ApiResponse<UserInfo[]>>(`/section-admin/sections/${sectionId}/users`)
  },

  getSectionRooms(sectionId: string) {
    return client.get<ApiResponse<RoomInfo[]>>(`/section-admin/sections/${sectionId}/rooms`)
  },

  assignSpecialRole(userId: string, role: string) {
    return client.post<ApiResponse<UserInfo>>(`/section-admin/users/${userId}/special-roles`, { role })
  },

  removeSpecialRole(userId: string, role: string) {
    return client.delete<ApiResponse<UserInfo>>(`/section-admin/users/${userId}/special-roles/${role}`)
  },

  createRoom(data: CreateSectionRoomRequest) {
    return client.post<ApiResponse<RoomInfo>>('/section-admin/rooms', data)
  },
}
