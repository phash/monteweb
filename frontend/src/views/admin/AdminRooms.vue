<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { roomsApi } from '@/api/rooms.api'
import type { RoomInfo, CreateRoomRequest, RoomType } from '@/types/room'
import { sectionsApi } from '@/api/sections.api'
import type { SchoolSectionInfo } from '@/types/family'
import PageTitle from '@/components/common/PageTitle.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

const { t } = useI18n()

const rooms = ref<RoomInfo[]>([])
const sections = ref<SchoolSectionInfo[]>([])
const loading = ref(false)
const showCreate = ref(false)

const form = ref<CreateRoomRequest>({
  name: '',
  type: 'KLASSE',
  description: '',
  sectionId: undefined,
})

const roomTypes: { label: string; value: RoomType }[] = [
  { label: 'Klasse', value: 'KLASSE' },
  { label: 'Gruppe', value: 'GRUPPE' },
  { label: 'Projekt', value: 'PROJEKT' },
  { label: 'Interessengruppe', value: 'INTEREST' },
  { label: 'Sonstige', value: 'CUSTOM' },
]

async function loadData() {
  loading.value = true
  try {
    const [roomsRes, sectionsRes] = await Promise.all([
      roomsApi.getAll({ page: 0, size: 100 }),
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

onMounted(loadData)
</script>

<template>
  <div>
    <div class="header-row">
      <PageTitle :title="t('admin.rooms')" />
      <Button :label="t('rooms.create')" icon="pi pi-plus" @click="showCreate = true" />
    </div>

    <DataTable :value="rooms" :loading="loading" stripedRows class="card">
      <Column field="name" :header="t('common.name')" />
      <Column field="type" :header="t('admin.columnType')">
        <template #body="{ data }">
          <Tag :value="t(`rooms.types.${data.type}`)" />
        </template>
      </Column>
      <Column field="memberCount" :header="t('admin.columnMembers')" />
    </DataTable>

    <Dialog v-model:visible="showCreate" :header="t('rooms.create')" modal style="width: 500px">
      <form @submit.prevent="createRoom" class="create-form">
        <div class="form-field">
          <label>{{ t('rooms.name') }}</label>
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
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
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

.w-full {
  width: 100%;
}
</style>
