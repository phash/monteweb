<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

declare const __APP_VERSION__: string
declare const __BUILD_TIME__: string
declare const __GIT_BRANCH__: string

const version = __APP_VERSION__
const buildTime = new Date(__BUILD_TIME__).toLocaleDateString('de-DE', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})
const branch = __GIT_BRANCH__
</script>

<template>
  <footer class="app-footer">
    <div class="footer-content">
      <span class="version">MonteWeb v{{ version }} ({{ branch }}) &middot; {{ t('footer.deployed') }} {{ buildTime }}</span>
      <span class="powered">
        Powered by
        <a href="https://github.com/phash/monteweb" target="_blank" rel="noopener noreferrer">phash.de</a>
      </span>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  border-top: 1px solid var(--p-surface-200);
  padding: 0.75rem 1.5rem;
  background: var(--p-surface-50);
  font-size: 0.75rem;
  color: var(--p-text-muted-color, var(--p-surface-500));
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.powered a {
  color: var(--mw-primary);
  text-decoration: none;
}

.powered a:hover {
  text-decoration: underline;
}

@media (max-width: 767px) {
  .app-footer {
    padding-bottom: calc(var(--mw-bottom-nav-height) + 0.75rem);
  }

  .footer-content {
    flex-direction: column;
    text-align: center;
    gap: 0.25rem;
  }
}
</style>
