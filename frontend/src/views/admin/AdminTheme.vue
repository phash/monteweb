<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { adminApi } from '@/api/admin.api'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
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
const schoolFullName = ref('')
const schoolAddress = ref('')
const schoolPrincipal = ref('')
const techContactName = ref('')
const techContactEmail = ref('')
const saving = ref(false)
const savingSchoolInfo = ref(false)

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
    schoolFullName.value = adminStore.config.schoolFullName || ''
    schoolAddress.value = adminStore.config.schoolAddress || ''
    schoolPrincipal.value = adminStore.config.schoolPrincipal || ''
    techContactName.value = adminStore.config.techContactName || ''
    techContactEmail.value = adminStore.config.techContactEmail || ''
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

async function saveSchoolInfo() {
  savingSchoolInfo.value = true
  try {
    const res = await adminApi.updateConfig({
      schoolFullName: schoolFullName.value,
      schoolAddress: schoolAddress.value,
      schoolPrincipal: schoolPrincipal.value,
      techContactName: techContactName.value,
      techContactEmail: techContactEmail.value,
    })
    adminStore.config = res.data.data
    toast.add({ severity: 'success', summary: t('admin.schoolInfoSaved'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    savingSchoolInfo.value = false
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

    <!-- School Info -->
    <div class="settings-section">
      <h2 class="text-lg font-semibold mb-3">{{ t('admin.schoolInfo') }}</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('admin.schoolFullName') }}</label>
          <InputText v-model="schoolFullName" class="w-full" :placeholder="t('admin.schoolFullNamePlaceholder')" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('admin.schoolPrincipal') }}</label>
          <InputText v-model="schoolPrincipal" class="w-full" :placeholder="t('admin.schoolPrincipalPlaceholder')" />
        </div>
        <div class="md:col-span-2">
          <label class="block text-sm font-medium mb-1">{{ t('admin.schoolAddress') }}</label>
          <Textarea v-model="schoolAddress" class="w-full" rows="2" :placeholder="t('admin.schoolAddressPlaceholder')" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('admin.techContactName') }}</label>
          <InputText v-model="techContactName" class="w-full" :placeholder="t('admin.techContactNamePlaceholder')" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('admin.techContactEmail') }}</label>
          <InputText v-model="techContactEmail" class="w-full" type="email" :placeholder="t('admin.techContactEmailPlaceholder')" />
        </div>
      </div>
      <small class="text-gray-500 mb-3 block">{{ t('admin.schoolInfoHint') }}</small>
      <Button :label="t('common.save')" icon="pi pi-check" :loading="savingSchoolInfo" @click="saveSchoolInfo" />
    </div>

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
.settings-section {
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--mw-border-light);
}

small {
  display: block;
  margin-top: 0.25rem;
  line-height: 1.4;
}
</style>
