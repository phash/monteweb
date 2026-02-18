<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePwaInstall } from '@/composables/usePwaInstall'
import Button from 'primevue/button'

const { t } = useI18n()
const { isInstallable, isInstalled, dismissed, install, dismiss } = usePwaInstall()

const visible = computed(() => isInstallable.value && !isInstalled.value && !dismissed.value)
</script>

<template>
  <Transition name="slide-down">
    <div v-if="visible" class="install-banner">
      <div class="install-banner-content">
        <i class="pi pi-download install-icon" />
        <div class="install-text">
          <strong>{{ t('pwa.installTitle') }}</strong>
          <span>{{ t('pwa.installDescription') }}</span>
        </div>
      </div>
      <div class="install-actions">
        <Button :label="t('pwa.install')" size="small" @click="install" />
        <Button icon="pi pi-times" text severity="secondary" size="small" :aria-label="t('common.close')" @click="dismiss" />
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.install-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--mw-primary);
  color: var(--mw-primary-contrast);
  font-size: var(--mw-font-size-sm);
}

.install-banner-content {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}

.install-icon {
  font-size: 1.25rem;
  flex-shrink: 0;
}

.install-text {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.install-text strong {
  font-weight: 600;
}

.install-text span {
  opacity: 0.9;
  font-size: var(--mw-font-size-xs);
}

.install-actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-shrink: 0;
}

.install-actions :deep(.p-button) {
  white-space: nowrap;
}

.install-actions :deep(.p-button.p-button-text) {
  color: var(--mw-primary-contrast);
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

@media (max-width: 767px) {
  .install-text span {
    display: none;
  }
}
</style>
