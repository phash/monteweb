<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import MentionInput from '@/components/common/MentionInput.vue'
import PollComposer from '@/components/common/PollComposer.vue'
import type { CreatePollRequest } from '@/types/feed'

const emit = defineEmits<{
  submit: [data: { title?: string; content?: string; poll?: CreatePollRequest }]
}>()

const { t } = useI18n()
const title = ref('')
const content = ref('')
const submitting = ref(false)
const showPollComposer = ref(false)

const canSubmit = computed(() =>
  content.value.trim().length > 0 || showPollComposer.value
)

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
    })
    title.value = ''
    content.value = ''
    showPollComposer.value = false
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

    <div class="composer-actions">
      <Button
        v-if="!showPollComposer"
        icon="pi pi-chart-bar"
        :aria-label="t('poll.createPoll')"
        text
        severity="secondary"
        size="small"
        @click="showPollComposer = true"
      />
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
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
