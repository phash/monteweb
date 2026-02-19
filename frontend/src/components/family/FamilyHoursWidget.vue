<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useJobboardStore } from '@/stores/jobboard'
import { useAdminStore } from '@/stores/admin'
import { jobboardApi } from '@/api/jobboard.api'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import type { JobAssignmentInfo } from '@/types/jobboard'

const { t } = useI18n()
const props = defineProps<{ familyId: string }>()
const jobboard = useJobboardStore()
const admin = useAdminStore()

const jobboardEnabled = admin.isModuleEnabled('jobboard')

const showJobs = ref(false)
const familyAssignments = ref<JobAssignmentInfo[]>([])
const loadingJobs = ref(false)

onMounted(() => {
  if (jobboardEnabled) {
    jobboard.fetchFamilyHours(props.familyId)
  }
})

function trafficLightSeverity(light: string) {
  switch (light) {
    case 'GREEN': return 'success'
    case 'YELLOW': return 'warn'
    case 'RED': return 'danger'
    default: return 'info'
  }
}

function trafficLightLabel(light: string) {
  switch (light) {
    case 'GREEN': return t('admin.trafficLight.green')
    case 'YELLOW': return t('admin.trafficLight.yellow')
    case 'RED': return t('admin.trafficLight.red')
    default: return light
  }
}

function progressPercent(completed: number, target: number) {
  if (target === 0) return 100
  return Math.min(100, Math.round((completed / target) * 100))
}

async function openJobsList() {
  showJobs.value = true
  loadingJobs.value = true
  try {
    const res = await jobboardApi.getFamilyAssignments(props.familyId)
    familyAssignments.value = res.data.data
  } catch {
    familyAssignments.value = []
  } finally {
    loadingJobs.value = false
  }
}

function formatDate(dateStr: string | null) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' })
}
</script>

<template>
  <div v-if="jobboardEnabled && jobboard.familyHours" class="hours-widget card" @click="openJobsList">
    <div class="hours-header">
      <h3>{{ t('family.hours') }}</h3>
      <Tag
        :value="trafficLightLabel(jobboard.familyHours.trafficLight)"
        :severity="trafficLightSeverity(jobboard.familyHours.trafficLight)"
      />
    </div>

    <div class="hours-bars">
      <!-- Total hours -->
      <div class="progress-section">
        <div class="progress-section-label">{{ t('family.totalProgress') }}</div>
        <div class="progress-bg">
          <div
            class="progress-fill"
            :class="jobboard.familyHours.trafficLight.toLowerCase()"
            :style="{ width: progressPercent(jobboard.familyHours.totalHours, jobboard.familyHours.targetHours) + '%' }"
          />
        </div>
        <div class="progress-label">
          {{ jobboard.familyHours.totalHours }}/{{ jobboard.familyHours.targetHours }} {{ t('family.hoursUnit') }}
        </div>
      </div>

      <!-- Cleaning hours -->
      <div v-if="jobboard.familyHours.targetCleaningHours > 0" class="progress-section">
        <div class="progress-section-header">
          <span class="progress-section-label">{{ t('family.cleaningProgress') }}</span>
          <Tag
            :value="trafficLightLabel(jobboard.familyHours.cleaningTrafficLight)"
            :severity="trafficLightSeverity(jobboard.familyHours.cleaningTrafficLight)"
            class="cleaning-tag"
          />
        </div>
        <div class="progress-bg">
          <div
            class="progress-fill"
            :class="jobboard.familyHours.cleaningTrafficLight.toLowerCase()"
            :style="{ width: progressPercent(jobboard.familyHours.cleaningHours, jobboard.familyHours.targetCleaningHours) + '%' }"
          />
        </div>
        <div class="progress-label">
          {{ jobboard.familyHours.cleaningHours }}/{{ jobboard.familyHours.targetCleaningHours }} {{ t('family.hoursUnit') }}
        </div>
      </div>
    </div>

    <div class="hours-details">
      <div class="detail">
        <span class="detail-label">{{ t('family.jobHours') }}</span>
        <span class="detail-value">{{ jobboard.familyHours.completedHours }}h</span>
      </div>
      <div class="detail">
        <span class="detail-label">{{ t('family.cleaningHours') }}</span>
        <span class="detail-value">{{ jobboard.familyHours.cleaningHours }}h</span>
      </div>
      <div class="detail">
        <span class="detail-label">{{ t('family.pending') }}</span>
        <span class="detail-value">{{ jobboard.familyHours.pendingHours }}h</span>
      </div>
      <div class="detail">
        <span class="detail-label">{{ t('family.remaining') }}</span>
        <span class="detail-value">{{ jobboard.familyHours.remainingHours }}h</span>
      </div>
    </div>

    <div class="click-hint">
      <i class="pi pi-list" />
      <span>{{ t('family.clickForJobs') }}</span>
    </div>
  </div>

  <!-- Jobs Dialog -->
  <Dialog v-model:visible="showJobs" :header="t('family.completedJobs')" modal :style="{ width: '600px' }">
    <div v-if="loadingJobs" class="text-center p-4">
      <i class="pi pi-spin pi-spinner" style="font-size: 1.5rem" />
    </div>
    <div v-else-if="familyAssignments.length === 0" class="text-center p-4 text-gray-400">
      {{ t('family.noCompletedJobs') }}
    </div>
    <div v-else class="jobs-list">
      <div v-for="a in familyAssignments" :key="a.id" class="job-entry">
        <div class="job-entry-main">
          <span class="job-title">{{ a.jobTitle }}</span>
          <span class="job-hours">{{ a.actualHours }}h</span>
        </div>
        <div class="job-entry-meta">
          <span>{{ a.userName }}</span>
          <span v-if="a.completedAt">{{ formatDate(a.completedAt) }}</span>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.hours-widget {
  margin-bottom: 1.5rem;
  max-width: 480px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.hours-widget:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.hours-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.hours-header h3 {
  font-size: var(--mw-font-size-md);
}

.hours-bars {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.progress-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.25rem;
}

.progress-section-label {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-secondary);
  margin-bottom: 0.25rem;
}

.progress-bg {
  height: 12px;
  background: var(--mw-border-light);
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 0.25rem;
}

.progress-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.3s;
}

.progress-fill.green { background: #16a34a; }
.progress-fill.yellow { background: #ca8a04; }
.progress-fill.red { background: #dc2626; }

.progress-label {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-secondary);
  text-align: center;
}

.cleaning-tag {
  font-size: 0.65rem;
  padding: 0.1rem 0.4rem;
}

.hours-details {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.5rem;
  text-align: center;
}

.detail-label {
  display: block;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.detail-value {
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
}

.click-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  margin-top: 0.75rem;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.jobs-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.job-entry {
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--mw-border-light);
}

.job-entry:last-child {
  border-bottom: none;
}

.job-entry-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.job-title {
  font-weight: 500;
  font-size: var(--mw-font-size-sm);
}

.job-hours {
  font-weight: 600;
  color: var(--mw-primary);
}

.job-entry-meta {
  display: flex;
  justify-content: space-between;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.15rem;
}
</style>
