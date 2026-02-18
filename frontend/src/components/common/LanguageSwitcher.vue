<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useAdminStore } from '@/stores/admin'
import { computed, watch } from 'vue'
import Select from 'primevue/select'

const { locale } = useI18n()
const adminStore = useAdminStore()

const multilanguageEnabled = computed(() => adminStore.config?.multilanguageEnabled ?? true)

const languages = [
  { label: 'Deutsch', value: 'de' },
  { label: 'English', value: 'en' },
]

function changeLocale(event: { value: string }) {
  locale.value = event.value
  localStorage.setItem('monteweb-locale', event.value)
}

// When multilanguage is disabled, force locale to default language from config
watch(multilanguageEnabled, (enabled) => {
  if (!enabled && adminStore.config?.defaultLanguage) {
    locale.value = adminStore.config.defaultLanguage
    localStorage.setItem('monteweb-locale', adminStore.config.defaultLanguage)
  }
})
</script>

<template>
  <Select
    v-if="multilanguageEnabled"
    :modelValue="locale"
    :options="languages"
    optionLabel="label"
    optionValue="value"
    class="lang-select"
    @change="changeLocale"
  />
</template>

<style scoped>
.lang-select {
  width: 7rem;
}

.lang-select :deep(.p-select-label) {
  padding: 0.25rem 0.5rem;
  font-size: var(--mw-font-size-xs);
}
</style>
