<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import { privacyApi } from '@/api/privacy.api'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { markTermsAccepted } from '@/utils/termsCache'

const { t } = useI18n()
const toast = useToast()
const router = useRouter()
const authStore = useAuthStore()

const terms = ref<{ text: string | null; version: string | null }>({ text: null, version: null })
const termsAccepted = ref(false)
const loading = ref(true)

onMounted(async () => {
  try {
    const [termsRes, statusRes] = await Promise.all([
      privacyApi.getTerms(),
      authStore.isAuthenticated ? privacyApi.getTermsStatus() : Promise.resolve(null),
    ])
    terms.value = termsRes.data.data
    if (statusRes) {
      termsAccepted.value = statusRes.data.data.accepted
    }
  } finally {
    loading.value = false
  }
})

async function handleAccept() {
  try {
    await privacyApi.acceptTerms()
    termsAccepted.value = true
    markTermsAccepted()
    toast.add({ severity: 'success', summary: t('privacy.termsAccepted'), life: 3000 })
    if (authStore.isAuthenticated) {
      setTimeout(() => router.push('/'), 1000)
    }
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 3000 })
  }
}
</script>

<template>
  <div class="terms-view">
    <h1 class="terms-title">{{ t('privacy.termsOfService') }}</h1>
    <div v-if="loading" class="text-center py-8">
      <i class="pi pi-spin pi-spinner text-2xl" />
    </div>
    <template v-else>
      <div v-if="terms.text" class="prose terms-content" v-html="terms.text" />
      <p v-else class="text-muted">{{ t('privacy.noTermsConfigured') }}</p>
      <p v-if="terms.version" class="terms-version">Version {{ terms.version }}</p>
      <div v-if="authStore.isAuthenticated && terms.text && !termsAccepted" class="terms-actions">
        <Button :label="t('privacy.acceptTerms')" @click="handleAccept" />
      </div>
      <div v-if="termsAccepted" class="terms-accepted">
        <i class="pi pi-check" />
        {{ t('privacy.termsAccepted') }}
      </div>
    </template>
  </div>
</template>

<style scoped>
.terms-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 1rem;
}

.terms-title {
  font-size: var(--mw-font-size-2xl);
  font-weight: 700;
  color: var(--mw-text);
  margin: 0 0 1.5rem;
}

.terms-content {
  line-height: 1.7;
  color: var(--mw-text-secondary);
  font-size: var(--mw-font-size-sm);
}

.terms-content :deep(h1),
.terms-content :deep(h2),
.terms-content :deep(h3) {
  color: var(--mw-text);
  margin-top: 1.5rem;
  margin-bottom: 0.5rem;
}

.terms-content :deep(p) {
  margin-bottom: 0.75rem;
}

.terms-content :deep(ul),
.terms-content :deep(ol) {
  margin-bottom: 0.75rem;
  padding-left: 1.5rem;
}

.text-muted {
  color: var(--mw-text-muted);
}

.terms-version {
  font-size: var(--mw-font-size-xs, 0.75rem);
  color: var(--mw-text-muted);
  margin-top: 2rem;
}

.terms-actions {
  margin-top: 1.5rem;
}

.terms-accepted {
  margin-top: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--p-green-600);
  font-weight: 500;
  font-size: var(--mw-font-size-sm);
}
</style>
