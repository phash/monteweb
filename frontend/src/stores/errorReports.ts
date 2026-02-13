import { defineStore } from 'pinia'
import { ref } from 'vue'
import { errorReportApi } from '@/api/errorReport.api'
import type { ErrorReportInfo } from '@/types/errorReport'

export const useErrorReportsStore = defineStore('errorReports', () => {
  const reports = ref<ErrorReportInfo[]>([])
  const totalRecords = ref(0)
  const loading = ref(false)
  const githubRepo = ref('')
  const githubPatConfigured = ref(false)

  async function fetchReports(params?: { status?: string; source?: string; page?: number; size?: number; sort?: string }) {
    loading.value = true
    try {
      const res = await errorReportApi.getAll(params)
      reports.value = res.data.data.content
      totalRecords.value = res.data.data.totalElements
    } finally {
      loading.value = false
    }
  }

  async function updateStatus(id: string, status: string) {
    const res = await errorReportApi.updateStatus(id, status)
    const idx = reports.value.findIndex(r => r.id === id)
    if (idx >= 0) reports.value[idx] = res.data.data
    return res.data.data
  }

  async function createGithubIssue(id: string) {
    const res = await errorReportApi.createGithubIssue(id)
    const idx = reports.value.findIndex(r => r.id === id)
    if (idx >= 0) reports.value[idx] = res.data.data
    return res.data.data
  }

  async function deleteReport(id: string) {
    await errorReportApi.deleteReport(id)
    reports.value = reports.value.filter(r => r.id !== id)
    totalRecords.value--
  }

  async function updateGithubConfig(repo: string, pat: string) {
    await errorReportApi.updateGithubConfig({ githubRepo: repo, githubPat: pat })
    githubRepo.value = repo
    githubPatConfigured.value = true
  }

  return {
    reports,
    totalRecords,
    loading,
    githubRepo,
    githubPatConfigured,
    fetchReports,
    updateStatus,
    createGithubIssue,
    deleteReport,
    updateGithubConfig,
  }
})
