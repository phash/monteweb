<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import PageTitle from '@/components/common/PageTitle.vue'

const { t } = useI18n()

const adminLinks = [
  { to: '/admin/users', icon: 'pi pi-users', labelKey: 'admin.dashboard.users', descKey: 'admin.dashboard.usersDesc' },
  { to: '/admin/rooms', icon: 'pi pi-th-large', labelKey: 'admin.dashboard.rooms', descKey: 'admin.dashboard.roomsDesc' },
  { to: '/admin/sections', icon: 'pi pi-sitemap', labelKey: 'admin.dashboard.sections', descKey: 'admin.dashboard.sectionsDesc' },
  { to: '/admin/families', icon: 'pi pi-heart', labelKey: 'admin.dashboard.families', descKey: 'admin.dashboard.familiesDesc' },
  { to: '/admin/billing', icon: 'pi pi-calculator', labelKey: 'admin.dashboard.billing', descKey: 'admin.dashboard.billingDesc' },
  { to: '/admin/modules', icon: 'pi pi-cog', labelKey: 'admin.dashboard.modules', descKey: 'admin.dashboard.modulesDesc' },
  { to: '/admin/error-reports', icon: 'pi pi-exclamation-triangle', labelKey: 'errorReports.title', descKey: 'errorReports.subtitle' },
  { to: '/admin/theme', icon: 'pi pi-palette', labelKey: 'admin.dashboard.theme', descKey: 'admin.dashboard.themeDesc' },
  { to: '/admin/settings', icon: 'pi pi-sliders-h', labelKey: 'admin.dashboard.settings', descKey: 'admin.dashboard.settingsDesc' },
  { to: '/admin/privacy', icon: 'pi pi-shield', labelKey: 'privacy.adminPrivacy', descKey: 'privacy.adminPrivacyDesc' },
]
</script>

<template>
  <div>
    <PageTitle :title="t('admin.title')" />

    <div class="admin-grid">
      <router-link
        v-for="link in adminLinks"
        :key="link.to"
        :to="link.to"
        class="admin-tile card"
      >
        <i :class="link.icon" class="tile-icon" />
        <h3>{{ t(link.labelKey) }}</h3>
        <p>{{ t(link.descKey) }}</p>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1rem;
}

.admin-tile {
  text-decoration: none;
  color: inherit;
  text-align: center;
  padding: 2rem 1.5rem;
  transition: box-shadow 0.15s, transform 0.15s;
}

.admin-tile:hover {
  box-shadow: var(--mw-shadow-md);
  transform: translateY(-2px);
  text-decoration: none;
}

.tile-icon {
  font-size: 2rem;
  color: var(--mw-primary);
  margin-bottom: 0.75rem;
}

.admin-tile h3 {
  font-size: var(--mw-font-size-md);
  margin-bottom: 0.25rem;
}

.admin-tile p {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
}
</style>
