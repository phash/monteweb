import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useRoomsStore } from '@/stores/rooms'

vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getMine: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    createInterestRoom: vi.fn(),
    discover: vi.fn(),
    joinRoom: vi.fn(),
    leaveRoom: vi.fn(),
    muteRoom: vi.fn(),
    unmuteRoom: vi.fn(),
    getChatChannels: vi.fn(),
    getOrCreateChatChannel: vi.fn(),
  },
}))

import { roomsApi } from '@/api/rooms.api'

describe('Rooms Store - Extended', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('fetchRoom branching', () => {
    it('should set currentPublicRoom for rooms without members', async () => {
      const store = useRoomsStore()
      const publicRoom = {
        id: 'r2',
        name: 'Public Room',
        publicDescription: 'A public room',
      }

      vi.mocked(roomsApi.getById).mockResolvedValue({
        data: { data: publicRoom },
      } as any)

      await store.fetchRoom('r2')

      expect(store.currentRoom).toBeNull()
      expect(store.currentPublicRoom).not.toBeNull()
      expect(store.currentPublicRoom!.id).toBe('r2')
    })

    it('should clear both rooms on API error', async () => {
      const store = useRoomsStore()
      store.currentRoom = { id: 'old' } as any
      store.currentPublicRoom = { id: 'old-pub' } as any

      vi.mocked(roomsApi.getById).mockRejectedValue(new Error('404'))

      await store.fetchRoom('bad-id')

      expect(store.currentRoom).toBeNull()
      expect(store.currentPublicRoom).toBeNull()
      expect(store.loading).toBe(false)
    })

    it('should reset loading even on error in fetchMyRooms', async () => {
      const store = useRoomsStore()
      vi.mocked(roomsApi.getMine).mockRejectedValue(new Error('Network error'))

      try {
        await store.fetchMyRooms()
      } catch {
        // Expected
      }

      expect(store.loading).toBe(false)
    })
  })

  describe('discoverRooms error handling', () => {
    it('should clear discoverableRooms on error', async () => {
      const store = useRoomsStore()
      store.discoverableRooms = [{ id: 'old' }] as any

      vi.mocked(roomsApi.discover).mockRejectedValue(new Error('Error'))

      await store.discoverRooms('fail')

      expect(store.discoverableRooms).toEqual([])
      expect(store.loading).toBe(false)
    })

    it('should use default page 0 when not specified', async () => {
      const store = useRoomsStore()

      vi.mocked(roomsApi.discover).mockResolvedValue({
        data: { data: { content: [], totalPages: 0 } },
      } as any)

      await store.discoverRooms('query')

      expect(roomsApi.discover).toHaveBeenCalledWith({ q: 'query', page: 0, size: 20 })
    })
  })

  describe('joinRoom / leaveRoom', () => {
    it('should call joinRoom API with correct roomId', async () => {
      const store = useRoomsStore()
      vi.mocked(roomsApi.joinRoom).mockResolvedValue({} as any)

      await store.joinRoom('room-abc')
      expect(roomsApi.joinRoom).toHaveBeenCalledWith('room-abc')
    })

    it('should call leaveRoom API with correct roomId', async () => {
      const store = useRoomsStore()
      vi.mocked(roomsApi.leaveRoom).mockResolvedValue({} as any)

      await store.leaveRoom('room-abc')
      expect(roomsApi.leaveRoom).toHaveBeenCalledWith('room-abc')
    })
  })

  describe('mute / unmute', () => {
    it('should call muteRoom API', async () => {
      const store = useRoomsStore()
      vi.mocked(roomsApi.muteRoom).mockResolvedValue({} as any)

      await store.muteRoom('room-1')
      expect(roomsApi.muteRoom).toHaveBeenCalledWith('room-1')
    })

    it('should call unmuteRoom API', async () => {
      const store = useRoomsStore()
      vi.mocked(roomsApi.unmuteRoom).mockResolvedValue({} as any)

      await store.unmuteRoom('room-1')
      expect(roomsApi.unmuteRoom).toHaveBeenCalledWith('room-1')
    })
  })

  describe('getOrCreateChatChannel', () => {
    it('should return the created/existing channel', async () => {
      const store = useRoomsStore()
      const channel = { id: 'ch-new', type: 'MAIN' }

      vi.mocked(roomsApi.getOrCreateChatChannel).mockResolvedValue({
        data: { data: channel },
      } as any)

      const result = await store.getOrCreateChatChannel('room-1', 'MAIN')

      expect(result.id).toBe('ch-new')
    })

    it('should use default channel type MAIN', async () => {
      const store = useRoomsStore()

      vi.mocked(roomsApi.getOrCreateChatChannel).mockResolvedValue({
        data: { data: { id: 'ch-1' } },
      } as any)

      await store.getOrCreateChatChannel('room-1')

      expect(roomsApi.getOrCreateChatChannel).toHaveBeenCalledWith('room-1', 'MAIN')
    })
  })
})
