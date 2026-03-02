import { defineStore } from 'pinia'
import { ref } from 'vue'
import { parentLetterApi } from '@/api/parentletter.api'
import type {
  ParentLetterInfo,
  ParentLetterDetailInfo,
  ParentLetterConfigInfo,
  CreateParentLetterRequest,
  UpdateParentLetterRequest,
  UpdateParentLetterConfigRequest,
} from '@/types/parentletter'

export const useParentLetterStore = defineStore('parentletter', () => {
  const letters = ref<ParentLetterInfo[]>([])
  const inbox = ref<ParentLetterInfo[]>([])
  const currentLetter = ref<ParentLetterDetailInfo | null>(null)
  const config = ref<ParentLetterConfigInfo | null>(null)
  const loading = ref(false)
  const total = ref(0)
  const inboxTotal = ref(0)

  async function fetchMyLetters(page = 0, size = 20) {
    loading.value = true
    try {
      const res = await parentLetterApi.getMyLetters(page, size)
      letters.value = res.data.data.content
      total.value = res.data.data.totalElements
    } finally {
      loading.value = false
    }
  }

  async function fetchInbox(page = 0, size = 20) {
    loading.value = true
    try {
      const res = await parentLetterApi.getInbox(page, size)
      inbox.value = res.data.data.content
      inboxTotal.value = res.data.data.totalElements
    } finally {
      loading.value = false
    }
  }

  async function fetchLetter(id: string) {
    loading.value = true
    currentLetter.value = null
    try {
      const res = await parentLetterApi.getLetter(id)
      currentLetter.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function createLetter(data: CreateParentLetterRequest) {
    const res = await parentLetterApi.createLetter(data)
    currentLetter.value = res.data.data
    // Prepend summary to list
    const detail = res.data.data
    const summary: ParentLetterInfo = {
      id: detail.id,
      title: detail.title,
      status: detail.status,
      roomId: detail.roomId,
      roomName: detail.roomName,
      createdBy: detail.createdBy,
      creatorName: detail.creatorName,
      sendDate: detail.sendDate,
      deadline: detail.deadline,
      totalRecipients: detail.totalRecipients,
      confirmedCount: detail.confirmedCount,
      createdAt: detail.createdAt,
      updatedAt: detail.updatedAt,
    }
    letters.value.unshift(summary)
    return res.data.data
  }

  async function updateLetter(id: string, data: UpdateParentLetterRequest) {
    const res = await parentLetterApi.updateLetter(id, data)
    currentLetter.value = res.data.data
    _syncListItem(res.data.data)
    return res.data.data
  }

  async function sendLetter(id: string) {
    const res = await parentLetterApi.sendLetter(id)
    currentLetter.value = res.data.data
    _syncListItem(res.data.data)
    return res.data.data
  }

  async function closeLetter(id: string) {
    const res = await parentLetterApi.closeLetter(id)
    currentLetter.value = res.data.data
    _syncListItem(res.data.data)
    return res.data.data
  }

  async function deleteLetter(id: string) {
    await parentLetterApi.deleteLetter(id)
    letters.value = letters.value.filter(l => l.id !== id)
    if (currentLetter.value?.id === id) currentLetter.value = null
  }

  async function confirmLetter(id: string, studentId: string) {
    await parentLetterApi.confirmLetter(id, studentId)
    // Refresh the current detail to get updated recipient status
    if (currentLetter.value?.id === id) {
      await fetchLetter(id)
    }
    // Update inbox item status
    const idx = inbox.value.findIndex(l => l.id === id)
    if (idx !== -1) {
      const item = inbox.value[idx]!
      inbox.value[idx] = { ...item, confirmedCount: item.confirmedCount + 1 }
    }
  }

  async function markAsRead(id: string) {
    await parentLetterApi.markAsRead(id)
  }

  async function fetchConfig(sectionId?: string) {
    try {
      const res = await parentLetterApi.getConfig(sectionId)
      config.value = res.data.data
    } catch {
      config.value = null
    }
  }

  async function updateConfig(data: UpdateParentLetterConfigRequest, sectionId?: string) {
    const res = await parentLetterApi.updateConfig(data, sectionId)
    config.value = res.data.data
    return res.data.data
  }

  // Internal helper: sync updated detail back into the letters list
  function _syncListItem(detail: ParentLetterDetailInfo) {
    const idx = letters.value.findIndex(l => l.id === detail.id)
    if (idx !== -1) {
      letters.value[idx] = {
        id: detail.id,
        title: detail.title,
        status: detail.status,
        roomId: detail.roomId,
        roomName: detail.roomName,
        createdBy: detail.createdBy,
        creatorName: detail.creatorName,
        sendDate: detail.sendDate,
        deadline: detail.deadline,
        totalRecipients: detail.totalRecipients,
        confirmedCount: detail.confirmedCount,
        createdAt: detail.createdAt,
        updatedAt: detail.updatedAt,
      }
    }
  }

  return {
    letters,
    inbox,
    currentLetter,
    config,
    loading,
    total,
    inboxTotal,
    fetchMyLetters,
    fetchInbox,
    fetchLetter,
    createLetter,
    updateLetter,
    sendLetter,
    closeLetter,
    deleteLetter,
    confirmLetter,
    markAsRead,
    fetchConfig,
    updateConfig,
  }
})
