<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useCalendarStore } from '@/stores/calendar'
import { useAuthStore } from '@/stores/auth'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { calendarApi } from '@/api/calendar.api'
import type { CalendarEvent } from '@/types/calendar'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Checkbox from 'primevue/checkbox'
import SelectButton from 'primevue/selectbutton'

type ViewMode = 'agenda' | 'month' | 'quarter' | 'year'

const { t } = useI18n()
const { formatMonthYear, formatEventDate: formatEventDateLocale } = useLocaleDate()
const router = useRouter()
const calendar = useCalendarStore()
const auth = useAuthStore()

const viewMode = ref<ViewMode>('agenda')
const currentMonth = ref(new Date())
const selectedDay = ref<string | null>(null)
const showCleaning = ref(true)

const viewOptions = computed(() => [
  { label: t('calendar.agenda'), value: 'agenda' },
  { label: t('calendar.month'), value: 'month' },
  { label: t('calendar.threeMonth'), value: 'quarter' },
  { label: t('calendar.year'), value: 'year' },
])

const monthLabel = computed(() => {
  return formatMonthYear(currentMonth.value)
})

const yearLabel = computed(() => {
  return currentMonth.value.getFullYear().toString()
})

// Date range computation based on view mode
const dateRange = computed(() => {
  const year = currentMonth.value.getFullYear()
  const month = currentMonth.value.getMonth()

  if (viewMode.value === 'year') {
    return {
      from: formatDateToISO(new Date(year, 0, 1)),
      to: formatDateToISO(new Date(year, 11, 31)),
    }
  }
  if (viewMode.value === 'quarter') {
    return {
      from: formatDateToISO(new Date(year, month - 1, 1)),
      to: formatDateToISO(new Date(year, month + 2, 0)),
    }
  }
  // month and agenda
  return {
    from: formatDateToISO(new Date(year, month, 1)),
    to: formatDateToISO(new Date(year, month + 1, 0)),
  }
})

// Filtered events (hide cleaning events when toggle is off)
const filteredEvents = computed(() => {
  if (showCleaning.value) return calendar.events
  return calendar.events.filter(e => e.eventType !== 'CLEANING')
})

// Map events by date (YYYY-MM-DD) for calendar grid
const eventsByDate = computed(() => {
  const map = new Map<string, CalendarEvent[]>()
  for (const event of filteredEvents.value) {
    const startDate = event.startDate
    const endDate = event.endDate
    // Add event to each day it spans
    const start = new Date(startDate + 'T00:00:00')
    const end = new Date(endDate + 'T00:00:00')
    const d = new Date(start)
    while (d <= end) {
      const key = formatDateToISO(d)
      if (!map.has(key)) map.set(key, [])
      map.get(key)!.push(event)
      d.setDate(d.getDate() + 1)
    }
  }
  return map
})

// Events for selected day
const selectedDayEvents = computed(() => {
  if (!selectedDay.value) return []
  return eventsByDate.value.get(selectedDay.value) || []
})

// Generate month grid data
function getMonthGrid(year: number, month: number) {
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  // Monday = 0, Sunday = 6 (ISO week)
  let startDow = firstDay.getDay() - 1
  if (startDow < 0) startDow = 6

  const days: { date: string; day: number; currentMonth: boolean; isToday: boolean }[] = []
  const today = formatDateToISO(new Date())

  // Fill in previous month days
  for (let i = startDow - 1; i >= 0; i--) {
    const d = new Date(year, month, -i)
    days.push({
      date: formatDateToISO(d),
      day: d.getDate(),
      currentMonth: false,
      isToday: formatDateToISO(d) === today,
    })
  }

  // Current month days
  for (let day = 1; day <= lastDay.getDate(); day++) {
    const d = new Date(year, month, day)
    days.push({
      date: formatDateToISO(d),
      day,
      currentMonth: true,
      isToday: formatDateToISO(d) === today,
    })
  }

  // Fill remaining cells to complete the last week
  const remainder = days.length % 7
  if (remainder > 0) {
    const fill = 7 - remainder
    for (let i = 1; i <= fill; i++) {
      const d = new Date(year, month + 1, i)
      days.push({
        date: formatDateToISO(d),
        day: i,
        currentMonth: false,
        isToday: formatDateToISO(d) === today,
      })
    }
  }

  return days
}

const currentMonthGrid = computed(() => {
  return getMonthGrid(currentMonth.value.getFullYear(), currentMonth.value.getMonth())
})

// 3-month view: previous, current, next month
const quarterMonths = computed(() => {
  const year = currentMonth.value.getFullYear()
  const month = currentMonth.value.getMonth()
  return [
    { year, month: month - 1, label: formatMonthYear(new Date(year, month - 1, 1)), grid: getMonthGrid(year, month - 1) },
    { year, month, label: formatMonthYear(new Date(year, month, 1)), grid: getMonthGrid(year, month) },
    { year, month: month + 1, label: formatMonthYear(new Date(year, month + 1, 1)), grid: getMonthGrid(year, month + 1) },
  ]
})

// Year view: all 12 months
const yearMonths = computed(() => {
  const year = currentMonth.value.getFullYear()
  return Array.from({ length: 12 }, (_, i) => ({
    year,
    month: i,
    label: new Date(year, i, 1).toLocaleDateString(undefined, { month: 'short' }),
    grid: getMonthGrid(year, i),
  }))
})

// Weekday headers (short names, Monday first)
const weekdayHeaders = computed(() => {
  const days = []
  for (let i = 1; i <= 7; i++) {
    // 2024-01-01 is a Monday
    const d = new Date(2024, 0, i)
    days.push(d.toLocaleDateString(undefined, { weekday: 'narrow' }))
  }
  return days
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

watch([viewMode, dateRange], () => {
  selectedDay.value = null
  loadEvents()
})

async function loadEvents() {
  await calendar.fetchEvents(dateRange.value.from, dateRange.value.to)
}

function prevMonth() {
  if (viewMode.value === 'year') {
    currentMonth.value = new Date(currentMonth.value.getFullYear() - 1, currentMonth.value.getMonth(), 1)
  } else if (viewMode.value === 'quarter') {
    currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() - 3, 1)
  } else {
    currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() - 1, 1)
  }
}

function nextMonth() {
  if (viewMode.value === 'year') {
    currentMonth.value = new Date(currentMonth.value.getFullYear() + 1, currentMonth.value.getMonth(), 1)
  } else if (viewMode.value === 'quarter') {
    currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 3, 1)
  } else {
    currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 1)
  }
}

function goToday() {
  currentMonth.value = new Date()
}

function selectDay(date: string) {
  if (eventsByDate.value.has(date)) {
    selectedDay.value = selectedDay.value === date ? null : date
  }
}

function goToEvent(id: string) {
  router.push({ name: 'event-detail', params: { id } })
}

function dayEvents(date: string): CalendarEvent[] {
  return eventsByDate.value.get(date) || []
}

function dayEventCount(date: string): number {
  return dayEvents(date).length
}

function eventColorStyle(event: CalendarEvent): Record<string, string> {
  const color = event.color || 'var(--mw-primary)'
  return { '--event-color': color }
}

function formatEventDateDisplay(event: { allDay: boolean; startDate: string; startTime: string | null; endDate: string; endTime: string | null }) {
  const startStr = formatEventDateLocale(event.startDate)

  if (event.allDay) {
    if (event.startDate === event.endDate) return startStr
    return `${startStr} - ${formatEventDateLocale(event.endDate)}`
  }

  const time = event.startTime ? ` ${event.startTime.substring(0, 5)}` : ''
  const endTime = event.endTime ? ` - ${event.endTime.substring(0, 5)}` : ''
  return `${startStr}${time}${endTime}`
}

async function exportCalendar() {
  try {
    const res = await calendarApi.exportCalendar(dateRange.value.from, dateRange.value.to)
    const blob = new Blob([res.data], { type: 'text/calendar' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'calendar.ics'
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    // ignore
  }
}

function scopeSeverity(scope: string): 'info' | 'success' | 'warn' | 'secondary' {
  switch (scope) {
    case 'SCHOOL': return 'warn'
    case 'SECTION': return 'info'
    case 'ROOM': return 'success'
    default: return 'secondary'
  }
}

function formatSelectedDay(date: string): string {
  const d = new Date(date + 'T00:00:00')
  return d.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
}
</script>

<template>
  <div>
    <div class="calendar-header">
      <PageTitle :title="t('calendar.title')" />
      <div class="calendar-header-actions">
        <Button
          :label="t('calendar.exportCalendar')"
          icon="pi pi-download"
          severity="secondary"
          outlined
          @click="exportCalendar"
        />
        <Button
          v-if="auth.isTeacher || auth.isAdmin"
          :label="t('calendar.createEvent')"
          icon="pi pi-plus"
          @click="router.push({ name: 'calendar-create' })"
        />
      </div>
    </div>

    <div class="view-toggle">
      <SelectButton
        v-model="viewMode"
        :options="viewOptions"
        optionLabel="label"
        optionValue="value"
        :allowEmpty="false"
      />
    </div>

    <div class="filter-toggle">
      <Checkbox v-model="showCleaning" :binary="true" inputId="showCleaning" />
      <label for="showCleaning">{{ t('calendar.showCleaning') }}</label>
    </div>

    <div class="month-nav card">
      <Button icon="pi pi-chevron-left" text :aria-label="t('common.previous')" @click="prevMonth" />
      <Button :label="t('calendar.today')" text size="small" @click="goToday" />
      <span class="month-label">{{ viewMode === 'year' ? yearLabel : monthLabel }}</span>
      <Button icon="pi pi-chevron-right" text :aria-label="t('common.next')" @click="nextMonth" />
    </div>

    <LoadingSpinner v-if="calendar.loading && !calendar.events.length" />

    <!-- AGENDA VIEW -->
    <template v-if="viewMode === 'agenda'">
      <EmptyState
        v-if="!filteredEvents.length && !calendar.loading"
        icon="pi pi-calendar"
        :message="t('calendar.noEvents')"
      />
      <div v-else class="event-list">
        <router-link
          v-for="event in filteredEvents"
          :key="event.id"
          :to="{ name: 'event-detail', params: { id: event.id } }"
          class="event-item card"
          :class="{ cancelled: event.cancelled }"
        >
          <span class="event-color-bar" :style="{ background: event.color || 'var(--mw-primary)' }" />
          <div class="event-date-col">
            <span class="event-date-text">{{ formatEventDateDisplay(event) }}</span>
          </div>
          <div class="event-info">
            <div class="event-title-row">
              <strong>{{ event.title }}</strong>
              <Tag v-if="event.cancelled" :value="t('calendar.cancelled')" severity="danger" size="small" />
              <Tag v-if="event.eventType === 'CLEANING'" :value="t('calendar.cleaning')" severity="warn" size="small" icon="pi pi-sparkles" />
              <Tag :value="t(`calendar.scopes.${event.scope}`)" :severity="scopeSeverity(event.scope)" size="small" />
              <Tag v-if="event.linkedJobCount > 0" :value="t('jobboard.jobCount', { n: event.linkedJobCount })" severity="secondary" size="small" icon="pi pi-briefcase" />
            </div>
            <div class="event-meta">
              <span v-if="event.scopeName">{{ event.scopeName }}</span>
              <span v-if="event.location" class="separator">&middot;</span>
              <span v-if="event.location"><i class="pi pi-map-marker" /> {{ event.location }}</span>
              <span v-if="event.attendingCount > 0" class="separator">&middot;</span>
              <span v-if="event.attendingCount > 0">{{ t('calendar.attendingCount', { n: event.attendingCount }) }}</span>
            </div>
          </div>
          <i class="pi pi-chevron-right event-arrow" />
        </router-link>
      </div>
    </template>

    <!-- MONTH VIEW -->
    <template v-if="viewMode === 'month'">
      <div class="month-grid card">
        <div class="weekday-header">
          <span v-for="wd in weekdayHeaders" :key="wd" class="weekday-cell">{{ wd }}</span>
        </div>
        <div class="days-grid">
          <div
            v-for="day in currentMonthGrid"
            :key="day.date"
            class="day-cell"
            :class="{
              'other-month': !day.currentMonth,
              today: day.isToday,
              'has-events': dayEventCount(day.date) > 0,
              selected: selectedDay === day.date,
            }"
            @click="selectDay(day.date)"
          >
            <span class="day-number">{{ day.day }}</span>
            <div v-if="dayEventCount(day.date) > 0" class="event-bars">
              <div
                v-for="event in dayEvents(day.date).slice(0, 2)"
                :key="event.id"
                class="event-bar"
                :style="eventColorStyle(event)"
              >
                <span class="event-bar-title">{{ event.title }}</span>
              </div>
              <span v-if="dayEventCount(day.date) > 2" class="event-bar-more">+{{ dayEventCount(day.date) - 2 }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Selected day detail -->
      <div v-if="selectedDay && selectedDayEvents.length > 0" class="selected-day-events card">
        <h3 class="selected-day-title">{{ formatSelectedDay(selectedDay) }}</h3>
        <div class="day-event-list">
          <div
            v-for="event in selectedDayEvents"
            :key="event.id"
            class="day-event-item"
            :class="{ cancelled: event.cancelled }"
            @click="goToEvent(event.id)"
          >
            <div class="day-event-time">
              <span v-if="event.allDay" class="all-day-badge">{{ t('calendar.allDay') }}</span>
              <span v-else-if="event.startTime">{{ event.startTime.substring(0, 5) }}</span>
            </div>
            <span class="event-color-dot" :style="{ background: event.color || 'var(--mw-primary)' }" />
            <div class="day-event-info">
              <strong>{{ event.title }}</strong>
              <Tag :value="t(`calendar.scopes.${event.scope}`)" :severity="scopeSeverity(event.scope)" size="small" />
              <Tag v-if="event.cancelled" :value="t('calendar.cancelled')" severity="danger" size="small" />
            </div>
            <i class="pi pi-chevron-right event-arrow" />
          </div>
        </div>
      </div>
    </template>

    <!-- 3-MONTH VIEW -->
    <template v-if="viewMode === 'quarter'">
      <div class="quarter-grid">
        <div v-for="m in quarterMonths" :key="`${m.year}-${m.month}`" class="mini-month card">
          <h4 class="mini-month-title">{{ m.label }}</h4>
          <div class="weekday-header mini">
            <span v-for="wd in weekdayHeaders" :key="wd" class="weekday-cell">{{ wd }}</span>
          </div>
          <div class="days-grid mini">
            <div
              v-for="day in m.grid"
              :key="day.date"
              class="day-cell mini"
              :class="{
                'other-month': !day.currentMonth,
                today: day.isToday,
                'has-events': dayEventCount(day.date) > 0,
                selected: selectedDay === day.date,
              }"
              @click="selectDay(day.date)"
            >
              <span class="day-number">{{ day.day }}</span>
              <span v-if="dayEventCount(day.date) > 0" class="mini-dot" :style="{ background: dayEvents(day.date)[0]?.color || 'var(--mw-primary)' }" />
            </div>
          </div>
        </div>
      </div>

      <div v-if="selectedDay && selectedDayEvents.length > 0" class="selected-day-events card">
        <h3 class="selected-day-title">{{ formatSelectedDay(selectedDay) }}</h3>
        <div class="day-event-list">
          <div
            v-for="event in selectedDayEvents"
            :key="event.id"
            class="day-event-item"
            :class="{ cancelled: event.cancelled }"
            @click="goToEvent(event.id)"
          >
            <div class="day-event-time">
              <span v-if="event.allDay" class="all-day-badge">{{ t('calendar.allDay') }}</span>
              <span v-else-if="event.startTime">{{ event.startTime.substring(0, 5) }}</span>
            </div>
            <span class="event-color-dot" :style="{ background: event.color || 'var(--mw-primary)' }" />
            <div class="day-event-info">
              <strong>{{ event.title }}</strong>
              <Tag :value="t(`calendar.scopes.${event.scope}`)" :severity="scopeSeverity(event.scope)" size="small" />
            </div>
            <i class="pi pi-chevron-right event-arrow" />
          </div>
        </div>
      </div>
    </template>

    <!-- YEAR VIEW -->
    <template v-if="viewMode === 'year'">
      <div class="year-grid">
        <div v-for="m in yearMonths" :key="`${m.year}-${m.month}`" class="mini-month year-month card">
          <h4 class="mini-month-title">{{ m.label }}</h4>
          <div class="weekday-header mini">
            <span v-for="wd in weekdayHeaders" :key="wd" class="weekday-cell">{{ wd }}</span>
          </div>
          <div class="days-grid mini">
            <div
              v-for="day in m.grid"
              :key="day.date"
              class="day-cell mini year-cell"
              :class="{
                'other-month': !day.currentMonth,
                today: day.isToday,
                'has-events': dayEventCount(day.date) > 0,
                selected: selectedDay === day.date,
              }"
              @click="selectDay(day.date)"
            >
              <span class="day-number">{{ day.day }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="selectedDay && selectedDayEvents.length > 0" class="selected-day-events card">
        <h3 class="selected-day-title">{{ formatSelectedDay(selectedDay) }}</h3>
        <div class="day-event-list">
          <div
            v-for="event in selectedDayEvents"
            :key="event.id"
            class="day-event-item"
            :class="{ cancelled: event.cancelled }"
            @click="goToEvent(event.id)"
          >
            <div class="day-event-time">
              <span v-if="event.allDay" class="all-day-badge">{{ t('calendar.allDay') }}</span>
              <span v-else-if="event.startTime">{{ event.startTime.substring(0, 5) }}</span>
            </div>
            <span class="event-color-dot" :style="{ background: event.color || 'var(--mw-primary)' }" />
            <div class="day-event-info">
              <strong>{{ event.title }}</strong>
              <Tag :value="t(`calendar.scopes.${event.scope}`)" :severity="scopeSeverity(event.scope)" size="small" />
            </div>
            <i class="pi pi-chevron-right event-arrow" />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.calendar-header-actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.view-toggle {
  margin-bottom: 0.75rem;
}

.view-toggle :deep(.p-selectbutton) {
  flex-wrap: wrap;
}

.filter-toggle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  font-size: var(--mw-font-size-sm);
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

/* === AGENDA VIEW === */
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
  text-decoration: none;
  color: inherit;
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

/* === MONTH GRID === */
.month-grid {
  padding: 1rem;
  margin-bottom: 1rem;
}

.weekday-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  margin-bottom: 0.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--mw-border-light);
}

.weekday-header.mini {
  font-size: 0.65rem;
  margin-bottom: 0.25rem;
  padding-bottom: 0.25rem;
}

.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.days-grid.mini {
  gap: 1px;
}

.day-cell {
  min-height: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 8px;
  cursor: default;
  position: relative;
  transition: background 0.15s;
  padding: 0.25rem 0.15rem;
}

.day-cell.has-events {
  cursor: pointer;
}

.day-cell.has-events:hover {
  background: var(--mw-bg-hover);
}

.day-cell.other-month {
  opacity: 0.3;
}

.day-cell.today .day-number {
  background: var(--mw-primary);
  color: white;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.day-cell.selected {
  background: var(--p-blue-50, #eff6ff);
  outline: 2px solid var(--mw-primary);
  outline-offset: -2px;
}

.day-number {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
}

.event-color-bar {
  width: 4px;
  min-height: 32px;
  border-radius: 2px;
  flex-shrink: 0;
}

.event-color-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.event-bars {
  display: flex;
  flex-direction: column;
  gap: 1px;
  width: 100%;
  margin-top: 2px;
  overflow: hidden;
}

.event-bar {
  background: color-mix(in srgb, var(--event-color) 20%, transparent);
  border-left: 3px solid var(--event-color);
  border-radius: 2px;
  padding: 0 3px;
  line-height: 1.3;
}

.event-bar-title {
  font-size: 0.6rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.event-bar-more {
  font-size: 0.55rem;
  color: var(--mw-text-muted);
  text-align: center;
}

/* === MINI MONTH (3-month + year) === */
.day-cell.mini {
  aspect-ratio: 1;
  padding: 1px;
  border-radius: 4px;
}

.day-cell.mini .day-number {
  font-size: 0.7rem;
}

.day-cell.mini.today .day-number {
  width: 20px;
  height: 20px;
  font-size: 0.65rem;
}

.day-cell.mini.has-events {
  background: var(--p-blue-50, #eff6ff);
}

.day-cell.mini.has-events:hover {
  background: var(--p-blue-100, #dbeafe);
}

.mini-dot {
  position: absolute;
  bottom: 2px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
}

.mini-month {
  padding: 0.75rem;
}

.mini-month-title {
  text-align: center;
  font-size: var(--mw-font-size-sm);
  font-weight: 600;
  margin-bottom: 0.5rem;
}

/* 3-month layout */
.quarter-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
  margin-bottom: 1rem;
}

/* Year layout */
.year-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.year-month {
  padding: 0.5rem;
}

.year-month .mini-month-title {
  font-size: 0.75rem;
}

.day-cell.year-cell {
  padding: 0;
}

.day-cell.year-cell .day-number {
  font-size: 0.6rem;
}

.day-cell.year-cell.today .day-number {
  width: 16px;
  height: 16px;
  font-size: 0.55rem;
}

.day-cell.year-cell.has-events {
  background: var(--p-blue-50, #eff6ff);
}

/* === SELECTED DAY EVENTS === */
.selected-day-events {
  padding: 1rem;
  margin-bottom: 1rem;
}

.selected-day-title {
  font-size: var(--mw-font-size-md);
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--mw-border-light);
}

.day-event-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.day-event-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.day-event-item:hover {
  background: var(--mw-bg-hover);
}

.day-event-item.cancelled {
  opacity: 0.6;
}

.day-event-time {
  min-width: 50px;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-primary);
  font-weight: 600;
  text-align: center;
}

.all-day-badge {
  font-size: 0.65rem;
  background: var(--p-surface-100);
  padding: 0.15rem 0.35rem;
  border-radius: 3px;
  white-space: nowrap;
}

.day-event-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* === RESPONSIVE === */
@media (max-width: 767px) {
  .quarter-grid {
    grid-template-columns: 1fr;
  }

  .year-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .event-date-col {
    min-width: 100px;
  }

  .day-cell.today .day-number {
    width: 24px;
    height: 24px;
    font-size: 0.75rem;
  }
}

@media (max-width: 480px) {
  .year-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 768px) and (max-width: 1024px) {
  .year-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
