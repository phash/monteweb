import { defineStore } from 'pinia'
import { ref } from 'vue'
import { billingApi } from '@/api/billing.api'
import type {
  BillingPeriodInfo,
  BillingReportInfo,
  CreateBillingPeriodRequest,
} from '@/types/billing'

export const useBillingStore = defineStore('billing', () => {
  const periods = ref<BillingPeriodInfo[]>([])
  const activePeriod = ref<BillingPeriodInfo | null>(null)
  const report = ref<BillingReportInfo | null>(null)
  const loading = ref(false)

  async function fetchPeriods() {
    loading.value = true
    try {
      const [periodsRes, activeRes] = await Promise.all([
        billingApi.listPeriods(),
        billingApi.getActivePeriod(),
      ])
      periods.value = periodsRes.data.data
      activePeriod.value = activeRes.data.data
    } finally {
      loading.value = false
    }
  }

  async function createPeriod(data: CreateBillingPeriodRequest) {
    const res = await billingApi.createPeriod(data)
    activePeriod.value = res.data.data
    periods.value.unshift(res.data.data)
    return res.data.data
  }

  async function fetchReport(periodId: string) {
    loading.value = true
    try {
      const res = await billingApi.getReport(periodId)
      report.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function closePeriod(periodId: string) {
    const res = await billingApi.closePeriod(periodId)
    // Refresh all data after closing
    await fetchPeriods()
    if (activePeriod.value) {
      await fetchReport(activePeriod.value.id)
    } else {
      report.value = null
    }
    return res.data.data
  }

  async function exportPdf(periodId: string) {
    const res = await billingApi.exportPdf(periodId)
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = 'jahresabrechnung.pdf'
    a.click()
    window.URL.revokeObjectURL(url)
  }

  async function exportCsv(periodId: string) {
    const res = await billingApi.exportCsv(periodId)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = 'jahresabrechnung.csv'
    a.click()
    window.URL.revokeObjectURL(url)
  }

  return {
    periods,
    activePeriod,
    report,
    loading,
    fetchPeriods,
    createPeriod,
    fetchReport,
    closePeriod,
    exportPdf,
    exportCsv,
  }
})
