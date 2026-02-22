<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'

const { t } = useI18n()

const props = withDefaults(defineProps<{
  imageUrl?: string | null
  size?: 'sm' | 'md' | 'lg'
  icon?: string
  editable?: boolean
}>(), {
  imageUrl: null,
  size: 'md',
  icon: 'pi-user',
  editable: false,
})

const emit = defineEmits<{
  upload: [file: File]
  remove: []
}>()

const fileInput = ref<HTMLInputElement>()

const sizeMap = { sm: '48px', md: '80px', lg: '120px' }
const iconSizeMap = { sm: '1.25rem', md: '2rem', lg: '3rem' }

function openFilePicker() {
  if (!props.editable) return
  fileInput.value?.click()
}

function onFileSelected(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) return
  if (file.size > 2 * 1024 * 1024) return
  emit('upload', file)
  target.value = ''
}
</script>

<template>
  <div class="avatar-upload" :class="[`avatar-${size}`, { editable }]">
    <div class="avatar-circle" :style="{ width: sizeMap[size], height: sizeMap[size] }" tabindex="0" role="button" :aria-label="t('common.changeAvatar')" @click="openFilePicker" @keydown.enter="openFilePicker" @keydown.space.prevent="openFilePicker">
      <img v-if="imageUrl" :src="imageUrl" alt="Avatar" class="avatar-img" />
      <i v-else :class="`pi ${icon}`" :style="{ fontSize: iconSizeMap[size] }" />
      <div v-if="editable" class="avatar-overlay">
        <i class="pi pi-camera" />
      </div>
    </div>
    <Button
      v-if="editable && imageUrl"
      icon="pi pi-trash"
      :label="t('common.removeAvatar')"
      severity="danger"
      text
      size="small"
      @click="emit('remove')"
    />
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      class="hidden-input"
      @change="onFileSelected"
    />
  </div>
</template>

<style scoped>
.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.avatar-circle {
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--mw-bg);
  border: 2px solid var(--mw-border-light);
  color: var(--mw-text-muted);
  position: relative;
  flex-shrink: 0;
}

.editable .avatar-circle {
  cursor: pointer;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  color: white;
  opacity: 0;
  transition: opacity 0.15s;
  border-radius: 50%;
}

.editable .avatar-circle:hover .avatar-overlay {
  opacity: 1;
}

.editable .avatar-circle:focus-visible {
  outline: 2px solid var(--mw-primary, #4f46e5);
  outline-offset: 2px;
}

.editable .avatar-circle:focus-visible .avatar-overlay {
  opacity: 1;
}

.hidden-input {
  display: none;
}
</style>
