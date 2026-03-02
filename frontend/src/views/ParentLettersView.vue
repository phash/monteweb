<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useParentLetterStore } from '@/stores/parentletter'
import { useAuthStore } from '@/stores/auth'
import type { ParentLetterStatus, RecipientStatus } from '@/types/parentletter'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import ProgressBar from 'primevue/progressbar'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'

const { t } = useI18n()
const { formatShortDate, formatCompactDateTime } = useLocaleDate()
const router = useRouter()
const toast = useToast()
const store = useParentLetterStore()
const auth = useAuthStore()

const activeTab = ref('0')
const deletingId = ref<string | null>(null)
const sendingId = ref<string | null>(null)
const closingId = ref<string | null>(null)

const canCreate = computed(() => auth.isTeacher || auth.isAdmin)
const showMyLettersTab = computed(() => auth.isTeacher || auth.isAdmin)
const showInboxTab = computed(() => auth.user?.role === 'PARENT' || auth.user?.role === 'STUDENT')

onMounted(async () => {
  if (showMyLettersTab.value) {
    await store.fetchMyLetters()
  }
  if (showInboxTab.value) {
    await store.fetchInbox()
  }
  // Default tab: parents start on inbox
  if (!showMyLettersTab.value && showInboxTab.value) {
    activeTab.value = '1'
  }
})

function statusSeverity(status: ParentLetterStatus): 'secondary' | 'info' | 'success' | 'warn' {
  switch (status) {
    case 'DRAFT': return 'secondary'
    case 'SCHEDULED': return 'info'
    case 'SENT': return 'success'
    case 'CLOSED': return 'warn'
  }
}

function inboxStatusSeverity(status: RecipientStatus): 'danger' | 'info' | 'success' {
  switch (status) {
    case 'OPEN': return 'danger'
    case 'READ': return 'info'
    case 'CONFIRMED': return 'success'
  }
}

function confirmProgress(letter: { confirmedCount: number; totalRecipients: number }): number {
  if (letter.totalRecipients === 0) return 0
  return Math.round((letter.confirmedCount / letter.totalRecipients) * 100)
}

async function handleSend(id: string) {
  sendingId.value = id
  try {
    await store.sendLetter(id)
    toast.add({ severity: 'success', summary: t('parentLetters.sent'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  } finally {
    sendingId.value = null
  }
}

async function handleClose(id: string) {
  closingId.value = id
  try {
    await store.closeLetter(id)
    toast.add({ severity: 'success', summary: t('parentLetters.closed'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  } finally {
    closingId.value = null
  }
}

async function handleDelete(id: string) {
  deletingId.value = id
  try {
    await store.deleteLetter(id)
    toast.add({ severity: 'success', summary: t('parentLetters.deleted'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  } finally {
    deletingId.value = null
  }
}
</script>

<template>
  <div>
    <div class="letters-header">
      <PageTitle :title="t('parentLetters.title')" />
      <Button
        v-if="canCreate"
        :label="t('parentLetters.create')"
        icon="pi pi-plus"
        @click="router.push({ name: 'parent-letter-create' })"
      />
    </div>

    <Tabs v-model:value="activeTab">
      <TabList>
        <Tab v-if="showMyLettersTab" value="0">{{ t('parentLetters.tabs.mine') }}</Tab>
        <Tab value="1">{{ t('parentLetters.tabs.inbox') }}</Tab>
      </TabList>
      <TabPanels>

        <!-- My Letters (teacher/admin tab) -->
        <TabPanel v-if="showMyLettersTab" value="0">
          <LoadingSpinner v-if="store.loading && !store.letters.length" />
          <EmptyState
            v-else-if="!store.letters.length"
            icon="pi pi-envelope"
            :message="t('parentLetters.noLetters')"
          />
          <div v-else class="letter-list">
            <div
              v-for="letter in store.letters"
              :key="letter.id"
              class="letter-item card"
            >
              <div class="letter-main" @click="router.push({ name: 'parent-letter-detail', params: { id: letter.id } })">
                <div class="letter-title-row">
                  <strong>{{ letter.title }}</strong>
                  <Tag
                    :value="t(`parentLetters.statuses.${letter.status}`)"
                    :severity="statusSeverity(letter.status)"
                    size="small"
                  />
                </div>
                <div class="letter-meta">
                  <span><i class="pi pi-home" /> {{ letter.roomName }}</span>
                  <span v-if="letter.deadline" class="separator">·</span>
                  <span v-if="letter.deadline">
                    <i class="pi pi-calendar" /> {{ t('parentLetters.deadline') }}: {{ formatShortDate(letter.deadline) }}
                  </span>
                  <span v-if="letter.sendDate" class="separator">·</span>
                  <span v-if="letter.sendDate">
                    <i class="pi pi-send" /> {{ formatShortDate(letter.sendDate) }}
                  </span>
                  <span class="separator">·</span>
                  <span>
                    <i class="pi pi-users" /> {{ letter.confirmedCount }}/{{ letter.totalRecipients }} {{ t('parentLetters.confirmed') }}
                  </span>
                </div>
                <ProgressBar
                  v-if="letter.totalRecipients > 0"
                  :value="confirmProgress(letter)"
                  :showValue="false"
                  class="confirm-progress"
                />
              </div>

              <div class="letter-actions">
                <Button
                  icon="pi pi-eye"
                  text
                  rounded
                  size="small"
                  :aria-label="t('common.view')"
                  @click="router.push({ name: 'parent-letter-detail', params: { id: letter.id } })"
                />
                <Button
                  v-if="letter.status === 'DRAFT'"
                  icon="pi pi-pencil"
                  text
                  rounded
                  size="small"
                  :aria-label="t('common.edit')"
                  @click="router.push({ name: 'parent-letter-edit', params: { id: letter.id } })"
                />
                <Button
                  v-if="letter.status === 'DRAFT'"
                  icon="pi pi-send"
                  text
                  rounded
                  size="small"
                  severity="success"
                  :loading="sendingId === letter.id"
                  :aria-label="t('parentLetters.send')"
                  @click.stop="handleSend(letter.id)"
                />
                <Button
                  v-if="letter.status === 'SENT'"
                  icon="pi pi-lock"
                  text
                  rounded
                  size="small"
                  severity="warn"
                  :loading="closingId === letter.id"
                  :aria-label="t('parentLetters.close')"
                  @click.stop="handleClose(letter.id)"
                />
                <Button
                  v-if="letter.status === 'DRAFT'"
                  icon="pi pi-trash"
                  text
                  rounded
                  size="small"
                  severity="danger"
                  :loading="deletingId === letter.id"
                  :aria-label="t('common.delete')"
                  @click.stop="handleDelete(letter.id)"
                />
              </div>
            </div>
          </div>
        </TabPanel>

        <!-- Inbox (parent tab) -->
        <TabPanel value="1">
          <LoadingSpinner v-if="store.loading && !store.inbox.length" />
          <EmptyState
            v-else-if="!store.inbox.length"
            icon="pi pi-inbox"
            :message="t('parentLetters.noInbox')"
          />
          <div v-else class="letter-list">
            <router-link
              v-for="letter in store.inbox"
              :key="letter.id"
              :to="{ name: 'parent-letter-detail', params: { id: letter.id } }"
              class="letter-item card inbox-item"
            >
              <div class="letter-main">
                <div class="letter-title-row">
                  <strong>{{ letter.title }}</strong>
                  <Tag
                    v-if="letter.confirmedCount === letter.totalRecipients && letter.totalRecipients > 0"
                    :value="t('parentLetters.recipientStatuses.CONFIRMED')"
                    severity="success"
                    size="small"
                    icon="pi pi-check"
                  />
                  <Tag
                    v-else
                    :value="t('parentLetters.recipientStatuses.OPEN')"
                    severity="danger"
                    size="small"
                  />
                </div>
                <div class="letter-meta">
                  <span><i class="pi pi-home" /> {{ letter.roomName }}</span>
                  <span v-if="letter.sendDate" class="separator">·</span>
                  <span v-if="letter.sendDate">
                    <i class="pi pi-calendar" /> {{ formatCompactDateTime(letter.sendDate) }}
                  </span>
                  <span v-if="letter.deadline" class="separator">·</span>
                  <span v-if="letter.deadline">
                    <i class="pi pi-clock" /> {{ t('parentLetters.deadline') }}: {{ formatShortDate(letter.deadline) }}
                  </span>
                </div>
              </div>
              <i class="pi pi-chevron-right letter-arrow" />
            </router-link>
          </div>
        </TabPanel>

      </TabPanels>
    </Tabs>
  </div>
</template>

<style scoped>
.letters-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1rem;
}

.letter-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.letter-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  transition: background 0.15s;
}

.inbox-item {
  cursor: pointer;
  text-decoration: none;
  color: inherit;
}

.inbox-item:hover,
.letter-item:hover {
  background: var(--mw-bg-hover);
}

.letter-main {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.letter-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 0.25rem;
}

.letter-meta {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.letter-meta i {
  margin-right: 0.2rem;
}

.separator {
  margin: 0 0.25rem;
}

.confirm-progress {
  margin-top: 0.5rem;
  height: 0.4rem;
}

.letter-actions {
  display: flex;
  gap: 0.25rem;
  align-items: center;
  flex-shrink: 0;
}

.letter-arrow {
  color: var(--mw-text-muted);
  font-size: 0.75rem;
  flex-shrink: 0;
}

@media (max-width: 600px) {
  .letter-actions {
    flex-direction: column;
  }
}
</style>
