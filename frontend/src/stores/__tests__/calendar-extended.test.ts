import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCalendarStore } from '@/stores/calendar'

vi.mock('@/api/calendar.api', () => ({
  calendarApi: {
    getEvents: vi.fn(),
    getEvent: vi.fn(),
    createEvent: vi.fn(),
    updateEvent: vi.fn(),
    deleteEvent: vi.fn(),
    cancelEvent: vi.fn(),
    rsvp: vi.fn(),
    getRoomEvents: vi.fn(),
  },
}))

import { calendarApi } from '@/api/calendar.api'

describe('Calendar Store - Extended', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('fetchEvents with append mode', () => {
    it('should append events when reset=false', async () => {
      const store = useCalendarStore()
      store.events = [{ id: 'existing' }] as any

      vi.mocked(calendarApi.getEvents).mockResolvedValue({
        data: { data: { content: [{ id: 'new' }], totalElements: 2, last: false } },
      } as any)

      await store.fetchEvents('2026-01-01', '2026-01-31', false, 1)

      expect(store.events).toHaveLength(2)
      expect(store.events[0].id).toBe('existing')
      expect(store.events[1].id).toBe('new')
    })

    it('should replace events when reset=true (default)', async () => {
      const store = useCalendarStore()
      store.events = [{ id: 'old' }] as any

      vi.mocked(calendarApi.getEvents).mockResolvedValue({
        data: { data: { content: [{ id: 'new' }], totalElements: 1, last: true } },
      } as any)

      await store.fetchEvents('2026-01-01', '2026-01-31')

      expect(store.events).toHaveLength(1)
      expect(store.events[0].id).toBe('new')
    })

    it('should reset loading even on error', async () => {
      const store = useCalendarStore()

      vi.mocked(calendarApi.getEvents).mockRejectedValue(new Error('Error'))

      try {
        await store.fetchEvents('2026-01-01', '2026-01-31')
      } catch {
        // Expected
      }

      expect(store.loading).toBe(false)
    })
  })

  describe('fetchRoomEvents error handling', () => {
    it('should clear state on error', async () => {
      const store = useCalendarStore()
      store.events = [{ id: 'old' }] as any
      store.totalEvents = 5
      store.hasMore = true

      vi.mocked(calendarApi.getRoomEvents).mockRejectedValue(new Error('Network'))

      await store.fetchRoomEvents('room-1')

      expect(store.events).toEqual([])
      expect(store.totalEvents).toBe(0)
      expect(store.hasMore).toBe(false)
      expect(store.loading).toBe(false)
    })
  })

  describe('deleteEvent clearing currentEvent', () => {
    it('should clear currentEvent when deleting the active event', async () => {
      const store = useCalendarStore()
      store.events = [{ id: 'e1' }, { id: 'e2' }] as any
      store.currentEvent = { id: 'e1' } as any

      vi.mocked(calendarApi.deleteEvent).mockResolvedValue({} as any)

      await store.deleteEvent('e1')

      expect(store.currentEvent).toBeNull()
      expect(store.events).toHaveLength(1)
    })

    it('should not clear currentEvent when deleting a different event', async () => {
      const store = useCalendarStore()
      store.events = [{ id: 'e1' }, { id: 'e2' }] as any
      store.currentEvent = { id: 'e2' } as any

      vi.mocked(calendarApi.deleteEvent).mockResolvedValue({} as any)

      await store.deleteEvent('e1')

      expect(store.currentEvent).not.toBeNull()
      expect(store.currentEvent!.id).toBe('e2')
    })
  })

  describe('updateEvent edge cases', () => {
    it('should not update currentEvent when it is a different event', async () => {
      const store = useCalendarStore()
      store.events = [{ id: 'e1', title: 'Old' }] as any
      store.currentEvent = { id: 'other', title: 'Other' } as any

      vi.mocked(calendarApi.updateEvent).mockResolvedValue({
        data: { data: { id: 'e1', title: 'Updated' } },
      } as any)

      await store.updateEvent('e1', { title: 'Updated' } as any)

      expect(store.events[0].title).toBe('Updated')
      expect(store.currentEvent!.title).toBe('Other') // Unchanged
    })

    it('should return the updated event', async () => {
      const store = useCalendarStore()
      store.events = [{ id: 'e1' }] as any

      const updated = { id: 'e1', title: 'Updated' }
      vi.mocked(calendarApi.updateEvent).mockResolvedValue({
        data: { data: updated },
      } as any)

      const result = await store.updateEvent('e1', { title: 'Updated' } as any)
      expect(result.title).toBe('Updated')
    })
  })

  describe('cancelEvent edge cases', () => {
    it('should not update currentEvent when it is a different event', async () => {
      const store = useCalendarStore()
      store.events = [{ id: 'e1', cancelled: false }] as any
      store.currentEvent = { id: 'other', cancelled: false } as any

      vi.mocked(calendarApi.cancelEvent).mockResolvedValue({
        data: { data: { id: 'e1', cancelled: true } },
      } as any)

      await store.cancelEvent('e1')

      expect(store.events[0].cancelled).toBe(true)
      expect(store.currentEvent!.cancelled).toBe(false) // Unchanged
    })
  })

  describe('rsvp edge cases', () => {
    it('should not update if event not found in list', async () => {
      const store = useCalendarStore()
      store.events = [] as any
      store.currentEvent = null

      vi.mocked(calendarApi.rsvp).mockResolvedValue({
        data: { data: { id: 'unknown', currentUserRsvp: 'ATTENDING' } },
      } as any)

      const result = await store.rsvp('unknown', 'ATTENDING' as any)
      expect(result.currentUserRsvp).toBe('ATTENDING')
    })
  })

  describe('fetchEvent', () => {
    it('should set currentEvent', async () => {
      const store = useCalendarStore()
      const event = { id: 'e1', title: 'My Event' }

      vi.mocked(calendarApi.getEvent).mockResolvedValue({
        data: { data: event },
      } as any)

      await store.fetchEvent('e1')

      expect(store.currentEvent).toEqual(event)
    })
  })
})
