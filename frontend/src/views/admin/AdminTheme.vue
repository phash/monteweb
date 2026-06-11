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

// Defaults mirror variables.css. Every key is bridged to a --mw-* token in
// useTheme.ts (and PrimeVue is bridged to those tokens in main.ts), so each of
// these recolours its element type across the whole UI.
const THEME_DEFAULTS: Record<string, string> = {
  // Primary / accent
  primaryColor: '#F5B400',
  primaryHover: '#DBA300',
  primaryDark: '#B88600',
  primaryContrast: '#111111',
  secondaryColor: '#FF8F00',
  linkColor: '#946A00',
  focusRing: '#1A1A1A',
  // Backgrounds
  bgMain: '#FFFFFF',
  bgCard: '#FFFFFF',
  bgSidebar: '#FFFBEA',
  bgActive: '#FEF3C7',
  bgHighlight: '#FEF6D6',
  // Text
  textColor: '#111111',
  textSecondary: '#333333',
  textMuted: '#595959',
  // Borders
  borderColor: '#DEE2E6',
  borderLight: '#EFE9D6',
  // Status
  successColor: '#2E7D32',
  warningColor: '#FF8F00',
  dangerColor: '#C62828',
  infoColor: '#1565C0',
}

const theme = ref<Record<string, string>>({ ...THEME_DEFAULTS })

const schoolName = ref('')
const schoolFullName = ref('')
const schoolAddress = ref('')
const schoolPrincipal = ref('')
const techContactName = ref('')
const techContactEmail = ref('')
const saving = ref(false)
const savingSchoolInfo = ref(false)

const themeGroups = [
  { id: 'primary', labelKey: 'admin.theme.groupPrimary' },
  { id: 'background', labelKey: 'admin.theme.groupBackground' },
  { id: 'text', labelKey: 'admin.theme.groupText' },
  { id: 'border', labelKey: 'admin.theme.groupBorder' },
  { id: 'status', labelKey: 'admin.theme.groupStatus' },
]

const themeFields = [
  { key: 'primaryColor', labelKey: 'admin.theme.primaryColor', group: 'primary' },
  { key: 'primaryHover', labelKey: 'admin.theme.primaryHover', group: 'primary' },
  { key: 'primaryDark', labelKey: 'admin.theme.primaryDark', group: 'primary' },
  { key: 'primaryContrast', labelKey: 'admin.theme.primaryContrast', group: 'primary' },
  { key: 'secondaryColor', labelKey: 'admin.theme.secondaryColor', group: 'primary' },
  { key: 'linkColor', labelKey: 'admin.theme.linkColor', group: 'primary' },
  { key: 'focusRing', labelKey: 'admin.theme.focusRing', group: 'primary' },
  { key: 'bgMain', labelKey: 'admin.theme.background', group: 'background' },
  { key: 'bgCard', labelKey: 'admin.theme.cardBg', group: 'background' },
  { key: 'bgSidebar', labelKey: 'admin.theme.sidebarBg', group: 'background' },
  { key: 'bgActive', labelKey: 'admin.theme.activeBg', group: 'background' },
  { key: 'bgHighlight', labelKey: 'admin.theme.highlightBg', group: 'background' },
  { key: 'textColor', labelKey: 'admin.theme.textColor', group: 'text' },
  { key: 'textSecondary', labelKey: 'admin.theme.secondaryText', group: 'text' },
  { key: 'textMuted', labelKey: 'admin.theme.mutedText', group: 'text' },
  { key: 'borderColor', labelKey: 'admin.theme.borderColorStrong', group: 'border' },
  { key: 'borderLight', labelKey: 'admin.theme.borderColor', group: 'border' },
  { key: 'successColor', labelKey: 'admin.theme.successColor', group: 'status' },
  { key: 'warningColor', labelKey: 'admin.theme.warningColor', group: 'status' },
  { key: 'dangerColor', labelKey: 'admin.theme.dangerColor', group: 'status' },
  { key: 'infoColor', labelKey: 'admin.theme.infoColor', group: 'status' },
]

function fieldsForGroup(groupId: string) {
  return themeFields.filter(f => f.group === groupId)
}

function resetTheme() {
  theme.value = { ...THEME_DEFAULTS }
}

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
    <h2 class="text-lg font-semibold mb-1">{{ t('admin.colorScheme') }}</h2>
    <p class="text-sm mb-4" style="color: var(--mw-text-muted)">{{ t('admin.theme.colorSchemeHint') }}</p>
    <div v-for="group in themeGroups" :key="group.id" class="theme-group">
      <h3 class="theme-group-title">{{ t(group.labelKey) }}</h3>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div v-for="field in fieldsForGroup(group.id)" :key="field.key" class="flex items-center gap-3">
          <input
            type="color"
            class="color-swatch"
            :value="theme[field.key]"
            :aria-label="t(field.labelKey)"
            @input="theme[field.key] = ($event.target as HTMLInputElement).value"
          />
          <div class="flex-1">
            <label class="block text-sm font-medium">{{ t(field.labelKey) }}</label>
            <InputText v-model="theme[field.key]" class="w-full text-sm" />
          </div>
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
        <button class="px-4 py-2 rounded text-sm"
                :style="{ backgroundColor: theme.primaryColor, color: theme.primaryContrast, border: '1px solid ' + theme.primaryDark }">
          {{ t('admin.theme.primaryButton') }}
        </button>
        <button class="px-4 py-2 rounded text-sm border"
                :style="{ borderColor: theme.borderLight, color: theme.textSecondary }">
          {{ t('admin.theme.secondaryButton') }}
        </button>
      </div>
    </div>

    <div class="flex gap-2">
      <Button :label="t('common.save')" icon="pi pi-check" :loading="saving"
              @click="saveTheme" />
      <Button :label="t('admin.theme.reset')" icon="pi pi-replay" severity="secondary"
              outlined @click="resetTheme" />
    </div>
  </div>
</template>

<style scoped>
.settings-section {
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--mw-border-light);
}

.theme-group {
  margin-bottom: 1.5rem;
}

.theme-group-title {
  font-size: var(--mw-font-size-sm);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--mw-text-muted);
  margin-bottom: 0.75rem;
  padding-bottom: 0.35rem;
  border-bottom: 1px solid var(--mw-border-light);
}

.color-swatch {
  width: 2.25rem;
  height: 2.25rem;
  min-width: 2.25rem;
  padding: 0;
  border: 1px solid var(--mw-border);
  border-radius: var(--mw-border-radius);
  background: none;
  cursor: pointer;
}
.color-swatch::-webkit-color-swatch-wrapper { padding: 2px; }
.color-swatch::-webkit-color-swatch { border: none; border-radius: 5px; }

small {
  display: block;
  margin-top: 0.25rem;
  line-height: 1.4;
}
</style>
