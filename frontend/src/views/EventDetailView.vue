<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useCalendarStore } from '@/stores/calendar'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { calendarApi } from '@/api/calendar.api'
import type { JobInfo } from '@/types/jobboard'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'

const props = defineProps<{ id: string }>()
const { t } = useI18n()
const router = useRouter()
const calendar = useCalendarStore()
const auth = useAuthStore()
const admin = useAdminStore()

const loading = ref(true)
const showCancelDialog = ref(false)
const showDeleteDialog = ref(false)
const linkedJobs = ref<JobInfo[]>([])
const jobboardEnabled = admin.isModuleEnabled('jobboard')

onMounted(async () => {
  try {
    await calendar.fetchEvent(props.id)
    if (jobboardEnabled) {
      const res = await calendarApi.getEventJobs(props.id)
      linkedJobs.value = res.data.data
    }
  } finally {
    loading.value = false
  }
})

function canManage() {
  if (!calendar.currentEvent) return false
  if (auth.isAdmin) return true
  return calendar.currentEvent.createdBy === auth.user?.id
}

async function handleRsvp(status: 'ATTENDING' | 'MAYBE' | 'DECLINED') {
  await calendar.rsvp(props.id, status)
}

async function handleCancel() {
  await calendar.cancelEvent(props.id)
  showCancelDialog.value = false
}

async function handleDelete() {
  await calendar.deleteEvent(props.id)
  showDeleteDialog.value = false
  router.push({ name: 'calendar' })
}

function formatDate(date: string) {
  return new Date(date).toLocaleDateString('de-DE', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  })
}

function formatTime(time: string | null) {
  if (!time) return ''
  return time.substring(0, 5)
}

function rsvpSeverity(status: string | null, target: string): 'success' | 'warn' | 'danger' | 'secondary' {
  if (status !== target) return 'secondary'
  switch (target) {
    case 'ATTENDING': return 'success'
    case 'MAYBE': return 'warn'
    case 'DECLINED': return 'danger'
    default: return 'secondary'
  }
}
</script>

<template>
  <div>
    <Button
      icon="pi pi-arrow-left"
      :label="t('common.back')"
      severity="secondary"
      text
      @click="router.push({ name: 'calendar' })"
      class="mb-1"
    />

    <LoadingSpinner v-if="loading" />

    <template v-else-if="calendar.currentEvent">
      <div class="event-header">
        <PageTitle :title="calendar.currentEvent.title" />
        <Tag v-if="calendar.currentEvent.cancelled" :value="t('calendar.cancelled')" severity="danger" />
        <Tag :value="t(`calendar.scopes.${calendar.currentEvent.scope}`)" severity="info" />
      </div>

      <div class="event-details card">
        <div class="detail-row">
          <i class="pi pi-calendar" />
          <div>
            <div>{{ formatDate(calendar.currentEvent.startDate) }}</div>
            <div v-if="!calendar.currentEvent.allDay && calendar.currentEvent.startTime">
              {{ formatTime(calendar.currentEvent.startTime) }}
              <template v-if="calendar.currentEvent.endTime"> - {{ formatTime(calendar.currentEvent.endTime) }}</template>
            </div>
            <div v-if="calendar.currentEvent.startDate !== calendar.currentEvent.endDate" class="text-muted">
              {{ t('calendar.endDate') }}: {{ formatDate(calendar.currentEvent.endDate) }}
            </div>
          </div>
        </div>

        <div v-if="calendar.currentEvent.location" class="detail-row">
          <i class="pi pi-map-marker" />
          <span>{{ calendar.currentEvent.location }}</span>
        </div>

        <div v-if="calendar.currentEvent.scopeName" class="detail-row">
          <i class="pi pi-th-large" />
          <span>{{ calendar.currentEvent.scopeName }}</span>
        </div>

        <div class="detail-row">
          <i class="pi pi-user" />
          <span>{{ calendar.currentEvent.creatorName }}</span>
        </div>

        <div v-if="calendar.currentEvent.recurrence !== 'NONE'" class="detail-row">
          <i class="pi pi-replay" />
          <span>{{ t(`calendar.recurrences.${calendar.currentEvent.recurrence}`) }}</span>
          <span v-if="calendar.currentEvent.recurrenceEnd" class="text-muted">
            ({{ t('calendar.recurrenceEnd') }}: {{ formatDate(calendar.currentEvent.recurrenceEnd) }})
          </span>
        </div>
      </div>

      <div v-if="calendar.currentEvent.description" class="event-description card">
        <p>{{ calendar.currentEvent.description }}</p>
      </div>

      <!-- RSVP Section -->
      <div v-if="!calendar.currentEvent.cancelled" class="rsvp-section card">
        <h2>{{ t('calendar.rsvp') }}</h2>
        <div class="rsvp-buttons">
          <Button
            :label="t('calendar.attending')"
            icon="pi pi-check"
            :severity="rsvpSeverity(calendar.currentEvent.currentUserRsvp, 'ATTENDING')"
            :outlined="calendar.currentEvent.currentUserRsvp !== 'ATTENDING'"
            @click="handleRsvp('ATTENDING')"
          />
          <Button
            :label="t('calendar.maybe')"
            icon="pi pi-question"
            :severity="rsvpSeverity(calendar.currentEvent.currentUserRsvp, 'MAYBE')"
            :outlined="calendar.currentEvent.currentUserRsvp !== 'MAYBE'"
            @click="handleRsvp('MAYBE')"
          />
          <Button
            :label="t('calendar.declined')"
            icon="pi pi-times"
            :severity="rsvpSeverity(calendar.currentEvent.currentUserRsvp, 'DECLINED')"
            :outlined="calendar.currentEvent.currentUserRsvp !== 'DECLINED'"
            @click="handleRsvp('DECLINED')"
          />
        </div>
        <div class="rsvp-counts">
          <span>{{ t('calendar.attendingCount', { n: calendar.currentEvent.attendingCount }) }}</span>
          <span class="separator">·</span>
          <span>{{ t('calendar.maybeCount', { n: calendar.currentEvent.maybeCount }) }}</span>
          <span class="separator">·</span>
          <span>{{ t('calendar.declinedCount', { n: calendar.currentEvent.declinedCount }) }}</span>
        </div>
      </div>

      <!-- Linked Jobs -->
      <div v-if="jobboardEnabled" class="linked-jobs-section card">
        <div class="linked-jobs-header">
          <h2>{{ t('jobboard.linkedJobs') }}</h2>
          <Button
            v-if="auth.isTeacher || auth.isAdmin"
            :label="t('jobboard.createLinkedJob')"
            icon="pi pi-plus"
            size="small"
            severity="secondary"
            @click="router.push({ name: 'job-create', query: { eventId: props.id } })"
          />
        </div>
        <div v-if="linkedJobs.length" class="linked-jobs-list">
          <router-link
            v-for="job in linkedJobs"
            :key="job.id"
            :to="{ name: 'job-detail', params: { id: job.id } }"
            class="linked-job-item"
          >
            <div class="linked-job-info">
              <strong>{{ job.title }}</strong>
              <span class="linked-job-meta">{{ job.category }} · {{ job.estimatedHours }}h · {{ job.currentAssignees }}/{{ job.maxAssignees }} {{ t('jobboard.assignees') }}</span>
            </div>
            <Tag :value="t(`jobboard.statuses.${job.status}`)" :severity="job.status === 'OPEN' ? 'success' : job.status === 'COMPLETED' ? 'secondary' : 'info'" size="small" />
          </router-link>
        </div>
        <p v-else class="text-muted">{{ t('jobboard.noLinkedJobs') }}</p>
      </div>

      <!-- Actions -->
      <div v-if="canManage()" class="event-actions">
        <Button
          :label="t('common.edit')"
          icon="pi pi-pencil"
          severity="secondary"
          @click="router.push({ name: 'event-edit', params: { id: props.id } })"
        />
        <Button
          v-if="!calendar.currentEvent.cancelled"
          :label="t('calendar.cancelEvent')"
          icon="pi pi-ban"
          severity="warn"
          @click="showCancelDialog = true"
        />
        <Button
          :label="t('common.delete')"
          icon="pi pi-trash"
          severity="danger"
          @click="showDeleteDialog = true"
        />
      </div>
    </template>

    <!-- Cancel Dialog -->
    <Dialog v-model:visible="showCancelDialog" :header="t('calendar.cancelEvent')" modal :style="{ width: '400px', maxWidth: '90vw' }">
      <p>{{ t('calendar.cancelConfirm') }}</p>
      <template #footer>
        <Button :label="t('common.no')" severity="secondary" text @click="showCancelDialog = false" />
        <Button :label="t('common.yes')" severity="warn" @click="handleCancel" />
      </template>
    </Dialog>

    <!-- Delete Dialog -->
    <Dialog v-model:visible="showDeleteDialog" :header="t('common.delete')" modal :style="{ width: '400px', maxWidth: '90vw' }">
      <p>{{ t('calendar.deleteConfirm') }}</p>
      <template #footer>
        <Button :label="t('common.no')" severity="secondary" text @click="showDeleteDialog = false" />
        <Button :label="t('common.yes')" severity="danger" @click="handleDelete" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.event-header {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.event-details {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1rem;
  margin-bottom: 1rem;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.detail-row i {
  color: var(--mw-text-muted);
  margin-top: 0.15rem;
}

.event-description {
  padding: 1rem;
  margin-bottom: 1rem;
}

.event-description p {
  white-space: pre-wrap;
  margin: 0;
}

.rsvp-section {
  padding: 1rem;
  margin-bottom: 1rem;
}

.rsvp-section h2 {
  font-size: var(--mw-font-size-md);
  margin: 0 0 0.75rem;
}

.rsvp-buttons {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}

.rsvp-counts {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
}

.separator {
  margin: 0 0.25rem;
}

.text-muted {
  color: var(--mw-text-muted);
}

.linked-jobs-section {
  padding: 1rem;
  margin-bottom: 1rem;
}

.linked-jobs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.linked-jobs-header h2 {
  font-size: var(--mw-font-size-md);
  margin: 0;
}

.linked-jobs-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.linked-job-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-sm);
  cursor: pointer;
  transition: background 0.15s;
  text-decoration: none;
  color: inherit;
}

.linked-job-item:hover {
  background: var(--mw-bg-hover);
}

.linked-job-meta {
  display: block;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.15rem;
}

.event-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
</style>
