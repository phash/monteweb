<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { useTheme } from '@/composables/useTheme'

const auth = useAuthStore()
const admin = useAdminStore()

// Activate dynamic theming (watches admin config for changes)
useTheme()

onMounted(async () => {
  // Load public tenant config (theme, module flags)
  await admin.fetchConfig()

  // Restore session if token exists
  if (auth.isAuthenticated) {
    await auth.fetchUser()
  }
})
</script>

<template>
  <router-view />
</template>
