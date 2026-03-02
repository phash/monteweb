<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useParentLetterStore } from '@/stores/parentletter'
import { useRoomsStore } from '@/stores/rooms'
import type { CreateParentLetterRequest, UpdateParentLetterRequest } from '@/types/parentletter'
import type { RoomMember } from '@/types/room'
import { roomsApi } from '@/api/rooms.api'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import VariableHelpMenu from '@/components/parentletter/VariableHelpMenu.vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import Select from 'primevue/select'
import MultiSelect from 'primevue/multiselect'
import Checkbox from 'primevue/checkbox'
import InputNumber from 'primevue/inputnumber'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const toast = useToast()
const store = useParentLetterStore()
const rooms = useRoomsStore()

const isEdit = computed(() => route.name === 'parent-letter-edit')
const letterId = computed(() => route.params.id as string | undefined)

// Form state
const roomId = ref<string>('')
const title = ref('')
const content = ref<string>('')
const sendDate = ref<Date | null>(null)
const deadline = ref<Date | null>(null)
const reminderDays = ref<number>(3)
const sendToAll = ref(true)
const selectedStudentIds = ref<string[]>([])

// Room members (students) for the selected room
const roomStudents = ref<RoomMember[]>([])
const loadingStudents = ref(false)

const saving = ref(false)
const sending = ref(false)
const initialLoading = ref(false)

// Reference to the content textarea for cursor-position variable insertion
const contentTextareaRef = ref<HTMLTextAreaElement | null>(null)

const klasseRooms = computed(() =>
  rooms.myRooms.filter(r => r.type === 'KLASSE')
)

const roomOptions = computed(() =>
  klasseRooms.value.map(r => ({ label: r.name, value: r.id }))
)

const studentOptions = computed(() =>
  roomStudents.value
    .filter(m => m.userRole === 'STUDENT' || m.role === 'MEMBER')
    .map(m => ({ label: m.displayName, value: m.userId }))
)

const canSubmit = computed(() =>
  roomId.value &&
  title.value.trim() &&
  content.value.trim()
)

// When room changes, load students
watch(roomId, async (newRoomId) => {
  if (!newRoomId) {
    roomStudents.value = []
    selectedStudentIds.value = []
    return
  }
  loadingStudents.value = true
  try {
    const res = await roomsApi.getById(newRoomId)
    const data = res.data.data as any
    roomStudents.value = data.members || []
  } catch {
    roomStudents.value = []
  } finally {
    loadingStudents.value = false
  }
})

onMounted(async () => {
  await rooms.fetchMyRooms()

  if (isEdit.value && letterId.value) {
    initialLoading.value = true
    try {
      await store.fetchLetter(letterId.value)
      if (store.currentLetter) {
        const l = store.currentLetter
        roomId.value = l.roomId
        title.value = l.title
        content.value = l.content
        sendDate.value = l.sendDate ? new Date(l.sendDate) : null
        deadline.value = l.deadline ? new Date(l.deadline) : null
        reminderDays.value = l.reminderDays
        if (l.recipients.length > 0) {
          const ids = l.recipients.map(r => r.studentId)
          selectedStudentIds.value = ids
          sendToAll.value = false
        } else {
          sendToAll.value = true
        }
      }
    } finally {
      initialLoading.value = false
    }
  }

  // Pre-fill roomId from query if provided
  if (route.query.roomId) {
    roomId.value = route.query.roomId as string
  }
})

function formatDateISO(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function buildCreateRequest(): CreateParentLetterRequest {
  return {
    roomId: roomId.value,
    title: title.value.trim(),
    content: content.value.trim(),
    sendDate: sendDate.value ? formatDateISO(sendDate.value) : null,
    deadline: deadline.value ? formatDateISO(deadline.value) : null,
    reminderDays: reminderDays.value,
    studentIds: sendToAll.value ? null : selectedStudentIds.value,
  }
}

function buildUpdateRequest(): UpdateParentLetterRequest {
  return {
    title: title.value.trim(),
    content: content.value.trim(),
    sendDate: sendDate.value ? formatDateISO(sendDate.value) : null,
    deadline: deadline.value ? formatDateISO(deadline.value) : null,
    reminderDays: reminderDays.value,
    studentIds: sendToAll.value ? null : selectedStudentIds.value,
  }
}

async function handleSave() {
  if (!canSubmit.value) return
  saving.value = true
  try {
    if (isEdit.value && letterId.value) {
      await store.updateLetter(letterId.value, buildUpdateRequest())
      toast.add({ severity: 'success', summary: t('parentLetters.saved'), life: 3000 })
    } else {
      const created = await store.createLetter(buildCreateRequest())
      toast.add({ severity: 'success', summary: t('parentLetters.saved'), life: 3000 })
      router.push({ name: 'parent-letter-detail', params: { id: created.id } })
      return
    }
    router.push({ name: 'parent-letters' })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  } finally {
    saving.value = false
  }
}

async function handleSaveAndSend() {
  if (!canSubmit.value) return
  sending.value = true
  try {
    let id: string
    if (isEdit.value && letterId.value) {
      await store.updateLetter(letterId.value, buildUpdateRequest())
      id = letterId.value
    } else {
      const created = await store.createLetter(buildCreateRequest())
      id = created.id
    }
    await store.sendLetter(id)
    toast.add({ severity: 'success', summary: t('parentLetters.sent'), life: 3000 })
    router.push({ name: 'parent-letter-detail', params: { id } })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  } finally {
    sending.value = false
  }
}

function insertVariable(variable: string) {
  // Insert variable at cursor position in content textarea
  const textarea = contentTextareaRef.value
  if (!textarea) {
    content.value += variable
    return
  }
  const start = textarea.selectionStart ?? content.value.length
  const end = textarea.selectionEnd ?? content.value.length
  content.value = content.value.slice(0, start) + variable + content.value.slice(end)
  // Restore cursor after inserted text
  requestAnimationFrame(() => {
    textarea.selectionStart = start + variable.length
    textarea.selectionEnd = start + variable.length
    textarea.focus()
  })
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

    <PageTitle
      :title="isEdit ? t('parentLetters.editLetter') : t('parentLetters.newLetter')"
    />

    <LoadingSpinner v-if="initialLoading" />

    <div v-else class="letter-form card">
      <div class="form-grid">

        <!-- Raum -->
        <div class="field" v-if="!isEdit">
          <label for="letter-room" class="required">{{ t('parentLetters.form.room') }}</label>
          <Select
            id="letter-room"
            v-model="roomId"
            :options="roomOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="t('parentLetters.form.selectRoom')"
            class="w-full"
          />
          <span v-if="klasseRooms.length === 0" class="field-hint">
            {{ t('parentLetters.form.noKlasseRooms') }}
          </span>
        </div>

        <!-- Titel -->
        <div class="field">
          <label for="letter-title" class="required">{{ t('parentLetters.form.title') }}</label>
          <InputText
            id="letter-title"
            v-model="title"
            :placeholder="t('parentLetters.form.titlePlaceholder')"
            class="w-full"
          />
        </div>

        <!-- Inhalt -->
        <div class="field">
          <div class="field-label-row">
            <label for="letter-content" class="required">{{ t('parentLetters.form.content') }}</label>
            <VariableHelpMenu @insert="insertVariable" />
          </div>
          <Textarea
            id="letter-content"
            ref="contentTextareaRef"
            v-model="content"
            :autoResize="true"
            rows="10"
            class="w-full content-textarea"
            :placeholder="t('parentLetters.form.contentPlaceholder')"
          />
        </div>

        <!-- Empfänger -->
        <div class="field" v-if="roomId">
          <label>{{ t('parentLetters.form.recipients') }}</label>
          <div class="recipients-option">
            <Checkbox v-model="sendToAll" :binary="true" inputId="send-all" />
            <label for="send-all">{{ t('parentLetters.form.sendToAll') }}</label>
          </div>
          <div v-if="!sendToAll" class="field mt-half">
            <label>{{ t('parentLetters.form.selectStudents') }}</label>
            <LoadingSpinner v-if="loadingStudents" />
            <MultiSelect
              v-else
              v-model="selectedStudentIds"
              :options="studentOptions"
              optionLabel="label"
              optionValue="value"
              :placeholder="t('parentLetters.form.selectStudentsPlaceholder')"
              display="chip"
              class="w-full"
            />
          </div>
        </div>

        <!-- Dates row -->
        <div class="field-row">
          <div class="field">
            <label for="letter-senddate">{{ t('parentLetters.form.sendDate') }}</label>
            <DatePicker
              id="letter-senddate"
              v-model="sendDate"
              dateFormat="dd.mm.yy"
              class="w-full"
              showIcon
              :placeholder="t('parentLetters.form.optional')"
            />
          </div>
          <div class="field">
            <label for="letter-deadline">{{ t('parentLetters.form.deadline') }}</label>
            <DatePicker
              id="letter-deadline"
              v-model="deadline"
              dateFormat="dd.mm.yy"
              class="w-full"
              showIcon
              :placeholder="t('parentLetters.form.optional')"
            />
          </div>
        </div>

        <!-- Erinnerung -->
        <div class="field field-narrow">
          <label for="letter-reminder">{{ t('parentLetters.form.reminderDays') }}</label>
          <InputNumber
            id="letter-reminder"
            v-model="reminderDays"
            :min="0"
            :max="30"
            showButtons
            buttonLayout="horizontal"
            :step="1"
            class="w-full"
          />
          <span class="field-hint">{{ t('parentLetters.form.reminderDaysHint') }}</span>
        </div>

      </div>

      <div class="form-actions">
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          text
          @click="router.back()"
        />
        <Button
          :label="t('parentLetters.saveDraft')"
          severity="secondary"
          icon="pi pi-save"
          :disabled="!canSubmit || saving || sending"
          :loading="saving"
          @click="handleSave"
        />
        <Button
          :label="t('parentLetters.send')"
          icon="pi pi-send"
          :disabled="!canSubmit || saving || sending"
          :loading="sending"
          @click="handleSaveAndSend"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.letter-form {
  padding: 1.5rem;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  flex: 1;
}

.field label {
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
}

.field label.required::after {
  content: ' *';
  color: var(--p-red-500);
}

.field-label-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.1rem;
}

.field-label-row label {
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
  margin-bottom: 0;
}

.field-label-row label.required::after {
  content: ' *';
  color: var(--p-red-500);
}

.field-hint {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.1rem;
}

.field-row {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.field-narrow {
  max-width: 280px;
}

.content-textarea {
  font-family: inherit;
  min-height: 200px;
}

.recipients-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.mt-half {
  margin-top: 0.5rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--mw-border);
}

@media (max-width: 767px) {
  .field-row {
    flex-direction: column;
  }

  .field-narrow {
    max-width: 100%;
  }
}
</style>
