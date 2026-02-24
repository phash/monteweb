<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { calendarApi } from '@/api/calendar.api'
import type { ICalSubscription } from '@/types/calendar'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import ColorPicker from 'primevue/colorpicker'
import { useToast } from 'primevue/usetoast'
import { useConfirmDialog } from '@/composables/useConfirmDialog'

const { t } = useI18n()
const toast = useToast()
const { confirm } = useConfirmDialog()

const subscriptions = ref<ICalSubscription[]>([])
const loading = ref(false)
const showForm = ref(false)
const saving = ref(false)

const newName = ref('')
const newUrl = ref('')
const newColor = ref('6366f1')

onMounted(() => {
  loadSubscriptions()
})

async function loadSubscriptions() {
  loading.value = true
  try {
    const res = await calendarApi.getICalSubscriptions()
    subscriptions.value = res.data.data
  } finally {
    loading.value = false
  }
}

async function createSubscription() {
  if (!newName.value || !newUrl.value) return
  saving.value = true
  try {
    const color = newColor.value.startsWith('#') ? newColor.value : `#${newColor.value}`
    await calendarApi.createICalSubscription({
      name: newName.value,
      url: newUrl.value,
      color,
    })
    toast.add({ severity: 'success', summary: t('calendar.ical.created'), life: 3000 })
    newName.value = ''
    newUrl.value = ''
    newColor.value = '6366f1'
    showForm.value = false
    await loadSubscriptions()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    saving.value = false
  }
}

async function deleteSubscription(id: string) {
  const confirmed = await confirm({ header: t('calendar.ical.deleteConfirm'), message: t('calendar.ical.deleteConfirm') })
  if (!confirmed) return
  try {
    await calendarApi.deleteICalSubscription(id)
    toast.add({ severity: 'success', summary: t('calendar.ical.deleted'), life: 3000 })
    await loadSubscriptions()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function syncSubscription(id: string) {
  try {
    await calendarApi.syncICalSubscription(id)
    toast.add({ severity: 'success', summary: t('calendar.ical.synced'), life: 3000 })
    await loadSubscriptions()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}
</script>

<template>
  <div class="p-4">
    <div class="flex justify-between items-center mb-4">
      <PageTitle :title="t('calendar.ical.title')" />
      <Button
        :label="t('calendar.ical.addSubscription')"
        icon="pi pi-plus"
        @click="showForm = !showForm"
      />
    </div>

    <!-- Add Form -->
    <div v-if="showForm" class="card p-4 mb-4">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('calendar.ical.name') }}</label>
          <InputText
            v-model="newName"
            :placeholder="t('calendar.ical.namePlaceholder')"
            class="w-full"
          />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('calendar.ical.url') }}</label>
          <InputText
            v-model="newUrl"
            :placeholder="t('calendar.ical.urlPlaceholder')"
            class="w-full"
          />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('calendar.ical.color') }}</label>
          <ColorPicker v-model="newColor" />
        </div>
      </div>
      <div class="flex gap-2">
        <Button :label="t('common.save')" icon="pi pi-check" :loading="saving" @click="createSubscription" />
        <Button :label="t('common.cancel')" severity="secondary" @click="showForm = false" />
      </div>
    </div>

    <LoadingSpinner v-if="loading" />

    <EmptyState
      v-else-if="subscriptions.length === 0"
      icon="pi pi-calendar-plus"
      :message="t('calendar.ical.noSubscriptions')"
    />

    <DataTable v-else :value="subscriptions" stripedRows>
      <Column :header="t('calendar.ical.color')" style="width: 60px">
        <template #body="{ data }">
          <span
            class="color-dot"
            :style="{ background: data.color || '#6366f1' }"
          />
        </template>
      </Column>
      <Column field="name" :header="t('calendar.ical.name')" />
      <Column field="url" :header="t('calendar.ical.url')">
        <template #body="{ data }">
          <span class="url-text">{{ data.url }}</span>
        </template>
      </Column>
      <Column :header="t('calendar.ical.lastSynced')">
        <template #body="{ data }">
          {{ formatDate(data.lastSyncedAt) }}
        </template>
      </Column>
      <Column :header="t('common.actions')" style="width: 140px">
        <template #body="{ data }">
          <div class="flex gap-1">
            <Button
              icon="pi pi-refresh"
              severity="secondary"
              text
              rounded
              size="small"
              :aria-label="t('calendar.ical.sync')"
              @click="syncSubscription(data.id)"
            />
            <Button
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              size="small"
              :aria-label="t('common.delete')"
              @click="deleteSubscription(data.id)"
            />
          </div>
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style scoped>
.color-dot {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 50%;
}

.url-text {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  word-break: break-all;
}
</style>
