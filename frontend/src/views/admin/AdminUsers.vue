<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { usersApi } from '@/api/users.api'
import { roomsApi } from '@/api/rooms.api'
import { familyApi } from '@/api/family.api'
import { sectionsApi } from '@/api/sections.api'
import type { UserInfo, UserRole } from '@/types/user'
import type { RoomInfo, RoomRole } from '@/types/room'
import type { FamilyInfo, SchoolSectionInfo } from '@/types/family'
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
import MultiSelect from 'primevue/multiselect'
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
const filterSpecialRole = ref<string | null>(null)
const filterStatus = ref<string>('all')
const searchQuery = ref('')
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null

// Sections (for SECTION_ADMIN assignment)
const allSections = ref<SchoolSectionInfo[]>([])
const editSectionIds = ref<string[]>([])

// Special roles editing
const editPutzorga = ref(false)
const editPutzorgaSections = ref<string[]>([])
const editElternbeirat = ref(false)
const editElternbeiratSections = ref<string[]>([])

// Edit dialog
const showEdit = ref(false)
const editUser = ref<UserInfo | null>(null)
const editTab = ref('0')
const editLoading = ref(false)

// Profile form
const profileForm = ref({ email: '', firstName: '', lastName: '', phone: '' })
const editActive = ref(true)
const editAssignedRoles = ref<string[]>([])

const assignableRoleOptions = computed(() => [
  { label: t('profile.roleLabels.TEACHER'), value: 'TEACHER' },
  { label: t('profile.roleLabels.PARENT'), value: 'PARENT' },
  { label: t('profile.roleLabels.SECTION_ADMIN'), value: 'SECTION_ADMIN' },
])

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

const roleOptions = computed<{ label: string; value: UserRole }[]>(() => [
  { label: t('profile.roleLabels.SUPERADMIN'), value: 'SUPERADMIN' },
  { label: t('profile.roleLabels.SECTION_ADMIN'), value: 'SECTION_ADMIN' },
  { label: t('profile.roleLabels.TEACHER'), value: 'TEACHER' },
  { label: t('profile.roleLabels.PARENT'), value: 'PARENT' },
  { label: t('profile.roleLabels.STUDENT'), value: 'STUDENT' },
])

const filterRoleOptions = computed(() => [
  { label: t('admin.allRoles'), value: null },
  ...roleOptions.value,
])

const specialRoleFilterOptions = computed(() => [
  { label: t('admin.allSpecialRoles'), value: null },
  { label: t('admin.specialRoleLabels.PUTZORGA'), value: 'PUTZORGA' },
  { label: t('admin.specialRoleLabels.ELTERNBEIRAT'), value: 'ELTERNBEIRAT' },
  { label: t('profile.roleLabels.SECTION_ADMIN'), value: 'SECTION_ADMIN' },
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

// Pending approval users
const pendingUsers = ref<UserInfo[]>([])
const pendingLoading = ref(false)

// When filtering by special role, store results separately
const specialRoleUsers = ref<UserInfo[]>([])
const isSpecialRoleFiltering = computed(() => !!filterSpecialRole.value)

const displayedUsers = computed(() => {
  if (isSpecialRoleFiltering.value) return specialRoleUsers.value
  return users.value
})

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

async function onFilterChange() {
  page.value = 0
  if (filterSpecialRole.value) {
    await loadSpecialRoleUsers()
  } else {
    specialRoleUsers.value = []
    await loadUsers()
  }
}

async function loadSpecialRoleUsers() {
  loading.value = true
  try {
    const res = await usersApi.findBySpecialRole(filterSpecialRole.value!)
    specialRoleUsers.value = res.data.data
  } finally {
    loading.value = false
  }
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

async function openEdit(user: UserInfo) {
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
  // Extract SECTION_ADMIN:<sectionId> entries
  editSectionIds.value = (user.specialRoles || [])
    .filter(r => r.startsWith('SECTION_ADMIN:'))
    .map(r => r.substring('SECTION_ADMIN:'.length))
  // Extract PUTZORGA special roles
  const putzorgaRoles = (user.specialRoles || []).filter(r => r === 'PUTZORGA' || r.startsWith('PUTZORGA:'))
  editPutzorga.value = putzorgaRoles.length > 0
  editPutzorgaSections.value = putzorgaRoles
    .filter(r => r.startsWith('PUTZORGA:'))
    .map(r => r.substring('PUTZORGA:'.length))
  // Extract ELTERNBEIRAT special roles
  const elternbeiratRoles = (user.specialRoles || []).filter(r => r === 'ELTERNBEIRAT' || r.startsWith('ELTERNBEIRAT:'))
  editElternbeirat.value = elternbeiratRoles.length > 0
  editElternbeiratSections.value = elternbeiratRoles
    .filter(r => r.startsWith('ELTERNBEIRAT:'))
    .map(r => r.substring('ELTERNBEIRAT:'.length))
  userRooms.value = []
  userFamilies.value = []
  selectedRoom.value = null
  showEdit.value = true
  // Load sections if needed
  if (allSections.value.length === 0) {
    try {
      const res = await sectionsApi.getAll()
      allSections.value = res.data.data
    } catch { /* ignore */ }
  }
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
    // Save SECTION_ADMIN section assignments
    if (editAssignedRoles.value.includes('SECTION_ADMIN')) {
      await syncSpecialRoles(editUser.value.id, 'SECTION_ADMIN', editSectionIds.value, editUser.value.specialRoles || [])
    }
    // Save PUTZORGA special roles
    await syncSpecialRoles(
      editUser.value.id, 'PUTZORGA',
      editPutzorga.value ? editPutzorgaSections.value : null,
      editUser.value.specialRoles || [],
    )
    // Save ELTERNBEIRAT special roles
    await syncSpecialRoles(
      editUser.value.id, 'ELTERNBEIRAT',
      editElternbeirat.value ? editElternbeiratSections.value : null,
      editUser.value.specialRoles || [],
    )
    toast.add({ severity: 'success', summary: t('admin.userSaved'), life: 3000 })
    showEdit.value = false
    if (isSpecialRoleFiltering.value) {
      await loadSpecialRoleUsers()
    } else {
      await loadUsers()
    }
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  } finally {
    editLoading.value = false
  }
}

/**
 * Syncs special role entries for a given prefix.
 * @param sectionIds - array of section IDs (section-scoped roles), or null to remove all
 *   An empty array with the role enabled means global (prefix only, no sections).
 */
async function syncSpecialRoles(userId: string, prefix: string, sectionIds: string[] | null, currentSpecialRoles: string[]) {
  const currentRoles = currentSpecialRoles.filter(r => r === prefix || r.startsWith(prefix + ':'))
  if (sectionIds === null) {
    // Remove all roles with this prefix
    for (const role of currentRoles) {
      await usersApi.removeSpecialRole(userId, role)
    }
    return
  }
  const newRoles = sectionIds.length > 0
    ? sectionIds.map(id => `${prefix}:${id}`)
    : [prefix] // global role if no sections selected
  // Remove old entries not in new set
  for (const role of currentRoles) {
    if (!newRoles.includes(role)) {
      await usersApi.removeSpecialRole(userId, role)
    }
  }
  // Add new entries not in current set
  for (const role of newRoles) {
    if (!currentRoles.includes(role)) {
      await usersApi.addSpecialRole(userId, role)
    }
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

async function toggleHoursExempt(family: FamilyInfo) {
  try {
    await familyApi.setHoursExempt(family.id, !family.hoursExempt)
    family.hoursExempt = !family.hoursExempt
    toast.add({
      severity: 'success',
      summary: family.hoursExempt ? t('admin.familyExempted') : t('admin.familyNotExempted'),
      life: 3000,
    })
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

function getSectionName(sectionId: string): string {
  const section = allSections.value.find(s => s.id === sectionId)
  return section?.name || sectionId.substring(0, 8) + '...'
}

async function loadPendingUsers() {
  pendingLoading.value = true
  try {
    const res = await usersApi.list({ page: 0, size: 100, active: false })
    pendingUsers.value = res.data.data.content
  } catch {
    pendingUsers.value = []
  } finally {
    pendingLoading.value = false
  }
}

async function approveUser(userId: string) {
  try {
    await usersApi.setActive(userId, true)
    pendingUsers.value = pendingUsers.value.filter(u => u.id !== userId)
    toast.add({ severity: 'success', summary: t('admin.userApproved'), life: 3000 })
    loadUsers()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  }
}


onMounted(async () => {
  loadUsers()
  loadPendingUsers()
  try {
    const res = await sectionsApi.getAll()
    allSections.value = res.data.data
  } catch { /* ignore */ }
})

onUnmounted(() => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
})
</script>

<template>
  <div>
    <PageTitle :title="t('admin.users')" />

    <!-- Pending Approval Users -->
    <div v-if="pendingUsers.length" class="pending-section card">
      <h3><i class="pi pi-user-plus" /> {{ t('admin.pendingUsers') }} ({{ pendingUsers.length }})</h3>
      <div class="pending-list">
        <div v-for="u in pendingUsers" :key="u.id" class="pending-item">
          <div class="pending-info">
            <strong>{{ u.displayName }}</strong>
            <span class="pending-email">{{ u.email }}</span>
          </div>
          <Button
            :label="t('admin.approve')"
            icon="pi pi-check"
            severity="success"
            size="small"
            @click="approveUser(u.id)"
          />
        </div>
      </div>
    </div>

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
      <Select
        v-model="filterSpecialRole"
        :options="specialRoleFilterOptions"
        optionLabel="label"
        optionValue="value"
        :placeholder="t('admin.filterBySpecialRole')"
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
      :value="displayedUsers"
      :loading="loading"
      :paginator="true"
      :rows="rows"
      :totalRecords="isSpecialRoleFiltering ? displayedUsers.length : totalRecords"
      :lazy="!isSpecialRoleFiltering"
      @page="onPage"
      stripedRows
      scrollable
      class="card"
    >
      <template #empty>
        <div class="empty-table">{{ t('admin.noUsersFound') }}</div>
      </template>
      <Column field="displayName" :header="t('common.name')">
        <template #body="{ data }">
          <div>
            <span>{{ data.displayName }}</span>
            <span class="mobile-email hide-desktop">{{ data.email }}</span>
          </div>
        </template>
      </Column>
      <Column field="email" :header="t('auth.email')" class="hide-mobile-column" />
      <Column field="role" :header="t('admin.columnRole')">
        <template #body="{ data }">
          <div class="role-tags">
            <Tag :value="data.role" :severity="roleSeverity(data.role) as any" />
            <template v-for="sr in (data.specialRoles || [])" :key="sr">
              <Tag
                v-if="sr.startsWith('PUTZORGA')"
                value="Putz-Orga"
                severity="warn"
              />
              <Tag
                v-else-if="sr.startsWith('ELTERNBEIRAT')"
                value="Elternbeirat"
                severity="info"
              />
              <Tag
                v-else-if="sr.startsWith('SECTION_ADMIN:')"
                :value="'Bereich: ' + getSectionName(sr.substring('SECTION_ADMIN:'.length))"
                severity="warn"
              />
            </template>
          </div>
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
              <div v-if="editAssignedRoles.includes('SECTION_ADMIN')" class="form-field">
                <label>{{ t('admin.sectionAdminSections') }}</label>
                <MultiSelect
                  v-model="editSectionIds"
                  :options="allSections.map(s => ({ label: s.name, value: s.id }))"
                  optionLabel="label"
                  optionValue="value"
                  :placeholder="t('admin.selectSections')"
                  class="w-full"
                  display="chip"
                />
              </div>
              <div class="form-field">
                <label>{{ t('admin.specialRoles') }}</label>
                <div class="special-roles-section">
                  <div class="special-role-row">
                    <Checkbox v-model="editPutzorga" inputId="sr-putzorga" :binary="true" />
                    <label for="sr-putzorga">Putz-Orga</label>
                  </div>
                  <MultiSelect
                    v-if="editPutzorga"
                    v-model="editPutzorgaSections"
                    :options="allSections.map(s => ({ label: s.name, value: s.id }))"
                    optionLabel="label"
                    optionValue="value"
                    :placeholder="t('admin.allSectionsGlobal')"
                    class="w-full special-role-sections"
                    display="chip"
                  />
                  <div class="special-role-row">
                    <Checkbox v-model="editElternbeirat" inputId="sr-elternbeirat" :binary="true" />
                    <label for="sr-elternbeirat">Elternbeirat</label>
                  </div>
                  <MultiSelect
                    v-if="editElternbeirat"
                    v-model="editElternbeiratSections"
                    :options="allSections.map(s => ({ label: s.name, value: s.id }))"
                    optionLabel="label"
                    optionValue="value"
                    :placeholder="t('admin.allSectionsGlobal')"
                    class="w-full special-role-sections"
                    display="chip"
                  />
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
                  <Tag v-if="fam.hoursExempt" :value="t('admin.hoursExempt')" severity="secondary" />
                  <Button
                    :icon="fam.hoursExempt ? 'pi pi-check-circle' : 'pi pi-ban'"
                    :severity="fam.hoursExempt ? 'success' : 'warn'"
                    text
                    size="small"
                    v-tooltip="fam.hoursExempt ? t('admin.requireHours') : t('admin.exemptHours')"
                    @click="toggleHoursExempt(fam)"
                    :aria-label="t('admin.toggleHoursExempt')"
                  />
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
.pending-section {
  margin-bottom: 1.5rem;
  border-left: 4px solid var(--p-orange-500);
}

.pending-section h3 {
  font-size: var(--mw-font-size-md);
  margin-bottom: 0.75rem;
  color: var(--p-orange-700);
}

.pending-section h3 i {
  margin-right: 0.5rem;
}

.pending-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.pending-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--mw-border-light);
}

.pending-item:last-child {
  border-bottom: none;
}

.pending-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: var(--mw-font-size-sm);
}

.pending-email {
  color: var(--mw-text-secondary);
}

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

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.special-roles-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.special-role-row {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.special-role-row label {
  font-size: var(--mw-font-size-sm);
  cursor: pointer;
}

.special-role-sections {
  margin-left: 1.5rem;
}

.mobile-email {
  display: block;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.125rem;
}

@media (max-width: 767px) {
  :deep(.hide-mobile-column) {
    display: none !important;
  }
  .filter-role {
    min-width: 0;
    width: 100%;
  }
  .filter-search {
    min-width: 0;
    width: 100%;
  }
  .filter-bar {
    flex-direction: column;
  }
  .form-row {
    flex-direction: column;
  }
  .add-row {
    flex-direction: column;
  }
  .add-row .room-autocomplete {
    min-width: 0;
    width: 100%;
  }
  .item-row {
    flex-wrap: wrap;
  }
}
</style>
