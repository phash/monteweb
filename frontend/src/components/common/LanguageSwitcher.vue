<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useAdminStore } from '@/stores/admin'
import { computed, watch } from 'vue'
import Select from 'primevue/select'

const { locale } = useI18n()
const adminStore = useAdminStore()

const allLanguages = [
  { label: 'Deutsch', value: 'de' },
  { label: 'English', value: 'en' },
]

const availableLanguages = computed(() => {
  const available = adminStore.config?.availableLanguages ?? ['de', 'en']
  return allLanguages.filter(l => available.includes(l.value))
})

const showSwitcher = computed(() => availableLanguages.value.length > 1)

function changeLocale(event: { value: string }) {
  locale.value = event.value
  localStorage.setItem('monteweb-locale', event.value)
}

// When available languages change and current locale is not available, reset to default
watch(availableLanguages, (langs) => {
  if (langs.length > 0 && !langs.some(l => l.value === locale.value)) {
    const defaultLang = adminStore.config?.defaultLanguage ?? 'de'
    locale.value = defaultLang
    localStorage.setItem('monteweb-locale', defaultLang)
  }
})
</script>

<template>
  <Select
    v-if="showSwitcher"
    :modelValue="locale"
    :options="availableLanguages"
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
