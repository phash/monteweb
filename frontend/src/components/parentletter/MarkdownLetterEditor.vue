<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMarkdown } from '@/composables/useMarkdown'

const props = defineProps<{
  modelValue: string
  userName: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t } = useI18n()
const { renderMarkdown, resolveVariablesPreview } = useMarkdown()
const textareaRef = ref<HTMLTextAreaElement | null>(null)

const previewHtml = computed(() => {
  const resolved = resolveVariablesPreview(props.modelValue, props.userName)
  return renderMarkdown(resolved)
})

function onInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
}

function insertAtCursor(text: string) {
  const textarea = textareaRef.value
  if (!textarea) return
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const before = props.modelValue.substring(0, start)
  const after = props.modelValue.substring(end)
  emit('update:modelValue', before + text + after)
  requestAnimationFrame(() => {
    textarea.selectionStart = textarea.selectionEnd = start + text.length
    textarea.focus()
  })
}

defineExpose({ insertAtCursor, textareaRef })
</script>

<template>
  <div class="markdown-editor">
    <div class="editor-toolbar">
      <span class="toolbar-label">{{ t('parentLetters.content') }}</span>
      <span class="toolbar-preview-label">{{ t('parentLetters.preview') }}</span>
    </div>
    <div class="editor-split">
      <div class="editor-pane">
        <textarea
          ref="textareaRef"
          :value="modelValue"
          :placeholder="placeholder"
          class="editor-textarea"
          rows="15"
          @input="onInput"
        />
      </div>
      <div class="preview-pane" v-html="previewHtml" />
    </div>
  </div>
</template>

<style scoped>
.markdown-editor {
  border: 1px solid var(--mw-border-light);
  border-radius: 6px;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 1rem;
  background: var(--mw-bg-subtle);
  border-bottom: 1px solid var(--mw-border-light);
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
}

.editor-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 300px;
}

.editor-pane {
  border-right: 1px solid var(--mw-border-light);
}

.editor-textarea {
  width: 100%;
  height: 100%;
  border: none;
  outline: none;
  resize: none;
  padding: 1rem;
  font-family: monospace;
  font-size: var(--mw-font-size-sm);
  background: var(--mw-bg);
  color: var(--mw-text);
}

.preview-pane {
  padding: 1rem;
  overflow-y: auto;
  max-height: 500px;
  line-height: 1.6;
}

.preview-pane :deep(h1) { font-size: 1.5rem; margin: 0 0 0.5rem; }
.preview-pane :deep(h2) { font-size: 1.25rem; margin: 0 0 0.5rem; }
.preview-pane :deep(h3) { font-size: 1.1rem; margin: 0 0 0.5rem; }
.preview-pane :deep(p) { margin: 0 0 0.75rem; }
.preview-pane :deep(ul) { padding-left: 1.5rem; }
.preview-pane :deep(blockquote) {
  border-left: 3px solid var(--mw-primary);
  padding-left: 1rem;
  color: var(--mw-text-secondary);
}

@media (max-width: 768px) {
  .editor-split {
    grid-template-columns: 1fr;
  }

  .editor-pane {
    border-right: none;
    border-bottom: 1px solid var(--mw-border-light);
  }
}
</style>
