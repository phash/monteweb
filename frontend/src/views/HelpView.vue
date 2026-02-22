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

interface PdfDownload {
  label: string
  file: string
  icon: string
}

const { t } = useI18n()
const auth = useAuthStore()

const searchQuery = ref('')

const handbookKey = computed(() => {
  const role = auth.user?.role
  if (role === 'SUPERADMIN' || role === 'SECTION_ADMIN') return 'admin'
  if (role === 'TEACHER') return 'teacher'
  if (role === 'STUDENT') return 'student'
  return 'parent'
})

const downloads = computed<PdfDownload[]>(() => {
  const role = auth.user?.role
  const items: PdfDownload[] = []

  if (role === 'SUPERADMIN') {
    items.push(
      { label: t('help.downloads.handbookAdmin'), file: 'MonteWeb_Handbuch_Administration.pdf', icon: 'pi pi-book' },
      { label: t('help.downloads.cheatsheetSuperadmin'), file: 'MonteWeb_CheatSheet_Superadmin.pdf', icon: 'pi pi-list-check' },
    )
  }
  if (role === 'SECTION_ADMIN') {
    items.push(
      { label: t('help.downloads.handbookAdmin'), file: 'MonteWeb_Handbuch_Administration.pdf', icon: 'pi pi-book' },
      { label: t('help.downloads.cheatsheetSectionAdmin'), file: 'MonteWeb_CheatSheet_Bereichsleitung.pdf', icon: 'pi pi-list-check' },
    )
  }
  if (role === 'TEACHER') {
    items.push(
      { label: t('help.downloads.handbookTeacher'), file: 'MonteWeb_Handbuch_Lehrkraefte.pdf', icon: 'pi pi-book' },
      { label: t('help.downloads.cheatsheetTeacher'), file: 'MonteWeb_CheatSheet_Lehrkraefte.pdf', icon: 'pi pi-list-check' },
    )
  }
  if (role === 'PARENT') {
    items.push(
      { label: t('help.downloads.handbookParent'), file: 'MonteWeb_Handbuch_Eltern.pdf', icon: 'pi pi-book' },
      { label: t('help.downloads.cheatsheetParent'), file: 'MonteWeb_CheatSheet_Eltern.pdf', icon: 'pi pi-list-check' },
    )
  }
  if (role === 'STUDENT') {
    items.push(
      { label: t('help.downloads.cheatsheetParent'), file: 'MonteWeb_CheatSheet_Eltern.pdf', icon: 'pi pi-list-check' },
    )
  }

  return items
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

    <div v-if="downloads.length" class="help-downloads">
      <h2 class="downloads-title">
        <i class="pi pi-download" />
        {{ t('help.downloads.title') }}
      </h2>
      <div class="downloads-grid">
        <a
          v-for="dl in downloads"
          :key="dl.file"
          :href="`/docs/${dl.file}`"
          target="_blank"
          class="download-card"
        >
          <i :class="dl.icon" />
          <span class="download-label">{{ dl.label }}</span>
          <span class="download-badge">PDF</span>
        </a>
      </div>
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

.help-downloads {
  margin-bottom: 2rem;
}

.downloads-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--mw-font-size-lg);
  font-weight: 600;
  color: var(--mw-text);
  margin: 0 0 1rem;
}

.downloads-title .pi {
  color: var(--mw-primary);
}

.downloads-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 0.75rem;
}

.download-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background: var(--mw-bg-card);
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius);
  text-decoration: none;
  color: var(--mw-text);
  transition: border-color 0.15s, box-shadow 0.15s;
}

.download-card:hover {
  border-color: var(--mw-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  text-decoration: none;
}

.download-card .pi {
  font-size: 1.25rem;
  color: var(--mw-primary);
  flex-shrink: 0;
}

.download-label {
  flex: 1;
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
}

.download-badge {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
  background: color-mix(in srgb, var(--mw-primary) 12%, transparent);
  color: var(--mw-primary);
  flex-shrink: 0;
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
