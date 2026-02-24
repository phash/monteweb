<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'

const emit = defineEmits<{
  submit: [data: { question: string; options: string[]; multiple: boolean }]
  cancel: []
}>()

const { t } = useI18n()

const question = ref('')
const options = ref(['', ''])
const multiple = ref(false)

const canSubmit = computed(() =>
  question.value.trim().length > 0 &&
  options.value.filter(o => o.trim().length > 0).length >= 2
)

function addOption() {
  if (options.value.length < 10) {
    options.value.push('')
  }
}

function removeOption(index: number) {
  if (options.value.length > 2) {
    options.value.splice(index, 1)
  }
}

function submit() {
  if (!canSubmit.value) return
  emit('submit', {
    question: question.value.trim(),
    options: options.value.filter(o => o.trim().length > 0).map(o => o.trim()),
    multiple: multiple.value,
  })
  // Reset
  question.value = ''
  options.value = ['', '']
  multiple.value = false
}
</script>

<template>
  <div class="poll-composer">
    <div class="poll-composer-header">
      <i class="pi pi-chart-bar" />
      <strong>{{ t('poll.createPoll') }}</strong>
      <Button
        icon="pi pi-times"
        text
        severity="secondary"
        size="small"
        class="close-btn"
        :aria-label="t('common.close')"
        @click="emit('cancel')"
      />
    </div>

    <InputText
      v-model="question"
      :placeholder="t('poll.questionPlaceholder')"
      class="poll-question-input"
    />

    <div class="poll-options-list">
      <div v-for="(_, index) in options" :key="index" class="poll-option-row">
        <InputText
          v-model="options[index]"
          :placeholder="t('poll.optionPlaceholder', { n: index + 1 })"
          class="poll-option-input"
        />
        <Button
          v-if="options.length > 2"
          icon="pi pi-times"
          text
          severity="secondary"
          size="small"
          :aria-label="t('common.delete')"
          @click="removeOption(index)"
        />
      </div>
    </div>

    <Button
      v-if="options.length < 10"
      :label="t('poll.addOption')"
      icon="pi pi-plus"
      text
      size="small"
      @click="addOption"
    />

    <div class="poll-settings">
      <label class="multiple-label">
        <Checkbox v-model="multiple" :binary="true" />
        <span>{{ t('poll.multipleChoice') }}</span>
      </label>
    </div>

    <div class="poll-composer-actions">
      <Button
        :label="t('common.cancel')"
        text
        severity="secondary"
        size="small"
        @click="emit('cancel')"
      />
      <Button
        :label="t('poll.createPoll')"
        size="small"
        :disabled="!canSubmit"
        @click="submit"
      />
    </div>
  </div>
</template>

<style scoped>
.poll-composer {
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius);
  padding: 0.75rem;
  background: var(--mw-bg-hover);
}

.poll-composer-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  font-size: var(--mw-font-size-sm);
}

.close-btn {
  margin-left: auto;
}

.poll-question-input {
  width: 100%;
  margin-bottom: 0.5rem;
}

.poll-options-list {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  margin-bottom: 0.5rem;
}

.poll-option-row {
  display: flex;
  gap: 0.375rem;
  align-items: center;
}

.poll-option-input {
  flex: 1;
}

.poll-settings {
  margin: 0.5rem 0;
}

.multiple-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--mw-font-size-sm);
  cursor: pointer;
}

.poll-composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
</style>
