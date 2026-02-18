import client from './client'
import type { ApiResponse } from '@/types/api'
import type { TenantConfig } from '@/types/family'

export const adminApi = {
  getConfig() {
    return client.get<ApiResponse<TenantConfig>>('/admin/config')
  },

  updateConfig(data: { schoolName?: string; logoUrl?: string; targetHoursPerFamily?: number; targetCleaningHours?: number; bundesland?: string; schoolVacations?: { name: string; from: string; to: string }[]; requireAssignmentConfirmation?: boolean }) {
    return client.put<ApiResponse<TenantConfig>>('/admin/config', data)
  },

  updateTheme(theme: Record<string, string>) {
    return client.put<ApiResponse<TenantConfig>>('/admin/config/theme', theme)
  },

  updateModules(modules: Record<string, boolean>) {
    return client.put<ApiResponse<TenantConfig>>('/admin/config/modules', modules)
  },

  uploadLogo(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return client.post<ApiResponse<TenantConfig>>('/admin/config/logo', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  getPublicConfig() {
    return client.get<ApiResponse<TenantConfig>>('/config')
  },
}
