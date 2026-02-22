<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { privacyApi } from '@/api/privacy.api'

const { t } = useI18n()
const policy = ref<{ text: string | null; version: string | null }>({ text: null, version: null })
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await privacyApi.getPrivacyPolicy()
    policy.value = res.data.data
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="privacy-policy-view">
    <h1 class="privacy-title">{{ t('privacy.privacyPolicy') }}</h1>
    <div v-if="loading" class="text-center py-8">
      <i class="pi pi-spin pi-spinner text-2xl" />
    </div>
    <div v-else-if="policy.text" class="prose privacy-content" v-html="policy.text" />
    <p v-else class="text-muted">{{ t('privacy.noPrivacyPolicy') }}</p>
    <p v-if="policy.version" class="privacy-version">Version {{ policy.version }}</p>
  </div>
</template>

<style scoped>
.privacy-policy-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 1rem;
}

.privacy-title {
  font-size: var(--mw-font-size-2xl);
  font-weight: 700;
  color: var(--mw-text);
  margin: 0 0 1.5rem;
}

.privacy-content {
  line-height: 1.7;
  color: var(--mw-text-secondary);
  font-size: var(--mw-font-size-sm);
}

.privacy-content :deep(h1),
.privacy-content :deep(h2),
.privacy-content :deep(h3) {
  color: var(--mw-text);
  margin-top: 1.5rem;
  margin-bottom: 0.5rem;
}

.privacy-content :deep(p) {
  margin-bottom: 0.75rem;
}

.privacy-content :deep(ul),
.privacy-content :deep(ol) {
  margin-bottom: 0.75rem;
  padding-left: 1.5rem;
}

.text-muted {
  color: var(--mw-text-muted);
}

.privacy-version {
  font-size: var(--mw-font-size-xs, 0.75rem);
  color: var(--mw-text-muted);
  margin-top: 2rem;
}
</style>
