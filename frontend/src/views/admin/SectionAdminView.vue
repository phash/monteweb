<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { useRouter } from 'vue-router'
import { sectionAdminApi } from '@/api/sectionAdmin.api'
import type { SectionInfo } from '@/api/sectionAdmin.api'
import type { UserInfo } from '@/types/user'
import type { RoomInfo } from '@/types/room'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'

const { t } = useI18n()
const toast = useToast()
const router = useRouter()

const sections = ref<SectionInfo[]>([])
const selectedSectionId = ref<string | null>(null)
const users = ref<UserInfo[]>([])
const rooms = ref<RoomInfo[]>([])
const loading = ref(false)
const usersLoading = ref(false)
const roomsLoading = ref(false)
const activeTab = ref('0')

// Filters
const userSearch = ref('')
const userRoleFilter = ref<string | null>(null)
const roomSearch = ref('')
const roomTypeFilter = ref<string | null>(null)

// Create room dialog
const showCreateRoom = ref(false)
const roomForm = ref({ name: '', description: '', type: 'KLASSE' as string })
const roomCreating = ref(false)

// Assign role dialog
const showAssignRole = ref(false)
const assignTarget = ref<UserInfo | null>(null)
const assignRole = ref('PUTZORGA')

const sectionOptions = computed(() =>
  sections.value.map(s => ({ label: s.name, value: s.id }))
)

const roomTypeOptions = computed(() => [
  { label: t('rooms.types.KLASSE'), value: 'KLASSE' },
  { label: t('rooms.types.GRUPPE'), value: 'GRUPPE' },
  { label: t('rooms.types.PROJEKT'), value: 'PROJEKT' },
  { label: t('rooms.types.CUSTOM'), value: 'CUSTOM' },
])

const userRoleOptions = computed(() => [
  { label: t('sectionAdmin.allRoles'), value: null },
  { label: 'SUPERADMIN', value: 'SUPERADMIN' },
  { label: 'SECTION_ADMIN', value: 'SECTION_ADMIN' },
  { label: 'TEACHER', value: 'TEACHER' },
  { label: 'PARENT', value: 'PARENT' },
  { label: 'STUDENT', value: 'STUDENT' },
])

const roomTypeFilterOptions = computed(() => [
  { label: t('sectionAdmin.allTypes'), value: null },
  { label: t('rooms.types.KLASSE'), value: 'KLASSE' },
  { label: t('rooms.types.GRUPPE'), value: 'GRUPPE' },
  { label: t('rooms.types.PROJEKT'), value: 'PROJEKT' },
  { label: t('rooms.types.CUSTOM'), value: 'CUSTOM' },
])

const roleOptions = [
  { label: 'PUTZORGA', value: 'PUTZORGA' },
  { label: 'ELTERNBEIRAT', value: 'ELTERNBEIRAT' },
]

const filteredUsers = computed(() => {
  let result = users.value
  if (userRoleFilter.value) {
    result = result.filter(u => u.role === userRoleFilter.value)
  }
  if (userSearch.value.trim()) {
    const q = userSearch.value.trim().toLowerCase()
    result = result.filter(u =>
      u.displayName.toLowerCase().includes(q) ||
      u.email.toLowerCase().includes(q)
    )
  }
  return result
})

const filteredRooms = computed(() => {
  let result = rooms.value
  if (roomTypeFilter.value) {
    result = result.filter(r => r.type === roomTypeFilter.value)
  }
  if (roomSearch.value.trim()) {
    const q = roomSearch.value.trim().toLowerCase()
    result = result.filter(r =>
      r.name.toLowerCase().includes(q) ||
      (r.description && r.description.toLowerCase().includes(q))
    )
  }
  return result
})

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

function specialRoleSeverity(role: string): string {
  if (role.startsWith('PUTZORGA')) return 'warn'
  if (role.startsWith('ELTERNBEIRAT')) return 'info'
  return 'secondary'
}

function roomTypeSeverity(type: string): string {
  const map: Record<string, string> = {
    KLASSE: 'info',
    GRUPPE: 'success',
    PROJEKT: 'warn',
    CUSTOM: 'secondary',
  }
  return map[type] ?? 'secondary'
}

async function loadSections() {
  loading.value = true
  try {
    const res = await sectionAdminApi.getMySections()
    sections.value = res.data.data
    if (sections.value.length > 0 && !selectedSectionId.value) {
      selectedSectionId.value = sections.value[0]!.id
    }
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  if (!selectedSectionId.value) {
    users.value = []
    return
  }
  usersLoading.value = true
  try {
    const res = await sectionAdminApi.getSectionUsers(selectedSectionId.value)
    users.value = res.data.data
  } finally {
    usersLoading.value = false
  }
}

async function loadRooms() {
  if (!selectedSectionId.value) {
    rooms.value = []
    return
  }
  roomsLoading.value = true
  try {
    const res = await sectionAdminApi.getSectionRooms(selectedSectionId.value)
    rooms.value = res.data.data
  } finally {
    roomsLoading.value = false
  }
}

watch(selectedSectionId, () => {
  loadUsers()
  loadRooms()
})

function openAssignRole(user: UserInfo) {
  assignTarget.value = user
  assignRole.value = 'PUTZORGA'
  showAssignRole.value = true
}

async function doAssignRole() {
  if (!assignTarget.value) return
  try {
    await sectionAdminApi.assignSpecialRole(assignTarget.value.id, assignRole.value)
    toast.add({ severity: 'success', summary: t('sectionAdmin.roleAssigned'), life: 3000 })
    showAssignRole.value = false
    await loadUsers()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  }
}

async function removeRole(user: UserInfo, role: string) {
  try {
    await sectionAdminApi.removeSpecialRole(user.id, role)
    toast.add({ severity: 'success', summary: t('sectionAdmin.roleRemoved'), life: 3000 })
    await loadUsers()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  }
}

function openCreateRoom() {
  roomForm.value = { name: '', description: '', type: 'KLASSE' }
  showCreateRoom.value = true
}

async function doCreateRoom() {
  if (!selectedSectionId.value) return
  roomCreating.value = true
  try {
    await sectionAdminApi.createRoom({
      name: roomForm.value.name,
      description: roomForm.value.description || undefined,
      type: roomForm.value.type,
      sectionId: selectedSectionId.value,
    })
    toast.add({ severity: 'success', summary: t('sectionAdmin.roomCreated'), life: 3000 })
    showCreateRoom.value = false
    await loadRooms()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  } finally {
    roomCreating.value = false
  }
}

function viewRoomMembers(room: RoomInfo) {
  router.push({ name: 'room-detail', params: { id: room.id } })
}

function getRemovableSpecialRoles(user: UserInfo): string[] {
  return (user.specialRoles || []).filter(
    (r: string) => r === 'PUTZORGA' || r === 'ELTERNBEIRAT' || r.startsWith('PUTZORGA:') || r.startsWith('ELTERNBEIRAT:')
  )
}

onMounted(loadSections)
</script>

<template>
  <div>
    <PageTitle :title="t('sectionAdmin.title')" />

    <LoadingSpinner v-if="loading && sections.length === 0" />

    <div v-else-if="sections.length === 0" class="empty-state">
      {{ t('sectionAdmin.noSections') }}
    </div>

    <template v-else>
      <!-- Section selector -->
      <div class="toolbar">
        <Select
          v-model="selectedSectionId"
          :options="sectionOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('sectionAdmin.selectSection')"
          class="section-select"
        />
      </div>

      <!-- Tabs -->
      <Tabs v-model:value="activeTab">
        <TabList>
          <Tab value="0">{{ t('sectionAdmin.tabUsers') }}</Tab>
          <Tab value="1">{{ t('sectionAdmin.tabRooms') }}</Tab>
        </TabList>

        <TabPanels>
          <!-- Users Tab -->
          <TabPanel value="0">
            <div class="filter-bar">
              <Select
                v-model="userRoleFilter"
                :options="userRoleOptions"
                optionLabel="label"
                optionValue="value"
                :placeholder="t('sectionAdmin.allRoles')"
                class="filter-select"
              />
              <IconField>
                <InputIcon class="pi pi-search" />
                <InputText
                  v-model="userSearch"
                  :placeholder="t('sectionAdmin.searchUsers')"
                  class="filter-search"
                />
              </IconField>
            </div>

            <LoadingSpinner v-if="usersLoading && users.length === 0" />

            <div v-else-if="filteredUsers.length === 0 && selectedSectionId" class="empty-state">
              {{ t('sectionAdmin.noUsers') }}
            </div>

            <DataTable
              v-else
              :value="filteredUsers"
              :loading="usersLoading"
              stripedRows
              scrollable
              sortField="displayName"
              :sortOrder="1"
              class="card"
            >
              <Column field="displayName" :header="t('common.name')" sortable />
              <Column field="email" :header="t('auth.email')" sortable />
              <Column field="role" :header="t('admin.columnRole')" sortable>
                <template #body="{ data }">
                  <Tag :value="data.role" :severity="roleSeverity(data.role) as any" />
                </template>
              </Column>
              <Column :header="t('sectionAdmin.specialRoles')">
                <template #body="{ data }">
                  <div class="special-roles-cell">
                    <span
                      v-for="role in getRemovableSpecialRoles(data)"
                      :key="role"
                      class="special-role-tag"
                    >
                      <Tag :value="role" :severity="specialRoleSeverity(role) as any" />
                      <Button
                        icon="pi pi-times"
                        severity="danger"
                        text
                        size="small"
                        class="role-remove-btn"
                        @click="removeRole(data, role)"
                        :aria-label="t('sectionAdmin.removeRole')"
                      />
                    </span>
                  </div>
                </template>
              </Column>
              <Column :header="t('common.actions')" style="width: 120px">
                <template #body="{ data }">
                  <Button
                    icon="pi pi-user-plus"
                    severity="secondary"
                    text
                    size="small"
                    @click="openAssignRole(data)"
                    :aria-label="t('sectionAdmin.assignRole')"
                    v-tooltip.top="t('sectionAdmin.assignRole')"
                  />
                </template>
              </Column>
            </DataTable>
          </TabPanel>

          <!-- Rooms Tab -->
          <TabPanel value="1">
            <div class="filter-bar">
              <Select
                v-model="roomTypeFilter"
                :options="roomTypeFilterOptions"
                optionLabel="label"
                optionValue="value"
                :placeholder="t('sectionAdmin.allTypes')"
                class="filter-select"
              />
              <IconField>
                <InputIcon class="pi pi-search" />
                <InputText
                  v-model="roomSearch"
                  :placeholder="t('sectionAdmin.searchRooms')"
                  class="filter-search"
                />
              </IconField>
              <Button
                :label="t('sectionAdmin.createRoom')"
                icon="pi pi-plus"
                size="small"
                @click="openCreateRoom"
                :disabled="!selectedSectionId"
              />
            </div>

            <LoadingSpinner v-if="roomsLoading && rooms.length === 0" />

            <div v-else-if="filteredRooms.length === 0 && selectedSectionId" class="empty-state">
              {{ t('sectionAdmin.noRooms') }}
            </div>

            <DataTable
              v-else
              :value="filteredRooms"
              :loading="roomsLoading"
              stripedRows
              scrollable
              sortField="name"
              :sortOrder="1"
              class="card"
            >
              <Column field="name" :header="t('common.name')" sortable />
              <Column field="type" :header="t('sectionAdmin.roomType')" sortable>
                <template #body="{ data }">
                  <Tag :value="t('rooms.types.' + data.type)" :severity="roomTypeSeverity(data.type) as any" />
                </template>
              </Column>
              <Column field="memberCount" :header="t('sectionAdmin.memberCount')" sortable style="width: 120px" />
              <Column :header="t('common.actions')" style="width: 140px">
                <template #body="{ data }">
                  <Button
                    icon="pi pi-users"
                    severity="secondary"
                    text
                    size="small"
                    @click="viewRoomMembers(data)"
                    :aria-label="t('sectionAdmin.viewMembers')"
                    v-tooltip.top="t('sectionAdmin.viewMembers')"
                  />
                </template>
              </Column>
            </DataTable>
          </TabPanel>
        </TabPanels>
      </Tabs>
    </template>

    <!-- Assign Role Dialog -->
    <Dialog
      v-model:visible="showAssignRole"
      :header="t('sectionAdmin.assignRole')"
      modal
      :style="{ width: '400px', maxWidth: '95vw' }"
    >
      <div class="dialog-form">
        <p v-if="assignTarget">{{ assignTarget.displayName }} ({{ assignTarget.email }})</p>
        <div class="form-field">
          <label>{{ t('sectionAdmin.specialRoles') }}</label>
          <Select
            v-model="assignRole"
            :options="roleOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>
        <p class="hint">{{ t('sectionAdmin.onlyAllowedRoles') }}</p>
        <div class="form-actions">
          <Button :label="t('sectionAdmin.assignRole')" @click="doAssignRole" />
        </div>
      </div>
    </Dialog>

    <!-- Create Room Dialog -->
    <Dialog
      v-model:visible="showCreateRoom"
      :header="t('sectionAdmin.createRoom')"
      modal
      :style="{ width: '500px', maxWidth: '95vw' }"
    >
      <form @submit.prevent="doCreateRoom" class="dialog-form">
        <div class="form-field">
          <label>{{ t('sectionAdmin.roomName') }}</label>
          <InputText v-model="roomForm.name" class="w-full" required />
        </div>
        <div class="form-field">
          <label>{{ t('sectionAdmin.roomType') }}</label>
          <Select
            v-model="roomForm.type"
            :options="roomTypeOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>{{ t('sectionAdmin.roomDescription') }}</label>
          <Textarea v-model="roomForm.description" rows="3" class="w-full" />
        </div>
        <div class="form-actions">
          <Button :label="t('sectionAdmin.createRoom')" type="submit" :loading="roomCreating" :disabled="!roomForm.name" />
        </div>
      </form>
    </Dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.section-select {
  min-width: 250px;
}

.filter-bar {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.filter-select {
  min-width: 180px;
}

.filter-search {
  min-width: 220px;
}

.empty-state {
  padding: 3rem 1rem;
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 0.5rem;
}

.hint {
  color: var(--p-text-muted-color);
  font-size: var(--mw-font-size-sm);
  margin: 0;
}

.special-roles-cell {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
  align-items: center;
}

.special-role-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.125rem;
}

.role-remove-btn {
  padding: 0.125rem;
  width: 1.25rem;
  height: 1.25rem;
}
</style>
