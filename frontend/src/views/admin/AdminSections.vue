<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { sectionsApi } from '@/api/sections.api'
import type { SchoolSectionInfo } from '@/types/family'
import PageTitle from '@/components/common/PageTitle.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'

const { t } = useI18n()

const sections = ref<SchoolSectionInfo[]>([])
const loading = ref(false)
const showCreate = ref(false)

const form = ref({ name: '', description: '', sortOrder: 0 })

async function loadSections() {
  loading.value = true
  try {
    const res = await sectionsApi.getAll()
    sections.value = res.data.data
  } finally {
    loading.value = false
  }
}

async function createSection() {
  await sectionsApi.create(form.value)
  showCreate.value = false
  form.value = { name: '', description: '', sortOrder: 0 }
  await loadSections()
}

async function deactivateSection(id: string) {
  await sectionsApi.deactivate(id)
  await loadSections()
}

onMounted(loadSections)
</script>

<template>
  <div>
    <div class="header-row">
      <PageTitle :title="t('admin.sections')" />
      <Button :label="t('admin.createSection')" icon="pi pi-plus" @click="showCreate = true" />
    </div>

    <DataTable :value="sections" :loading="loading" stripedRows class="card">
      <Column field="name" :header="t('common.name')" />
      <Column field="slug" :header="t('admin.columnSlug')" />
      <Column field="sortOrder" :header="t('admin.columnOrder')" />
      <Column :header="t('common.actions')" style="width: 100px">
        <template #body="{ data }">
          <Button
            icon="pi pi-trash"
            severity="danger"
            text
            size="small"
            @click="deactivateSection(data.id)"
          />
        </template>
      </Column>
    </DataTable>

    <Dialog v-model:visible="showCreate" :header="t('admin.sectionDialog')" modal :style="{ width: '450px', maxWidth: '90vw' }">
      <form @submit.prevent="createSection" class="create-form">
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
        <Button :label="t('common.cancel')" severity="secondary" @click="showCreate = false" />
        <Button :label="t('common.create')" @click="createSection" />
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
</style>
