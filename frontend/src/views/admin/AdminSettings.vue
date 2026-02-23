<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { adminApi } from '@/api/admin.api'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import MultiSelect from 'primevue/multiselect'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import ToggleSwitch from 'primevue/toggleswitch'
import Message from 'primevue/message'
import Password from 'primevue/password'
import { useToast } from 'primevue/usetoast'
import { predefinedVacations } from '@/data/schoolVacations'

const { t } = useI18n()
const adminStore = useAdminStore()
const toast = useToast()
const saving = ref(false)
const savingHours = ref(false)
const savingVacations = ref(false)
const savingLdap = ref(false)
const testingLdap = ref(false)

const defaultLanguage = ref('de')
const availableLanguages = ref<string[]>(['de', 'en'])
const requireUserApproval = ref(true)
const requireAssignmentConfirmation = ref(true)
const twoFactorMode = ref('DISABLED')
const twoFactorGraceDeadline = ref<string | null>(null)

const twoFactorModeOptions = [
  { label: t('twoFactor.modes.DISABLED'), value: 'DISABLED' },
  { label: t('twoFactor.modes.OPTIONAL'), value: 'OPTIONAL' },
  { label: t('twoFactor.modes.MANDATORY'), value: 'MANDATORY' },
]
const targetHoursPerFamily = ref(30)
const targetCleaningHours = ref(3)
const bundesland = ref('BY')
const schoolVacations = ref<{ name: string; from: string; to: string }[]>([])

// LDAP/AD fields
const ldapEnabled = ref(false)
const ldapUrl = ref('')
const ldapBaseDn = ref('')
const ldapBindDn = ref('')
const ldapBindPassword = ref('')
const ldapUserSearchFilter = ref('(uid={0})')
const ldapAttrEmail = ref('mail')
const ldapAttrFirstName = ref('givenName')
const ldapAttrLastName = ref('sn')
const ldapDefaultRole = ref('PARENT')
const ldapUseSsl = ref(false)
const ldapPasswordStored = ref(false)
const ldapExpanded = ref(false)

const languageOptions = [
  { label: 'Deutsch', value: 'de' },
  { label: 'English', value: 'en' },
]

const ldapRoleOptions = [
  { label: 'Teacher', value: 'TEACHER' },
  { label: 'Parent', value: 'PARENT' },
  { label: 'Student', value: 'STUDENT' },
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
  // Use admin config endpoint to get full config including LDAP fields
  await adminStore.fetchAdminConfig()
  if (adminStore.config) {
    defaultLanguage.value = adminStore.config.defaultLanguage ?? 'de'
    availableLanguages.value = adminStore.config.availableLanguages ?? ['de', 'en']
    requireUserApproval.value = adminStore.config.requireUserApproval ?? true
    requireAssignmentConfirmation.value = adminStore.config.requireAssignmentConfirmation ?? true
    twoFactorMode.value = adminStore.config.twoFactorMode ?? 'DISABLED'
    twoFactorGraceDeadline.value = adminStore.config.twoFactorGraceDeadline ?? null
    targetHoursPerFamily.value = adminStore.config.targetHoursPerFamily ?? 30
    targetCleaningHours.value = adminStore.config.targetCleaningHours ?? 3
    bundesland.value = adminStore.config.bundesland || 'BY'
    schoolVacations.value = (adminStore.config.schoolVacations || []).map(v => ({ ...v }))
    // LDAP fields
    ldapEnabled.value = adminStore.config.ldapEnabled ?? false
    ldapUrl.value = adminStore.config.ldapUrl ?? ''
    ldapBaseDn.value = adminStore.config.ldapBaseDn ?? ''
    ldapBindDn.value = adminStore.config.ldapBindDn ?? ''
    ldapUserSearchFilter.value = adminStore.config.ldapUserSearchFilter ?? '(uid={0})'
    ldapAttrEmail.value = adminStore.config.ldapAttrEmail ?? 'mail'
    ldapAttrFirstName.value = adminStore.config.ldapAttrFirstName ?? 'givenName'
    ldapAttrLastName.value = adminStore.config.ldapAttrLastName ?? 'sn'
    ldapDefaultRole.value = adminStore.config.ldapDefaultRole ?? 'PARENT'
    ldapUseSsl.value = adminStore.config.ldapUseSsl ?? false
    ldapPasswordStored.value = adminStore.config.ldapConfigured ?? false
    if (ldapEnabled.value) {
      ldapExpanded.value = true
    }
  }
})

async function saveSettings() {
  saving.value = true
  try {
    // Ensure default language is always in available languages
    const langs = availableLanguages.value.includes(defaultLanguage.value)
      ? availableLanguages.value
      : [defaultLanguage.value, ...availableLanguages.value]
    await adminStore.updateConfig({
      defaultLanguage: defaultLanguage.value,
      availableLanguages: langs,
      requireUserApproval: requireUserApproval.value,
      requireAssignmentConfirmation: requireAssignmentConfirmation.value,
      twoFactorMode: twoFactorMode.value,
    })
    // Update grace deadline from response
    if (adminStore.config) {
      twoFactorGraceDeadline.value = adminStore.config.twoFactorGraceDeadline ?? null
    }
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

async function saveLdapConfig() {
  savingLdap.value = true
  try {
    const data: Record<string, any> = {
      ldapEnabled: ldapEnabled.value,
      ldapUrl: ldapUrl.value || undefined,
      ldapBaseDn: ldapBaseDn.value || undefined,
      ldapBindDn: ldapBindDn.value || undefined,
      ldapUserSearchFilter: ldapUserSearchFilter.value || undefined,
      ldapAttrEmail: ldapAttrEmail.value || undefined,
      ldapAttrFirstName: ldapAttrFirstName.value || undefined,
      ldapAttrLastName: ldapAttrLastName.value || undefined,
      ldapDefaultRole: ldapDefaultRole.value || undefined,
      ldapUseSsl: ldapUseSsl.value,
    }
    // Only send password if user entered a new one
    if (ldapBindPassword.value) {
      data.ldapBindPassword = ldapBindPassword.value
    }
    const res = await adminApi.updateConfig(data)
    adminStore.config = res.data.data
    ldapBindPassword.value = ''
    ldapPasswordStored.value = !!(res.data.data.ldapConfigured)
    toast.add({ severity: 'success', summary: t('admin.settings.ldap.saved'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    savingLdap.value = false
  }
}

async function testLdapConnection() {
  testingLdap.value = true
  try {
    const res = await adminApi.testLdapConnection({
      ldapUrl: ldapUrl.value || undefined,
      ldapBaseDn: ldapBaseDn.value || undefined,
      ldapBindDn: ldapBindDn.value || undefined,
      ldapBindPassword: ldapBindPassword.value || undefined,
      ldapUseSsl: ldapUseSsl.value ? 'true' : 'false',
    })
    const result = res.data.data
    if (result.success) {
      toast.add({ severity: 'success', summary: t('admin.settings.ldap.testSuccess'), life: 3000 })
    } else {
      toast.add({ severity: 'error', summary: t('admin.settings.ldap.testFailed'), detail: result.message, life: 5000 })
    }
  } catch (e: any) {
    toast.add({ severity: 'error', summary: t('admin.settings.ldap.testFailed'), detail: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    testingLdap.value = false
  }
}
</script>

<template>
  <div class="p-4">
    <h1 class="text-2xl font-bold mb-6">{{ t('admin.settings.title') }}</h1>

    <!-- Language Section -->
    <div class="settings-section">
      <h2 class="text-lg font-semibold mb-3">{{ t('admin.settings.language') }}</h2>
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
      <div class="mb-4">
        <label class="block text-sm font-medium mb-1">{{ t('admin.settings.availableLanguages') }}</label>
        <MultiSelect
          v-model="availableLanguages"
          :options="languageOptions"
          optionLabel="label"
          optionValue="value"
          class="w-full md:w-1/3"
        />
        <small class="text-gray-500">{{ t('admin.settings.availableLanguagesHint') }}</small>
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

    <!-- Two-Factor Authentication Section -->
    <div class="settings-section">
      <h2 class="text-lg font-semibold mb-3">{{ t('twoFactor.title') }}</h2>
      <div class="mb-4">
        <label class="block text-sm font-medium mb-1">{{ t('twoFactor.adminMode') }}</label>
        <Select
          v-model="twoFactorMode"
          :options="twoFactorModeOptions"
          optionLabel="label"
          optionValue="value"
          class="w-full md:w-1/3"
        />
        <small class="text-gray-500">{{ t('twoFactor.adminModeHint') }}</small>
      </div>
      <div v-if="twoFactorMode === 'MANDATORY' && twoFactorGraceDeadline" class="mb-4">
        <Message severity="info" :closable="false">
          {{ t('twoFactor.graceDeadline', { date: new Date(twoFactorGraceDeadline).toLocaleDateString() }) }}
        </Message>
      </div>
    </div>

    <Button :label="t('common.save')" icon="pi pi-check" :loading="saving" @click="saveSettings" class="mb-6" />

    <!-- LDAP/AD Section -->
    <div class="settings-section">
      <div class="flex items-center gap-2 cursor-pointer mb-3" @click="ldapExpanded = !ldapExpanded">
        <i :class="ldapExpanded ? 'pi pi-chevron-down' : 'pi pi-chevron-right'" />
        <h2 class="text-lg font-semibold">{{ t('admin.settings.ldap.title') }}</h2>
        <span v-if="ldapEnabled" class="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full">{{ t('common.active') }}</span>
      </div>

      <div v-if="ldapExpanded">
        <div class="mb-4 flex items-center gap-3">
          <ToggleSwitch v-model="ldapEnabled" />
          <div>
            <label class="block text-sm font-medium">{{ t('admin.settings.ldap.enabled') }}</label>
            <small class="text-gray-500">{{ t('admin.settings.ldap.enabledHint') }}</small>
          </div>
        </div>

        <div v-if="ldapEnabled">
          <!-- Server Settings -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <div>
              <label class="block text-sm font-medium mb-1">{{ t('admin.settings.ldap.url') }}</label>
              <InputText v-model="ldapUrl" :placeholder="t('admin.settings.ldap.urlPlaceholder')" class="w-full" />
              <small class="text-gray-500">{{ t('admin.settings.ldap.urlHint') }}</small>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1">{{ t('admin.settings.ldap.baseDn') }}</label>
              <InputText v-model="ldapBaseDn" :placeholder="t('admin.settings.ldap.baseDnPlaceholder')" class="w-full" />
              <small class="text-gray-500">{{ t('admin.settings.ldap.baseDnHint') }}</small>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <div>
              <label class="block text-sm font-medium mb-1">{{ t('admin.settings.ldap.bindDn') }}</label>
              <InputText v-model="ldapBindDn" :placeholder="t('admin.settings.ldap.bindDnPlaceholder')" class="w-full" />
              <small class="text-gray-500">{{ t('admin.settings.ldap.bindDnHint') }}</small>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1">{{ t('admin.settings.ldap.bindPassword') }}</label>
              <Password v-model="ldapBindPassword" :placeholder="ldapPasswordStored ? t('admin.settings.ldap.passwordNotShown') : t('admin.settings.ldap.bindPasswordPlaceholder')" :feedback="false" toggleMask class="w-full" inputClass="w-full" />
              <small class="text-gray-500">{{ t('admin.settings.ldap.bindPasswordHint') }}</small>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <div>
              <label class="block text-sm font-medium mb-1">{{ t('admin.settings.ldap.userSearchFilter') }}</label>
              <InputText v-model="ldapUserSearchFilter" class="w-full" />
              <small class="text-gray-500">{{ t('admin.settings.ldap.userSearchFilterHint') }}</small>
            </div>
            <div class="flex items-center gap-3 pt-5">
              <ToggleSwitch v-model="ldapUseSsl" />
              <div>
                <label class="block text-sm font-medium">{{ t('admin.settings.ldap.useSsl') }}</label>
                <small class="text-gray-500">{{ t('admin.settings.ldap.useSslHint') }}</small>
              </div>
            </div>
          </div>

          <!-- Attribute Mapping -->
          <h3 class="text-md font-medium mb-2">{{ t('admin.settings.ldap.attrMapping') }}</h3>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            <div>
              <label class="block text-sm font-medium mb-1">{{ t('admin.settings.ldap.attrEmail') }}</label>
              <InputText v-model="ldapAttrEmail" placeholder="mail" class="w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1">{{ t('admin.settings.ldap.attrFirstName') }}</label>
              <InputText v-model="ldapAttrFirstName" placeholder="givenName" class="w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1">{{ t('admin.settings.ldap.attrLastName') }}</label>
              <InputText v-model="ldapAttrLastName" placeholder="sn" class="w-full" />
            </div>
          </div>

          <div class="mb-4">
            <label class="block text-sm font-medium mb-1">{{ t('admin.settings.ldap.defaultRole') }}</label>
            <Select v-model="ldapDefaultRole" :options="ldapRoleOptions" optionLabel="label" optionValue="value" class="w-full md:w-1/3" />
            <small class="text-gray-500">{{ t('admin.settings.ldap.defaultRoleHint') }}</small>
          </div>

          <div class="flex gap-2">
            <Button :label="t('admin.settings.ldap.testConnection')" icon="pi pi-bolt" severity="secondary" :loading="testingLdap" @click="testLdapConnection" />
            <Button :label="t('common.save')" icon="pi pi-check" :loading="savingLdap" @click="saveLdapConfig" />
          </div>
        </div>
      </div>
    </div>

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
            <Button icon="pi pi-trash" severity="danger" text rounded size="small" :aria-label="t('common.delete')" @click="removeVacation(index)" />
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
