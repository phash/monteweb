<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { usersApi } from '@/api/users.api'
import { sectionsApi } from '@/api/sections.api'
import { roomsApi } from '@/api/rooms.api'
import { useAdminStore } from '@/stores/admin'
import { useAuthStore } from '@/stores/auth'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Paginator from 'primevue/paginator'
import Avatar from 'primevue/avatar'
import type { UserInfo, UserRole } from '@/types/user'
import type { SchoolSectionInfo } from '@/types/family'
import type { RoomInfo } from '@/types/room'

const { t } = useI18n()
const router = useRouter()
const admin = useAdminStore()
const auth = useAuthStore()

// Filters
const searchQuery = ref('')
const selectedRole = ref<string | null>(null)
const selectedSectionId = ref<string | null>(null)
const selectedRoomId = ref<string | null>(null)

// Data
const users = ref<UserInfo[]>([])
const sections = ref<SchoolSectionInfo[]>([])
const allRooms = ref<RoomInfo[]>([])
const loading = ref(false)
const totalElements = ref(0)
const page = ref(0)
const pageSize = ref(18)

// Debounce
let searchTimeout: ReturnType<typeof setTimeout> | null = null

const roleOptions = computed(() => [
  { label: t('directory.filterRole'), value: null },
  { label: t('directory.roles.SUPERADMIN'), value: 'SUPERADMIN' },
  { label: t('directory.roles.SECTION_ADMIN'), value: 'SECTION_ADMIN' },
  { label: t('directory.roles.TEACHER'), value: 'TEACHER' },
  { label: t('directory.roles.PARENT'), value: 'PARENT' },
  { label: t('directory.roles.STUDENT'), value: 'STUDENT' },
])

const sectionOptions = computed(() => [
  { label: t('directory.filterSection'), value: null },
  ...sections.value.filter(s => s.active).map(s => ({ label: s.name, value: s.id })),
])

const roomOptions = computed(() => {
  const filtered = selectedSectionId.value
    ? allRooms.value.filter(r => r.sectionId === selectedSectionId.value)
    : allRooms.value
  return [
    { label: t('directory.filterRoom'), value: null },
    ...filtered.map(r => ({ label: r.name, value: r.id })),
  ]
})

function roleSeverity(role: UserRole): string {
  switch (role) {
    case 'SUPERADMIN': return 'danger'
    case 'SECTION_ADMIN': return 'warn'
    case 'TEACHER': return 'info'
    case 'PARENT': return 'success'
    case 'STUDENT': return 'secondary'
    default: return 'secondary'
  }
}

function userInitials(user: UserInfo): string {
  const first = user.firstName?.charAt(0) ?? ''
  const last = user.lastName?.charAt(0) ?? ''
  return (first + last).toUpperCase() || '?'
}

async function loadUsers() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: page.value,
      size: pageSize.value,
    }
    if (searchQuery.value.trim()) params.q = searchQuery.value.trim()
    if (selectedRole.value) params.role = selectedRole.value
    if (selectedSectionId.value) params.sectionId = selectedSectionId.value
    if (selectedRoomId.value) params.roomId = selectedRoomId.value

    const res = await usersApi.directory(params as Parameters<typeof usersApi.directory>[0])
    users.value = res.data.data.content
    totalElements.value = res.data.data.totalElements
  } catch {
    users.value = []
    totalElements.value = 0
  } finally {
    loading.value = false
  }
}

function onSearchInput() {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    page.value = 0
    loadUsers()
  }, 300)
}

function onFilterChange() {
  page.value = 0
  loadUsers()
}

function onSectionChange() {
  // Reset room filter when section changes
  selectedRoomId.value = null
  page.value = 0
  loadUsers()
}

function onPageChange(event: { page: number; rows: number }) {
  page.value = event.page
  pageSize.value = event.rows
  loadUsers()
}

function onCardClick(user: UserInfo) {
  if (admin.isModuleEnabled('messaging') && user.id !== auth.user?.id) {
    router.push({ name: 'messages', query: { userId: user.id } })
  }
}

onMounted(async () => {
  const [sectionsRes, roomsRes] = await Promise.all([
    sectionsApi.getAll().catch(() => ({ data: { data: [] as SchoolSectionInfo[] } })),
    roomsApi.getMine().catch(() => ({ data: { data: [] as RoomInfo[] } })),
  ])
  sections.value = sectionsRes.data.data
  allRooms.value = roomsRes.data.data
  await loadUsers()
})
</script>

<template>
  <div class="directory-view">
    <PageTitle :title="t('directory.title')" />

    <!-- Filter bar -->
    <div class="filter-bar">
      <div class="search-wrapper">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchQuery"
            :placeholder="t('directory.search')"
            class="search-input"
            @input="onSearchInput"
          />
        </span>
      </div>
      <div class="filter-selects">
        <Select
          v-model="selectedRole"
          :options="roleOptions"
          option-label="label"
          option-value="value"
          :placeholder="t('directory.filterRole')"
          class="filter-select"
          @change="onFilterChange"
        />
        <Select
          v-model="selectedSectionId"
          :options="sectionOptions"
          option-label="label"
          option-value="value"
          :placeholder="t('directory.filterSection')"
          class="filter-select"
          @change="onSectionChange"
        />
        <Select
          v-model="selectedRoomId"
          :options="roomOptions"
          option-label="label"
          option-value="value"
          :placeholder="t('directory.filterRoom')"
          class="filter-select"
          @change="onFilterChange"
        />
      </div>
    </div>

    <!-- Loading state -->
    <LoadingSpinner v-if="loading && users.length === 0" @retry="loadUsers" />

    <!-- Empty state -->
    <div v-else-if="!loading && users.length === 0" class="empty-state">
      <i class="pi pi-users empty-icon" />
      <p>{{ t('directory.noResults') }}</p>
    </div>

    <!-- User cards grid -->
    <template v-else>
      <p class="showing-count">{{ t('directory.showingCount', { count: totalElements }) }}</p>
      <div class="user-grid">
        <div
          v-for="user in users"
          :key="user.id"
          class="user-card"
          :class="{ clickable: admin.isModuleEnabled('messaging') && user.id !== auth.user?.id }"
          role="button"
          :tabindex="admin.isModuleEnabled('messaging') && user.id !== auth.user?.id ? 0 : undefined"
          @click="onCardClick(user)"
          @keydown.enter="onCardClick(user)"
        >
          <div class="user-card-header">
            <Avatar
              v-if="user.avatarUrl"
              :image="user.avatarUrl"
              shape="circle"
              size="large"
              class="user-avatar"
            />
            <Avatar
              v-else
              :label="userInitials(user)"
              shape="circle"
              size="large"
              class="user-avatar user-avatar-initials"
            />
            <div class="user-info">
              <span class="user-name">{{ user.displayName }}</span>
              <span class="user-email">{{ user.email }}</span>
            </div>
          </div>
          <div class="user-card-footer">
            <Tag
              :value="t(`directory.roles.${user.role}`)"
              :severity="roleSeverity(user.role)"
              class="role-tag"
            />
            <button
              v-if="admin.isModuleEnabled('messaging') && user.id !== auth.user?.id"
              class="chat-btn"
              :title="t('directory.startChat')"
              @click.stop="onCardClick(user)"
            >
              <i class="pi pi-comment" />
            </button>
          </div>
        </div>
      </div>

      <Paginator
        v-if="totalElements > pageSize"
        :rows="pageSize"
        :total-records="totalElements"
        :first="page * pageSize"
        :rows-per-page-options="[18, 36, 54]"
        @page="onPageChange"
      />
    </template>
  </div>
</template>

<style scoped>
.directory-view {
  max-width: 1200px;
  margin: 0 auto;
}

.filter-bar {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.search-wrapper {
  width: 100%;
}

.search-wrapper .p-input-icon-left {
  width: 100%;
}

.search-input {
  width: 100%;
}

.filter-selects {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.filter-select {
  min-width: 160px;
  flex: 1;
}

.showing-count {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  margin-bottom: 1rem;
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.user-card {
  background: var(--mw-bg-card);
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-lg);
  padding: 1rem;
  transition: box-shadow 0.2s, transform 0.15s;
}

.user-card.clickable {
  cursor: pointer;
}

.user-card.clickable:hover {
  box-shadow: var(--mw-shadow-md);
  transform: translateY(-2px);
}

.user-card.clickable:focus-visible {
  outline: 2px solid var(--mw-primary);
  outline-offset: 2px;
}

.user-card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.user-avatar-initials {
  background-color: var(--mw-primary);
  color: white;
}

.user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  font-weight: 600;
  font-size: var(--mw-font-size-base);
  color: var(--mw-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-email {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.role-tag {
  font-size: var(--mw-font-size-xs);
}

.chat-btn {
  background: none;
  border: none;
  color: var(--mw-text-muted);
  cursor: pointer;
  padding: 0.25rem;
  border-radius: var(--mw-border-radius);
  transition: color 0.15s, background-color 0.15s;
}

.chat-btn:hover {
  color: var(--mw-primary);
  background-color: color-mix(in srgb, var(--mw-primary) 10%, transparent);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  color: var(--mw-text-muted);
  text-align: center;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

/* Tablet: 2 columns */
@media (max-width: 1024px) {
  .user-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Mobile: 1 column */
@media (max-width: 640px) {
  .user-grid {
    grid-template-columns: 1fr;
  }

  .filter-selects {
    flex-direction: column;
  }

  .filter-select {
    min-width: unset;
  }
}
</style>
