import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  CleaningConfigInfo,
  CleaningSlotInfo,
  CreateConfigRequest,
  DashboardInfo,
  GenerateSlotsRequest,
  RegistrationInfo
} from '@/types/cleaning'

// ── Slots (User) ──────────────────────────────────────────────────────

export function getUpcomingSlots(page = 0, size = 20) {
  return client.get<ApiResponse<PageResponse<CleaningSlotInfo>>>('/cleaning/slots', {
    params: { page, size }
  })
}

export function getMySlots() {
  return client.get<ApiResponse<CleaningSlotInfo[]>>('/cleaning/slots/mine')
}

export function getSlotById(id: string) {
  return client.get<ApiResponse<CleaningSlotInfo>>(`/cleaning/slots/${id}`)
}

export function registerForSlot(slotId: string) {
  return client.post<ApiResponse<CleaningSlotInfo>>(`/cleaning/slots/${slotId}/register`)
}

export function unregisterFromSlot(slotId: string) {
  return client.delete<ApiResponse<void>>(`/cleaning/slots/${slotId}/register`)
}

export function offerSwap(slotId: string) {
  return client.post<ApiResponse<void>>(`/cleaning/slots/${slotId}/swap`)
}

export function getSwapOffers(slotId: string) {
  return client.get<ApiResponse<RegistrationInfo[]>>(`/cleaning/slots/${slotId}/swaps`)
}

export function checkIn(slotId: string, qrToken: string) {
  return client.post<ApiResponse<CleaningSlotInfo>>(`/cleaning/slots/${slotId}/checkin`, { qrToken })
}

export function checkOut(slotId: string) {
  return client.post<ApiResponse<CleaningSlotInfo>>(`/cleaning/slots/${slotId}/checkout`)
}

// ── Admin: Configs ────────────────────────────────────────────────────

export function getConfigs(sectionId?: string) {
  return client.get<ApiResponse<CleaningConfigInfo[]>>('/cleaning/configs', {
    params: sectionId ? { sectionId } : {}
  })
}

export function createConfig(request: CreateConfigRequest) {
  return client.post<ApiResponse<CleaningConfigInfo>>('/cleaning/configs', request)
}

export function updateConfig(id: string, request: Partial<CreateConfigRequest & { active: boolean }>) {
  return client.put<ApiResponse<CleaningConfigInfo>>(`/cleaning/configs/${id}`, request)
}

// ── Admin: Slot Generation ────────────────────────────────────────────

export function generateSlots(configId: string, request: GenerateSlotsRequest) {
  return client.post<ApiResponse<CleaningSlotInfo[]>>(`/cleaning/configs/${configId}/generate`, request)
}

// ── Admin: Slot Management ────────────────────────────────────────────

export function updateSlot(id: string, request: Record<string, unknown>) {
  return client.put<ApiResponse<CleaningSlotInfo>>(`/cleaning/slots/${id}`, request)
}

export function cancelSlot(id: string) {
  return client.delete<ApiResponse<void>>(`/cleaning/slots/${id}`)
}

export function getQrToken(slotId: string) {
  return client.get<ApiResponse<{ qrToken: string }>>(`/cleaning/slots/${slotId}/qr`)
}

// ── Admin: Dashboard ──────────────────────────────────────────────────

export function getDashboard(sectionId: string, from: string, to: string) {
  return client.get<ApiResponse<DashboardInfo>>('/cleaning/dashboard', {
    params: { sectionId, from, to }
  })
}
