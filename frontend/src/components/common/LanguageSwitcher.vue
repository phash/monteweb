<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import Select from 'primevue/select'

const { locale } = useI18n()

const languages = [
  { label: 'Deutsch', value: 'de' },
  { label: 'English', value: 'en' },
]

function changeLocale(event: { value: string }) {
  locale.value = event.value
  localStorage.setItem('monteweb-locale', event.value)
}
</script>

<template>
  <Select
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
