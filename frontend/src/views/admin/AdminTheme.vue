<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { adminApi } from '@/api/admin.api'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import FileUpload from 'primevue/fileupload'
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
const targetHoursPerFamily = ref(30)
const targetCleaningHours = ref(3)

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
