<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Menu from 'primevue/menu'
import NotificationBell from '@/components/common/NotificationBell.vue'
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue'
import { ref } from 'vue'

const { t } = useI18n()
const auth = useAuthStore()
const admin = useAdminStore()
const router = useRouter()
const userMenu = ref()

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
</script>

<template>
  <header class="app-header">
    <div class="header-left">
      <router-link to="/" class="header-logo">
        {{ admin.config?.schoolName ?? 'MonteWeb' }}
      </router-link>
    </div>

    <div class="header-right">
      <LanguageSwitcher />
      <NotificationBell />
      <Button
        :label="auth.user?.displayName ?? ''"
        icon="pi pi-user"
        severity="secondary"
        text
        @click="toggleUserMenu"
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

.header-logo {
  font-size: var(--mw-font-size-lg);
  font-weight: 700;
  color: var(--mw-primary);
  text-decoration: none;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
</style>
