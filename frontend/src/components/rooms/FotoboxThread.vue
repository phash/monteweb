<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useFotoboxStore } from '@/stores/fotobox'
import { useAuthStore } from '@/stores/auth'
import { fotoboxApi } from '@/api/fotobox.api'
import type { FotoboxPermissionLevel } from '@/types/fotobox'
import FotoboxImageUploader from './FotoboxImageUploader.vue'
import FotoboxLightbox from './FotoboxLightbox.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import { useToast } from 'primevue/usetoast'
import { useConfirmDialog } from '@/composables/useConfirmDialog'

const props = defineProps<{
  roomId: string
  threadId: string
  permission: FotoboxPermissionLevel
  isLeader: boolean
  maxFileSizeMb?: number
}>()
const emit = defineEmits<{ (e: 'back'): void }>()

const { t } = useI18n()
const { formatCompactDateTime } = useLocaleDate()
const fotobox = useFotoboxStore()
const auth = useAuthStore()
const toast = useToast()
const { visible: confirmVisible, header: confirmHeader, message: confirmMessage, confirm: confirmDialog, onConfirm, onCancel } = useConfirmDialog()

const showUploader = ref(false)
const lightboxVisible = ref(false)
const lightboxIndex = ref(0)

const canUpload = computed(() =>
  props.permission === 'POST_IMAGES' || props.permission === 'CREATE_THREADS',
)

onMounted(async () => {
  await fotobox.fetchThread(props.roomId, props.threadId)
  await fotobox.fetchImages(props.roomId, props.threadId)
})

function openLightbox(index: number) {
  lightboxIndex.value = index
  lightboxVisible.value = true
}

async function onUploaded() {
  showUploader.value = false
  await fotobox.fetchImages(props.roomId, props.threadId)
}

async function deleteImage(imageId: string) {
  const ok = await confirmDialog({ header: t('common.confirmDeleteTitle'), message: t('fotobox.confirmDeleteImage') })
  if (!ok) return
  try {
    await fotobox.deleteImage(imageId)
    toast.add({ severity: 'success', summary: t('fotobox.deleteImage'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

function canDeleteImage(uploadedBy: string): boolean {
  return uploadedBy === auth.user?.id || props.isLeader
}

function formatDate(date: string) {
  return formatCompactDateTime(date)
}
</script>

<template>
  <div>
    <div class="thread-header">
      <Button
        icon="pi pi-arrow-left"
        :label="t('fotobox.back')"
        severity="secondary"
        text
        size="small"
        @click="emit('back')"
      />
      <Button
        v-if="canUpload"
        :label="t('fotobox.uploadImages')"
        icon="pi pi-upload"
        size="small"
        @click="showUploader = !showUploader"
      />
    </div>

    <div v-if="fotobox.currentThread" class="thread-info">
      <h3>{{ fotobox.currentThread.title }}</h3>
      <p v-if="fotobox.currentThread.description" class="thread-desc">
        {{ fotobox.currentThread.description }}
      </p>
      <span class="thread-meta">
        {{ fotobox.currentThread.createdByName }} · {{ formatDate(fotobox.currentThread.createdAt) }}
      </span>
    </div>

    <FotoboxImageUploader
      v-if="showUploader"
      :roomId="roomId"
      :threadId="threadId"
      :maxFileSizeMb="maxFileSizeMb"
      @uploaded="onUploaded"
      class="mb-1"
    />

    <LoadingSpinner v-if="fotobox.loading && !fotobox.images.length" />
    <EmptyState
      v-else-if="!fotobox.images.length"
      icon="pi pi-images"
      :message="t('fotobox.noImages')"
    />
    <div v-else class="image-grid">
      <div
        v-for="(image, index) in fotobox.images"
        :key="image.id"
        class="image-card"
      >
        <img
          :src="fotoboxApi.thumbnailUrl(image.id)"
          :alt="image.caption || image.originalFilename"
          class="image-thumb"
          @click="openLightbox(index)"
        />
        <div class="image-overlay">
          <span v-if="image.caption" class="image-caption">{{ image.caption }}</span>
          <Button
            v-if="canDeleteImage(image.uploadedBy)"
            icon="pi pi-trash"
            severity="danger"
            text
            rounded
            size="small"
            class="image-delete-btn"
            @click.stop="deleteImage(image.id)"
          />
        </div>
      </div>
    </div>

    <FotoboxLightbox
      :images="fotobox.images"
      v-model:currentIndex="lightboxIndex"
      v-model:visible="lightboxVisible"
    />

    <Dialog v-model:visible="confirmVisible" :header="confirmHeader" modal :style="{ width: '400px', maxWidth: '90vw' }">
      <p>{{ confirmMessage }}</p>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" text @click="onCancel" />
        <Button :label="t('common.delete')" severity="danger" @click="onConfirm" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.thread-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.thread-info {
  margin-bottom: 1rem;
}

.thread-info h3 {
  margin: 0 0 0.25rem;
}

.thread-desc {
  color: var(--mw-text-secondary);
  font-size: var(--mw-font-size-sm);
  margin-bottom: 0.25rem;
}

.thread-meta {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.mb-1 {
  margin-bottom: 1rem;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 0.5rem;
}

.image-card {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--mw-border-radius-sm, 4px);
  overflow: hidden;
  cursor: pointer;
}

.image-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s;
}

.image-card:hover .image-thumb {
  transform: scale(1.05);
}

.image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.6));
  padding: 0.5rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-card:hover .image-overlay {
  opacity: 1;
}

.image-caption {
  color: white;
  font-size: var(--mw-font-size-xs);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.image-delete-btn {
  flex-shrink: 0;
}
</style>
