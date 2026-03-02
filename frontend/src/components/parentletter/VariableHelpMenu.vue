<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Popover from 'primevue/popover'

const { t } = useI18n()
const toast = useToast()

const emit = defineEmits<{
  insert: [variable: string]
}>()

const popoverRef = ref<InstanceType<typeof Popover> | null>(null)

interface TemplateVariable {
  token: string
  label: string
  description: string
}

const variables: TemplateVariable[] = [
  { token: '{Familie}', label: 'parentLetters.variables.familyLabel', description: 'parentLetters.variables.familyDesc' },
  { token: '{NameKind}', label: 'parentLetters.variables.childNameLabel', description: 'parentLetters.variables.childNameDesc' },
  { token: '{Anrede}', label: 'parentLetters.variables.salutationLabel', description: 'parentLetters.variables.salutationDesc' },
  { token: '{LehrerName}', label: 'parentLetters.variables.teacherNameLabel', description: 'parentLetters.variables.teacherNameDesc' },
]

function togglePopover(event: MouseEvent) {
  popoverRef.value?.toggle(event)
}

function copyAndInsert(variable: TemplateVariable) {
  emit('insert', variable.token)
  // Also copy to clipboard
  navigator.clipboard.writeText(variable.token).catch(() => {
    // Clipboard API may not be available in all contexts — silent fail
  })
  toast.add({
    severity: 'info',
    summary: t('parentLetters.variables.inserted', { token: variable.token }),
    life: 2000,
  })
  popoverRef.value?.hide()
}
</script>

<template>
  <span>
    <Button
      icon="pi pi-question-circle"
      text
      rounded
      size="small"
      severity="secondary"
      :aria-label="t('parentLetters.variables.helpTitle')"
      :title="t('parentLetters.variables.helpTitle')"
      @click="togglePopover"
    />
    <Popover ref="popoverRef">
      <div class="variable-menu">
        <p class="variable-title">{{ t('parentLetters.variables.helpTitle') }}</p>
        <p class="variable-subtitle">{{ t('parentLetters.variables.helpSubtitle') }}</p>
        <div
          v-for="v in variables"
          :key="v.token"
          class="variable-item"
        >
          <div class="variable-info">
            <code class="variable-token">{{ v.token }}</code>
            <span class="variable-desc">{{ t(v.description) }}</span>
          </div>
          <Button
            icon="pi pi-copy"
            text
            rounded
            size="small"
            :aria-label="t('parentLetters.variables.insert')"
            :title="t('parentLetters.variables.insert')"
            @click="copyAndInsert(v)"
          />
        </div>
      </div>
    </Popover>
  </span>
</template>

<style scoped>
.variable-menu {
  min-width: 280px;
  max-width: 360px;
}

.variable-title {
  font-weight: 700;
  font-size: var(--mw-font-size-sm);
  margin: 0 0 0.25rem;
}

.variable-subtitle {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin: 0 0 0.75rem;
}

.variable-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.4rem 0;
  border-bottom: 1px solid var(--mw-border);
}

.variable-item:last-child {
  border-bottom: none;
}

.variable-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 0;
}

.variable-token {
  font-family: 'Courier New', Courier, monospace;
  font-size: var(--mw-font-size-sm);
  background: var(--mw-bg-hover);
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  font-weight: 700;
  width: fit-content;
}

.variable-desc {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}
</style>
