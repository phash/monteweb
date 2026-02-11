<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useNotificationsStore } from '@/stores/notifications'
import Badge from 'primevue/badge'
import Button from 'primevue/button'
import Popover from 'primevue/popover'

const { t } = useI18n()
const notifications = useNotificationsStore()
const router = useRouter()
const popover = ref()

let pollInterval: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  notifications.fetchUnreadCount()
  pollInterval = setInterval(() => notifications.fetchUnreadCount(), 30000)
})

onUnmounted(() => {
  if (pollInterval) clearInterval(pollInterval)
})

function toggle(event: Event) {
  popover.value.toggle(event)
  if (!notifications.notifications.length) {
    notifications.fetchNotifications()
  }
}

function handleClick(link: string | null, id: string) {
  notifications.markAsRead(id)
  popover.value.hide()
  if (link) router.push(link)
}

function formatTime(date: string) {
  const diff = Date.now() - new Date(date).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return t('notifications.justNow')
  if (minutes < 60) return t('notifications.minutesAgo', { n: minutes })
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return t('notifications.hoursAgo', { n: hours })
  const days = Math.floor(hours / 24)
  return t('notifications.daysAgo', { n: days })
}
</script>

<template>
  <div class="notification-bell">
    <Button
      icon="pi pi-bell"
      text
      rounded
      severity="secondary"
      :aria-label="notifications.unreadCount > 0
        ? t('notifications.bellWithCount', { count: notifications.unreadCount })
        : t('notifications.title')"
      @click="toggle"
      class="bell-button"
    />
    <Badge
      v-if="notifications.unreadCount > 0"
      :value="notifications.unreadCount > 99 ? '99+' : notifications.unreadCount"
      severity="danger"
      class="bell-badge"
    />

    <Popover ref="popover" class="notification-popover">
      <div class="notification-header">
        <strong>{{ t('notifications.title') }}</strong>
        <Button
          v-if="notifications.unreadCount > 0"
          :label="t('notifications.markAllRead')"
          text
          size="small"
          @click="notifications.markAllAsRead()"
        />
      </div>

      <div class="notification-list">
        <div
          v-for="n in notifications.notifications.slice(0, 10)"
          :key="n.id"
          class="notification-item"
          :class="{ unread: !n.read }"
          role="button"
          tabindex="0"
          @click="handleClick(n.link, n.id)"
          @keydown.enter="handleClick(n.link, n.id)"
        >
          <div class="notification-content">
            <strong>{{ n.title }}</strong>
            <p>{{ n.message }}</p>
          </div>
          <span class="notification-time">{{ formatTime(n.createdAt) }}</span>
        </div>

        <p v-if="!notifications.notifications.length" class="empty-state">
          {{ t('notifications.noNotifications') }}
        </p>
      </div>
    </Popover>
  </div>
</template>

<style scoped>
.notification-bell {
  position: relative;
  display: inline-flex;
}

.bell-badge {
  position: absolute;
  top: 0;
  right: 0;
  transform: translate(25%, -25%);
  pointer-events: none;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--mw-border-light);
  margin-bottom: 0.5rem;
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
  min-width: 300px;
}

.notification-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--mw-border-light);
  cursor: pointer;
}

.notification-item:hover {
  background: var(--mw-bg-hover);
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item.unread {
  background: var(--mw-bg-highlight, rgba(59, 130, 246, 0.05));
}

.notification-content strong {
  font-size: var(--mw-font-size-sm);
}

.notification-content p {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-secondary);
  margin-top: 0.125rem;
}

.notification-time {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  white-space: nowrap;
}

.empty-state {
  text-align: center;
  color: var(--mw-text-muted);
  padding: 1rem;
}
</style>
