<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRoomsStore } from '@/stores/rooms'
import { useI18n } from 'vue-i18n'
import { roomsApi } from '@/api/rooms.api'
import { sectionsApi } from '@/api/sections.api'
import type { RoomInfo } from '@/types/room'
import type { SchoolSectionInfo } from '@/types/family'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import Dialog from 'primevue/dialog'
import Chips from 'primevue/chips'
import Textarea from 'primevue/textarea'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const router = useRouter()
const roomsStore = useRoomsStore()
const toast = useToast()

const searchQuery = ref('')
const showCreateDialog = ref(false)
const newRoom = ref({ name: '', description: '', tags: [] as string[] })

// Data
const allRooms = ref<RoomInfo[]>([])
const sections = ref<SchoolSectionInfo[]>([])
const loading = ref(false)

// Filters
const selectedSectionId = ref<string | null>(null)
const selectedType = ref<string | null>(null)

// Join request dialog
const showJoinRequestDialog = ref(false)
const joinRequestRoomId = ref('')
const joinRequestRoomName = ref('')
const joinRequestMessage = ref('')
const joinRequestLoading = ref(false)

const roomTypes = computed(() => [
  { label: t('discover.allTypes'), value: null },
  { label: t('rooms.types.KLASSE'), value: 'KLASSE' },
  { label: t('rooms.types.GRUPPE'), value: 'GRUPPE' },
  { label: t('rooms.types.PROJEKT'), value: 'PROJEKT' },
  { label: t('rooms.types.INTEREST'), value: 'INTEREST' },
  { label: t('rooms.types.CUSTOM'), value: 'CUSTOM' },
])

const sectionOptions = computed(() => [
  { label: t('discover.allSections'), value: null },
  ...sections.value.map(s => ({ label: s.name, value: s.id })),
])

// Filter rooms
const filteredRooms = computed(() => {
  let rooms = allRooms.value
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    rooms = rooms.filter(r =>
      r.name.toLowerCase().includes(q) ||
      r.publicDescription?.toLowerCase().includes(q) ||
      r.description?.toLowerCase().includes(q),
    )
  }
  if (selectedSectionId.value) {
    rooms = rooms.filter(r => r.sectionId === selectedSectionId.value)
  }
  if (selectedType.value) {
    rooms = rooms.filter(r => r.type === selectedType.value)
  }
  return rooms
})

// Group rooms by section, sorted by section sortOrder, rooms alphabetically
const groupedRooms = computed(() => {
  const groups: { sectionId: string | null; sectionName: string; rooms: RoomInfo[] }[] = []
  const bySection = new Map<string | null, RoomInfo[]>()

  for (const room of filteredRooms.value) {
    const key = room.sectionId
    if (!bySection.has(key)) bySection.set(key, [])
    bySection.get(key)!.push(room)
  }

  // Sort sections by sortOrder
  const sortedSections = sections.value.slice().sort((a, b) => a.sortOrder - b.sortOrder)

  for (const section of sortedSections) {
    const rooms = bySection.get(section.id)
    if (rooms && rooms.length > 0) {
      rooms.sort((a, b) => a.name.localeCompare(b.name, 'de'))
      groups.push({ sectionId: section.id, sectionName: section.name, rooms })
      bySection.delete(section.id)
    }
  }

  // Rooms without section
  const noSection = bySection.get(null)
  if (noSection && noSection.length > 0) {
    noSection.sort((a, b) => a.name.localeCompare(b.name, 'de'))
    groups.push({ sectionId: null, sectionName: t('discover.otherRooms'), rooms: noSection })
  }

  return groups
})

onMounted(async () => {
  loading.value = true
  try {
    const [sectionsRes, roomsRes] = await Promise.all([
      sectionsApi.getAll(),
      roomsApi.browse({ size: 200 }),
    ])
    sections.value = sectionsRes.data.data
    allRooms.value = roomsRes.data.data.content
  } catch {
    // fallback
  } finally {
    loading.value = false
  }
})

async function joinRoom(roomId: string) {
  try {
    await roomsStore.joinRoom(roomId)
    toast.add({ severity: 'success', summary: t('discover.joined'), life: 3000 })
    // Reload rooms
    const res = await roomsApi.browse({ size: 200 })
    allRooms.value = res.data.data.content
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

function openJoinRequestDialog(room: RoomInfo) {
  joinRequestRoomId.value = room.id
  joinRequestRoomName.value = room.name
  joinRequestMessage.value = ''
  showJoinRequestDialog.value = true
}

async function submitJoinRequest() {
  joinRequestLoading.value = true
  try {
    await roomsApi.requestJoin(joinRequestRoomId.value, joinRequestMessage.value || undefined)
    toast.add({ severity: 'success', summary: t('rooms.joinRequestSent'), life: 3000 })
    showJoinRequestDialog.value = false
    const res = await roomsApi.browse({ size: 200 })
    allRooms.value = res.data.data.content
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    joinRequestLoading.value = false
  }
}

async function createInterestRoom() {
  try {
    const room = await roomsStore.createInterestRoom({
      name: newRoom.value.name,
      description: newRoom.value.description || undefined,
      tags: newRoom.value.tags.length > 0 ? newRoom.value.tags : undefined,
    })
    showCreateDialog.value = false
    newRoom.value = { name: '', description: '', tags: [] }
    toast.add({ severity: 'success', summary: t('discover.created'), life: 3000 })
    router.push({ name: 'room-detail', params: { id: room.id } })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>{{ t('discover.title') }}</h1>
      <Button :label="t('discover.createRoom')" icon="pi pi-plus" @click="showCreateDialog = true" />
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <InputText
        v-model="searchQuery"
        :placeholder="t('discover.searchPlaceholder')"
        class="search-input"
      />
      <Select
        v-model="selectedSectionId"
        :options="sectionOptions"
        optionLabel="label"
        optionValue="value"
        :placeholder="t('discover.filterBySection')"
        class="section-filter"
      />
      <Select
        v-model="selectedType"
        :options="roomTypes"
        optionLabel="label"
        optionValue="value"
        :placeholder="t('discover.filterByType')"
        class="type-filter"
      />
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <i class="pi pi-spin pi-spinner" style="font-size: 1.5rem" />
    </div>

    <!-- Empty -->
    <div v-else-if="groupedRooms.length === 0" class="empty-state">
      <i class="pi pi-search" />
      <p>{{ t('discover.noRooms') }}</p>
    </div>

    <!-- Grouped rooms -->
    <template v-else>
      <div v-for="group in groupedRooms" :key="group.sectionId ?? 'none'" class="section-group">
        <h2 class="section-heading">
          <i class="pi pi-sitemap" />
          {{ group.sectionName }}
          <span class="section-count">{{ group.rooms.length }}</span>
        </h2>

        <div class="rooms-grid">
          <div
            v-for="room in group.rooms"
            :key="room.id"
            class="room-card card"
          >
            <!-- Avatar -->
            <div
              class="room-avatar"
              @click="router.push({ name: 'room-detail', params: { id: room.id } })"
            >
              <img v-if="room.avatarUrl" :src="room.avatarUrl" :alt="room.name" class="room-avatar-img" />
              <i v-else class="pi pi-home room-avatar-icon" />
            </div>

            <!-- Info -->
            <div class="room-info">
              <div class="room-info-top">
                <h3
                  class="room-name"
                  tabindex="0"
                  role="link"
                  @click="router.push({ name: 'room-detail', params: { id: room.id } })"
                  @keydown.enter="router.push({ name: 'room-detail', params: { id: room.id } })"
                >
                  {{ room.name }}
                </h3>
                <Tag :value="t('rooms.types.' + room.type)" severity="info" class="type-tag" />
              </div>

              <p v-if="room.publicDescription || room.description" class="room-desc">
                {{ room.publicDescription || room.description }}
              </p>

              <div v-if="room.tags && room.tags.length > 0" class="room-tags">
                <Tag v-for="tag in room.tags" :key="tag" :value="tag" severity="secondary" class="room-tag" />
              </div>

              <div class="room-footer">
                <div class="room-meta">
                  <span class="meta-item">
                    <i class="pi pi-users" /> {{ room.memberCount }} {{ t('discover.members') }}
                  </span>
                  <Tag
                    :value="t('rooms.joinPolicies.' + room.joinPolicy)"
                    :severity="room.joinPolicy === 'OPEN' ? 'success' : room.joinPolicy === 'REQUEST' ? 'warn' : 'secondary'"
                    class="policy-tag"
                  />
                </div>
                <Button
                  v-if="room.joinPolicy === 'OPEN'"
                  :label="t('discover.join')"
                  icon="pi pi-sign-in"
                  size="small"
                  @click="joinRoom(room.id)"
                />
                <Button
                  v-else-if="room.joinPolicy === 'REQUEST'"
                  :label="t('rooms.requestJoin')"
                  icon="pi pi-send"
                  size="small"
                  severity="secondary"
                  @click="openJoinRequestDialog(room)"
                />
                <Tag v-else :value="t('rooms.inviteOnly')" severity="warn" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Create Interest Room Dialog -->
    <Dialog v-model:visible="showCreateDialog" :header="t('discover.createRoom')" modal :style="{ width: '500px', maxWidth: '90vw' }">
      <div class="dialog-form">
        <div class="form-field">
          <label class="required">{{ t('rooms.name') }}</label>
          <InputText v-model="newRoom.name" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('rooms.description') }}</label>
          <InputText v-model="newRoom.description" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('discover.tags') }}</label>
          <Chips v-model="newRoom.tags" :placeholder="t('discover.tagsPlaceholder')" class="w-full" />
        </div>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" text @click="showCreateDialog = false" />
        <Button :label="t('common.create')" icon="pi pi-check" @click="createInterestRoom"
                :disabled="!newRoom.name" />
      </template>
    </Dialog>

    <!-- Join Request Dialog -->
    <Dialog v-model:visible="showJoinRequestDialog" :header="t('rooms.requestJoin')" modal :style="{ width: '450px', maxWidth: '90vw' }">
      <div class="dialog-form">
        <p>{{ t('rooms.joinRequestMessage', { room: joinRequestRoomName }) }}</p>
        <Textarea v-model="joinRequestMessage" :placeholder="t('rooms.joinRequestPlaceholder')"
                  class="w-full" rows="3" />
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" text @click="showJoinRequestDialog = false" />
        <Button :label="t('rooms.requestJoin')" icon="pi pi-send"
                :loading="joinRequestLoading" @click="submitJoinRequest" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
}

.page-header h1 {
  font-size: var(--mw-font-size-xl);
  font-weight: 700;
}

.filter-bar {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 1.5rem;
}

.search-input {
  flex: 1;
  min-width: 200px;
}

.section-filter,
.type-filter {
  min-width: 180px;
}

.loading-state {
  text-align: center;
  padding: 3rem;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: var(--mw-text-muted);
}

.empty-state i {
  font-size: 2rem;
  margin-bottom: 0.75rem;
  display: block;
}

/* Section groups */
.section-group {
  margin-bottom: 2rem;
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--mw-font-size-lg);
  font-weight: 600;
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--mw-border-light);
}

.section-heading i {
  color: var(--mw-primary, #3b82f6);
}

.section-count {
  font-size: var(--mw-font-size-xs);
  font-weight: 500;
  color: var(--mw-text-muted);
  background: var(--mw-bg);
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
}

/* Rooms grid */
.rooms-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.75rem;
}

@media (min-width: 768px) {
  .rooms-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1200px) {
  .rooms-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* Room card */
.room-card {
  display: flex;
  gap: 0.875rem;
  transition: box-shadow 0.15s;
}

.room-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.room-avatar {
  width: 56px;
  height: 56px;
  border-radius: var(--mw-border-radius-sm);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--mw-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.room-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.room-avatar-icon {
  font-size: 1.5rem;
  color: var(--mw-text-muted);
}

.room-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.room-info-top {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.room-name {
  font-size: var(--mw-font-size-md);
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.room-name:hover {
  color: var(--mw-primary, #3b82f6);
}

.type-tag {
  font-size: 0.65rem;
  flex-shrink: 0;
}

.room-desc {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}

.room-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.room-tag {
  font-size: 0.6rem;
}

.room-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 0.375rem;
}

.room-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.meta-item {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.policy-tag {
  font-size: 0.6rem;
}

/* Dialog form */
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
</style>
