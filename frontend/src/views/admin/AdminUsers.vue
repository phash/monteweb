<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { usersApi } from '@/api/users.api'
import { roomsApi } from '@/api/rooms.api'
import { familyApi } from '@/api/family.api'
import type { UserInfo, UserRole } from '@/types/user'
import type { RoomInfo, RoomRole } from '@/types/room'
import type { FamilyInfo } from '@/types/family'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import Select from 'primevue/select'
import SelectButton from 'primevue/selectbutton'
import ToggleSwitch from 'primevue/toggleswitch'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import AutoComplete from 'primevue/autocomplete'
import Checkbox from 'primevue/checkbox'

const { t } = useI18n()
const toast = useToast()

const users = ref<UserInfo[]>([])
const totalRecords = ref(0)
const loading = ref(false)
const page = ref(0)
const rows = ref(20)

// Filters
const filterRole = ref<string | null>(null)
const filterStatus = ref<string>('all')
const searchQuery = ref('')
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null

// Edit dialog
const showEdit = ref(false)
const editUser = ref<UserInfo | null>(null)
const editTab = ref('0')
const editLoading = ref(false)

// Profile form
const profileForm = ref({ email: '', firstName: '', lastName: '', phone: '' })
const editActive = ref(true)
const editAssignedRoles = ref<string[]>([])

const assignableRoleOptions = [
  { label: 'Teacher', value: 'TEACHER' },
  { label: 'Parent', value: 'PARENT' },
  { label: 'Section Admin', value: 'SECTION_ADMIN' },
]

const isFixedRoleUser = computed(() =>
  editUser.value?.role === 'SUPERADMIN' || editUser.value?.role === 'STUDENT'
)

// Rooms tab
const userRooms = ref<RoomInfo[]>([])
const roomsLoading = ref(false)
const roomSearchResults = ref<RoomInfo[]>([])
const selectedRoom = ref<RoomInfo | null>(null)
const addRoomRole = ref<RoomRole>('MEMBER')

// Family tab
const userFamilies = ref<FamilyInfo[]>([])
const allFamilies = ref<FamilyInfo[]>([])
const familiesLoading = ref(false)
const addFamilyId = ref('')

const roleOptions: { label: string; value: UserRole }[] = [
  { label: 'Superadmin', value: 'SUPERADMIN' },
  { label: 'Section Admin', value: 'SECTION_ADMIN' },
  { label: 'Teacher', value: 'TEACHER' },
  { label: 'Parent', value: 'PARENT' },
  { label: 'Student', value: 'STUDENT' },
]

const filterRoleOptions = computed(() => [
  { label: t('admin.allRoles'), value: null },
  ...roleOptions,
])

const statusOptions = computed(() => [
  { label: t('admin.allStatuses'), value: 'all' },
  { label: t('common.active'), value: 'active' },
  { label: t('common.inactive'), value: 'inactive' },
])

const roomRoleOptions = computed(() => [
  { label: t('rooms.roles.LEADER'), value: 'LEADER' as RoomRole },
  { label: t('rooms.roles.MEMBER'), value: 'MEMBER' as RoomRole },
  { label: t('rooms.roles.PARENT_MEMBER'), value: 'PARENT_MEMBER' as RoomRole },
  { label: t('rooms.roles.GUEST'), value: 'GUEST' as RoomRole },
])

async function loadUsers() {
  loading.value = true
  try {
    const params: { page: number; size: number; role?: string; active?: boolean; search?: string } = {
      page: page.value,
      size: rows.value,
    }
    if (filterRole.value) {
      params.role = filterRole.value
    }
    if (filterStatus.value === 'active') {
      params.active = true
    } else if (filterStatus.value === 'inactive') {
      params.active = false
    }
    if (searchQuery.value.trim()) {
      params.search = searchQuery.value.trim()
    }
    const res = await usersApi.list(params)
    users.value = res.data.data.content
    totalRecords.value = res.data.data.totalElements
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  page.value = 0
  loadUsers()
}

function onSearchInput() {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
  searchDebounceTimer = setTimeout(() => {
    page.value = 0
    loadUsers()
  }, 300)
}

function onPage(event: { page: number; rows: number }) {
  page.value = event.page
  rows.value = event.rows
  loadUsers()
}

function roleSeverity(role: string): string {
  const map: Record<string, string> = {
    SUPERADMIN: 'danger',
    SECTION_ADMIN: 'warn',
    TEACHER: 'info',
    PARENT: 'success',
    STUDENT: 'secondary',
  }
  return map[role] ?? 'secondary'
}

function openEdit(user: UserInfo) {
  editUser.value = user
  editTab.value = '0'
  profileForm.value = {
    email: user.email,
    firstName: user.firstName,
    lastName: user.lastName,
    phone: user.phone || '',
  }
  editActive.value = user.active
  editAssignedRoles.value = [...(user.assignedRoles || [])]
  userRooms.value = []
  userFamilies.value = []
  selectedRoom.value = null
  showEdit.value = true
}

async function saveProfile() {
  if (!editUser.value) return
  editLoading.value = true
  try {
    await usersApi.adminUpdateProfile(editUser.value.id, profileForm.value)
    if (editActive.value !== editUser.value.active) {
      await usersApi.setActive(editUser.value.id, editActive.value)
    }
    // Save assigned roles (only for non-fixed-role users)
    if (!isFixedRoleUser.value) {
      const currentAssigned = editUser.value.assignedRoles || []
      const newAssigned = editAssignedRoles.value
      if (JSON.stringify([...currentAssigned].sort()) !== JSON.stringify([...newAssigned].sort())) {
        await usersApi.updateAssignedRoles(editUser.value.id, newAssigned)
      }
    }
    toast.add({ severity: 'success', summary: t('admin.userSaved'), life: 3000 })
    showEdit.value = false
    await loadUsers()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  } finally {
    editLoading.value = false
  }
}

async function loadUserRooms() {
  if (!editUser.value) return
  roomsLoading.value = true
  try {
    const res = await usersApi.getUserRooms(editUser.value.id)
    userRooms.value = res.data.data
  } finally {
    roomsLoading.value = false
  }
}

async function addMemberToRoom() {
  if (!editUser.value || !selectedRoom.value) return
  try {
    await roomsApi.addMember(selectedRoom.value.id, editUser.value.id, addRoomRole.value)
    toast.add({ severity: 'success', summary: t('admin.memberAdded'), life: 3000 })
    selectedRoom.value = null
    await loadUserRooms()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  }
}

async function removeFromRoom(roomId: string) {
  if (!editUser.value) return
  try {
    await roomsApi.removeMember(roomId, editUser.value.id)
    toast.add({ severity: 'success', summary: t('admin.memberRemoved'), life: 3000 })
    await loadUserRooms()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  }
}

async function loadUserFamilies() {
  if (!editUser.value) return
  familiesLoading.value = true
  try {
    const [famRes, allFamRes] = await Promise.all([
      usersApi.getUserFamilies(editUser.value.id),
      familyApi.getAll(),
    ])
    userFamilies.value = famRes.data.data
    allFamilies.value = allFamRes.data.data
  } finally {
    familiesLoading.value = false
  }
}

async function addToFamily() {
  if (!editUser.value || !addFamilyId.value) return
  const autoRole = editUser.value.role === 'STUDENT' ? 'CHILD' : 'PARENT'
  try {
    await usersApi.addUserToFamily(editUser.value.id, addFamilyId.value, autoRole)
    toast.add({ severity: 'success', summary: t('admin.familyMemberAdded'), life: 3000 })
    addFamilyId.value = ''
    await loadUserFamilies()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  }
}

async function removeFromFamily(familyId: string) {
  if (!editUser.value) return
  try {
    await usersApi.removeUserFromFamily(editUser.value.id, familyId)
    toast.add({ severity: 'success', summary: t('admin.familyMemberRemoved'), life: 3000 })
    await loadUserFamilies()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  }
}

function onTabChange(val: string | number) {
  const tab = String(val)
  if (tab === '1' && userRooms.value.length === 0) {
    loadUserRooms()
  } else if (tab === '2' && userFamilies.value.length === 0) {
    loadUserFamilies()
  }
}

// Room search (for adding user to room from rooms tab)
const allRooms = ref<RoomInfo[]>([])
const allRoomsLoaded = ref(false)

async function searchRooms(event: { query: string }) {
  if (!allRoomsLoaded.value) {
    const res = await roomsApi.getAll({ page: 0, size: 200 })
    allRooms.value = res.data.data.content
    allRoomsLoaded.value = true
  }
  const q = event.query.toLowerCase()
  roomSearchResults.value = allRooms.value
    .filter((r: RoomInfo) => r.name.toLowerCase().includes(q))
    .filter((r: RoomInfo) => !userRooms.value.some(ur => ur.id === r.id))
}

onMounted(loadUsers)

onUnmounted(() => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
})
</script>

<template>
  <div>
    <PageTitle :title="t('admin.users')" />

    <div class="filter-bar">
      <Select
        v-model="filterRole"
        :options="filterRoleOptions"
        optionLabel="label"
        optionValue="value"
        :placeholder="t('admin.filterByRole')"
        class="filter-role"
        @change="onFilterChange"
      />
      <SelectButton
        v-model="filterStatus"
        :options="statusOptions"
        optionLabel="label"
        optionValue="value"
        @change="onFilterChange"
      />
      <IconField class="filter-search">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model="searchQuery"
          :placeholder="t('admin.searchUsers')"
          class="w-full"
          @input="onSearchInput"
        />
      </IconField>
    </div>

    <LoadingSpinner v-if="loading && users.length === 0" />

    <DataTable
      v-else
      :value="users"
      :loading="loading"
      :paginator="true"
      :rows="rows"
      :totalRecords="totalRecords"
      :lazy="true"
      @page="onPage"
      stripedRows
      scrollable
      class="card"
    >
      <template #empty>
        <div class="empty-table">{{ t('admin.noUsersFound') }}</div>
      </template>
      <Column field="displayName" :header="t('common.name')" />
      <Column field="email" :header="t('auth.email')" />
      <Column field="role" :header="t('admin.columnRole')">
        <template #body="{ data }">
          <Tag :value="data.role" :severity="roleSeverity(data.role) as any" />
        </template>
      </Column>
      <Column field="active" :header="t('common.status')">
        <template #body="{ data }">
          <Tag :value="data.active ? t('common.active') : t('common.inactive')" :severity="data.active ? 'success' : 'danger'" />
        </template>
      </Column>
      <Column :header="t('common.actions')" style="width: 100px">
        <template #body="{ data }">
          <Button icon="pi pi-pencil" severity="secondary" text size="small" @click="openEdit(data)" :aria-label="t('common.edit')" />
        </template>
      </Column>
    </DataTable>

    <!-- Edit Dialog -->
    <Dialog
      v-model:visible="showEdit"
      :header="t('admin.editUser')"
      modal
      :style="{ width: '650px', maxWidth: '95vw' }"
    >
      <Tabs :value="editTab" @update:value="onTabChange">
        <TabList>
          <Tab value="0">{{ t('admin.tabProfile') }}</Tab>
          <Tab value="1">{{ t('admin.tabRooms') }}</Tab>
          <Tab value="2">{{ t('admin.tabFamily') }}</Tab>
        </TabList>
        <TabPanels>
          <!-- Profile Tab -->
          <TabPanel value="0">
            <form @submit.prevent="saveProfile" class="dialog-form">
              <div class="form-field">
                <label>{{ t('auth.email') }}</label>
                <InputText v-model="profileForm.email" type="email" class="w-full" />
              </div>
              <div class="form-row">
                <div class="form-field">
                  <label>{{ t('auth.firstName') }}</label>
                  <InputText v-model="profileForm.firstName" class="w-full" />
                </div>
                <div class="form-field">
                  <label>{{ t('auth.lastName') }}</label>
                  <InputText v-model="profileForm.lastName" class="w-full" />
                </div>
              </div>
              <div class="form-field">
                <label>{{ t('auth.phone') }}</label>
                <InputText v-model="profileForm.phone" class="w-full" />
              </div>
              <div class="form-field toggle-field">
                <label>{{ t('common.active') }}</label>
                <ToggleSwitch v-model="editActive" />
              </div>
              <div v-if="!isFixedRoleUser" class="form-field">
                <label>{{ t('admin.assignedRoles') }}</label>
                <small class="assigned-roles-hint">{{ t('admin.assignedRolesHint') }}</small>
                <div class="assigned-roles-checkboxes">
                  <div v-for="opt in assignableRoleOptions" :key="opt.value" class="assigned-role-item">
                    <Checkbox
                      v-model="editAssignedRoles"
                      :inputId="'ar-' + opt.value"
                      :value="opt.value"
                    />
                    <label :for="'ar-' + opt.value">{{ opt.label }}</label>
                  </div>
                </div>
              </div>
              <div class="form-actions">
                <Button :label="t('common.save')" type="submit" :loading="editLoading" />
              </div>
            </form>
          </TabPanel>

          <!-- Rooms Tab -->
          <TabPanel value="1">
            <div v-if="roomsLoading" class="tab-loading">
              <i class="pi pi-spin pi-spinner" />
            </div>
            <template v-else>
              <div class="add-row">
                <AutoComplete
                  v-model="selectedRoom"
                  :suggestions="roomSearchResults"
                  optionLabel="name"
                  :placeholder="t('admin.searchRoom')"
                  @complete="searchRooms"
                  class="room-autocomplete"
                />
                <Select v-model="addRoomRole" :options="roomRoleOptions" optionLabel="label" optionValue="value" style="width: 160px" />
                <Button icon="pi pi-plus" :label="t('admin.addMember')" size="small" @click="addMemberToRoom" :disabled="!selectedRoom" />
              </div>
              <div v-if="userRooms.length === 0" class="empty-tab">{{ t('admin.noRoomMemberships') }}</div>
              <div v-else class="item-list">
                <div v-for="room in userRooms" :key="room.id" class="item-row">
                  <span class="item-name">{{ room.name }}</span>
                  <Tag :value="t(`rooms.types.${room.type}`)" severity="info" />
                  <Button icon="pi pi-trash" severity="danger" text size="small" @click="removeFromRoom(room.id)" :aria-label="t('common.delete')" />
                </div>
              </div>
            </template>
          </TabPanel>

          <!-- Family Tab -->
          <TabPanel value="2">
            <div v-if="familiesLoading" class="tab-loading">
              <i class="pi pi-spin pi-spinner" />
            </div>
            <template v-else>
              <div class="add-row">
                <Select
                  v-model="addFamilyId"
                  :options="allFamilies.filter(f => !userFamilies.some(uf => uf.id === f.id))"
                  optionLabel="name"
                  optionValue="id"
                  :placeholder="t('admin.selectFamily')"
                  class="flex-grow"
                  showClear
                />
                <Button icon="pi pi-plus" :label="t('admin.addToFamily')" size="small" @click="addToFamily" :disabled="!addFamilyId" />
              </div>
              <div v-if="userFamilies.length === 0" class="empty-tab">{{ t('admin.noFamilyMemberships') }}</div>
              <div v-else class="item-list">
                <div v-for="fam in userFamilies" :key="fam.id" class="item-row">
                  <span class="item-name">{{ fam.name }}</span>
                  <span class="item-members">{{ fam.members.length }} {{ t('family.members') }}</span>
                  <Button icon="pi pi-trash" severity="danger" text size="small" @click="removeFromFamily(fam.id)" :aria-label="t('common.delete')" />
                </div>
              </div>
            </template>
          </TabPanel>
        </TabPanels>
      </Tabs>
    </Dialog>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.filter-role {
  min-width: 180px;
}

.filter-search {
  flex: 1;
  min-width: 200px;
}

.empty-table {
  padding: 2rem 0;
  text-align: center;
  color: var(--p-text-muted-color);
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding-top: 0.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-field label {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
}

.form-row {
  display: flex;
  gap: 1rem;
}

.form-row .form-field {
  flex: 1;
}

.toggle-field {
  flex-direction: row;
  align-items: center;
  gap: 0.75rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 0.5rem;
}

.add-row {
  display: flex;
  gap: 0.5rem;
  align-items: flex-start;
  margin-bottom: 1rem;
  padding-top: 0.5rem;
  flex-wrap: wrap;
}

.flex-grow {
  flex: 1;
  min-width: 150px;
}

.room-autocomplete {
  flex: 2;
  min-width: 200px;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.5rem;
  border-radius: var(--p-border-radius);
}

.item-row:hover {
  background-color: var(--p-surface-100);
}

.item-name {
  font-weight: 500;
  flex: 1;
  min-width: 0;
}

.item-members {
  color: var(--p-text-muted-color);
  font-size: var(--mw-font-size-sm);
  white-space: nowrap;
}

.empty-tab {
  padding: 2rem 0;
  text-align: center;
  color: var(--p-text-muted-color);
}

.tab-loading {
  display: flex;
  justify-content: center;
  padding: 2rem 0;
  font-size: 1.5rem;
  color: var(--p-text-muted-color);
}

.assigned-roles-hint {
  color: var(--p-text-muted-color);
  font-size: var(--mw-font-size-xs, 0.75rem);
}

.assigned-roles-checkboxes {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.assigned-role-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.assigned-role-item label {
  font-size: var(--mw-font-size-sm);
  cursor: pointer;
}
</style>
