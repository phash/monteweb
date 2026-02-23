<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCleaningStore } from '@/stores/cleaning'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import { useLocaleDate } from '@/composables/useLocaleDate'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const { formatDate: formatLocaleDate } = useLocaleDate()
const route = useRoute()
const router = useRouter()
const cleaningStore = useCleaningStore()
const authStore = useAuthStore()
const toast = useToast()

const slotId = route.params.id as string

onMounted(() => {
  cleaningStore.loadSlot(slotId)
})

const slot = computed(() => cleaningStore.currentSlot)

const isRegistered = computed(() => {
  if (!slot.value || !authStore.user) return false
  return slot.value.registrations.some(r => r.userId === authStore.user!.id)
})

const myRegistration = computed(() => {
  if (!slot.value || !authStore.user) return null
  return slot.value.registrations.find(r => r.userId === authStore.user!.id) || null
})

const canRegister = computed(() => {
  if (!slot.value) return false
  return (slot.value.status === 'OPEN' || slot.value.status === 'FULL')
    && !slot.value.cancelled
    && !isRegistered.value
    && slot.value.currentRegistrations < slot.value.maxParticipants
})

const canUnregister = computed(() => {
  return isRegistered.value && myRegistration.value && !myRegistration.value.checkedIn
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
  return formatLocaleDate(dateStr, { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' })
}

async function register() {
  try {
    await cleaningStore.registerForSlot(slotId)
    toast.add({ severity: 'success', summary: t('cleaning.registered'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function unregister() {
  try {
    await cleaningStore.unregisterFromSlot(slotId)
    toast.add({ severity: 'info', summary: t('cleaning.unregistered'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function swap() {
  try {
    await cleaningStore.offerSwap(slotId)
    toast.add({ severity: 'info', summary: t('cleaning.swapOffered'), life: 3000 })
    await cleaningStore.loadSlot(slotId)
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}


</script>

<template>
  <div>
    <Button
      :label="t('common.back')"
      icon="pi pi-arrow-left"
      text
      severity="secondary"
      class="mb-1"
      @click="router.push({ name: 'cleaning' })"
    />

    <LoadingSpinner v-if="cleaningStore.loading" />

    <template v-else-if="slot">
      <div class="slot-detail-header">
        <div>
          <PageTitle :title="slot.configTitle" />
          <p class="slot-section">{{ slot.sectionName }}</p>
        </div>
        <Tag :value="t('cleaning.status.' + slot.status)" :severity="statusSeverity(slot.status)" />
      </div>

      <!-- Info Grid -->
      <div class="info-grid">
        <div class="info-card card">
          <div class="info-label">{{ t('cleaning.date') }}</div>
          <div class="info-value">{{ formatDate(slot.slotDate) }}</div>
        </div>
        <div class="info-card card">
          <div class="info-label">{{ t('cleaning.time') }}</div>
          <div class="info-value">{{ slot.startTime }} - {{ slot.endTime }}</div>
        </div>
        <div class="info-card card">
          <div class="info-label">{{ t('cleaning.participants') }}</div>
          <div class="info-value">{{ slot.currentRegistrations }} / {{ slot.maxParticipants }}</div>
          <div class="info-hint">{{ t('cleaning.minRequired', { n: slot.minParticipants }) }}</div>
        </div>
        <div class="info-card card">
          <div class="info-label">{{ t('cleaning.slotStatus') }}</div>
          <div class="info-value">
            {{ slot.currentRegistrations >= slot.minParticipants
              ? t('cleaning.enoughParticipants') : t('cleaning.needMore') }}
          </div>
        </div>
      </div>

      <!-- Job Link -->
      <div v-if="slot.jobId" class="job-link-card card">
        <div class="job-link-content">
          <i class="pi pi-briefcase" />
          <span>{{ t('cleaning.linkedJob') }}</span>
        </div>
        <Button :label="t('cleaning.viewJob')" icon="pi pi-arrow-right" iconPos="right"
                size="small" outlined @click="router.push({ name: 'jobs' })" />
      </div>

      <!-- Action Buttons -->
      <div class="slot-actions">
        <Button v-if="canRegister" :label="t('cleaning.register')" icon="pi pi-plus" @click="register" />
        <Button v-if="canUnregister" :label="t('cleaning.unregister')" icon="pi pi-minus"
                severity="secondary" @click="unregister" />
        <Button v-if="isRegistered && canUnregister && !myRegistration?.swapOffered"
                :label="t('cleaning.offerSwap')" icon="pi pi-sync"
                severity="secondary" outlined @click="swap" />
      </div>

      <!-- Registrations Table -->
      <h2 class="registrations-title">{{ t('cleaning.registrations') }}</h2>
      <DataTable :value="slot.registrations" stripedRows>
        <Column field="userName" :header="t('common.name')" />
        <Column :header="t('cleaning.swapCol')">
          <template #body="{ data }">
            <Tag v-if="data.swapOffered" :value="t('cleaning.swapAvailable')" severity="warn" />
            <Tag v-if="data.noShow" :value="t('cleaning.noShow')" severity="danger" />
          </template>
        </Column>
      </DataTable>
    </template>

  </div>
</template>

<style scoped>
.slot-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.slot-section {
  color: var(--mw-text-muted);
  font-size: var(--mw-font-size-sm);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
  margin-bottom: 1.5rem;
}

@media (min-width: 768px) {
  .info-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.info-card {
  text-align: center;
  padding: 0.75rem;
}

.info-label {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  margin-bottom: 0.25rem;
}

.info-value {
  font-weight: 600;
}

.info-hint {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.125rem;
}

.slot-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
}

.job-link-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  background: color-mix(in srgb, var(--mw-primary) 6%, var(--mw-bg-card));
  border: 1px solid color-mix(in srgb, var(--mw-primary) 20%, transparent);
}

.job-link-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 500;
  color: var(--mw-text);
}

.job-link-content .pi {
  color: var(--mw-primary);
}

.registrations-title {
  font-size: var(--mw-font-size-lg);
  font-weight: 600;
  margin-bottom: 0.75rem;
}

</style>
