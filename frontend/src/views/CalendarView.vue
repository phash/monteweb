<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useCalendarStore } from '@/stores/calendar'
import { useAuthStore } from '@/stores/auth'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'

const { t } = useI18n()
const router = useRouter()
const calendar = useCalendarStore()
const auth = useAuthStore()

const currentMonth = ref(new Date())

const monthLabel = computed(() => {
  return currentMonth.value.toLocaleDateString('de-DE', { month: 'long', year: 'numeric' })
})

const fromDate = computed(() => {
  const d = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), 1)
  return formatDateToISO(d)
})

const toDate = computed(() => {
  const d = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 0)
  return formatDateToISO(d)
})

function formatDateToISO(d: Date): string {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

onMounted(() => {
  loadEvents()
})

async function loadEvents() {
  await calendar.fetchEvents(fromDate.value, toDate.value)
}

function prevMonth() {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() - 1, 1)
  loadEvents()
}

function nextMonth() {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 1)
  loadEvents()
}

function goToday() {
  currentMonth.value = new Date()
  loadEvents()
}

function formatEventDate(event: { allDay: boolean; startDate: string; startTime: string | null; endDate: string; endTime: string | null }) {
  const start = new Date(event.startDate)
  const startStr = start.toLocaleDateString('de-DE', { weekday: 'short', day: '2-digit', month: '2-digit' })

  if (event.allDay) {
    if (event.startDate === event.endDate) return startStr
    const end = new Date(event.endDate)
    return `${startStr} - ${end.toLocaleDateString('de-DE', { weekday: 'short', day: '2-digit', month: '2-digit' })}`
  }

  const time = event.startTime ? ` ${event.startTime.substring(0, 5)}` : ''
  const endTime = event.endTime ? ` - ${event.endTime.substring(0, 5)}` : ''
  return `${startStr}${time}${endTime}`
}

function scopeSeverity(scope: string): 'info' | 'success' | 'warn' | 'secondary' {
  switch (scope) {
    case 'SCHOOL': return 'warn'
    case 'SECTION': return 'info'
    case 'ROOM': return 'success'
    default: return 'secondary'
  }
}
</script>

<template>
  <div>
    <div class="calendar-header">
      <PageTitle :title="t('calendar.title')" />
      <Button
        v-if="auth.isTeacher || auth.isAdmin"
        :label="t('calendar.createEvent')"
        icon="pi pi-plus"
        @click="router.push({ name: 'calendar-create' })"
      />
    </div>

    <div class="month-nav card">
      <Button icon="pi pi-chevron-left" text @click="prevMonth" />
      <Button :label="t('calendar.today')" text size="small" @click="goToday" />
      <span class="month-label">{{ monthLabel }}</span>
      <Button icon="pi pi-chevron-right" text @click="nextMonth" />
    </div>

    <LoadingSpinner v-if="calendar.loading && !calendar.events.length" />
    <EmptyState
      v-else-if="!calendar.events.length"
      icon="pi pi-calendar"
      :message="t('calendar.noEvents')"
    />
    <div v-else class="event-list">
      <div
        v-for="event in calendar.events"
        :key="event.id"
        class="event-item card"
        :class="{ cancelled: event.cancelled }"
        @click="router.push({ name: 'event-detail', params: { id: event.id } })"
      >
        <div class="event-date-col">
          <span class="event-date-text">{{ formatEventDate(event) }}</span>
        </div>
        <div class="event-info">
          <div class="event-title-row">
            <strong>{{ event.title }}</strong>
            <Tag v-if="event.cancelled" :value="t('calendar.cancelled')" severity="danger" size="small" />
            <Tag :value="t(`calendar.scopes.${event.scope}`)" :severity="scopeSeverity(event.scope)" size="small" />
            <Tag v-if="event.linkedJobCount > 0" :value="t('jobboard.jobCount', { n: event.linkedJobCount })" severity="secondary" size="small" icon="pi pi-briefcase" />
          </div>
          <div class="event-meta">
            <span v-if="event.scopeName">{{ event.scopeName }}</span>
            <span v-if="event.location" class="separator">·</span>
            <span v-if="event.location"><i class="pi pi-map-marker" /> {{ event.location }}</span>
            <span v-if="event.attendingCount > 0" class="separator">·</span>
            <span v-if="event.attendingCount > 0">{{ t('calendar.attendingCount', { n: event.attendingCount }) }}</span>
          </div>
        </div>
        <i class="pi pi-chevron-right event-arrow" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.month-nav {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  margin-bottom: 1rem;
}

.month-label {
  flex: 1;
  text-align: center;
  font-weight: 600;
  font-size: var(--mw-font-size-md);
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.event-item {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: background 0.15s;
  gap: 1rem;
}

.event-item:hover {
  background: var(--mw-bg-hover);
}

.event-item.cancelled {
  opacity: 0.6;
}

.event-date-col {
  min-width: 140px;
  flex-shrink: 0;
}

.event-date-text {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-primary);
  font-weight: 600;
}

.event-info {
  flex: 1;
  min-width: 0;
}

.event-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.event-meta {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.25rem;
}

.separator {
  margin: 0 0.25rem;
}

.event-arrow {
  color: var(--mw-text-muted);
  font-size: 0.75rem;
}
</style>
