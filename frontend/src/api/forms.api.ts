import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  FormInfo,
  FormDetailInfo,
  CreateFormRequest,
  UpdateFormRequest,
  SubmitResponseRequest,
  FormResultsSummary,
  IndividualResponse,
} from '@/types/forms'

export const formsApi = {
  getAvailableForms(page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<FormInfo>>>('/forms', {
      params: { page, size },
    })
  },

  getMyForms(page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<FormInfo>>>('/forms/mine', {
      params: { page, size },
    })
  },

  getForm(id: string) {
    return client.get<ApiResponse<FormDetailInfo>>(`/forms/${id}`)
  },

  createForm(data: CreateFormRequest) {
    return client.post<ApiResponse<FormDetailInfo>>('/forms', data)
  },

  updateForm(id: string, data: UpdateFormRequest) {
    return client.put<ApiResponse<FormDetailInfo>>(`/forms/${id}`, data)
  },

  deleteForm(id: string) {
    return client.delete<ApiResponse<void>>(`/forms/${id}`)
  },

  publishForm(id: string) {
    return client.post<ApiResponse<FormInfo>>(`/forms/${id}/publish`)
  },

  closeForm(id: string) {
    return client.post<ApiResponse<FormInfo>>(`/forms/${id}/close`)
  },

  submitResponse(id: string, data: SubmitResponseRequest) {
    return client.post<ApiResponse<void>>(`/forms/${id}/respond`, data)
  },

  getResults(id: string) {
    return client.get<ApiResponse<FormResultsSummary>>(`/forms/${id}/results`)
  },

  getIndividualResponses(id: string) {
    return client.get<ApiResponse<IndividualResponse[]>>(`/forms/${id}/responses`)
  },

  exportCsv(id: string) {
    return client.get(`/forms/${id}/results/csv`, { responseType: 'blob' })
  },

  exportPdf(id: string) {
    return client.get(`/forms/${id}/results/pdf`, { responseType: 'blob' })
  },
}
