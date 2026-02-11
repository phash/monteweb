<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { sectionsApi } from '@/api/sections.api'
import { roomsApi } from '@/api/rooms.api'
import type { SchoolSectionInfo } from '@/types/family'
import type { RoomInfo } from '@/types/room'
import PageTitle from '@/components/common/PageTitle.vue'
import Accordion from 'primevue/accordion'
import AccordionPanel from 'primevue/accordionpanel'
import AccordionHeader from 'primevue/accordionheader'
import AccordionContent from 'primevue/accordioncontent'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'

const { t } = useI18n()
const router = useRouter()
const toast = useToast()

const sections = ref<SchoolSectionInfo[]>([])
const rooms = ref<RoomInfo[]>([])
const loading = ref(false)

// Dialog state
const showDialog = ref(false)
const editingSection = ref<SchoolSectionInfo | null>(null)
const form = ref({ name: '', description: '', sortOrder: 0 })

// Delete confirmation
const confirmDeleteSection = ref<SchoolSectionInfo | null>(null)

// Group rooms by sectionId
const roomsBySection = computed(() => {
  const map = new Map<string | null, RoomInfo[]>()
  for (const room of rooms.value) {
    const key = room.sectionId
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(room)
  }
  return map
})

const unassignedRooms = computed(() => roomsBySection.value.get(null) ?? [])

function roomCountForSection(sectionId: string): number {
  return roomsBySection.value.get(sectionId)?.length ?? 0
}

async function loadData() {
  loading.value = true
  try {
    const [sectionsRes, roomsRes] = await Promise.all([
      sectionsApi.getAll(),
      roomsApi.getAll({ page: 0, size: 200 }),
    ])
    sections.value = sectionsRes.data.data
    rooms.value = roomsRes.data.data.content
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingSection.value = null
  form.value = { name: '', description: '', sortOrder: 0 }
  showDialog.value = true
}

function openEdit(section: SchoolSectionInfo) {
  editingSection.value = section
  form.value = {
    name: section.name,
    description: section.description ?? '',
    sortOrder: section.sortOrder,
  }
  showDialog.value = true
}

async function saveSection() {
  if (editingSection.value) {
    await sectionsApi.update(editingSection.value.id, form.value)
  } else {
    await sectionsApi.create(form.value)
  }
  showDialog.value = false
  toast.add({ severity: 'success', summary: t('admin.sectionSaved'), life: 3000 })
  await loadData()
}

async function deleteSection() {
  if (!confirmDeleteSection.value) return
  await sectionsApi.deactivate(confirmDeleteSection.value.id)
  confirmDeleteSection.value = null
  toast.add({ severity: 'success', summary: t('common.delete'), life: 3000 })
  await loadData()
}

function navigateToRoom(roomId: string) {
  router.push({ name: 'room-detail', params: { id: roomId } })
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="header-row">
      <PageTitle :title="t('admin.sectionsAndRooms')" />
      <Button :label="t('admin.createSection')" icon="pi pi-plus" @click="openCreate" />
    </div>

    <div v-if="loading" class="loading-state">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem" />
    </div>

    <template v-else>
      <Accordion v-if="sections.length" multiple :value="sections.map(s => s.id)">
        <AccordionPanel v-for="section in sections" :key="section.id" :value="section.id">
          <AccordionHeader>
            <div class="section-header">
              <div class="section-header-info">
                <span class="section-name">{{ section.name }}</span>
                <Tag
                  :value="t('admin.roomCount', roomCountForSection(section.id), { count: roomCountForSection(section.id) })"
                  severity="secondary"
                  class="room-count-tag"
                />
              </div>
              <div class="section-actions" @click.stop>
                <Button
                  icon="pi pi-pencil"
                  text
                  size="small"
                  :aria-label="t('admin.editSection')"
                  @click="openEdit(section)"
                />
                <Button
                  icon="pi pi-trash"
                  text
                  size="small"
                  severity="danger"
                  :aria-label="t('admin.deleteSection')"
                  @click="confirmDeleteSection = section"
                />
              </div>
            </div>
          </AccordionHeader>
          <AccordionContent>
            <div v-if="roomCountForSection(section.id) === 0" class="no-rooms">
              {{ t('common.noData') }}
            </div>
            <div v-else class="room-list">
              <div
                v-for="room in roomsBySection.get(section.id)"
                :key="room.id"
                class="room-item"
                tabindex="0"
                role="link"
                @click="navigateToRoom(room.id)"
                @keydown.enter="navigateToRoom(room.id)"
              >
                <span class="room-name">{{ room.name }}</span>
                <Tag :value="t(`rooms.types.${room.type}`)" severity="info" />
                <span class="room-members">{{ room.memberCount }} {{ t('rooms.members') }}</span>
              </div>
            </div>
          </AccordionContent>
        </AccordionPanel>
      </Accordion>

      <!-- Unassigned rooms -->
      <div v-if="unassignedRooms.length" class="unassigned-section">
        <div class="unassigned-header">{{ t('admin.noSectionRooms') }}</div>
        <div class="room-list">
          <div
            v-for="room in unassignedRooms"
            :key="room.id"
            class="room-item"
            tabindex="0"
            role="link"
            @click="navigateToRoom(room.id)"
            @keydown.enter="navigateToRoom(room.id)"
          >
            <span class="room-name">{{ room.name }}</span>
            <Tag :value="t(`rooms.types.${room.type}`)" severity="info" />
            <span class="room-members">{{ room.memberCount }} {{ t('rooms.members') }}</span>
          </div>
        </div>
      </div>

      <div v-if="!sections.length && !unassignedRooms.length" class="empty-state">
        {{ t('common.noData') }}
      </div>
    </template>

    <!-- Create / Edit Dialog -->
    <Dialog
      v-model:visible="showDialog"
      :header="editingSection ? t('admin.editSectionDialog') : t('admin.sectionDialog')"
      modal
      :style="{ width: '450px', maxWidth: '90vw' }"
    >
      <form @submit.prevent="saveSection" class="create-form">
        <div class="form-field">
          <label>{{ t('admin.sectionName') }}</label>
          <InputText v-model="form.name" required class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('admin.sectionDesc') }}</label>
          <Textarea v-model="form.description" rows="2" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('admin.sectionOrder') }}</label>
          <InputNumber v-model="form.sortOrder" class="w-full" />
        </div>
      </form>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showDialog = false" />
        <Button :label="editingSection ? t('common.save') : t('common.create')" @click="saveSection" />
      </template>
    </Dialog>

    <!-- Delete Confirm Dialog -->
    <Dialog
      :visible="!!confirmDeleteSection"
      :header="t('admin.deleteSection')"
      modal
      :style="{ width: '420px', maxWidth: '90vw' }"
      @update:visible="confirmDeleteSection = null"
    >
      <p>{{ t('admin.deleteSectionConfirm', { name: confirmDeleteSection?.name }) }}</p>
      <p v-if="confirmDeleteSection && roomCountForSection(confirmDeleteSection.id) > 0" class="delete-warn">
        {{ t('admin.deleteSectionWarn') }}
      </p>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="confirmDeleteSection = null" />
        <Button :label="t('common.delete')" severity="danger" @click="deleteSection" />
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

.loading-state {
  display: flex;
  justify-content: center;
  padding: 3rem 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 0.5rem;
}

.section-header-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.section-name {
  font-weight: 600;
}

.section-actions {
  display: flex;
  gap: 0.25rem;
}

.room-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.room-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem;
  border-radius: var(--p-border-radius);
  cursor: pointer;
  transition: background-color 0.15s;
}

.room-item:hover,
.room-item:focus-visible {
  background-color: var(--p-surface-100);
}

.room-name {
  font-weight: 500;
  min-width: 0;
}

.room-members {
  margin-left: auto;
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
  white-space: nowrap;
}

.no-rooms {
  padding: 1rem 0.75rem;
  color: var(--p-text-muted-color);
  font-style: italic;
}

.unassigned-section {
  margin-top: 1.5rem;
}

.unassigned-header {
  font-weight: 600;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--p-surface-200);
  margin-bottom: 0.5rem;
  color: var(--p-text-muted-color);
}

.empty-state {
  text-align: center;
  padding: 3rem 0;
  color: var(--p-text-muted-color);
}

.create-form {
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

.delete-warn {
  color: var(--p-orange-500);
  font-size: 0.875rem;
  margin-top: 0.5rem;
}
</style>
