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

describe('Rooms Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useRoomsStore()
    expect(store.myRooms).toEqual([])
    expect(store.currentRoom).toBeNull()
    expect(store.discoverableRooms).toEqual([])
    expect(store.chatChannels).toEqual([])
    expect(store.loading).toBe(false)
  })

  it('should fetch my rooms', async () => {
    const store = useRoomsStore()
    const mockRooms = [
      { id: 'r1', name: 'Room 1' },
      { id: 'r2', name: 'Room 2' },
    ]

    vi.mocked(roomsApi.getMine).mockResolvedValue({
      data: { data: mockRooms },
    } as any)

    await store.fetchMyRooms()

    expect(store.myRooms).toHaveLength(2)
    expect(store.myRooms[0].id).toBe('r1')
  })

  it('should fetch single room', async () => {
    const store = useRoomsStore()
    const mockRoom = { id: 'r1', name: 'Room 1', members: [] }

    vi.mocked(roomsApi.getById).mockResolvedValue({
      data: { data: mockRoom },
    } as any)

    await store.fetchRoom('r1')

    expect(store.currentRoom).toEqual(mockRoom)
  })

  it('should create room and append to myRooms', async () => {
    const store = useRoomsStore()
    const newRoom = { id: 'r-new', name: 'New Room' }

    vi.mocked(roomsApi.create).mockResolvedValue({
      data: { data: newRoom },
    } as any)

    const result = await store.createRoom({ name: 'New Room' } as any)

    expect(result.id).toBe('r-new')
    expect(store.myRooms).toHaveLength(1)
    expect(store.myRooms[0].name).toBe('New Room')
  })

  it('should create interest room and append to myRooms', async () => {
    const store = useRoomsStore()
    const newRoom = { id: 'ir-1', name: 'Interest Room' }

    vi.mocked(roomsApi.createInterestRoom).mockResolvedValue({
      data: { data: newRoom },
    } as any)

    const result = await store.createInterestRoom({ name: 'Interest Room' } as any)

    expect(result.id).toBe('ir-1')
    expect(store.myRooms).toHaveLength(1)
  })

  it('should discover rooms with pagination', async () => {
    const store = useRoomsStore()
    const mockRooms = [{ id: 'r1' }, { id: 'r2' }]

    vi.mocked(roomsApi.discover).mockResolvedValue({
      data: { data: { content: mockRooms, totalPages: 3 } },
    } as any)

    await store.discoverRooms('music', 1)

    expect(store.discoverableRooms).toHaveLength(2)
    expect(store.discoverTotalPages).toBe(3)
    expect(store.discoverPage).toBe(1)
  })

  it('should fetch chat channels', async () => {
    const store = useRoomsStore()
    const mockChannels = [{ id: 'ch1', type: 'MAIN' }]

    vi.mocked(roomsApi.getChatChannels).mockResolvedValue({
      data: { data: mockChannels },
    } as any)

    await store.fetchChatChannels('r1')

    expect(store.chatChannels).toHaveLength(1)
    expect(store.chatChannels[0].id).toBe('ch1')
  })

  it('should set loading during fetch and reset after', async () => {
    const store = useRoomsStore()

    let resolvePromise: Function
    vi.mocked(roomsApi.getMine).mockReturnValue(
      new Promise((resolve) => { resolvePromise = resolve }) as any
    )

    const promise = store.fetchMyRooms()
    expect(store.loading).toBe(true)

    resolvePromise!({ data: { data: [] } })
    await promise

    expect(store.loading).toBe(false)
  })
})
