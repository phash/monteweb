<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useFotoboxStore } from '@/stores/fotobox'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import ProgressBar from 'primevue/progressbar'

const props = defineProps<{
  roomId: string
  threadId: string
  maxFileSizeMb?: number
}>()
const emit = defineEmits<{ (e: 'uploaded'): void }>()

const { t } = useI18n()
const fotobox = useFotoboxStore()
const toast = useToast()

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const dragging = ref(false)
const uploading = ref(false)
const selectedFiles = ref<File[]>([])
const previews = ref<string[]>([])
const caption = ref('')

function onDragOver(e: DragEvent) {
  e.preventDefault()
  dragging.value = true
}

function onDragLeave() {
  dragging.value = false
}

function onDrop(e: DragEvent) {
  e.preventDefault()
  dragging.value = false
  if (e.dataTransfer?.files) {
    addFiles(Array.from(e.dataTransfer.files))
  }
}

function onFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files) {
    addFiles(Array.from(input.files))
    input.value = ''
  }
}

function addFiles(files: File[]) {
  const maxSize = (props.maxFileSizeMb || 10) * 1024 * 1024
  for (const file of files) {
    if (!ALLOWED_TYPES.includes(file.type)) {
      toast.add({ severity: 'warn', summary: t('fotobox.invalidFileType'), life: 5000 })
      continue
    }
    if (file.size > maxSize) {
      toast.add({
        severity: 'warn',
        summary: t('fotobox.fileTooLarge', { max: props.maxFileSizeMb || 10 }),
        life: 5000,
      })
      continue
    }
    selectedFiles.value.push(file)
    const url = URL.createObjectURL(file)
    previews.value.push(url)
  }
}

function removeFile(index: number) {
  const url = previews.value[index]
  if (url) URL.revokeObjectURL(url)
  selectedFiles.value.splice(index, 1)
  previews.value.splice(index, 1)
}

async function upload() {
  if (!selectedFiles.value.length) return
  uploading.value = true
  try {
    await fotobox.uploadImages(
      props.roomId,
      props.threadId,
      selectedFiles.value,
      caption.value || undefined,
    )
    toast.add({
      severity: 'success',
      summary: t('fotobox.uploadSuccess', { count: selectedFiles.value.length }),
      life: 3000,
    })
    // Cleanup
    previews.value.forEach((url) => URL.revokeObjectURL(url))
    selectedFiles.value = []
    previews.value = []
    caption.value = ''
    emit('uploaded')
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Upload failed', life: 5000 })
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="uploader">
    <div
      class="drop-zone"
      :class="{ dragging }"
      @dragover="onDragOver"
      @dragleave="onDragLeave"
      @drop="onDrop"
      @click="($refs.fileInput as HTMLInputElement)?.click()"
    >
      <i class="pi pi-images drop-icon" />
      <p>{{ t('fotobox.dragDropHint') }}</p>
      <input
        ref="fileInput"
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        multiple
        class="hidden-input"
        @change="onFileSelect"
      />
    </div>

    <div v-if="previews.length" class="preview-grid">
      <div v-for="(preview, index) in previews" :key="index" class="preview-item">
        <img :src="preview" alt="" class="preview-img" />
        <Button
          icon="pi pi-times"
          severity="danger"
          text
          rounded
          size="small"
          class="remove-btn"
          @click.stop="removeFile(index)"
        />
      </div>
    </div>

    <div v-if="selectedFiles.length" class="upload-actions">
      <InputText
        v-model="caption"
        :placeholder="t('fotobox.caption')"
        class="caption-input"
      />
      <Button
        :label="t('fotobox.uploadImages') + ` (${selectedFiles.length})`"
        icon="pi pi-upload"
        :loading="uploading"
        @click="upload"
      />
    </div>

    <ProgressBar v-if="uploading" mode="indeterminate" class="upload-progress" />
  </div>
</template>

<style scoped>
.drop-zone {
  border: 2px dashed var(--mw-border-light);
  border-radius: var(--mw-border-radius-md, 8px);
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.drop-zone:hover,
.drop-zone.dragging {
  border-color: var(--mw-primary);
  background: var(--mw-bg-hover);
}

.drop-icon {
  font-size: 2rem;
  color: var(--mw-text-muted);
  margin-bottom: 0.5rem;
}

.drop-zone p {
  color: var(--mw-text-secondary);
  font-size: var(--mw-font-size-sm);
}

.hidden-input {
  display: none;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 0.5rem;
  margin-top: 1rem;
}

.preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--mw-border-radius-sm, 4px);
  overflow: hidden;
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 0.25rem;
  right: 0.25rem;
}

.upload-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
  align-items: center;
}

.caption-input {
  flex: 1;
}

.upload-progress {
  margin-top: 0.5rem;
}
</style>
