<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { useFamilyStore } from '@/stores/family'
import { useTheme } from '@/composables/useTheme'
import { useImageToken } from '@/composables/useImageToken'
import { useDarkMode } from '@/composables/useDarkMode'
import Toast from 'primevue/toast'

const router = useRouter()
const auth = useAuthStore()
const admin = useAdminStore()
const familyStore = useFamilyStore()

// Activate dynamic theming (watches admin config for changes)
useTheme()
const { loadFromUser: loadDarkMode } = useDarkMode()

// Listen for maintenance mode events from API client
function onMaintenance() {
  if (!auth.isAdmin) {
    router.replace('/maintenance')
  }
}

onMounted(async () => {
  window.addEventListener('monteweb:maintenance', onMaintenance)

  // Load public tenant config (theme, module flags)
  await admin.fetchConfig()

  // Check for maintenance mode on startup
  if (admin.isModuleEnabled('maintenance') && !auth.isAuthenticated) {
    router.replace('/maintenance')
    return
  }

  // Restore session if token exists
  if (auth.isAuthenticated) {
    await auth.fetchUser()
    // Redirect non-admins during maintenance
    if (admin.isModuleEnabled('maintenance') && !auth.isAdmin) {
      router.replace('/maintenance')
      return
    }
    // Fetch short-lived image token for authenticated image access
    const { fetchImageToken } = useImageToken()
    await fetchImageToken()
    // Apply dark mode from user profile
    await loadDarkMode()
    // Pre-load family data so sidebar can check membership
    if (auth.canHaveFamily) {
      familyStore.fetchFamilies()
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('monteweb:maintenance', onMaintenance)
})
</script>

<template>
  <Toast />
  <router-view />
</template>
