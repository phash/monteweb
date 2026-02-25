<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useRouter } from 'vue-router'
import { useCalendarStore } from '@/stores/calendar'
import { useAuthStore } from '@/stores/auth'
import { useRoomsStore } from '@/stores/rooms'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'
import SelectButton from 'primevue/selectbutton'
import Tag from 'primevue/tag'
import type { CalendarEvent } from '@/types/calendar'

const props = defineProps<{ roomId: string }>()
const { t } = useI18n()
const { formatEventDate: localeFormatEventDate, formatShortDate } = useLocaleDate()
const router = useRouter()
const calendar = useCalendarStore()
const auth = useAuthStore()
const rooms = useRoomsStore()

type ViewMode = 'list' | 'month' | 'quarter' | 'year'
const viewMode = ref<ViewMode>('list')
const currentDate = ref(new Date())

const viewModeOptions = [
  { label: t('calendar.viewList'), value: 'list' },
  { label: t('calendar.viewMonth'), value: 'month' },
  { label: t('calendar.view3Months'), value: 'quarter' },
  { label: t('calendar.viewYear'), value: 'year' },
]

const isLeader = (() => {
  const member = rooms.currentRoom?.members?.find(m => m.userId === auth.user?.id)
  return member?.role === 'LEADER' || auth.isAdmin
})()

const selectedDate = ref<string | null>(null)

// Events indexed by date (YYYY-MM-DD)
const eventsByDate = computed(() => {
  const map = new Map<string, CalendarEvent[]>()
  for (const ev of calendar.events) {
    const date = ev.startDate
    if (!map.has(date)) map.set(date, [])
    map.get(date)!.push(ev)
  }
  return map
})

const selectedDayEvents = computed(() => {
  if (!selectedDate.value) return []
  return eventsByDate.value.get(selectedDate.value) || []
})

// Month grid
const monthDays = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  const first = new Date(year, month, 1)
  const startDay = (first.getDay() + 6) % 7 // Monday start
  const daysInMonth = new Date(year, month + 1, 0).getDate()

  const days: { date: string; day: number; isCurrentMonth: boolean; isToday: boolean }[] = []
  const today = new Date().toISOString().substring(0, 10)

  // Previous month filler
  const prevMonthDays = new Date(year, month, 0).getDate()
  for (let i = startDay - 1; i >= 0; i--) {
    const d = prevMonthDays - i
    const dateStr = formatDateStr(year, month - 1, d)
    days.push({ date: dateStr, day: d, isCurrentMonth: false, isToday: dateStr === today })
  }

  // Current month
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = formatDateStr(year, month, d)
    days.push({ date: dateStr, day: d, isCurrentMonth: true, isToday: dateStr === today })
  }

  // Next month filler
  const remaining = 42 - days.length
  for (let d = 1; d <= remaining; d++) {
    const dateStr = formatDateStr(year, month + 1, d)
    days.push({ date: dateStr, day: d, isCurrentMonth: false, isToday: dateStr === today })
  }

  return days
})

// Quarter (3 months) grid
const quarterMonths = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  return [0, 1, 2].map(offset => {
    const m = month + offset
    const y = year + Math.floor(m / 12)
    const mo = ((m % 12) + 12) % 12
    return { year: y, month: mo, label: new Date(y, mo, 1).toLocaleDateString('de-DE', { month: 'long', year: 'numeric' }) }
  })
})

function getMonthDays(year: number, month: number) {
  const first = new Date(year, month, 1)
  const startDay = (first.getDay() + 6) % 7
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const today = new Date().toISOString().substring(0, 10)
  const days: { date: string; day: number; isCurrentMonth: boolean; isToday: boolean }[] = []

  for (let i = 0; i < startDay; i++) days.push({ date: '', day: 0, isCurrentMonth: false, isToday: false })
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = formatDateStr(year, month, d)
    days.push({ date: dateStr, day: d, isCurrentMonth: true, isToday: dateStr === today })
  }
  return days
}

// School year months (Sep-Aug)
const schoolYearMonths = computed(() => {
  const now = currentDate.value
  const startYear = now.getMonth() >= 8 ? now.getFullYear() : now.getFullYear() - 1
  const months = []
  for (let i = 0; i < 12; i++) {
    const m = (8 + i) % 12 // Sep=8, Oct=9, ..., Aug=7
    const y = m >= 8 ? startYear : startYear + 1
    months.push({ year: y, month: m, label: new Date(y, m, 1).toLocaleDateString('de-DE', { month: 'short' }) })
  }
  return months
})

const monthLabel = computed(() =>
  currentDate.value.toLocaleDateString('de-DE', { month: 'long', year: 'numeric' })
)

function formatDateStr(year: number, month: number, day: number): string {
  const d = new Date(year, month, day)
  return d.toISOString().substring(0, 10)
}

function navigateMonth(delta: number) {
  const d = new Date(currentDate.value)
  if (viewMode.value === 'quarter') {
    d.setMonth(d.getMonth() + delta * 3)
  } else {
    d.setMonth(d.getMonth() + delta)
  }
  currentDate.value = d
}

function selectDay(dateStr: string) {
  selectedDate.value = selectedDate.value === dateStr ? null : dateStr
}

function hasEvents(dateStr: string): boolean {
  return eventsByDate.value.has(dateStr)
}

function eventCount(dateStr: string): number {
  return eventsByDate.value.get(dateStr)?.length || 0
}

onMounted(async () => {
  try {
    await calendar.fetchRoomEvents(props.roomId)
  } catch {
    // Calendar events not available
  }
})

function formatEventDate(event: { allDay: boolean; startDate: string; startTime: string | null; endDate: string }) {
  const startStr = localeFormatEventDate(event.startDate)
  if (event.allDay) {
    if (event.startDate === event.endDate) return startStr
    return `${startStr} - ${localeFormatEventDate(event.endDate)}`
  }
  const time = event.startTime ? ` ${event.startTime.substring(0, 5)}` : ''
  return `${startStr}${time}`
}

const weekdays = ['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So']
</script>

<template>
  <div>
    <div class="events-header">
      <SelectButton
        v-model="viewMode"
        :options="viewModeOptions"
        optionLabel="label"
        optionValue="value"
        class="view-switcher"
      />
      <Button
        v-if="isLeader"
        :label="t('calendar.createEvent')"
        icon="pi pi-plus"
        size="small"
        @click="router.push({ name: 'calendar-create', query: { roomId } })"
      />
    </div>

    <!-- Navigation for month/quarter views -->
    <div v-if="viewMode !== 'list' && viewMode !== 'year'" class="month-nav">
      <Button icon="pi pi-chevron-left" text size="small" @click="navigateMonth(-1)" />
      <strong class="month-label">{{ monthLabel }}</strong>
      <Button icon="pi pi-chevron-right" text size="small" @click="navigateMonth(1)" />
    </div>

    <LoadingSpinner v-if="calendar.loading && !calendar.events.length" />
    <EmptyState
      v-else-if="viewMode === 'list' && !calendar.events.length"
      icon="pi pi-calendar"
      :message="t('calendar.noEvents')"
    />

    <!-- List view -->
    <div v-else-if="viewMode === 'list'" class="event-list">
      <div
        v-for="event in calendar.events"
        :key="event.id"
        class="event-item card"
        :class="{ cancelled: event.cancelled }"
        @click="router.push({ name: 'event-detail', params: { id: event.id } })"
      >
        <div class="event-info">
          <div class="event-title-row">
            <strong>{{ event.title }}</strong>
            <Tag v-if="event.cancelled" :value="t('calendar.cancelled')" severity="danger" size="small" />
          </div>
          <div class="event-meta">
            <span>{{ formatEventDate(event) }}</span>
            <span v-if="event.location" class="separator">·</span>
            <span v-if="event.location">{{ event.location }}</span>
            <span v-if="event.attendingCount > 0" class="separator">·</span>
            <span v-if="event.attendingCount > 0">{{ t('calendar.attendingCount', { n: event.attendingCount }) }}</span>
          </div>
        </div>
        <i class="pi pi-chevron-right event-arrow" />
      </div>
    </div>

    <!-- Month view -->
    <template v-else-if="viewMode === 'month'">
      <div class="cal-grid">
        <div v-for="day in weekdays" :key="day" class="cal-weekday">{{ day }}</div>
        <div
          v-for="(d, i) in monthDays"
          :key="i"
          class="cal-day"
          :class="{
            'other-month': !d.isCurrentMonth,
            'is-today': d.isToday,
            'has-events': hasEvents(d.date),
            'selected': selectedDate === d.date
          }"
          @click="d.date && selectDay(d.date)"
        >
          <span class="day-number">{{ d.day }}</span>
          <span v-if="eventCount(d.date) > 0" class="event-dot">{{ eventCount(d.date) }}</span>
        </div>
      </div>

      <!-- Selected day events -->
      <div v-if="selectedDate && selectedDayEvents.length" class="selected-day-events">
        <h4>{{ formatShortDate(selectedDate) }}</h4>
        <div
          v-for="event in selectedDayEvents"
          :key="event.id"
          class="event-item card compact"
          @click="router.push({ name: 'event-detail', params: { id: event.id } })"
        >
          <div class="event-info">
            <strong>{{ event.title }}</strong>
            <div class="event-meta">
              <span>{{ formatEventDate(event) }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Quarter (3 month) view -->
    <template v-else-if="viewMode === 'quarter'">
      <div class="quarter-grid">
        <div v-for="qm in quarterMonths" :key="`${qm.year}-${qm.month}`" class="quarter-month">
          <h4 class="quarter-month-label">{{ qm.label }}</h4>
          <div class="cal-grid mini">
            <div v-for="day in weekdays" :key="day" class="cal-weekday mini">{{ day.charAt(0) }}</div>
            <div
              v-for="(d, i) in getMonthDays(qm.year, qm.month)"
              :key="i"
              class="cal-day mini"
              :class="{
                'other-month': !d.isCurrentMonth,
                'is-today': d.isToday,
                'has-events': hasEvents(d.date),
                'selected': selectedDate === d.date
              }"
              @click="d.date && selectDay(d.date)"
            >
              <span v-if="d.day" class="day-number">{{ d.day }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="selectedDate && selectedDayEvents.length" class="selected-day-events">
        <h4>{{ formatShortDate(selectedDate) }}</h4>
        <div
          v-for="event in selectedDayEvents"
          :key="event.id"
          class="event-item card compact"
          @click="router.push({ name: 'event-detail', params: { id: event.id } })"
        >
          <div class="event-info">
            <strong>{{ event.title }}</strong>
            <div class="event-meta">{{ formatEventDate(event) }}</div>
          </div>
        </div>
      </div>
    </template>

    <!-- School year view -->
    <template v-else-if="viewMode === 'year'">
      <div class="year-grid">
        <div v-for="ym in schoolYearMonths" :key="`${ym.year}-${ym.month}`" class="year-month">
          <h4 class="year-month-label">{{ ym.label }}</h4>
          <div class="cal-grid mini">
            <div
              v-for="(d, i) in getMonthDays(ym.year, ym.month)"
              :key="i"
              class="cal-day mini tiny"
              :class="{
                'other-month': !d.isCurrentMonth,
                'is-today': d.isToday,
                'has-events': hasEvents(d.date),
                'selected': selectedDate === d.date
              }"
              @click="d.date && selectDay(d.date)"
            >
              <span v-if="d.day" class="day-number">{{ d.day }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="selectedDate && selectedDayEvents.length" class="selected-day-events">
        <h4>{{ formatShortDate(selectedDate) }}</h4>
        <div
          v-for="event in selectedDayEvents"
          :key="event.id"
          class="event-item card compact"
          @click="router.push({ name: 'event-detail', params: { id: event.id } })"
        >
          <div class="event-info">
            <strong>{{ event.title }}</strong>
            <div class="event-meta">{{ formatEventDate(event) }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.events-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.view-switcher {
  font-size: var(--mw-font-size-xs);
}

.month-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.month-label {
  min-width: 10rem;
  text-align: center;
  font-size: var(--mw-font-size-base);
}

/* Event list */
.event-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.event-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: background 0.15s;
}

.event-item:hover {
  background: var(--mw-bg-hover);
}

.event-item.cancelled {
  opacity: 0.6;
}

.event-item.compact {
  padding: 0.5rem 0.75rem;
}

.event-info {
  flex: 1;
  min-width: 0;
}

.event-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
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

/* Calendar grid */
.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 1px;
  background: var(--mw-border-light);
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius);
  overflow: hidden;
}

.cal-weekday {
  text-align: center;
  font-size: var(--mw-font-size-xs);
  font-weight: 600;
  color: var(--mw-text-muted);
  padding: 0.5rem 0.25rem;
  background: var(--mw-bg-card);
}

.cal-weekday.mini {
  padding: 0.25rem 0.125rem;
  font-size: 0.5625rem;
}

.cal-day {
  background: var(--mw-bg-card);
  padding: 0.5rem;
  min-height: 3rem;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}

.cal-day:hover {
  background: var(--mw-bg-hover);
}

.cal-day.mini {
  min-height: 1.75rem;
  padding: 0.25rem;
  text-align: center;
}

.cal-day.mini.tiny {
  min-height: 1.25rem;
  padding: 0.125rem;
  font-size: 0.625rem;
}

.cal-day.other-month {
  opacity: 0.35;
}

.cal-day.is-today .day-number {
  background: var(--mw-primary);
  color: white;
  border-radius: 50%;
  width: 1.5rem;
  height: 1.5rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.cal-day.mini.is-today .day-number {
  width: 1.25rem;
  height: 1.25rem;
  font-size: 0.625rem;
}

.cal-day.has-events {
  font-weight: 600;
}

.cal-day.selected {
  background: color-mix(in srgb, var(--mw-primary) 12%, var(--mw-bg-card));
  outline: 2px solid var(--mw-primary);
  outline-offset: -2px;
}

.day-number {
  font-size: var(--mw-font-size-xs);
}

.event-dot {
  position: absolute;
  bottom: 0.25rem;
  right: 0.25rem;
  background: var(--mw-primary);
  color: white;
  font-size: 0.5rem;
  font-weight: 700;
  width: 1rem;
  height: 1rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Selected day events */
.selected-day-events {
  margin-top: 1rem;
}

.selected-day-events h4 {
  margin: 0 0 0.5rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
}

/* Quarter grid */
.quarter-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

.quarter-month-label {
  margin: 0 0 0.5rem;
  font-size: var(--mw-font-size-sm);
  text-align: center;
}

/* Year grid */
.year-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
}

.year-month-label {
  margin: 0 0 0.25rem;
  font-size: var(--mw-font-size-xs);
  text-align: center;
  color: var(--mw-text-secondary);
}

@media (max-width: 767px) {
  .quarter-grid {
    grid-template-columns: 1fr;
  }

  .year-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .events-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
