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
    } finally {
      loading.value = false
    }
  }

  async function createItem(data: CreateFundgrubeItemRequest) {
    const res = await fundgrubeApi.createItem(data)
    items.value.unshift(res.data.data)
    return res.data.data
  }

  async function updateItem(itemId: string, data: UpdateFundgrubeItemRequest) {
    const res = await fundgrubeApi.updateItem(itemId, data)
    const idx = items.value.findIndex((i) => i.id === itemId)
    if (idx !== -1) items.value[idx] = res.data.data
    return res.data.data
  }

  async function deleteItem(itemId: string) {
    await fundgrubeApi.deleteItem(itemId)
    items.value = items.value.filter((i) => i.id !== itemId)
  }

  async function claimItem(itemId: string, data: ClaimItemRequest = {}) {
    const res = await fundgrubeApi.claimItem(itemId, data)
    const idx = items.value.findIndex((i) => i.id === itemId)
    if (idx !== -1) items.value[idx] = res.data.data
    return res.data.data
  }

  async function uploadImages(itemId: string, files: File[]) {
    const res = await fundgrubeApi.uploadImages(itemId, files)
    const idx = items.value.findIndex((i) => i.id === itemId)
    if (idx !== -1) items.value[idx]!.images.push(...res.data.data)
    return res.data.data
  }

  async function deleteImage(itemId: string, imageId: string) {
    await fundgrubeApi.deleteImage(imageId)
    const item = items.value.find((i) => i.id === itemId)
    if (item) item.images = item.images.filter((img) => img.id !== imageId)
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
