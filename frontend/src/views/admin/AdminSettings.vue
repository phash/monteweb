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
import Accordion from 'primevue/accordion'
import AccordionPanel from 'primevue/accordionpanel'
import AccordionHeader from 'primevue/accordionheader'
import AccordionContent from 'primevue/accordioncontent'
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
const directoryAdminOnly = ref(false)
const requireAssignmentConfirmation = ref(true)
const twoFactorMode = ref('DISABLED')
const twoFactorGraceDeadline = ref<string | null>(null)

// Communication settings
const parentToParentMessaging = ref(false)
const studentToStudentMessaging = ref(false)

// Family settings
const soleCustodyEnabled = ref(false)
const requireFamilySwitchApproval = ref(false)

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
const openPanels = ref(['general', 'communication'])

// Impersonation (enabled via modules map)
const impersonationEnabled = ref(false)

// Maintenance mode (enabled via modules map)
const maintenanceEnabled = ref(false)
const maintenanceMessage = ref('')
const savingMaintenance = ref(false)

// ClamAV virus scanner (enabled via modules page)
const clamavHost = ref('clamav')
const clamavPort = ref(3310)
const savingClamav = ref(false)

// Jitsi video conferencing (enabled via modules page)
const jitsiServerUrl = ref('https://meet.jit.si')
const savingJitsi = ref(false)

// WOPI / ONLYOFFICE (enabled via modules page)
const wopiOfficeUrl = ref('')
const savingWopi = ref(false)

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
    directoryAdminOnly.value = adminStore.isModuleEnabled('directoryAdminOnly')
    requireAssignmentConfirmation.value = adminStore.config.requireAssignmentConfirmation ?? true
    twoFactorMode.value = adminStore.config.twoFactorMode ?? 'DISABLED'
    twoFactorGraceDeadline.value = adminStore.config.twoFactorGraceDeadline ?? null
    targetHoursPerFamily.value = adminStore.config.targetHoursPerFamily ?? 30
    targetCleaningHours.value = adminStore.config.targetCleaningHours ?? 3
    bundesland.value = adminStore.config.bundesland || 'BY'
    schoolVacations.value = (adminStore.config.schoolVacations || []).map(v => ({ ...v }))
    // Communication settings
    parentToParentMessaging.value = adminStore.config.parentToParentMessaging ?? false
    studentToStudentMessaging.value = adminStore.config.studentToStudentMessaging ?? false
    // Family settings
    soleCustodyEnabled.value = adminStore.config.soleCustodyEnabled ?? false
    requireFamilySwitchApproval.value = adminStore.config.requireFamilySwitchApproval ?? false
    // LDAP fields (enabled via modules map)
    ldapEnabled.value = adminStore.isModuleEnabled('ldap')
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
    // Impersonation (enabled via modules map)
    impersonationEnabled.value = adminStore.isModuleEnabled('impersonation')
    // Maintenance (enabled via modules map)
    maintenanceEnabled.value = adminStore.isModuleEnabled('maintenance')
    maintenanceMessage.value = adminStore.config.maintenanceMessage ?? ''
    // ClamAV
    clamavHost.value = adminStore.config.clamavHost ?? 'clamav'
    clamavPort.value = adminStore.config.clamavPort ?? 3310
    // Jitsi
    jitsiServerUrl.value = adminStore.config.jitsiServerUrl ?? 'https://meet.jit.si'
    // WOPI
    wopiOfficeUrl.value = adminStore.config.wopiOfficeUrl ?? ''
  }
})

async function saveSettings() {
  saving.value = true
  try {
    // Ensure default language is always in available languages
    const langs = availableLanguages.value.includes(defaultLanguage.value)
      ? availableLanguages.value
      : [defaultLanguage.value, ...availableLanguages.value]
    // Toggle directoryAdminOnly in modules map
    const currentModules = adminStore.config?.modules ? { ...adminStore.config.modules } : {}
    currentModules.directoryAdminOnly = directoryAdminOnly.value
    await adminApi.updateModules(currentModules)
    await adminStore.updateConfig({
      defaultLanguage: defaultLanguage.value,
      availableLanguages: langs,
      requireUserApproval: requireUserApproval.value,
      requireAssignmentConfirmation: requireAssignmentConfirmation.value,
      twoFactorMode: twoFactorMode.value,
      parentToParentMessaging: parentToParentMessaging.value,
      studentToStudentMessaging: studentToStudentMessaging.value,
      soleCustodyEnabled: soleCustodyEnabled.value,
      requireFamilySwitchApproval: requireFamilySwitchApproval.value,
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

async function saveMaintenance() {
  savingMaintenance.value = true
  try {
    const res = await adminApi.updateMaintenance(maintenanceEnabled.value, maintenanceMessage.value)
    adminStore.config = res.data.data
    toast.add({ severity: 'success', summary: t('admin.maintenance.saved'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    savingMaintenance.value = false
  }
}

async function saveImpersonationToggle(enabled: boolean) {
  try {
    const currentModules = adminStore.config?.modules ? { ...adminStore.config.modules } : {}
    currentModules.impersonation = enabled
    const res = await adminApi.updateModules(currentModules)
    adminStore.config = res.data.data
    impersonationEnabled.value = enabled
    toast.add({ severity: 'success', summary: t('admin.settings.saved'), life: 3000 })
  } catch (e: any) {
    impersonationEnabled.value = !enabled
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function saveClamavConfig() {
  savingClamav.value = true
  try {
    const res = await adminApi.updateConfig({
      clamavHost: clamavHost.value,
      clamavPort: clamavPort.value,
    })
    adminStore.config = res.data.data
    toast.add({ severity: 'success', summary: t('admin.clamav.saved'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    savingClamav.value = false
  }
}

async function saveJitsiConfig() {
  savingJitsi.value = true
  try {
    const res = await adminApi.updateConfig({
      jitsiServerUrl: jitsiServerUrl.value,
    })
    adminStore.config = res.data.data
    toast.add({ severity: 'success', summary: t('admin.jitsi.saved'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    savingJitsi.value = false
  }
}

async function saveWopiConfig() {
  savingWopi.value = true
  try {
    const res = await adminApi.updateConfig({
      wopiOfficeUrl: wopiOfficeUrl.value || undefined,
    })
    adminStore.config = res.data.data
    toast.add({ severity: 'success', summary: t('wopi.saved'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    savingWopi.value = false
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
    // Toggle LDAP in modules map
    const currentModules = adminStore.config?.modules ? { ...adminStore.config.modules } : {}
    currentModules.ldap = ldapEnabled.value
    await adminApi.updateModules(currentModules)
    const data: Record<string, any> = {
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

    <Accordion multiple :value="openPanels">

      <!-- 1. Allgemein -->
      <AccordionPanel value="general">
        <AccordionHeader>
          <span class="flex items-center gap-2">
            <i class="pi pi-cog" />
            {{ t('admin.settings.groups.general') }}
          </span>
        </AccordionHeader>
        <AccordionContent>
          <!-- Language subsection -->
          <div class="settings-subsection">
            <h3 class="subsection-title">{{ t('admin.settings.language') }}</h3>
            <div class="mb-4">
              <label class="block text-sm font-medium mb-1">{{ t('admin.settings.defaultLanguage') }}</label>
              <Select v-model="defaultLanguage" :options="languageOptions" optionLabel="label" optionValue="value" class="w-full md:w-1/3" />
            </div>
            <div class="mb-4">
              <label class="block text-sm font-medium mb-1">{{ t('admin.settings.availableLanguages') }}</label>
              <MultiSelect v-model="availableLanguages" :options="languageOptions" optionLabel="label" optionValue="value" class="w-full md:w-1/3" />
              <small class="text-gray-500">{{ t('admin.settings.availableLanguagesHint') }}</small>
            </div>
          </div>

          <!-- Registration subsection -->
          <div class="settings-subsection">
            <h3 class="subsection-title">{{ t('admin.settings.registration') }}</h3>
            <div class="mb-4 flex items-center gap-3">
              <ToggleSwitch v-model="requireUserApproval" />
              <div>
                <label class="block text-sm font-medium">{{ t('admin.settings.requireUserApproval') }}</label>
                <small class="text-gray-500">{{ t('admin.settings.requireUserApprovalHint') }}</small>
              </div>
            </div>
          </div>

          <!-- Bundesland & Vacations subsection -->
          <div class="settings-subsection">
            <h3 class="subsection-title">{{ t('admin.holidaysAndVacations') }}</h3>
            <div class="mb-4">
              <label class="block text-sm font-medium mb-1">{{ t('admin.bundesland') }}</label>
              <Select v-model="bundesland" :options="bundeslandOptions" optionLabel="label" optionValue="value" class="w-full md:w-1/2" />
              <small class="text-gray-500">{{ t('admin.bundeslandHint') }}</small>
            </div>
            <h3 class="text-md font-medium mb-2">{{ t('admin.schoolVacations') }}</h3>
            <div class="mb-3">
              <Button :label="t('admin.loadVacations')" icon="pi pi-download" severity="secondary" size="small" @click="loadVacationsForBundesland" />
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
              <Button :label="t('admin.addVacation')" icon="pi pi-plus" severity="secondary" size="small" @click="addVacation" />
              <Button :label="t('common.save')" icon="pi pi-check" size="small" :loading="savingVacations" @click="saveVacationsConfig" />
            </div>
          </div>

          <Button :label="t('common.save')" icon="pi pi-check" :loading="saving" @click="saveSettings" class="mt-2" />
        </AccordionContent>
      </AccordionPanel>

      <!-- 2. Kommunikation -->
      <AccordionPanel value="communication">
        <AccordionHeader>
          <span class="flex items-center gap-2">
            <i class="pi pi-comments" />
            {{ t('admin.settings.groups.communication') }}
          </span>
        </AccordionHeader>
        <AccordionContent>
          <!-- Directory subsection -->
          <div class="settings-subsection">
            <h3 class="subsection-title">{{ t('admin.settings.directory') }}</h3>
            <div class="mb-4 flex items-center gap-3">
              <ToggleSwitch v-model="directoryAdminOnly" />
              <div>
                <label class="block text-sm font-medium">{{ t('admin.settings.directoryAdminOnly') }}</label>
                <small class="text-gray-500">{{ t('admin.settings.directoryAdminOnlyHint') }}</small>
              </div>
            </div>
          </div>

          <!-- Communication rules subsection (conditional on messaging module) -->
          <div v-if="adminStore.isModuleEnabled('messaging')" class="settings-subsection">
            <h3 class="subsection-title">{{ t('admin.settings.communication') }}</h3>
            <div class="mb-4 flex items-center gap-3">
              <ToggleSwitch v-model="parentToParentMessaging" />
              <div>
                <label class="block text-sm font-medium">{{ t('admin.settings.parentToParentMessaging') }}</label>
                <small class="text-gray-500">{{ t('admin.settings.parentToParentMessagingHint') }}</small>
              </div>
            </div>
            <div class="mb-4 flex items-center gap-3">
              <ToggleSwitch v-model="studentToStudentMessaging" />
              <div>
                <label class="block text-sm font-medium">{{ t('admin.settings.studentToStudentMessaging') }}</label>
                <small class="text-gray-500">{{ t('admin.settings.studentToStudentMessagingHint') }}</small>
              </div>
            </div>
          </div>

          <!-- Jobboard subsection -->
          <div class="settings-subsection">
            <h3 class="subsection-title">{{ t('admin.settings.jobboard') }}</h3>
            <div class="mb-4 flex items-center gap-3">
              <ToggleSwitch v-model="requireAssignmentConfirmation" />
              <div>
                <label class="block text-sm font-medium">{{ t('admin.requireConfirmation') }}</label>
                <small class="text-gray-500">{{ t('admin.requireConfirmationHint') }}</small>
              </div>
            </div>
          </div>

          <!-- Family subsection -->
          <div class="settings-subsection">
            <h3 class="subsection-title">{{ t('admin.settings.family') }}</h3>
            <div class="mb-4 flex items-center gap-3">
              <ToggleSwitch v-model="soleCustodyEnabled" />
              <div>
                <label class="block text-sm font-medium">{{ t('admin.settings.soleCustodyEnabled') }}</label>
                <small class="text-gray-500">{{ t('admin.settings.soleCustodyEnabledHint') }}</small>
              </div>
            </div>
            <div class="mb-4 flex items-center gap-3">
              <ToggleSwitch v-model="requireFamilySwitchApproval" />
              <div>
                <label class="block text-sm font-medium">{{ t('admin.settings.requireFamilySwitchApproval') }}</label>
                <small class="text-gray-500">{{ t('admin.settings.requireFamilySwitchApprovalHint') }}</small>
              </div>
            </div>
          </div>

          <Button :label="t('common.save')" icon="pi pi-check" :loading="saving" @click="saveSettings" class="mt-2" />
        </AccordionContent>
      </AccordionPanel>

      <!-- 3. Integrationen -->
      <AccordionPanel value="integration">
        <AccordionHeader>
          <span class="flex items-center gap-2">
            <i class="pi pi-link" />
            {{ t('admin.settings.groups.integration') }}
          </span>
        </AccordionHeader>
        <AccordionContent>
          <!-- Jitsi subsection -->
          <div v-if="adminStore.isModuleEnabled('jitsi')" class="settings-subsection">
            <h3 class="subsection-title">{{ t('admin.jitsi.title') }}</h3>
            <Message severity="info" :closable="false">{{ t('admin.jitsi.hint') }}</Message>
            <div class="mb-4">
              <label class="block text-sm font-medium mb-1">{{ t('admin.jitsi.serverUrl') }}</label>
              <InputText v-model="jitsiServerUrl" class="w-full" placeholder="https://meet.jit.si" />
            </div>
            <Button :label="t('common.save')" :loading="savingJitsi" @click="saveJitsiConfig" />
          </div>

          <!-- WOPI / ONLYOFFICE subsection -->
          <div v-if="adminStore.isModuleEnabled('wopi')" class="settings-subsection">
            <h3 class="subsection-title">{{ t('wopi.title') }}</h3>
            <Message severity="info" :closable="false">{{ t('wopi.hint') }}</Message>
            <div class="mb-4">
              <label class="block text-sm font-medium mb-1">{{ t('wopi.officeUrl') }}</label>
              <InputText v-model="wopiOfficeUrl" class="w-full" placeholder="https://office.example.com" />
              <small class="text-gray-500">{{ t('wopi.officeUrlHint') }}</small>
            </div>
            <Button :label="t('common.save')" :loading="savingWopi" @click="saveWopiConfig" />
          </div>

          <!-- ClamAV subsection -->
          <div v-if="adminStore.isModuleEnabled('clamav')" class="settings-subsection">
            <h3 class="subsection-title">{{ t('admin.clamav.title') }}</h3>
            <Message severity="info" :closable="false">{{ t('admin.clamav.hint') }}</Message>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              <div>
                <label class="block text-sm font-medium mb-1">{{ t('admin.clamav.host') }}</label>
                <InputText v-model="clamavHost" class="w-full" />
              </div>
              <div>
                <label class="block text-sm font-medium mb-1">{{ t('admin.clamav.port') }}</label>
                <InputNumber v-model="clamavPort" :min="1" :max="65535" class="w-full" />
              </div>
            </div>
            <Button :label="t('common.save')" :loading="savingClamav" @click="saveClamavConfig" />
          </div>

          <!-- LDAP subsection -->
          <div class="settings-subsection">
            <div class="flex items-center gap-2 mb-3">
              <h3 class="subsection-title mb-0">{{ t('admin.settings.ldap.title') }}</h3>
              <span v-if="ldapEnabled" class="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full">{{ t('common.active') }}</span>
            </div>
            <div class="mb-4 flex items-center gap-3">
              <ToggleSwitch v-model="ldapEnabled" />
              <div>
                <label class="block text-sm font-medium">{{ t('admin.settings.ldap.enabled') }}</label>
                <small class="text-gray-500">{{ t('admin.settings.ldap.enabledHint') }}</small>
              </div>
            </div>
            <div v-if="ldapEnabled">
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
        </AccordionContent>
      </AccordionPanel>

      <!-- 4. Sicherheit -->
      <AccordionPanel value="security">
        <AccordionHeader>
          <span class="flex items-center gap-2">
            <i class="pi pi-shield" />
            {{ t('admin.settings.groups.security') }}
          </span>
        </AccordionHeader>
        <AccordionContent>
          <!-- Maintenance subsection -->
          <div class="settings-subsection">
            <h3 class="subsection-title">{{ t('admin.maintenance.title') }}</h3>
            <div class="mb-4 flex items-center gap-3">
              <ToggleSwitch v-model="maintenanceEnabled" />
              <label>{{ t('admin.maintenance.enabled') }}</label>
            </div>
            <Message v-if="maintenanceEnabled" severity="warn" :closable="false">{{ t('admin.maintenance.warning') }}</Message>
            <div v-if="maintenanceEnabled" class="mb-4">
              <label class="block text-sm font-medium mb-1">{{ t('admin.maintenance.message') }}</label>
              <InputText v-model="maintenanceMessage" class="w-full" :placeholder="t('admin.maintenance.messagePlaceholder')" />
            </div>
            <Button :label="t('common.save')" :loading="savingMaintenance" @click="saveMaintenance" />
          </div>

          <!-- 2FA subsection -->
          <div class="settings-subsection">
            <h3 class="subsection-title">{{ t('twoFactor.title') }}</h3>
            <div class="mb-4">
              <label class="block text-sm font-medium mb-1">{{ t('twoFactor.adminMode') }}</label>
              <Select v-model="twoFactorMode" :options="twoFactorModeOptions" optionLabel="label" optionValue="value" class="w-full md:w-1/3" />
              <small class="text-gray-500">{{ t('twoFactor.adminModeHint') }}</small>
            </div>
            <div v-if="twoFactorMode === 'MANDATORY' && twoFactorGraceDeadline" class="mb-4">
              <Message severity="info" :closable="false">{{ t('twoFactor.graceDeadline', { date: new Date(twoFactorGraceDeadline).toLocaleDateString() }) }}</Message>
            </div>
          </div>

          <!-- Impersonation Toggle -->
          <div class="settings-subsection danger-section">
            <h3 class="subsection-title">
              <i class="pi pi-user-edit" /> {{ t('auth.impersonation.toggle') }}
            </h3>
            <p class="danger-hint">
              <i class="pi pi-exclamation-triangle" /> {{ t('auth.impersonation.dangerWarning') }}
            </p>
            <div class="toggle-row">
              <ToggleSwitch
                v-model="impersonationEnabled"
                @update:modelValue="saveImpersonationToggle"
              />
              <span>{{ t('auth.impersonation.toggleDescription') }}</span>
            </div>
          </div>

          <Button :label="t('common.save')" icon="pi pi-check" :loading="saving" @click="saveSettings" class="mt-2" />
        </AccordionContent>
      </AccordionPanel>

      <!-- 5. Stunden -->
      <AccordionPanel value="hours">
        <AccordionHeader>
          <span class="flex items-center gap-2">
            <i class="pi pi-clock" />
            {{ t('admin.settings.groups.hours') }}
          </span>
        </AccordionHeader>
        <AccordionContent>
          <div class="settings-subsection">
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
            <Button :label="t('admin.saveHoursConfig')" icon="pi pi-check" :loading="savingHours" @click="saveHoursConfig" />
          </div>
        </AccordionContent>
      </AccordionPanel>

    </Accordion>
  </div>
</template>

<style scoped>
.settings-subsection {
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--mw-border-light);
}

.settings-subsection:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.settings-subsection label {
  margin-bottom: 0.125rem;
}

.settings-subsection small {
  display: block;
  line-height: 1.4;
}

.subsection-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
  color: var(--mw-text-secondary);
}

.danger-section {
  border: 1px solid var(--p-red-200, #fecaca);
  border-radius: var(--mw-border-radius, 8px);
  padding: 1rem;
  background: color-mix(in srgb, var(--p-red-50, #fef2f2) 50%, transparent);
}

.danger-hint {
  color: var(--p-red-600, #dc2626);
  font-size: var(--mw-font-size-sm);
  margin-bottom: 0.75rem;
}

.toggle-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
</style>
