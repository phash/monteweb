<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import ProgressSpinner from 'primevue/progressspinner'
import Button from 'primevue/button'

const props = withDefaults(defineProps<{
  timeout?: number
}>(), {
  timeout: 10000,
})

const emit = defineEmits<{
  retry: []
}>()

const { t } = useI18n()
const showTimeout = ref(false)
let timer: ReturnType<typeof setTimeout> | null = null

onMounted(() => {
  if (props.timeout > 0) {
    timer = setTimeout(() => {
      showTimeout.value = true
    }, props.timeout)
  }
})

onUnmounted(() => {
  if (timer) clearTimeout(timer)
})

function handleRetry() {
  showTimeout.value = false
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => {
    showTimeout.value = true
  }, props.timeout)
  emit('retry')
}
</script>

<template>
  <div class="loading-wrapper" role="status" :aria-label="t('common.loading')">
    <ProgressSpinner style="width: 40px; height: 40px" aria-hidden="true" />
    <p v-if="showTimeout" class="timeout-message">
      {{ t('common.loadingTimeout') }}
      <Button
        :label="t('common.retry')"
        icon="pi pi-refresh"
        text
        size="small"
        @click="handleRetry"
      />
    </p>
  </div>
</template>

<style scoped>
.loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  gap: 1rem;
}

.timeout-message {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}
</style>
