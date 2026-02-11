import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  RoomInfo, RoomDetail, CreateRoomRequest, CreateInterestRoomRequest,
  RoomSettings, RoomRole, RoomChatChannelInfo
} from '@/types/room'

export const roomsApi = {
  getMine() {
    return client.get<ApiResponse<RoomInfo[]>>('/rooms/mine')
  },

  getAll(params?: { page?: number; size?: number }) {
    return client.get<ApiResponse<PageResponse<RoomInfo>>>('/rooms', { params })
  },

  // Interest rooms: browse & search
  discover(params?: { q?: string; page?: number; size?: number }) {
    return client.get<ApiResponse<PageResponse<RoomInfo>>>('/rooms/discover', { params })
  },

  getById(id: string) {
    return client.get<ApiResponse<RoomDetail>>(`/rooms/${id}`)
  },

  create(data: CreateRoomRequest) {
    return client.post<ApiResponse<RoomInfo>>('/rooms', data)
  },

  createInterestRoom(data: CreateInterestRoomRequest) {
    return client.post<ApiResponse<RoomInfo>>('/rooms/interest', data)
  },

  update(id: string, data: { name?: string; description?: string; publicDescription?: string }) {
    return client.put<ApiResponse<RoomInfo>>(`/rooms/${id}`, data)
  },

  updateSettings(id: string, settings: RoomSettings) {
    return client.put<ApiResponse<RoomInfo>>(`/rooms/${id}/settings`, settings)
  },

  updateInterestFields(id: string, data: { tags?: string[]; discoverable?: boolean; expiresAt?: string }) {
    return client.put<ApiResponse<RoomInfo>>(`/rooms/${id}/interest`, data)
  },

  uploadAvatar(roomId: string, file: File) {
    const form = new FormData()
    form.append('file', file)
    return client.post<ApiResponse<void>>(`/rooms/${roomId}/avatar`, form)
  },

  removeAvatar(roomId: string) {
    return client.delete<ApiResponse<void>>(`/rooms/${roomId}/avatar`)
  },

  // Join / Leave
  joinRoom(id: string) {
    return client.post<ApiResponse<void>>(`/rooms/${id}/join`)
  },

  leaveRoom(id: string) {
    return client.post<ApiResponse<void>>(`/rooms/${id}/leave`)
  },

  // Members
  addMember(roomId: string, userId: string, role: RoomRole) {
    return client.post<ApiResponse<void>>(`/rooms/${roomId}/members`, { userId, role })
  },

  removeMember(roomId: string, userId: string) {
    return client.delete<ApiResponse<void>>(`/rooms/${roomId}/members/${userId}`)
  },

  updateMemberRole(roomId: string, userId: string, role: RoomRole) {
    return client.put<ApiResponse<void>>(`/rooms/${roomId}/members/${userId}/role`, { role })
  },

  // Room Chat
  getChatChannels(roomId: string) {
    return client.get<ApiResponse<RoomChatChannelInfo[]>>(`/rooms/${roomId}/chat/channels`)
  },

  getOrCreateChatChannel(roomId: string, channelType = 'MAIN') {
    return client.post<ApiResponse<RoomChatChannelInfo>>(`/rooms/${roomId}/chat/channels`, { channelType })
  },
}
