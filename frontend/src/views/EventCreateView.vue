<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useCalendarStore } from '@/stores/calendar'
import { useRoomsStore } from '@/stores/rooms'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { calendarApi } from '@/api/calendar.api'
import type { CreateEventRequest, EventScope, EventRecurrence } from '@/types/calendar'
import PageTitle from '@/components/common/PageTitle.vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import Select from 'primevue/select'
import Checkbox from 'primevue/checkbox'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const calendar = useCalendarStore()
const rooms = useRoomsStore()
const auth = useAuthStore()
const admin = useAdminStore()

const isEdit = computed(() => route.name === 'event-edit')
const eventId = computed(() => route.params.id as string)

const title = ref('')
const description = ref('')
const location = ref('')
const allDay = ref(false)
const startDate = ref<Date>(new Date())
const startTime = ref('')
const endDate = ref<Date>(new Date())
const endTime = ref('')
const scope = ref<EventScope>('ROOM')
const scopeId = ref<string | undefined>(undefined)
const recurrence = ref<EventRecurrence>('NONE')
const recurrenceEnd = ref<Date | null>(null)
const eventColor = ref<string | null>(null)
const customColor = ref('')
const addJitsi = ref(false)
const saving = ref(false)

const pastelColors = [
  '#FFB3BA', '#FFDFBA', '#FFFFBA', '#BAFFC9', '#BAE1FF',
  '#E8BAFF', '#FFB3E6', '#B3FFE6', '#FFE6B3', '#B3D9FF',
  '#D9B3FF', '#FFB3B3', '#B3FFB3', '#B3FFFF', '#FFB3FF',
  '#FFDAB3', '#C9FFB3', '#B3C9FF', '#FFB3D9', '#D9FFB3',
  '#B3FFD9', '#FFD9B3', '#B3DAFF', '#E6B3FF', '#FFE6E6',
  '#E6FFE6', '#E6E6FF', '#FFF0E6', '#E6FFF0', '#F0E6FF',
  '#FFE6F0', '#F0FFE6',
]

// Auto-adjust end date when start date moves past it
watch(startDate, (newStart) => {
  if (newStart && endDate.value && newStart > endDate.value) {
    endDate.value = new Date(newStart)
  }
})

const scopeOptions = computed(() => {
  const options = [
    { label: t('calendar.scopes.ROOM'), value: 'ROOM' },
  ]
  if (auth.isTeacher || auth.isAdmin) {
    options.push({ label: t('calendar.scopes.SECTION'), value: 'SECTION' })
  }
  if (auth.isAdmin) {
    options.push({ label: t('calendar.scopes.SCHOOL'), value: 'SCHOOL' })
  }
  return options
})

const recurrenceOptions = [
  { label: 'NONE', value: 'NONE' },
  { label: 'DAILY', value: 'DAILY' },
  { label: 'WEEKLY', value: 'WEEKLY' },
  { label: 'MONTHLY', value: 'MONTHLY' },
  { label: 'YEARLY', value: 'YEARLY' },
]

const roomOptions = computed(() =>
  rooms.myRooms.map(r => ({ label: r.name, value: r.id }))
)

const canSubmit = computed(() =>
  title.value.trim() && startDate.value && endDate.value &&
  (scope.value === 'SCHOOL' || scopeId.value)
)

onMounted(async () => {
  await rooms.fetchMyRooms()

  if (isEdit.value && eventId.value) {
    await calendar.fetchEvent(eventId.value)
    if (calendar.currentEvent) {
      const e = calendar.currentEvent
      title.value = e.title
      description.value = e.description || ''
      location.value = e.location || ''
      allDay.value = e.allDay
      startDate.value = new Date(e.startDate)
      startTime.value = e.startTime ? e.startTime.substring(0, 5) : ''
      endDate.value = new Date(e.endDate)
      endTime.value = e.endTime ? e.endTime.substring(0, 5) : ''
      scope.value = e.scope
      scopeId.value = e.scopeId || undefined
      recurrence.value = e.recurrence
      recurrenceEnd.value = e.recurrenceEnd ? new Date(e.recurrenceEnd) : null
      eventColor.value = e.color || null
      if (e.color && !pastelColors.includes(e.color)) {
        customColor.value = e.color
      }
    }
  }

  // Pre-fill from query params (from RoomEvents)
  if (route.query.roomId) {
    scope.value = 'ROOM'
    scopeId.value = route.query.roomId as string
  }
})

function formatDateToISO(d: Date): string {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatTimeWithSeconds(time: string): string {
  if (time.length === 5) return time + ':00'
  return time
}

async function handleSubmit() {
  if (!canSubmit.value) return
  saving.value = true

  try {
    const data: CreateEventRequest = {
      title: title.value.trim(),
      description: description.value.trim() || undefined,
      location: location.value.trim() || undefined,
      allDay: allDay.value,
      startDate: formatDateToISO(startDate.value),
      startTime: allDay.value ? undefined : (startTime.value ? formatTimeWithSeconds(startTime.value) : undefined),
      endDate: formatDateToISO(endDate.value),
      endTime: allDay.value ? undefined : (endTime.value ? formatTimeWithSeconds(endTime.value) : undefined),
      scope: scope.value,
      scopeId: scope.value === 'SCHOOL' ? undefined : scopeId.value,
      recurrence: recurrence.value,
      recurrenceEnd: recurrenceEnd.value ? formatDateToISO(recurrenceEnd.value) : undefined,
      color: eventColor.value || undefined,
    }

    if (isEdit.value && eventId.value) {
      await calendar.updateEvent(eventId.value, data)
    } else {
      const created = await calendar.createEvent(data)
      // Auto-generate Jitsi room if checkbox was checked
      if (addJitsi.value && created?.id) {
        try {
          await calendarApi.generateJitsiRoom(created.id)
        } catch {
          // ignore - event was created, jitsi is optional
        }
      }
    }

    router.push({ name: 'calendar' })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <Button
      icon="pi pi-arrow-left"
      :label="t('common.back')"
      severity="secondary"
      text
      @click="router.back()"
      class="mb-1"
    />

    <PageTitle :title="isEdit ? t('calendar.editEvent') : t('calendar.createEvent')" />

    <div class="form-card card">
      <div class="form-grid">
        <div class="field">
          <label for="event-title" class="required">{{ t('calendar.eventTitle') }}</label>
          <InputText id="event-title" v-model="title" :placeholder="t('calendar.titlePlaceholder')" class="w-full" />
        </div>

        <div class="field">
          <label for="event-description">{{ t('calendar.description') }}</label>
          <Textarea id="event-description" v-model="description" :placeholder="t('calendar.descriptionPlaceholder')" :autoResize="true" rows="3" class="w-full" />
        </div>

        <div class="field">
          <label for="event-location">{{ t('calendar.location') }}</label>
          <InputText id="event-location" v-model="location" :placeholder="t('calendar.locationPlaceholder')" class="w-full" />
        </div>

        <div class="field-row">
          <div class="field-check">
            <Checkbox v-model="allDay" :binary="true" inputId="allDay" />
            <label for="allDay">{{ t('calendar.allDay') }}</label>
          </div>
        </div>

        <div class="field-row">
          <div class="field">
            <label for="event-start-date" class="required">{{ t('calendar.startDate') }}</label>
            <DatePicker id="event-start-date" v-model="startDate" dateFormat="dd.mm.yy" class="w-full" />
          </div>
          <div v-if="!allDay" class="field">
            <label for="event-start-time">{{ t('calendar.startTime') }}</label>
            <InputText id="event-start-time" v-model="startTime" placeholder="HH:MM" class="w-full" />
          </div>
        </div>

        <div class="field-row">
          <div class="field">
            <label for="event-end-date" class="required">{{ t('calendar.endDate') }}</label>
            <DatePicker id="event-end-date" v-model="endDate" dateFormat="dd.mm.yy" class="w-full" />
          </div>
          <div v-if="!allDay" class="field">
            <label for="event-end-time">{{ t('calendar.endTime') }}</label>
            <InputText id="event-end-time" v-model="endTime" placeholder="HH:MM" class="w-full" />
          </div>
        </div>

        <div class="field-row" v-if="!isEdit">
          <div class="field">
            <label for="event-scope" class="required">{{ t('calendar.scope') }}</label>
            <Select
              id="event-scope"
              v-model="scope"
              :options="scopeOptions"
              optionLabel="label"
              optionValue="value"
              class="w-full"
            />
          </div>
          <div v-if="scope === 'ROOM'" class="field">
            <label for="event-room">{{ t('calendar.selectRoom') }}</label>
            <Select
              id="event-room"
              v-model="scopeId"
              :options="roomOptions"
              optionLabel="label"
              optionValue="value"
              :placeholder="t('calendar.selectRoom')"
              class="w-full"
            />
          </div>
        </div>

        <div class="field-row">
          <div class="field">
            <label for="event-recurrence">{{ t('calendar.recurrence') }}</label>
            <Select
              id="event-recurrence"
              v-model="recurrence"
              :options="recurrenceOptions.map(o => ({ label: t(`calendar.recurrences.${o.value}`), value: o.value }))"
              optionLabel="label"
              optionValue="value"
              class="w-full"
            />
          </div>
          <div v-if="recurrence !== 'NONE'" class="field">
            <label for="event-recurrence-end">{{ t('calendar.recurrenceEnd') }}</label>
            <DatePicker id="event-recurrence-end" v-model="recurrenceEnd" dateFormat="dd.mm.yy" class="w-full" />
          </div>
        </div>
      </div>

        <div v-if="admin.config?.jitsiEnabled && !isEdit" class="field-row">
          <div class="field-check">
            <Checkbox v-model="addJitsi" :binary="true" inputId="addJitsi" />
            <label for="addJitsi">{{ t('admin.jitsi.addVideoConference') }}</label>
          </div>
        </div>

        <div class="field">
          <label>{{ t('calendar.color') }}</label>
          <p class="color-hint">{{ t('calendar.colorHint') }}</p>
          <div class="color-palette">
            <button
              type="button"
              class="color-swatch no-color"
              :class="{ active: !eventColor }"
              @click="eventColor = null; customColor = ''"
              :title="t('common.none')"
            >
              <i class="pi pi-ban" />
            </button>
            <button
              v-for="c in pastelColors"
              :key="c"
              type="button"
              class="color-swatch"
              :class="{ active: eventColor === c }"
              :style="{ background: c }"
              @click="eventColor = c; customColor = ''"
            />
          </div>
          <div class="custom-color-row">
            <label for="custom-color">{{ t('calendar.customColor') }}</label>
            <div class="custom-color-input">
              <input
                id="custom-color"
                type="color"
                :value="customColor || '#cccccc'"
                @input="(e: Event) => { const v = (e.target as HTMLInputElement).value; customColor = v; eventColor = v; }"
              />
              <span v-if="customColor" class="custom-color-label">{{ customColor }}</span>
            </div>
          </div>
        </div>

      <div class="form-actions">
        <Button :label="t('common.cancel')" severity="secondary" text @click="router.back()" />
        <Button
          :label="isEdit ? t('common.save') : t('common.create')"
          :disabled="!canSubmit || saving"
          :loading="saving"
          @click="handleSubmit"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-card {
  padding: 1.5rem;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
}

.field label {
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
}

.field-row {
  display: flex;
  gap: 1rem;
}

.field-check {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1.5rem;
}

.color-hint {
  font-size: var(--mw-font-size-xs, 0.75rem);
  color: var(--mw-text-muted);
  margin: 0 0 0.5rem;
}

.color-palette {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 0.75rem;
}

.color-swatch {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: border-color 0.15s, transform 0.1s;
  padding: 0;
}

.color-swatch:hover {
  transform: scale(1.15);
}

.color-swatch.active {
  border-color: var(--mw-primary);
  box-shadow: 0 0 0 2px var(--mw-primary);
}

.color-swatch.no-color {
  background: var(--p-surface-100);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--mw-text-muted);
  font-size: 0.75rem;
}

.custom-color-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: var(--mw-font-size-sm);
}

.custom-color-row label {
  font-weight: 500;
}

.custom-color-input {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.custom-color-input input[type="color"] {
  width: 32px;
  height: 32px;
  border: 1px solid var(--mw-border-light);
  border-radius: 6px;
  cursor: pointer;
  padding: 2px;
}

.custom-color-label {
  font-family: monospace;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
}

@media (max-width: 767px) {
  .field-row {
    flex-direction: column;
  }
}
</style>
