<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { adminApi } from '@/api/admin.api'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const adminStore = useAdminStore()
const toast = useToast()
const saving = ref(false)

const privacyPolicyText = ref('')
const privacyPolicyVersion = ref('1.0')
const termsText = ref('')
const termsVersion = ref('1.0')
const retentionNotificationDays = ref<number | null>(90)
const retentionAuditDays = ref<number | null>(365)

onMounted(async () => {
  if (!adminStore.config) {
    await adminStore.fetchConfig()
  }
  if (adminStore.config) {
    privacyPolicyText.value = adminStore.config.privacyPolicyText ?? ''
    privacyPolicyVersion.value = adminStore.config.privacyPolicyVersion ?? '1.0'
    termsText.value = adminStore.config.termsText ?? ''
    termsVersion.value = adminStore.config.termsVersion ?? '1.0'
    retentionNotificationDays.value = adminStore.config.dataRetentionDaysNotifications ?? 90
    retentionAuditDays.value = adminStore.config.dataRetentionDaysAudit ?? 365
  }
})

async function savePrivacySettings() {
  saving.value = true
  try {
    const res = await adminApi.updateConfig({
      privacyPolicyText: privacyPolicyText.value,
      privacyPolicyVersion: privacyPolicyVersion.value,
      termsText: termsText.value,
      termsVersion: termsVersion.value,
      dataRetentionDaysNotifications: retentionNotificationDays.value ?? undefined,
      dataRetentionDaysAudit: retentionAuditDays.value ?? undefined,
    })
    adminStore.config = res.data.data
    toast.add({ severity: 'success', summary: t('privacy.saved'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="p-4">
    <h1 class="text-2xl font-bold mb-6">{{ t('privacy.adminPrivacy') }}</h1>

    <!-- Privacy Policy -->
    <div class="settings-section">
      <h2 class="text-lg font-semibold mb-3">{{ t('privacy.privacyPolicy') }}</h2>
      <div class="mb-4">
        <label class="block text-sm font-medium mb-1">{{ t('privacy.policyVersion') }}</label>
        <InputText v-model="privacyPolicyVersion" class="w-full md:w-1/3" />
      </div>
      <div class="mb-4">
        <label class="block text-sm font-medium mb-1">{{ t('privacy.policyText') }}</label>
        <Textarea v-model="privacyPolicyText" rows="12" class="w-full" autoResize />
        <small class="text-gray-500">{{ t('privacy.policyTextHint') }}</small>
      </div>
    </div>

    <!-- Terms of Service -->
    <div class="settings-section">
      <h2 class="text-lg font-semibold mb-3">{{ t('privacy.termsOfService') }}</h2>
      <div class="mb-4">
        <label class="block text-sm font-medium mb-1">{{ t('privacy.termsVersion') }}</label>
        <InputText v-model="termsVersion" class="w-full md:w-1/3" />
      </div>
      <div class="mb-4">
        <label class="block text-sm font-medium mb-1">{{ t('privacy.termsText') }}</label>
        <Textarea v-model="termsText" rows="12" class="w-full" autoResize />
        <small class="text-gray-500">{{ t('privacy.termsTextHint') }}</small>
      </div>
    </div>

    <!-- Data Retention -->
    <div class="settings-section">
      <h2 class="text-lg font-semibold mb-3">{{ t('privacy.retention') }}</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('privacy.retentionNotificationDays') }}</label>
          <InputNumber v-model="retentionNotificationDays" :min="1" :max="9999" class="w-full" />
          <small class="text-gray-500">{{ t('privacy.retentionNotificationDaysHint') }}</small>
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('privacy.retentionAuditDays') }}</label>
          <InputNumber v-model="retentionAuditDays" :min="1" :max="9999" class="w-full" />
          <small class="text-gray-500">{{ t('privacy.retentionAuditDaysHint') }}</small>
        </div>
      </div>
    </div>

    <Button :label="t('common.save')" icon="pi pi-check" :loading="saving" @click="savePrivacySettings" />
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
