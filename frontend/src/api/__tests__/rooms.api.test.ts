import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { data: {} } }),
    post: vi.fn().mockResolvedValue({ data: { data: {} } }),
    put: vi.fn().mockResolvedValue({ data: { data: {} } }),
    delete: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

import client from '../client'
import { roomsApi } from '../rooms.api'

describe('roomsApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getMine', () => {
    it('should GET /rooms/mine', async () => {
      await roomsApi.getMine()
      expect(client.get).toHaveBeenCalledWith('/rooms/mine')
    })
  })

  describe('getAll', () => {
    it('should GET /rooms with params', async () => {
      await roomsApi.getAll({ page: 1, size: 10 })
      expect(client.get).toHaveBeenCalledWith('/rooms', { params: { page: 1, size: 10 } })
    })
  })

  describe('discover', () => {
    it('should GET /rooms/discover with search query', async () => {
      await roomsApi.discover({ q: 'science', page: 0, size: 20 })
      expect(client.get).toHaveBeenCalledWith('/rooms/discover', {
        params: { q: 'science', page: 0, size: 20 },
      })
    })
  })

  describe('getById', () => {
    it('should GET /rooms/{id}', async () => {
      await roomsApi.getById('room-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1')
    })
  })

  describe('create', () => {
    it('should POST /rooms with data', async () => {
      const data = { name: 'Test Room', type: 'CLASS', sectionId: 's-1' }
      await roomsApi.create(data as any)
      expect(client.post).toHaveBeenCalledWith('/rooms', data)
    })
  })

  describe('update', () => {
    it('should PUT /rooms/{id}', async () => {
      const data = { name: 'Updated Room', description: 'New desc' }
      await roomsApi.update('room-1', data)
      expect(client.put).toHaveBeenCalledWith('/rooms/room-1', data)
    })
  })

  describe('toggleArchive', () => {
    it('should PUT /rooms/{id}/archive', async () => {
      await roomsApi.toggleArchive('room-1')
      expect(client.put).toHaveBeenCalledWith('/rooms/room-1/archive')
    })
  })

  describe('deleteRoom', () => {
    it('should DELETE /rooms/{id}', async () => {
      await roomsApi.deleteRoom('room-1')
      expect(client.delete).toHaveBeenCalledWith('/rooms/room-1')
    })
  })

  describe('members', () => {
    it('should POST /rooms/{id}/members to add member', async () => {
      await roomsApi.addMember('room-1', 'user-1', 'MEMBER' as any)
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/members', { userId: 'user-1', role: 'MEMBER' })
    })

    it('should DELETE /rooms/{id}/members/{userId} to remove member', async () => {
      await roomsApi.removeMember('room-1', 'user-1')
      expect(client.delete).toHaveBeenCalledWith('/rooms/room-1/members/user-1')
    })

    it('should PUT /rooms/{id}/members/{userId}/role to update role', async () => {
      await roomsApi.updateMemberRole('room-1', 'user-1', 'LEADER' as any)
      expect(client.put).toHaveBeenCalledWith('/rooms/room-1/members/user-1/role', { role: 'LEADER' })
    })
  })

  describe('join requests', () => {
    it('should POST /rooms/{id}/join-request', async () => {
      await roomsApi.requestJoin('room-1', 'Please let me in')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/join-request', { message: 'Please let me in' })
    })

    it('should GET /rooms/{id}/join-requests', async () => {
      await roomsApi.getJoinRequests('room-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/join-requests')
    })

    it('should POST approve join request', async () => {
      await roomsApi.approveJoinRequest('room-1', 'req-1')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/join-requests/req-1/approve')
    })

    it('should POST deny join request', async () => {
      await roomsApi.denyJoinRequest('room-1', 'req-1')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/join-requests/req-1/deny')
    })

    it('should GET /rooms/my-join-requests', async () => {
      await roomsApi.getMyJoinRequests()
      expect(client.get).toHaveBeenCalledWith('/rooms/my-join-requests')
    })
  })

  describe('mute/unmute', () => {
    it('should POST /rooms/{id}/mute', async () => {
      await roomsApi.muteRoom('room-1')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/mute')
    })

    it('should POST /rooms/{id}/unmute', async () => {
      await roomsApi.unmuteRoom('room-1')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/unmute')
    })
  })

  describe('joinRoom / leaveRoom', () => {
    it('should POST /rooms/{id}/join', async () => {
      await roomsApi.joinRoom('room-1')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/join')
    })

    it('should POST /rooms/{id}/leave', async () => {
      await roomsApi.leaveRoom('room-1')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/leave')
    })
  })

  describe('chat channels', () => {
    it('should GET /rooms/{id}/chat/channels', async () => {
      await roomsApi.getChatChannels('room-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/chat/channels')
    })

    it('should POST /rooms/{id}/chat/channels', async () => {
      await roomsApi.getOrCreateChatChannel('room-1', 'MAIN')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/chat/channels', { channelType: 'MAIN' })
    })
  })
})
