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
    getChatChannels: vi.fn(),
    getOrCreateChatChannel: vi.fn(),
    joinRoom: vi.fn(),
    leaveRoom: vi.fn(),
    muteRoom: vi.fn(),
    unmuteRoom: vi.fn(),
  },
}))

import { roomsApi } from '@/api/rooms.api'

describe('Rooms Store – Role Concept Features', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  // ── muteRoom / unmuteRoom ─────────────────────────────────────────

  describe('muteRoom', () => {
    it('should call roomsApi.muteRoom with the room id', async () => {
      const store = useRoomsStore()
      vi.mocked(roomsApi.muteRoom).mockResolvedValue({} as any)

      await store.muteRoom('room-1')

      expect(roomsApi.muteRoom).toHaveBeenCalledWith('room-1')
      expect(roomsApi.muteRoom).toHaveBeenCalledTimes(1)
    })

    it('should propagate errors from roomsApi.muteRoom', async () => {
      const store = useRoomsStore()
      vi.mocked(roomsApi.muteRoom).mockRejectedValue(new Error('Network error'))

      await expect(store.muteRoom('room-1')).rejects.toThrow('Network error')
    })
  })

  describe('unmuteRoom', () => {
    it('should call roomsApi.unmuteRoom with the room id', async () => {
      const store = useRoomsStore()
      vi.mocked(roomsApi.unmuteRoom).mockResolvedValue({} as any)

      await store.unmuteRoom('room-2')

      expect(roomsApi.unmuteRoom).toHaveBeenCalledWith('room-2')
      expect(roomsApi.unmuteRoom).toHaveBeenCalledTimes(1)
    })

    it('should propagate errors from roomsApi.unmuteRoom', async () => {
      const store = useRoomsStore()
      vi.mocked(roomsApi.unmuteRoom).mockRejectedValue(new Error('Forbidden'))

      await expect(store.unmuteRoom('room-2')).rejects.toThrow('Forbidden')
    })
  })

  // ── fetchRoom with joinPolicy ─────────────────────────────────────

  describe('fetchRoom with joinPolicy', () => {
    it('should set currentRoom for member view with joinPolicy', async () => {
      const store = useRoomsStore()
      const mockRoom = {
        id: 'r1',
        name: 'Open Room',
        joinPolicy: 'OPEN',
        members: [{ userId: 'u1', displayName: 'User', role: 'LEADER' }],
        settings: {
          chatEnabled: false,
          filesEnabled: false,
          parentSpaceEnabled: false,
          visibility: 'MEMBERS_ONLY',
          discussionMode: 'FULL',
          allowMemberThreadCreation: false,
          childDiscussionEnabled: false,
        },
      }

      vi.mocked(roomsApi.getById).mockResolvedValue({
        data: { data: mockRoom },
      } as any)

      await store.fetchRoom('r1')

      expect(store.currentRoom).not.toBeNull()
      expect(store.currentRoom!.joinPolicy).toBe('OPEN')
    })

    it('should set currentPublicRoom for non-member view with joinPolicy', async () => {
      const store = useRoomsStore()
      const mockPublicRoom = {
        id: 'r2',
        name: 'Request Room',
        joinPolicy: 'REQUEST',
        memberCount: 10,
        publicDescription: 'A room you need to request access to',
      }

      vi.mocked(roomsApi.getById).mockResolvedValue({
        data: { data: mockPublicRoom },
      } as any)

      await store.fetchRoom('r2')

      expect(store.currentRoom).toBeNull()
      expect(store.currentPublicRoom).not.toBeNull()
      expect(store.currentPublicRoom!.joinPolicy).toBe('REQUEST')
    })

    it('should handle INVITE_ONLY joinPolicy in public view', async () => {
      const store = useRoomsStore()
      const mockPublicRoom = {
        id: 'r3',
        name: 'Invite Only Room',
        joinPolicy: 'INVITE_ONLY',
        memberCount: 5,
      }

      vi.mocked(roomsApi.getById).mockResolvedValue({
        data: { data: mockPublicRoom },
      } as any)

      await store.fetchRoom('r3')

      expect(store.currentPublicRoom).not.toBeNull()
      expect(store.currentPublicRoom!.joinPolicy).toBe('INVITE_ONLY')
    })
  })

  // ── Room settings with discussion mode ────────────────────────────

  describe('room settings with discussion mode', () => {
    it('should preserve discussionMode in room settings', async () => {
      const store = useRoomsStore()
      const mockRoom = {
        id: 'r1',
        name: 'Announcements Room',
        members: [],
        settings: {
          chatEnabled: true,
          filesEnabled: false,
          parentSpaceEnabled: false,
          visibility: 'MEMBERS_ONLY',
          discussionMode: 'ANNOUNCEMENTS_ONLY',
          allowMemberThreadCreation: true,
          childDiscussionEnabled: false,
        },
      }

      vi.mocked(roomsApi.getById).mockResolvedValue({
        data: { data: mockRoom },
      } as any)

      await store.fetchRoom('r1')

      expect(store.currentRoom?.settings?.discussionMode).toBe('ANNOUNCEMENTS_ONLY')
      expect(store.currentRoom?.settings?.allowMemberThreadCreation).toBe(true)
      expect(store.currentRoom?.settings?.childDiscussionEnabled).toBe(false)
    })

    it('should handle DISABLED discussion mode', async () => {
      const store = useRoomsStore()
      const mockRoom = {
        id: 'r2',
        name: 'No Discussion Room',
        members: [],
        settings: {
          chatEnabled: false,
          filesEnabled: false,
          parentSpaceEnabled: false,
          visibility: 'MEMBERS_ONLY',
          discussionMode: 'DISABLED',
          allowMemberThreadCreation: false,
          childDiscussionEnabled: false,
        },
      }

      vi.mocked(roomsApi.getById).mockResolvedValue({
        data: { data: mockRoom },
      } as any)

      await store.fetchRoom('r2')

      expect(store.currentRoom?.settings?.discussionMode).toBe('DISABLED')
    })
  })

  // ── Discover rooms with joinPolicy ────────────────────────────────

  describe('discoverRooms with joinPolicy', () => {
    it('should load discoverable rooms with OPEN joinPolicy', async () => {
      const store = useRoomsStore()
      const mockRooms = [
        { id: 'r1', name: 'Open Room', joinPolicy: 'OPEN', memberCount: 10 },
        { id: 'r2', name: 'Another Open', joinPolicy: 'OPEN', memberCount: 5 },
      ]

      vi.mocked(roomsApi.discover).mockResolvedValue({
        data: { data: { content: mockRooms, totalPages: 1 } },
      } as any)

      await store.discoverRooms()

      expect(store.discoverableRooms).toHaveLength(2)
      expect(store.discoverableRooms[0].joinPolicy).toBe('OPEN')
      expect(store.discoverableRooms[1].joinPolicy).toBe('OPEN')
    })
  })
})
