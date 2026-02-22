<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useFormsStore } from '@/stores/forms'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'primevue/usetoast'
import type { AnswerRequest } from '@/types/forms'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import RadioButton from 'primevue/radiobutton'
import Checkbox from 'primevue/checkbox'
import Rating from 'primevue/rating'

const { t } = useI18n()
const { formatShortDate } = useLocaleDate()
const router = useRouter()
const route = useRoute()
const forms = useFormsStore()
const auth = useAuthStore()
const toast = useToast()

const formId = computed(() => route.params.id as string)
const submitting = ref(false)
const isEditing = ref(false)
const answers = ref<Record<string, AnswerRequest>>({})

function getAnswer(questionId: string): AnswerRequest {
  if (!answers.value[questionId]) {
    answers.value[questionId] = { questionId, selectedOptions: [] }
  }
  return answers.value[questionId]
}

const isCreator = computed(() =>
  forms.currentForm?.form.createdBy === auth.user?.id
)

const isAdmin = computed(() => auth.isAdmin)

const canManage = computed(() => isCreator.value || isAdmin.value)

const deadlineNotPassed = computed(() => {
  if (!forms.currentForm) return false
  const f = forms.currentForm.form
  if (!f.deadline) return true
  const dl: string = f.deadline
  return new Date(dl) >= new Date(new Date().toISOString().split('T')[0]!)
})

const canEditResponse = computed(() => {
  if (!forms.currentForm) return false
  const f = forms.currentForm.form
  return f.status === 'PUBLISHED' && f.hasUserResponded && !f.anonymous && deadlineNotPassed.value
})

const canRespond = computed(() => {
  if (!forms.currentForm) return false
  const f = forms.currentForm.form
  return (f.status === 'PUBLISHED' && !f.hasUserResponded) || isEditing.value
})

onMounted(async () => {
  await forms.fetchForm(formId.value)
  if (forms.currentForm) {
    // Initialize answer refs
    for (const q of forms.currentForm.questions) {
      answers.value[q.id] = {
        questionId: q.id,
        selectedOptions: [],
      }
    }
  }
})

async function handlePublish() {
  try {
    await forms.publishForm(formId.value)
    toast.add({ severity: 'success', summary: t('forms.published'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  }
}

async function handleClose() {
  try {
    await forms.closeForm(formId.value)
    toast.add({ severity: 'success', summary: t('forms.closed'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  }
}

async function handleDelete() {
  try {
    await forms.deleteForm(formId.value)
    toast.add({ severity: 'success', summary: t('forms.deleted'), life: 3000 })
    router.push({ name: 'forms' })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  }
}

async function handleEditResponse() {
  await forms.fetchMyResponse(formId.value)
  if (forms.myResponse) {
    // Pre-fill answers from existing response
    for (const a of forms.myResponse.answers) {
      answers.value[a.questionId] = {
        questionId: a.questionId,
        text: a.text ?? undefined,
        selectedOptions: a.selectedOptions ?? [],
        rating: a.rating ?? undefined,
      }
    }
  }
  isEditing.value = true
}

async function handleArchive() {
  try {
    await forms.archiveForm(formId.value)
    toast.add({ severity: 'success', summary: t('forms.formArchived'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  }
}

async function handleSubmitResponse() {
  submitting.value = true
  try {
    const answerList = Object.values(answers.value).filter(a => {
      return a.text || (a.selectedOptions && a.selectedOptions.length > 0) || a.rating != null
    })

    if (isEditing.value) {
      await forms.updateResponse(formId.value, { answers: answerList })
      toast.add({ severity: 'success', summary: t('forms.responseUpdated'), life: 3000 })
      isEditing.value = false
    } else {
      await forms.submitResponse(formId.value, { answers: answerList })
      toast.add({ severity: 'success', summary: t('forms.thankYou'), life: 3000 })
    }
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  } finally {
    submitting.value = false
  }
}

function statusSeverity(status: string): 'info' | 'success' | 'warn' | 'danger' | 'secondary' {
  switch (status) {
    case 'DRAFT': return 'secondary'
    case 'PUBLISHED': return 'success'
    case 'CLOSED': return 'warn'
    case 'ARCHIVED': return 'danger'
    default: return 'secondary'
  }
}

function onRadioSelect(questionId: string, option: string) {
  const existing = answers.value[questionId]!
  answers.value[questionId] = {
    ...existing,
    selectedOptions: [option],
  }
}

function onCheckboxToggle(questionId: string, option: string, checked: boolean) {
  const existing = answers.value[questionId]!
  const current = existing.selectedOptions || []
  if (checked) {
    answers.value[questionId] = {
      ...existing,
      selectedOptions: [...current, option],
    }
  } else {
    answers.value[questionId] = {
      ...existing,
      selectedOptions: current.filter(o => o !== option),
    }
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

    <LoadingSpinner v-if="!forms.currentForm" />

    <template v-else>
      <div class="form-header">
        <PageTitle :title="forms.currentForm.form.title" />
        <div class="header-tags">
          <Tag :value="t(`forms.types.${forms.currentForm.form.type}`)" :severity="forms.currentForm.form.type === 'CONSENT' ? 'warn' : 'info'" />
          <Tag :value="t(`forms.statuses.${forms.currentForm.form.status}`)" :severity="statusSeverity(forms.currentForm.form.status)" />
          <Tag v-if="forms.currentForm.form.anonymous" :value="t('forms.anonymous')" severity="secondary" icon="pi pi-eye-slash" />
        </div>
      </div>

      <p v-if="forms.currentForm.form.description" class="form-description">
        {{ forms.currentForm.form.description }}
      </p>

      <div class="form-meta-bar card">
        <span v-if="forms.currentForm.form.sectionNames?.length"><i class="pi pi-tag" /> {{ forms.currentForm.form.sectionNames.join(', ') }}</span>
        <span v-else-if="forms.currentForm.form.scopeName"><i class="pi pi-tag" /> {{ forms.currentForm.form.scopeName }}</span>
        <span><i class="pi pi-user" /> {{ forms.currentForm.form.creatorName }}</span>
        <span v-if="forms.currentForm.form.deadline"><i class="pi pi-calendar" /> {{ t('forms.deadlineLabel') }}: {{ formatShortDate(forms.currentForm.form.deadline) }}</span>
        <span><i class="pi pi-inbox" /> {{ forms.currentForm.form.responseCount }} {{ t('forms.responsesCount') }}</span>
      </div>

      <!-- Management buttons for creator/admin -->
      <div v-if="canManage" class="management-actions card">
        <Button
          v-if="forms.currentForm.form.status === 'DRAFT'"
          :label="t('forms.editQuestions')"
          icon="pi pi-pencil"
          severity="secondary"
          size="small"
          @click="router.push({ name: 'form-edit', params: { id: formId } })"
        />
        <Button
          v-if="forms.currentForm.form.status === 'DRAFT'"
          :label="t('forms.publish')"
          icon="pi pi-send"
          size="small"
          @click="handlePublish"
        />
        <Button
          v-if="forms.currentForm.form.status === 'PUBLISHED'"
          :label="t('forms.close')"
          icon="pi pi-lock"
          severity="warn"
          size="small"
          @click="handleClose"
        />
        <Button
          v-if="forms.currentForm.form.status === 'CLOSED'"
          :label="t('forms.archive')"
          icon="pi pi-box"
          severity="secondary"
          size="small"
          @click="handleArchive"
        />
        <Button
          v-if="forms.currentForm.form.status !== 'DRAFT'"
          :label="t('forms.viewResults')"
          icon="pi pi-chart-bar"
          severity="info"
          size="small"
          @click="router.push({ name: 'form-results', params: { id: formId } })"
        />
        <Button
          :label="t('forms.deleteForm')"
          icon="pi pi-trash"
          severity="danger"
          text
          size="small"
          @click="handleDelete"
        />
      </div>

      <div v-if="canEditResponse && forms.currentForm.form.deadline" class="edit-hint">
        <i class="pi pi-info-circle" />
        {{ t('forms.canEditUntil') }}: {{ formatShortDate(forms.currentForm.form.deadline) }}
      </div>

      <!-- Already responded message -->
      <div v-if="forms.currentForm.form.hasUserResponded && !isEditing" class="responded-message card">
        <i class="pi pi-check-circle" />
        <span>{{ t('forms.alreadyResponded') }}</span>
        <div class="responded-actions">
          <Button
            v-if="canEditResponse"
            :label="t('forms.editResponse')"
            icon="pi pi-pencil"
            severity="secondary"
            size="small"
            @click="handleEditResponse"
          />
          <Button
            v-if="(forms.currentForm.form.status === 'CLOSED' || forms.currentForm.form.status === 'ARCHIVED')"
            :label="t('forms.viewResultsHint')"
            icon="pi pi-chart-bar"
            severity="info"
            size="small"
            @click="router.push({ name: 'form-results', params: { id: formId } })"
          />
        </div>
      </div>

      <!-- Response form -->
      <div v-if="canRespond" class="response-form">
        <div
          v-for="q in forms.currentForm.questions"
          :key="q.id"
          class="question-block card"
        >
          <div class="question-label">
            {{ q.label }}
            <span v-if="q.required" class="required-star">*</span>
          </div>
          <p v-if="q.description" class="question-desc">{{ q.description }}</p>

          <!-- TEXT -->
          <Textarea
            v-if="q.type === 'TEXT'"
            v-model="getAnswer(q.id).text"
            :autoResize="true"
            rows="3"
            class="w-full"
          />

          <!-- SINGLE_CHOICE -->
          <div v-if="q.type === 'SINGLE_CHOICE' && q.options" class="radio-group">
            <div v-for="opt in q.options" :key="opt" class="radio-item">
              <RadioButton
                :modelValue="getAnswer(q.id).selectedOptions?.[0]"
                :value="opt"
                :name="`q-${q.id}`"
                :inputId="`q-${q.id}-${opt}`"
                @update:modelValue="onRadioSelect(q.id, opt)"
              />
              <label :for="`q-${q.id}-${opt}`">{{ opt }}</label>
            </div>
          </div>

          <!-- MULTIPLE_CHOICE -->
          <div v-if="q.type === 'MULTIPLE_CHOICE' && q.options" class="checkbox-group">
            <div v-for="opt in q.options" :key="opt" class="checkbox-item">
              <Checkbox
                :modelValue="(getAnswer(q.id).selectedOptions || []).includes(opt)"
                :binary="true"
                :inputId="`q-${q.id}-${opt}`"
                @update:modelValue="(val: boolean) => onCheckboxToggle(q.id, opt, val)"
              />
              <label :for="`q-${q.id}-${opt}`">{{ opt }}</label>
            </div>
          </div>

          <!-- RATING -->
          <Rating
            v-if="q.type === 'RATING'"
            v-model="getAnswer(q.id).rating"
            :stars="q.ratingConfig?.max || 5"
          />

          <!-- YES_NO -->
          <div v-if="q.type === 'YES_NO'" class="yesno-group">
            <Button
              :label="t('forms.yes')"
              :severity="getAnswer(q.id).text === 'yes' ? 'success' : 'secondary'"
              :outlined="getAnswer(q.id).text !== 'yes'"
              size="small"
              @click="getAnswer(q.id).text = 'yes'"
            />
            <Button
              :label="t('forms.no')"
              :severity="getAnswer(q.id).text === 'no' ? 'danger' : 'secondary'"
              :outlined="getAnswer(q.id).text !== 'no'"
              size="small"
              @click="getAnswer(q.id).text = 'no'"
            />
          </div>
        </div>

        <div class="submit-actions">
          <Button
            :label="isEditing ? t('forms.updateResponse') : t('forms.submitResponse')"
            icon="pi pi-send"
            :loading="submitting"
            :disabled="submitting"
            @click="handleSubmitResponse"
          />
        </div>
      </div>

      <!-- Questions display for non-respondable states -->
      <div v-else-if="forms.currentForm.form.status !== 'PUBLISHED'" class="questions-readonly">
        <div
          v-for="(q, idx) in forms.currentForm.questions"
          :key="q.id"
          class="question-block card"
        >
          <div class="question-label">
            {{ idx + 1 }}. {{ q.label }}
            <Tag :value="t(`forms.questionTypes.${q.type}`)" severity="secondary" size="small" />
          </div>
          <p v-if="q.description" class="question-desc">{{ q.description }}</p>
          <div v-if="q.options" class="options-preview">
            <span v-for="opt in q.options" :key="opt" class="option-tag">{{ opt }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.form-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  flex-wrap: wrap;
}

.header-tags {
  display: flex;
  gap: 0.5rem;
}

.form-description {
  color: var(--mw-text-muted);
  margin-bottom: 1rem;
}

.form-meta-bar {
  display: flex;
  gap: 1.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  flex-wrap: wrap;
}

.form-meta-bar i {
  margin-right: 0.25rem;
}

.management-actions {
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.responded-message {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  margin-bottom: 1rem;
  color: var(--p-green-600);
  font-weight: 600;
}

.responded-message i {
  font-size: 1.5rem;
}

.question-block {
  padding: 1rem;
  margin-bottom: 0.75rem;
}

.question-label {
  font-weight: 600;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.required-star {
  color: var(--p-red-500);
}

.question-desc {
  color: var(--mw-text-muted);
  font-size: var(--mw-font-size-sm);
  margin-bottom: 0.75rem;
  margin-top: 0;
}

.radio-group,
.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.radio-item,
.checkbox-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.yesno-group {
  display: flex;
  gap: 0.5rem;
}

.submit-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

.options-preview {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.option-tag {
  background: var(--mw-bg-hover);
  padding: 0.25rem 0.75rem;
  border-radius: 1rem;
  font-size: var(--mw-font-size-sm);
}

.responded-actions {
  display: flex;
  gap: 0.5rem;
  margin-left: auto;
}

.edit-hint {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
</style>
