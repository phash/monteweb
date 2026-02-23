<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
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
import { jobboardApi } from '@/api/jobboard.api'
import { usersApi } from '@/api/users.api'
import { sectionsApi } from '@/api/sections.api'
import { useHolidays } from '@/composables/useHolidays'
import { useAdminStore } from '@/stores/admin'
import { useRoomsStore } from '@/stores/rooms'
import { roomsApi } from '@/api/rooms.api'
import type { RoomInfo } from '@/types/room'

const { t } = useI18n()
const route = useRoute()
const cleaningStore = useCleaningStore()
const adminStore = useAdminStore()
const roomsStore = useRoomsStore()
const toast = useToast()

const currentYear = ref(new Date().getFullYear())
const { getDateClass, getDateTooltip } = useHolidays(currentYear)

const showCreateDialog = ref(false)
const showPutzOrgaDialog = ref(false)
const showAssignmentsDialog = ref(false)

const dayOptions = [
  { label: t('cleaning.days.monday'), value: 1 },
  { label: t('cleaning.days.tuesday'), value: 2 },
  { label: t('cleaning.days.wednesday'), value: 3 },
  { label: t('cleaning.days.thursday'), value: 4 },
  { label: t('cleaning.days.friday'), value: 5 }
]

const scopeType = ref<'section' | 'room'>('section')
const roomSuggestions = ref<RoomInfo[]>([])
const selectedRoom = ref<RoomInfo | null>(null)

async function searchRooms(event: { query: string }) {
  try {
    const res = await roomsApi.browse({ q: event.query, size: 20 })
    roomSuggestions.value = res.data.data.content
  } catch {
    roomSuggestions.value = []
  }
}

function onRoomSelect(event: { value: RoomInfo }) {
  selectedRoom.value = event.value
  newConfig.value.roomId = event.value.id
}

const newConfig = ref({
  sectionId: '',
  roomId: '' as string,
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

onMounted(async () => {
  if (!adminStore.config) {
    await adminStore.fetchConfig()
  }
  cleaningStore.loadConfigs()
  await loadSections()
  if (!roomsStore.myRooms.length) {
    await roomsStore.fetchMyRooms()
  }
  // Auto-open create dialog when navigated from room with roomId query param
  if (route.query.roomId) {
    scopeType.value = 'room'
    newConfig.value.roomId = route.query.roomId as string
    // Pre-select room in autocomplete
    const roomName = route.query.roomName as string
    if (roomName) {
      selectedRoom.value = { id: route.query.roomId as string, name: roomName } as RoomInfo
    }
    showCreateDialog.value = true
  }
})

async function createConfig() {
  try {
    const payload: any = {
      ...newConfig.value,
      hoursCredit: newConfig.value.hoursCredit,
    }
    if (scopeType.value === 'room' && newConfig.value.roomId) {
      // For room-scoped: use the room's sectionId
      const room = roomsStore.myRooms.find(r => r.id === newConfig.value.roomId)
      if (room?.sectionId) payload.sectionId = room.sectionId
      payload.roomId = newConfig.value.roomId
    } else {
      delete payload.roomId
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
    scopeType.value = 'section'
    selectedRoom.value = null
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

// ── Putzaktion Assignments ─────────────────────────────────────────
const assignmentsConfig = ref<CleaningConfigInfo | null>(null)
const assignments = ref<any[]>([])
const loadingAssignments = ref(false)

async function openAssignments(config: CleaningConfigInfo) {
  if (!config.jobId) return
  assignmentsConfig.value = config
  showAssignmentsDialog.value = true
  loadingAssignments.value = true
  try {
    const res = await jobboardApi.getAssignments(config.jobId)
    assignments.value = res.data.data
  } catch {
    assignments.value = []
  } finally {
    loadingAssignments.value = false
  }
}

function assignmentStatusSeverity(status: string) {
  switch (status) {
    case 'ASSIGNED': return 'info'
    case 'IN_PROGRESS': return 'warn'
    case 'COMPLETED': return 'success'
    case 'CANCELLED': return 'danger'
    default: return 'info'
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

    <!-- Configs Table (Desktop) -->
    <DataTable :value="cleaningStore.configs" :loading="cleaningStore.loading" stripedRows scrollable class="hide-mobile">
      <Column field="title" :header="t('cleaning.admin.configTitle')" />
      <Column :header="t('cleaning.admin.scope')">
        <template #body="{ data }">
          <span v-if="data.roomName"><i class="pi pi-home mr-1" />{{ data.roomName }}</span>
          <span v-else><i class="pi pi-sitemap mr-1" />{{ data.sectionName }}</span>
        </template>
      </Column>
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
      <Column :header="t('cleaning.admin.registrations')">
        <template #body="{ data }">
          <Button v-if="data.jobId" icon="pi pi-users" text rounded size="small"
                  v-tooltip="t('cleaning.admin.showRegistrations')"
                  :aria-label="t('cleaning.admin.showRegistrations')"
                  @click="openAssignments(data)" />
          <span v-else class="text-muted">-</span>
        </template>
      </Column>
      <Column :header="t('common.actions')">
        <template #body="{ data }">
          <div class="flex gap-1">
            <Button :icon="data.active ? 'pi pi-ban' : 'pi pi-check'"
                    text rounded size="small"
                    :severity="data.active ? 'danger' : 'success'"
                    :aria-label="data.active ? t('common.inactive') : t('common.active')"
                    @click="toggleActive(data)" />
          </div>
        </template>
      </Column>
    </DataTable>

    <!-- Configs Cards (Mobile) -->
    <div class="mobile-cards hide-desktop">
      <div v-if="cleaningStore.loading" class="text-center p-4"><i class="pi pi-spinner pi-spin" /></div>
      <div v-for="config in cleaningStore.configs" :key="config.id" class="mobile-card card">
        <div class="mobile-card-header">
          <strong>{{ config.title }}</strong>
          <Tag :value="config.active ? t('common.active') : t('common.inactive')"
               :severity="config.active ? 'success' : 'danger'" />
        </div>
        <div class="mobile-card-details">
          <span v-if="config.roomName"><i class="pi pi-home" /> {{ config.roomName }}</span>
          <span v-else><i class="pi pi-sitemap" /> {{ config.sectionName }}</span>
          <span><i class="pi pi-calendar" /> {{ config.specificDate ? formatSpecificDate(config.specificDate) : getDayName(config.dayOfWeek) }}</span>
          <span><i class="pi pi-clock" /> {{ config.startTime }} - {{ config.endTime }}</span>
          <span><i class="pi pi-users" /> {{ config.minParticipants }}-{{ config.maxParticipants }} | {{ config.hoursCredit }}h</span>
        </div>
        <div class="mobile-card-actions">
          <Button v-if="config.jobId" icon="pi pi-users" text size="small" :label="t('cleaning.admin.registrations')" @click="openAssignments(config)" />
          <Button :icon="config.active ? 'pi pi-ban' : 'pi pi-check'" text size="small"
                  :severity="config.active ? 'danger' : 'success'"
                  :label="config.active ? t('common.inactive') : t('common.active')"
                  @click="toggleActive(config)" />
        </div>
      </div>
    </div>

    <!-- Create Config Dialog -->
    <Dialog v-model:visible="showCreateDialog" :header="t('cleaning.admin.newConfig')" modal
            :style="{ width: '500px', maxWidth: '90vw' }">
      <div class="flex flex-col gap-4">
        <div>
          <label for="cfg-title" class="block text-sm font-medium mb-1">{{ t('cleaning.admin.configTitle') }}</label>
          <InputText id="cfg-title" v-model="newConfig.title" class="w-full" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.scope') }}</label>
          <div class="flex gap-3">
            <label class="flex items-center gap-1 cursor-pointer">
              <input type="radio" v-model="scopeType" value="section" />
              {{ t('cleaning.admin.section') }}
            </label>
            <label class="flex items-center gap-1 cursor-pointer">
              <input type="radio" v-model="scopeType" value="room" />
              {{ t('cleaning.admin.room') }}
            </label>
          </div>
        </div>
        <div v-if="scopeType === 'section'">
          <label for="cfg-section" class="block text-sm font-medium mb-1">{{ t('cleaning.admin.section') }}</label>
          <Select v-model="newConfig.sectionId" :options="sections"
                  optionLabel="name" optionValue="id"
                  :placeholder="t('cleaning.admin.selectSection')"
                  inputId="cfg-section" class="w-full" />
        </div>
        <div v-else>
          <label for="cfg-room" class="block text-sm font-medium mb-1">{{ t('cleaning.admin.room') }}</label>
          <AutoComplete v-model="selectedRoom" :suggestions="roomSuggestions"
                        @complete="searchRooms" @item-select="onRoomSelect"
                        optionLabel="name" :placeholder="t('cleaning.admin.searchRoom')"
                        inputId="cfg-room" class="w-full" :minLength="1" forceSelection />
        </div>
        <!-- Specific Date (for one-time actions) -->
        <div>
          <label for="cfg-date" class="block text-sm font-medium mb-1">{{ t('cleaning.admin.specificDate') }}</label>
          <DatePicker v-model="newConfig.specificDate" dateFormat="dd.mm.yy" class="w-full"
                      inputId="cfg-date" showIcon :showOnFocus="false">
            <template #date="{ date }">
              <span :class="getDateClass(date)" v-tooltip="getDateTooltip(date)">{{ date.day }}</span>
            </template>
          </DatePicker>
          <small class="text-gray-500">{{ t('cleaning.admin.specificDateHint') }}</small>
        </div>
        <!-- Day of week (only when no specific date) -->
        <div v-if="!newConfig.specificDate">
          <label for="cfg-day" class="block text-sm font-medium mb-1">{{ t('cleaning.admin.orRecurring') }}</label>
          <Select v-model="newConfig.dayOfWeek" :options="dayOptions"
                  optionLabel="label" optionValue="value" inputId="cfg-day" class="w-full" />
        </div>
        <div class="time-grid">
          <div>
            <label for="cfg-start" class="block text-sm font-medium mb-1">{{ t('cleaning.admin.startTime') }}</label>
            <InputText id="cfg-start" v-model="newConfig.startTime" class="w-full" />
          </div>
          <div>
            <label for="cfg-end" class="block text-sm font-medium mb-1">{{ t('cleaning.admin.endTime') }}</label>
            <InputText id="cfg-end" v-model="newConfig.endTime" class="w-full" />
          </div>
        </div>
        <div class="time-grid">
          <div>
            <label for="cfg-min" class="block text-sm font-medium mb-1">{{ t('cleaning.admin.minParticipants') }}</label>
            <InputNumber v-model="newConfig.minParticipants" :min="1" inputId="cfg-min" class="w-full" />
          </div>
          <div>
            <label for="cfg-max" class="block text-sm font-medium mb-1">{{ t('cleaning.admin.maxParticipants') }}</label>
            <InputNumber v-model="newConfig.maxParticipants" :min="1" inputId="cfg-max" class="w-full" />
          </div>
        </div>
        <div>
          <label for="cfg-hours" class="block text-sm font-medium mb-1">{{ t('cleaning.admin.hoursCredit') }}</label>
          <InputNumber v-model="newConfig.hoursCredit" :minFractionDigits="1" :maxFractionDigits="2"
                       :min="0.5" :step="0.5" inputId="cfg-hours" class="w-full" />
        </div>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" text @click="showCreateDialog = false" />
        <Button :label="t('common.create')" icon="pi pi-check" @click="createConfig"
                :disabled="!newConfig.title || (scopeType === 'section' ? !newConfig.sectionId : !newConfig.roomId)" />
      </template>
    </Dialog>

    <!-- Assignments Dialog (Issue #19) -->
    <Dialog v-model:visible="showAssignmentsDialog"
            :header="t('cleaning.admin.registrationsTitle', { title: assignmentsConfig?.title })" modal
            :style="{ width: '650px', maxWidth: '95vw' }">
      <DataTable :value="assignments" :loading="loadingAssignments" stripedRows>
        <template #empty>{{ t('cleaning.admin.noRegistrations') }}</template>
        <Column field="userName" :header="t('common.name')" />
        <Column field="familyName" :header="t('cleaning.admin.family')" />
        <Column :header="t('cleaning.admin.assignmentStatus')">
          <template #body="{ data }">
            <Tag :value="t(`jobboard.assignmentStatuses.${data.status}`)"
                 :severity="assignmentStatusSeverity(data.status)" size="small" />
          </template>
        </Column>
        <Column :header="t('cleaning.admin.hoursCol')">
          <template #body="{ data }">
            {{ data.actualHours != null ? data.actualHours + 'h' : '-' }}
          </template>
        </Column>
        <Column :header="t('jobboard.confirmed')">
          <template #body="{ data }">
            <i v-if="data.confirmed" class="pi pi-check" style="color: var(--p-green-500)" />
            <i v-else class="pi pi-times" style="color: var(--p-gray-400)" />
          </template>
        </Column>
      </DataTable>
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

        <DataTable :value="putzOrgaUsers" :loading="loadingPutzOrga" stripedRows scrollable>
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
.time-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

@media (max-width: 767px) {
  .time-grid {
    grid-template-columns: 1fr;
  }
  .flex.justify-between {
    flex-direction: column;
    gap: 0.75rem;
  }
  .flex.gap-2 {
    flex-direction: column;
  }
}

.mobile-cards {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.mobile-card {
  padding: 1rem;
}

.mobile-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.mobile-card-header strong {
  font-size: var(--mw-font-size-md);
}

.mobile-card-details {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  margin-bottom: 0.75rem;
}

.mobile-card-details i {
  width: 1.25rem;
  text-align: center;
  margin-right: 0.375rem;
}

.mobile-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  border-top: 1px solid var(--mw-border-light);
  padding-top: 0.75rem;
}

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
