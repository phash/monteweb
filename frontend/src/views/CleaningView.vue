<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useCleaningStore } from '@/stores/cleaning'
import { useI18n } from 'vue-i18n'
import { useLocaleDate } from '@/composables/useLocaleDate'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import ProgressBar from 'primevue/progressbar'

const { t } = useI18n()
const { formatDate: formatLocaleDate } = useLocaleDate()
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
  return formatLocaleDate(dateStr, { weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric' })
}

function participantPercent(slot: { currentRegistrations: number; minParticipants: number }) {
  if (slot.minParticipants === 0) return 100
  return Math.min(100, Math.round((slot.currentRegistrations / slot.minParticipants) * 100))
}
</script>

<template>
  <div>
    <PageTitle :title="t('cleaning.title')" />

    <Tabs v-model:value="activeTab">
      <TabList>
        <Tab value="0">{{ t('cleaning.upcomingSlots') }}</Tab>
        <Tab value="1">{{ t('cleaning.mySlots') }}</Tab>
      </TabList>

      <TabPanels>
        <TabPanel value="0">
          <LoadingSpinner v-if="cleaningStore.loading && !cleaningStore.upcomingSlots.length" />

          <EmptyState
            v-else-if="cleaningStore.upcomingSlots.length === 0"
            icon="pi pi-calendar"
            :message="t('cleaning.noSlots')"
          />

          <div v-else class="slot-list">
            <router-link
              v-for="slot in cleaningStore.upcomingSlots"
              :key="slot.id"
              :to="{ name: 'cleaning-slot', params: { id: slot.id } }"
              class="slot-card card"
            >
              <div class="slot-header">
                <div>
                  <h3 class="slot-title">{{ slot.configTitle }}</h3>
                  <span class="slot-section">{{ slot.sectionName }}</span>
                </div>
                <Tag :value="t('cleaning.status.' + slot.status)" :severity="statusSeverity(slot.status)" />
              </div>
              <div class="slot-meta">
                <span><i class="pi pi-calendar" /> {{ formatDate(slot.slotDate) }}</span>
                <span><i class="pi pi-clock" /> {{ slot.startTime }} - {{ slot.endTime }}</span>
              </div>
              <div class="slot-progress">
                <span class="progress-label">{{ slot.currentRegistrations }}/{{ slot.minParticipants }}+</span>
                <ProgressBar :value="participantPercent(slot)" class="progress-bar" :showValue="false" />
              </div>
            </router-link>
          </div>

          <div v-if="cleaningStore.totalPages > 1" class="pagination">
            <Button
              :label="t('common.previous')"
              icon="pi pi-chevron-left"
              text
              :disabled="cleaningStore.currentPage === 0"
              @click="cleaningStore.loadUpcomingSlots(cleaningStore.currentPage - 1)"
            />
            <Button
              :label="t('common.next')"
              icon="pi pi-chevron-right"
              iconPos="right"
              text
              :disabled="cleaningStore.currentPage >= cleaningStore.totalPages - 1"
              @click="cleaningStore.loadUpcomingSlots(cleaningStore.currentPage + 1)"
            />
          </div>
        </TabPanel>

        <TabPanel value="1">
          <LoadingSpinner v-if="cleaningStore.loading && !cleaningStore.mySlots.length" />

          <EmptyState
            v-else-if="cleaningStore.mySlots.length === 0"
            icon="pi pi-check-circle"
            :message="t('cleaning.noMySlots')"
          />

          <div v-else class="slot-list">
            <router-link
              v-for="slot in cleaningStore.mySlots"
              :key="slot.id"
              :to="{ name: 'cleaning-slot', params: { id: slot.id } }"
              class="slot-card card"
            >
              <div class="slot-header">
                <div>
                  <h3 class="slot-title">{{ slot.configTitle }}</h3>
                  <span class="slot-section">{{ slot.sectionName }}</span>
                </div>
                <Tag :value="t('cleaning.status.' + slot.status)" :severity="statusSeverity(slot.status)" />
              </div>
              <div class="slot-meta">
                <span><i class="pi pi-calendar" /> {{ formatDate(slot.slotDate) }}</span>
                <span><i class="pi pi-clock" /> {{ slot.startTime }} - {{ slot.endTime }}</span>
              </div>
            </router-link>
          </div>
        </TabPanel>
      </TabPanels>
    </Tabs>
  </div>
</template>

<style scoped>
.slot-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.slot-card {
  display: block;
  text-decoration: none;
  color: inherit;
  cursor: pointer;
  transition: box-shadow 0.15s;
}

.slot-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.slot-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
}

.slot-title {
  font-size: var(--mw-font-size-md);
  font-weight: 600;
}

.slot-section {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
}

.slot-meta {
  display: flex;
  gap: 1rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  margin-bottom: 0.5rem;
}

.slot-meta i {
  margin-right: 0.25rem;
}

.slot-progress {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.progress-label {
  font-size: var(--mw-font-size-sm);
  white-space: nowrap;
}

.progress-bar {
  flex: 1;
  height: 0.5rem;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
  padding-top: 1rem;
}
</style>
