<script setup lang="ts">
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
import BottomNav from './BottomNav.vue'
import AppBreadcrumb from '@/components/common/AppBreadcrumb.vue'
import ErrorBoundary from '@/components/common/ErrorBoundary.vue'
import AppFooter from './AppFooter.vue'
import HelpButton from '@/components/common/HelpButton.vue'
import PwaInstallBanner from '@/components/common/PwaInstallBanner.vue'
import OfflineBanner from '@/components/common/OfflineBanner.vue'
</script>

<template>
  <div class="app-layout">
    <a href="#main-content" class="skip-link">{{ $t('common.skipToContent', 'Zum Inhalt springen') }}</a>
    <PwaInstallBanner />
    <OfflineBanner />
    <AppHeader />
    <div class="app-body">
      <AppSidebar class="hide-mobile" />
      <main id="main-content" class="app-main" tabindex="-1">
        <AppBreadcrumb />
        <ErrorBoundary>
          <router-view />
        </ErrorBoundary>
      </main>
    </div>
    <AppFooter class="hide-mobile" />
    <BottomNav class="hide-desktop" />
    <HelpButton />
  </div>
</template>

<style scoped>
.skip-link {
  position: absolute;
  left: -9999px;
  top: auto;
  width: 1px;
  height: 1px;
  overflow: hidden;
}

.skip-link:focus {
  position: fixed;
  top: 0;
  left: 0;
  width: auto;
  height: auto;
  padding: 0.75rem 1.5rem;
  background: var(--mw-primary);
  color: white;
  z-index: 9999;
  font-weight: 600;
  text-decoration: none;
}

.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-body {
  display: flex;
  flex: 1;
}

.app-main {
  flex: 1;
  padding: 1.5rem;
  max-width: 100%;
  overflow-x: hidden;
}

.app-main > * {
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
}

@media (min-width: 768px) and (max-width: 1400px) {
  .app-main {
    padding: 1rem;
  }
}

@media (max-width: 767px) {
  .app-main {
    padding: 1rem;
    padding-bottom: calc(var(--mw-bottom-nav-height) + 1rem);
  }
}
</style>
