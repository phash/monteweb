<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const isOffline = ref(!navigator.onLine)

function onOffline() {
  isOffline.value = true
}

function onOnline() {
  isOffline.value = false
}

onMounted(() => {
  window.addEventListener('offline', onOffline)
  window.addEventListener('online', onOnline)
})

onUnmounted(() => {
  window.removeEventListener('offline', onOffline)
  window.removeEventListener('online', onOnline)
})
</script>

<template>
  <Transition name="slide-down">
    <div v-if="isOffline" class="offline-banner" role="alert">
      <i class="pi pi-wifi-off" />
      <span>{{ t('pwa.offline') }}</span>
    </div>
  </Transition>
</template>

<style scoped>
.offline-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--mw-warning);
  color: var(--mw-secondary-contrast);
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
}

.offline-banner i {
  font-size: 1rem;
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
</style>
