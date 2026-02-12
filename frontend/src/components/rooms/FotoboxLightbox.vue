<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { fotoboxApi } from '@/api/fotobox.api'
import type { FotoboxImageInfo } from '@/types/fotobox'
import Button from 'primevue/button'

const props = defineProps<{
  images: FotoboxImageInfo[]
  currentIndex: number
  visible: boolean
}>()
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'update:currentIndex', value: number): void
}>()

const { t } = useI18n()

const currentImage = computed(() => props.images[props.currentIndex] ?? null)

function next() {
  if (props.currentIndex < props.images.length - 1) {
    emit('update:currentIndex', props.currentIndex + 1)
  }
}

function prev() {
  if (props.currentIndex > 0) {
    emit('update:currentIndex', props.currentIndex - 1)
  }
}

function close() {
  emit('update:visible', false)
}

function onKeydown(e: KeyboardEvent) {
  if (!props.visible) return
  if (e.key === 'Escape') close()
  if (e.key === 'ArrowRight') next()
  if (e.key === 'ArrowLeft') prev()
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <Teleport to="body">
    <div v-if="visible && currentImage" class="lightbox-overlay" @click.self="close">
      <div class="lightbox-content">
        <Button
          icon="pi pi-times"
          severity="secondary"
          text
          rounded
          class="close-btn"
          :aria-label="t('fotobox.close')"
          @click="close"
        />

        <Button
          v-if="currentIndex > 0"
          icon="pi pi-chevron-left"
          severity="secondary"
          text
          rounded
          class="nav-btn nav-left"
          :aria-label="t('fotobox.previous')"
          @click="prev"
        />

        <img
          :src="fotoboxApi.imageUrl(currentImage.id)"
          :alt="currentImage.caption || currentImage.originalFilename"
          class="lightbox-image"
        />

        <Button
          v-if="currentIndex < images.length - 1"
          icon="pi pi-chevron-right"
          severity="secondary"
          text
          rounded
          class="nav-btn nav-right"
          :aria-label="t('fotobox.next')"
          @click="next"
        />

        <div class="lightbox-footer">
          <p v-if="currentImage.caption" class="lightbox-caption">{{ currentImage.caption }}</p>
          <span class="lightbox-counter">
            {{ currentIndex + 1 }} {{ t('fotobox.of') }} {{ images.length }}
          </span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.lightbox-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.9);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lightbox-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.lightbox-image {
  max-width: 90vw;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 4px;
}

.close-btn {
  position: fixed;
  top: 1rem;
  right: 1rem;
  color: white !important;
  z-index: 10;
}

.nav-btn {
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  color: white !important;
  font-size: 1.5rem;
  z-index: 10;
}

.nav-left {
  left: 1rem;
}

.nav-right {
  right: 1rem;
}

.lightbox-footer {
  margin-top: 0.75rem;
  text-align: center;
  color: white;
}

.lightbox-caption {
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}

.lightbox-counter {
  font-size: 0.75rem;
  opacity: 0.7;
}
</style>
