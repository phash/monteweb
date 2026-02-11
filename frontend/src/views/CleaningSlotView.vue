<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCleaningStore } from '@/stores/cleaning'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const cleaningStore = useCleaningStore()
const authStore = useAuthStore()
const toast = useToast()

const slotId = route.params.id as string
const showQrDialog = ref(false)
const scannerInput = ref('')

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

const canCheckIn = computed(() => {
  return isRegistered.value && myRegistration.value && !myRegistration.value.checkedIn
})

const canCheckOut = computed(() => {
  return isRegistered.value && myRegistration.value
    && myRegistration.value.checkedIn && !myRegistration.value.checkedOut
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
    weekday: 'long', day: '2-digit', month: 'long', year: 'numeric'
  })
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

function openQrScanner() {
  showQrDialog.value = true
  scannerInput.value = ''
}

async function submitCheckIn() {
  try {
    await cleaningStore.checkIn(slotId, scannerInput.value)
    showQrDialog.value = false
    toast.add({ severity: 'success', summary: t('cleaning.checkedIn'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Invalid QR', life: 5000 })
  }
}

async function doCheckOut() {
  try {
    await cleaningStore.checkOut(slotId)
    toast.add({ severity: 'success', summary: t('cleaning.checkedOut'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}
</script>

<template>
  <div class="p-4">
    <Button :label="t('common.back')" icon="pi pi-arrow-left" text class="mb-4"
            @click="router.push({ name: 'cleaning' })" />

    <div v-if="cleaningStore.loading" class="text-center p-8">
      <i class="pi pi-spin pi-spinner text-2xl"></i>
    </div>

    <div v-else-if="slot">
      <div class="flex justify-between items-start mb-4">
        <div>
          <h1 class="text-2xl font-bold">{{ slot.configTitle }}</h1>
          <p class="text-gray-500">{{ slot.sectionName }}</p>
        </div>
        <Tag :value="t('cleaning.status.' + slot.status)" :severity="statusSeverity(slot.status)" class="text-lg" />
      </div>

      <!-- Info Grid -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div class="border rounded-lg p-3 text-center">
          <div class="text-sm text-gray-500">{{ t('cleaning.date') }}</div>
          <div class="font-semibold">{{ formatDate(slot.slotDate) }}</div>
        </div>
        <div class="border rounded-lg p-3 text-center">
          <div class="text-sm text-gray-500">{{ t('cleaning.time') }}</div>
          <div class="font-semibold">{{ slot.startTime }} - {{ slot.endTime }}</div>
        </div>
        <div class="border rounded-lg p-3 text-center">
          <div class="text-sm text-gray-500">{{ t('cleaning.participants') }}</div>
          <div class="font-semibold">{{ slot.currentRegistrations }} / {{ slot.maxParticipants }}</div>
          <div class="text-xs text-gray-400">{{ t('cleaning.minRequired', { n: slot.minParticipants }) }}</div>
        </div>
        <div class="border rounded-lg p-3 text-center">
          <div class="text-sm text-gray-500">{{ t('cleaning.slotStatus') }}</div>
          <div class="font-semibold">
            {{ slot.currentRegistrations >= slot.minParticipants
              ? t('cleaning.enoughParticipants') : t('cleaning.needMore') }}
          </div>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="flex flex-wrap gap-2 mb-6">
        <Button v-if="canRegister" :label="t('cleaning.register')" icon="pi pi-plus" @click="register" />
        <Button v-if="canUnregister" :label="t('cleaning.unregister')" icon="pi pi-minus"
                severity="secondary" @click="unregister" />
        <Button v-if="canCheckIn" :label="t('cleaning.checkIn')" icon="pi pi-qrcode"
                severity="info" @click="openQrScanner" />
        <Button v-if="canCheckOut" :label="t('cleaning.checkOut')" icon="pi pi-sign-out"
                severity="warn" @click="doCheckOut" />
        <Button v-if="isRegistered && canUnregister && !myRegistration?.swapOffered"
                :label="t('cleaning.offerSwap')" icon="pi pi-sync"
                severity="secondary" outlined @click="swap" />
      </div>

      <!-- Registrations Table -->
      <h2 class="text-xl font-semibold mb-3">{{ t('cleaning.registrations') }}</h2>
      <DataTable :value="slot.registrations" stripedRows>
        <Column field="userName" :header="t('common.name')" />
        <Column :header="t('cleaning.checkedInCol')">
          <template #body="{ data }">
            <i :class="data.checkedIn ? 'pi pi-check text-green-600' : 'pi pi-times text-gray-400'"></i>
          </template>
        </Column>
        <Column :header="t('cleaning.checkedOutCol')">
          <template #body="{ data }">
            <i :class="data.checkedOut ? 'pi pi-check text-green-600' : 'pi pi-times text-gray-400'"></i>
          </template>
        </Column>
        <Column :header="t('cleaning.duration')">
          <template #body="{ data }">
            {{ data.actualMinutes != null ? data.actualMinutes + ' min' : '-' }}
          </template>
        </Column>
        <Column :header="t('cleaning.swapCol')">
          <template #body="{ data }">
            <Tag v-if="data.swapOffered" :value="t('cleaning.swapAvailable')" severity="warn" />
            <Tag v-if="data.noShow" :value="t('cleaning.noShow')" severity="danger" />
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- QR Check-in Dialog -->
    <Dialog v-model:visible="showQrDialog" :header="t('cleaning.qrCheckIn')" modal :style="{ width: '400px', maxWidth: '90vw' }">
      <p class="mb-3">{{ t('cleaning.qrInstructions') }}</p>
      <InputText v-model="scannerInput" :placeholder="t('cleaning.qrPlaceholder')"
                 class="w-full" autofocus @keyup.enter="submitCheckIn" />
      <template #footer>
        <Button :label="t('common.cancel')" text @click="showQrDialog = false" />
        <Button :label="t('cleaning.checkIn')" icon="pi pi-check" @click="submitCheckIn"
                :disabled="!scannerInput" />
      </template>
    </Dialog>
  </div>
</template>
