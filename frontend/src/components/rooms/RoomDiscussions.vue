<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useDiscussionsStore } from '@/stores/discussions'
import { useAuthStore } from '@/stores/auth'
import { useRoomsStore } from '@/stores/rooms'
import DiscussionThreadView from './DiscussionThreadView.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import type { ThreadAudience } from '@/types/discussion'

const props = defineProps<{ roomId: string }>()
const { t } = useI18n()
const { formatCompactDateTime } = useLocaleDate()
const discussions = useDiscussionsStore()
const auth = useAuthStore()
const rooms = useRoomsStore()

const selectedThreadId = ref<string | null>(null)
const showCreateDialog = ref(false)
const newTitle = ref('')
const newContent = ref('')
const newAudience = ref<ThreadAudience>('ALLE')

const audienceOptions = [
  { label: t('discussions.audienceAlle'), value: 'ALLE' },
  { label: t('discussions.audienceEltern'), value: 'ELTERN' },
  { label: t('discussions.audienceKinder'), value: 'KINDER' },
]

const isLeader = ref(false)
const userRoomRole = ref<string | null>(null)

// Backend already filters threads by audience based on both room role and global user role
const filteredThreads = computed(() => discussions.threads)

onMounted(async () => {
  await discussions.fetchThreads(props.roomId)
  // Check if current user is LEADER and determine room role
  const member = rooms.currentRoom?.members?.find(m => m.userId === auth.user?.id)
  isLeader.value = member?.role === 'LEADER' || auth.isAdmin
  userRoomRole.value = member?.role ?? null
})

function selectThread(threadId: string) {
  selectedThreadId.value = threadId
}

function backToList() {
  selectedThreadId.value = null
  discussions.fetchThreads(props.roomId)
}

async function createThread() {
  if (!newTitle.value.trim()) return
  await discussions.createThread(props.roomId, newTitle.value.trim(), newContent.value.trim() || undefined, newAudience.value)
  showCreateDialog.value = false
  newTitle.value = ''
  newContent.value = ''
  newAudience.value = 'ALLE'
}

function audienceSeverity(audience: string): 'info' | 'warn' | 'success' | 'secondary' {
  switch (audience) {
    case 'ELTERN': return 'warn'
    case 'KINDER': return 'info'
    default: return 'secondary'
  }
}

function formatDate(date: string) {
  return formatCompactDateTime(date)
}
</script>

<template>
  <div>
    <template v-if="selectedThreadId">
      <DiscussionThreadView
        :roomId="roomId"
        :threadId="selectedThreadId"
        :isLeader="isLeader"
        @back="backToList"
      />
    </template>

    <template v-else>
      <div class="discussions-header">
        <Button
          v-if="isLeader"
          :label="t('discussions.create')"
          icon="pi pi-plus"
          size="small"
          @click="showCreateDialog = true"
        />
      </div>

      <LoadingSpinner v-if="discussions.loading && !filteredThreads.length" />
      <EmptyState
        v-else-if="!filteredThreads.length"
        icon="pi pi-comments"
        :message="t('discussions.noThreads')"
      />
      <div v-else class="thread-list">
        <div
          v-for="thread in filteredThreads"
          :key="thread.id"
          class="thread-item card"
          @click="selectThread(thread.id)"
        >
          <div class="thread-info">
            <div class="thread-title-row">
              <strong>{{ thread.title }}</strong>
              <Tag
                v-if="thread.audience && thread.audience !== 'ALLE'"
                :value="t(`discussions.audience${thread.audience.charAt(0) + thread.audience.slice(1).toLowerCase()}`)"
                :severity="audienceSeverity(thread.audience)"
                size="small"
              />
              <Tag
                v-if="thread.status === 'ARCHIVED'"
                :value="t('discussions.archived')"
                severity="secondary"
                size="small"
              />
            </div>
            <div class="thread-meta">
              <span>{{ thread.creatorName }}</span>
              <span class="separator">·</span>
              <span>{{ formatDate(thread.createdAt) }}</span>
              <span class="separator">·</span>
              <span>{{ t('discussions.replies', { n: thread.replyCount }) }}</span>
            </div>
          </div>
          <i class="pi pi-chevron-right thread-arrow" />
        </div>
      </div>
    </template>

    <Dialog
      v-model:visible="showCreateDialog"
      :header="t('discussions.createTitle')"
      modal
      :style="{ width: '500px', maxWidth: '90vw' }"
    >
      <div class="create-form">
        <div class="field">
          <label>{{ t('discussions.titleLabel') }}</label>
          <InputText v-model="newTitle" :placeholder="t('discussions.titlePlaceholder')" class="w-full" />
        </div>
        <div class="field">
          <label>{{ t('discussions.contentLabel') }}</label>
          <Textarea v-model="newContent" :placeholder="t('discussions.contentPlaceholder')" :autoResize="true" rows="4" class="w-full" />
        </div>
        <div class="field">
          <label>{{ t('discussions.audience') }}</label>
          <Select
            v-model="newAudience"
            :options="audienceOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" text @click="showCreateDialog = false" />
        <Button :label="t('common.create')" :disabled="!newTitle.trim()" @click="createThread" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.discussions-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1rem;
}

.thread-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.thread-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: background 0.15s;
}

.thread-item:hover {
  background: var(--mw-bg-hover);
}

.thread-info {
  flex: 1;
  min-width: 0;
}

.thread-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.thread-meta {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.25rem;
}

.separator {
  margin: 0 0.25rem;
}

.thread-arrow {
  color: var(--mw-text-muted);
  font-size: 0.75rem;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.field label {
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
}
</style>
