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

describe('Calendar Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useCalendarStore()
    expect(store.events).toEqual([])
    expect(store.currentEvent).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.totalEvents).toBe(0)
    expect(store.hasMore).toBe(true)
  })

  it('should fetch events and set list + totalEvents + hasMore', async () => {
    const store = useCalendarStore()
    const mockEvents = [
      { id: '1', title: 'Event 1' },
      { id: '2', title: 'Event 2' },
    ]

    vi.mocked(calendarApi.getEvents).mockResolvedValue({
      data: { data: { content: mockEvents, totalElements: 5, last: false } },
    } as any)

    await store.fetchEvents('2026-01-01', '2026-01-31')

    expect(store.events).toHaveLength(2)
    expect(store.totalEvents).toBe(5)
    expect(store.hasMore).toBe(true)
  })

  it('should set hasMore=false when last page', async () => {
    const store = useCalendarStore()

    vi.mocked(calendarApi.getEvents).mockResolvedValue({
      data: { data: { content: [{ id: '1' }], totalElements: 1, last: true } },
    } as any)

    await store.fetchEvents('2026-01-01', '2026-01-31')

    expect(store.hasMore).toBe(false)
  })

  it('should create event and prepend to list', async () => {
    const store = useCalendarStore()
    const newEvent = { id: 'new', title: 'New Event' }

    vi.mocked(calendarApi.createEvent).mockResolvedValue({
      data: { data: newEvent },
    } as any)

    const result = await store.createEvent({ title: 'New Event' } as any)

    expect(result.id).toBe('new')
    expect(store.events[0].id).toBe('new')
  })

  it('should update event in list and currentEvent', async () => {
    const store = useCalendarStore()
    store.events = [{ id: '1', title: 'Old' }] as any
    store.currentEvent = { id: '1', title: 'Old' } as any

    const updated = { id: '1', title: 'Updated' }
    vi.mocked(calendarApi.updateEvent).mockResolvedValue({
      data: { data: updated },
    } as any)

    await store.updateEvent('1', { title: 'Updated' })

    expect(store.events[0].title).toBe('Updated')
    expect(store.currentEvent?.title).toBe('Updated')
  })

  it('should cancel event and set cancelled=true', async () => {
    const store = useCalendarStore()
    store.events = [{ id: '1', cancelled: false }] as any
    store.currentEvent = { id: '1', cancelled: false } as any

    const cancelled = { id: '1', cancelled: true }
    vi.mocked(calendarApi.cancelEvent).mockResolvedValue({
      data: { data: cancelled },
    } as any)

    await store.cancelEvent('1')

    expect(store.events[0].cancelled).toBe(true)
    expect(store.currentEvent?.cancelled).toBe(true)
  })

  it('should delete event and remove from list', async () => {
    const store = useCalendarStore()
    store.events = [{ id: '1' }, { id: '2' }] as any

    vi.mocked(calendarApi.deleteEvent).mockResolvedValue({} as any)

    await store.deleteEvent('1')

    expect(store.events).toHaveLength(1)
    expect(store.events[0].id).toBe('2')
  })

  it('should update RSVP data on event', async () => {
    const store = useCalendarStore()
    store.events = [{ id: '1', currentUserRsvp: null, attendingCount: 0 }] as any
    store.currentEvent = { id: '1', currentUserRsvp: null, attendingCount: 0 } as any

    const updated = { id: '1', currentUserRsvp: 'ATTENDING', attendingCount: 1 }
    vi.mocked(calendarApi.rsvp).mockResolvedValue({
      data: { data: updated },
    } as any)

    await store.rsvp('1', 'ATTENDING')

    expect(store.events[0].currentUserRsvp).toBe('ATTENDING')
    expect(store.events[0].attendingCount).toBe(1)
    expect(store.currentEvent?.currentUserRsvp).toBe('ATTENDING')
  })

  it('should fetch room events', async () => {
    const store = useCalendarStore()
    const mockEvents = [{ id: 'r1', title: 'Room Event' }]

    vi.mocked(calendarApi.getRoomEvents).mockResolvedValue({
      data: { data: { content: mockEvents, totalElements: 1, last: true } },
    } as any)

    await store.fetchRoomEvents('room-123')

    expect(store.events).toHaveLength(1)
    expect(store.events[0].title).toBe('Room Event')
    expect(store.totalEvents).toBe(1)
    expect(store.hasMore).toBe(false)
  })
})
