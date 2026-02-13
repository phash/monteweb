<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useFotoboxStore } from '@/stores/fotobox'
import { useAuthStore } from '@/stores/auth'
import { useRoomsStore } from '@/stores/rooms'
import { fotoboxApi } from '@/api/fotobox.api'
import type { FotoboxPermissionLevel } from '@/types/fotobox'
import type { FileAudience } from '@/types/files'
import FotoboxThread from './FotoboxThread.vue'
import FotoboxSettings from './FotoboxSettings.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { useToast } from 'primevue/usetoast'

const props = defineProps<{ roomId: string; isLeader: boolean }>()
const { t } = useI18n()
const { formatCompactDateTime } = useLocaleDate()
const fotobox = useFotoboxStore()
const auth = useAuthStore()
const rooms = useRoomsStore()
const toast = useToast()

const selectedThreadId = ref<string | null>(null)
const showCreateDialog = ref(false)
const showSettingsDialog = ref(false)
const newTitle = ref('')
const newDescription = ref('')
const newAudience = ref<FileAudience>('ALL')

const audienceOptions = [
  { label: t('files.audienceAll'), value: 'ALL' as FileAudience },
  { label: t('files.audienceParents'), value: 'PARENTS_ONLY' as FileAudience },
  { label: t('files.audienceStudents'), value: 'STUDENTS_ONLY' as FileAudience },
]

const permission = ref<FotoboxPermissionLevel>('VIEW_ONLY')
const fotoboxEnabled = ref(false)

const canCreateThread = computed(
  () => permission.value === 'CREATE_THREADS',
)

onMounted(async () => {
  await fotobox.fetchSettings(props.roomId)
  if (fotobox.settings) {
    fotoboxEnabled.value = fotobox.settings.enabled
    if (fotobox.settings.enabled) {
      // Determine permission level
      const member = rooms.currentRoom?.members?.find((m) => m.userId === auth.user?.id)
      if (member?.role === 'LEADER' || auth.isAdmin) {
        permission.value = 'CREATE_THREADS'
      } else {
        permission.value = fotobox.settings.defaultPermission
      }
      await fotobox.fetchThreads(props.roomId)
    }
  }
})

function selectThread(threadId: string) {
  selectedThreadId.value = threadId
}

function backToList() {
  selectedThreadId.value = null
  fotobox.fetchThreads(props.roomId)
}

async function createThread() {
  if (!newTitle.value.trim()) return
  try {
    await fotobox.createThread(props.roomId, {
      title: newTitle.value.trim(),
      description: newDescription.value.trim() || undefined,
      audience: props.isLeader ? newAudience.value : undefined,
    })
    showCreateDialog.value = false
    newTitle.value = ''
    newDescription.value = ''
    newAudience.value = 'ALL'
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function deleteThread(threadId: string) {
  if (!confirm(t('fotobox.confirmDeleteThread'))) return
  try {
    await fotobox.deleteThread(props.roomId, threadId)
    toast.add({ severity: 'success', summary: t('fotobox.deleteThread'), life: 2000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

function formatDate(date: string) {
  return formatCompactDateTime(date)
}

function audienceSeverity(audience: string): 'info' | 'warn' | 'secondary' {
  switch (audience) {
    case 'PARENTS_ONLY': return 'warn'
    case 'STUDENTS_ONLY': return 'info'
    default: return 'secondary'
  }
}

function audienceLabel(audience: string): string {
  switch (audience) {
    case 'PARENTS_ONLY': return t('files.audienceParents')
    case 'STUDENTS_ONLY': return t('files.audienceStudents')
    default: return t('files.audienceAll')
  }
}
</script>

<template>
  <div>
    <!-- Thread detail view -->
    <template v-if="selectedThreadId">
      <FotoboxThread
        :roomId="roomId"
        :threadId="selectedThreadId"
        :permission="permission"
        :isLeader="isLeader"
        :maxFileSizeMb="fotobox.settings?.maxFileSizeMb"
        @back="backToList"
      />
    </template>

    <!-- Thread list view -->
    <template v-else>
      <!-- Not enabled message -->
      <template v-if="!fotoboxEnabled">
        <EmptyState icon="pi pi-images" :message="t('fotobox.noThreads')">
          <Button
            v-if="isLeader"
            :label="t('fotobox.settings')"
            icon="pi pi-cog"
            size="small"
            severity="secondary"
            class="mt-1"
            @click="showSettingsDialog = true"
          />
        </EmptyState>
      </template>

      <template v-else>
        <div class="fotobox-header">
          <div class="header-actions">
            <Button
              v-if="canCreateThread"
              :label="t('fotobox.newThread')"
              icon="pi pi-plus"
              size="small"
              @click="showCreateDialog = true"
            />
            <Button
              v-if="isLeader"
              icon="pi pi-cog"
              severity="secondary"
              text
              size="small"
              :aria-label="t('fotobox.settings')"
              @click="showSettingsDialog = true"
            />
          </div>
        </div>

        <LoadingSpinner v-if="fotobox.loading && !fotobox.threads.length" />
        <EmptyState
          v-else-if="!fotobox.threads.length"
          icon="pi pi-images"
          :message="t('fotobox.noThreads')"
        />
        <div v-else class="thread-grid">
          <div
            v-for="thread in fotobox.threads"
            :key="thread.id"
            class="thread-card card"
            @click="selectThread(thread.id)"
          >
            <div class="thread-cover">
              <img
                v-if="thread.coverImageId"
                :src="fotoboxApi.thumbnailUrl(thread.coverImageId)"
                alt=""
                class="cover-img"
              />
              <i v-else class="pi pi-images cover-placeholder" />
            </div>
            <div class="thread-body">
              <div class="thread-title-row">
                <strong class="thread-title">{{ thread.title }}</strong>
                <Tag
                  v-if="thread.audience && thread.audience !== 'ALL'"
                  :value="audienceLabel(thread.audience)"
                  :severity="audienceSeverity(thread.audience)"
                  size="small"
                />
              </div>
              <div class="thread-meta">
                <span>{{ thread.imageCount }} {{ t('fotobox.images') }}</span>
                <span class="separator">·</span>
                <span>{{ thread.createdByName }}</span>
                <span class="separator">·</span>
                <span>{{ formatDate(thread.createdAt) }}</span>
              </div>
            </div>
            <Button
              v-if="isLeader || thread.createdBy === auth.user?.id"
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              size="small"
              class="thread-delete-btn"
              @click.stop="deleteThread(thread.id)"
            />
          </div>
        </div>
      </template>
    </template>

    <!-- Create Thread Dialog -->
    <Dialog
      v-model:visible="showCreateDialog"
      :header="t('fotobox.newThread')"
      modal
      :style="{ width: '500px', maxWidth: '90vw' }"
    >
      <div class="create-form">
        <div class="field">
          <label>{{ t('fotobox.threadTitle') }}</label>
          <InputText v-model="newTitle" :placeholder="t('fotobox.threadTitle')" class="w-full" />
        </div>
        <div class="field">
          <label>{{ t('fotobox.threadDescription') }}</label>
          <Textarea
            v-model="newDescription"
            :placeholder="t('fotobox.threadDescription')"
            :autoResize="true"
            rows="3"
            class="w-full"
          />
        </div>
        <div v-if="isLeader" class="field">
          <label>{{ t('files.audience') }}</label>
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
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          text
          @click="showCreateDialog = false"
        />
        <Button
          :label="t('common.create')"
          :disabled="!newTitle.trim()"
          @click="createThread"
        />
      </template>
    </Dialog>

    <!-- Settings Dialog -->
    <FotoboxSettings
      v-if="showSettingsDialog"
      :roomId="roomId"
      :visible="showSettingsDialog"
      @update:visible="showSettingsDialog = $event"
    />
  </div>
</template>

<style scoped>
.fotobox-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1rem;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.mt-1 {
  margin-top: 1rem;
}

.thread-grid {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.thread-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}

.thread-card:hover {
  background: var(--mw-bg-hover);
}

.thread-cover {
  width: 64px;
  height: 64px;
  border-radius: var(--mw-border-radius-sm, 4px);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--mw-bg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  font-size: 1.5rem;
  color: var(--mw-text-muted);
}

.thread-body {
  flex: 1;
  min-width: 0;
}

.thread-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.thread-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.thread-meta {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.25rem;
}

.separator {
  margin: 0 0.25rem;
}

.thread-delete-btn {
  flex-shrink: 0;
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
