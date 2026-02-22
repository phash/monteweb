<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { useFamilyStore } from '@/stores/family'
import { useTheme } from '@/composables/useTheme'
import { useImageToken } from '@/composables/useImageToken'
import Toast from 'primevue/toast'

const auth = useAuthStore()
const admin = useAdminStore()
const familyStore = useFamilyStore()

// Activate dynamic theming (watches admin config for changes)
useTheme()

onMounted(async () => {
  // Load public tenant config (theme, module flags)
  await admin.fetchConfig()

  // Restore session if token exists
  if (auth.isAuthenticated) {
    await auth.fetchUser()
    // Fetch short-lived image token for authenticated image access
    const { fetchImageToken } = useImageToken()
    await fetchImageToken()
    // Pre-load family data so sidebar can check membership
    if (auth.canHaveFamily) {
      familyStore.fetchFamilies()
    }
  }
})
</script>

<template>
  <Toast />
  <router-view />
</template>
