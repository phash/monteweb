<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useJobboardStore } from '@/stores/jobboard'
import { useAdminStore } from '@/stores/admin'
import { useCalendarStore } from '@/stores/calendar'
import PageTitle from '@/components/common/PageTitle.vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import Select from 'primevue/select'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const jobboard = useJobboardStore()
const admin = useAdminStore()
const calendar = useCalendarStore()

const title = ref('')
const description = ref('')
const category = ref('')
const location = ref('')
const estimatedHours = ref(2)
const maxAssignees = ref(1)
const scheduledDate = ref<Date | null>(null)
const scheduledTime = ref('')
const contactInfo = ref('')
const selectedEventId = ref<string | null>(null)
const submitting = ref(false)
const calendarEnabled = admin.isModuleEnabled('calendar')

onMounted(async () => {
  jobboard.fetchCategories()
  if (calendarEnabled) {
    const now = new Date()
    const from = new Date(now.getFullYear(), now.getMonth() - 1, 1)
    const to = new Date(now.getFullYear(), now.getMonth() + 6, 0)
    const formatDate = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    await calendar.fetchEvents(formatDate(from), formatDate(to))
  }
  if (route.query.eventId) {
    selectedEventId.value = route.query.eventId as string
  }
})

async function submit() {
  if (!title.value.trim() || !category.value.trim()) return
  submitting.value = true
  try {
    const job = await jobboard.createJob({
      title: title.value.trim(),
      description: description.value.trim() || undefined,
      category: category.value.trim(),
      location: location.value.trim() || undefined,
      estimatedHours: estimatedHours.value,
      maxAssignees: maxAssignees.value,
      scheduledDate: scheduledDate.value?.toISOString().split('T')[0],
      scheduledTime: scheduledTime.value.trim() || undefined,
      contactInfo: contactInfo.value.trim() || undefined,
      eventId: selectedEventId.value || undefined,
    })
    router.push({ name: 'job-detail', params: { id: job.id } })
  } finally {
    submitting.value = false
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
      @click="router.push({ name: 'jobs' })"
      class="mb-1"
    />

    <PageTitle :title="t('jobboard.create')" />

    <div class="create-form card">
      <div class="form-field">
        <label>{{ t('jobboard.titleLabel') }} *</label>
        <InputText v-model="title" :placeholder="t('jobboard.create_form.titlePlaceholder')" class="full-width" />
      </div>

      <div class="form-field">
        <label>{{ t('jobboard.category') }} *</label>
        <InputText v-model="category" :placeholder="t('jobboard.create_form.categoryPlaceholder')" class="full-width" list="category-suggestions" />
        <datalist id="category-suggestions">
          <option v-for="cat in jobboard.categories" :key="cat" :value="cat" />
        </datalist>
      </div>

      <div class="form-field">
        <label>{{ t('common.description') }}</label>
        <Textarea v-model="description" :autoResize="true" rows="4" class="full-width" />
      </div>

      <div class="form-row">
        <div class="form-field">
          <label>{{ t('jobboard.estimatedHours') }}</label>
          <InputNumber v-model="estimatedHours" :minFractionDigits="1" :maxFractionDigits="2" :min="0.5" :step="0.5" />
        </div>
        <div class="form-field">
          <label>{{ t('jobboard.maxHelpers') }}</label>
          <InputNumber v-model="maxAssignees" :min="1" :max="20" />
        </div>
      </div>

      <div class="form-field">
        <label>{{ t('jobboard.location') }}</label>
        <InputText v-model="location" :placeholder="t('jobboard.create_form.locationPlaceholder')" class="full-width" />
      </div>

      <div class="form-row">
        <div class="form-field">
          <label>{{ t('jobboard.date') }}</label>
          <DatePicker v-model="scheduledDate" dateFormat="dd.mm.yy" />
        </div>
        <div class="form-field">
          <label>{{ t('jobboard.time') }}</label>
          <InputText v-model="scheduledTime" :placeholder="t('jobboard.create_form.timePlaceholder')" />
        </div>
      </div>

      <div class="form-field">
        <label>{{ t('jobboard.contact') }}</label>
        <InputText v-model="contactInfo" :placeholder="t('jobboard.create_form.contactPlaceholder')" class="full-width" />
      </div>

      <div v-if="calendarEnabled && calendar.events.length" class="form-field">
        <label>{{ t('jobboard.linkedEvent') }}</label>
        <Select
          v-model="selectedEventId"
          :options="[{ label: t('jobboard.noLinkedEvent'), value: null }, ...calendar.events.map(e => ({ label: e.title, value: e.id }))]"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('jobboard.selectEvent')"
          class="full-width"
        />
      </div>

      <div class="form-actions">
        <Button :label="t('common.cancel')" severity="secondary" text @click="router.push({ name: 'jobs' })" />
        <Button
          :label="t('jobboard.create')"
          icon="pi pi-check"
          :loading="submitting"
          :disabled="!title.trim() || !category.trim()"
          @click="submit"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.mb-1 {
  margin-bottom: 1rem;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 600px;
}

.form-field label {
  display: block;
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
  margin-bottom: 0.25rem;
}

.full-width {
  width: 100%;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--mw-border-light);
}
</style>
