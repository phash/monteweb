<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { adminApi } from '@/api/admin.api'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import FileUpload from 'primevue/fileupload'
import Select from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const adminStore = useAdminStore()
const toast = useToast()

const theme = ref<Record<string, string>>({
  primaryColor: '#3B82F6',
  primaryHover: '#2563EB',
  bgMain: '#F9FAFB',
  bgCard: '#FFFFFF',
  bgSidebar: '#FFFFFF',
  textColor: '#111827',
  textSecondary: '#6B7280',
  borderLight: '#E5E7EB',
})

const schoolName = ref('')
const saving = ref(false)
const savingHours = ref(false)
const savingVacations = ref(false)
const targetHoursPerFamily = ref(30)
const targetCleaningHours = ref(3)
const bundesland = ref('BY')
const schoolVacations = ref<{ name: string; from: string; to: string }[]>([])

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

const themeFields = [
  { key: 'primaryColor', labelKey: 'admin.theme.primaryColor' },
  { key: 'primaryHover', labelKey: 'admin.theme.primaryHover' },
  { key: 'bgMain', labelKey: 'admin.theme.background' },
  { key: 'bgCard', labelKey: 'admin.theme.cardBg' },
  { key: 'bgSidebar', labelKey: 'admin.theme.sidebarBg' },
  { key: 'textColor', labelKey: 'admin.theme.textColor' },
  { key: 'textSecondary', labelKey: 'admin.theme.secondaryText' },
  { key: 'borderLight', labelKey: 'admin.theme.borderColor' },
]

onMounted(async () => {
  if (!adminStore.config) {
    await adminStore.fetchConfig()
  }
  if (adminStore.config) {
    schoolName.value = adminStore.config.schoolName || ''
    targetHoursPerFamily.value = adminStore.config.targetHoursPerFamily ?? 30
    targetCleaningHours.value = adminStore.config.targetCleaningHours ?? 3
    bundesland.value = adminStore.config.bundesland || 'BY'
    schoolVacations.value = (adminStore.config.schoolVacations || []).map(v => ({ ...v }))
    if (adminStore.config.theme) {
      theme.value = { ...theme.value, ...(adminStore.config.theme as Record<string, string>) }
    }
  }
})

async function saveTheme() {
  saving.value = true
  try {
    await adminStore.updateTheme(theme.value)
    toast.add({ severity: 'success', summary: t('admin.themeSaved'), life: 3000 })
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

async function uploadLogo(event: { files: File[] }) {
  const file = event.files[0]
  if (!file) return
  try {
    const res = await adminApi.uploadLogo(file)
    if (adminStore.config) {
      adminStore.config.logoUrl = res.data.data.logoUrl
    }
    toast.add({ severity: 'success', summary: t('admin.logoUploaded'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

</script>

<template>
  <div class="p-4">
    <h1 class="text-2xl font-bold mb-6">{{ t('admin.themeTitle') }}</h1>

    <!-- Logo Upload -->
    <div class="mb-6">
      <h2 class="text-lg font-semibold mb-3">{{ t('admin.logo') }}</h2>
      <div class="flex items-center gap-4">
        <img v-if="adminStore.config?.logoUrl"
             :src="adminStore.config.logoUrl"
             alt="Logo" class="h-16 w-16 object-contain border rounded" />
        <div v-else class="h-16 w-16 border rounded flex items-center justify-center text-gray-400">
          <i class="pi pi-image text-2xl"></i>
        </div>
        <FileUpload mode="basic" accept="image/*" :maxFileSize="2097152"
                    :auto="true" :chooseLabel="t('admin.theme.uploadLogo')"
                    @select="uploadLogo" />
      </div>
    </div>

    <!-- Hours Configuration -->
    <div class="mb-6">
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
    <div class="mb-6">
      <h2 class="text-lg font-semibold mb-3">{{ t('admin.holidaysAndVacations') }}</h2>
      <div class="mb-4">
        <label class="block text-sm font-medium mb-1">{{ t('admin.bundesland') }}</label>
        <Select v-model="bundesland" :options="bundeslandOptions" optionLabel="label" optionValue="value"
                class="w-full md:w-1/2" />
        <small class="text-gray-500">{{ t('admin.bundeslandHint') }}</small>
      </div>

      <h3 class="text-md font-medium mb-2">{{ t('admin.schoolVacations') }}</h3>
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

    <!-- Color Scheme -->
    <h2 class="text-lg font-semibold mb-3">{{ t('admin.colorScheme') }}</h2>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
      <div v-for="field in themeFields" :key="field.key" class="flex items-center gap-3">
        <div class="w-8 h-8 rounded border"
             :style="{ backgroundColor: theme[field.key] }"></div>
        <div class="flex-1">
          <label class="block text-sm font-medium">{{ t(field.labelKey) }}</label>
          <InputText v-model="theme[field.key]" class="w-full text-sm" />
        </div>
      </div>
    </div>

    <!-- Preview -->
    <h2 class="text-lg font-semibold mb-3">{{ t('admin.preview') }}</h2>
    <div class="border rounded-lg p-4 mb-6"
         :style="{
           backgroundColor: theme.bgCard,
           color: theme.textColor,
           borderColor: theme.borderLight
         }">
      <div class="flex items-center gap-3 mb-3">
        <div class="w-8 h-8 rounded"
             :style="{ backgroundColor: theme.primaryColor }"></div>
        <span class="font-semibold">{{ schoolName || t('admin.theme.schoolNameFallback') }}</span>
      </div>
      <p :style="{ color: theme.textSecondary }">
        {{ t('admin.theme.previewText') }}
      </p>
      <div class="mt-3 flex gap-2">
        <button class="px-4 py-2 rounded text-white text-sm"
                :style="{ backgroundColor: theme.primaryColor }">
          {{ t('admin.theme.primaryButton') }}
        </button>
        <button class="px-4 py-2 rounded text-sm border"
                :style="{ borderColor: theme.borderLight, color: theme.textSecondary }">
          {{ t('admin.theme.secondaryButton') }}
        </button>
      </div>
    </div>

    <Button :label="t('common.save')" icon="pi pi-check" :loading="saving"
            @click="saveTheme" />
  </div>
</template>

<style scoped>
small {
  display: block;
  margin-top: 0.25rem;
  line-height: 1.4;
}
</style>
