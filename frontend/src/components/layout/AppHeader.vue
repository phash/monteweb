<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Menu from 'primevue/menu'
import Tag from 'primevue/tag'
import Popover from 'primevue/popover'
import NotificationBell from '@/components/common/NotificationBell.vue'
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue'
import { ref, computed } from 'vue'
import type { UserRole } from '@/types/user'

const { t } = useI18n()
const auth = useAuthStore()
const admin = useAdminStore()
const router = useRouter()
const toast = useToast()
const userMenu = ref()
const rolePopover = ref()
const switching = ref(false)

const menuItems = ref([
  {
    label: t('nav.profile'),
    icon: 'pi pi-user',
    command: () => router.push({ name: 'profile' }),
  },
  { separator: true },
  {
    label: t('nav.logout'),
    icon: 'pi pi-sign-out',
    command: async () => {
      await auth.logout()
      router.push({ name: 'login' })
    },
  },
])

function toggleUserMenu(event: Event) {
  userMenu.value.toggle(event)
}

function roleSeverity(role: string): string {
  const map: Record<string, string> = {
    SUPERADMIN: 'danger',
    SECTION_ADMIN: 'warn',
    TEACHER: 'info',
    PARENT: 'success',
    STUDENT: 'secondary',
  }
  return map[role] ?? 'secondary'
}

const otherRoles = computed(() =>
  auth.assignedRoles.filter(r => r !== auth.user?.role)
)

function toggleRolePopover(event: Event) {
  rolePopover.value.toggle(event)
}

async function onSwitchRole(role: string) {
  switching.value = true
  try {
    await auth.switchRole(role as UserRole)
    rolePopover.value.hide()
    toast.add({ severity: 'success', summary: t('profile.roleSwitched', { role: t('profile.roleLabels.' + role) }), life: 3000 })
    router.go(0)
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  } finally {
    switching.value = false
  }
}
</script>

<template>
  <header class="app-header">
    <div class="header-left">
      <router-link to="/" class="header-logo">
        {{ admin.config?.schoolName ?? 'MonteWeb' }}
      </router-link>
    </div>

    <div class="header-right">
      <Tag
        v-if="auth.user?.role"
        :value="t('profile.roleLabels.' + auth.user.role)"
        :severity="roleSeverity(auth.user.role) as any"
        class="role-badge"
        :class="{ clickable: auth.canSwitchRole }"
        :aria-label="auth.canSwitchRole ? t('profile.switchRole') : undefined"
        @click="auth.canSwitchRole ? toggleRolePopover($event) : undefined"
      />
      <Popover ref="rolePopover" v-if="auth.canSwitchRole">
        <div class="role-switcher-popover">
          <p class="role-switcher-title">{{ t('profile.switchRole') }}</p>
          <div class="role-switcher-list">
            <button
              v-for="role in otherRoles"
              :key="role"
              class="role-switcher-item"
              :disabled="switching"
              @click="onSwitchRole(role)"
            >
              <Tag :value="t('profile.roleLabels.' + role)" :severity="roleSeverity(role) as any" />
            </button>
          </div>
        </div>
      </Popover>
      <LanguageSwitcher />
      <NotificationBell />
      <Button
        :label="auth.user?.displayName ?? ''"
        icon="pi pi-user"
        severity="secondary"
        text
        :aria-label="t('nav.profile')"
        @click="toggleUserMenu"
        class="user-menu-button"
      />
      <Menu ref="userMenu" :model="menuItems" popup />
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--mw-header-height);
  padding: 0 1rem;
  background: var(--mw-bg-card);
  border-bottom: 1px solid var(--mw-border-light);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  min-width: 0;
}

.header-logo {
  font-size: var(--mw-font-size-lg);
  font-weight: 700;
  color: var(--mw-primary);
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-shrink: 0;
}

.role-badge.clickable {
  cursor: pointer;
}

.role-switcher-popover {
  padding: 0.5rem;
  min-width: 150px;
}

.role-switcher-title {
  font-size: var(--mw-font-size-sm);
  font-weight: 600;
  margin: 0 0 0.5rem 0;
  color: var(--mw-text-secondary);
}

.role-switcher-list {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.role-switcher-item {
  background: none;
  border: none;
  padding: 0.25rem;
  cursor: pointer;
  border-radius: var(--p-border-radius);
  text-align: left;
}

.role-switcher-item:hover {
  background: var(--p-surface-100);
}

.role-switcher-item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 767px) {
  .user-menu-button :deep(.p-button-label) {
    display: none;
  }
}
</style>
