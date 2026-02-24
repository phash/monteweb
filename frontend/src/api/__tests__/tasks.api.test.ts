import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { data: {} } }),
    post: vi.fn().mockResolvedValue({ data: { data: {} } }),
    put: vi.fn().mockResolvedValue({ data: { data: {} } }),
    delete: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

import client from '../client'
import { tasksApi } from '../tasks.api'

describe('tasksApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getBoard', () => {
    it('should call GET /rooms/{roomId}/tasks', async () => {
      await tasksApi.getBoard('room-1')
      expect(client.get).toHaveBeenCalledWith('/rooms/room-1/tasks')
    })
  })

  describe('createTask', () => {
    it('should call POST /rooms/{roomId}/tasks', async () => {
      const data = { title: 'Test task', columnId: 'col-1' }
      await tasksApi.createTask('room-1', data)
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/tasks', data)
    })

    it('should call POST with all optional fields', async () => {
      const data = {
        title: 'Full task',
        description: 'Description',
        assigneeId: 'user-1',
        dueDate: '2026-03-01',
        columnId: 'col-1',
      }
      await tasksApi.createTask('room-1', data)
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/tasks', data)
    })
  })

  describe('updateTask', () => {
    it('should call PUT /rooms/{roomId}/tasks/{taskId}', async () => {
      const data = { title: 'Updated title' }
      await tasksApi.updateTask('room-1', 'task-1', data)
      expect(client.put).toHaveBeenCalledWith('/rooms/room-1/tasks/task-1', data)
    })
  })

  describe('moveTask', () => {
    it('should call PUT /rooms/{roomId}/tasks/{taskId}/move', async () => {
      const data = { columnId: 'col-2', position: 0 }
      await tasksApi.moveTask('room-1', 'task-1', data)
      expect(client.put).toHaveBeenCalledWith('/rooms/room-1/tasks/task-1/move', data)
    })
  })

  describe('deleteTask', () => {
    it('should call DELETE /rooms/{roomId}/tasks/{taskId}', async () => {
      await tasksApi.deleteTask('room-1', 'task-1')
      expect(client.delete).toHaveBeenCalledWith('/rooms/room-1/tasks/task-1')
    })
  })

  describe('addColumn', () => {
    it('should call POST /rooms/{roomId}/tasks/columns', async () => {
      const data = { name: 'New Column' }
      await tasksApi.addColumn('room-1', data)
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/tasks/columns', data)
    })
  })

  describe('updateColumn', () => {
    it('should call PUT /rooms/{roomId}/tasks/columns/{columnId}', async () => {
      const data = { name: 'Renamed' }
      await tasksApi.updateColumn('room-1', 'col-1', data)
      expect(client.put).toHaveBeenCalledWith('/rooms/room-1/tasks/columns/col-1', data)
    })
  })

  describe('deleteColumn', () => {
    it('should call DELETE /rooms/{roomId}/tasks/columns/{columnId}', async () => {
      await tasksApi.deleteColumn('room-1', 'col-1')
      expect(client.delete).toHaveBeenCalledWith('/rooms/room-1/tasks/columns/col-1')
    })
  })

  describe('addChecklistItem', () => {
    it('should call POST /rooms/{roomId}/tasks/{taskId}/checklist', async () => {
      await tasksApi.addChecklistItem('room-1', 'task-1', 'Buy milk')
      expect(client.post).toHaveBeenCalledWith('/rooms/room-1/tasks/task-1/checklist', {
        title: 'Buy milk',
      })
    })
  })

  describe('toggleChecklistItem', () => {
    it('should call PUT /rooms/{roomId}/tasks/{taskId}/checklist/{itemId}/toggle', async () => {
      await tasksApi.toggleChecklistItem('room-1', 'task-1', 'item-1')
      expect(client.put).toHaveBeenCalledWith(
        '/rooms/room-1/tasks/task-1/checklist/item-1/toggle',
      )
    })
  })

  describe('deleteChecklistItem', () => {
    it('should call DELETE /rooms/{roomId}/tasks/{taskId}/checklist/{itemId}', async () => {
      await tasksApi.deleteChecklistItem('room-1', 'task-1', 'item-1')
      expect(client.delete).toHaveBeenCalledWith(
        '/rooms/room-1/tasks/task-1/checklist/item-1',
      )
    })
  })
})
