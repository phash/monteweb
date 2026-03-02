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

export const cleaningApi = {
  // ── Slots (User) ──────────────────────────────────────────────────────

  getUpcomingSlots(page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<CleaningSlotInfo>>>('/cleaning/slots', {
      params: { page, size }
    })
  },

  getMySlots() {
    return client.get<ApiResponse<CleaningSlotInfo[]>>('/cleaning/slots/mine')
  },

  getSlotById(id: string) {
    return client.get<ApiResponse<CleaningSlotInfo>>(`/cleaning/slots/${id}`)
  },

  registerForSlot(slotId: string) {
    return client.post<ApiResponse<CleaningSlotInfo>>(`/cleaning/slots/${slotId}/register`)
  },

  unregisterFromSlot(slotId: string) {
    return client.delete<ApiResponse<void>>(`/cleaning/slots/${slotId}/register`)
  },

  offerSwap(slotId: string) {
    return client.post<ApiResponse<void>>(`/cleaning/slots/${slotId}/swap`)
  },

  checkIn(slotId: string, qrToken: string) {
    return client.post<ApiResponse<CleaningSlotInfo>>(`/cleaning/slots/${slotId}/checkin`, { qrToken })
  },

  checkOut(slotId: string) {
    return client.post<ApiResponse<CleaningSlotInfo>>(`/cleaning/slots/${slotId}/checkout`)
  },

  // ── Confirmations ────────────────────────────────────────────────────

  getPendingConfirmations() {
    return client.get<ApiResponse<RegistrationInfo[]>>('/cleaning/registrations/pending-confirmation')
  },

  confirmRegistration(registrationId: string) {
    return client.put<ApiResponse<RegistrationInfo>>(`/cleaning/registrations/${registrationId}/confirm`)
  },

  rejectRegistration(registrationId: string) {
    return client.put<ApiResponse<void>>(`/cleaning/registrations/${registrationId}/reject`)
  },

  updateRegistrationMinutes(registrationId: string, actualMinutes: number) {
    return client.put<ApiResponse<RegistrationInfo>>(`/cleaning/registrations/${registrationId}/update-minutes`, { actualMinutes })
  },

  // ── Admin: Configs ────────────────────────────────────────────────────

  getConfigs(sectionId?: string, roomId?: string) {
    const params: Record<string, string> = {}
    if (sectionId) params.sectionId = sectionId
    if (roomId) params.roomId = roomId
    return client.get<ApiResponse<CleaningConfigInfo[]>>('/cleaning/configs', { params })
  },

  createConfig(request: CreateConfigRequest) {
    return client.post<ApiResponse<CleaningConfigInfo>>('/cleaning/configs', request)
  },

  updateConfig(id: string, request: Partial<CreateConfigRequest & { active: boolean }>) {
    return client.put<ApiResponse<CleaningConfigInfo>>(`/cleaning/configs/${id}`, request)
  },

  // ── Admin: Slot Generation ────────────────────────────────────────────

  generateSlots(configId: string, request: GenerateSlotsRequest) {
    return client.post<ApiResponse<CleaningSlotInfo[]>>(`/cleaning/configs/${configId}/generate`, request)
  },

  // ── Admin: Slot Management ────────────────────────────────────────────

  updateSlot(id: string, request: Record<string, unknown>) {
    return client.put<ApiResponse<CleaningSlotInfo>>(`/cleaning/slots/${id}`, request)
  },

  cancelSlot(id: string) {
    return client.delete<ApiResponse<void>>(`/cleaning/slots/${id}`)
  },

  getQrToken(slotId: string) {
    return client.get<ApiResponse<{ qrToken: string }>>(`/cleaning/slots/${slotId}/qr`)
  },

  // ── Admin: QR Code PDF Export ─────────────────────────────────────────

  exportQrCodesPdf(configId: string, from: string, to: string) {
    return client.get(`/cleaning/configs/${configId}/qr-codes`, {
      params: { from, to },
      responseType: 'blob',
    })
  },

  // ── Admin: Dashboard ──────────────────────────────────────────────────

  getDashboard(sectionId: string, from: string, to: string) {
    return client.get<ApiResponse<DashboardInfo>>('/cleaning/dashboard', {
      params: { sectionId, from, to }
    })
  },
}
