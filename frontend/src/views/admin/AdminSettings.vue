<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { adminApi } from '@/api/admin.api'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import ToggleSwitch from 'primevue/toggleswitch'
import { useToast } from 'primevue/usetoast'
import { predefinedVacations } from '@/data/schoolVacations'

const { t } = useI18n()
const adminStore = useAdminStore()
const toast = useToast()
const saving = ref(false)
const savingHours = ref(false)
const savingVacations = ref(false)

const multilanguageEnabled = ref(true)
const defaultLanguage = ref('de')
const requireUserApproval = ref(true)
const requireAssignmentConfirmation = ref(true)
const targetHoursPerFamily = ref(30)
const targetCleaningHours = ref(3)
const bundesland = ref('BY')
const schoolVacations = ref<{ name: string; from: string; to: string }[]>([])

const languageOptions = [
  { label: 'Deutsch', value: 'de' },
  { label: 'English', value: 'en' },
]

const bundeslandOptions = [
  { label: 'Baden-Württemberg', value: 'BW' },
  { label: 'Bayern', value: 'BY' },
  { label: 'Berlin', value: 'BE' },
  { label: 'Brandenburg', value: 'BB' },
  { label: 'Bremen', value: 'HB' },
  { label: 'Hamburg', value: 'HH' },
  { label: 'Hessen', value: 'HE' },
  { label: 'Mecklenburg-Vorpommern', value: 'MV' },
  { label: 'Niedersachsen', value: 'NI' },
  { label: 'Nordrhein-Westfalen', value: 'NW' },
  { label: 'Rheinland-Pfalz', value: 'RP' },
  { label: 'Saarland', value: 'SL' },
  { label: 'Sachsen', value: 'SN' },
  { label: 'Sachsen-Anhalt', value: 'ST' },
  { label: 'Schleswig-Holstein', value: 'SH' },
  { label: 'Thüringen', value: 'TH' },
]

onMounted(async () => {
  if (!adminStore.config) {
    await adminStore.fetchConfig()
  }
  if (adminStore.config) {
    multilanguageEnabled.value = adminStore.config.multilanguageEnabled ?? true
    defaultLanguage.value = adminStore.config.defaultLanguage ?? 'de'
    requireUserApproval.value = adminStore.config.requireUserApproval ?? true
    requireAssignmentConfirmation.value = adminStore.config.requireAssignmentConfirmation ?? true
    targetHoursPerFamily.value = adminStore.config.targetHoursPerFamily ?? 30
    targetCleaningHours.value = adminStore.config.targetCleaningHours ?? 3
    bundesland.value = adminStore.config.bundesland || 'BY'
    schoolVacations.value = (adminStore.config.schoolVacations || []).map(v => ({ ...v }))
  }
})

async function saveSettings() {
  saving.value = true
  try {
    await adminStore.updateConfig({
      multilanguageEnabled: multilanguageEnabled.value,
      defaultLanguage: defaultLanguage.value,
      requireUserApproval: requireUserApproval.value,
      requireAssignmentConfirmation: requireAssignmentConfirmation.value,
    })
    toast.add({ severity: 'success', summary: t('admin.settings.saved'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    saving.value = false
  }
}

async function saveHoursConfig() {
  savingHours.value = true
  try {
    const res = await adminApi.updateConfig({
      targetHoursPerFamily: targetHoursPerFamily.value,
      targetCleaningHours: targetCleaningHours.value,
    })
    adminStore.config = res.data.data
    toast.add({ severity: 'success', summary: t('admin.hoursConfigSaved'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    savingHours.value = false
  }
}

function addVacation() {
  schoolVacations.value.push({ name: '', from: '', to: '' })
}

function removeVacation(index: number) {
  schoolVacations.value.splice(index, 1)
}

function loadVacationsForBundesland() {
  const vacations = predefinedVacations[bundesland.value]
  if (vacations) {
    schoolVacations.value = vacations.map(v => ({ ...v }))
  }
}

async function saveVacationsConfig() {
  savingVacations.value = true
  try {
    const res = await adminApi.updateConfig({
      bundesland: bundesland.value,
      schoolVacations: schoolVacations.value.filter(v => v.name && v.from && v.to),
    })
    adminStore.config = res.data.data
    toast.add({ severity: 'success', summary: t('admin.vacationsSaved'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    savingVacations.value = false
  }
}
</script>

<template>
  <div class="p-4">
    <h1 class="text-2xl font-bold mb-6">{{ t('admin.settings.title') }}</h1>

    <!-- Language Section -->
    <div class="settings-section">
      <h2 class="text-lg font-semibold mb-3">{{ t('admin.settings.language') }}</h2>
      <div class="mb-4 flex items-center gap-3">
        <ToggleSwitch v-model="multilanguageEnabled" />
        <div>
          <label class="block text-sm font-medium">{{ t('admin.settings.multilanguageEnabled') }}</label>
          <small class="text-gray-500">{{ t('admin.settings.multilanguageHint') }}</small>
        </div>
      </div>
      <div class="mb-4">
        <label class="block text-sm font-medium mb-1">{{ t('admin.settings.defaultLanguage') }}</label>
        <Select
          v-model="defaultLanguage"
          :options="languageOptions"
          optionLabel="label"
          optionValue="value"
          class="w-full md:w-1/3"
        />
      </div>
    </div>

    <!-- Registration Section -->
    <div class="settings-section">
      <h2 class="text-lg font-semibold mb-3">{{ t('admin.settings.registration') }}</h2>
      <div class="mb-4 flex items-center gap-3">
        <ToggleSwitch v-model="requireUserApproval" />
        <div>
          <label class="block text-sm font-medium">{{ t('admin.settings.requireUserApproval') }}</label>
          <small class="text-gray-500">{{ t('admin.settings.requireUserApprovalHint') }}</small>
        </div>
      </div>
    </div>

    <!-- Jobboard Section -->
    <div class="settings-section">
      <h2 class="text-lg font-semibold mb-3">{{ t('admin.settings.jobboard') }}</h2>
      <div class="mb-4 flex items-center gap-3">
        <ToggleSwitch v-model="requireAssignmentConfirmation" />
        <div>
          <label class="block text-sm font-medium">{{ t('admin.requireConfirmation') }}</label>
          <small class="text-gray-500">{{ t('admin.requireConfirmationHint') }}</small>
        </div>
      </div>
    </div>

    <Button :label="t('common.save')" icon="pi pi-check" :loading="saving" @click="saveSettings" class="mb-6" />

    <!-- Hours Configuration -->
    <div class="settings-section">
      <h2 class="text-lg font-semibold mb-3">{{ t('admin.hoursConfig') }}</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('admin.totalHoursTarget') }}</label>
          <InputNumber v-model="targetHoursPerFamily" :min="0" :max="999" :minFractionDigits="0" :maxFractionDigits="1" class="w-full" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('admin.cleaningHoursTarget') }}</label>
          <InputNumber v-model="targetCleaningHours" :min="0" :max="999" :minFractionDigits="0" :maxFractionDigits="1" class="w-full" />
        </div>
      </div>
      <Button :label="t('admin.saveHoursConfig')" icon="pi pi-check" :loading="savingHours"
              @click="saveHoursConfig" />
    </div>

    <!-- Bundesland & School Vacations -->
    <div class="settings-section">
      <h2 class="text-lg font-semibold mb-3">{{ t('admin.holidaysAndVacations') }}</h2>
      <div class="mb-4">
        <label class="block text-sm font-medium mb-1">{{ t('admin.bundesland') }}</label>
        <Select v-model="bundesland" :options="bundeslandOptions" optionLabel="label" optionValue="value"
                class="w-full md:w-1/2" />
        <small class="text-gray-500">{{ t('admin.bundeslandHint') }}</small>
      </div>

      <h3 class="text-md font-medium mb-2">{{ t('admin.schoolVacations') }}</h3>
      <div class="mb-3">
        <Button :label="t('admin.loadVacations')" icon="pi pi-download" severity="secondary" size="small"
                @click="loadVacationsForBundesland" />
        <small class="text-gray-500 ml-2">{{ t('admin.loadVacationsHint') }}</small>
      </div>
      <DataTable :value="schoolVacations" stripedRows class="mb-3">
        <template #empty>
          <span class="text-gray-400">{{ t('common.noData') }}</span>
        </template>
        <Column :header="t('admin.vacationName')">
          <template #body="{ data }">
            <InputText v-model="data.name" class="w-full" />
          </template>
        </Column>
        <Column :header="t('admin.vacationFrom')">
          <template #body="{ data }">
            <InputText v-model="data.from" placeholder="YYYY-MM-DD" class="w-full" />
          </template>
        </Column>
        <Column :header="t('admin.vacationTo')">
          <template #body="{ data }">
            <InputText v-model="data.to" placeholder="YYYY-MM-DD" class="w-full" />
          </template>
        </Column>
        <Column :header="t('common.actions')" style="width: 80px">
          <template #body="{ index }">
            <Button icon="pi pi-trash" severity="danger" text rounded size="small" @click="removeVacation(index)" />
          </template>
        </Column>
      </DataTable>
      <div class="flex gap-2">
        <Button :label="t('admin.addVacation')" icon="pi pi-plus" severity="secondary" size="small"
                @click="addVacation" />
        <Button :label="t('common.save')" icon="pi pi-check" size="small" :loading="savingVacations"
                @click="saveVacationsConfig" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-section {
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--mw-border-light);
}

.settings-section label {
  margin-bottom: 0.125rem;
}

.settings-section small {
  display: block;
  line-height: 1.4;
}
</style>
