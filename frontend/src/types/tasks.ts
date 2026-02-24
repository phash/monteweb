export interface TaskBoardResponse {
  id: string
  roomId: string
  columns: TaskColumnResponse[]
  tasks: TaskResponse[]
}

export interface TaskColumnResponse {
  id: string
  name: string
  position: number
}

export interface ChecklistItemResponse {
  id: string
  title: string
  checked: boolean
  position: number
}

export interface TaskResponse {
  id: string
  columnId: string
  title: string
  description: string | null
  assigneeId: string | null
  assigneeName: string | null
  createdBy: string
  createdByName: string
  dueDate: string | null
  position: number
  createdAt: string
  checklistItems: ChecklistItemResponse[]
  checklistTotal: number
  checklistChecked: number
}

export interface CreateTaskRequest {
  title: string
  description?: string
  assigneeId?: string
  dueDate?: string
  columnId: string
}

export interface UpdateTaskRequest {
  title?: string
  description?: string
  assigneeId?: string
  dueDate?: string
  columnId?: string
  position?: number
}

export interface MoveTaskRequest {
  columnId: string
  position: number
}

export interface CreateColumnRequest {
  name: string
}

export interface UpdateColumnRequest {
  name?: string
  position?: number
}
