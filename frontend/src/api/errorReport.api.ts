import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { ErrorReportInfo, UpdateGithubConfigRequest } from '@/types/errorReport'

export const errorReportApi = {
  // Admin endpoints
  getAll(params?: { status?: string; source?: string; page?: number; size?: number; sort?: string }) {
    return client.get<ApiResponse<PageResponse<ErrorReportInfo>>>('/admin/error-reports', { params })
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
