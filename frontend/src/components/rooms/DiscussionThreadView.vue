<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDiscussionsStore } from '@/stores/discussions'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'

const props = defineProps<{
  roomId: string
  threadId: string
  isLeader: boolean
}>()

const emit = defineEmits<{ back: [] }>()

const { t } = useI18n()
const discussions = useDiscussionsStore()

const replyText = ref('')

onMounted(async () => {
  await discussions.fetchThread(props.roomId, props.threadId)
  await discussions.fetchReplies(props.roomId, props.threadId)
})

async function sendReply() {
  if (!replyText.value.trim()) return
  await discussions.addReply(props.roomId, props.threadId, replyText.value.trim())
  replyText.value = ''
}

async function archiveThread() {
  await discussions.archiveThread(props.roomId, props.threadId)
}

async function deleteThread() {
  await discussions.deleteThread(props.roomId, props.threadId)
  emit('back')
}

function formatDate(date: string) {
  return new Date(date).toLocaleString('de-DE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const isArchived = () => discussions.currentThread?.status === 'ARCHIVED'
</script>

<template>
  <div>
    <Button
      icon="pi pi-arrow-left"
      :label="t('common.back')"
      severity="secondary"
      text
      size="small"
      @click="emit('back')"
      class="mb-1"
    />

    <LoadingSpinner v-if="discussions.loading && !discussions.currentThread" />

    <template v-else-if="discussions.currentThread">
      <div class="thread-header card">
        <div class="thread-title-row">
          <h3>{{ discussions.currentThread.title }}</h3>
          <Tag
            v-if="isArchived()"
            :value="t('discussions.archived')"
            severity="secondary"
          />
        </div>
        <div class="thread-meta">
          {{ discussions.currentThread.creatorName }} · {{ formatDate(discussions.currentThread.createdAt) }}
        </div>
        <p v-if="discussions.currentThread.content" class="thread-content">
          {{ discussions.currentThread.content }}
        </p>
        <div v-if="isLeader" class="thread-actions">
          <Button
            v-if="!isArchived()"
            :label="t('discussions.archive')"
            icon="pi pi-inbox"
            severity="secondary"
            size="small"
            @click="archiveThread"
          />
          <Button
            :label="t('common.delete')"
            icon="pi pi-trash"
            severity="danger"
            size="small"
            @click="deleteThread"
          />
        </div>
      </div>

      <div class="replies-section">
        <div
          v-for="reply in discussions.replies"
          :key="reply.id"
          class="reply-item"
        >
          <div class="reply-header">
            <strong>{{ reply.authorName }}</strong>
            <span class="reply-time">{{ formatDate(reply.createdAt) }}</span>
          </div>
          <p>{{ reply.content }}</p>
        </div>

        <div v-if="!discussions.replies.length && !discussions.loading" class="no-replies">
          {{ t('discussions.noReplies') }}
        </div>
      </div>

      <div v-if="!isArchived()" class="reply-input">
        <Textarea
          v-model="replyText"
          :placeholder="t('discussions.replyPlaceholder')"
          :autoResize="true"
          rows="2"
          class="reply-field"
          @keydown.enter.exact.prevent="sendReply"
        />
        <Button
          icon="pi pi-send"
          :disabled="!replyText.trim()"
          @click="sendReply"
        />
      </div>
      <div v-else class="archived-notice">
        <i class="pi pi-lock" />
        {{ t('discussions.archivedNotice') }}
      </div>
    </template>
  </div>
</template>

<style scoped>
.thread-header {
  padding: 1rem;
  margin-bottom: 1rem;
}

.thread-title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.thread-title-row h3 {
  margin: 0;
}

.thread-meta {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.25rem;
}

.thread-content {
  margin-top: 0.75rem;
  white-space: pre-wrap;
}

.thread-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.replies-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.reply-item {
  padding: 0.75rem 1rem;
  background: var(--mw-bg-card);
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-sm);
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.25rem;
}

.reply-header strong {
  font-size: var(--mw-font-size-sm);
}

.reply-time {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.reply-item p {
  margin: 0;
  white-space: pre-wrap;
}

.no-replies {
  text-align: center;
  color: var(--mw-text-muted);
  padding: 2rem;
}

.reply-input {
  display: flex;
  gap: 0.5rem;
  align-items: flex-end;
}

.reply-field {
  flex: 1;
}

.archived-notice {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--mw-text-muted);
  font-size: var(--mw-font-size-sm);
  padding: 0.75rem;
  background: var(--mw-bg-hover);
  border-radius: var(--mw-border-radius-sm);
}
</style>
