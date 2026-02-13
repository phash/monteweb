import client from './client'
import type { ApiResponse } from '@/types/api'
import type {
  BillingPeriodInfo,
  BillingReportInfo,
  CreateBillingPeriodRequest,
} from '@/types/billing'

export const billingApi = {
  listPeriods() {
    return client.get<ApiResponse<BillingPeriodInfo[]>>('/billing/periods')
  },

  getActivePeriod() {
    return client.get<ApiResponse<BillingPeriodInfo | null>>('/billing/periods/active')
  },

  createPeriod(data: CreateBillingPeriodRequest) {
    return client.post<ApiResponse<BillingPeriodInfo>>('/billing/periods', data)
  },

  getReport(periodId: string) {
    return client.get<ApiResponse<BillingReportInfo>>(`/billing/periods/${periodId}/report`)
  },

  closePeriod(periodId: string) {
    return client.post<ApiResponse<BillingPeriodInfo>>(`/billing/periods/${periodId}/close`)
  },

  exportPdf(periodId: string) {
    return client.get(`/billing/periods/${periodId}/export/pdf`, { responseType: 'blob' })
  },

  exportCsv(periodId: string) {
    return client.get(`/billing/periods/${periodId}/export/csv`, { responseType: 'blob' })
  },
}
