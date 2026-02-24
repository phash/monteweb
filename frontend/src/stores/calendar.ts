import { defineStore } from 'pinia'
import { ref } from 'vue'
import { calendarApi } from '@/api/calendar.api'
import type { CalendarEvent, CreateEventRequest, UpdateEventRequest, RsvpStatus, ICalEvent } from '@/types/calendar'

export const useCalendarStore = defineStore('calendar', () => {
  const events = ref<CalendarEvent[]>([])
  const icalEvents = ref<ICalEvent[]>([])
  const currentEvent = ref<CalendarEvent | null>(null)
  const loading = ref(false)
  const totalEvents = ref(0)
  const hasMore = ref(true)

  async function fetchEvents(from: string, to: string, reset = true, page = 0) {
    loading.value = true
    try {
      const res = await calendarApi.getEvents(from, to, page)
      if (reset) {
        events.value = res.data.data.content
      } else {
        events.value = [...events.value, ...res.data.data.content]
      }
      totalEvents.value = res.data.data.totalElements
      hasMore.value = !res.data.data.last
    } finally {
      loading.value = false
    }
  }

  async function fetchEvent(id: string) {
    const res = await calendarApi.getEvent(id)
    currentEvent.value = res.data.data
  }

  async function createEvent(data: CreateEventRequest) {
    const res = await calendarApi.createEvent(data)
    events.value.unshift(res.data.data)
    return res.data.data
  }

  async function updateEvent(id: string, data: UpdateEventRequest) {
    const res = await calendarApi.updateEvent(id, data)
    const idx = events.value.findIndex(e => e.id === id)
    if (idx !== -1) events.value[idx] = res.data.data
    if (currentEvent.value?.id === id) currentEvent.value = res.data.data
    return res.data.data
  }

  async function cancelEvent(id: string) {
    const res = await calendarApi.cancelEvent(id)
    const idx = events.value.findIndex(e => e.id === id)
    if (idx !== -1) events.value[idx] = res.data.data
    if (currentEvent.value?.id === id) currentEvent.value = res.data.data
    return res.data.data
  }

  async function deleteEvent(id: string) {
    await calendarApi.deleteEvent(id)
    events.value = events.value.filter(e => e.id !== id)
    if (currentEvent.value?.id === id) currentEvent.value = null
  }

  async function rsvp(id: string, status: RsvpStatus) {
    const res = await calendarApi.rsvp(id, status)
    const idx = events.value.findIndex(e => e.id === id)
    if (idx !== -1) events.value[idx] = res.data.data
    if (currentEvent.value?.id === id) currentEvent.value = res.data.data
    return res.data.data
  }

  async function fetchRoomEvents(roomId: string, from?: string, to?: string, page = 0) {
    loading.value = true
    try {
      const res = await calendarApi.getRoomEvents(roomId, from, to, page)
      events.value = res.data.data.content
      totalEvents.value = res.data.data.totalElements
      hasMore.value = !res.data.data.last
    } catch {
      events.value = []
      totalEvents.value = 0
      hasMore.value = false
    } finally {
      loading.value = false
    }
  }

  async function fetchICalEvents(from: string, to: string) {
    try {
      const res = await calendarApi.getICalEvents(from, to)
      icalEvents.value = res.data.data
    } catch {
      icalEvents.value = []
    }
  }

  return {
    events,
    icalEvents,
    currentEvent,
    loading,
    totalEvents,
    hasMore,
    fetchEvents,
    fetchEvent,
    createEvent,
    updateEvent,
    cancelEvent,
    deleteEvent,
    rsvp,
    fetchRoomEvents,
    fetchICalEvents,
  }
})
