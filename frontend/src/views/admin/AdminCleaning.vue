<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useCleaningStore } from '@/stores/cleaning'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import Tag from 'primevue/tag'
import AutoComplete from 'primevue/autocomplete'
import { useToast } from 'primevue/usetoast'
import type { CleaningConfigInfo } from '@/types/cleaning'
import type { UserInfo } from '@/types/user'
import type { SchoolSectionInfo } from '@/types/family'
import * as cleaningApi from '@/api/cleaning.api'
import { usersApi } from '@/api/users.api'
import { sectionsApi } from '@/api/sections.api'
import { useHolidays } from '@/composables/useHolidays'
import { useAdminStore } from '@/stores/admin'

const { t } = useI18n()
const cleaningStore = useCleaningStore()
const adminStore = useAdminStore()
const toast = useToast()

const currentYear = ref(new Date().getFullYear())
const { getDateClass, getDateTooltip } = useHolidays(currentYear)

const showCreateDialog = ref(false)
const showGenerateDialog = ref(false)
const showQrExportDialog = ref(false)
const showPutzOrgaDialog = ref(false)
const selectedConfig = ref<CleaningConfigInfo | null>(null)

const dayOptions = [
  { label: t('cleaning.days.monday'), value: 1 },
  { label: t('cleaning.days.tuesday'), value: 2 },
  { label: t('cleaning.days.wednesday'), value: 3 },
  { label: t('cleaning.days.thursday'), value: 4 },
  { label: t('cleaning.days.friday'), value: 5 }
]

const newConfig = ref({
  sectionId: '',
  title: '',
  description: '',
  specificDate: null as Date | null,
  dayOfWeek: 1,
  startTime: '14:00',
  endTime: '16:00',
  minParticipants: 3,
  maxParticipants: 6,
  hoursCredit: 2.0
})

const generateRange = ref({
  from: null as Date | null,
  to: null as Date | null
})

const qrExportRange = ref({
  from: null as Date | null,
  to: null as Date | null
})

onMounted(async () => {
  if (!adminStore.config) {
    await adminStore.fetchConfig()
  }
  cleaningStore.loadConfigs()
  await loadSections()
})

async function createConfig() {
  try {
    const payload: any = {
      ...newConfig.value,
      hoursCredit: newConfig.value.hoursCredit,
    }
    if (newConfig.value.specificDate) {
      payload.specificDate = newConfig.value.specificDate.toISOString().split('T')[0]
      payload.dayOfWeek = newConfig.value.specificDate.getDay() === 0 ? 7 : newConfig.value.specificDate.getDay()
    }
    delete payload.specificDate
    await cleaningStore.createConfig({
      ...payload,
      ...(newConfig.value.specificDate ? { specificDate: newConfig.value.specificDate.toISOString().split('T')[0] } : {}),
    })
    showCreateDialog.value = false
    newConfig.value.specificDate = null
    toast.add({ severity: 'success', summary: t('cleaning.admin.configCreated'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

function formatSpecificDate(dateStr: string | null | undefined): string {
  if (!dateStr) return ''
  const d = new Date(dateStr + 'T00:00:00')
  return d.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function openGenerate(config: CleaningConfigInfo) {
  selectedConfig.value = config
  generateRange.value = { from: null, to: null }
  showGenerateDialog.value = true
}

function openQrExport(config: CleaningConfigInfo) {
  selectedConfig.value = config
  qrExportRange.value = { from: null, to: null }
  showQrExportDialog.value = true
}

async function exportQrCodes() {
  if (!selectedConfig.value || !qrExportRange.value.from || !qrExportRange.value.to) return
  try {
    const from = qrExportRange.value.from!.toISOString().split('T')[0]
    const to = qrExportRange.value.to!.toISOString().split('T')[0]
    const res = await cleaningApi.exportQrCodesPdf(selectedConfig.value!.id, from!, to!)
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = `qr-codes-${selectedConfig.value!.title}.pdf`
    a.click()
    window.URL.revokeObjectURL(url)
    showQrExportDialog.value = false
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function generateSlots() {
  if (!selectedConfig.value || !generateRange.value.from || !generateRange.value.to) return
  try {
    const from = generateRange.value.from!.toISOString().split('T')[0]
    const to = generateRange.value.to!.toISOString().split('T')[0]
    const slots = await cleaningStore.generateSlots(selectedConfig.value!.id, from!, to!)
    showGenerateDialog.value = false
    toast.add({
      severity: 'success',
      summary: t('cleaning.admin.slotsGenerated', { n: slots.length }),
      life: 3000
    })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function toggleActive(config: CleaningConfigInfo) {
  try {
    await cleaningApi.updateConfig(config.id, { active: !config.active })
    await cleaningStore.loadConfigs()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

function getDayName(day: number) {
  return dayOptions.find(d => d.value === day)?.label || ''
}

// ── PutzOrga Management ──────────────────────────────────────────
const sections = ref<SchoolSectionInfo[]>([])
const selectedSection = ref<SchoolSectionInfo | null>(null)
const putzOrgaUsers = ref<UserInfo[]>([])
const loadingPutzOrga = ref(false)
const userSuggestions = ref<UserInfo[]>([])
const selectedUser = ref<UserInfo | null>(null)

async function loadSections() {
  try {
    const res = await sectionsApi.getAll()
    sections.value = res.data.data
  } catch { /* ignore */ }
}

async function loadPutzOrgaForSection() {
  if (!selectedSection.value) return
  loadingPutzOrga.value = true
  try {
    const res = await usersApi.findBySpecialRole(`PUTZORGA:${selectedSection.value.id}`)
    putzOrgaUsers.value = res.data.data
  } catch { /* ignore */ } finally {
    loadingPutzOrga.value = false
  }
}

async function searchParents(event: { query: string }) {
  if (!event.query || event.query.length < 2) {
    userSuggestions.value = []
    return
  }
  try {
    const res = await usersApi.search(event.query)
    userSuggestions.value = (res.data.data.content || [])
      .filter((u: UserInfo) => u.role === 'PARENT' && u.active)
  } catch {
    userSuggestions.value = []
  }
}

async function assignPutzOrga() {
  if (!selectedUser.value || !selectedSection.value) return
  try {
    await usersApi.addSpecialRole(selectedUser.value.id, `PUTZORGA:${selectedSection.value.id}`)
    toast.add({ severity: 'success', summary: t('cleaning.admin.putzOrgaAssigned'), life: 3000 })
    selectedUser.value = null
    await loadPutzOrgaForSection()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function removePutzOrga(user: UserInfo) {
  if (!selectedSection.value) return
  try {
    await usersApi.removeSpecialRole(user.id, `PUTZORGA:${selectedSection.value.id}`)
    toast.add({ severity: 'success', summary: t('cleaning.admin.putzOrgaRemoved'), life: 3000 })
    await loadPutzOrgaForSection()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

</script>

<template>
  <div class="p-4">
    <div class="flex justify-between items-center mb-4">
      <h1 class="text-2xl font-bold">{{ t('cleaning.admin.title') }}</h1>
      <div class="flex gap-2">
        <Button :label="t('cleaning.admin.putzOrgaManagement')" icon="pi pi-users" severity="secondary"
                @click="showPutzOrgaDialog = true" />
        <Button :label="t('cleaning.admin.newConfig')" icon="pi pi-plus" @click="showCreateDialog = true" />
      </div>
    </div>

    <!-- Configs Table -->
    <DataTable :value="cleaningStore.configs" :loading="cleaningStore.loading" stripedRows>
      <Column field="title" :header="t('cleaning.admin.configTitle')" />
      <Column field="sectionName" :header="t('cleaning.admin.section')" />
      <Column :header="t('cleaning.admin.day')">
        <template #body="{ data }">
          <template v-if="data.specificDate">
            {{ formatSpecificDate(data.specificDate) }}
          </template>
          <template v-else>{{ getDayName(data.dayOfWeek) }}</template>
        </template>
      </Column>
      <Column :header="t('cleaning.admin.timeRange')">
        <template #body="{ data }">{{ data.startTime }} - {{ data.endTime }}</template>
      </Column>
      <Column :header="t('cleaning.admin.participants')">
        <template #body="{ data }">{{ data.minParticipants }} - {{ data.maxParticipants }}</template>
      </Column>
      <Column field="hoursCredit" :header="t('cleaning.admin.hoursCredit')" />
      <Column :header="t('cleaning.admin.status')">
        <template #body="{ data }">
          <Tag :value="data.active ? t('common.active') : t('common.inactive')"
               :severity="data.active ? 'success' : 'danger'" />
        </template>
      </Column>
      <Column :header="t('common.actions')">
        <template #body="{ data }">
          <div class="flex gap-1">
            <Button icon="pi pi-calendar-plus" text rounded size="small"
                    v-tooltip="t('cleaning.admin.generate')"
                    @click="openGenerate(data)" :disabled="!data.active" />
            <Button icon="pi pi-file-pdf" text rounded size="small"
                    v-tooltip="t('cleaning.admin.exportQrCodes')"
                    @click="openQrExport(data)" />
            <Button :icon="data.active ? 'pi pi-ban' : 'pi pi-check'"
                    text rounded size="small"
                    :severity="data.active ? 'danger' : 'success'"
                    @click="toggleActive(data)" />
          </div>
        </template>
      </Column>
    </DataTable>

    <!-- Create Config Dialog -->
    <Dialog v-model:visible="showCreateDialog" :header="t('cleaning.admin.newConfig')" modal
            style="width: 500px">
      <div class="flex flex-col gap-4">
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.configTitle') }}</label>
          <InputText v-model="newConfig.title" class="w-full" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.section') }}</label>
          <Select v-model="newConfig.sectionId" :options="sections"
                  optionLabel="name" optionValue="id"
                  :placeholder="t('cleaning.admin.selectSection')"
                  class="w-full" />
        </div>
        <!-- Specific Date (for one-time actions) -->
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.specificDate') }}</label>
          <DatePicker v-model="newConfig.specificDate" dateFormat="dd.mm.yy" class="w-full"
                      showIcon :showOnFocus="false">
            <template #date="{ date }">
              <span :class="getDateClass(date)" v-tooltip="getDateTooltip(date)">{{ date.day }}</span>
            </template>
          </DatePicker>
          <small class="text-gray-500">{{ t('cleaning.admin.specificDateHint') }}</small>
        </div>
        <!-- Day of week (only when no specific date) -->
        <div v-if="!newConfig.specificDate">
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.orRecurring') }}</label>
          <Select v-model="newConfig.dayOfWeek" :options="dayOptions"
                  optionLabel="label" optionValue="value" class="w-full" />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.startTime') }}</label>
            <InputText v-model="newConfig.startTime" class="w-full" />
          </div>
          <div>
            <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.endTime') }}</label>
            <InputText v-model="newConfig.endTime" class="w-full" />
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.minParticipants') }}</label>
            <InputNumber v-model="newConfig.minParticipants" :min="1" class="w-full" />
          </div>
          <div>
            <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.maxParticipants') }}</label>
            <InputNumber v-model="newConfig.maxParticipants" :min="1" class="w-full" />
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.hoursCredit') }}</label>
          <InputNumber v-model="newConfig.hoursCredit" :minFractionDigits="1" :maxFractionDigits="2"
                       :min="0.5" :step="0.5" class="w-full" />
        </div>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" text @click="showCreateDialog = false" />
        <Button :label="t('common.create')" icon="pi pi-check" @click="createConfig"
                :disabled="!newConfig.title || !newConfig.sectionId" />
      </template>
    </Dialog>

    <!-- Generate Slots Dialog -->
    <Dialog v-model:visible="showGenerateDialog" :header="t('cleaning.admin.generateTitle')" modal
            style="width: 400px">
      <p class="mb-3">{{ t('cleaning.admin.generateHint', { title: selectedConfig?.title }) }}</p>
      <div class="flex flex-col gap-3">
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.fromDate') }}</label>
          <DatePicker v-model="generateRange.from" dateFormat="dd.mm.yy" class="w-full">
            <template #date="{ date }">
              <span :class="getDateClass(date)" v-tooltip="getDateTooltip(date)">{{ date.day }}</span>
            </template>
          </DatePicker>
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.toDate') }}</label>
          <DatePicker v-model="generateRange.to" dateFormat="dd.mm.yy" class="w-full">
            <template #date="{ date }">
              <span :class="getDateClass(date)" v-tooltip="getDateTooltip(date)">{{ date.day }}</span>
            </template>
          </DatePicker>
        </div>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" text @click="showGenerateDialog = false" />
        <Button :label="t('cleaning.admin.generate')" icon="pi pi-calendar-plus" @click="generateSlots"
                :disabled="!generateRange.from || !generateRange.to" />
      </template>
    </Dialog>

    <!-- QR Code PDF Export Dialog -->
    <Dialog v-model:visible="showQrExportDialog" :header="t('cleaning.admin.exportQrCodesTitle')" modal
            style="width: 400px">
      <p class="mb-3">{{ t('cleaning.admin.exportQrCodesHint', { title: selectedConfig?.title }) }}</p>
      <div class="flex flex-col gap-3">
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.fromDate') }}</label>
          <DatePicker v-model="qrExportRange.from" dateFormat="dd.mm.yy" class="w-full">
            <template #date="{ date }">
              <span :class="getDateClass(date)" v-tooltip="getDateTooltip(date)">{{ date.day }}</span>
            </template>
          </DatePicker>
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.toDate') }}</label>
          <DatePicker v-model="qrExportRange.to" dateFormat="dd.mm.yy" class="w-full">
            <template #date="{ date }">
              <span :class="getDateClass(date)" v-tooltip="getDateTooltip(date)">{{ date.day }}</span>
            </template>
          </DatePicker>
        </div>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" text @click="showQrExportDialog = false" />
        <Button :label="t('cleaning.admin.exportQrCodes')" icon="pi pi-file-pdf" @click="exportQrCodes"
                :disabled="!qrExportRange.from || !qrExportRange.to" />
      </template>
    </Dialog>

    <!-- PutzOrga Management Dialog -->
    <Dialog v-model:visible="showPutzOrgaDialog" :header="t('cleaning.admin.putzOrgaManagement')" modal
            style="width: 600px; max-width: 95vw">
      <p class="text-sm text-muted mb-3">{{ t('cleaning.admin.putzOrgaHint') }}</p>

      <div class="flex gap-3 items-end mb-4">
        <div class="flex-1">
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.section') }}</label>
          <Select
            v-model="selectedSection"
            :options="sections"
            optionLabel="name"
            :placeholder="t('cleaning.admin.selectSection')"
            class="w-full"
            @change="loadPutzOrgaForSection"
          />
        </div>
      </div>

      <template v-if="selectedSection">
        <div class="flex gap-2 items-end mb-3">
          <div class="flex-1">
            <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.assignPutzOrga') }}</label>
            <AutoComplete
              v-model="selectedUser"
              :suggestions="userSuggestions"
              optionLabel="displayName"
              :placeholder="t('cleaning.admin.searchParent')"
              @complete="searchParents"
              class="w-full"
              :minLength="2"
            />
          </div>
          <Button
            icon="pi pi-plus"
            :label="t('cleaning.admin.assign')"
            size="small"
            @click="assignPutzOrga"
            :disabled="!selectedUser"
          />
        </div>

        <DataTable :value="putzOrgaUsers" :loading="loadingPutzOrga" stripedRows>
          <template #empty>{{ t('cleaning.admin.noPutzOrga') }}</template>
          <Column field="displayName" :header="t('admin.columnName')" />
          <Column field="email" :header="t('admin.columnEmail')" />
          <Column :header="t('common.actions')">
            <template #body="{ data }">
              <Button
                icon="pi pi-trash"
                severity="danger"
                text
                rounded
                size="small"
                @click="removePutzOrga(data)"
              />
            </template>
          </Column>
        </DataTable>
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
:deep(.mw-holiday) {
  color: #dc2626;
  font-weight: 700;
  position: relative;
}
:deep(.mw-holiday)::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #dc2626;
}
:deep(.mw-vacation) {
  color: #ea580c;
  font-weight: 600;
  position: relative;
}
:deep(.mw-vacation)::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ea580c;
}
</style>
