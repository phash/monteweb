import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { ErrorReportInfo, UpdateGithubConfigRequest } from '@/types/errorReport'

export const errorReportApi = {
  // Public endpoint - submits error report
  submitReport(data: { source: string; errorType?: string; message: string; stackTrace?: string; location?: string }) {
    return client.post<ApiResponse<void>>('/error-reports', {
      ...data,
      userAgent: navigator.userAgent,
      requestUrl: window.location.href,
    })
  },

  // Admin endpoints
  getAll(params?: { status?: string; source?: string; page?: number; size?: number; sort?: string }) {
    return client.get<ApiResponse<PageResponse<ErrorReportInfo>>>('/admin/error-reports', { params })
  },

  getById(id: string) {
    return client.get<ApiResponse<ErrorReportInfo>>(`/admin/error-reports/${id}`)
  },

  updateStatus(id: string, status: string) {
    return client.put<ApiResponse<ErrorReportInfo>>(`/admin/error-reports/${id}/status`, { status })
  },

  createGithubIssue(id: string) {
    return client.post<ApiResponse<ErrorReportInfo>>(`/admin/error-reports/${id}/github`)
  },

  deleteReport(id: string) {
    return client.delete<ApiResponse<void>>(`/admin/error-reports/${id}`)
  },

  updateGithubConfig(data: UpdateGithubConfigRequest) {
    return client.put<ApiResponse<void>>('/admin/error-reports/github-config', data)
  },
}
