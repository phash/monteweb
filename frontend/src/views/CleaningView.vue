<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useCleaningStore } from '@/stores/cleaning'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
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
import { useAdminStore } from '@/stores/admin'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const router = useRouter()
const { formatDate: formatLocaleDate } = useLocaleDate()
const auth = useAuthStore()
const admin = useAdminStore()
const cleaningStore = useCleaningStore()
const toast = useToast()
const activeTab = ref('0')
const confirmingId = ref<string | null>(null)
const rejectingId = ref<string | null>(null)
const pendingLoaded = ref(false)

const canManageCleaning = auth.isAdmin || auth.isPutzOrga || auth.isSectionAdmin
const showPendingTab = computed(() =>
  canManageCleaning && admin.config?.requireAssignmentConfirmation !== false
)

watch(activeTab, (val) => {
  if (val === 'pending' && !pendingLoaded.value) {
    pendingLoaded.value = true
    cleaningStore.fetchPendingConfirmations()
  }
})

async function handleConfirmRegistration(registrationId: string) {
  confirmingId.value = registrationId
  try {
    await cleaningStore.confirmRegistration(registrationId)
    toast.add({ severity: 'success', summary: t('cleaning.hoursConfirmed'), life: 3000 })
  } finally {
    confirmingId.value = null
  }
}

async function handleRejectRegistration(registrationId: string) {
  rejectingId.value = registrationId
  try {
    await cleaningStore.rejectRegistration(registrationId)
    toast.add({ severity: 'warn', summary: t('cleaning.hoursRejected'), life: 3000 })
  } finally {
    rejectingId.value = null
  }
}

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
    <div class="cleaning-header">
      <PageTitle :title="t('cleaning.title')" />
      <Button
        v-if="canManageCleaning"
        :label="t('cleaning.manage')"
        icon="pi pi-cog"
        severity="secondary"
        size="small"
        @click="router.push({ name: 'admin-cleaning' })"
      />
    </div>

    <Tabs v-model:value="activeTab">
      <TabList>
        <Tab value="0">{{ t('cleaning.upcomingSlots') }}</Tab>
        <Tab value="1">{{ t('cleaning.mySlots') }}</Tab>
        <Tab v-if="showPendingTab" value="pending">
          {{ t('cleaning.pendingTab') }}
          <span v-if="cleaningStore.pendingConfirmations.length" class="pending-badge">{{ cleaningStore.pendingConfirmations.length }}</span>
        </Tab>
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

        <!-- Pending Confirmations -->
        <TabPanel v-if="showPendingTab" value="pending">
          <LoadingSpinner v-if="!pendingLoaded" />
          <EmptyState
            v-else-if="!cleaningStore.pendingConfirmations.length"
            icon="pi pi-check-circle"
            :message="t('cleaning.noPendingConfirmations')"
          />
          <div v-else class="slot-list">
            <div
              v-for="reg in cleaningStore.pendingConfirmations"
              :key="reg.id"
              class="slot-card card"
            >
              <div class="slot-header">
                <h3 class="slot-title">{{ reg.userName }}</h3>
              </div>
              <div class="slot-meta">
                <span v-if="reg.actualMinutes"><i class="pi pi-clock" /> {{ Math.round(reg.actualMinutes / 60 * 10) / 10 }}h</span>
              </div>
              <div class="pending-actions">
                <Button
                  :label="t('jobboard.reject')"
                  icon="pi pi-times"
                  size="small"
                  severity="danger"
                  outlined
                  :loading="rejectingId === reg.id"
                  @click="handleRejectRegistration(reg.id)"
                />
                <Button
                  :label="t('common.confirm')"
                  icon="pi pi-check"
                  size="small"
                  :loading="confirmingId === reg.id"
                  @click="handleConfirmRegistration(reg.id)"
                />
              </div>
            </div>
          </div>
        </TabPanel>
      </TabPanels>
    </Tabs>
  </div>
</template>

<style scoped>
.cleaning-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

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

.pending-badge {
  background: var(--p-orange-500);
  color: white;
  border-radius: 999px;
  font-size: var(--mw-font-size-xs);
  padding: 0.1rem 0.45rem;
  margin-left: 0.35rem;
  font-weight: 600;
}

.pending-actions {
  margin-top: 0.5rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
