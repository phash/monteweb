<script setup lang="ts">
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
import BottomNav from './BottomNav.vue'
import ErrorBoundary from '@/components/common/ErrorBoundary.vue'
</script>

<template>
  <div class="app-layout">
    <AppHeader />
    <div class="app-body">
      <AppSidebar class="hide-mobile" />
      <main class="app-main">
        <ErrorBoundary>
          <router-view />
        </ErrorBoundary>
      </main>
    </div>
    <BottomNav class="hide-desktop" />
  </div>
</template>

<style scoped>
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

@media (max-width: 767px) {
  .app-main {
    padding: 1rem;
    padding-bottom: calc(var(--mw-bottom-nav-height) + 1rem);
  }
}
</style>
