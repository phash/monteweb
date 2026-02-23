import client from './client'
import type { ApiResponse } from '@/types/api'
import type { TenantConfig } from '@/types/family'
import type { CsvImportResult } from '@/types/user'

export const adminApi = {
  getConfig() {
    return client.get<ApiResponse<TenantConfig>>('/admin/config')
  },

  updateConfig(data: { schoolName?: string; logoUrl?: string; targetHoursPerFamily?: number; targetCleaningHours?: number; bundesland?: string; schoolVacations?: { name: string; from: string; to: string }[]; requireAssignmentConfirmation?: boolean; multilanguageEnabled?: boolean; defaultLanguage?: string; availableLanguages?: string[]; requireUserApproval?: boolean; privacyPolicyText?: string; privacyPolicyVersion?: string; termsText?: string; termsVersion?: string; dataRetentionDaysNotifications?: number; dataRetentionDaysAudit?: number; schoolFullName?: string; schoolAddress?: string; schoolPrincipal?: string; techContactName?: string; techContactEmail?: string; twoFactorMode?: string; ldapEnabled?: boolean; ldapUrl?: string; ldapBaseDn?: string; ldapBindDn?: string; ldapBindPassword?: string; ldapUserSearchFilter?: string; ldapAttrEmail?: string; ldapAttrFirstName?: string; ldapAttrLastName?: string; ldapDefaultRole?: string; ldapUseSsl?: boolean }) {
    return client.put<ApiResponse<TenantConfig>>('/admin/config', data)
  },

  testLdapConnection(data: { ldapUrl?: string; ldapBaseDn?: string; ldapBindDn?: string; ldapBindPassword?: string; ldapUseSsl?: string }) {
    return client.post<ApiResponse<{ success: boolean; message: string }>>('/admin/ldap/test', data)
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

  // CSV Import
  uploadCsv(file: File, dryRun = false) {
    const form = new FormData()
    form.append('file', file)
    return client.post<ApiResponse<CsvImportResult>>(`/admin/csv-import?dryRun=${dryRun}`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  downloadExampleCsv() {
    return client.get('/admin/csv-import/example', { responseType: 'blob' })
  },
}
