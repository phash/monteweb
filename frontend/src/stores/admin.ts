import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminApi } from '@/api/admin.api'
import type { TenantConfig } from '@/types/family'

export const useAdminStore = defineStore('admin', () => {
  const config = ref<TenantConfig | null>(null)
  const loading = ref(false)

  async function fetchConfig() {
    loading.value = true
    try {
      const res = await adminApi.getPublicConfig()
      config.value = res.data.data
    } catch {
      // Config not available (not authenticated) - use defaults
    } finally {
      loading.value = false
    }
  }

  async function fetchAdminConfig() {
    loading.value = true
    try {
      const res = await adminApi.getConfig()
      config.value = res.data.data
    } catch {
      // Fallback to public config
      await fetchConfig()
    } finally {
      loading.value = false
    }
  }

  async function updateConfig(data: Parameters<typeof adminApi.updateConfig>[0]) {
    try {
      const res = await adminApi.updateConfig(data)
      config.value = res.data.data
    } catch (e) {
      console.error('Failed to update config:', e)
      throw e
    }
  }

  async function updateModules(modules: Record<string, boolean>) {
    try {
      const res = await adminApi.updateModules(modules)
      config.value = res.data.data
    } catch (e) {
      console.error('Failed to update modules:', e)
      throw e
    }
  }

  async function updateTheme(theme: Record<string, string>) {
    try {
      const res = await adminApi.updateTheme(theme)
      config.value = res.data.data
    } catch (e) {
      console.error('Failed to update theme:', e)
      throw e
    }
  }

  function isModuleEnabled(name: string): boolean {
    return config.value?.modules[name] ?? false
  }

  return {
    config,
    loading,
    fetchConfig,
    fetchAdminConfig,
    updateConfig,
    updateModules,
    updateTheme,
    isModuleEnabled,
  }
})
