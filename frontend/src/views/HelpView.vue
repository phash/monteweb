<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import Accordion from 'primevue/accordion'
import AccordionPanel from 'primevue/accordionpanel'
import AccordionHeader from 'primevue/accordionheader'
import AccordionContent from 'primevue/accordioncontent'
import InputText from 'primevue/inputtext'
import { handbookContent } from '@/data/helpContent'

const { t } = useI18n()
const auth = useAuthStore()

const searchQuery = ref('')

const handbookKey = computed(() => {
  const role = auth.user?.role
  if (role === 'SUPERADMIN' || role === 'SECTION_ADMIN') return 'admin'
  if (role === 'TEACHER') return 'teacher'
  return 'parent'
})

const chapters = computed(() => {
  return handbookContent[handbookKey.value] ?? []
})

const filteredChapters = computed(() => {
  const query = searchQuery.value.toLowerCase().trim()
  if (!query) return chapters.value

  return chapters.value
    .map(chapter => {
      const filteredSections = chapter.sections.filter(section => {
        const titleMatch = t(section.title).toLowerCase().includes(query)
        const contentMatch = section.content.some(c => t(c).toLowerCase().includes(query))
        return titleMatch || contentMatch
      })

      if (filteredSections.length > 0) {
        return { ...chapter, sections: filteredSections }
      }

      if (t(chapter.title).toLowerCase().includes(query)) {
        return chapter
      }

      return null
    })
    .filter(Boolean) as typeof chapters.value
})

const roleLabel = computed(() => {
  const role = auth.user?.role
  if (!role) return ''
  return t(`help.handbook.roleLabel.${role}`)
})
</script>

<template>
  <div class="help-view">
    <div class="help-header">
      <h1>{{ t('help.handbook.title') }}</h1>
      <p class="help-subtitle">
        {{ t('help.handbook.subtitle', { role: roleLabel }) }}
      </p>
    </div>

    <div class="help-search">
      <span class="p-input-icon-left w-full">
        <i class="pi pi-search" />
        <InputText
          v-model="searchQuery"
          :placeholder="t('help.handbook.searchPlaceholder')"
          class="w-full"
        />
      </span>
    </div>

    <div v-if="filteredChapters.length === 0" class="help-no-results">
      <i class="pi pi-search help-no-results-icon" />
      <p>{{ t('help.handbook.noResults') }}</p>
    </div>

    <div v-else class="help-chapters">
      <div v-for="(chapter, ci) in filteredChapters" :key="ci" class="help-chapter">
        <h2 class="chapter-title">{{ t(chapter.title) }}</h2>
        <Accordion>
          <AccordionPanel
            v-for="(section, si) in chapter.sections"
            :key="si"
            :value="String(si)"
          >
            <AccordionHeader>{{ t(section.title) }}</AccordionHeader>
            <AccordionContent>
              <p
                v-for="(paragraph, pi) in section.content"
                :key="pi"
                class="section-content"
              >
                {{ t(paragraph) }}
              </p>
            </AccordionContent>
          </AccordionPanel>
        </Accordion>
      </div>
    </div>
  </div>
</template>

<style scoped>
.help-view {
  max-width: 800px;
  margin: 0 auto;
}

.help-header {
  margin-bottom: 1.5rem;
}

.help-header h1 {
  font-size: var(--mw-font-size-2xl);
  font-weight: 700;
  color: var(--mw-text);
  margin: 0 0 0.25rem;
}

.help-subtitle {
  color: var(--mw-text-secondary);
  font-size: var(--mw-font-size-sm);
  margin: 0;
}

.help-search {
  margin-bottom: 1.5rem;
}

.help-search .p-input-icon-left {
  display: flex;
  align-items: center;
  position: relative;
}

.help-search .p-input-icon-left > .pi {
  position: absolute;
  left: 0.75rem;
  color: var(--mw-text-muted);
  z-index: 1;
}

.help-search .p-input-icon-left > input {
  padding-left: 2.5rem;
}

.w-full {
  width: 100%;
}

.help-chapters {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.chapter-title {
  font-size: var(--mw-font-size-lg);
  font-weight: 600;
  color: var(--mw-text);
  margin: 0 0 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--mw-primary);
}

.section-content {
  font-size: var(--mw-font-size-sm);
  line-height: 1.7;
  color: var(--mw-text-secondary);
  margin: 0 0 0.75rem;
}

.section-content:last-child {
  margin-bottom: 0;
}

.help-no-results {
  text-align: center;
  padding: 3rem 1rem;
  color: var(--mw-text-muted);
}

.help-no-results-icon {
  font-size: 2.5rem;
  margin-bottom: 0.75rem;
  display: block;
}
</style>
