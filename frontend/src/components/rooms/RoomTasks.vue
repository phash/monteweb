<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useRoomsStore } from '@/stores/rooms'
import { tasksApi } from '@/api/tasks.api'
import type {
  TaskBoardResponse,
  TaskResponse,
  CreateTaskRequest,
} from '@/types/tasks'
import { useLocaleDate } from '@/composables/useLocaleDate'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import ProgressBar from 'primevue/progressbar'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import { useToast } from 'primevue/usetoast'

const props = defineProps<{ roomId: string; isLeader: boolean }>()
const { t } = useI18n()
const { formatShortDate } = useLocaleDate()
const auth = useAuthStore()
const rooms = useRoomsStore()
const toast = useToast()

const board = ref<TaskBoardResponse | null>(null)
const loading = ref(false)

// Create task dialog
const showCreateDialog = ref(false)
const createForm = ref<{ title: string; description: string; assigneeId: string | null; dueDate: Date | null; columnId: string }>({
  title: '',
  description: '',
  assigneeId: null,
  dueDate: null,
  columnId: '',
})
const creating = ref(false)

// Edit task dialog
const showEditDialog = ref(false)
const editingTask = ref<TaskResponse | null>(null)
const editForm = ref<{ title: string; description: string; assigneeId: string | null; dueDate: Date | null; columnId: string }>({
  title: '',
  description: '',
  assigneeId: null,
  dueDate: null,
  columnId: '',
})
const saving = ref(false)

// Help panel
const showHelp = ref(false)

// Column management dialog
const showColumnDialog = ref(false)
const newColumnName = ref('')
const addingColumn = ref(false)

// Checklist
const newChecklistItemTitle = ref('')
const addingChecklistItem = ref(false)

// Drag state
const dragTaskId = ref<string | null>(null)
const dragOverColumnId = ref<string | null>(null)

const memberOptions = computed(() => {
  if (!rooms.currentRoom?.members) return []
  return rooms.currentRoom.members.map(m => ({
    label: m.displayName,
    value: m.userId,
  }))
})

const columnOptions = computed(() => {
  if (!board.value) return []
  return board.value.columns.map(c => ({
    label: c.name,
    value: c.id,
  }))
})

function tasksForColumn(columnId: string): TaskResponse[] {
  if (!board.value) return []
  return board.value.tasks
    .filter(t => t.columnId === columnId)
    .sort((a, b) => a.position - b.position)
}

function isOverdue(dueDate: string | null): boolean {
  if (!dueDate) return false
  const due = new Date(dueDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return due < today
}

async function loadBoard() {
  loading.value = true
  try {
    const res = await tasksApi.getBoard(props.roomId)
    board.value = res.data.data
  } catch {
    board.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => loadBoard())

function openCreateDialog(columnId?: string) {
  createForm.value = {
    title: '',
    description: '',
    assigneeId: null,
    dueDate: null,
    columnId: columnId || board.value?.columns[0]?.id || '',
  }
  showCreateDialog.value = true
}

async function createTask() {
  if (!createForm.value.title.trim()) return
  creating.value = true
  try {
    const data: CreateTaskRequest = {
      title: createForm.value.title.trim(),
      description: createForm.value.description.trim() || undefined,
      assigneeId: createForm.value.assigneeId || undefined,
      dueDate: createForm.value.dueDate
        ? createForm.value.dueDate.toISOString().split('T')[0]
        : undefined,
      columnId: createForm.value.columnId,
    }
    await tasksApi.createTask(props.roomId, data)
    showCreateDialog.value = false
    toast.add({ severity: 'success', summary: t('tasks.created'), life: 3000 })
    await loadBoard()
  } catch {
    toast.add({ severity: 'error', summary: t('tasks.createError'), life: 5000 })
  } finally {
    creating.value = false
  }
}

function openEditDialog(task: TaskResponse) {
  editingTask.value = task
  editForm.value = {
    title: task.title,
    description: task.description || '',
    assigneeId: task.assigneeId,
    dueDate: task.dueDate ? new Date(task.dueDate + 'T00:00:00') : null,
    columnId: task.columnId,
  }
  showEditDialog.value = true
}

async function saveTask() {
  if (!editingTask.value || !editForm.value.title.trim()) return
  saving.value = true
  try {
    await tasksApi.updateTask(props.roomId, editingTask.value.id, {
      title: editForm.value.title.trim(),
      description: editForm.value.description.trim(),
      assigneeId: editForm.value.assigneeId || undefined,
      dueDate: editForm.value.dueDate
        ? editForm.value.dueDate.toISOString().split('T')[0]
        : undefined,
      columnId: editForm.value.columnId,
    })
    showEditDialog.value = false
    toast.add({ severity: 'success', summary: t('tasks.saved'), life: 3000 })
    await loadBoard()
  } catch {
    toast.add({ severity: 'error', summary: t('tasks.saveError'), life: 5000 })
  } finally {
    saving.value = false
  }
}

async function deleteTask() {
  if (!editingTask.value) return
  try {
    await tasksApi.deleteTask(props.roomId, editingTask.value.id)
    showEditDialog.value = false
    toast.add({ severity: 'success', summary: t('tasks.deleted'), life: 3000 })
    await loadBoard()
  } catch {
    toast.add({ severity: 'error', summary: t('tasks.deleteError'), life: 5000 })
  }
}

// ---- Checklist ----
async function addChecklistItem() {
  if (!editingTask.value || !newChecklistItemTitle.value.trim()) return
  addingChecklistItem.value = true
  try {
    await tasksApi.addChecklistItem(props.roomId, editingTask.value.id, newChecklistItemTitle.value.trim())
    newChecklistItemTitle.value = ''
    toast.add({ severity: 'success', summary: t('tasks.checklistItemAdded'), life: 2000 })
    await refreshEditingTask()
  } catch {
    toast.add({ severity: 'error', summary: t('tasks.checklistError'), life: 5000 })
  } finally {
    addingChecklistItem.value = false
  }
}

async function toggleChecklistItem(itemId: string) {
  if (!editingTask.value) return
  try {
    await tasksApi.toggleChecklistItem(props.roomId, editingTask.value.id, itemId)
    await refreshEditingTask()
  } catch {
    toast.add({ severity: 'error', summary: t('tasks.checklistError'), life: 5000 })
  }
}

async function deleteChecklistItem(itemId: string) {
  if (!editingTask.value) return
  try {
    await tasksApi.deleteChecklistItem(props.roomId, editingTask.value.id, itemId)
    toast.add({ severity: 'success', summary: t('tasks.checklistItemDeleted'), life: 2000 })
    await refreshEditingTask()
  } catch {
    toast.add({ severity: 'error', summary: t('tasks.checklistError'), life: 5000 })
  }
}

async function refreshEditingTask() {
  const res = await tasksApi.getBoard(props.roomId)
  board.value = res.data.data
  if (editingTask.value && board.value) {
    const updated = board.value.tasks.find(t => t.id === editingTask.value!.id)
    if (updated) {
      editingTask.value = updated
    }
  }
}

// ---- Drag & drop ----
function onDragStart(event: DragEvent, task: TaskResponse) {
  dragTaskId.value = task.id
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', task.id)
  }
}

function onDragOver(event: DragEvent, columnId: string) {
  event.preventDefault()
  dragOverColumnId.value = columnId
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

function onDragLeave(columnId: string) {
  if (dragOverColumnId.value === columnId) {
    dragOverColumnId.value = null
  }
}

async function onDrop(event: DragEvent, columnId: string) {
  event.preventDefault()
  dragOverColumnId.value = null
  const taskId = dragTaskId.value
  dragTaskId.value = null
  if (!taskId || !board.value) return

  const task = board.value.tasks.find(t => t.id === taskId)
  if (!task || task.columnId === columnId) return

  // Move to end of target column
  const targetTasks = tasksForColumn(columnId)
  const newPosition = targetTasks.length > 0
    ? Math.max(...targetTasks.map(t => t.position)) + 1
    : 0

  try {
    await tasksApi.moveTask(props.roomId, taskId, { columnId, position: newPosition })
    await loadBoard()
  } catch {
    toast.add({ severity: 'error', summary: t('tasks.moveError'), life: 5000 })
  }
}

function onDragEnd() {
  dragTaskId.value = null
  dragOverColumnId.value = null
}

// ---- Column management ----
async function addColumn() {
  if (!newColumnName.value.trim()) return
  addingColumn.value = true
  try {
    await tasksApi.addColumn(props.roomId, { name: newColumnName.value.trim() })
    newColumnName.value = ''
    toast.add({ severity: 'success', summary: t('tasks.columnAdded'), life: 3000 })
    await loadBoard()
  } catch {
    toast.add({ severity: 'error', summary: t('tasks.columnAddError'), life: 5000 })
  } finally {
    addingColumn.value = false
  }
}

async function deleteColumn(columnId: string) {
  try {
    await tasksApi.deleteColumn(props.roomId, columnId)
    toast.add({ severity: 'success', summary: t('tasks.columnDeleted'), life: 3000 })
    await loadBoard()
  } catch (e: any) {
    toast.add({
      severity: 'error',
      summary: e.response?.data?.message || t('tasks.columnDeleteError'),
      life: 5000,
    })
  }
}
</script>

<template>
  <div class="room-tasks">
    <LoadingSpinner v-if="loading" />

    <template v-else-if="board">
      <div class="tasks-header">
        <div class="tasks-header-left">
          <Button
            :label="t('tasks.newTask')"
            icon="pi pi-plus"
            size="small"
            @click="openCreateDialog()"
          />
          <Button
            v-if="props.isLeader || auth.isAdmin"
            :label="t('tasks.manageColumns')"
            icon="pi pi-cog"
            size="small"
            severity="secondary"
            text
            @click="showColumnDialog = true"
          />
        </div>
        <Button
          icon="pi pi-question-circle"
          :label="t('tasks.help')"
          size="small"
          severity="secondary"
          text
          @click="showHelp = !showHelp"
        />
      </div>

      <!-- Help panel -->
      <div v-if="showHelp" class="help-panel">
        <h3>{{ t('tasks.helpTitle') }}</h3>
        <p>{{ t('tasks.helpIntro') }}</p>
        <h4>{{ t('tasks.helpWhatIsKanban') }}</h4>
        <p>{{ t('tasks.helpKanbanExplain') }}</p>
        <h4>{{ t('tasks.helpHowToUse') }}</h4>
        <ol>
          <li>{{ t('tasks.helpStep1') }}</li>
          <li>{{ t('tasks.helpStep2') }}</li>
          <li>{{ t('tasks.helpStep3') }}</li>
          <li>{{ t('tasks.helpStep4') }}</li>
        </ol>
        <p class="help-tip"><i class="pi pi-info-circle" /> {{ t('tasks.helpTip') }}</p>
      </div>

      <div class="kanban-board">
        <div
          v-for="column in board.columns"
          :key="column.id"
          class="kanban-column"
          :class="{ 'drag-over': dragOverColumnId === column.id }"
          @dragover="onDragOver($event, column.id)"
          @dragleave="onDragLeave(column.id)"
          @drop="onDrop($event, column.id)"
        >
          <div class="column-header">
            <span class="column-name">{{ column.name }}</span>
            <span class="column-count">{{ tasksForColumn(column.id).length }}</span>
          </div>

          <div class="column-tasks">
            <div
              v-for="task in tasksForColumn(column.id)"
              :key="task.id"
              class="task-card"
              :class="{ dragging: dragTaskId === task.id }"
              draggable="true"
              @dragstart="onDragStart($event, task)"
              @dragend="onDragEnd"
              @click="openEditDialog(task)"
            >
              <div class="task-title">{{ task.title }}</div>
              <div class="task-meta">
                <span v-if="task.assigneeName" class="task-assignee">
                  <i class="pi pi-user" />
                  {{ task.assigneeName }}
                </span>
                <span
                  v-if="task.dueDate"
                  class="task-due"
                  :class="{ overdue: isOverdue(task.dueDate) }"
                >
                  <i class="pi pi-calendar" />
                  {{ formatShortDate(task.dueDate) }}
                </span>
                <span v-if="task.checklistTotal > 0" class="task-checklist-progress">
                  <i class="pi pi-check-square" />
                  {{ t('tasks.checklistProgress', { checked: task.checklistChecked, total: task.checklistTotal }) }}
                </span>
              </div>
              <ProgressBar
                v-if="task.checklistTotal > 0"
                :value="Math.round((task.checklistChecked / task.checklistTotal) * 100)"
                :showValue="false"
                class="task-checklist-bar"
              />
            </div>
          </div>

          <Button
            :label="t('tasks.addHere')"
            icon="pi pi-plus"
            size="small"
            severity="secondary"
            text
            class="add-task-btn"
            @click="openCreateDialog(column.id)"
          />
        </div>
      </div>
    </template>

    <p v-else class="text-muted text-center">{{ t('tasks.loadError') }}</p>

    <!-- Create Task Dialog -->
    <Dialog
      v-model:visible="showCreateDialog"
      :header="t('tasks.newTask')"
      modal
      :style="{ width: '500px', maxWidth: '90vw' }"
    >
      <div class="task-form">
        <div class="form-field">
          <label>{{ t('tasks.title') }} *</label>
          <InputText
            v-model="createForm.title"
            :placeholder="t('tasks.titlePlaceholder')"
            class="w-full"
            @keydown.enter="createTask"
          />
        </div>
        <div class="form-field">
          <label>{{ t('tasks.description') }}</label>
          <Textarea
            v-model="createForm.description"
            :placeholder="t('tasks.descriptionPlaceholder')"
            class="w-full"
            rows="3"
            autoResize
          />
        </div>
        <div class="form-field">
          <label>{{ t('tasks.column') }}</label>
          <Select
            v-model="createForm.columnId"
            :options="columnOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>{{ t('tasks.assignee') }}</label>
          <Select
            v-model="createForm.assigneeId"
            :options="memberOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="t('tasks.unassigned')"
            class="w-full"
            showClear
          />
        </div>
        <div class="form-field">
          <label>{{ t('tasks.dueDate') }}</label>
          <DatePicker
            v-model="createForm.dueDate"
            dateFormat="dd.mm.yy"
            :placeholder="t('tasks.dueDatePlaceholder')"
            class="w-full"
            showIcon
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          text
          @click="showCreateDialog = false"
        />
        <Button
          :label="t('common.create')"
          icon="pi pi-plus"
          :loading="creating"
          :disabled="!createForm.title.trim()"
          @click="createTask"
        />
      </template>
    </Dialog>

    <!-- Edit Task Dialog -->
    <Dialog
      v-model:visible="showEditDialog"
      :header="t('tasks.editTask')"
      modal
      :style="{ width: '500px', maxWidth: '90vw' }"
    >
      <div class="task-form">
        <div class="form-field">
          <label>{{ t('tasks.title') }} *</label>
          <InputText v-model="editForm.title" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('tasks.description') }}</label>
          <Textarea v-model="editForm.description" class="w-full" rows="3" autoResize />
        </div>
        <div class="form-field">
          <label>{{ t('tasks.column') }}</label>
          <Select
            v-model="editForm.columnId"
            :options="columnOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>{{ t('tasks.assignee') }}</label>
          <Select
            v-model="editForm.assigneeId"
            :options="memberOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="t('tasks.unassigned')"
            class="w-full"
            showClear
          />
        </div>
        <div class="form-field">
          <label>{{ t('tasks.dueDate') }}</label>
          <DatePicker
            v-model="editForm.dueDate"
            dateFormat="dd.mm.yy"
            class="w-full"
            showIcon
          />
        </div>
        <!-- Checklist -->
        <div v-if="editingTask" class="form-field checklist-section">
          <label>{{ t('tasks.checklist') }}</label>
          <div v-if="editingTask.checklistItems.length > 0" class="checklist-items">
            <div
              v-for="item in editingTask.checklistItems"
              :key="item.id"
              class="checklist-item"
            >
              <Checkbox
                :modelValue="item.checked"
                :binary="true"
                @update:modelValue="toggleChecklistItem(item.id)"
              />
              <span class="checklist-item-title" :class="{ checked: item.checked }">
                {{ item.title }}
              </span>
              <Button
                icon="pi pi-times"
                text
                rounded
                severity="danger"
                size="small"
                class="checklist-item-delete"
                @click="deleteChecklistItem(item.id)"
              />
            </div>
          </div>
          <div v-if="editingTask.checklistTotal > 0" class="checklist-progress-info">
            <ProgressBar
              :value="Math.round((editingTask.checklistChecked / editingTask.checklistTotal) * 100)"
              :showValue="false"
              class="checklist-bar"
            />
            <span class="text-muted text-sm">
              {{ t('tasks.checklistProgress', { checked: editingTask.checklistChecked, total: editingTask.checklistTotal }) }}
            </span>
          </div>
          <div class="add-checklist-form">
            <InputText
              v-model="newChecklistItemTitle"
              :placeholder="t('tasks.addChecklistItem')"
              class="flex-1"
              @keydown.enter="addChecklistItem"
            />
            <Button
              icon="pi pi-plus"
              size="small"
              :loading="addingChecklistItem"
              :disabled="!newChecklistItemTitle.trim()"
              @click="addChecklistItem"
            />
          </div>
        </div>

        <div v-if="editingTask" class="task-info-meta">
          <span class="text-muted text-sm">
            {{ t('common.createdBy') }}: {{ editingTask.createdByName }}
          </span>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.delete')"
          severity="danger"
          text
          icon="pi pi-trash"
          @click="deleteTask"
        />
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          text
          @click="showEditDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          :disabled="!editForm.title.trim()"
          @click="saveTask"
        />
      </template>
    </Dialog>

    <!-- Column Management Dialog -->
    <Dialog
      v-model:visible="showColumnDialog"
      :header="t('tasks.manageColumns')"
      modal
      :style="{ width: '450px', maxWidth: '90vw' }"
    >
      <div class="column-management">
        <div v-if="board" class="column-list">
          <div v-for="col in board.columns" :key="col.id" class="column-item">
            <span>{{ col.name }}</span>
            <Button
              icon="pi pi-trash"
              text
              rounded
              severity="danger"
              size="small"
              @click="deleteColumn(col.id)"
            />
          </div>
        </div>
        <div class="add-column-form">
          <InputText
            v-model="newColumnName"
            :placeholder="t('tasks.newColumnName')"
            class="flex-1"
            @keydown.enter="addColumn"
          />
          <Button
            :label="t('common.add')"
            icon="pi pi-plus"
            size="small"
            :loading="addingColumn"
            :disabled="!newColumnName.trim()"
            @click="addColumn"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.close')"
          severity="secondary"
          text
          @click="showColumnDialog = false"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.room-tasks {
  padding-top: 0.5rem;
}

.tasks-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.tasks-header-left {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.help-panel {
  background: var(--mw-bg);
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-sm);
  padding: 1rem 1.25rem;
  margin-bottom: 1rem;
  font-size: var(--mw-font-size-sm);
  line-height: 1.6;
}

.help-panel h3 {
  font-size: var(--mw-font-size-md);
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.help-panel h4 {
  font-weight: 600;
  margin-top: 0.75rem;
  margin-bottom: 0.25rem;
}

.help-panel ol {
  padding-left: 1.25rem;
  margin: 0.25rem 0;
}

.help-panel li {
  margin-bottom: 0.25rem;
}

.help-tip {
  margin-top: 0.75rem;
  padding: 0.5rem 0.75rem;
  background: color-mix(in srgb, var(--mw-primary) 8%, var(--mw-bg));
  border-radius: var(--mw-border-radius-sm);
  display: flex;
  gap: 0.5rem;
  align-items: flex-start;
}

.kanban-board {
  display: flex;
  gap: 0.75rem;
  overflow-x: auto;
  padding-bottom: 1rem;
  min-height: 300px;
}

.kanban-column {
  min-width: 250px;
  max-width: 300px;
  flex: 1;
  background: var(--mw-bg);
  border-radius: var(--mw-border-radius-sm);
  border: 1px solid var(--mw-border-light);
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  transition: border-color 0.2s;
}

.kanban-column.drag-over {
  border-color: var(--mw-primary);
  background: color-mix(in srgb, var(--mw-primary) 5%, var(--mw-bg));
}

.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--mw-border-light);
}

.column-name {
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
}

.column-count {
  background: var(--mw-bg-card);
  color: var(--mw-text-muted);
  font-size: var(--mw-font-size-xs);
  padding: 0.125rem 0.5rem;
  border-radius: 99px;
  font-weight: 500;
}

.column-tasks {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
  min-height: 50px;
}

.task-card {
  background: var(--mw-bg-card);
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-sm);
  padding: 0.625rem 0.75rem;
  cursor: pointer;
  transition: box-shadow 0.15s, opacity 0.15s;
  user-select: none;
}

.task-card:hover {
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.task-card.dragging {
  opacity: 0.5;
}

.task-title {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
  margin-bottom: 0.25rem;
  word-break: break-word;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.task-assignee,
.task-due {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.task-due.overdue {
  color: var(--p-red-500);
  font-weight: 600;
}

.add-task-btn {
  margin-top: 0.5rem;
  width: 100%;
  justify-content: center;
}

.task-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-field label {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
  color: var(--mw-text-secondary);
}

.task-info-meta {
  padding-top: 0.5rem;
  border-top: 1px solid var(--mw-border-light);
}

.text-muted {
  color: var(--mw-text-muted);
}

.text-sm {
  font-size: var(--mw-font-size-sm);
}

.text-center {
  text-align: center;
  padding: 2rem;
}

.column-management {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.column-list {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.column-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-sm);
}

.add-column-form {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.add-column-form .flex-1 {
  flex: 1;
}

.task-checklist-progress {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.task-checklist-bar {
  height: 4px;
  margin-top: 0.375rem;
  border-radius: 2px;
}

.checklist-section {
  border-top: 1px solid var(--mw-border-light);
  padding-top: 0.75rem;
}

.checklist-items {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  margin-bottom: 0.5rem;
}

.checklist-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0;
}

.checklist-item-title {
  flex: 1;
  font-size: var(--mw-font-size-sm);
  word-break: break-word;
}

.checklist-item-title.checked {
  text-decoration: line-through;
  color: var(--mw-text-muted);
}

.checklist-item-delete {
  flex-shrink: 0;
}

.checklist-progress-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-bottom: 0.5rem;
}

.checklist-bar {
  height: 6px;
  border-radius: 3px;
}

.add-checklist-form {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.add-checklist-form .flex-1 {
  flex: 1;
}

@media (max-width: 767px) {
  .kanban-board {
    flex-direction: column;
  }

  .kanban-column {
    min-width: unset;
    max-width: unset;
  }

  .tasks-header {
    flex-direction: column;
    gap: 0.5rem;
    align-items: stretch;
  }

  .tasks-header-left {
    flex-direction: column;
  }
}
</style>
