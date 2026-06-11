<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useAdminStore } from '@/stores/admin'
import { useAuthStore } from '@/stores/auth'
import { useRoute, useRouter } from 'vue-router'
import { computed, ref, watch, nextTick, onBeforeUnmount } from 'vue'

const { t } = useI18n()
const admin = useAdminStore()
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const showMore = ref(false)
const moreMenuRef = ref<HTMLElement | null>(null)
const moreTriggerRef = ref<HTMLButtonElement | null>(null)

function closeMore() {
  showMore.value = false
}

function openMore() {
  showMore.value = true
}

function toggleMore() {
  if (showMore.value) closeMore()
  else openMore()
}

function onWindowKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    closeMore()
  }
}

function focusMenuItemAt(index: number) {
  const items = moreMenuRef.value?.querySelectorAll<HTMLButtonElement>('.more-menu-item')
  if (!items || items.length === 0) return
  const clamped = (index + items.length) % items.length
  items[clamped]?.focus()
}

function onMenuKeydown(event: KeyboardEvent) {
  const items = moreMenuRef.value?.querySelectorAll<HTMLButtonElement>('.more-menu-item')
  if (!items || items.length === 0) return
  const current = Array.from(items).indexOf(document.activeElement as HTMLButtonElement)
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    focusMenuItemAt(current < 0 ? 0 : current + 1)
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    focusMenuItemAt(current < 0 ? items.length - 1 : current - 1)
  }
}

// Manage focus + global Escape listener when the more menu opens/closes
watch(showMore, async (open) => {
  if (open) {
    window.addEventListener('keydown', onWindowKeydown)
    await nextTick()
    focusMenuItemAt(0)
  } else {
    window.removeEventListener('keydown', onWindowKeydown)
    moreTriggerRef.value?.focus()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onWindowKeydown)
})

const primaryItems = computed(() => [
  { to: '/', icon: 'pi pi-home', label: t('nav.dashboard'), name: 'dashboard' },
  { to: '/rooms', icon: 'pi pi-th-large', label: t('nav.rooms'), name: 'rooms' },
  ...(auth.canHaveFamily
    ? [{ to: '/family', icon: 'pi pi-users', label: t('nav.family'), name: 'family' }]
    : []),
  ...(admin.isModuleEnabled('messaging')
    ? [{ to: '/messages', icon: 'pi pi-comments', label: t('nav.messages'), name: 'messages' }]
    : []),
])

const moreItems = computed(() => {
  const items: { to: string; icon: string; label: string; name: string }[] = []

  if (!admin.isModuleEnabled('directoryAdminOnly') || auth.isAdmin) {
    items.push({ to: '/directory', icon: 'pi pi-address-book', label: t('nav.directory'), name: 'directory' })
  }

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
  if (admin.isModuleEnabled('bookmarks')) {
    items.push({ to: '/bookmarks', icon: 'pi pi-bookmark', label: t('nav.bookmarks'), name: 'bookmarks' })
  }

  items.push({ to: '/profile', icon: 'pi pi-user', label: t('nav.profile'), name: 'profile' })

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
  closeMore()
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
      <div v-if="showMore" class="more-overlay" @click="closeMore" />
    </Transition>

    <!-- More menu panel -->
    <Transition name="slide-up">
      <div v-if="showMore" ref="moreMenuRef" class="more-menu" role="menu" @keydown="onMenuKeydown">
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
    <nav class="bottom-nav" :aria-label="t('nav.mainNavigation')">
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
        ref="moreTriggerRef"
        class="bottom-nav-item"
        :class="{ active: isMoreActive }"
        :aria-expanded="showMore"
        :aria-haspopup="true"
        :aria-label="t('nav.more')"
        @click="toggleMore"
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
  /* Dark text + yellow-tinted highlight; a dark-amber icon keeps the accent
     visible (yellow text/icon on white is only 1.84:1). */
  color: var(--mw-text);
  font-weight: 600;
  background-color: var(--mw-bg-active);
}

.bottom-nav-item.active i {
  color: var(--mw-primary-dark);
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
  color: var(--mw-text);
  font-weight: 600;
  background-color: var(--mw-bg-active);
}

.more-menu-item.active i {
  color: var(--mw-primary-dark);
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
