<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Textarea from 'primevue/textarea'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'

const emit = defineEmits<{
  submit: [data: { title?: string; content: string }]
}>()

const { t } = useI18n()
const title = ref('')
const content = ref('')
const submitting = ref(false)

async function submit() {
  if (!content.value.trim()) return
  submitting.value = true
  try {
    emit('submit', {
      title: title.value.trim() || undefined,
      content: content.value.trim(),
    })
    title.value = ''
    content.value = ''
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
    <Textarea
      v-model="content"
      :placeholder="t('feed.contentPlaceholder')"
      :aria-label="t('feed.contentPlaceholder')"
      :autoResize="true"
      rows="3"
      class="composer-content"
    />
    <div class="composer-actions">
      <Button
        :label="t('feed.post')"
        icon="pi pi-send"
        :loading="submitting"
        :disabled="!content.trim()"
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
  justify-content: flex-end;
}
</style>
