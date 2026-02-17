import client from './client'
import type { ApiResponse } from '@/types/api'
import type {
  FundgrubeItemInfo,
  FundgrubeImageInfo,
  CreateFundgrubeItemRequest,
  UpdateFundgrubeItemRequest,
  ClaimItemRequest,
} from '@/types/fundgrube'

export const fundgrubeApi = {
  // Items
  listItems(sectionId?: string) {
    const params = sectionId ? { sectionId } : {}
    return client.get<ApiResponse<FundgrubeItemInfo[]>>('/fundgrube/items', { params })
  },
  getItem(itemId: string) {
    return client.get<ApiResponse<FundgrubeItemInfo>>(`/fundgrube/items/${itemId}`)
  },
  createItem(data: CreateFundgrubeItemRequest) {
    return client.post<ApiResponse<FundgrubeItemInfo>>('/fundgrube/items', data)
  },
  updateItem(itemId: string, data: UpdateFundgrubeItemRequest) {
    return client.put<ApiResponse<FundgrubeItemInfo>>(`/fundgrube/items/${itemId}`, data)
  },
  deleteItem(itemId: string) {
    return client.delete<ApiResponse<void>>(`/fundgrube/items/${itemId}`)
  },
  claimItem(itemId: string, data: ClaimItemRequest = {}) {
    return client.post<ApiResponse<FundgrubeItemInfo>>(`/fundgrube/items/${itemId}/claim`, data)
  },

  // Images
  uploadImages(itemId: string, files: File[]) {
    const formData = new FormData()
    files.forEach((f) => formData.append('files', f))
    return client.post<ApiResponse<FundgrubeImageInfo[]>>(
      `/fundgrube/items/${itemId}/images`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
  },
  deleteImage(imageId: string) {
    return client.delete<ApiResponse<void>>(`/fundgrube/images/${imageId}`)
  },

  // Image URL helpers — JWT via ?token= for <img> tags
  imageUrl(imageId: string) {
    const token = localStorage.getItem('accessToken')
    return `/api/v1/fundgrube/images/${imageId}${token ? `?token=${encodeURIComponent(token)}` : ''}`
  },
  thumbnailUrl(imageId: string) {
    const token = localStorage.getItem('accessToken')
    return `/api/v1/fundgrube/images/${imageId}/thumbnail${token ? `?token=${encodeURIComponent(token)}` : ''}`
  },
}
