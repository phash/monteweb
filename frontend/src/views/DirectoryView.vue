<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { usersApi } from '@/api/users.api'
import { sectionsApi } from '@/api/sections.api'
import { roomsApi } from '@/api/rooms.api'
import { familyApi } from '@/api/family.api'
import { useAdminStore } from '@/stores/admin'
import { useAuthStore } from '@/stores/auth'
import { useMessagingStore } from '@/stores/messaging'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Paginator from 'primevue/paginator'
import Avatar from 'primevue/avatar'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import type { UserInfo, UserRole } from '@/types/user'
import type { SchoolSectionInfo, FamilyInfo } from '@/types/family'
import type { RoomInfo } from '@/types/room'

const { t } = useI18n()
const router = useRouter()
const admin = useAdminStore()
const auth = useAuthStore()
const messaging = useMessagingStore()

const activeTab = ref('users')

// User filters
const searchQuery = ref('')
const selectedRole = ref<string | null>(null)
const selectedSectionId = ref<string | null>(null)
const selectedRoomId = ref<string | null>(null)

// User data
const users = ref<UserInfo[]>([])
const sections = ref<SchoolSectionInfo[]>([])
const allRooms = ref<RoomInfo[]>([])
const loading = ref(false)
const totalElements = ref(0)
const page = ref(0)
const pageSize = ref(18)

// Family data
const allFamilies = ref<FamilyInfo[]>([])
const familiesLoading = ref(false)
const familySearch = ref('')

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

const filteredFamilies = computed(() => {
  if (!familySearch.value.trim()) return allFamilies.value
  const q = familySearch.value.trim().toLowerCase()
  return allFamilies.value.filter(f =>
    f.name.toLowerCase().includes(q) ||
    f.members.some(m => m.displayName.toLowerCase().includes(q))
  )
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

function familyInitials(name: string): string {
  return name.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase() || '?'
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

async function loadFamilies() {
  familiesLoading.value = true
  try {
    const res = await familyApi.getAll()
    allFamilies.value = res.data.data
  } catch {
    allFamilies.value = []
  } finally {
    familiesLoading.value = false
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
  selectedRoomId.value = null
  page.value = 0
  loadUsers()
}

function onPageChange(event: { page: number; rows: number }) {
  page.value = event.page
  pageSize.value = event.rows
  loadUsers()
}

function onUserClick(user: UserInfo) {
  router.push({ name: 'user-profile', params: { userId: user.id } })
}

async function onChatClick(user: UserInfo) {
  try {
    const conv = await messaging.startDirectConversation(user.id)
    router.push({ name: 'messages', params: { conversationId: conv.id } })
  } catch {
    // Communication not allowed
  }
}

function onMemberClick(memberId: string) {
  router.push({ name: 'user-profile', params: { userId: memberId } })
}

watch(activeTab, (tab) => {
  if (tab === 'families' && !allFamilies.value.length && !familiesLoading.value) {
    loadFamilies()
  }
})

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

    <Tabs v-model:value="activeTab">
      <TabList>
        <Tab value="users">{{ t('directory.tabUsers') }}</Tab>
        <Tab value="families">{{ t('directory.tabFamilies') }}</Tab>
      </TabList>

      <TabPanels>
        <!-- Users Tab -->
        <TabPanel value="users">
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

          <LoadingSpinner v-if="loading && users.length === 0" @retry="loadUsers" />

          <div v-else-if="!loading && users.length === 0" class="empty-state">
            <i class="pi pi-users empty-icon" />
            <p>{{ t('directory.noResults') }}</p>
          </div>

          <template v-else>
            <p class="showing-count">{{ t('directory.showingCount', { count: totalElements }) }}</p>
            <div class="user-grid">
              <div
                v-for="u in users"
                :key="u.id"
                class="user-card clickable"
                role="button"
                tabindex="0"
                @click="onUserClick(u)"
                @keydown.enter="onUserClick(u)"
              >
                <div class="user-card-header">
                  <Avatar
                    v-if="u.avatarUrl"
                    :image="u.avatarUrl"
                    shape="circle"
                    size="large"
                    class="user-avatar"
                  />
                  <Avatar
                    v-else
                    :label="userInitials(u)"
                    shape="circle"
                    size="large"
                    class="user-avatar user-avatar-initials"
                  />
                  <div class="user-info">
                    <span class="user-name">{{ u.displayName }}</span>
                    <span class="user-email">{{ u.email }}</span>
                  </div>
                </div>
                <div class="user-card-footer">
                  <Tag
                    :value="t(`directory.roles.${u.role}`)"
                    :severity="roleSeverity(u.role)"
                    class="role-tag"
                  />
                  <button
                    v-if="admin.isModuleEnabled('messaging') && u.id !== auth.user?.id"
                    class="chat-btn"
                    :title="t('directory.startChat')"
                    @click.stop="onChatClick(u)"
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
        </TabPanel>

        <!-- Families Tab -->
        <TabPanel value="families">
          <div class="filter-bar">
            <div class="search-wrapper">
              <span class="p-input-icon-left">
                <i class="pi pi-search" />
                <InputText
                  v-model="familySearch"
                  :placeholder="t('directory.searchFamilies')"
                  class="search-input"
                />
              </span>
            </div>
          </div>

          <LoadingSpinner v-if="familiesLoading" />

          <EmptyState
            v-else-if="!filteredFamilies.length"
            icon="pi pi-users"
            :message="t('directory.noFamilies')"
          />

          <div v-else class="family-grid">
            <div
              v-for="fam in filteredFamilies"
              :key="fam.id"
              class="family-card card"
            >
              <div class="family-card-header">
                <Avatar
                  v-if="fam.avatarUrl"
                  :image="fam.avatarUrl"
                  shape="circle"
                  size="large"
                  class="family-avatar"
                />
                <Avatar
                  v-else
                  :label="familyInitials(fam.name)"
                  shape="circle"
                  size="large"
                  class="family-avatar family-avatar-initials"
                />
                <div class="family-info">
                  <span class="family-name">{{ fam.name }}</span>
                  <span class="family-count">{{ t('directory.memberCount', { count: fam.members.length }) }}</span>
                </div>
              </div>
              <div class="family-members">
                <div
                  v-for="member in fam.members"
                  :key="member.userId"
                  class="family-member"
                  role="button"
                  tabindex="0"
                  @click="onMemberClick(member.userId)"
                  @keydown.enter="onMemberClick(member.userId)"
                >
                  <i class="pi pi-user" />
                  <span class="member-name">{{ member.displayName }}</span>
                  <Tag
                    :value="member.role === 'PARENT' ? t('family.parent') : t('family.child')"
                    :severity="member.role === 'PARENT' ? 'info' : 'secondary'"
                    class="member-role"
                  />
                </div>
              </div>
            </div>
          </div>
        </TabPanel>
      </TabPanels>
    </Tabs>
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

/* Family grid */
.family-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.family-card {
  padding: 1rem;
}

.family-card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.family-avatar-initials {
  background-color: var(--mw-primary);
  color: white;
}

.family-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.family-name {
  font-weight: 600;
  font-size: var(--mw-font-size-base);
  color: var(--mw-text);
}

.family-count {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.family-members {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.family-member {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.5rem;
  border-radius: var(--mw-border-radius-sm);
  cursor: pointer;
  transition: background 0.15s;
  font-size: var(--mw-font-size-sm);
}

.family-member:hover {
  background: var(--mw-bg-hover);
}

.family-member i {
  color: var(--mw-text-muted);
  font-size: 0.75rem;
}

.member-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-role {
  font-size: 0.625rem;
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

@media (max-width: 1024px) {
  .user-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .user-grid {
    grid-template-columns: 1fr;
  }

  .family-grid {
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
