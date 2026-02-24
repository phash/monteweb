import { defineStore } from 'pinia'
import { ref } from 'vue'
import { profileFieldsApi } from '@/api/profilefields.api'
import type {
  ProfileFieldDefinition,
  CreateProfileFieldRequest,
  UpdateProfileFieldRequest,
} from '@/types/profilefields'

export const useProfileFieldsStore = defineStore('profilefields', () => {
  const definitions = ref<ProfileFieldDefinition[]>([])
  const values = ref<Record<string, string>>({})
  const loading = ref(false)

  async function fetchDefinitions() {
    loading.value = true
    try {
      const res = await profileFieldsApi.getDefinitions()
      definitions.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function fetchMyValues() {
    const res = await profileFieldsApi.getMyValues()
    values.value = res.data.data
  }

  async function updateMyValues(data: Record<string, string>) {
    const res = await profileFieldsApi.updateMyValues(data)
    values.value = res.data.data
  }

  // Admin
  const allDefinitions = ref<ProfileFieldDefinition[]>([])

  async function fetchAllDefinitions() {
    loading.value = true
    try {
      const res = await profileFieldsApi.listAllDefinitions()
      allDefinitions.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function createDefinition(data: CreateProfileFieldRequest) {
    const res = await profileFieldsApi.createDefinition(data)
    allDefinitions.value.push(res.data.data)
    return res.data.data
  }

  async function updateDefinition(id: string, data: UpdateProfileFieldRequest) {
    const res = await profileFieldsApi.updateDefinition(id, data)
    const idx = allDefinitions.value.findIndex((d) => d.id === id)
    if (idx >= 0) allDefinitions.value[idx] = res.data.data
    return res.data.data
  }

  async function deleteDefinition(id: string) {
    await profileFieldsApi.deleteDefinition(id)
    allDefinitions.value = allDefinitions.value.filter((d) => d.id !== id)
  }

  return {
    definitions,
    values,
    loading,
    allDefinitions,
    fetchDefinitions,
    fetchMyValues,
    updateMyValues,
    fetchAllDefinitions,
    createDefinition,
    updateDefinition,
    deleteDefinition,
  }
})
