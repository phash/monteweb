<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import MentionInput from '@/components/common/MentionInput.vue'
import PollComposer from '@/components/common/PollComposer.vue'
import type { CreatePollRequest } from '@/types/feed'

const emit = defineEmits<{
  submit: [data: { title?: string; content?: string; poll?: CreatePollRequest; files?: File[] }]
}>()

const { t } = useI18n()
const title = ref('')
const content = ref('')
const submitting = ref(false)
const showPollComposer = ref(false)
const selectedFiles = ref<File[]>([])
const fileInput = ref<HTMLInputElement | null>(null)

const MAX_FILES = 10
const MAX_FILE_SIZE = 50 * 1024 * 1024 // 50 MB

const canSubmit = computed(() =>
  content.value.trim().length > 0 || showPollComposer.value || selectedFiles.value.length > 0
)

function openFilePicker() {
  fileInput.value?.click()
}

function onFilesSelected(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files) return
  const newFiles = Array.from(input.files)
  const total = selectedFiles.value.length + newFiles.length
  if (total > MAX_FILES) {
    // Only add files up to the limit
    const allowed = MAX_FILES - selectedFiles.value.length
    selectedFiles.value.push(...newFiles.slice(0, allowed))
  } else {
    selectedFiles.value.push(...newFiles)
  }
  // Reset input so the same file can be re-selected
  input.value = ''
}

function removeFile(index: number) {
  selectedFiles.value.splice(index, 1)
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function getFileIcon(file: File): string {
  const type = file.type
  if (type.startsWith('image/')) return 'pi pi-image'
  if (type === 'application/pdf') return 'pi pi-file-pdf'
  if (type.startsWith('video/')) return 'pi pi-video'
  if (type.startsWith('audio/')) return 'pi pi-volume-up'
  return 'pi pi-file'
}

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    emit('submit', {
      title: title.value.trim() || undefined,
      content: content.value.trim() || undefined,
      files: selectedFiles.value.length > 0 ? [...selectedFiles.value] : undefined,
    })
    title.value = ''
    content.value = ''
    selectedFiles.value = []
  } finally {
    submitting.value = false
  }
}

function onPollSubmit(data: { question: string; options: string[]; multiple: boolean }) {
  submitting.value = true
  try {
    emit('submit', {
      title: title.value.trim() || undefined,
      content: content.value.trim() || undefined,
      poll: {
        question: data.question,
        options: data.options,
        multiple: data.multiple,
      },
      files: selectedFiles.value.length > 0 ? [...selectedFiles.value] : undefined,
    })
    title.value = ''
    content.value = ''
    showPollComposer.value = false
    selectedFiles.value = []
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="post-composer card">
    <InputText
      v-model="title"
      :placeholder="t('feed.titlePlaceholder')"
      :aria-label="t('feed.titlePlaceholder')"
      class="composer-title"
    />
    <MentionInput
      v-model="content"
      :placeholder="t('feed.contentPlaceholder')"
      :autoResize="true"
      :rows="3"
      class="composer-content"
    />

    <PollComposer
      v-if="showPollComposer"
      @submit="onPollSubmit"
      @cancel="showPollComposer = false"
    />

    <!-- Selected files list -->
    <div v-if="selectedFiles.length" class="selected-files">
      <div v-for="(file, index) in selectedFiles" :key="index" class="file-chip">
        <i :class="getFileIcon(file)" />
        <span class="file-chip-name">{{ file.name }}</span>
        <span class="file-chip-size">{{ formatFileSize(file.size) }}</span>
        <span
          v-if="file.size > MAX_FILE_SIZE"
          class="file-chip-error"
        >{{ t('feed.fileTooLarge') }}</span>
        <Button
          icon="pi pi-times"
          text
          rounded
          severity="danger"
          size="small"
          :aria-label="t('feed.removeFile')"
          @click="removeFile(index)"
          class="file-chip-remove"
        />
      </div>
    </div>

    <input
      ref="fileInput"
      type="file"
      multiple
      class="hidden-file-input"
      @change="onFilesSelected"
    />

    <div class="composer-actions">
      <div class="composer-actions-left">
        <Button
          v-if="!showPollComposer"
          icon="pi pi-paperclip"
          :aria-label="t('feed.attachFiles')"
          text
          severity="secondary"
          size="small"
          @click="openFilePicker"
        />
        <Button
          v-if="!showPollComposer"
          icon="pi pi-chart-bar"
          :aria-label="t('poll.createPoll')"
          text
          severity="secondary"
          size="small"
          @click="showPollComposer = true"
        />
      </div>
      <Button
        v-if="!showPollComposer"
        :label="t('feed.post')"
        icon="pi pi-send"
        :loading="submitting"
        :disabled="!canSubmit"
        @click="submit"
        size="small"
      />
    </div>
  </div>
</template>

<style scoped>
.post-composer {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.composer-title,
.composer-content {
  width: 100%;
}

.composer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.composer-actions-left {
  display: flex;
  gap: 0.25rem;
}

.hidden-file-input {
  display: none;
}

.selected-files {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.file-chip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.5rem;
  background: var(--mw-bg, #f8f9fa);
  border: 1px solid var(--mw-border-light, #dee2e6);
  border-radius: var(--mw-border-radius-sm, 4px);
  font-size: var(--mw-font-size-sm, 0.875rem);
}

.file-chip i {
  color: var(--mw-text-muted);
  flex-shrink: 0;
}

.file-chip-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-chip-size {
  color: var(--mw-text-muted);
  flex-shrink: 0;
  font-size: var(--mw-font-size-xs, 0.75rem);
}

.file-chip-error {
  color: var(--mw-danger, #ef4444);
  font-size: var(--mw-font-size-xs, 0.75rem);
  flex-shrink: 0;
}

.file-chip-remove {
  flex-shrink: 0;
}
</style>
