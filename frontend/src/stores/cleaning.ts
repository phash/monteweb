import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { CleaningConfigInfo, CleaningSlotInfo, DashboardInfo, RegistrationInfo } from '@/types/cleaning'
import { cleaningApi } from '@/api/cleaning.api'

export const useCleaningStore = defineStore('cleaning', () => {
  const upcomingSlots = ref<CleaningSlotInfo[]>([])
  const mySlots = ref<CleaningSlotInfo[]>([])
  const currentSlot = ref<CleaningSlotInfo | null>(null)
  const configs = ref<CleaningConfigInfo[]>([])
  const dashboard = ref<DashboardInfo | null>(null)
  const pendingConfirmations = ref<RegistrationInfo[]>([])
  const loading = ref(false)
  const loadingSlot = ref(false)
  const loadingDashboard = ref(false)
  const totalPages = ref(0)
  const currentPage = ref(0)

  async function loadUpcomingSlots(page = 0) {
    loading.value = true
    try {
      const res = await cleaningApi.getUpcomingSlots(page)
      upcomingSlots.value = res.data.data.content
      totalPages.value = res.data.data.totalPages
      currentPage.value = page
    } catch (e) {
      console.error('Failed to load upcoming slots:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function loadMySlots() {
    loading.value = true
    try {
      const res = await cleaningApi.getMySlots()
      mySlots.value = res.data.data
    } catch (e) {
      console.error('Failed to load my slots:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function loadSlot(id: string) {
    loadingSlot.value = true
    try {
      const res = await cleaningApi.getSlotById(id)
      currentSlot.value = res.data.data
    } catch (e) {
      console.error('Failed to load slot:', e)
      throw e
    } finally {
      loadingSlot.value = false
    }
  }

  async function registerForSlot(slotId: string) {
    try {
      const res = await cleaningApi.registerForSlot(slotId)
      currentSlot.value = res.data.data
      return res.data.data
    } catch (e) {
      console.error('Failed to register for slot:', e)
      throw e
    }
  }

  async function unregisterFromSlot(slotId: string) {
    try {
      await cleaningApi.unregisterFromSlot(slotId)
      if (currentSlot.value?.id === slotId) {
        await loadSlot(slotId)
      }
    } catch (e) {
      console.error('Failed to unregister from slot:', e)
      throw e
    }
  }

  async function offerSwap(slotId: string) {
    try {
      await cleaningApi.offerSwap(slotId)
    } catch (e) {
      console.error('Failed to offer swap:', e)
      throw e
    }
  }

  async function checkIn(slotId: string, qrToken: string) {
    try {
      const res = await cleaningApi.checkIn(slotId, qrToken)
      currentSlot.value = res.data.data
      return res.data.data
    } catch (e) {
      console.error('Failed to check in:', e)
      throw e
    }
  }

  async function checkOut(slotId: string) {
    try {
      const res = await cleaningApi.checkOut(slotId)
      currentSlot.value = res.data.data
      return res.data.data
    } catch (e) {
      console.error('Failed to check out:', e)
      throw e
    }
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
    try {
      await cleaningApi.confirmRegistration(registrationId)
      pendingConfirmations.value = pendingConfirmations.value.filter(r => r.id !== registrationId)
    } catch (e) {
      console.error('Failed to confirm registration:', e)
      throw e
    }
  }

  async function rejectRegistration(registrationId: string) {
    try {
      await cleaningApi.rejectRegistration(registrationId)
      pendingConfirmations.value = pendingConfirmations.value.filter(r => r.id !== registrationId)
    } catch (e) {
      console.error('Failed to reject registration:', e)
      throw e
    }
  }

  // ── Admin ───────────────────────────────────────────────────────────

  async function loadConfigs(sectionId?: string, roomId?: string) {
    loading.value = true
    try {
      const res = await cleaningApi.getConfigs(sectionId, roomId)
      configs.value = res.data.data
    } catch (e) {
      console.error('Failed to load configs:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function createConfig(request: Parameters<typeof cleaningApi.createConfig>[0]) {
    try {
      const res = await cleaningApi.createConfig(request)
      configs.value.push(res.data.data)
      return res.data.data
    } catch (e) {
      console.error('Failed to create config:', e)
      throw e
    }
  }

  async function generateSlots(configId: string, from: string, to: string) {
    try {
      const res = await cleaningApi.generateSlots(configId, { from, to })
      return res.data.data
    } catch (e) {
      console.error('Failed to generate slots:', e)
      throw e
    }
  }

  async function cancelSlot(slotId: string) {
    try {
      await cleaningApi.cancelSlot(slotId)
    } catch (e) {
      console.error('Failed to cancel slot:', e)
      throw e
    }
  }

  async function loadDashboard(sectionId: string, from: string, to: string) {
    loadingDashboard.value = true
    try {
      const res = await cleaningApi.getDashboard(sectionId, from, to)
      dashboard.value = res.data.data
    } catch (e) {
      console.error('Failed to load dashboard:', e)
      throw e
    } finally {
      loadingDashboard.value = false
    }
  }

  return {
    upcomingSlots, mySlots, currentSlot, configs, dashboard, pendingConfirmations,
    loading, loadingSlot, loadingDashboard, totalPages, currentPage,
    loadUpcomingSlots, loadMySlots, loadSlot,
    registerForSlot, unregisterFromSlot, offerSwap,
    checkIn, checkOut,
    fetchPendingConfirmations, confirmRegistration, rejectRegistration,
    loadConfigs, createConfig, generateSlots, cancelSlot, loadDashboard
  }
})
