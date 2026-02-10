<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'

const { t } = useI18n()
const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="not-found-content">
      <i class="pi pi-exclamation-triangle icon" />
      <h1>{{ t('error.notFound') }}</h1>
      <p>{{ t('error.notFoundMessage') }}</p>
      <Button
        :label="t('error.backHome')"
        icon="pi pi-home"
        @click="router.push({ name: 'dashboard' })"
      />
    </div>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  padding: 2rem;
}

.not-found-content {
  text-align: center;
  max-width: 480px;
}

.icon {
  font-size: 4rem;
  color: var(--mw-text-muted);
  margin-bottom: 1.5rem;
}

h1 {
  font-size: var(--mw-font-size-xl);
  margin-bottom: 0.75rem;
}

p {
  color: var(--mw-text-secondary);
  margin-bottom: 1.5rem;
}
</style>
