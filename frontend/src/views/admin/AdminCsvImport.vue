<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { useRouter } from 'vue-router'
import { adminApi } from '@/api/admin.api'
import type { CsvImportResult } from '@/types/user'
import PageTitle from '@/components/common/PageTitle.vue'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import Stepper from 'primevue/stepper'
import StepList from 'primevue/steplist'
import StepPanels from 'primevue/steppanels'
import Step from 'primevue/step'
import StepPanel from 'primevue/steppanel'

const { t } = useI18n()
const toast = useToast()
const router = useRouter()

const activeStep = ref('1')
const selectedFile = ref<File | null>(null)
const previewResult = ref<CsvImportResult | null>(null)
const importResult = ref<CsvImportResult | null>(null)
const loading = ref(false)
const importing = ref(false)
const dragOver = ref(false)

const exampleRows = [
  { email: 'maria.mueller@schule.de', firstName: 'Maria', lastName: 'Mueller', role: 'PARENT', familyName: 'Familie Mueller', familyRole: 'PARENT', sectionSlug: 'grundstufe' },
  { email: 'thomas.mueller@schule.de', firstName: 'Thomas', lastName: 'Mueller', role: 'PARENT', familyName: 'Familie Mueller', familyRole: 'PARENT', sectionSlug: 'grundstufe' },
  { email: 'leon.mueller@schule.de', firstName: 'Leon', lastName: 'Mueller', role: 'STUDENT', familyName: 'Familie Mueller', familyRole: 'CHILD', sectionSlug: 'grundstufe' },
  { email: 'sandra.schmidt@schule.de', firstName: 'Sandra', lastName: 'Schmidt', role: 'PARENT', familyName: 'Familie Schmidt', familyRole: 'PARENT', sectionSlug: 'mittelstufe' },
  { email: 'julia.weber@schule.de', firstName: 'Julia', lastName: 'Weber', role: 'TEACHER', familyName: '', familyRole: '', sectionSlug: 'grundstufe' },
]

const hasErrors = computed(() => {
  return previewResult.value ? previewResult.value.errorsCount > 0 : false
})

const canImport = computed(() => {
  return previewResult.value && !hasErrors.value && previewResult.value.totalRows > 0
})

async function downloadExample() {
  try {
    const response = await adminApi.downloadExampleCsv()
    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'import-example.csv'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch {
    toast.add({ severity: 'error', summary: 'Download failed', life: 3000 })
  }
}

function onFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files && input.files.length > 0) {
    selectedFile.value = input.files[0] ?? null
    previewResult.value = null
    importResult.value = null
  }
}

function onDrop(event: DragEvent) {
  dragOver.value = false
  event.preventDefault()
  if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
    const file = event.dataTransfer.files[0] ?? null
    if (file && (file.name.endsWith('.csv') || file.type === 'text/csv' || file.type === 'application/vnd.ms-excel')) {
      selectedFile.value = file
      previewResult.value = null
      importResult.value = null
    } else {
      toast.add({ severity: 'warn', summary: 'Bitte nur CSV-Dateien', life: 3000 })
    }
  }
}

function onDragOver(event: DragEvent) {
  event.preventDefault()
  dragOver.value = true
}

function onDragLeave() {
  dragOver.value = false
}

async function validateFile() {
  if (!selectedFile.value) return
  loading.value = true
  try {
    const res = await adminApi.uploadCsv(selectedFile.value, true)
    previewResult.value = res.data.data
  } catch {
    toast.add({ severity: 'error', summary: 'Validierung fehlgeschlagen', life: 5000 })
  } finally {
    loading.value = false
  }
}

async function executeImport() {
  if (!selectedFile.value) return
  importing.value = true
  try {
    const res = await adminApi.uploadCsv(selectedFile.value, false)
    importResult.value = res.data.data
    activeStep.value = '3'
    toast.add({ severity: 'success', summary: t('csvImport.resultSuccess'), life: 5000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Import fehlgeschlagen', life: 5000 })
  } finally {
    importing.value = false
  }
}

function startOver() {
  selectedFile.value = null
  previewResult.value = null
  importResult.value = null
  activeStep.value = '1'
}

function goToUsers() {
  router.push({ name: 'admin-users' })
}

function getRoleSeverity(role: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' | undefined {
  switch (role) {
    case 'PARENT': return 'success'
    case 'STUDENT': return 'info'
    case 'TEACHER': return 'warn'
    case 'SECTION_ADMIN': return 'danger'
    default: return 'secondary'
  }
}
</script>

<template>
  <div class="csv-import-view">
    <PageTitle :title="t('csvImport.title')" />
    <p class="subtitle">{{ t('csvImport.subtitle') }}</p>

    <Stepper v-model:value="activeStep" linear>
      <StepList>
        <Step value="1">{{ t('csvImport.stepInstructions') }}</Step>
        <Step value="2">{{ t('csvImport.stepUpload') }}</Step>
        <Step value="3">{{ t('csvImport.stepImport') }}</Step>
      </StepList>
      <StepPanels>
        <!-- Step 1: Instructions & Example -->
        <StepPanel v-slot="{ activateCallback }" value="1">
          <div class="step-content">
            <Message severity="info" :closable="false">
              {{ t('csvImport.description') }}
            </Message>

            <div class="card mt-3">
              <h3>{{ t('csvImport.formatTitle') }}</h3>
              <p>{{ t('csvImport.formatDescription') }}</p>

              <div class="format-table">
                <table class="column-info-table">
                  <thead>
                    <tr>
                      <th>Spalte</th>
                      <th>{{ t('common.description') }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td><code>email</code></td>
                      <td>{{ t('csvImport.colEmail') }}</td>
                    </tr>
                    <tr>
                      <td><code>firstName</code></td>
                      <td>{{ t('csvImport.colFirstName') }}</td>
                    </tr>
                    <tr>
                      <td><code>lastName</code></td>
                      <td>{{ t('csvImport.colLastName') }}</td>
                    </tr>
                    <tr>
                      <td><code>role</code></td>
                      <td>{{ t('csvImport.colRole') }}</td>
                    </tr>
                    <tr>
                      <td><code>familyName</code></td>
                      <td>{{ t('csvImport.colFamilyName') }}</td>
                    </tr>
                    <tr>
                      <td><code>familyRole</code></td>
                      <td>{{ t('csvImport.colFamilyRole') }}</td>
                    </tr>
                    <tr>
                      <td><code>sectionSlug</code></td>
                      <td>{{ t('csvImport.colSectionSlug') }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="hints mt-3">
                <Message severity="secondary" :closable="false" class="mb-2">
                  <i class="pi pi-info-circle mr-2" />{{ t('csvImport.separatorHint') }}
                </Message>
                <Message severity="secondary" :closable="false" class="mb-2">
                  <i class="pi pi-info-circle mr-2" />{{ t('csvImport.encodingHint') }}
                </Message>
                <Message severity="warn" :closable="false">
                  <i class="pi pi-lock mr-2" />{{ t('csvImport.passwordHint') }}
                </Message>
              </div>
            </div>

            <div class="card mt-3">
              <h3>{{ t('csvImport.exampleTitle') }}</h3>
              <DataTable :value="exampleRows" size="small" stripedRows class="example-table">
                <Column field="email" header="email" />
                <Column field="firstName" header="firstName" />
                <Column field="lastName" header="lastName" />
                <Column field="role" header="role">
                  <template #body="{ data }">
                    <Tag :value="data.role" :severity="getRoleSeverity(data.role)" />
                  </template>
                </Column>
                <Column field="familyName" header="familyName" />
                <Column field="familyRole" header="familyRole" />
                <Column field="sectionSlug" header="sectionSlug" />
              </DataTable>
              <div class="mt-3">
                <Button
                  :label="t('csvImport.downloadExample')"
                  icon="pi pi-download"
                  severity="secondary"
                  @click="downloadExample"
                />
              </div>
            </div>

            <div class="step-actions mt-3">
              <Button :label="t('common.next')" icon="pi pi-arrow-right" iconPos="right" @click="activateCallback('2')" />
            </div>
          </div>
        </StepPanel>

        <!-- Step 2: Upload & Preview -->
        <StepPanel v-slot="{ activateCallback }" value="2">
          <div class="step-content">
            <div class="card">
              <h3>{{ t('csvImport.uploadTitle') }}</h3>

              <!-- Drop zone -->
              <div
                class="drop-zone"
                :class="{ 'drag-over': dragOver, 'has-file': selectedFile }"
                @drop="onDrop"
                @dragover="onDragOver"
                @dragleave="onDragLeave"
              >
                <template v-if="!selectedFile">
                  <i class="pi pi-cloud-upload drop-icon" />
                  <p>{{ t('csvImport.dragDrop') }}</p>
                  <label class="file-input-label">
                    <Button :label="t('csvImport.uploadButton')" icon="pi pi-file" severity="secondary" as="span" />
                    <input type="file" accept=".csv" class="hidden-input" @change="onFileSelect">
                  </label>
                </template>
                <template v-else>
                  <i class="pi pi-file drop-icon file-selected-icon" />
                  <p>{{ t('csvImport.fileSelected', { name: selectedFile.name }) }}</p>
                  <label class="file-input-label">
                    <Button :label="t('csvImport.changeFile')" icon="pi pi-refresh" severity="secondary" size="small" as="span" />
                    <input type="file" accept=".csv" class="hidden-input" @change="onFileSelect">
                  </label>
                </template>
              </div>

              <div class="mt-3 text-center" v-if="selectedFile && !previewResult">
                <Button
                  :label="loading ? t('csvImport.validating') : t('csvImport.stepUpload')"
                  icon="pi pi-check"
                  :loading="loading"
                  @click="validateFile"
                />
              </div>
            </div>

            <!-- Preview results -->
            <div v-if="previewResult" class="card mt-3">
              <h3>{{ t('csvImport.previewTitle') }}</h3>

              <div class="preview-summary mb-3">
                <Message v-if="!hasErrors" severity="success" :closable="false">
                  {{ t('csvImport.previewValid') }} -
                  {{ t('csvImport.previewSummary', { users: previewResult.usersCreated, families: previewResult.familiesCreated }) }}
                </Message>
                <Message v-else severity="error" :closable="false">
                  {{ t('csvImport.previewErrors', { count: previewResult.errorsCount }) }}
                </Message>
              </div>

              <DataTable
                :value="previewResult.preview"
                size="small"
                stripedRows
                paginator
                :rows="20"
                :rowsPerPageOptions="[10, 20, 50, 100]"
              >
                <Column field="row" :header="t('csvImport.colRow')" style="width: 60px" />
                <Column field="email" header="E-Mail" />
                <Column field="name" :header="t('csvImport.colName')" />
                <Column field="role" :header="t('admin.dashboard.usersDesc').split(',')[0]?.trim() || 'Rolle'">
                  <template #body="{ data }">
                    <Tag v-if="data.role" :value="data.role" :severity="getRoleSeverity(data.role)" />
                  </template>
                </Column>
                <Column field="familyName" :header="t('admin.dashboard.families')" />
                <Column :header="t('csvImport.colStatus')" style="width: 120px">
                  <template #body="{ data }">
                    <Tag
                      :value="data.valid ? t('csvImport.statusValid') : t('csvImport.statusError')"
                      :severity="data.valid ? 'success' : 'danger'"
                    />
                  </template>
                </Column>
                <Column header="" style="min-width: 200px">
                  <template #body="{ data }">
                    <span v-if="data.error" class="error-text">{{ data.error }}</span>
                  </template>
                </Column>
              </DataTable>
            </div>

            <div class="step-actions mt-3">
              <Button :label="t('common.previous')" icon="pi pi-arrow-left" severity="secondary" @click="activateCallback('1')" />
              <Button
                v-if="canImport"
                :label="importing ? t('csvImport.importing') : t('csvImport.importButton')"
                icon="pi pi-upload"
                iconPos="right"
                :loading="importing"
                @click="executeImport"
              />
            </div>
          </div>
        </StepPanel>

        <!-- Step 3: Results -->
        <StepPanel value="3">
          <div class="step-content">
            <div v-if="importResult" class="card">
              <h3>{{ t('csvImport.resultTitle') }}</h3>

              <Message severity="success" :closable="false" class="mb-3">
                {{ t('csvImport.resultSuccess') }}
              </Message>

              <div class="result-stats">
                <div class="stat-card">
                  <i class="pi pi-users stat-icon" />
                  <div class="stat-value">{{ importResult.usersCreated }}</div>
                  <div class="stat-label">{{ t('csvImport.resultUsersCreated', { count: importResult.usersCreated }) }}</div>
                </div>
                <div class="stat-card">
                  <i class="pi pi-heart stat-icon" />
                  <div class="stat-value">{{ importResult.familiesCreated }}</div>
                  <div class="stat-label">{{ t('csvImport.resultFamiliesCreated', { count: importResult.familiesCreated }) }}</div>
                </div>
                <div class="stat-card" :class="{ 'stat-error': importResult.errorsCount > 0 }">
                  <i class="pi pi-exclamation-triangle stat-icon" />
                  <div class="stat-value">{{ importResult.errorsCount }}</div>
                  <div class="stat-label">
                    {{ importResult.errorsCount > 0
                      ? t('csvImport.resultErrors', { count: importResult.errorsCount })
                      : t('csvImport.resultNoErrors') }}
                  </div>
                </div>
              </div>

              <!-- Error details if any -->
              <div v-if="importResult.errors.length > 0" class="card mt-3">
                <h4>{{ t('csvImport.errorDetails') }}</h4>
                <DataTable :value="importResult.errors" size="small" stripedRows>
                  <Column field="row" :header="t('csvImport.colRow')" style="width: 80px" />
                  <Column field="message" header="Fehler" />
                </DataTable>
              </div>
            </div>

            <div class="step-actions mt-3">
              <Button :label="t('csvImport.startOver')" icon="pi pi-refresh" severity="secondary" @click="startOver" />
              <Button :label="t('csvImport.goToUsers')" icon="pi pi-users" @click="goToUsers" />
            </div>
          </div>
        </StepPanel>
      </StepPanels>
    </Stepper>
  </div>
</template>

<style scoped>
.csv-import-view {
  max-width: 1200px;
  margin: 0 auto;
}

.subtitle {
  color: var(--mw-text-secondary);
  margin-bottom: 1.5rem;
}

.step-content {
  padding: 1rem 0;
}

.step-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.mt-3 {
  margin-top: 1rem;
}

.mb-2 {
  margin-bottom: 0.5rem;
}

.mb-3 {
  margin-bottom: 1rem;
}

.mr-2 {
  margin-right: 0.5rem;
}

.column-info-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 0.75rem;
}

.column-info-table th,
.column-info-table td {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--mw-border);
  text-align: left;
}

.column-info-table th {
  background: var(--mw-surface-100);
  font-weight: 600;
}

.column-info-table code {
  background: var(--mw-surface-100);
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
}

.drop-zone {
  border: 2px dashed var(--mw-border);
  border-radius: 12px;
  padding: 2rem;
  text-align: center;
  transition: all 0.2s;
  cursor: pointer;
}

.drop-zone.drag-over {
  border-color: var(--mw-primary);
  background: var(--mw-surface-50);
}

.drop-zone.has-file {
  border-style: solid;
  border-color: var(--mw-primary);
}

.drop-icon {
  font-size: 3rem;
  color: var(--mw-text-secondary);
  display: block;
  margin-bottom: 0.75rem;
}

.file-selected-icon {
  color: var(--mw-primary);
}

.hidden-input {
  display: none;
}

.file-input-label {
  cursor: pointer;
}

.text-center {
  text-align: center;
}

.error-text {
  color: var(--p-red-500, #ef4444);
  font-size: 0.85rem;
}

.result-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.stat-card {
  text-align: center;
  padding: 1.5rem;
  border-radius: 12px;
  background: var(--mw-surface-50);
  border: 1px solid var(--mw-border);
}

.stat-card.stat-error .stat-icon,
.stat-card.stat-error .stat-value {
  color: var(--p-red-500, #ef4444);
}

.stat-icon {
  font-size: 2rem;
  color: var(--mw-primary);
  margin-bottom: 0.5rem;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: var(--mw-primary);
}

.stat-label {
  font-size: 0.875rem;
  color: var(--mw-text-secondary);
  margin-top: 0.25rem;
}

.example-table :deep(.p-datatable-header-cell) {
  font-size: 0.85rem;
}

.format-table {
  overflow-x: auto;
}

.preview-summary {
  margin-top: 0;
}

h3 {
  margin-top: 0;
  margin-bottom: 0.75rem;
}

h4 {
  margin-top: 0;
  margin-bottom: 0.5rem;
}
</style>
