<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useProfileFieldsStore } from '@/stores/profilefields'
import { useToast } from 'primevue/usetoast'
import { useConfirmDialog } from '@/composables/useConfirmDialog'
import PageTitle from '@/components/common/PageTitle.vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import ToggleSwitch from 'primevue/toggleswitch'
import Tag from 'primevue/tag'
import type { ProfileFieldDefinition } from '@/types/profilefields'

const { t, locale } = useI18n()
const store = useProfileFieldsStore()
const toast = useToast()
const { visible: confirmVisible, header: confirmHeader, message: confirmMessage, confirm, onConfirm, onCancel } = useConfirmDialog()

const showDialog = ref(false)
const editingField = ref<ProfileFieldDefinition | null>(null)

const fieldTypeOptions = [
  { label: t('profileFields.fieldTypes.TEXT'), value: 'TEXT' },
  { label: t('profileFields.fieldTypes.DATE'), value: 'DATE' },
  { label: t('profileFields.fieldTypes.SELECT'), value: 'SELECT' },
  { label: t('profileFields.fieldTypes.BOOLEAN'), value: 'BOOLEAN' },
]

const form = ref({
  fieldKey: '',
  labelDe: '',
  labelEn: '',
  fieldType: 'TEXT',
  optionsText: '',
  required: false,
  position: 0,
  active: true,
})

onMounted(() => {
  store.fetchAllDefinitions()
})

function openCreate() {
  editingField.value = null
  form.value = {
    fieldKey: '',
    labelDe: '',
    labelEn: '',
    fieldType: 'TEXT',
    optionsText: '',
    required: false,
    position: store.allDefinitions.length,
    active: true,
  }
  showDialog.value = true
}

function openEdit(field: ProfileFieldDefinition) {
  editingField.value = field
  form.value = {
    fieldKey: field.fieldKey,
    labelDe: field.labelDe,
    labelEn: field.labelEn,
    fieldType: field.fieldType,
    optionsText: field.options?.join(', ') ?? '',
    required: field.required,
    position: field.position,
    active: true,
  }
  showDialog.value = true
}

function getFieldLabel(field: ProfileFieldDefinition): string {
  return locale.value === 'en' ? field.labelEn : field.labelDe
}

async function saveField() {
  const options = form.value.fieldType === 'SELECT' && form.value.optionsText.trim()
    ? form.value.optionsText.split(',').map((o) => o.trim()).filter(Boolean)
    : undefined

  try {
    if (editingField.value) {
      await store.updateDefinition(editingField.value.id, {
        labelDe: form.value.labelDe,
        labelEn: form.value.labelEn,
        options: options ?? undefined,
        required: form.value.required,
        position: form.value.position,
        active: form.value.active,
      })
      toast.add({ severity: 'success', summary: t('profileFields.admin.updated'), life: 3000 })
    } else {
      await store.createDefinition({
        fieldKey: form.value.fieldKey,
        labelDe: form.value.labelDe,
        labelEn: form.value.labelEn,
        fieldType: form.value.fieldType,
        options,
        required: form.value.required,
        position: form.value.position,
      })
      toast.add({ severity: 'success', summary: t('profileFields.admin.created'), life: 3000 })
    }
    showDialog.value = false
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e?.response?.data?.message || t('error.unexpected'), life: 5000 })
  }
}

async function deleteField(field: ProfileFieldDefinition) {
  const confirmed = await confirm({
    header: t('common.delete'),
    message: t('profileFields.admin.deleteConfirm'),
  })
  if (!confirmed) return

  try {
    await store.deleteDefinition(field.id)
    toast.add({ severity: 'success', summary: t('profileFields.admin.deleted'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e?.response?.data?.message || t('error.unexpected'), life: 5000 })
  }
}
</script>

<template>
  <div>
    <PageTitle :title="t('profileFields.admin.title')" :subtitle="t('profileFields.admin.subtitle')" />

    <div class="admin-actions">
      <Button :label="t('profileFields.admin.newField')" icon="pi pi-plus" @click="openCreate" />
    </div>

    <div v-if="store.allDefinitions.length === 0 && !store.loading" class="empty-state card">
      <p>{{ t('profileFields.admin.noFields') }}</p>
    </div>

    <div v-else class="fields-list">
      <div v-for="field in store.allDefinitions" :key="field.id" class="field-card card">
        <div class="field-header">
          <div class="field-info">
            <h3>{{ getFieldLabel(field) }}</h3>
            <span class="field-key">{{ field.fieldKey }}</span>
          </div>
          <div class="field-tags">
            <Tag :value="t('profileFields.fieldTypes.' + field.fieldType)" severity="info" />
            <Tag v-if="field.required" :value="t('profileFields.admin.required')" severity="warn" />
          </div>
        </div>
        <div class="field-meta">
          <span class="meta-item">
            <i class="pi pi-sort-alt" />
            {{ t('profileFields.admin.position') }}: {{ field.position }}
          </span>
          <span v-if="field.options?.length" class="meta-item">
            <i class="pi pi-list" />
            {{ field.options.join(', ') }}
          </span>
        </div>
        <div class="field-actions">
          <Button icon="pi pi-pencil" text size="small" severity="secondary" @click="openEdit(field)" />
          <Button icon="pi pi-trash" text size="small" severity="danger" @click="deleteField(field)" />
        </div>
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <Dialog v-model:visible="showDialog" :header="editingField ? t('profileFields.admin.editField') : t('profileFields.admin.newField')" modal :style="{ width: '500px' }">
      <div class="dialog-form">
        <div class="form-field">
          <label>{{ t('profileFields.admin.fieldKey') }}</label>
          <InputText v-model="form.fieldKey" class="w-full" :disabled="!!editingField" />
          <small v-if="!editingField" class="field-hint">{{ t('profileFields.admin.fieldKeyHint') }}</small>
        </div>

        <div class="form-field">
          <label>{{ t('profileFields.admin.labelDe') }}</label>
          <InputText v-model="form.labelDe" class="w-full" />
        </div>

        <div class="form-field">
          <label>{{ t('profileFields.admin.labelEn') }}</label>
          <InputText v-model="form.labelEn" class="w-full" />
        </div>

        <div class="form-field">
          <label>{{ t('profileFields.admin.fieldType') }}</label>
          <Select v-model="form.fieldType" :options="fieldTypeOptions" optionLabel="label" optionValue="value" class="w-full" :disabled="!!editingField" />
        </div>

        <div v-if="form.fieldType === 'SELECT'" class="form-field">
          <label>{{ t('profileFields.admin.options') }}</label>
          <InputText v-model="form.optionsText" class="w-full" />
          <small class="field-hint">{{ t('profileFields.admin.optionsHint') }}</small>
        </div>

        <div class="form-row-inline">
          <div class="form-field">
            <label>{{ t('profileFields.admin.position') }}</label>
            <InputNumber v-model="form.position" class="w-full" :min="0" />
          </div>
          <div class="form-field toggle-field">
            <label>{{ t('profileFields.admin.required') }}</label>
            <ToggleSwitch v-model="form.required" />
          </div>
        </div>

        <div v-if="editingField" class="form-field toggle-field">
          <label>{{ t('profileFields.admin.active') }}</label>
          <ToggleSwitch v-model="form.active" />
        </div>
      </div>

      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showDialog = false" />
        <Button :label="t('common.save')" @click="saveField" :disabled="!form.fieldKey || !form.labelDe || !form.labelEn" />
      </template>
    </Dialog>

    <!-- Confirm Dialog -->
    <Dialog v-model:visible="confirmVisible" :header="confirmHeader" modal :style="{ width: '400px' }">
      <p>{{ confirmMessage }}</p>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="onCancel" />
        <Button :label="t('common.confirm')" severity="danger" @click="onConfirm" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.admin-actions {
  margin-bottom: 1rem;
}

.empty-state {
  text-align: center;
  color: var(--mw-text-muted);
  padding: 2rem;
}

.fields-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.field-card {
  padding: 1rem 1.25rem;
}

.field-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.field-info h3 {
  margin: 0;
  font-size: var(--mw-font-size-md);
}

.field-key {
  font-size: var(--mw-font-size-xs, 0.75rem);
  color: var(--mw-text-muted);
  font-family: monospace;
}

.field-tags {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.field-meta {
  display: flex;
  gap: 1rem;
  margin-top: 0.5rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.field-actions {
  display: flex;
  gap: 0.25rem;
  justify-content: flex-end;
  margin-top: 0.5rem;
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
  color: var(--mw-text-secondary);
}

.field-hint {
  font-size: var(--mw-font-size-xs, 0.75rem);
  color: var(--mw-text-muted);
}

.form-row-inline {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.toggle-field {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}
</style>
