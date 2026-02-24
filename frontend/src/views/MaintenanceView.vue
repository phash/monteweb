<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import Button from 'primevue/button'

const { t } = useI18n()
const router = useRouter()
const admin = useAdminStore()
const message = ref('')

onMounted(async () => {
  try {
    await admin.fetchConfig()
    if (admin.config && !admin.isModuleEnabled('maintenance')) {
      router.replace('/')
      return
    }
    message.value = admin.config?.maintenanceMessage || t('maintenance.defaultMessage')
  } catch {
    message.value = t('maintenance.defaultMessage')
  }
})

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="maintenance-page">
    <div class="maintenance-card">
      <i class="pi pi-wrench maintenance-icon" />
      <h1>{{ t('maintenance.title') }}</h1>
      <p class="maintenance-message">{{ message }}</p>
      <Button :label="t('maintenance.loginAsAdmin')" icon="pi pi-sign-in" text @click="goToLogin" />
    </div>
  </div>
</template>

<style scoped>
.maintenance-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--mw-bg);
  padding: 1rem;
}

.maintenance-card {
  text-align: center;
  max-width: 500px;
  padding: 3rem 2rem;
  background: var(--mw-bg-card);
  border-radius: var(--mw-border-radius-lg);
  box-shadow: var(--mw-shadow-lg);
}

.maintenance-icon {
  font-size: 4rem;
  color: var(--mw-warning);
  margin-bottom: 1.5rem;
}

h1 {
  font-size: 1.5rem;
  margin-bottom: 1rem;
  color: var(--mw-text);
}

.maintenance-message {
  color: var(--mw-text-secondary);
  margin-bottom: 2rem;
  line-height: 1.6;
}
</style>
