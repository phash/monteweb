import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fundgrubeApi } from '@/api/fundgrube.api'
import type {
  FundgrubeItemInfo,
  CreateFundgrubeItemRequest,
  UpdateFundgrubeItemRequest,
  ClaimItemRequest,
} from '@/types/fundgrube'

export const useFundgrubeStore = defineStore('fundgrube', () => {
  const items = ref<FundgrubeItemInfo[]>([])
  const loading = ref(false)
  const activeSectionId = ref<string | null>(null)

  async function fetchItems(sectionId?: string) {
    loading.value = true
    try {
      const res = await fundgrubeApi.listItems(sectionId)
      items.value = res.data.data
      activeSectionId.value = sectionId ?? null
    } catch (e) {
      console.error('Failed to fetch fundgrube items:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function createItem(data: CreateFundgrubeItemRequest) {
    try {
      const res = await fundgrubeApi.createItem(data)
      items.value.unshift(res.data.data)
      return res.data.data
    } catch (e) {
      console.error('Failed to create fundgrube item:', e)
      throw e
    }
  }

  async function updateItem(itemId: string, data: UpdateFundgrubeItemRequest) {
    try {
      const res = await fundgrubeApi.updateItem(itemId, data)
      const idx = items.value.findIndex((i) => i.id === itemId)
      if (idx !== -1) items.value[idx] = res.data.data
      return res.data.data
    } catch (e) {
      console.error('Failed to update fundgrube item:', e)
      throw e
    }
  }

  async function deleteItem(itemId: string) {
    try {
      await fundgrubeApi.deleteItem(itemId)
      items.value = items.value.filter((i) => i.id !== itemId)
    } catch (e) {
      console.error('Failed to delete fundgrube item:', e)
      throw e
    }
  }

  async function claimItem(itemId: string, data: ClaimItemRequest = {}) {
    try {
      const res = await fundgrubeApi.claimItem(itemId, data)
      const idx = items.value.findIndex((i) => i.id === itemId)
      if (idx !== -1) items.value[idx] = res.data.data
      return res.data.data
    } catch (e) {
      console.error('Failed to claim fundgrube item:', e)
      throw e
    }
  }

  async function uploadImages(itemId: string, files: File[]) {
    try {
      const res = await fundgrubeApi.uploadImages(itemId, files)
      const idx = items.value.findIndex((i) => i.id === itemId)
      if (idx !== -1) items.value[idx]!.images.push(...res.data.data)
      return res.data.data
    } catch (e) {
      console.error('Failed to upload fundgrube images:', e)
      throw e
    }
  }

  async function deleteImage(itemId: string, imageId: string) {
    try {
      await fundgrubeApi.deleteImage(imageId)
      const item = items.value.find((i) => i.id === itemId)
      if (item) item.images = item.images.filter((img) => img.id !== imageId)
    } catch (e) {
      console.error('Failed to delete fundgrube image:', e)
      throw e
    }
  }

  return {
    items,
    loading,
    activeSectionId,
    fetchItems,
    createItem,
    updateItem,
    deleteItem,
    claimItem,
    uploadImages,
    deleteImage,
  }
})
