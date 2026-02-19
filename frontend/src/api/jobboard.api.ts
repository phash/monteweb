import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  JobInfo,
  JobAssignmentInfo,
  FamilyHoursInfo,
  ReportSummary,
  CreateJobRequest,
  JobStatus,
} from '@/types/jobboard'

export const jobboardApi = {
  listJobs(page = 0, size = 20, category?: string, status?: JobStatus[], eventId?: string,
           fromDate?: string, toDate?: string) {
    return client.get<ApiResponse<PageResponse<JobInfo>>>('/jobs', {
      params: { page, size, category, status: status?.join(','), eventId, fromDate, toDate },
    })
  },

  getJobsByEvent(eventId: string) {
    return client.get<ApiResponse<JobInfo[]>>(`/jobs/by-event/${eventId}`)
  },

  listMyJobs(page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<JobInfo>>>('/jobs/mine', { params: { page, size } })
  },

  getCategories() {
    return client.get<ApiResponse<string[]>>('/jobs/categories')
  },

  getJob(id: string) {
    return client.get<ApiResponse<JobInfo>>(`/jobs/${id}`)
  },

  createJob(data: CreateJobRequest) {
    return client.post<ApiResponse<JobInfo>>('/jobs', data)
  },

  updateJob(id: string, data: Partial<CreateJobRequest>) {
    return client.put<ApiResponse<JobInfo>>(`/jobs/${id}`, data)
  },

  cancelJob(id: string) {
    return client.delete<ApiResponse<void>>(`/jobs/${id}`)
  },

  deleteJob(id: string) {
    return client.delete<ApiResponse<void>>(`/jobs/${id}`, { params: { permanent: true } })
  },

  linkEvent(jobId: string, eventId: string) {
    return client.put<ApiResponse<JobInfo>>(`/jobs/${jobId}/link-event`, { eventId })
  },

  // Assignments
  getAssignments(jobId: string) {
    return client.get<ApiResponse<JobAssignmentInfo[]>>(`/jobs/${jobId}/assignments`)
  },

  applyForJob(jobId: string) {
    return client.post<ApiResponse<JobAssignmentInfo>>(`/jobs/${jobId}/apply`)
  },

  startAssignment(assignmentId: string) {
    return client.put<ApiResponse<JobAssignmentInfo>>(`/jobs/assignments/${assignmentId}/start`)
  },

  completeAssignment(assignmentId: string, actualHours: number, notes?: string) {
    return client.put<ApiResponse<JobAssignmentInfo>>(`/jobs/assignments/${assignmentId}/complete`, {
      actualHours,
      notes,
    })
  },

  confirmAssignment(assignmentId: string) {
    return client.put<ApiResponse<JobAssignmentInfo>>(`/jobs/assignments/${assignmentId}/confirm`)
  },

  rejectAssignment(assignmentId: string) {
    return client.put<ApiResponse<void>>(`/jobs/assignments/${assignmentId}/reject`)
  },

  getPendingConfirmations() {
    return client.get<ApiResponse<JobAssignmentInfo[]>>('/jobs/assignments/pending-confirmation')
  },

  cancelAssignment(assignmentId: string) {
    return client.delete<ApiResponse<void>>(`/jobs/assignments/${assignmentId}`)
  },

  getMyAssignments() {
    return client.get<ApiResponse<JobAssignmentInfo[]>>('/jobs/my-assignments')
  },

  getFamilyAssignments(familyId: string) {
    return client.get<ApiResponse<JobAssignmentInfo[]>>(`/jobs/family/${familyId}/assignments`)
  },

  // Hours / Reports
  getFamilyHours(familyId: string) {
    return client.get<ApiResponse<FamilyHoursInfo>>(`/jobs/family/${familyId}/hours`)
  },

  getReport() {
    return client.get<ApiResponse<FamilyHoursInfo[]>>('/jobs/report')
  },

  getReportSummary() {
    return client.get<ApiResponse<ReportSummary>>('/jobs/report/summary')
  },

  exportCsv() {
    return client.get('/jobs/report/export', { responseType: 'blob' })
  },

  exportPdf() {
    return client.get('/jobs/report/pdf', { responseType: 'blob' })
  },
}
