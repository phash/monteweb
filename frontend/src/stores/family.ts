import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { familyApi } from '@/api/family.api'
import type { FamilyInfo } from '@/types/family'

export const useFamilyStore = defineStore('family', () => {
  const families = ref<FamilyInfo[]>([])
  const loading = ref(false)

  const primaryFamily = computed(() => families.value[0] ?? null)
  const hasFamily = computed(() => families.value.length > 0)

  async function fetchFamilies() {
    loading.value = true
    try {
      const res = await familyApi.getMine()
      families.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function createFamily(name: string) {
    const res = await familyApi.create(name)
    families.value.push(res.data.data)
    return res.data.data
  }

  async function joinFamily(inviteCode: string) {
    const res = await familyApi.join(inviteCode)
    families.value.push(res.data.data)
    return res.data.data
  }

  return {
    families,
    primaryFamily,
    hasFamily,
    loading,
    fetchFamilies,
    createFamily,
    joinFamily,
  }
})
