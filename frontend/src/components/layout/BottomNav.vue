<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useAdminStore } from '@/stores/admin'
import { useAuthStore } from '@/stores/auth'
import { useRoute, useRouter } from 'vue-router'
import { computed, ref, watch } from 'vue'

const { t } = useI18n()
const admin = useAdminStore()
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const showMore = ref(false)

const primaryItems = computed(() => [
  { to: '/', icon: 'pi pi-home', label: t('nav.dashboard'), name: 'dashboard' },
  { to: '/rooms', icon: 'pi pi-th-large', label: t('nav.rooms'), name: 'rooms' },
  { to: '/family', icon: 'pi pi-users', label: t('nav.family'), name: 'family' },
  ...(admin.isModuleEnabled('messaging')
    ? [{ to: '/messages', icon: 'pi pi-comments', label: t('nav.messages'), name: 'messages' }]
    : []),
])

const moreItems = computed(() => {
  const items: { to: string; icon: string; label: string; name: string }[] = []

  if (admin.isModuleEnabled('jobboard')) {
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

  items.push({ to: '/profile', icon: 'pi pi-user', label: t('nav.profile'), name: 'profile' })

  if (auth.isPutzOrga && !auth.isAdmin) {
    items.push({ to: '/admin/cleaning', icon: 'pi pi-wrench', label: t('cleaning.admin.putzOrgaManagement'), name: 'admin-cleaning' })
  }

  if (auth.isAdmin) {
    items.push({ to: '/admin', icon: 'pi pi-cog', label: t('nav.admin'), name: 'admin' })
  }

  return items
})

const isMoreActive = computed(() =>
  moreItems.value.some(item => isActive(item))
)

function isActive(item: { to: string }) {
  if (item.to === '/') return route.name === 'dashboard'
  const path = route.path
  if (path === item.to) return true
  return path.startsWith(item.to + '/')
}

function navigateTo(to: string) {
  showMore.value = false
  router.push(to)
}

// Close more menu on route change
watch(() => route.path, () => {
  showMore.value = false
})
</script>

<template>
  <div>
    <!-- More menu overlay -->
    <Transition name="fade">
      <div v-if="showMore" class="more-overlay" @click="showMore = false" />
    </Transition>

    <!-- More menu panel -->
    <Transition name="slide-up">
      <div v-if="showMore" class="more-menu" role="menu">
        <button
          v-for="item in moreItems"
          :key="item.name"
          class="more-menu-item"
          :class="{ active: isActive(item) }"
          role="menuitem"
          @click="navigateTo(item.to)"
        >
          <i :class="item.icon" />
          <span>{{ item.label }}</span>
        </button>
      </div>
    </Transition>

    <!-- Bottom navigation bar -->
    <nav class="bottom-nav" aria-label="Hauptnavigation">
      <router-link
        v-for="item in primaryItems"
        :key="item.name"
        :to="item.to"
        class="bottom-nav-item"
        :class="{ active: isActive(item) }"
        :aria-current="isActive(item) ? 'page' : undefined"
        active-class=""
      >
        <i :class="item.icon" />
        <span>{{ item.label }}</span>
      </router-link>

      <button
        class="bottom-nav-item"
        :class="{ active: isMoreActive }"
        :aria-expanded="showMore"
        :aria-label="t('nav.more')"
        @click="showMore = !showMore"
      >
        <i class="pi pi-ellipsis-h" />
        <span>{{ t('nav.more') }}</span>
      </button>
    </nav>
  </div>
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
  min-width: 44px;
  min-height: 44px;
  justify-content: center;
  color: var(--mw-text-muted);
  text-decoration: none;
  font-size: var(--mw-font-size-xs);
  transition: color 0.15s;
  background: none;
  border: none;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
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

/* More menu overlay */
.more-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 99;
}

/* More menu panel */
.more-menu {
  position: fixed;
  bottom: var(--mw-bottom-nav-height);
  left: 0;
  right: 0;
  background: var(--mw-bg-card);
  border-top: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-lg) var(--mw-border-radius-lg) 0 0;
  box-shadow: var(--mw-shadow-lg);
  padding: 0.5rem 0;
  z-index: 101;
  max-height: 60vh;
  overflow-y: auto;
}

.more-menu-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.75rem 1.25rem;
  min-height: 44px;
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
  color: var(--mw-text-secondary);
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  -webkit-tap-highlight-color: transparent;
}

.more-menu-item:active {
  background-color: var(--mw-border-light);
}

.more-menu-item.active {
  color: var(--mw-primary);
}

.more-menu-item i {
  font-size: 1.125rem;
  width: 1.25rem;
  text-align: center;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.25s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}
</style>
