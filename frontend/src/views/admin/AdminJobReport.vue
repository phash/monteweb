<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useJobboardStore } from '@/stores/jobboard'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'

const { t } = useI18n()
const jobboard = useJobboardStore()

onMounted(() => {
  jobboard.fetchReport()
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
  <div>
    <div class="page-header">
      <PageTitle :title="t('jobboard.report')" :subtitle="t('admin.reportSubtitle')" />
      <div class="flex gap-2">
        <Button
          :label="t('admin.pdfExport')"
          icon="pi pi-file-pdf"
          severity="secondary"
          @click="jobboard.exportPdf()"
        />
        <Button
          :label="t('admin.csvExport')"
          icon="pi pi-download"
          severity="secondary"
          @click="jobboard.exportCsv()"
        />
      </div>
    </div>

    <!-- Summary Cards -->
    <div v-if="jobboard.reportSummary" class="summary-cards">
      <div class="summary-card card">
        <div class="summary-value">{{ jobboard.reportSummary.openJobs }}</div>
        <div class="summary-label">{{ t('admin.openJobs') }}</div>
      </div>
      <div class="summary-card card">
        <div class="summary-value">{{ jobboard.reportSummary.activeJobs }}</div>
        <div class="summary-label">{{ t('admin.activeJobs') }}</div>
      </div>
      <div class="summary-card card">
        <div class="summary-value">{{ jobboard.reportSummary.completedJobs }}</div>
        <div class="summary-label">{{ t('admin.completed') }}</div>
      </div>
      <div class="summary-card card traffic-green">
        <div class="summary-value">{{ jobboard.reportSummary.greenFamilies }}</div>
        <div class="summary-label">{{ t('admin.trafficLight.green') }}</div>
      </div>
      <div class="summary-card card traffic-yellow">
        <div class="summary-value">{{ jobboard.reportSummary.yellowFamilies }}</div>
        <div class="summary-label">{{ t('admin.trafficLight.yellow') }}</div>
      </div>
      <div class="summary-card card traffic-red">
        <div class="summary-value">{{ jobboard.reportSummary.redFamilies }}</div>
        <div class="summary-label">{{ t('admin.trafficLight.red') }}</div>
      </div>
    </div>

    <LoadingSpinner v-if="jobboard.loading" />

    <DataTable v-else :value="jobboard.report" stripedRows class="report-table">
      <Column field="familyName" :header="t('admin.familyCol')" sortable />
      <Column :header="t('admin.progressCol')" sortable sortField="completedHours">
        <template #body="{ data }">
          <div class="progress-bar-container">
            <div
              class="progress-bar"
              :class="data.trafficLight.toLowerCase()"
              :style="{ width: progressPercent(data.completedHours, data.targetHours) + '%' }"
            />
            <span class="progress-text">
              {{ data.completedHours }}/{{ data.targetHours }}h
              ({{ progressPercent(data.completedHours, data.targetHours) }}%)
            </span>
          </div>
        </template>
      </Column>
      <Column field="pendingHours" :header="t('admin.pendingCol')" sortable>
        <template #body="{ data }">
          {{ data.pendingHours }}h
        </template>
      </Column>
      <Column field="remainingHours" :header="t('admin.remainingCol')" sortable>
        <template #body="{ data }">
          {{ data.remainingHours }}h
        </template>
      </Column>
      <Column :header="t('admin.trafficLightCol')" sortable sortField="trafficLight">
        <template #body="{ data }">
          <Tag
            :value="trafficLightLabel(data.trafficLight)"
            :severity="trafficLightSeverity(data.trafficLight)"
          />
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.summary-card {
  text-align: center;
  padding: 1rem;
}

.summary-value {
  font-size: 1.75rem;
  font-weight: 700;
}

.summary-label {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.25rem;
}

.traffic-green .summary-value { color: #16a34a; }
.traffic-yellow .summary-value { color: #ca8a04; }
.traffic-red .summary-value { color: #dc2626; }

.progress-bar-container {
  position: relative;
  height: 24px;
  background: var(--mw-border-light);
  border-radius: 12px;
  overflow: hidden;
  min-width: 150px;
}

.progress-bar {
  height: 100%;
  border-radius: 12px;
  transition: width 0.3s;
}

.progress-bar.green { background: #16a34a; }
.progress-bar.yellow { background: #ca8a04; }
.progress-bar.red { background: #dc2626; }

.progress-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--mw-font-size-xs);
  font-weight: 600;
  color: var(--mw-text);
}
</style>
