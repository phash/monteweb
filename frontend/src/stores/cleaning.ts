import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { CleaningConfigInfo, CleaningSlotInfo, DashboardInfo, RegistrationInfo } from '@/types/cleaning'
import * as cleaningApi from '@/api/cleaning.api'

export const useCleaningStore = defineStore('cleaning', () => {
  const upcomingSlots = ref<CleaningSlotInfo[]>([])
  const mySlots = ref<CleaningSlotInfo[]>([])
  const currentSlot = ref<CleaningSlotInfo | null>(null)
  const configs = ref<CleaningConfigInfo[]>([])
  const dashboard = ref<DashboardInfo | null>(null)
  const pendingConfirmations = ref<RegistrationInfo[]>([])
  const loading = ref(false)
  const totalPages = ref(0)
  const currentPage = ref(0)

  async function loadUpcomingSlots(page = 0) {
    loading.value = true
    try {
      const res = await cleaningApi.getUpcomingSlots(page)
      upcomingSlots.value = res.data.data.content
      totalPages.value = res.data.data.totalPages
      currentPage.value = page
    } finally {
      loading.value = false
    }
  }

  async function loadMySlots() {
    loading.value = true
    try {
      const res = await cleaningApi.getMySlots()
      mySlots.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function loadSlot(id: string) {
    loading.value = true
    try {
      const res = await cleaningApi.getSlotById(id)
      currentSlot.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function registerForSlot(slotId: string) {
    const res = await cleaningApi.registerForSlot(slotId)
    currentSlot.value = res.data.data
    return res.data.data
  }

  async function unregisterFromSlot(slotId: string) {
    await cleaningApi.unregisterFromSlot(slotId)
    if (currentSlot.value?.id === slotId) {
      await loadSlot(slotId)
    }
  }

  async function offerSwap(slotId: string) {
    await cleaningApi.offerSwap(slotId)
  }

  async function checkIn(slotId: string, qrToken: string) {
    const res = await cleaningApi.checkIn(slotId, qrToken)
    currentSlot.value = res.data.data
    return res.data.data
  }

  async function checkOut(slotId: string) {
    const res = await cleaningApi.checkOut(slotId)
    currentSlot.value = res.data.data
    return res.data.data
  }

  // ── Confirmations ──────────────────────────────────────────────────

  async function fetchPendingConfirmations() {
    try {
      const res = await cleaningApi.getPendingConfirmations()
      pendingConfirmations.value = res.data.data
    } catch {
      pendingConfirmations.value = []
    }
  }

  async function confirmRegistration(registrationId: string) {
    await cleaningApi.confirmRegistration(registrationId)
    pendingConfirmations.value = pendingConfirmations.value.filter(r => r.id !== registrationId)
  }

  async function rejectRegistration(registrationId: string) {
    await cleaningApi.rejectRegistration(registrationId)
    pendingConfirmations.value = pendingConfirmations.value.filter(r => r.id !== registrationId)
  }

  // ── Admin ───────────────────────────────────────────────────────────

  async function loadConfigs(sectionId?: string, roomId?: string) {
    loading.value = true
    try {
      const res = await cleaningApi.getConfigs(sectionId, roomId)
      configs.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function createConfig(request: Parameters<typeof cleaningApi.createConfig>[0]) {
    const res = await cleaningApi.createConfig(request)
    configs.value.push(res.data.data)
    return res.data.data
  }

  async function generateSlots(configId: string, from: string, to: string) {
    const res = await cleaningApi.generateSlots(configId, { from, to })
    return res.data.data
  }

  async function cancelSlot(slotId: string) {
    await cleaningApi.cancelSlot(slotId)
  }

  async function loadDashboard(sectionId: string, from: string, to: string) {
    loading.value = true
    try {
      const res = await cleaningApi.getDashboard(sectionId, from, to)
      dashboard.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  return {
    upcomingSlots, mySlots, currentSlot, configs, dashboard, pendingConfirmations,
    loading, totalPages, currentPage,
    loadUpcomingSlots, loadMySlots, loadSlot,
    registerForSlot, unregisterFromSlot, offerSwap,
    checkIn, checkOut,
    fetchPendingConfirmations, confirmRegistration, rejectRegistration,
    loadConfigs, createConfig, generateSlots, cancelSlot, loadDashboard
  }
})
