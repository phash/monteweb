import { defineStore } from 'pinia'
import { ref } from 'vue'
import { jobboardApi } from '@/api/jobboard.api'
import type {
  JobInfo,
  JobAssignmentInfo,
  JobStatus,
  FamilyHoursInfo,
  ReportSummary,
  CreateJobRequest,
} from '@/types/jobboard'

export const useJobboardStore = defineStore('jobboard', () => {
  const jobs = ref<JobInfo[]>([])
  const currentJob = ref<JobInfo | null>(null)
  const assignments = ref<JobAssignmentInfo[]>([])
  const myAssignments = ref<JobAssignmentInfo[]>([])
  const pendingConfirmations = ref<JobAssignmentInfo[]>([])
  const categories = ref<string[]>([])
  const familyHours = ref<FamilyHoursInfo | null>(null)
  const report = ref<FamilyHoursInfo[]>([])
  const reportSummary = ref<ReportSummary | null>(null)
  const loading = ref(false)
  const hasMore = ref(true)
  const page = ref(0)

  async function fetchJobs(reset = false, category?: string, eventId?: string, statuses?: JobStatus[],
                           fromDate?: string, toDate?: string) {
    if (reset) {
      page.value = 0
      hasMore.value = true
      jobs.value = []
    }
    if (!hasMore.value) return

    loading.value = true
    try {
      const res = await jobboardApi.listJobs(page.value, 20, category, statuses, eventId, fromDate, toDate)
      const data = res.data.data
      jobs.value = reset ? data.content : [...jobs.value, ...data.content]
      hasMore.value = !data.last
      page.value++
    } catch {
      // Jobs not available
    } finally {
      loading.value = false
    }
  }

  async function fetchCategories() {
    try {
      const res = await jobboardApi.getCategories()
      categories.value = res.data.data
    } catch {
      categories.value = []
    }
  }

  async function fetchJob(id: string) {
    loading.value = true
    try {
      const res = await jobboardApi.getJob(id)
      currentJob.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function createJob(data: CreateJobRequest) {
    const res = await jobboardApi.createJob(data)
    jobs.value.unshift(res.data.data)
    return res.data.data
  }

  async function applyForJob(jobId: string) {
    const res = await jobboardApi.applyForJob(jobId)
    myAssignments.value.push(res.data.data)
    // Update job in list
    const job = jobs.value.find(j => j.id === jobId)
    if (job) job.currentAssignees++
    return res.data.data
  }

  async function fetchAssignments(jobId: string) {
    const res = await jobboardApi.getAssignments(jobId)
    assignments.value = res.data.data
  }

  async function fetchMyAssignments() {
    try {
      const res = await jobboardApi.getMyAssignments()
      myAssignments.value = res.data.data
    } catch {
      myAssignments.value = []
    }
  }

  async function startAssignment(assignmentId: string) {
    const res = await jobboardApi.startAssignment(assignmentId)
    updateAssignmentInList(res.data.data)
  }

  async function cancelAssignment(assignmentId: string) {
    await jobboardApi.cancelAssignment(assignmentId)
    myAssignments.value = myAssignments.value.filter(a => a.id !== assignmentId)
  }

  async function completeAssignment(assignmentId: string, hours: number, notes?: string) {
    const res = await jobboardApi.completeAssignment(assignmentId, hours, notes)
    updateAssignmentInList(res.data.data)
  }

  async function confirmAssignment(assignmentId: string) {
    const res = await jobboardApi.confirmAssignment(assignmentId)
    updateAssignmentInList(res.data.data)
    pendingConfirmations.value = pendingConfirmations.value.filter(a => a.id !== assignmentId)
  }

  async function fetchPendingConfirmations() {
    try {
      const res = await jobboardApi.getPendingConfirmations()
      pendingConfirmations.value = res.data.data
    } catch {
      pendingConfirmations.value = []
    }
  }

  async function fetchFamilyHours(familyId: string) {
    try {
      const res = await jobboardApi.getFamilyHours(familyId)
      familyHours.value = res.data.data
    } catch {
      familyHours.value = null
    }
  }

  async function fetchReport() {
    loading.value = true
    try {
      const [reportRes, summaryRes] = await Promise.all([
        jobboardApi.getReport(),
        jobboardApi.getReportSummary(),
      ])
      report.value = reportRes.data.data
      reportSummary.value = summaryRes.data.data
    } finally {
      loading.value = false
    }
  }

  async function exportCsv() {
    const res = await jobboardApi.exportCsv()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = 'familien-stundenbericht.csv'
    a.click()
    window.URL.revokeObjectURL(url)
  }

  async function exportPdf() {
    const res = await jobboardApi.exportPdf()
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = 'familien-stundenbericht.pdf'
    a.click()
    window.URL.revokeObjectURL(url)
  }

  function updateAssignmentInList(updated: JobAssignmentInfo) {
    const idx = myAssignments.value.findIndex(a => a.id === updated.id)
    if (idx >= 0) myAssignments.value[idx] = updated
    const idx2 = assignments.value.findIndex(a => a.id === updated.id)
    if (idx2 >= 0) assignments.value[idx2] = updated
  }

  return {
    jobs,
    currentJob,
    assignments,
    myAssignments,
    pendingConfirmations,
    categories,
    familyHours,
    report,
    reportSummary,
    loading,
    hasMore,
    fetchJobs,
    fetchCategories,
    fetchJob,
    createJob,
    applyForJob,
    fetchAssignments,
    fetchMyAssignments,
    startAssignment,
    cancelAssignment,
    completeAssignment,
    confirmAssignment,
    fetchPendingConfirmations,
    fetchFamilyHours,
    fetchReport,
    exportCsv,
    exportPdf,
  }
})
