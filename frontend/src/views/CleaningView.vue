<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCleaningStore } from '@/stores/cleaning'
import { useI18n } from 'vue-i18n'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import ProgressBar from 'primevue/progressbar'

const { t } = useI18n()
const router = useRouter()
const cleaningStore = useCleaningStore()
const activeTab = ref('0')

onMounted(() => {
  cleaningStore.loadUpcomingSlots()
  cleaningStore.loadMySlots()
})

function statusSeverity(status: string) {
  switch (status) {
    case 'OPEN': return 'success'
    case 'FULL': return 'warn'
    case 'IN_PROGRESS': return 'info'
    case 'COMPLETED': return 'secondary'
    case 'CANCELLED': return 'danger'
    default: return 'info'
  }
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('de-DE', {
    weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric'
  })
}

function participantPercent(slot: { currentRegistrations: number; minParticipants: number }) {
  if (slot.minParticipants === 0) return 100
  return Math.min(100, Math.round((slot.currentRegistrations / slot.minParticipants) * 100))
}
</script>

<template>
  <div class="p-4">
    <h1 class="text-2xl font-bold mb-4">{{ t('cleaning.title') }}</h1>

    <Tabs v-model:value="activeTab">
      <TabList>
        <Tab value="0">{{ t('cleaning.upcomingSlots') }}</Tab>
        <Tab value="1">{{ t('cleaning.mySlots') }}</Tab>
      </TabList>

      <TabPanels>
        <TabPanel value="0">
          <div v-if="cleaningStore.loading" class="text-center p-8">
            <i class="pi pi-spin pi-spinner text-2xl"></i>
          </div>

          <div v-else-if="cleaningStore.upcomingSlots.length === 0" class="text-center p-8 text-gray-500">
            {{ t('cleaning.noSlots') }}
          </div>

          <div v-else class="flex flex-col gap-3">
            <div v-for="slot in cleaningStore.upcomingSlots" :key="slot.id"
                 class="border rounded-lg p-4 cursor-pointer hover:bg-gray-50 transition-colors"
                 @click="router.push({ name: 'cleaning-slot', params: { id: slot.id } })">
              <div class="flex justify-between items-start mb-2">
                <div>
                  <h3 class="font-semibold text-lg">{{ slot.configTitle }}</h3>
                  <p class="text-sm text-gray-500">{{ slot.sectionName }}</p>
                </div>
                <Tag :value="t('cleaning.status.' + slot.status)" :severity="statusSeverity(slot.status)" />
              </div>
              <div class="flex gap-4 text-sm text-gray-600 mb-2">
                <span><i class="pi pi-calendar mr-1"></i>{{ formatDate(slot.slotDate) }}</span>
                <span><i class="pi pi-clock mr-1"></i>{{ slot.startTime }} - {{ slot.endTime }}</span>
              </div>
              <div class="flex items-center gap-2">
                <span class="text-sm">{{ slot.currentRegistrations }}/{{ slot.minParticipants }}+</span>
                <ProgressBar :value="participantPercent(slot)" class="flex-1" style="height: 8px"
                             :showValue="false" />
              </div>
            </div>
          </div>

          <div v-if="cleaningStore.totalPages > 1" class="flex justify-center gap-2 mt-4">
            <Button :label="t('common.previous')" icon="pi pi-chevron-left" text
                    :disabled="cleaningStore.currentPage === 0"
                    @click="cleaningStore.loadUpcomingSlots(cleaningStore.currentPage - 1)" />
            <Button :label="t('common.next')" icon="pi pi-chevron-right" iconPos="right" text
                    :disabled="cleaningStore.currentPage >= cleaningStore.totalPages - 1"
                    @click="cleaningStore.loadUpcomingSlots(cleaningStore.currentPage + 1)" />
          </div>
        </TabPanel>

        <TabPanel value="1">
          <div v-if="cleaningStore.loading" class="text-center p-8">
            <i class="pi pi-spin pi-spinner text-2xl"></i>
          </div>

          <div v-else-if="cleaningStore.mySlots.length === 0" class="text-center p-8 text-gray-500">
            {{ t('cleaning.noMySlots') }}
          </div>

          <div v-else class="flex flex-col gap-3">
            <div v-for="slot in cleaningStore.mySlots" :key="slot.id"
                 class="border rounded-lg p-4 cursor-pointer hover:bg-gray-50 transition-colors"
                 @click="router.push({ name: 'cleaning-slot', params: { id: slot.id } })">
              <div class="flex justify-between items-start mb-2">
                <div>
                  <h3 class="font-semibold">{{ slot.configTitle }}</h3>
                  <p class="text-sm text-gray-500">{{ slot.sectionName }}</p>
                </div>
                <Tag :value="t('cleaning.status.' + slot.status)" :severity="statusSeverity(slot.status)" />
              </div>
              <div class="flex gap-4 text-sm text-gray-600">
                <span><i class="pi pi-calendar mr-1"></i>{{ formatDate(slot.slotDate) }}</span>
                <span><i class="pi pi-clock mr-1"></i>{{ slot.startTime }} - {{ slot.endTime }}</span>
              </div>
            </div>
          </div>
        </TabPanel>
      </TabPanels>
    </Tabs>
  </div>
</template>
