<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const { t } = useI18n()
const auth = useAuthStore()
const admin = useAdminStore()
const route = useRoute()

const navItems = computed(() => {
  const items = [
    { to: '/', icon: 'pi pi-home', label: t('nav.dashboard'), name: 'dashboard' },
    { to: '/rooms', icon: 'pi pi-th-large', label: t('nav.rooms'), name: 'rooms' },
    { to: '/family', icon: 'pi pi-users', label: t('nav.family'), name: 'family' },
  ]

  if (admin.isModuleEnabled('messaging')) {
    items.push({ to: '/messages', icon: 'pi pi-comments', label: t('nav.messages'), name: 'messages' })
  }

  items.push({ to: '/profile', icon: 'pi pi-user', label: t('nav.profile'), name: 'profile' })

  return items
})

function isActive(item: { to: string }) {
  if (item.to === '/') return route.name === 'dashboard'
  return route.path.startsWith(item.to)
}
</script>

<template>
  <nav class="bottom-nav">
    <router-link
      v-for="item in navItems"
      :key="item.name"
      :to="item.to"
      class="bottom-nav-item"
      :class="{ active: isActive(item) }"
    >
      <i :class="item.icon" />
      <span>{{ item.label }}</span>
    </router-link>
  </nav>
</template>

<style scoped>
.bottom-nav {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: var(--mw-bottom-nav-height);
  background: var(--mw-bg-card);
  border-top: 1px solid var(--mw-border-light);
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
}

.bottom-nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.125rem;
  padding: 0.375rem 0.75rem;
  color: var(--mw-text-muted);
  text-decoration: none;
  font-size: var(--mw-font-size-xs);
  transition: color 0.15s;
}

.bottom-nav-item:hover {
  text-decoration: none;
}

.bottom-nav-item.active {
  color: var(--mw-primary);
}

.bottom-nav-item i {
  font-size: 1.25rem;
}
</style>
