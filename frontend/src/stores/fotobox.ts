import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fotoboxApi } from '@/api/fotobox.api'
import type {
  FotoboxThreadInfo,
  FotoboxImageInfo,
  FotoboxRoomSettings,
  CreateFotoboxThreadRequest,
} from '@/types/fotobox'

export const useFotoboxStore = defineStore('fotobox', () => {
  const threads = ref<FotoboxThreadInfo[]>([])
  const currentThread = ref<FotoboxThreadInfo | null>(null)
  const images = ref<FotoboxImageInfo[]>([])
  const settings = ref<FotoboxRoomSettings | null>(null)
  const loading = ref(false)

  async function fetchSettings(roomId: string) {
    try {
      const res = await fotoboxApi.getSettings(roomId)
      settings.value = res.data.data
    } catch {
      settings.value = null
    }
  }

  async function updateSettings(roomId: string, data: Partial<FotoboxRoomSettings>) {
    try {
      const res = await fotoboxApi.updateSettings(roomId, data)
      settings.value = res.data.data
    } catch (e) {
      console.error('Failed to update fotobox settings:', e)
      throw e
    }
  }

  async function fetchThreads(roomId: string) {
    loading.value = true
    try {
      const res = await fotoboxApi.getThreads(roomId)
      threads.value = res.data.data
    } catch (e) {
      console.error('Failed to fetch fotobox threads:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchThread(roomId: string, threadId: string) {
    try {
      const res = await fotoboxApi.getThread(roomId, threadId)
      currentThread.value = res.data.data
    } catch (e) {
      console.error('Failed to fetch fotobox thread:', e)
      throw e
    }
  }

  async function fetchImages(roomId: string, threadId: string) {
    loading.value = true
    try {
      const res = await fotoboxApi.getThreadImages(roomId, threadId)
      images.value = res.data.data
    } catch (e) {
      console.error('Failed to fetch fotobox images:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function createThread(roomId: string, data: CreateFotoboxThreadRequest) {
    try {
      const res = await fotoboxApi.createThread(roomId, data)
      threads.value.unshift(res.data.data)
      return res.data.data
    } catch (e) {
      console.error('Failed to create fotobox thread:', e)
      throw e
    }
  }

  async function deleteThread(roomId: string, threadId: string) {
    try {
      await fotoboxApi.deleteThread(roomId, threadId)
      threads.value = threads.value.filter((t) => t.id !== threadId)
      if (currentThread.value?.id === threadId) currentThread.value = null
    } catch (e) {
      console.error('Failed to delete fotobox thread:', e)
      throw e
    }
  }

  async function uploadImages(roomId: string, threadId: string, files: File[], caption?: string) {
    try {
      const res = await fotoboxApi.uploadImages(roomId, threadId, files, caption)
      images.value.push(...res.data.data)
      // Update thread image count
      const thread = threads.value.find((t) => t.id === threadId)
      if (thread) thread.imageCount += res.data.data.length
      if (currentThread.value?.id === threadId)
        currentThread.value.imageCount += res.data.data.length
      return res.data.data
    } catch (e) {
      console.error('Failed to upload fotobox images:', e)
      throw e
    }
  }

  async function deleteImage(imageId: string) {
    try {
      await fotoboxApi.deleteImage(imageId)
      const image = images.value.find((i) => i.id === imageId)
      images.value = images.value.filter((i) => i.id !== imageId)
      // Update thread image count
      if (image) {
        const thread = threads.value.find((t) => t.id === image.threadId)
        if (thread) thread.imageCount--
        if (currentThread.value?.id === image.threadId) currentThread.value.imageCount--
      }
    } catch (e) {
      console.error('Failed to delete fotobox image:', e)
      throw e
    }
  }

  return {
    threads,
    currentThread,
    images,
    settings,
    loading,
    fetchSettings,
    updateSettings,
    fetchThreads,
    fetchThread,
    fetchImages,
    createThread,
    deleteThread,
    uploadImages,
    deleteImage,
  }
})
