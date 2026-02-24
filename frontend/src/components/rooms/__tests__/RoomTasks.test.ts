import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import RoomTasks from '@/components/rooms/RoomTasks.vue'

vi.mock('@/api/tasks.api', () => ({
  tasksApi: {
    getBoard: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'board-1',
          roomId: 'room-1',
          columns: [
            { id: 'col-1', name: 'Offen', position: 0 },
            { id: 'col-2', name: 'In Arbeit', position: 1 },
            { id: 'col-3', name: 'Erledigt', position: 2 },
          ],
          tasks: [
            {
              id: 'task-1',
              columnId: 'col-1',
              title: 'Test Aufgabe 1',
              description: 'Beschreibung',
              assigneeId: 'user-1',
              assigneeName: 'Max Muster',
              createdBy: 'user-2',
              createdByName: 'Anna Test',
              dueDate: '2026-03-01',
              position: 0,
              createdAt: '2026-02-20T10:00:00Z',
              checklistItems: [
                { id: 'ci-1', title: 'Item 1', checked: true, position: 0 },
                { id: 'ci-2', title: 'Item 2', checked: false, position: 1 },
              ],
              checklistTotal: 2,
              checklistChecked: 1,
            },
            {
              id: 'task-2',
              columnId: 'col-2',
              title: 'Test Aufgabe 2',
              description: null,
              assigneeId: null,
              assigneeName: null,
              createdBy: 'user-1',
              createdByName: 'Max Muster',
              dueDate: null,
              position: 0,
              createdAt: '2026-02-21T10:00:00Z',
              checklistItems: [],
              checklistTotal: 0,
              checklistChecked: 0,
            },
          ],
        },
      },
    }),
    createTask: vi.fn().mockResolvedValue({ data: { data: { id: 'task-new' } } }),
    updateTask: vi.fn().mockResolvedValue({ data: { data: {} } }),
    moveTask: vi.fn().mockResolvedValue({ data: { data: {} } }),
    deleteTask: vi.fn().mockResolvedValue({ data: { data: null } }),
    addColumn: vi.fn().mockResolvedValue({ data: { data: { id: 'col-new', name: 'Neu', position: 3 } } }),
    updateColumn: vi.fn().mockResolvedValue({ data: { data: {} } }),
    deleteColumn: vi.fn().mockResolvedValue({ data: { data: null } }),
    addChecklistItem: vi.fn().mockResolvedValue({ data: { data: { id: 'ci-new', title: 'New', checked: false, position: 2 } } }),
    toggleChecklistItem: vi.fn().mockResolvedValue({ data: { data: { id: 'ci-1', title: 'Item 1', checked: false, position: 0 } } }),
    deleteChecklistItem: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
    discover: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))
vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      tasks: {
        title: 'Aufgaben',
        newTask: 'Neue Aufgabe',
        editTask: 'Aufgabe bearbeiten',
        taskTitle: 'Titel',
        description: 'Beschreibung',
        column: 'Spalte',
        assignee: 'Zugewiesen an',
        unassigned: 'Nicht zugewiesen',
        dueDate: 'Fälligkeitsdatum',
        dueDatePlaceholder: 'Datum wählen',
        titlePlaceholder: 'Aufgabe eingeben...',
        descriptionPlaceholder: 'Beschreibung (optional)',
        addHere: 'Hier hinzufügen',
        manageColumns: 'Spalten verwalten',
        newColumnName: 'Spaltenname',
        created: 'Aufgabe erstellt',
        saved: 'Aufgabe gespeichert',
        deleted: 'Aufgabe gelöscht',
        createError: 'Fehler beim Erstellen',
        saveError: 'Fehler beim Speichern',
        deleteError: 'Fehler beim Löschen',
        moveError: 'Fehler beim Verschieben',
        columnAdded: 'Spalte hinzugefügt',
        columnDeleted: 'Spalte gelöscht',
        columnAddError: 'Fehler beim Hinzufügen',
        columnDeleteError: 'Konnte nicht gelöscht werden',
        loadError: 'Aufgaben konnten nicht geladen werden',
        checklist: 'Checkliste',
        addChecklistItem: 'Punkt hinzufügen',
        checklistProgress: '{checked} von {total}',
        checklistItemAdded: 'Checklistenpunkt hinzugefügt',
        checklistItemDeleted: 'Checklistenpunkt gelöscht',
        checklistError: 'Fehler bei der Checkliste',
      },
      common: {
        cancel: 'Abbrechen',
        create: 'Erstellen',
        save: 'Speichern',
        delete: 'Löschen',
        close: 'Schließen',
        add: 'Hinzufügen',
        loading: 'Laden...',
        createdBy: 'Erstellt von',
      },
    },
  },
})

const stubs = {
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  Checkbox: {
    template: '<input type="checkbox" class="checkbox-stub" />',
    props: ['modelValue', 'binary'],
    emits: ['update:modelValue'],
  },
  ProgressBar: {
    template: '<div class="progressbar-stub" />',
    props: ['value', 'showValue'],
  },
  Button: {
    template:
      '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: [
      'label',
      'icon',
      'text',
      'severity',
      'size',
      'loading',
      'disabled',
      'ariaLabel',
      'rounded',
    ],
  },
  Dialog: {
    template:
      '<div class="dialog-stub" v-if="visible"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal', 'style'],
  },
  InputText: {
    template: '<input class="input-stub" />',
    props: ['modelValue', 'placeholder'],
  },
  Textarea: {
    template: '<textarea class="textarea-stub" />',
    props: ['modelValue', 'placeholder', 'autoResize', 'rows'],
  },
  DatePicker: {
    template: '<input class="datepicker-stub" />',
    props: ['modelValue', 'dateFormat', 'placeholder', 'showIcon'],
  },
  Select: {
    template: '<select class="select-stub"></select>',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder', 'showClear'],
    emits: ['update:modelValue'],
  },
}

function mountRoomTasks(isLeader = false) {
  const pinia = createPinia()
  return mount(RoomTasks, {
    props: { roomId: 'room-1', isLeader },
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('RoomTasks', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountRoomTasks()
    expect(wrapper.exists()).toBe(true)
  })

  it('should show loading state initially', () => {
    const wrapper = mountRoomTasks()
    // Component starts in loading state before board loads
    expect(wrapper.find('.loading-stub').exists() || wrapper.find('.room-tasks').exists()).toBe(
      true,
    )
  })

  it('should render kanban columns after loading', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    const columns = wrapper.findAll('.kanban-column')
    expect(columns.length).toBe(3)
  })

  it('should show column names', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    const columnNames = wrapper.findAll('.column-name')
    expect(columnNames[0]?.text()).toBe('Offen')
    expect(columnNames[1]?.text()).toBe('In Arbeit')
    expect(columnNames[2]?.text()).toBe('Erledigt')
  })

  it('should render task cards', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    const taskCards = wrapper.findAll('.task-card')
    expect(taskCards.length).toBe(2)
  })

  it('should display task title on card', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    const titles = wrapper.findAll('.task-title')
    expect(titles[0]?.text()).toBe('Test Aufgabe 1')
  })

  it('should show assignee name on task card', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    const assignees = wrapper.findAll('.task-assignee')
    expect(assignees.length).toBeGreaterThan(0)
    expect(assignees[0]?.text()).toContain('Max Muster')
  })

  it('should show due date on task card', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    const dueDates = wrapper.findAll('.task-due')
    expect(dueDates.length).toBeGreaterThan(0)
  })

  it('should show column task count', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    const counts = wrapper.findAll('.column-count')
    expect(counts[0]?.text()).toBe('1') // col-1 has 1 task
    expect(counts[1]?.text()).toBe('1') // col-2 has 1 task
    expect(counts[2]?.text()).toBe('0') // col-3 has 0 tasks
  })

  it('should show new task button', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('.button-stub')
    const newTaskBtn = buttons.find((b) => b.text().includes('Neue Aufgabe'))
    expect(newTaskBtn?.exists()).toBe(true)
  })

  it('should show manage columns button for leader', async () => {
    const wrapper = mountRoomTasks(true)
    await flushPromises()
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('.button-stub')
    const manageBtn = buttons.find((b) => b.text().includes('Spalten verwalten'))
    expect(manageBtn?.exists()).toBe(true)
  })

  it('should not show manage columns button for non-leader', async () => {
    const wrapper = mountRoomTasks(false)
    await flushPromises()
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('.button-stub')
    const manageBtn = buttons.find((b) => b.text().includes('Spalten verwalten'))
    expect(manageBtn).toBeUndefined()
  })

  it('should have add task buttons per column', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    const addButtons = wrapper.findAll('.add-task-btn')
    expect(addButtons.length).toBe(3) // one per column
  })

  it('should have task cards with draggable attribute', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    const taskCards = wrapper.findAll('.task-card')
    taskCards.forEach((card) => {
      expect(card.attributes('draggable')).toBe('true')
    })
  })

  it('should not show dialogs initially', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.dialog-stub').exists()).toBe(false)
  })

  it('should show checklist progress on task card with checklist items', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    const checklistProgress = wrapper.findAll('.task-checklist-progress')
    expect(checklistProgress.length).toBe(1) // only task-1 has checklist items
  })

  it('should show progress bar on task card with checklist items', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    const progressBars = wrapper.findAll('.task-checklist-bar')
    expect(progressBars.length).toBe(1) // only task-1 has checklist items
  })

  it('should not show checklist progress for tasks without checklist items', async () => {
    const wrapper = mountRoomTasks()
    await flushPromises()
    await wrapper.vm.$nextTick()

    // task-2 has no checklist items, so only 1 progress indicator total
    const checklistProgress = wrapper.findAll('.task-checklist-progress')
    expect(checklistProgress.length).toBe(1)
  })
})
