<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { useErrorReportsStore } from '@/stores/errorReports'
import { useAdminStore } from '@/stores/admin'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import Dialog from 'primevue/dialog'

const { t } = useI18n()
const toast = useToast()
const store = useErrorReportsStore()
const adminStore = useAdminStore()

const statusFilter = ref<string | null>(null)
const sourceFilter = ref<string | null>(null)
const expandedRows = ref<Record<string, boolean>>({})
const deleteDialog = ref(false)
const deleteTarget = ref<string | null>(null)
const currentPage = ref(0)

// GitHub config
const githubRepo = ref('')
const githubPat = ref('')
const savingGithub = ref(false)

const statusOptions = [
  { label: t('errorReports.all'), value: null },
  { label: t('errorReports.statusNew'), value: 'NEW' },
  { label: t('errorReports.statusReported'), value: 'REPORTED' },
  { label: t('errorReports.statusResolved'), value: 'RESOLVED' },
  { label: t('errorReports.statusIgnored'), value: 'IGNORED' },
]

const sourceOptions = [
  { label: t('errorReports.all'), value: null },
  { label: 'Backend', value: 'BACKEND' },
  { label: 'Frontend', value: 'FRONTEND' },
]

const statusChangeOptions = [
  { label: t('errorReports.statusNew'), value: 'NEW' },
  { label: t('errorReports.statusResolved'), value: 'RESOLVED' },
  { label: t('errorReports.statusIgnored'), value: 'IGNORED' },
]

onMounted(async () => {
  await loadReports()
  try {
    await adminStore.fetchConfig()
    githubRepo.value = adminStore.config?.githubRepo || ''
    store.githubPatConfigured = !!adminStore.config?.githubPatConfigured
    store.githubRepo = githubRepo.value
  } catch {
    // Admin config might not have these fields yet
  }
})

async function loadReports() {
  await store.fetchReports({
    status: statusFilter.value || undefined,
    source: sourceFilter.value || undefined,
    page: currentPage.value,
    size: 20,
    sort: 'lastSeenAt,desc',
  })
}

function onFilterChange() {
  currentPage.value = 0
  loadReports()
}

async function onPage(event: { page: number }) {
  currentPage.value = event.page
  await loadReports()
}

async function onStatusChange(id: string, status: string) {
  try {
    await store.updateStatus(id, status)
    toast.add({ severity: 'success', summary: t('errorReports.statusUpdated'), life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Error', life: 5000 })
  }
}

async function onCreateGithubIssue(id: string) {
  try {
    await store.createGithubIssue(id)
    toast.add({ severity: 'success', summary: t('errorReports.githubIssueCreated'), life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Error', life: 5000 })
  }
}

function confirmDelete(id: string) {
  deleteTarget.value = id
  deleteDialog.value = true
}

async function onDelete() {
  if (!deleteTarget.value) return
  deleteDialog.value = false
  try {
    await store.deleteReport(deleteTarget.value)
    toast.add({ severity: 'success', summary: t('errorReports.deleted'), life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Error', life: 5000 })
  }
  deleteTarget.value = null
}

async function saveGithubConfig() {
  if (!githubRepo.value) return
  savingGithub.value = true
  try {
    await store.updateGithubConfig(githubRepo.value, githubPat.value)
    toast.add({ severity: 'success', summary: t('errorReports.githubConfigSaved'), life: 3000 })
    githubPat.value = ''
  } catch {
    toast.add({ severity: 'error', summary: 'Error', life: 5000 })
  } finally {
    savingGithub.value = false
  }
}

function statusSeverity(status: string): string {
  switch (status) {
    case 'NEW': return 'danger'
    case 'REPORTED': return 'info'
    case 'RESOLVED': return 'success'
    case 'IGNORED': return 'secondary'
    default: return 'info'
  }
}

function statusLabel(status: string): string {
  switch (status) {
    case 'NEW': return t('errorReports.statusNew')
    case 'REPORTED': return t('errorReports.statusReported')
    case 'RESOLVED': return t('errorReports.statusResolved')
    case 'IGNORED': return t('errorReports.statusIgnored')
    default: return status
  }
}

function sourceSeverity(source: string): string {
  return source === 'BACKEND' ? 'warn' : 'info'
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('de-DE') + ' ' + d.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' })
}

function truncate(str: string | null, maxLen: number): string {
  if (!str) return '-'
  return str.length > maxLen ? str.substring(0, maxLen) + '...' : str
}
</script>

<template>
  <div>
    <PageTitle :title="t('errorReports.title')" :subtitle="t('errorReports.subtitle')" />

    <!-- GitHub Config Card -->
    <div v-if="!store.githubPatConfigured" class="card github-config-card">
      <h3>{{ t('errorReports.githubConfig') }}</h3>
      <p class="hint-text">{{ t('errorReports.githubNotConfigured') }}</p>
      <div class="github-form">
        <div class="form-field">
          <label>{{ t('errorReports.githubRepo') }}</label>
          <InputText v-model="githubRepo" placeholder="owner/repo" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('errorReports.githubPat') }}</label>
          <InputText v-model="githubPat" type="password" class="w-full" />
        </div>
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :disabled="!githubRepo || !githubPat"
          :loading="savingGithub"
          @click="saveGithubConfig"
        />
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <div class="filter-group">
        <label>{{ t('common.status') }}</label>
        <Select
          v-model="statusFilter"
          :options="statusOptions"
          optionLabel="label"
          optionValue="value"
          class="filter-select"
          @change="onFilterChange"
        />
      </div>
      <div class="filter-group">
        <label>{{ t('errorReports.source') }}</label>
        <Select
          v-model="sourceFilter"
          :options="sourceOptions"
          optionLabel="label"
          optionValue="value"
          class="filter-select"
          @change="onFilterChange"
        />
      </div>
    </div>

    <!-- Loading -->
    <LoadingSpinner v-if="store.loading && store.reports.length === 0" />

    <!-- Empty state -->
    <div v-else-if="store.reports.length === 0" class="card empty-state">
      <i class="pi pi-check-circle empty-icon" />
      <p>{{ t('errorReports.noReports') }}</p>
    </div>

    <!-- DataTable -->
    <DataTable
      v-else
      v-model:expandedRows="expandedRows"
      :value="store.reports"
      dataKey="id"
      :loading="store.loading"
      :lazy="true"
      :paginator="true"
      :rows="20"
      :totalRecords="store.totalRecords"
      :rowsPerPageOptions="[10, 20, 50]"
      stripedRows
      @page="onPage"
    >
      <Column expander style="width: 3rem" />
      <Column :header="t('common.status')" style="width: 8rem">
        <template #body="{ data }">
          <Tag :value="statusLabel(data.status)" :severity="statusSeverity(data.status)" />
        </template>
      </Column>
      <Column :header="t('errorReports.source')" style="width: 7rem">
        <template #body="{ data }">
          <Tag :value="data.source" :severity="sourceSeverity(data.source)" />
        </template>
      </Column>
      <Column field="errorType" :header="t('errorReports.errorType')" style="width: 10rem">
        <template #body="{ data }">
          <span class="error-type">{{ data.errorType || '-' }}</span>
        </template>
      </Column>
      <Column field="message" :header="t('errorReports.message')">
        <template #body="{ data }">
          <span class="error-message" :title="data.message">{{ truncate(data.message, 60) }}</span>
        </template>
      </Column>
      <Column :header="t('errorReports.occurrenceCount')" style="width: 6rem; text-align: center">
        <template #body="{ data }">
          <span class="occurrence-count">{{ data.occurrenceCount }}</span>
        </template>
      </Column>
      <Column :header="t('errorReports.lastSeen')" style="width: 10rem">
        <template #body="{ data }">
          {{ formatDateTime(data.lastSeenAt) }}
        </template>
      </Column>
      <Column :header="t('common.actions')" style="width: 16rem">
        <template #body="{ data }">
          <div class="action-buttons">
            <Button
              v-if="store.githubPatConfigured && data.status !== 'REPORTED'"
              icon="pi pi-github"
              severity="secondary"
              size="small"
              v-tooltip="t('errorReports.createGithubIssue')"
              @click="onCreateGithubIssue(data.id)"
            />
            <Select
              :modelValue="data.status"
              :options="statusChangeOptions"
              optionLabel="label"
              optionValue="value"
              class="status-select"
              @update:modelValue="(val: string) => onStatusChange(data.id, val)"
            />
            <Button
              icon="pi pi-trash"
              severity="danger"
              size="small"
              text
              @click="confirmDelete(data.id)"
            />
          </div>
        </template>
      </Column>

      <!-- Expansion template -->
      <template #expansion="{ data }">
        <div class="expansion-content">
          <div v-if="data.stackTrace" class="detail-section">
            <h4>{{ t('errorReports.stackTrace') }}</h4>
            <pre class="stack-trace">{{ data.stackTrace }}</pre>
          </div>
          <div class="detail-grid">
            <div v-if="data.userAgent" class="detail-item">
              <strong>{{ t('errorReports.userAgent') }}:</strong>
              <span>{{ data.userAgent }}</span>
            </div>
            <div v-if="data.requestUrl" class="detail-item">
              <strong>{{ t('errorReports.requestUrl') }}:</strong>
              <span>{{ data.requestUrl }}</span>
            </div>
            <div class="detail-item">
              <strong>{{ t('errorReports.firstSeen') }}:</strong>
              <span>{{ formatDateTime(data.firstSeenAt) }}</span>
            </div>
            <div class="detail-item">
              <strong>Fingerprint:</strong>
              <span class="fingerprint">{{ data.fingerprint }}</span>
            </div>
            <div v-if="data.githubIssueUrl" class="detail-item">
              <strong>GitHub Issue:</strong>
              <a :href="data.githubIssueUrl" target="_blank" rel="noopener noreferrer">
                {{ data.githubIssueUrl }}
              </a>
            </div>
          </div>
        </div>
      </template>
    </DataTable>

    <!-- Delete confirmation dialog -->
    <Dialog
      v-model:visible="deleteDialog"
      :header="t('common.confirmDeleteTitle')"
      :modal="true"
      :style="{ width: '450px' }"
    >
      <p>{{ t('errorReports.confirmDelete') }}</p>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="deleteDialog = false" />
        <Button :label="t('common.delete')" severity="danger" @click="onDelete" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.github-config-card {
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  max-width: 600px;
}

.github-config-card h3 {
  margin-bottom: 0.5rem;
}

.hint-text {
  color: var(--mw-text-secondary);
  font-size: var(--mw-font-size-sm);
  margin-bottom: 1rem;
}

.github-form {
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

.filter-bar {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.filter-group label {
  font-size: var(--mw-font-size-xs);
  font-weight: 600;
  color: var(--mw-text-secondary);
}

.filter-select {
  min-width: 160px;
}

.empty-state {
  text-align: center;
  padding: 3rem;
}

.empty-icon {
  font-size: 3rem;
  color: var(--mw-text-muted);
  margin-bottom: 1rem;
}

.error-type {
  font-family: monospace;
  font-size: var(--mw-font-size-sm);
}

.error-message {
  font-size: var(--mw-font-size-sm);
}

.occurrence-count {
  font-weight: 700;
  font-size: var(--mw-font-size-md);
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.status-select {
  width: 120px;
}

.expansion-content {
  padding: 1rem 1.5rem;
}

.detail-section {
  margin-bottom: 1rem;
}

.detail-section h4 {
  margin-bottom: 0.5rem;
  font-size: var(--mw-font-size-sm);
}

.stack-trace {
  background: var(--mw-bg);
  border: 1px solid var(--mw-border-light);
  border-radius: 6px;
  padding: 1rem;
  font-size: 0.75rem;
  line-height: 1.5;
  overflow-x: auto;
  max-height: 300px;
  white-space: pre-wrap;
  word-break: break-all;
}

.detail-grid {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.detail-item {
  font-size: var(--mw-font-size-sm);
}

.detail-item strong {
  margin-right: 0.5rem;
}

.fingerprint {
  font-family: monospace;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
  }

  .action-buttons {
    flex-wrap: wrap;
  }
}
</style>
