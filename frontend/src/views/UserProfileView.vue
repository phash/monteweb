<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { usersApi } from '@/api/users.api'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { useMessagingStore } from '@/stores/messaging'
import type { UserInfo } from '@/types/user'
import type { RoomInfo } from '@/types/room'
import type { FamilyInfo } from '@/types/family'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Avatar from 'primevue/avatar'
import Button from 'primevue/button'
import Tag from 'primevue/tag'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const admin = useAdminStore()
const messaging = useMessagingStore()

const user = ref<UserInfo | null>(null)
const rooms = ref<RoomInfo[]>([])
const families = ref<FamilyInfo[]>([])
const loading = ref(true)
const startingChat = ref(false)

const userId = computed(() => route.params.userId as string)
const isOwnProfile = computed(() => userId.value === auth.user?.id)
const messagingEnabled = computed(() => admin.config?.modules?.messaging ?? false)

function roleSeverity(role: string): string {
  switch (role) {
    case 'SUPERADMIN': return 'danger'
    case 'SECTION_ADMIN': return 'warn'
    case 'TEACHER': return 'info'
    case 'PARENT': return 'success'
    case 'STUDENT': return 'secondary'
    default: return 'secondary'
  }
}

function userInitials(u: UserInfo): string {
  const first = u.firstName?.charAt(0) ?? ''
  const last = u.lastName?.charAt(0) ?? ''
  return (first + last).toUpperCase() || '?'
}

async function startConversation() {
  if (!user.value || isOwnProfile.value) return
  startingChat.value = true
  try {
    const conv = await messaging.startDirectConversation(user.value.id)
    router.push({ name: 'messages', params: { conversationId: conv.id } })
  } catch {
    // Communication not allowed
  } finally {
    startingChat.value = false
  }
}

onMounted(async () => {
  try {
    const [userRes, roomsRes, familiesRes] = await Promise.all([
      usersApi.getById(userId.value),
      auth.isAdmin ? usersApi.getUserRooms(userId.value).catch(() => ({ data: { data: [] } })) : Promise.resolve({ data: { data: [] as RoomInfo[] } }),
      auth.isAdmin ? usersApi.getUserFamilies(userId.value).catch(() => ({ data: { data: [] } })) : Promise.resolve({ data: { data: [] as FamilyInfo[] } }),
    ])
    user.value = userRes.data.data
    rooms.value = roomsRes.data.data
    families.value = familiesRes.data.data
  } catch {
    // user not found
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="user-profile-view">
    <LoadingSpinner v-if="loading" />

    <template v-else-if="user">
      <Button
        icon="pi pi-arrow-left"
        :label="t('common.back')"
        text
        severity="secondary"
        size="small"
        class="back-btn"
        @click="router.back()"
      />

      <div class="profile-card card">
        <div class="profile-header">
          <Avatar
            v-if="user.avatarUrl"
            :image="user.avatarUrl"
            shape="circle"
            size="xlarge"
            class="profile-avatar"
          />
          <Avatar
            v-else
            :label="userInitials(user)"
            shape="circle"
            size="xlarge"
            class="profile-avatar profile-avatar-initials"
          />
          <div class="profile-info">
            <h2 class="profile-name">{{ user.displayName }}</h2>
            <p class="profile-email">{{ user.email }}</p>
            <Tag
              :value="t(`directory.roles.${user.role}`)"
              :severity="roleSeverity(user.role)"
              class="profile-role"
            />
          </div>
        </div>

        <div v-if="user.phone" class="profile-detail">
          <i class="pi pi-phone" />
          <span>{{ user.phone }}</span>
        </div>

        <div v-if="messagingEnabled && !isOwnProfile" class="profile-actions">
          <Button
            :label="t('directory.sendMessage')"
            icon="pi pi-comment"
            :loading="startingChat"
            @click="startConversation"
          />
        </div>
      </div>

      <!-- Admin: User's rooms -->
      <div v-if="auth.isAdmin && rooms.length" class="section card">
        <h3>{{ t('rooms.title') }}</h3>
        <div class="list">
          <div
            v-for="room in rooms"
            :key="room.id"
            class="list-item"
            @click="router.push({ name: 'room-detail', params: { id: room.id } })"
          >
            <i class="pi pi-home" />
            <span>{{ room.name }}</span>
          </div>
        </div>
      </div>

      <!-- Admin: User's families -->
      <div v-if="auth.isAdmin && families.length" class="section card">
        <h3>{{ t('family.title') }}</h3>
        <div class="list">
          <div
            v-for="fam in families"
            :key="fam.id"
            class="list-item"
          >
            <i class="pi pi-users" />
            <span>{{ fam.name }}</span>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="not-found">
      <i class="pi pi-user-minus empty-icon" />
      <p>{{ t('directory.noResults') }}</p>
    </div>
  </div>
</template>

<style scoped>
.user-profile-view {
  max-width: 700px;
  margin: 0 auto;
}

.back-btn {
  margin-bottom: 1rem;
}

.profile-card {
  padding: 1.5rem;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  margin-bottom: 1rem;
}

.profile-avatar-initials {
  background-color: var(--mw-primary);
  color: white;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name {
  margin: 0;
  font-size: 1.25rem;
}

.profile-email {
  margin: 0.25rem 0 0.5rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
}

.profile-role {
  font-size: var(--mw-font-size-xs);
}

.profile-detail {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  margin-bottom: 1rem;
}

.profile-detail i {
  color: var(--mw-text-muted);
}

.profile-actions {
  padding-top: 1rem;
  border-top: 1px solid var(--mw-border-light);
}

.section {
  margin-top: 1rem;
  padding: 1rem 1.25rem;
}

.section h3 {
  margin: 0 0 0.75rem;
  font-size: var(--mw-font-size-base);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.5rem;
  border-radius: var(--mw-border-radius-sm);
  cursor: pointer;
  font-size: var(--mw-font-size-sm);
  transition: background 0.15s;
}

.list-item:hover {
  background: var(--mw-bg-hover);
}

.list-item i {
  color: var(--mw-text-muted);
}

.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4rem 2rem;
  color: var(--mw-text-muted);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}
</style>
