<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useFormsStore } from '@/stores/forms'
import { useRoomsStore } from '@/stores/rooms'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'primevue/usetoast'
import type { FormType, FormScope, QuestionType, QuestionRequest, CreateFormRequest } from '@/types/forms'
import PageTitle from '@/components/common/PageTitle.vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import Select from 'primevue/select'
import Checkbox from 'primevue/checkbox'
import Divider from 'primevue/divider'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const forms = useFormsStore()
const rooms = useRoomsStore()
const auth = useAuthStore()
const toast = useToast()

const isEdit = computed(() => route.name === 'form-edit')
const formId = computed(() => route.params.id as string)

const title = ref('')
const description = ref('')
const formType = ref<FormType>('SURVEY')
const scope = ref<FormScope>('ROOM')
const scopeId = ref<string | undefined>(undefined)
const anonymous = ref(false)
const deadline = ref<Date | null>(null)
const saving = ref(false)

interface QuestionDraft {
  type: QuestionType
  label: string
  description: string
  required: boolean
  options: string[]
  ratingMin: number
  ratingMax: number
}

const questions = ref<QuestionDraft[]>([])

const typeOptions = [
  { label: () => t('forms.types.SURVEY'), value: 'SURVEY' },
  { label: () => t('forms.types.CONSENT'), value: 'CONSENT' },
]

const questionTypeOptions = [
  { label: () => t('forms.questionTypes.TEXT'), value: 'TEXT' },
  { label: () => t('forms.questionTypes.SINGLE_CHOICE'), value: 'SINGLE_CHOICE' },
  { label: () => t('forms.questionTypes.MULTIPLE_CHOICE'), value: 'MULTIPLE_CHOICE' },
  { label: () => t('forms.questionTypes.RATING'), value: 'RATING' },
  { label: () => t('forms.questionTypes.YES_NO'), value: 'YES_NO' },
]

const scopeOptions = computed(() => {
  const opts = [
    { label: t('forms.scopes.ROOM'), value: 'ROOM' },
  ]
  if (auth.isTeacher || auth.isAdmin) {
    opts.push({ label: t('forms.scopes.SECTION'), value: 'SECTION' })
  }
  if (auth.isAdmin) {
    opts.push({ label: t('forms.scopes.SCHOOL'), value: 'SCHOOL' })
  }
  return opts
})

const roomOptions = computed(() =>
  rooms.myRooms.map(r => ({ label: r.name, value: r.id }))
)

const canSubmit = computed(() =>
  title.value.trim() &&
  (scope.value === 'SCHOOL' || scopeId.value)
)

onMounted(async () => {
  await rooms.fetchMyRooms()

  if (isEdit.value && formId.value) {
    await forms.fetchForm(formId.value)
    if (forms.currentForm) {
      const f = forms.currentForm.form
      title.value = f.title
      description.value = f.description || ''
      formType.value = f.type
      scope.value = f.scope
      scopeId.value = f.scopeId || undefined
      anonymous.value = f.anonymous
      deadline.value = f.deadline ? new Date(f.deadline) : null

      questions.value = forms.currentForm.questions.map(q => ({
        type: q.type,
        label: q.label,
        description: q.description || '',
        required: q.required,
        options: q.options || [],
        ratingMin: q.ratingConfig?.min ?? 1,
        ratingMax: q.ratingConfig?.max ?? 5,
      }))
    }
  }

  if (route.query.roomId) {
    scope.value = 'ROOM'
    scopeId.value = route.query.roomId as string
  }
})

function addQuestion() {
  questions.value.push({
    type: 'TEXT',
    label: '',
    description: '',
    required: false,
    options: [],
    ratingMin: 1,
    ratingMax: 5,
  })
}

function removeQuestion(idx: number) {
  questions.value.splice(idx, 1)
}

function moveQuestion(idx: number, direction: -1 | 1) {
  const newIdx = idx + direction
  if (newIdx < 0 || newIdx >= questions.value.length) return
  const temp = questions.value[idx]
  questions.value[idx] = questions.value[newIdx]
  questions.value[newIdx] = temp
}

function addOption(q: QuestionDraft) {
  q.options.push('')
}

function removeOption(q: QuestionDraft, idx: number) {
  q.options.splice(idx, 1)
}

function formatDateToISO(d: Date): string {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function buildQuestionRequests(): QuestionRequest[] {
  return questions.value.map(q => {
    const req: QuestionRequest = {
      type: q.type,
      label: q.label,
      required: q.required,
    }
    if (q.description) req.description = q.description
    if (q.type === 'SINGLE_CHOICE' || q.type === 'MULTIPLE_CHOICE') {
      req.options = q.options.filter(o => o.trim())
    }
    if (q.type === 'RATING') {
      req.ratingConfig = { min: q.ratingMin, max: q.ratingMax }
    }
    return req
  })
}

async function handleSaveDraft() {
  await handleSubmit(false)
}

async function handlePublish() {
  await handleSubmit(true)
}

async function handleSubmit(publish: boolean) {
  if (!canSubmit.value) return
  saving.value = true

  try {
    const questionReqs = buildQuestionRequests()

    if (isEdit.value && formId.value) {
      await forms.updateForm(formId.value, {
        title: title.value.trim(),
        description: description.value.trim() || undefined,
        deadline: deadline.value ? formatDateToISO(deadline.value) : undefined,
        questions: questionReqs,
      })
      if (publish) {
        await forms.publishForm(formId.value)
      }
      toast.add({ severity: 'success', summary: t('forms.saved'), life: 3000 })
    } else {
      const data: CreateFormRequest = {
        title: title.value.trim(),
        description: description.value.trim() || undefined,
        type: formType.value,
        scope: scope.value,
        scopeId: scope.value === 'SCHOOL' ? undefined : scopeId.value,
        anonymous: anonymous.value,
        deadline: deadline.value ? formatDateToISO(deadline.value) : undefined,
        questions: questionReqs,
      }
      const created = await forms.createForm(data)
      if (publish) {
        await forms.publishForm(created.form.id)
      }
      toast.add({ severity: 'success', summary: publish ? t('forms.published') : t('forms.saved'), life: 3000 })
    }

    router.push({ name: 'forms' })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
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

    <PageTitle :title="isEdit ? t('forms.editForm') : t('forms.createForm')" />

    <div class="form-card card">
      <div class="form-grid">
        <div class="field">
          <label for="form-title">{{ t('forms.formTitle') }} *</label>
          <InputText id="form-title" v-model="title" :placeholder="t('forms.titlePlaceholder')" class="w-full" />
        </div>

        <div class="field">
          <label for="form-description">{{ t('forms.description') }}</label>
          <Textarea id="form-description" v-model="description" :autoResize="true" rows="3" class="w-full" />
        </div>

        <div class="field-row" v-if="!isEdit">
          <div class="field">
            <label>{{ t('forms.formType') }} *</label>
            <Select
              v-model="formType"
              :options="typeOptions.map(o => ({ label: o.label(), value: o.value }))"
              optionLabel="label"
              optionValue="value"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('forms.scope') }} *</label>
            <Select
              v-model="scope"
              :options="scopeOptions"
              optionLabel="label"
              optionValue="value"
              class="w-full"
            />
          </div>
        </div>

        <div v-if="scope === 'ROOM' && !isEdit" class="field">
          <label>{{ t('forms.selectRoom') }}</label>
          <Select
            v-model="scopeId"
            :options="roomOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="t('forms.selectRoom')"
            class="w-full"
          />
        </div>

        <div class="field-row">
          <div class="field">
            <label>{{ t('forms.deadline') }}</label>
            <DatePicker v-model="deadline" dateFormat="dd.mm.yy" class="w-full" showIcon />
          </div>
          <div class="field-check" v-if="!isEdit">
            <Checkbox v-model="anonymous" :binary="true" inputId="anonymous" />
            <label for="anonymous">{{ t('forms.anonymousLabel') }}</label>
          </div>
        </div>
      </div>

      <Divider />

      <div class="questions-section">
        <div class="questions-header">
          <h3>{{ t('forms.questions') }} ({{ questions.length }})</h3>
          <Button :label="t('forms.addQuestion')" icon="pi pi-plus" size="small" outlined @click="addQuestion" />
        </div>

        <div
          v-for="(q, idx) in questions"
          :key="idx"
          class="question-card card"
        >
          <div class="question-header">
            <span class="question-number">{{ idx + 1 }}.</span>
            <div class="question-actions">
              <Button icon="pi pi-arrow-up" text size="small" :disabled="idx === 0" @click="moveQuestion(idx, -1)" />
              <Button icon="pi pi-arrow-down" text size="small" :disabled="idx === questions.length - 1" @click="moveQuestion(idx, 1)" />
              <Button icon="pi pi-trash" text size="small" severity="danger" @click="removeQuestion(idx)" />
            </div>
          </div>

          <div class="question-fields">
            <div class="field-row">
              <div class="field">
                <label>{{ t('forms.questionType') }}</label>
                <Select
                  v-model="q.type"
                  :options="questionTypeOptions.map(o => ({ label: o.label(), value: o.value }))"
                  optionLabel="label"
                  optionValue="value"
                  class="w-full"
                />
              </div>
              <div class="field-check">
                <Checkbox v-model="q.required" :binary="true" :inputId="`required-${idx}`" />
                <label :for="`required-${idx}`">{{ t('forms.required') }}</label>
              </div>
            </div>

            <div class="field">
              <label>{{ t('forms.questionLabel') }} *</label>
              <InputText v-model="q.label" class="w-full" />
            </div>

            <div class="field">
              <label>{{ t('forms.questionDescription') }}</label>
              <InputText v-model="q.description" class="w-full" />
            </div>

            <!-- Options for choice questions -->
            <div v-if="q.type === 'SINGLE_CHOICE' || q.type === 'MULTIPLE_CHOICE'" class="options-section">
              <label>{{ t('forms.options') }}</label>
              <div v-for="(opt, optIdx) in q.options" :key="optIdx" class="option-row">
                <InputText v-model="q.options[optIdx]" class="w-full" :placeholder="`${t('forms.option')} ${optIdx + 1}`" />
                <Button icon="pi pi-times" text size="small" severity="danger" @click="removeOption(q, optIdx)" />
              </div>
              <Button :label="t('forms.addOption')" icon="pi pi-plus" text size="small" @click="addOption(q)" />
            </div>

            <!-- Rating config -->
            <div v-if="q.type === 'RATING'" class="field-row">
              <div class="field">
                <label>Min</label>
                <InputText v-model.number="q.ratingMin" type="number" class="w-full" />
              </div>
              <div class="field">
                <label>Max</label>
                <InputText v-model.number="q.ratingMax" type="number" class="w-full" />
              </div>
            </div>
          </div>
        </div>

        <EmptyState
          v-if="!questions.length"
          icon="pi pi-question-circle"
          :message="t('forms.noQuestions')"
        />
      </div>

      <div class="form-actions">
        <Button :label="t('common.cancel')" severity="secondary" text @click="router.back()" />
        <Button
          :label="t('forms.saveDraft')"
          severity="secondary"
          :disabled="!canSubmit || saving"
          :loading="saving"
          @click="handleSaveDraft"
        />
        <Button
          :label="t('forms.publish')"
          :disabled="!canSubmit || !questions.length || saving"
          :loading="saving"
          @click="handlePublish"
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
  align-items: flex-end;
}

.field-check {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding-bottom: 0.5rem;
}

.questions-section {
  margin-top: 0.5rem;
}

.questions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.questions-header h3 {
  margin: 0;
}

.question-card {
  padding: 1rem;
  margin-bottom: 0.75rem;
  border: 1px solid var(--mw-border);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.question-number {
  font-weight: 700;
  font-size: var(--mw-font-size-md);
}

.question-actions {
  display: flex;
  gap: 0.25rem;
}

.question-fields {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.options-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.option-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
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
