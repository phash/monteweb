<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Select from 'primevue/select'
import type { SchoolYearInfo } from '@/types/jobboard'

const props = defineProps<{
  modelValue: string | null
  options: SchoolYearInfo[]
}>()

const emit = defineEmits<{ 'update:modelValue': [value: string | null] }>()

const { t } = useI18n()

const value = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})
</script>

<template>
  <Select
    v-model="value"
    :options="props.options"
    option-label="name"
    option-value="id"
    :placeholder="t('jobboard.schoolYear')"
    class="school-year-select"
  />
</template>

<style scoped>
.school-year-select {
  min-width: 14rem;
}
</style>
