<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { useFamilyStore } from '@/stores/family'
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const { t } = useI18n()
const auth = useAuthStore()
const admin = useAdminStore()
const familyStore = useFamilyStore()
const route = useRoute()

const navItems = computed(() => {
  const items = [
    { to: '/', icon: 'pi pi-home', label: t('nav.dashboard'), name: 'dashboard' },
    { to: '/rooms', icon: 'pi pi-th-large', label: t('nav.rooms'), name: 'rooms' },
  ]

  if (!admin.isModuleEnabled('directoryAdminOnly') || auth.isAdmin) {
    items.push({ to: '/directory', icon: 'pi pi-address-book', label: t('nav.directory'), name: 'directory' })
  }

  if (auth.canHaveFamily) {
    items.push({ to: '/family', icon: 'pi pi-users', label: t('nav.family'), name: 'family' })
  }

  if (admin.isModuleEnabled('messaging')) {
    items.push({ to: '/messages', icon: 'pi pi-comments', label: t('nav.messages'), name: 'messages' })
  }
  if (admin.isModuleEnabled('jobboard') && (auth.isAdmin || auth.isTeacher || auth.isSectionAdmin || familyStore.hasFamily)) {
    items.push({ to: '/jobs', icon: 'pi pi-briefcase', label: t('nav.jobs'), name: 'jobs' })
  }
  if (admin.isModuleEnabled('cleaning')) {
    items.push({ to: '/cleaning', icon: 'pi pi-calendar', label: t('nav.cleaning'), name: 'cleaning' })
  }
  if (admin.isModuleEnabled('calendar')) {
    items.push({ to: '/calendar', icon: 'pi pi-calendar-plus', label: t('nav.calendar'), name: 'calendar' })
  }
  if (admin.isModuleEnabled('forms')) {
    items.push({ to: '/forms', icon: 'pi pi-list-check', label: t('nav.forms'), name: 'forms' })
  }
  if (admin.isModuleEnabled('fundgrube')) {
    items.push({ to: '/fundgrube', icon: 'pi pi-box', label: t('nav.fundgrube'), name: 'fundgrube' })
  }
  if (admin.isModuleEnabled('bookmarks')) {
    items.push({ to: '/bookmarks', icon: 'pi pi-bookmark', label: t('nav.bookmarks'), name: 'bookmarks' })
  }

  if (auth.isSectionAdmin && !auth.isAdmin) {
    items.push({ to: '/section-admin', icon: 'pi pi-sitemap', label: t('sectionAdmin.title'), name: 'section-admin' })
  }

  if (auth.isPutzOrga && !auth.isAdmin) {
    items.push({ to: '/admin/cleaning', icon: 'pi pi-wrench', label: t('cleaning.admin.putzOrgaManagement'), name: 'admin-cleaning' })
  }

  if (auth.isAdmin) {
    items.push({ to: '/admin', icon: 'pi pi-cog', label: t('nav.admin'), name: 'admin' })
  }

  items.push({ to: '/help', icon: 'pi pi-question-circle', label: t('help.nav'), name: 'help' })

  return items
})

function isActive(item: { to: string; name: string }) {
  if (item.to === '/') return route.name === 'dashboard'
  // Exact segment match: /rooms should not match /rooms/discover
  const path = route.path
  if (path === item.to) return true
  return path.startsWith(item.to + '/')
}
</script>

<template>
  <aside class="app-sidebar" :aria-label="t('nav.mainNavigation')">
    <nav class="sidebar-nav">
      <router-link
        v-for="item in navItems"
        :key="item.name"
        :to="item.to"
        class="nav-item"
        :class="{ active: isActive(item) }"
        :aria-current="isActive(item) ? 'page' : undefined"
        active-class=""
      >
        <i :class="item.icon" />
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<style scoped>
.app-sidebar {
  width: var(--mw-sidebar-width);
  background: var(--mw-bg-sidebar);
  border-right: 1px solid var(--mw-border-light);
  height: calc(100vh - var(--mw-header-height));
  position: sticky;
  top: var(--mw-header-height);
  overflow-y: auto;
  padding: 1rem 0;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1.25rem;
  color: var(--mw-text-secondary);
  text-decoration: none;
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
  border-radius: 0;
  transition: background-color 0.15s, color 0.15s;
}

.nav-item:hover {
  background-color: var(--mw-border-light);
  color: var(--mw-text);
  text-decoration: none;
}

.nav-item.active {
  color: var(--mw-primary);
  background-color: color-mix(in srgb, var(--mw-primary) 8%, transparent);
  border-right: 3px solid var(--mw-primary);
}

.nav-item i {
  font-size: 1.125rem;
  width: 1.25rem;
  text-align: center;
}
</style>
