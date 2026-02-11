<script setup lang="ts">
import { onMounted, computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useJobboardStore } from '@/stores/jobboard'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'

const props = defineProps<{ id: string }>()
const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const jobboard = useJobboardStore()

const showCompleteDialog = ref(false)
const completeAssignmentId = ref<string | null>(null)
const actualHours = ref<number>(0)
const completeNotes = ref('')

onMounted(async () => {
  await Promise.all([
    jobboard.fetchJob(props.id),
    jobboard.fetchAssignments(props.id),
    jobboard.fetchMyAssignments(),
  ])
})

const myAssignment = computed(() =>
  jobboard.myAssignments.find(a => a.jobId === props.id && a.status !== 'CANCELLED')
)

const canApply = computed(() => {
  if (!jobboard.currentJob) return false
  if (jobboard.currentJob.status !== 'OPEN') return false
  if (myAssignment.value) return false
  return true
})

async function apply() {
  await jobboard.applyForJob(props.id)
  await jobboard.fetchAssignments(props.id)
}

async function start() {
  if (!myAssignment.value) return
  await jobboard.startAssignment(myAssignment.value.id)
  await jobboard.fetchMyAssignments()
}

function openComplete() {
  if (!myAssignment.value) return
  completeAssignmentId.value = myAssignment.value.id
  actualHours.value = jobboard.currentJob?.estimatedHours ?? 0
  completeNotes.value = ''
  showCompleteDialog.value = true
}

async function submitComplete() {
  if (!completeAssignmentId.value) return
  await jobboard.completeAssignment(completeAssignmentId.value, actualHours.value, completeNotes.value || undefined)
  showCompleteDialog.value = false
  await jobboard.fetchMyAssignments()
  await jobboard.fetchAssignments(props.id)
}

async function confirm(assignmentId: string) {
  await jobboard.confirmAssignment(assignmentId)
  await jobboard.fetchAssignments(props.id)
}

function statusSeverity(status: string) {
  switch (status) {
    case 'OPEN': return 'success'
    case 'ASSIGNED': return 'info'
    case 'IN_PROGRESS': return 'warn'
    case 'COMPLETED': return 'secondary'
    case 'CANCELLED': return 'danger'
    default: return 'info'
  }
}

function formatDate(date: string | null) {
  if (!date) return ''
  return new Date(date).toLocaleDateString('de-DE')
}
</script>

<template>
  <div>
    <Button
      icon="pi pi-arrow-left"
      :label="t('common.back')"
      severity="secondary"
      text
      @click="router.push({ name: 'jobs' })"
      class="mb-1"
    />

    <LoadingSpinner v-if="jobboard.loading" />

    <template v-else-if="jobboard.currentJob">
      <div class="job-header">
        <PageTitle :title="jobboard.currentJob.title" />
        <Tag
          :value="t(`jobboard.statuses.${jobboard.currentJob.status}`)"
          :severity="statusSeverity(jobboard.currentJob.status)"
        />
      </div>

      <div class="job-details card">
        <div class="detail-grid">
          <div class="detail-item">
            <label>{{ t('jobboard.category') }}</label>
            <span>{{ jobboard.currentJob.category }}</span>
          </div>
          <div v-if="jobboard.currentJob.location" class="detail-item">
            <label>{{ t('jobboard.location') }}</label>
            <span>{{ jobboard.currentJob.location }}</span>
          </div>
          <div class="detail-item">
            <label>{{ t('jobboard.estimatedHours') }}</label>
            <span>{{ jobboard.currentJob.estimatedHours }}h</span>
          </div>
          <div v-if="jobboard.currentJob.scheduledDate" class="detail-item">
            <label>{{ t('jobboard.date') }}</label>
            <span>{{ formatDate(jobboard.currentJob.scheduledDate) }} {{ jobboard.currentJob.scheduledTime ?? '' }}</span>
          </div>
          <div class="detail-item">
            <label>{{ t('common.createdBy') }}</label>
            <span>{{ jobboard.currentJob.creatorName }}</span>
          </div>
          <div v-if="jobboard.currentJob.contactInfo" class="detail-item">
            <label>{{ t('jobboard.contact') }}</label>
            <span>{{ jobboard.currentJob.contactInfo }}</span>
          </div>
          <div class="detail-item">
            <label>{{ t('common.slots') }}</label>
            <span>{{ jobboard.currentJob.currentAssignees }}/{{ jobboard.currentJob.maxAssignees }}</span>
          </div>
          <div v-if="jobboard.currentJob.eventTitle" class="detail-item">
            <label>{{ t('jobboard.linkedEvent') }}</label>
            <a class="event-link" @click.stop="router.push({ name: 'event-detail', params: { id: jobboard.currentJob.eventId! } })">
              <i class="pi pi-calendar-plus" /> {{ jobboard.currentJob.eventTitle }}
            </a>
          </div>
        </div>

        <p v-if="jobboard.currentJob.description" class="job-description">
          {{ jobboard.currentJob.description }}
        </p>
      </div>

      <!-- Action buttons -->
      <div class="job-actions">
        <Button
          v-if="canApply"
          :label="t('jobboard.apply')"
          icon="pi pi-check"
          @click="apply"
        />
        <Button
          v-if="myAssignment && myAssignment.status === 'ASSIGNED'"
          :label="t('jobboard.start')"
          icon="pi pi-play"
          severity="info"
          @click="start"
        />
        <Button
          v-if="myAssignment && (myAssignment.status === 'ASSIGNED' || myAssignment.status === 'IN_PROGRESS')"
          :label="t('jobboard.complete')"
          icon="pi pi-check-circle"
          severity="success"
          @click="openComplete"
        />
      </div>

      <!-- Assignments -->
      <div class="assignments-section">
        <h2>{{ t('jobboard.assignments') }}</h2>
        <div v-if="jobboard.assignments.length" class="assignments-list">
          <div v-for="a in jobboard.assignments" :key="a.id" class="assignment-item">
            <div class="assignment-info">
              <strong>{{ a.userName }}</strong>
              <span class="family-name">({{ a.familyName }})</span>
              <Tag :value="t(`jobboard.assignmentStatuses.${a.status}`)" :severity="statusSeverity(a.status)" size="small" />
              <Tag v-if="a.confirmed" :value="t('jobboard.confirmed')" severity="success" size="small" />
            </div>
            <div class="assignment-actions">
              <span v-if="a.actualHours" class="hours">{{ a.actualHours }}h</span>
              <Button
                v-if="a.status === 'COMPLETED' && !a.confirmed && (auth.isTeacher || auth.isAdmin)"
                :label="t('common.confirm')"
                icon="pi pi-check"
                severity="success"
                size="small"
                @click="confirm(a.id)"
              />
            </div>
          </div>
        </div>
        <p v-else class="text-muted">{{ t('jobboard.noAssignmentsYet') }}</p>
      </div>
    </template>

    <!-- Complete Dialog -->
    <Dialog v-model:visible="showCompleteDialog" :header="t('jobboard.completeTask')" modal :style="{ width: '450px' }">
      <div class="complete-form">
        <div class="form-field">
          <label>{{ t('jobboard.actualHours') }}</label>
          <InputNumber v-model="actualHours" :minFractionDigits="1" :maxFractionDigits="2" :min="0.5" :step="0.5" />
        </div>
        <div class="form-field">
          <label>{{ t('jobboard.notesOptional') }}</label>
          <Textarea v-model="completeNotes" :autoResize="true" rows="3" />
        </div>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" text @click="showCompleteDialog = false" />
        <Button :label="t('jobboard.complete')" icon="pi pi-check" @click="submitComplete" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.mb-1 {
  margin-bottom: 1rem;
}

.job-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.job-details {
  margin-bottom: 1.5rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.detail-item label {
  display: block;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-bottom: 0.25rem;
}

.detail-item span {
  font-size: var(--mw-font-size-sm);
}

.job-description {
  white-space: pre-wrap;
  border-top: 1px solid var(--mw-border-light);
  padding-top: 1rem;
}

.job-actions {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.assignments-section h2 {
  font-size: var(--mw-font-size-lg);
  margin-bottom: 0.75rem;
}

.assignments-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.assignment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0.75rem;
  background: var(--mw-bg-card);
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-sm);
}

.assignment-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--mw-font-size-sm);
}

.family-name {
  color: var(--mw-text-secondary);
}

.assignment-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.hours {
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
}

.event-link {
  color: var(--mw-primary);
  cursor: pointer;
  font-size: var(--mw-font-size-sm);
}

.event-link:hover {
  text-decoration: underline;
}

.event-link i {
  margin-right: 0.25rem;
}

.text-muted {
  color: var(--mw-text-muted);
}

.complete-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field label {
  display: block;
  font-size: var(--mw-font-size-sm);
  margin-bottom: 0.25rem;
}
</style>
