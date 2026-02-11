<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useJobboardStore } from '@/stores/jobboard'
import { useAdminStore } from '@/stores/admin'
import Tag from 'primevue/tag'

const { t } = useI18n()
const props = defineProps<{ familyId: string }>()
const jobboard = useJobboardStore()
const admin = useAdminStore()

const jobboardEnabled = admin.isModuleEnabled('jobboard')

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
</script>

<template>
  <div v-if="jobboardEnabled && jobboard.familyHours" class="hours-widget card">
    <div class="hours-header">
      <h3>{{ t('family.hours') }}</h3>
      <Tag
        :value="trafficLightLabel(jobboard.familyHours.trafficLight)"
        :severity="trafficLightSeverity(jobboard.familyHours.trafficLight)"
      />
    </div>

    <div class="progress-container">
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
  </div>
</template>

<style scoped>
.hours-widget {
  margin-bottom: 1.5rem;
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

.progress-container {
  margin-bottom: 0.75rem;
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
</style>
