<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Drawer from 'primevue/drawer'
import Button from 'primevue/button'
import { useContextHelp } from '@/composables/useContextHelp'
import { useRouter } from 'vue-router'

const { t } = useI18n()
const router = useRouter()
const { pageTitle, actions, tips, hasHelp } = useContextHelp()

const visible = ref(false)

function openHelp() {
  visible.value = true
}

function goToHandbook() {
  visible.value = false
  if (router.currentRoute.value.name === 'help') {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } else {
    router.push('/help')
  }
}
</script>

<template>
  <button
    class="help-fab"
    :aria-label="t('help.contextHelp')"
    :title="t('help.contextHelp')"
    @click="openHelp"
  >
    <i class="pi pi-question-circle" />
  </button>

  <Drawer
    v-model:visible="visible"
    :header="pageTitle"
    position="right"
    class="help-drawer"
  >
    <div v-if="hasHelp" class="help-content">
      <div v-if="actions.length" class="help-section">
        <h3 class="help-section-title">
          <i class="pi pi-check-circle" />
          {{ t('help.whatCanIDo') }}
        </h3>
        <ul class="help-list">
          <li v-for="(action, idx) in actions" :key="idx">{{ action }}</li>
        </ul>
      </div>

      <div v-if="tips.length" class="help-section">
        <h3 class="help-section-title">
          <i class="pi pi-lightbulb" />
          {{ t('help.tips') }}
        </h3>
        <ul class="help-list help-list--tips">
          <li v-for="(tip, idx) in tips" :key="idx">{{ tip }}</li>
        </ul>
      </div>
    </div>

    <div v-else class="help-empty">
      <i class="pi pi-info-circle help-empty-icon" />
      <p>{{ t('help.noContextHelp') }}</p>
    </div>

    <div class="help-footer">
      <Button
        :label="t('help.openHandbook')"
        icon="pi pi-book"
        severity="secondary"
        outlined
        class="w-full"
        @click="goToHandbook"
      />
    </div>
  </Drawer>
</template>

<style scoped>
.help-fab {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--mw-primary);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--mw-shadow-lg);
  z-index: 50;
  transition: transform 0.15s, box-shadow 0.15s;
}

.help-fab:hover {
  transform: scale(1.08);
  box-shadow: var(--mw-shadow-xl, 0 20px 25px -5px rgba(0,0,0,0.1));
}

.help-fab:active {
  transform: scale(0.95);
}

.help-fab:focus-visible {
  outline: 2px solid var(--mw-primary, #4f46e5);
  outline-offset: 2px;
}

.help-fab .pi {
  font-size: 1.5rem;
}

@media (max-width: 767px) {
  .help-fab {
    display: none;
  }
}

.help-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.help-section-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--mw-font-size-base);
  font-weight: 600;
  color: var(--mw-text);
  margin: 0 0 0.75rem;
}

.help-section-title .pi-check-circle {
  color: var(--mw-primary);
}

.help-section-title .pi-lightbulb {
  color: #f59e0b;
}

.help-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.help-list li {
  position: relative;
  padding-left: 1.25rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  line-height: 1.5;
}

.help-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.55em;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--mw-primary);
}

.help-list--tips li::before {
  background: #f59e0b;
}

.help-empty {
  text-align: center;
  padding: 2rem 1rem;
  color: var(--mw-text-muted);
}

.help-empty-icon {
  font-size: 2.5rem;
  margin-bottom: 0.75rem;
  display: block;
}

.help-footer {
  margin-top: auto;
  padding-top: 1.5rem;
  border-top: 1px solid var(--mw-border-light);
}

.w-full {
  width: 100%;
}
</style>
