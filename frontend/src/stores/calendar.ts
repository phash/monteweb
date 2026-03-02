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
  const totalRoomEvents = ref(0)
  const hasMoreRoom = ref(true)

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
    } catch (e) {
      console.error('Failed to fetch events:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchEvent(id: string) {
    try {
      const res = await calendarApi.getEvent(id)
      currentEvent.value = res.data.data
    } catch (e) {
      console.error('Failed to fetch event:', e)
      throw e
    }
  }

  async function createEvent(data: CreateEventRequest) {
    try {
      const res = await calendarApi.createEvent(data)
      events.value.unshift(res.data.data)
      return res.data.data
    } catch (e) {
      console.error('Failed to create event:', e)
      throw e
    }
  }

  async function updateEvent(id: string, data: UpdateEventRequest) {
    try {
      const res = await calendarApi.updateEvent(id, data)
      const idx = events.value.findIndex(e => e.id === id)
      if (idx !== -1) events.value[idx] = res.data.data
      if (currentEvent.value?.id === id) currentEvent.value = res.data.data
      return res.data.data
    } catch (e) {
      console.error('Failed to update event:', e)
      throw e
    }
  }

  async function cancelEvent(id: string) {
    try {
      const res = await calendarApi.cancelEvent(id)
      const idx = events.value.findIndex(e => e.id === id)
      if (idx !== -1) events.value[idx] = res.data.data
      if (currentEvent.value?.id === id) currentEvent.value = res.data.data
      return res.data.data
    } catch (e) {
      console.error('Failed to cancel event:', e)
      throw e
    }
  }

  async function deleteEvent(id: string) {
    try {
      await calendarApi.deleteEvent(id)
      events.value = events.value.filter(e => e.id !== id)
      if (currentEvent.value?.id === id) currentEvent.value = null
    } catch (e) {
      console.error('Failed to delete event:', e)
      throw e
    }
  }

  async function rsvp(id: string, status: RsvpStatus) {
    try {
      const res = await calendarApi.rsvp(id, status)
      const idx = events.value.findIndex(e => e.id === id)
      if (idx !== -1) events.value[idx] = res.data.data
      if (currentEvent.value?.id === id) currentEvent.value = res.data.data
      return res.data.data
    } catch (e) {
      console.error('Failed to RSVP:', e)
      throw e
    }
  }

  async function fetchRoomEvents(roomId: string, from?: string, to?: string, page = 0) {
    loading.value = true
    try {
      const res = await calendarApi.getRoomEvents(roomId, from, to, page)
      events.value = res.data.data.content
      totalRoomEvents.value = res.data.data.totalElements
      hasMoreRoom.value = !res.data.data.last
    } catch (e) {
      events.value = []
      totalRoomEvents.value = 0
      hasMoreRoom.value = false
      console.error('Failed to fetch room events:', e)
      throw e
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
    totalRoomEvents,
    hasMoreRoom,
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
