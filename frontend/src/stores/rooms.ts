import { defineStore } from 'pinia'
import { ref } from 'vue'
import { roomsApi } from '@/api/rooms.api'
import type { RoomInfo, RoomDetail, CreateRoomRequest, CreateInterestRoomRequest, RoomChatChannelInfo } from '@/types/room'

export const useRoomsStore = defineStore('rooms', () => {
  const myRooms = ref<RoomInfo[]>([])
  const currentRoom = ref<RoomDetail | null>(null)
  const discoverableRooms = ref<RoomInfo[]>([])
  const discoverTotalPages = ref(0)
  const discoverPage = ref(0)
  const chatChannels = ref<RoomChatChannelInfo[]>([])
  const loading = ref(false)

  async function fetchMyRooms() {
    loading.value = true
    try {
      const res = await roomsApi.getMine()
      myRooms.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function fetchRoom(id: string) {
    loading.value = true
    try {
      const res = await roomsApi.getById(id)
      currentRoom.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function createRoom(data: CreateRoomRequest) {
    const res = await roomsApi.create(data)
    myRooms.value.push(res.data.data)
    return res.data.data
  }

  async function createInterestRoom(data: CreateInterestRoomRequest) {
    const res = await roomsApi.createInterestRoom(data)
    myRooms.value.push(res.data.data)
    return res.data.data
  }

  async function discoverRooms(query?: string, page = 0) {
    loading.value = true
    try {
      const res = await roomsApi.discover({ q: query, page, size: 20 })
      discoverableRooms.value = res.data.data.content
      discoverTotalPages.value = res.data.data.totalPages
      discoverPage.value = page
    } finally {
      loading.value = false
    }
  }

  async function joinRoom(roomId: string) {
    await roomsApi.joinRoom(roomId)
  }

  async function leaveRoom(roomId: string) {
    await roomsApi.leaveRoom(roomId)
  }

  async function fetchChatChannels(roomId: string) {
    const res = await roomsApi.getChatChannels(roomId)
    chatChannels.value = res.data.data
  }

  async function getOrCreateChatChannel(roomId: string, channelType = 'MAIN') {
    const res = await roomsApi.getOrCreateChatChannel(roomId, channelType)
    return res.data.data
  }

  return {
    myRooms, currentRoom, discoverableRooms, discoverTotalPages, discoverPage,
    chatChannels, loading,
    fetchMyRooms, fetchRoom, createRoom, createInterestRoom,
    discoverRooms, joinRoom, leaveRoom,
    fetchChatChannels, getOrCreateChatChannel,
  }
})
