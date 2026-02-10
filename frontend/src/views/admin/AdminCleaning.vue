<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useCleaningStore } from '@/stores/cleaning'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import Tag from 'primevue/tag'
import { useToast } from 'primevue/usetoast'
import type { CleaningConfigInfo } from '@/types/cleaning'
import * as cleaningApi from '@/api/cleaning.api'

const { t } = useI18n()
const cleaningStore = useCleaningStore()
const toast = useToast()

const showCreateDialog = ref(false)
const showGenerateDialog = ref(false)
const selectedConfig = ref<CleaningConfigInfo | null>(null)

const dayOptions = [
  { label: t('cleaning.days.monday'), value: 1 },
  { label: t('cleaning.days.tuesday'), value: 2 },
  { label: t('cleaning.days.wednesday'), value: 3 },
  { label: t('cleaning.days.thursday'), value: 4 },
  { label: t('cleaning.days.friday'), value: 5 }
]

const newConfig = ref({
  sectionId: '',
  title: '',
  description: '',
  dayOfWeek: 1,
  startTime: '14:00',
  endTime: '16:00',
  minParticipants: 3,
  maxParticipants: 6,
  hoursCredit: 2.0
})

const generateRange = ref({
  from: null as Date | null,
  to: null as Date | null
})

onMounted(() => {
  cleaningStore.loadConfigs()
})

async function createConfig() {
  try {
    await cleaningStore.createConfig({
      ...newConfig.value,
      hoursCredit: newConfig.value.hoursCredit
    })
    showCreateDialog.value = false
    toast.add({ severity: 'success', summary: t('cleaning.configCreated'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

function openGenerate(config: CleaningConfigInfo) {
  selectedConfig.value = config
  generateRange.value = { from: null, to: null }
  showGenerateDialog.value = true
}

async function generateSlots() {
  if (!selectedConfig.value || !generateRange.value.from || !generateRange.value.to) return
  try {
    const from = generateRange.value.from!.toISOString().split('T')[0]
    const to = generateRange.value.to!.toISOString().split('T')[0]
    const slots = await cleaningStore.generateSlots(selectedConfig.value!.id, from!, to!)
    showGenerateDialog.value = false
    toast.add({
      severity: 'success',
      summary: t('cleaning.slotsGenerated', { n: slots.length }),
      life: 3000
    })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function toggleActive(config: CleaningConfigInfo) {
  try {
    await cleaningApi.updateConfig(config.id, { active: !config.active })
    await cleaningStore.loadConfigs()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

function getDayName(day: number) {
  return dayOptions.find(d => d.value === day)?.label || ''
}
</script>

<template>
  <div class="p-4">
    <div class="flex justify-between items-center mb-4">
      <h1 class="text-2xl font-bold">{{ t('cleaning.admin.title') }}</h1>
      <Button :label="t('cleaning.admin.newConfig')" icon="pi pi-plus" @click="showCreateDialog = true" />
    </div>

    <!-- Configs Table -->
    <DataTable :value="cleaningStore.configs" :loading="cleaningStore.loading" stripedRows>
      <Column field="title" :header="t('cleaning.admin.configTitle')" />
      <Column field="sectionName" :header="t('cleaning.admin.section')" />
      <Column :header="t('cleaning.admin.day')">
        <template #body="{ data }">{{ getDayName(data.dayOfWeek) }}</template>
      </Column>
      <Column :header="t('cleaning.admin.timeRange')">
        <template #body="{ data }">{{ data.startTime }} - {{ data.endTime }}</template>
      </Column>
      <Column :header="t('cleaning.admin.participants')">
        <template #body="{ data }">{{ data.minParticipants }} - {{ data.maxParticipants }}</template>
      </Column>
      <Column field="hoursCredit" :header="t('cleaning.admin.hoursCredit')" />
      <Column :header="t('cleaning.admin.status')">
        <template #body="{ data }">
          <Tag :value="data.active ? t('common.active') : t('common.inactive')"
               :severity="data.active ? 'success' : 'danger'" />
        </template>
      </Column>
      <Column :header="t('common.actions')">
        <template #body="{ data }">
          <div class="flex gap-1">
            <Button icon="pi pi-calendar-plus" text rounded size="small"
                    v-tooltip="t('cleaning.admin.generate')"
                    @click="openGenerate(data)" :disabled="!data.active" />
            <Button :icon="data.active ? 'pi pi-ban' : 'pi pi-check'"
                    text rounded size="small"
                    :severity="data.active ? 'danger' : 'success'"
                    @click="toggleActive(data)" />
          </div>
        </template>
      </Column>
    </DataTable>

    <!-- Create Config Dialog -->
    <Dialog v-model:visible="showCreateDialog" :header="t('cleaning.admin.newConfig')" modal
            style="width: 500px">
      <div class="flex flex-col gap-4">
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.configTitle') }}</label>
          <InputText v-model="newConfig.title" class="w-full" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.sectionId') }}</label>
          <InputText v-model="newConfig.sectionId" class="w-full"
                     :placeholder="t('cleaning.admin.sectionIdPlaceholder')" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.day') }}</label>
          <Select v-model="newConfig.dayOfWeek" :options="dayOptions"
                  optionLabel="label" optionValue="value" class="w-full" />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.startTime') }}</label>
            <InputText v-model="newConfig.startTime" class="w-full" />
          </div>
          <div>
            <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.endTime') }}</label>
            <InputText v-model="newConfig.endTime" class="w-full" />
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.minParticipants') }}</label>
            <InputNumber v-model="newConfig.minParticipants" :min="1" class="w-full" />
          </div>
          <div>
            <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.maxParticipants') }}</label>
            <InputNumber v-model="newConfig.maxParticipants" :min="1" class="w-full" />
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.hoursCredit') }}</label>
          <InputNumber v-model="newConfig.hoursCredit" :minFractionDigits="1" :maxFractionDigits="2"
                       :min="0.5" :step="0.5" class="w-full" />
        </div>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" text @click="showCreateDialog = false" />
        <Button :label="t('common.create')" icon="pi pi-check" @click="createConfig"
                :disabled="!newConfig.title || !newConfig.sectionId" />
      </template>
    </Dialog>

    <!-- Generate Slots Dialog -->
    <Dialog v-model:visible="showGenerateDialog" :header="t('cleaning.admin.generateTitle')" modal
            style="width: 400px">
      <p class="mb-3">{{ t('cleaning.admin.generateHint', { title: selectedConfig?.title }) }}</p>
      <div class="flex flex-col gap-3">
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.fromDate') }}</label>
          <DatePicker v-model="generateRange.from" dateFormat="dd.mm.yy" class="w-full" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('cleaning.admin.toDate') }}</label>
          <DatePicker v-model="generateRange.to" dateFormat="dd.mm.yy" class="w-full" />
        </div>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" text @click="showGenerateDialog = false" />
        <Button :label="t('cleaning.admin.generate')" icon="pi pi-calendar-plus" @click="generateSlots"
                :disabled="!generateRange.from || !generateRange.to" />
      </template>
    </Dialog>
  </div>
</template>
