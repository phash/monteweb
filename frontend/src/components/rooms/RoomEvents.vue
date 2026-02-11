<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useCalendarStore } from '@/stores/calendar'
import { useAuthStore } from '@/stores/auth'
import { useRoomsStore } from '@/stores/rooms'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'

const props = defineProps<{ roomId: string }>()
const { t } = useI18n()
const router = useRouter()
const calendar = useCalendarStore()
const auth = useAuthStore()
const rooms = useRoomsStore()

const isLeader = (() => {
  const member = rooms.currentRoom?.members?.find(m => m.userId === auth.user?.id)
  return member?.role === 'LEADER' || auth.isAdmin
})()

onMounted(async () => {
  try {
    await calendar.fetchRoomEvents(props.roomId)
  } catch {
    // Calendar events not available
  }
})

function formatEventDate(event: { allDay: boolean; startDate: string; startTime: string | null; endDate: string }) {
  const start = new Date(event.startDate)
  const startStr = start.toLocaleDateString('de-DE', { weekday: 'short', day: '2-digit', month: '2-digit' })

  if (event.allDay) {
    if (event.startDate === event.endDate) return startStr
    const end = new Date(event.endDate)
    return `${startStr} - ${end.toLocaleDateString('de-DE', { weekday: 'short', day: '2-digit', month: '2-digit' })}`
  }

  const time = event.startTime ? ` ${event.startTime.substring(0, 5)}` : ''
  return `${startStr}${time}`
}
</script>

<template>
  <div>
    <div class="events-header">
      <Button
        v-if="isLeader"
        :label="t('calendar.createEvent')"
        icon="pi pi-plus"
        size="small"
        @click="router.push({ name: 'calendar-create', query: { roomId } })"
      />
    </div>

    <LoadingSpinner v-if="calendar.loading && !calendar.events.length" />
    <EmptyState
      v-else-if="!calendar.events.length"
      icon="pi pi-calendar"
      :message="t('calendar.noEvents')"
    />
    <div v-else class="event-list">
      <div
        v-for="event in calendar.events"
        :key="event.id"
        class="event-item card"
        :class="{ cancelled: event.cancelled }"
        @click="router.push({ name: 'event-detail', params: { id: event.id } })"
      >
        <div class="event-info">
          <div class="event-title-row">
            <strong>{{ event.title }}</strong>
            <Tag v-if="event.cancelled" :value="t('calendar.cancelled')" severity="danger" size="small" />
          </div>
          <div class="event-meta">
            <span>{{ formatEventDate(event) }}</span>
            <span v-if="event.location" class="separator">·</span>
            <span v-if="event.location">{{ event.location }}</span>
            <span v-if="event.attendingCount > 0" class="separator">·</span>
            <span v-if="event.attendingCount > 0">{{ t('calendar.attendingCount', { n: event.attendingCount }) }}</span>
          </div>
        </div>
        <i class="pi pi-chevron-right event-arrow" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.events-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1rem;
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.event-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: background 0.15s;
}

.event-item:hover {
  background: var(--mw-bg-hover);
}

.event-item.cancelled {
  opacity: 0.6;
}

.event-info {
  flex: 1;
  min-width: 0;
}

.event-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.event-meta {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.25rem;
}

.separator {
  margin: 0 0.25rem;
}

.event-arrow {
  color: var(--mw-text-muted);
  font-size: 0.75rem;
}
</style>
