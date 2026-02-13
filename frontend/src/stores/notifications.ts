import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationsApi } from '@/api/notifications.api'
import type { NotificationInfo } from '@/types/notification'

export const useNotificationsStore = defineStore('notifications', () => {
  const notifications = ref<NotificationInfo[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)

  async function fetchNotifications() {
    loading.value = true
    try {
      const res = await notificationsApi.getNotifications()
      notifications.value = res.data.data.content
    } finally {
      loading.value = false
    }
  }

  async function fetchUnreadCount() {
    try {
      const res = await notificationsApi.getUnreadCount()
      unreadCount.value = res.data.data.count
    } catch {
      // ignore
    }
  }

  async function markAsRead(id: string) {
    await notificationsApi.markAsRead(id)
    const n = notifications.value.find(n => n.id === id)
    if (n && !n.read) {
      n.read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  async function markAllAsRead() {
    await notificationsApi.markAllAsRead()
    notifications.value.forEach(n => { n.read = true })
    unreadCount.value = 0
  }

  async function deleteNotification(id: string) {
    await notificationsApi.deleteNotification(id)
    const idx = notifications.value.findIndex(n => n.id === id)
    if (idx !== -1) {
      const notification = notifications.value[idx]
      if (notification && !notification.read) {
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
      notifications.value.splice(idx, 1)
    }
  }

  function addNotification(notification: NotificationInfo) {
    notifications.value.unshift(notification)
    if (!notification.read) unreadCount.value++
  }

  return {
    notifications,
    unreadCount,
    loading,
    fetchNotifications,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    addNotification,
  }
})
