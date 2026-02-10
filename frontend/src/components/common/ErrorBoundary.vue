<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'

const { t } = useI18n()
const hasError = ref(false)

onErrorCaptured((err) => {
  console.error('ErrorBoundary caught:', err)
  hasError.value = true
  return false
})

function retry() {
  hasError.value = false
}
</script>

<template>
  <div v-if="hasError" class="error-boundary">
    <div class="error-content">
      <i class="pi pi-exclamation-circle icon" />
      <p>{{ t('error.unexpected') }}</p>
      <Button
        :label="t('common.back')"
        icon="pi pi-refresh"
        severity="secondary"
        @click="retry"
      />
    </div>
  </div>
  <slot v-else />
</template>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem 2rem;
}

.error-content {
  text-align: center;
  max-width: 400px;
}

.icon {
  font-size: 3rem;
  color: var(--mw-danger, #dc2626);
  margin-bottom: 1rem;
}

p {
  color: var(--mw-text-secondary);
  margin-bottom: 1.5rem;
}
</style>
