<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'

const { t } = useI18n()
const auth = useAuthStore()

async function handleStop() {
  await auth.stopImpersonation()
  window.location.reload()
}
</script>

<template>
  <div v-if="auth.isImpersonating" class="impersonation-banner">
    <i class="pi pi-exclamation-triangle" />
    <span>{{ t('auth.impersonation.banner', { name: auth.user?.displayName || '...' }) }}</span>
    <Button
      :label="t('auth.impersonation.stop')"
      icon="pi pi-sign-out"
      size="small"
      severity="contrast"
      @click="handleStop"
    />
  </div>
</template>

<style scoped>
.impersonation-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 0.5rem 1rem;
  background: var(--p-red-600, #dc2626);
  color: white;
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
  z-index: 1000;
}

.impersonation-banner i {
  font-size: 1rem;
}
</style>
