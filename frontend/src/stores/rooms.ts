import { defineStore } from 'pinia'
import { ref } from 'vue'
import { roomsApi } from '@/api/rooms.api'
import type { RoomInfo, RoomDetail, CreateRoomRequest, CreateInterestRoomRequest, RoomChatChannelInfo, RoomPublicInfo } from '@/types/room'

export const useRoomsStore = defineStore('rooms', () => {
  const myRooms = ref<RoomInfo[]>([])
  const currentRoom = ref<RoomDetail | null>(null)
  const currentPublicRoom = ref<RoomPublicInfo | null>(null)
  const discoverableRooms = ref<RoomInfo[]>([])
  const discoverTotalPages = ref(0)
  const discoverPage = ref(0)
  const chatChannels = ref<RoomChatChannelInfo[]>([])
  const loading = ref(false)
  const loadingRoom = ref(false)

  async function fetchMyRooms() {
    loading.value = true
    try {
      const res = await roomsApi.getMine()
      myRooms.value = res.data.data
    } catch (e) {
      console.error('Failed to fetch rooms:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchRoom(id: string) {
    loadingRoom.value = true
    currentRoom.value = null
    currentPublicRoom.value = null
    try {
      const res = await roomsApi.getById(id)
      const data = res.data.data
      if ('members' in data) {
        currentRoom.value = data
        currentPublicRoom.value = null
      } else {
        currentRoom.value = null
        currentPublicRoom.value = data
      }
    } catch (e) {
      currentRoom.value = null
      currentPublicRoom.value = null
      console.error('Failed to fetch room:', e)
      throw e
    } finally {
      loadingRoom.value = false
    }
  }

  async function createRoom(data: CreateRoomRequest) {
    try {
      const res = await roomsApi.create(data)
      myRooms.value.push(res.data.data)
      return res.data.data
    } catch (e) {
      console.error('Failed to create room:', e)
      throw e
    }
  }

  async function createInterestRoom(data: CreateInterestRoomRequest) {
    try {
      const res = await roomsApi.createInterestRoom(data)
      myRooms.value.push(res.data.data)
      return res.data.data
    } catch (e) {
      console.error('Failed to create interest room:', e)
      throw e
    }
  }

  async function discoverRooms(query?: string, page = 0) {
    loading.value = true
    try {
      const res = await roomsApi.discover({ q: query, page, size: 20 })
      discoverableRooms.value = res.data.data.content
      discoverTotalPages.value = res.data.data.totalPages
      discoverPage.value = page
    } catch (e) {
      discoverableRooms.value = []
      console.error('Failed to discover rooms:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function joinRoom(roomId: string) {
    try {
      await roomsApi.joinRoom(roomId)
    } catch (e) {
      console.error('Failed to join room:', e)
      throw e
    }
  }

  async function leaveRoom(roomId: string) {
    try {
      await roomsApi.leaveRoom(roomId)
    } catch (e) {
      console.error('Failed to leave room:', e)
      throw e
    }
  }

  async function muteRoom(roomId: string) {
    try {
      await roomsApi.muteRoom(roomId)
    } catch (e) {
      console.error('Failed to mute room:', e)
      throw e
    }
  }

  async function unmuteRoom(roomId: string) {
    try {
      await roomsApi.unmuteRoom(roomId)
    } catch (e) {
      console.error('Failed to unmute room:', e)
      throw e
    }
  }

  async function fetchChatChannels(roomId: string) {
    try {
      const res = await roomsApi.getChatChannels(roomId)
      chatChannels.value = res.data.data
    } catch (e) {
      console.error('Failed to fetch chat channels:', e)
      throw e
    }
  }

  async function getOrCreateChatChannel(roomId: string, channelType = 'MAIN') {
    try {
      const res = await roomsApi.getOrCreateChatChannel(roomId, channelType)
      return res.data.data
    } catch (e) {
      console.error('Failed to get/create chat channel:', e)
      throw e
    }
  }

  return {
    myRooms, currentRoom, currentPublicRoom, discoverableRooms, discoverTotalPages, discoverPage,
    chatChannels, loading, loadingRoom,
    fetchMyRooms, fetchRoom, createRoom, createInterestRoom,
    discoverRooms, joinRoom, leaveRoom, muteRoom, unmuteRoom,
    fetchChatChannels, getOrCreateChatChannel,
  }
})
