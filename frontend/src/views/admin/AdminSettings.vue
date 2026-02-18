<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import Select from 'primevue/select'
import ToggleSwitch from 'primevue/toggleswitch'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const adminStore = useAdminStore()
const toast = useToast()
const saving = ref(false)

const multilanguageEnabled = ref(true)
const defaultLanguage = ref('de')
const requireUserApproval = ref(true)
const requireAssignmentConfirmation = ref(true)

const languageOptions = [
  { label: 'Deutsch', value: 'de' },
  { label: 'English', value: 'en' },
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

    <Button :label="t('common.save')" icon="pi pi-check" :loading="saving" @click="saveSettings" />
  </div>
</template>

<style scoped>
.settings-section {
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--mw-border-light);
}
</style>
