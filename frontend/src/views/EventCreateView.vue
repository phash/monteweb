<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useCalendarStore } from '@/stores/calendar'
import { useRoomsStore } from '@/stores/rooms'
import { useAuthStore } from '@/stores/auth'
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
const saving = ref(false)

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
      startTime: allDay.value ? undefined : startTime.value || undefined,
      endDate: formatDateToISO(endDate.value),
      endTime: allDay.value ? undefined : endTime.value || undefined,
      scope: scope.value,
      scopeId: scope.value === 'SCHOOL' ? undefined : scopeId.value,
      recurrence: recurrence.value,
      recurrenceEnd: recurrenceEnd.value ? formatDateToISO(recurrenceEnd.value) : undefined,
    }

    if (isEdit.value && eventId.value) {
      await calendar.updateEvent(eventId.value, data)
    } else {
      await calendar.createEvent(data)
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
          <label for="event-title">{{ t('calendar.eventTitle') }} *</label>
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
            <label>{{ t('calendar.startDate') }} *</label>
            <DatePicker v-model="startDate" dateFormat="dd.mm.yy" class="w-full" />
          </div>
          <div v-if="!allDay" class="field">
            <label>{{ t('calendar.startTime') }}</label>
            <InputText v-model="startTime" placeholder="HH:MM" class="w-full" />
          </div>
        </div>

        <div class="field-row">
          <div class="field">
            <label>{{ t('calendar.endDate') }} *</label>
            <DatePicker v-model="endDate" dateFormat="dd.mm.yy" class="w-full" />
          </div>
          <div v-if="!allDay" class="field">
            <label>{{ t('calendar.endTime') }}</label>
            <InputText v-model="endTime" placeholder="HH:MM" class="w-full" />
          </div>
        </div>

        <div class="field-row" v-if="!isEdit">
          <div class="field">
            <label>{{ t('calendar.scope') }} *</label>
            <Select
              v-model="scope"
              :options="scopeOptions"
              optionLabel="label"
              optionValue="value"
              class="w-full"
            />
          </div>
          <div v-if="scope === 'ROOM'" class="field">
            <label>{{ t('calendar.selectRoom') }}</label>
            <Select
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
            <label>{{ t('calendar.recurrence') }}</label>
            <Select
              v-model="recurrence"
              :options="recurrenceOptions.map(o => ({ label: t(`calendar.recurrences.${o.value}`), value: o.value }))"
              optionLabel="label"
              optionValue="value"
              class="w-full"
            />
          </div>
          <div v-if="recurrence !== 'NONE'" class="field">
            <label>{{ t('calendar.recurrenceEnd') }}</label>
            <DatePicker v-model="recurrenceEnd" dateFormat="dd.mm.yy" class="w-full" />
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

@media (max-width: 600px) {
  .field-row {
    flex-direction: column;
  }
}
</style>
