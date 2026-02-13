<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useFormsStore } from '@/stores/forms'
import Tag from 'primevue/tag'
import Button from 'primevue/button'

const { t } = useI18n()
const { formatShortDate } = useLocaleDate()
const router = useRouter()
const forms = useFormsStore()

const pendingForms = computed(() =>
  forms.availableForms.filter(f => !f.hasUserResponded).slice(0, 5)
)

onMounted(() => {
  if (!forms.availableForms.length) {
    forms.fetchAvailableForms()
  }
})

function typeSeverity(type: string): 'info' | 'warn' {
  return type === 'CONSENT' ? 'warn' : 'info'
}

function scopeLabel(form: { scope: string; sectionNames: string[]; scopeName: string | null }) {
  if (form.sectionNames?.length) return form.sectionNames.join(', ')
  if (form.scopeName) return form.scopeName
  return t(`forms.scopes.${form.scope}`)
}
</script>

<template>
  <div v-if="pendingForms.length" class="forms-widget card">
    <div class="widget-header">
      <h3><i class="pi pi-list-check" /> {{ t('forms.pendingForms') }}</h3>
      <Button
        :label="t('forms.viewAll')"
        text
        size="small"
        @click="router.push({ name: 'forms' })"
      />
    </div>
    <div class="forms-list">
      <router-link
        v-for="form in pendingForms"
        :key="form.id"
        :to="{ name: 'form-detail', params: { id: form.id } }"
        class="form-entry"
      >
        <div class="form-entry-info">
          <div class="form-entry-title">
            <strong>{{ form.title }}</strong>
            <Tag :value="t(`forms.types.${form.type}`)" :severity="typeSeverity(form.type)" size="small" />
          </div>
          <div class="form-entry-meta">
            <span>{{ scopeLabel(form) }}</span>
            <span v-if="form.deadline" class="separator">·</span>
            <span v-if="form.deadline"><i class="pi pi-calendar" /> {{ formatShortDate(form.deadline) }}</span>
          </div>
        </div>
        <i class="pi pi-chevron-right form-entry-arrow" />
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.forms-widget {
  margin-bottom: 1.5rem;
}

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.widget-header h3 {
  font-size: var(--mw-font-size-md);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.forms-list {
  display: flex;
  flex-direction: column;
}

.form-entry {
  display: flex;
  align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--mw-border-light);
  text-decoration: none;
  color: inherit;
  transition: background 0.15s;
  gap: 0.75rem;
}

.form-entry:last-child {
  border-bottom: none;
}

.form-entry:hover {
  background: var(--mw-bg-hover);
}

.form-entry-info {
  flex: 1;
  min-width: 0;
}

.form-entry-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--mw-font-size-sm);
}

.form-entry-meta {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.15rem;
}

.separator {
  margin: 0 0.25rem;
}

.form-entry-arrow {
  color: var(--mw-text-muted);
  font-size: 0.75rem;
  flex-shrink: 0;
}
</style>
