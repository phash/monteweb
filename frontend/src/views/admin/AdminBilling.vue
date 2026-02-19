<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { useBillingStore } from '@/stores/billing'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import DatePicker from 'primevue/datepicker'
import InputText from 'primevue/inputtext'
import Dialog from 'primevue/dialog'

const { t } = useI18n()
const toast = useToast()
const billing = useBillingStore()

const newName = ref('')
const newStartDate = ref<Date | null>(null)
const newEndDate = ref<Date | null>(null)
const showCloseDialog = ref(false)
const expandedRows = ref<Record<string, boolean>>({})

onMounted(async () => {
  await billing.fetchPeriods()
  if (billing.activePeriod) {
    await billing.fetchReport(billing.activePeriod.id)
  }
})

async function createPeriod() {
  if (!newName.value || !newStartDate.value || !newEndDate.value) return
  try {
    await billing.createPeriod({
      name: newName.value,
      startDate: formatDateISO(newStartDate.value),
      endDate: formatDateISO(newEndDate.value),
    })
    toast.add({ severity: 'success', summary: t('billing.periodCreated'), life: 3000 })
    if (billing.activePeriod) {
      await billing.fetchReport(billing.activePeriod.id)
    }
    newName.value = ''
    newStartDate.value = null
    newEndDate.value = null
  } catch {
    toast.add({ severity: 'error', summary: t('billing.createError'), life: 5000 })
  }
}

async function closePeriod() {
  if (!billing.activePeriod) return
  showCloseDialog.value = false
  try {
    await billing.closePeriod(billing.activePeriod.id)
    toast.add({ severity: 'success', summary: t('billing.periodClosed'), life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: t('billing.closeError'), life: 5000 })
  }
}

async function loadClosedReport(periodId: string) {
  await billing.fetchReport(periodId)
}

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

function formatDateISO(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

function formatDateDisplay(dateStr: string): string {
  const parts = dateStr.split('-')
  return `${parts[2]}.${parts[1]}.${parts[0]}`
}

function translateRole(role: string): string {
  switch (role) {
    case 'PARENT': return t('billing.roleParent')
    case 'CHILD': return t('billing.roleChild')
    default: return role
  }
}
</script>

<template>
  <div>
    <PageTitle :title="t('billing.title')" :subtitle="t('billing.subtitle')" />

    <LoadingSpinner v-if="billing.loading && !billing.report" />

    <!-- No active period: Create form -->
    <div v-else-if="!billing.activePeriod" class="card create-period-card">
      <h3>{{ t('billing.createPeriod') }}</h3>
      <div class="create-form">
        <div class="form-field">
          <label>{{ t('billing.periodName') }}</label>
          <InputText v-model="newName" :placeholder="t('billing.periodNamePlaceholder')" class="w-full" />
        </div>
        <div class="form-row">
          <div class="form-field">
            <label>{{ t('billing.startDate') }}</label>
            <DatePicker v-model="newStartDate" dateFormat="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-field">
            <label>{{ t('billing.endDate') }}</label>
            <DatePicker v-model="newEndDate" dateFormat="dd.mm.yy" class="w-full" />
          </div>
        </div>
        <Button
          :label="t('billing.create')"
          icon="pi pi-plus"
          :disabled="!newName || !newStartDate || !newEndDate"
          @click="createPeriod"
        />
      </div>
    </div>

    <!-- Active period with report -->
    <template v-else>
      <!-- Period info card -->
      <div class="card period-info">
        <div class="period-header">
          <div>
            <h3>{{ billing.activePeriod.name }}</h3>
            <p class="period-dates">
              {{ formatDateDisplay(billing.activePeriod.startDate) }} -
              {{ formatDateDisplay(billing.activePeriod.endDate) }}
            </p>
          </div>
          <Tag :value="t('billing.statusActive')" severity="success" />
        </div>
      </div>

      <!-- Summary cards -->
      <div v-if="billing.report" class="summary-cards">
        <div class="summary-card card">
          <div class="summary-value">{{ billing.report.summary.totalFamilies }}</div>
          <div class="summary-label">{{ t('billing.families') }}</div>
        </div>
        <div class="summary-card card">
          <div class="summary-value">{{ billing.report.summary.averageHours }}h</div>
          <div class="summary-label">{{ t('billing.average') }}</div>
        </div>
        <div class="summary-card card">
          <div class="summary-value">{{ billing.report.summary.totalHoursAll }}h</div>
          <div class="summary-label">{{ t('billing.totalHours') }}</div>
        </div>
        <div class="summary-card card traffic-green">
          <div class="summary-value">{{ billing.report.summary.greenCount }}</div>
          <div class="summary-label">{{ t('admin.trafficLight.green') }}</div>
        </div>
        <div class="summary-card card traffic-yellow">
          <div class="summary-value">{{ billing.report.summary.yellowCount }}</div>
          <div class="summary-label">{{ t('admin.trafficLight.yellow') }}</div>
        </div>
        <div class="summary-card card traffic-red">
          <div class="summary-value">{{ billing.report.summary.redCount }}</div>
          <div class="summary-label">{{ t('admin.trafficLight.red') }}</div>
        </div>
      </div>

      <!-- Report table -->
      <LoadingSpinner v-if="billing.loading" />
      <DataTable
        v-else-if="billing.report"
        v-model:expandedRows="expandedRows"
        :value="billing.report.families"
        dataKey="familyId"
        stripedRows
        class="report-table"
      >
        <Column expander style="width: 3rem" />
        <Column field="familyName" :header="t('admin.familyCol')" sortable />
        <Column :header="t('admin.progressCol')" sortable sortField="totalHours">
          <template #body="{ data }">
            <div class="progress-bar-container">
              <div
                class="progress-bar"
                :class="data.trafficLight.toLowerCase()"
                :style="{ width: progressPercent(data.totalHours, data.targetHours) + '%' }"
              />
              <span class="progress-text">
                {{ data.totalHours }}/{{ data.targetHours }}h
                ({{ progressPercent(data.totalHours, data.targetHours) }}%)
              </span>
            </div>
          </template>
        </Column>
        <Column field="jobHours" :header="t('admin.jobHoursCol')" sortable>
          <template #body="{ data }">{{ data.jobHours }}h</template>
        </Column>
        <Column field="cleaningHours" :header="t('admin.cleaningHoursCol')" sortable>
          <template #body="{ data }">{{ data.cleaningHours }}h</template>
        </Column>
        <Column field="balance" :header="t('billing.balanceCol')" sortable>
          <template #body="{ data }">
            <span :class="{ negative: data.balance < 0 }">
              {{ data.balance >= 0 ? '+' : '' }}{{ data.balance }}h
            </span>
          </template>
        </Column>
        <Column field="targetCleaningHours" :header="t('billing.targetCleaningCol')" sortable>
          <template #body="{ data }">{{ data.targetCleaningHours }}h</template>
        </Column>
        <Column field="cleaningBalance" :header="t('billing.cleaningBalanceCol')" sortable>
          <template #body="{ data }">
            <span :class="{ negative: data.cleaningBalance < 0 }">
              {{ data.cleaningBalance >= 0 ? '+' : '' }}{{ data.cleaningBalance }}h
            </span>
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
        <template #expansion="{ data }">
          <div class="expansion-content">
            <h4>{{ t('billing.members') }}</h4>
            <ul class="member-list">
              <li v-for="member in data.members" :key="member.userId">
                {{ member.displayName }}
                <Tag :value="translateRole(member.role)" severity="secondary" class="ml-2" />
              </li>
            </ul>
          </div>
        </template>
      </DataTable>

      <!-- Action bar -->
      <div class="action-bar">
        <div class="flex gap-2">
          <Button
            :label="t('admin.pdfExport')"
            icon="pi pi-file-pdf"
            severity="secondary"
            @click="billing.exportPdf(billing.activePeriod!.id)"
          />
          <Button
            :label="t('admin.csvExport')"
            icon="pi pi-download"
            severity="secondary"
            @click="billing.exportCsv(billing.activePeriod!.id)"
          />
        </div>
        <Button
          :label="t('billing.closePeriod')"
          icon="pi pi-lock"
          severity="danger"
          @click="showCloseDialog = true"
        />
      </div>
    </template>

    <!-- History of closed periods -->
    <div v-if="billing.periods.filter(p => p.status === 'CLOSED').length > 0" class="card history-card">
      <h3>{{ t('billing.history') }}</h3>
      <div
        v-for="period in billing.periods.filter(p => p.status === 'CLOSED')"
        :key="period.id"
        class="history-item"
      >
        <div class="history-info">
          <strong>{{ period.name }}</strong>
          <span class="history-dates">
            {{ formatDateDisplay(period.startDate) }} - {{ formatDateDisplay(period.endDate) }}
          </span>
        </div>
        <div class="flex gap-2">
          <Button
            icon="pi pi-eye"
            severity="secondary"
            size="small"
            :label="t('billing.viewReport')"
            @click="loadClosedReport(period.id)"
          />
          <Button
            icon="pi pi-file-pdf"
            severity="secondary"
            size="small"
            @click="billing.exportPdf(period.id)"
          />
          <Button
            icon="pi pi-download"
            severity="secondary"
            size="small"
            @click="billing.exportCsv(period.id)"
          />
        </div>
      </div>
    </div>

    <!-- Close confirmation dialog -->
    <Dialog
      v-model:visible="showCloseDialog"
      :header="t('billing.closeConfirmTitle')"
      :modal="true"
      :style="{ width: '450px' }"
    >
      <p>{{ t('billing.closeConfirmMessage') }}</p>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showCloseDialog = false" />
        <Button :label="t('billing.closeConfirm')" severity="danger" @click="closePeriod" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.create-period-card {
  max-width: 600px;
  padding: 1.5rem;
}

.create-period-card h3 {
  margin-bottom: 1rem;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-field label {
  font-size: var(--mw-font-size-sm);
  font-weight: 600;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.period-info {
  padding: 1.5rem;
  margin-bottom: 1rem;
}

.period-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.period-header h3 {
  margin: 0;
}

.period-dates {
  color: var(--mw-text-secondary);
  font-size: var(--mw-font-size-sm);
  margin-top: 0.25rem;
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

.negative { color: #dc2626; font-weight: 600; }

.expansion-content {
  padding: 0.75rem 1rem;
}

.expansion-content h4 {
  margin-bottom: 0.5rem;
  font-size: var(--mw-font-size-sm);
}

.member-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.member-list li {
  padding: 0.25rem 0;
  display: flex;
  align-items: center;
}

.ml-2 { margin-left: 0.5rem; }

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
  padding: 1rem 0;
}

.history-card {
  margin-top: 2rem;
  padding: 1.5rem;
}

.history-card h3 {
  margin-bottom: 1rem;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--mw-border-light);
}

.history-item:last-child {
  border-bottom: none;
}

.history-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.history-dates {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-secondary);
}

@media (max-width: 768px) {
  .form-row { grid-template-columns: 1fr; }
  .action-bar { flex-direction: column; gap: 1rem; }
  .history-item { flex-direction: column; align-items: flex-start; gap: 0.5rem; }
}
</style>
