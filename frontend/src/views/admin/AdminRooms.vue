<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { roomsApi } from '@/api/rooms.api'
import { usersApi } from '@/api/users.api'
import type { RoomInfo, RoomDetail, CreateRoomRequest, RoomType, RoomRole, RoomMember } from '@/types/room'
import type { UserInfo } from '@/types/user'
import { sectionsApi } from '@/api/sections.api'
import type { SchoolSectionInfo } from '@/types/family'
import PageTitle from '@/components/common/PageTitle.vue'
import AvatarUpload from '@/components/common/AvatarUpload.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import AutoComplete from 'primevue/autocomplete'

const { t } = useI18n()
const toast = useToast()

const rooms = ref<RoomInfo[]>([])
const sections = ref<SchoolSectionInfo[]>([])
const loading = ref(false)
const showCreate = ref(false)
const showEdit = ref(false)
const showArchive = ref(false)
const showDelete = ref(false)
const showMembers = ref(false)
const selectedRoom = ref<RoomInfo | null>(null)

// Filters
const sectionFilter = ref<string | null>(null)
const typeFilter = ref<RoomType | null>(null)

const form = ref<CreateRoomRequest>({
  name: '',
  type: 'KLASSE',
  description: '',
  sectionId: undefined,
})

const editForm = ref<{
  name: string
  type: RoomType
  description: string
  publicDescription: string
  sectionId: string | undefined
}>({
  name: '',
  type: 'KLASSE',
  description: '',
  publicDescription: '',
  sectionId: undefined,
})

const editAvatarUrl = ref<string | null>(null)

const roomTypes: { label: string; value: RoomType }[] = [
  { label: 'Klasse', value: 'KLASSE' },
  { label: 'Gruppe', value: 'GRUPPE' },
  { label: 'Projekt', value: 'PROJEKT' },
  { label: 'Interessengruppe', value: 'INTEREST' },
  { label: 'Sonstige', value: 'CUSTOM' },
]

const roomRoleOptions: { label: string; value: RoomRole }[] = [
  { label: 'Leader', value: 'LEADER' },
  { label: 'Member', value: 'MEMBER' },
  { label: 'Parent', value: 'PARENT_MEMBER' },
  { label: 'Guest', value: 'GUEST' },
]

const sectionMap = computed(() => {
  const map: Record<string, string> = {}
  for (const s of sections.value) {
    map[s.id] = s.name
  }
  return map
})

const filteredRooms = computed(() => {
  let result = rooms.value
  if (sectionFilter.value) {
    result = result.filter(r => r.sectionId === sectionFilter.value)
  }
  if (typeFilter.value) {
    result = result.filter(r => r.type === typeFilter.value)
  }
  return result
})

// Members dialog state
const membersRoom = ref<RoomDetail | null>(null)
const membersLoading = ref(false)
const memberSearchQuery = ref('')
const memberSearchResults = ref<UserInfo[]>([])
const addMemberRole = ref<RoomRole>('MEMBER')
const showMoveCopy = ref(false)
const moveCopyMember = ref<RoomMember | null>(null)
const moveCopyTarget = ref('')
const moveCopyMode = ref<'move' | 'copy'>('move')

async function loadData() {
  loading.value = true
  try {
    const [roomsRes, sectionsRes] = await Promise.all([
      roomsApi.getAll({ page: 0, size: 100, includeArchived: true }),
      sectionsApi.getAll(),
    ])
    rooms.value = roomsRes.data.data.content
    sections.value = sectionsRes.data.data
  } finally {
    loading.value = false
  }
}

async function createRoom() {
  await roomsApi.create(form.value)
  showCreate.value = false
  form.value = { name: '', type: 'KLASSE', description: '', sectionId: undefined }
  await loadData()
}

function openEdit(room: RoomInfo) {
  selectedRoom.value = room
  editForm.value = {
    name: room.name,
    type: room.type as RoomType,
    description: room.description || '',
    publicDescription: room.publicDescription || '',
    sectionId: room.sectionId || undefined,
  }
  editAvatarUrl.value = room.avatarUrl
  showEdit.value = true
}

async function saveEdit() {
  if (!selectedRoom.value) return
  await roomsApi.update(selectedRoom.value.id, {
    name: editForm.value.name,
    description: editForm.value.description,
    publicDescription: editForm.value.publicDescription,
    type: editForm.value.type,
    sectionId: editForm.value.sectionId || null,
  })
  toast.add({ severity: 'success', summary: t('admin.roomSaved'), life: 3000 })
  showEdit.value = false
  await loadData()
}

async function onAvatarUpload(file: File) {
  if (!selectedRoom.value) return
  await roomsApi.uploadAvatar(selectedRoom.value.id, file)
  toast.add({ severity: 'success', summary: t('common.avatarUploaded'), life: 3000 })
  const detail = await roomsApi.getById(selectedRoom.value.id)
  editAvatarUrl.value = detail.data.data.avatarUrl
  await loadData()
}

async function onAvatarRemove() {
  if (!selectedRoom.value) return
  await roomsApi.removeAvatar(selectedRoom.value.id)
  toast.add({ severity: 'success', summary: t('common.avatarRemoved'), life: 3000 })
  editAvatarUrl.value = null
  await loadData()
}

function openArchive(room: RoomInfo) {
  selectedRoom.value = room
  showArchive.value = true
}

async function confirmArchive() {
  if (!selectedRoom.value) return
  const res = await roomsApi.toggleArchive(selectedRoom.value.id)
  const msg = res.data.data.archived ? t('admin.roomDeactivated') : t('admin.roomReactivated')
  toast.add({ severity: 'success', summary: msg, life: 3000 })
  showArchive.value = false
  await loadData()
}

function openDelete(room: RoomInfo) {
  selectedRoom.value = room
  showDelete.value = true
}

async function confirmDelete() {
  if (!selectedRoom.value) return
  await roomsApi.deleteRoom(selectedRoom.value.id)
  toast.add({ severity: 'success', summary: t('admin.roomDeleted'), life: 3000 })
  showDelete.value = false
  await loadData()
}

// Members Dialog
async function openMembers(room: RoomInfo) {
  selectedRoom.value = room
  membersLoading.value = true
  showMembers.value = true
  try {
    const res = await roomsApi.getById(room.id)
    membersRoom.value = res.data.data
  } finally {
    membersLoading.value = false
  }
}

async function searchUsers(event: { query: string }) {
  const res = await usersApi.search(event.query, 0, 10)
  memberSearchResults.value = res.data.data.content.filter(
    (u: UserInfo) => !membersRoom.value?.members.some(m => m.userId === u.id),
  )
}

async function addMember(user: UserInfo) {
  if (!membersRoom.value) return
  try {
    await roomsApi.addMember(membersRoom.value.id, user.id, addMemberRole.value)
    toast.add({ severity: 'success', summary: t('admin.memberAdded'), life: 3000 })
    memberSearchQuery.value = ''
    const res = await roomsApi.getById(membersRoom.value.id)
    membersRoom.value = res.data.data
    await loadData()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 3000 })
  }
}

async function removeMember(userId: string) {
  if (!membersRoom.value) return
  try {
    await roomsApi.removeMember(membersRoom.value.id, userId)
    toast.add({ severity: 'success', summary: t('admin.memberRemoved'), life: 3000 })
    const res = await roomsApi.getById(membersRoom.value.id)
    membersRoom.value = res.data.data
    await loadData()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 3000 })
  }
}

async function updateMemberRole(userId: string, role: RoomRole) {
  if (!membersRoom.value) return
  try {
    await roomsApi.updateMemberRole(membersRoom.value.id, userId, role)
    const res = await roomsApi.getById(membersRoom.value.id)
    membersRoom.value = res.data.data
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 3000 })
  }
}

function openMoveCopy(member: RoomMember, mode: 'move' | 'copy') {
  moveCopyMember.value = member
  moveCopyMode.value = mode
  moveCopyTarget.value = ''
  showMoveCopy.value = true
}

async function executeMoveCopy() {
  if (!membersRoom.value || !moveCopyMember.value || !moveCopyTarget.value) return
  try {
    await roomsApi.addMember(moveCopyTarget.value, moveCopyMember.value.userId, moveCopyMember.value.role)
    if (moveCopyMode.value === 'move') {
      await roomsApi.removeMember(membersRoom.value.id, moveCopyMember.value.userId)
    }
    const msg = moveCopyMode.value === 'move' ? t('admin.memberMoved') : t('admin.memberCopied')
    toast.add({ severity: 'success', summary: msg, life: 3000 })
    showMoveCopy.value = false
    const res = await roomsApi.getById(membersRoom.value.id)
    membersRoom.value = res.data.data
    await loadData()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 3000 })
  }
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="header-row">
      <PageTitle :title="t('admin.rooms')" />
      <Button :label="t('rooms.create')" icon="pi pi-plus" @click="showCreate = true" />
    </div>

    <!-- Filters -->
    <div class="filter-row">
      <Select
        v-model="sectionFilter"
        :options="sections"
        optionLabel="name"
        optionValue="id"
        :placeholder="t('admin.allSections')"
        showClear
        style="width: 200px"
      />
      <Select
        v-model="typeFilter"
        :options="roomTypes"
        optionLabel="label"
        optionValue="value"
        :placeholder="t('admin.allTypes')"
        showClear
        style="width: 180px"
      />
    </div>

    <DataTable :value="filteredRooms" :loading="loading" stripedRows scrollable class="card">
      <Column field="name" :header="t('common.name')" />
      <Column field="type" :header="t('admin.columnType')">
        <template #body="{ data }">
          <Tag :value="t(`rooms.types.${data.type}`)" />
        </template>
      </Column>
      <Column :header="t('admin.columnSection')">
        <template #body="{ data }">
          {{ data.sectionId ? sectionMap[data.sectionId] || '—' : '—' }}
        </template>
      </Column>
      <Column field="memberCount" :header="t('admin.columnMembers')" />
      <Column :header="t('common.status')">
        <template #body="{ data }">
          <Tag
            :value="data.archived ? t('admin.deactivated') : t('common.active')"
            :severity="data.archived ? 'danger' : 'success'"
          />
        </template>
      </Column>
      <Column :header="t('common.actions')">
        <template #body="{ data }">
          <div class="action-buttons">
            <Button
              icon="pi pi-users"
              severity="info"
              text
              rounded
              :aria-label="t('admin.manageMembers')"
              @click="openMembers(data)"
            />
            <Button
              icon="pi pi-pencil"
              severity="secondary"
              text
              rounded
              :aria-label="t('admin.editRoom')"
              @click="openEdit(data)"
            />
            <Button
              :icon="data.archived ? 'pi pi-replay' : 'pi pi-ban'"
              severity="warn"
              text
              rounded
              :aria-label="data.archived ? t('admin.reactivateRoom') : t('admin.deactivateRoom')"
              @click="openArchive(data)"
            />
            <Button
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              :aria-label="t('admin.deleteRoom')"
              @click="openDelete(data)"
            />
          </div>
        </template>
      </Column>
    </DataTable>

    <!-- Create Dialog -->
    <Dialog v-model:visible="showCreate" :header="t('rooms.create')" modal :style="{ width: '500px', maxWidth: '90vw' }">
      <form @submit.prevent="createRoom" class="dialog-form">
        <div class="form-field">
          <label class="required">{{ t('rooms.name') }}</label>
          <InputText v-model="form.name" required class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('rooms.type') }}</label>
          <Select v-model="form.type" :options="roomTypes" optionLabel="label" optionValue="value" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('rooms.section') }}</label>
          <Select
            v-model="form.sectionId"
            :options="sections"
            optionLabel="name"
            optionValue="id"
            :placeholder="t('admin.noSection')"
            showClear
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>{{ t('rooms.description') }}</label>
          <Textarea v-model="form.description" rows="3" class="w-full" />
        </div>
      </form>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showCreate = false" />
        <Button :label="t('common.create')" @click="createRoom" />
      </template>
    </Dialog>

    <!-- Edit Dialog -->
    <Dialog v-model:visible="showEdit" :header="t('admin.editRoom')" modal :style="{ width: '500px', maxWidth: '90vw' }">
      <form @submit.prevent="saveEdit" class="dialog-form">
        <AvatarUpload
          :image-url="editAvatarUrl"
          size="lg"
          icon="pi-home"
          editable
          @upload="onAvatarUpload"
          @remove="onAvatarRemove"
        />
        <div class="form-field">
          <label class="required">{{ t('rooms.name') }}</label>
          <InputText v-model="editForm.name" required class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('rooms.type') }}</label>
          <Select v-model="editForm.type" :options="roomTypes" optionLabel="label" optionValue="value" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('rooms.section') }}</label>
          <Select
            v-model="editForm.sectionId"
            :options="sections"
            optionLabel="name"
            optionValue="id"
            :placeholder="t('admin.noSection')"
            showClear
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>{{ t('rooms.description') }}</label>
          <Textarea v-model="editForm.description" rows="3" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('rooms.publicDescription') }}</label>
          <Textarea v-model="editForm.publicDescription" rows="2" class="w-full" />
        </div>
      </form>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showEdit = false" />
        <Button :label="t('common.save')" @click="saveEdit" />
      </template>
    </Dialog>

    <!-- Members Dialog -->
    <Dialog v-model:visible="showMembers" :header="t('admin.manageMembers') + (selectedRoom ? ` — ${selectedRoom.name}` : '')" modal :style="{ width: '650px', maxWidth: '95vw' }">
      <div v-if="membersLoading" class="loading-center">
        <i class="pi pi-spin pi-spinner" style="font-size: 2rem" />
      </div>
      <template v-else-if="membersRoom">
        <div class="add-row">
          <AutoComplete
            v-model="memberSearchQuery"
            :suggestions="memberSearchResults"
            optionLabel="displayName"
            :placeholder="t('admin.searchUser')"
            @complete="searchUsers"
            @item-select="(e: any) => addMember(e.value)"
            class="flex-grow"
          />
          <Select v-model="addMemberRole" :options="roomRoleOptions" optionLabel="label" optionValue="value" style="width: 140px" />
        </div>
        <div v-if="membersRoom.members.length === 0" class="empty-state">{{ t('rooms.noMembers') }}</div>
        <div v-else class="member-list">
          <div v-for="member in membersRoom.members" :key="member.userId" class="member-row">
            <span class="member-name">{{ member.displayName }}</span>
            <Select
              :modelValue="member.role"
              :options="roomRoleOptions"
              optionLabel="label"
              optionValue="value"
              style="width: 140px"
              @update:modelValue="(val: RoomRole) => updateMemberRole(member.userId, val)"
            />
            <div class="member-actions">
              <Button
                icon="pi pi-arrow-right"
                severity="secondary"
                text
                size="small"
                :aria-label="t('admin.moveMember')"
                @click="openMoveCopy(member, 'move')"
                v-tooltip.top="t('admin.moveMember')"
              />
              <Button
                icon="pi pi-copy"
                severity="secondary"
                text
                size="small"
                :aria-label="t('admin.copyMember')"
                @click="openMoveCopy(member, 'copy')"
                v-tooltip.top="t('admin.copyMember')"
              />
              <Button
                icon="pi pi-trash"
                severity="danger"
                text
                size="small"
                :aria-label="t('common.delete')"
                @click="removeMember(member.userId)"
              />
            </div>
          </div>
        </div>
      </template>
    </Dialog>

    <!-- Move/Copy Dialog -->
    <Dialog v-model:visible="showMoveCopy" :header="moveCopyMode === 'move' ? t('admin.moveMember') : t('admin.copyMember')" modal :style="{ width: '400px', maxWidth: '90vw' }">
      <p>{{ moveCopyMember?.displayName }}</p>
      <div class="form-field" style="margin-top: 0.75rem;">
        <label>{{ t('admin.targetRoom') }}</label>
        <Select
          v-model="moveCopyTarget"
          :options="rooms.filter(r => r.id !== membersRoom?.id)"
          optionLabel="name"
          optionValue="id"
          :placeholder="t('admin.selectRoom')"
          class="w-full"
          filter
        />
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showMoveCopy = false" />
        <Button :label="moveCopyMode === 'move' ? t('admin.moveMember') : t('admin.copyMember')" @click="executeMoveCopy" :disabled="!moveCopyTarget" />
      </template>
    </Dialog>

    <!-- Archive/Reactivate Dialog -->
    <Dialog v-model:visible="showArchive" :header="selectedRoom?.archived ? t('admin.reactivateRoom') : t('admin.deactivateRoom')" modal :style="{ width: '450px', maxWidth: '90vw' }">
      <p>
        {{ selectedRoom?.archived
          ? t('admin.reactivateRoomConfirm', { name: selectedRoom?.name })
          : t('admin.deactivateRoomConfirm', { name: selectedRoom?.name })
        }}
      </p>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showArchive = false" />
        <Button
          :label="selectedRoom?.archived ? t('admin.reactivateRoom') : t('admin.deactivateRoom')"
          :severity="selectedRoom?.archived ? 'success' : 'warn'"
          @click="confirmArchive"
        />
      </template>
    </Dialog>

    <!-- Delete Dialog -->
    <Dialog v-model:visible="showDelete" :header="t('admin.deleteRoom')" modal :style="{ width: '450px', maxWidth: '90vw' }">
      <p>{{ t('admin.deleteRoomConfirm', { name: selectedRoom?.name }) }}</p>
      <p class="delete-warn">{{ t('admin.deleteRoomWarn') }}</p>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showDelete = false" />
        <Button :label="t('common.delete')" severity="danger" @click="confirmDelete" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.filter-row {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
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

.action-buttons {
  display: flex;
  gap: 0.25rem;
}

.delete-warn {
  color: var(--p-red-500);
  font-size: var(--mw-font-size-sm);
  margin-top: 0.5rem;
}

.add-row {
  display: flex;
  gap: 0.5rem;
  align-items: flex-start;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.flex-grow {
  flex: 1;
  min-width: 180px;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.member-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border-radius: var(--p-border-radius);
}

.member-row:hover {
  background-color: var(--p-surface-100);
}

.member-name {
  flex: 1;
  font-weight: 500;
  min-width: 0;
}

.member-actions {
  display: flex;
  gap: 0.125rem;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 2rem 0;
}

.empty-state {
  text-align: center;
  padding: 2rem 0;
  color: var(--p-text-muted-color);
}
</style>
