import client from './client'
import type { ApiResponse } from '@/types/api'
import type {
  TaskBoardResponse,
  TaskResponse,
  TaskColumnResponse,
  CreateTaskRequest,
  UpdateTaskRequest,
  MoveTaskRequest,
  CreateColumnRequest,
  UpdateColumnRequest,
} from '@/types/tasks'

export const tasksApi = {
  // Board
  getBoard(roomId: string) {
    return client.get<ApiResponse<TaskBoardResponse>>(`/rooms/${roomId}/tasks`)
  },

  // Tasks
  createTask(roomId: string, data: CreateTaskRequest) {
    return client.post<ApiResponse<TaskResponse>>(`/rooms/${roomId}/tasks`, data)
  },
  updateTask(roomId: string, taskId: string, data: UpdateTaskRequest) {
    return client.put<ApiResponse<TaskResponse>>(`/rooms/${roomId}/tasks/${taskId}`, data)
  },
  moveTask(roomId: string, taskId: string, data: MoveTaskRequest) {
    return client.put<ApiResponse<TaskResponse>>(`/rooms/${roomId}/tasks/${taskId}/move`, data)
  },
  deleteTask(roomId: string, taskId: string) {
    return client.delete<ApiResponse<void>>(`/rooms/${roomId}/tasks/${taskId}`)
  },

  // Columns
  addColumn(roomId: string, data: CreateColumnRequest) {
    return client.post<ApiResponse<TaskColumnResponse>>(`/rooms/${roomId}/tasks/columns`, data)
  },
  updateColumn(roomId: string, columnId: string, data: UpdateColumnRequest) {
    return client.put<ApiResponse<TaskColumnResponse>>(
      `/rooms/${roomId}/tasks/columns/${columnId}`,
      data,
    )
  },
  deleteColumn(roomId: string, columnId: string) {
    return client.delete<ApiResponse<void>>(`/rooms/${roomId}/tasks/columns/${columnId}`)
  },
}
