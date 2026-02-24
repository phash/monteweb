<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoomsStore } from '@/stores/rooms'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { feedApi } from '@/api/feed.api'
import { roomsApi } from '@/api/rooms.api'
import { familyApi } from '@/api/family.api'
import { usersApi } from '@/api/users.api'
import type { FeedPost } from '@/types/feed'
import type { UserInfo } from '@/types/user'
import type { JoinRequestInfo } from '@/types/room'
import type { FamilyInfo } from '@/types/family'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AvatarUpload from '@/components/common/AvatarUpload.vue'
import PostComposer from '@/components/feed/PostComposer.vue'
import FeedPostComponent from '@/components/feed/FeedPost.vue'
import RoomFiles from '@/components/rooms/RoomFiles.vue'
import RoomChat from '@/components/rooms/RoomChat.vue'
import RoomDiscussions from '@/components/rooms/RoomDiscussions.vue'
import RoomEvents from '@/components/rooms/RoomEvents.vue'
import RoomFotobox from '@/components/rooms/RoomFotobox.vue'
import RoomTasks from '@/components/rooms/RoomTasks.vue'
import RoomWiki from '@/components/rooms/RoomWiki.vue'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import Textarea from 'primevue/textarea'
import Dialog from 'primevue/dialog'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'

const props = defineProps<{ id: string }>()
const { t } = useI18n()
const rooms = useRoomsStore()
const auth = useAuthStore()
const admin = useAdminStore()
const router = useRouter()
const toast = useToast()

const roomPosts = ref<FeedPost[]>([])
const postsLoading = ref(false)
const activeTab = ref('0')
const editingPublicDesc = ref(false)
const publicDescDraft = ref('')

// Join request
const showJoinRequestDialog = ref(false)
const joinRequestMessage = ref('')
const joinRequestLoading = ref(false)

// Leader: pending join requests
const pendingRequests = ref<JoinRequestInfo[]>([])
const requestsLoading = ref(false)

// Mute state
const roomMuted = ref(false)

// Add family dialog
const showAddFamilyDialog = ref(false)
const families = ref<FamilyInfo[]>([])
const selectedFamilyId = ref<string | null>(null)
const addingFamily = ref(false)

// Add member dialog
const showAddMemberDialog = ref(false)
const memberSearchQuery = ref('')
const memberSearchResults = ref<UserInfo[]>([])
const memberSearchLoading = ref(false)
const addingMemberId = ref<string | null>(null)
const memberRoleFilter = ref<string>('ALL')
let searchTimeout: ReturnType<typeof setTimeout> | null = null

const isKlasse = computed(() => rooms.currentRoom?.type === 'KLASSE')

const roleFilterOptions = computed(() => [
  { label: t('rooms.filterAll'), value: 'ALL' },
  { label: t('rooms.filterTeacher'), value: 'TEACHER' },
  { label: t('rooms.filterParent'), value: 'PARENT' },
  { label: t('rooms.filterStudent'), value: 'STUDENT' },
  { label: t('rooms.filterFamily'), value: 'FAMILIE' },
])

const filteredSearchResults = computed(() => {
  const role = isKlasse.value ? 'TEACHER' : memberRoleFilter.value
  if (role === 'ALL') return memberSearchResults.value
  return memberSearchResults.value.filter(u => u.role === role)
})

const filesEnabled = admin.config?.modules?.files ?? false
const calendarEnabled = admin.config?.modules?.calendar ?? false
const fotoboxEnabled = admin.config?.modules?.fotobox ?? false
const tasksEnabled = admin.isModuleEnabled('tasks')
const wikiEnabled = admin.isModuleEnabled('wiki')
const messagingEnabled = admin.config?.modules?.messaging ?? false
const chatEnabled = computed(() =>
  messagingEnabled && rooms.currentRoom?.settings?.chatEnabled !== false
)

const isLeader = computed(() => {
  if (!rooms.currentRoom?.members) return false
  return rooms.currentRoom.members.some(
    m => m.userId === auth.user?.id && m.role === 'LEADER'
  )
})
const canEditRoom = computed(() => isLeader.value || auth.isAdmin || auth.isSectionAdmin)

// Member grouping computeds
const leaderMembers = computed(() => {
  if (!rooms.currentRoom?.members) return []
  return rooms.currentRoom.members.filter(m => m.role === 'LEADER')
})

const familyGroups = computed(() => {
  if (!rooms.currentRoom?.members) return []
  const nonLeaders = rooms.currentRoom.members.filter(m => m.role !== 'LEADER' && m.familyId)
  const grouped = new Map<string, { familyId: string; familyName: string; members: typeof nonLeaders }>()
  for (const member of nonLeaders) {
    const key = member.familyId!
    if (!grouped.has(key)) {
      grouped.set(key, { familyId: key, familyName: member.familyName || key, members: [] })
    }
    grouped.get(key)!.members.push(member)
  }
  return Array.from(grouped.values())
})

const otherMembers = computed(() => {
  if (!rooms.currentRoom?.members) return []
  return rooms.currentRoom.members.filter(m => m.role !== 'LEADER' && !m.familyId)
})

async function loadRoom() {
  activeTab.value = '0'
  roomPosts.value = []
  pendingRequests.value = []
  try {
    await rooms.fetchRoom(props.id)
    if (rooms.currentRoom) {
      loadPosts()
      if (isLeader.value || auth.isAdmin || auth.isSectionAdmin) {
        loadPendingRequests()
      }
    }
  } catch {
    // Room not accessible
  }
}

onMounted(() => loadRoom())

watch(() => props.id, () => loadRoom())

async function loadPosts() {
  postsLoading.value = true
  try {
    const res = await feedApi.getRoomPosts(props.id)
    roomPosts.value = res.data.data.content
  } catch {
    roomPosts.value = []
  } finally {
    postsLoading.value = false
  }
}

async function handlePost(data: { title?: string; content?: string; poll?: import('@/types/feed').CreatePollRequest }) {
  if (data.poll) {
    const res = await feedApi.createPost({
      sourceType: 'ROOM',
      sourceId: props.id,
      ...data,
    })
    roomPosts.value.unshift(res.data.data)
  } else if (data.content) {
    const res = await feedApi.createRoomPost(props.id, { title: data.title, content: data.content })
    roomPosts.value.unshift(res.data.data)
  }
}

async function handleRoomAvatarUpload(file: File) {
  await roomsApi.uploadAvatar(props.id, file)
  await rooms.fetchRoom(props.id)
}

async function handleRoomAvatarRemove() {
  await roomsApi.removeAvatar(props.id)
  await rooms.fetchRoom(props.id)
}

function startEditPublicDesc() {
  publicDescDraft.value = rooms.currentRoom?.publicDescription ?? ''
  editingPublicDesc.value = true
}

async function savePublicDesc() {
  await roomsApi.update(props.id, { publicDescription: publicDescDraft.value })
  await rooms.fetchRoom(props.id)
  editingPublicDesc.value = false
  toast.add({ severity: 'success', summary: t('common.save'), life: 3000 })
}

async function joinRoom() {
  try {
    await rooms.joinRoom(props.id)
    toast.add({ severity: 'success', summary: t('discover.joined'), life: 3000 })
    await rooms.fetchRoom(props.id)
    if (rooms.currentRoom?.members) loadPosts()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function submitJoinRequest() {
  joinRequestLoading.value = true
  try {
    await roomsApi.requestJoin(props.id, joinRequestMessage.value || undefined)
    toast.add({ severity: 'success', summary: t('rooms.joinRequestSent'), life: 3000 })
    showJoinRequestDialog.value = false
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    joinRequestLoading.value = false
  }
}

async function loadPendingRequests() {
  requestsLoading.value = true
  try {
    const res = await roomsApi.getJoinRequests(props.id)
    pendingRequests.value = res.data.data
  } catch {
    pendingRequests.value = []
  } finally {
    requestsLoading.value = false
  }
}

async function approveRequest(requestId: string) {
  try {
    await roomsApi.approveJoinRequest(props.id, requestId)
    toast.add({ severity: 'success', summary: t('rooms.requestApproved'), life: 3000 })
    await loadPendingRequests()
    await rooms.fetchRoom(props.id)
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function denyRequest(requestId: string) {
  try {
    await roomsApi.denyJoinRequest(props.id, requestId)
    toast.add({ severity: 'success', summary: t('rooms.requestDenied'), life: 3000 })
    await loadPendingRequests()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function openAddFamilyDialog() {
  try {
    const res = await familyApi.getAll()
    families.value = res.data.data
    selectedFamilyId.value = null
    showAddFamilyDialog.value = true
  } catch {
    // ignore
  }
}

async function addFamilyToRoom() {
  if (!selectedFamilyId.value) return
  addingFamily.value = true
  try {
    await roomsApi.addFamily(props.id, selectedFamilyId.value)
    toast.add({ severity: 'success', summary: t('rooms.familyAdded'), life: 3000 })
    showAddFamilyDialog.value = false
    await rooms.fetchRoom(props.id)
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    addingFamily.value = false
  }
}

function openAddMemberDialog() {
  memberSearchQuery.value = ''
  memberSearchResults.value = []
  memberRoleFilter.value = isKlasse.value ? 'TEACHER' : 'ALL'
  showAddMemberDialog.value = true
}

function onRoleFilterChange() {
  if (memberRoleFilter.value === 'FAMILIE') {
    showAddMemberDialog.value = false
    memberRoleFilter.value = 'ALL'
    openAddFamilyDialog()
  }
}

function onMemberSearch() {
  if (searchTimeout) clearTimeout(searchTimeout)
  const query = memberSearchQuery.value.trim()
  if (query.length < 2) {
    memberSearchResults.value = []
    return
  }
  searchTimeout = setTimeout(async () => {
    memberSearchLoading.value = true
    try {
      const res = await usersApi.search(query)
      memberSearchResults.value = res.data.data.content
    } catch {
      memberSearchResults.value = []
    } finally {
      memberSearchLoading.value = false
    }
  }, 300)
}

function isMemberAlready(userId: string): boolean {
  return rooms.currentRoom?.members?.some(m => m.userId === userId) ?? false
}

async function addMemberToRoom(userId: string) {
  addingMemberId.value = userId
  try {
    await roomsApi.addMember(props.id, userId, 'MEMBER')
    toast.add({ severity: 'success', summary: t('rooms.memberAdded'), life: 3000 })
    await rooms.fetchRoom(props.id)
    // Update search results to reflect new membership
    memberSearchResults.value = [...memberSearchResults.value]
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    addingMemberId.value = null
  }
}

const roomRoleOptions = [
  { label: 'LEADER', value: 'LEADER' },
  { label: 'MEMBER', value: 'MEMBER' },
  { label: 'PARENT_MEMBER', value: 'PARENT_MEMBER' },
]

async function removeMemberFromRoom(userId: string) {
  try {
    await roomsApi.removeMember(props.id, userId)
    toast.add({ severity: 'success', summary: t('rooms.memberRemoved'), life: 3000 })
    await rooms.fetchRoom(props.id)
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function changeMemberRole(userId: string, newRole: string) {
  try {
    await roomsApi.updateMemberRole(props.id, userId, newRole as any)
    toast.add({ severity: 'success', summary: t('rooms.roleChanged'), life: 3000 })
    await rooms.fetchRoom(props.id)
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function toggleMute() {
  try {
    if (roomMuted.value) {
      await rooms.unmuteRoom(props.id)
      roomMuted.value = false
      toast.add({ severity: 'success', summary: t('rooms.unmuted'), life: 3000 })
    } else {
      await rooms.muteRoom(props.id)
      roomMuted.value = true
      toast.add({ severity: 'success', summary: t('rooms.muted'), life: 3000 })
    }
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}
</script>

<template>
  <div>
    <Button
      icon="pi pi-arrow-left"
      :label="t('common.back')"
      severity="secondary"
      text
      @click="router.push({ name: 'rooms' })"
      class="mb-1"
    />

    <LoadingSpinner v-if="rooms.loading" />

    <!-- Public view (non-member) -->
    <template v-else-if="rooms.currentPublicRoom && !rooms.currentRoom?.members">
      <div class="room-header">
        <div class="room-header-left">
          <AvatarUpload
            :image-url="rooms.currentPublicRoom.avatarUrl"
            size="lg"
            icon="pi-home"
            :editable="false"
          />
          <div>
            <PageTitle :title="rooms.currentPublicRoom.name" />
            <Tag
              :value="t(`rooms.types.${rooms.currentPublicRoom.type}`)"
              severity="info"
            />
          </div>
        </div>
      </div>

      <div class="card room-public-info">
        <p v-if="rooms.currentPublicRoom.publicDescription" class="public-desc">{{ rooms.currentPublicRoom.publicDescription }}</p>
        <p v-else class="text-muted">{{ t('rooms.notMember') }}</p>

        <div class="room-public-meta">
          <span><i class="pi pi-users" /> {{ rooms.currentPublicRoom.memberCount }} {{ t('rooms.members') }}</span>
        </div>

        <div v-if="rooms.currentPublicRoom.tags?.length" class="room-tags">
          <Tag v-for="tag in rooms.currentPublicRoom.tags" :key="tag" :value="tag" severity="secondary" />
        </div>

        <div class="join-section">
          <p class="text-muted">{{ t('rooms.joinToSee') }}</p>
          <Button
            v-if="rooms.currentPublicRoom.joinPolicy === 'OPEN'"
            :label="t('discover.join')"
            icon="pi pi-sign-in"
            @click="joinRoom"
          />
          <Button
            v-else-if="rooms.currentPublicRoom.joinPolicy === 'REQUEST'"
            :label="t('rooms.requestJoin')"
            icon="pi pi-send"
            severity="secondary"
            @click="showJoinRequestDialog = true"
          />
          <Tag
            v-else
            :value="t('rooms.inviteOnly')"
            severity="warn"
          />
        </div>
      </div>
    </template>

    <!-- Full view (member / admin) -->
    <template v-else-if="rooms.currentRoom">
      <div class="room-header">
        <div class="room-header-left">
          <AvatarUpload
            :image-url="rooms.currentRoom.avatarUrl"
            size="lg"
            icon="pi-home"
            :editable="canEditRoom"
            @upload="handleRoomAvatarUpload"
            @remove="handleRoomAvatarRemove"
          />
          <div>
            <PageTitle :title="rooms.currentRoom.name" />
            <Tag
              :value="t(`rooms.types.${rooms.currentRoom.type}`)"
              severity="info"
            />
          </div>
        </div>
      </div>

      <div class="room-info card" v-if="rooms.currentRoom.description">
        <p>{{ rooms.currentRoom.description }}</p>
      </div>

      <!-- Mute/Unmute feed toggle -->
      <div class="card mute-card" v-if="!canEditRoom">
        <Button
          v-if="!roomMuted"
          :label="t('rooms.muteFeed')"
          icon="pi pi-volume-off"
          severity="secondary"
          text
          size="small"
          @click="toggleMute"
        />
        <Button
          v-else
          :label="t('rooms.unmuteFeed')"
          icon="pi pi-volume-up"
          severity="secondary"
          text
          size="small"
          @click="toggleMute"
        />
      </div>

      <!-- Create Putzaktion for this room (leader/admin/putzorga) -->
      <div v-if="canEditRoom" class="card" style="display: flex; align-items: center; gap: 0.75rem;">
        <Button
          :label="t('cleaning.createForRoom')"
          icon="pi pi-sparkles"
          severity="secondary"
          @click="router.push({ name: 'admin-cleaning', query: { roomId: id, roomName: rooms.currentRoom?.name } })"
        />
      </div>

      <!-- Public description (editable by leader) -->
      <div v-if="canEditRoom" class="card public-desc-card">
        <div class="public-desc-header">
          <label>{{ t('rooms.publicDescription') }}</label>
          <Button
            v-if="!editingPublicDesc"
            icon="pi pi-pencil"
            text
            size="small"
            @click="startEditPublicDesc"
          />
        </div>
        <template v-if="editingPublicDesc">
          <Textarea
            v-model="publicDescDraft"
            :placeholder="t('rooms.publicDescPlaceholder')"
            class="w-full"
            rows="3"
          />
          <div class="public-desc-actions">
            <Button :label="t('common.cancel')" severity="secondary" text size="small" @click="editingPublicDesc = false" />
            <Button :label="t('common.save')" size="small" @click="savePublicDesc" />
          </div>
        </template>
        <p v-else-if="rooms.currentRoom.publicDescription" class="public-desc-text">
          {{ rooms.currentRoom.publicDescription }}
        </p>
        <p v-else class="text-muted text-sm">{{ t('rooms.publicDescPlaceholder') }}</p>
      </div>

      <Tabs :value="activeTab">
        <TabList>
          <Tab value="0">{{ t('rooms.infoBoard') }}</Tab>
          <Tab value="1">{{ t('rooms.members') }} ({{ rooms.currentRoom.members?.length ?? 0 }})</Tab>
          <Tab value="2">{{ t('discussions.title') }}</Tab>
          <Tab v-if="chatEnabled" value="3">{{ t('chat.title') }}</Tab>
          <Tab v-if="filesEnabled" value="4">{{ t('files.title') }}</Tab>
          <Tab v-if="calendarEnabled" value="5">{{ t('calendar.title') }}</Tab>
          <Tab v-if="fotoboxEnabled" value="6">{{ t('fotobox.title') }}</Tab>
          <Tab v-if="tasksEnabled" value="7">{{ t('tasks.title') }}</Tab>
          <Tab v-if="wikiEnabled" value="8">{{ t('wiki.title') }}</Tab>
        </TabList>
        <TabPanels>
          <!-- Info-Board Tab -->
          <TabPanel value="0">
            <h2 class="sr-only">{{ t('rooms.infoBoard') }}</h2>
            <PostComposer
              v-if="auth.isTeacher || auth.isAdmin"
              @submit="handlePost"
            />
            <LoadingSpinner v-if="postsLoading" />
            <template v-else-if="roomPosts.length">
              <FeedPostComponent
                v-for="post in roomPosts"
                :key="post.id"
                :post="post"
              />
            </template>
            <p v-else class="text-muted text-center">{{ t('rooms.noPosts') }}</p>
          </TabPanel>

          <!-- Members Tab -->
          <TabPanel value="1">
            <h2 class="sr-only">{{ t('rooms.members') }}</h2>
            <div v-if="canEditRoom" class="member-actions mb-3">
              <Button
                :label="isKlasse ? t('rooms.addTeacher') : t('rooms.addMember')"
                icon="pi pi-user-plus"
                size="small"
                @click="openAddMemberDialog"
              />
              <Button
                :label="t('rooms.addFamily')"
                icon="pi pi-users"
                size="small"
                severity="secondary"
                @click="openAddFamilyDialog"
              />
            </div>

            <!-- Pending join requests (Leader only) -->
            <div v-if="(isLeader || auth.isAdmin || auth.isSectionAdmin) && pendingRequests.length > 0" class="pending-requests mb-4">
              <h3 class="text-md font-semibold mb-2">{{ t('rooms.pendingRequests') }} ({{ pendingRequests.length }})</h3>
              <div v-for="req in pendingRequests" :key="req.id" class="request-item">
                <div class="request-info">
                  <i class="pi pi-user" />
                  <div>
                    <span class="font-medium">{{ req.userName }}</span>
                    <p v-if="req.message" class="text-sm text-muted">{{ req.message }}</p>
                  </div>
                </div>
                <div class="request-actions">
                  <Button :label="t('rooms.approve')" icon="pi pi-check" size="small"
                          severity="success" @click="approveRequest(req.id)" />
                  <Button :label="t('rooms.deny')" icon="pi pi-times" size="small"
                          severity="danger" text @click="denyRequest(req.id)" />
                </div>
              </div>
            </div>

            <div v-if="rooms.currentRoom.members?.length" class="members-grouped">
              <!-- Teachers / Leaders -->
              <div v-if="leaderMembers.length" class="member-group">
                <h3 class="member-group-title">{{ t('rooms.teachers') }}</h3>
                <div class="members-list">
                  <div v-for="member in leaderMembers" :key="member.userId" class="member-item">
                    <div class="member-avatar">
                      <img v-if="member.avatarUrl" :src="member.avatarUrl" alt="" class="member-avatar-img" />
                      <i v-else class="pi pi-user" />
                    </div>
                    <span class="member-name">{{ member.displayName }}</span>
                    <div v-if="canEditRoom && member.userId !== auth.user?.id" class="member-controls">
                      <Select
                        :modelValue="member.role"
                        :options="roomRoleOptions"
                        optionLabel="label"
                        optionValue="value"
                        size="small"
                        class="role-select"
                        @update:modelValue="changeMemberRole(member.userId, $event)"
                      />
                      <Button
                        icon="pi pi-times"
                        text rounded
                        severity="danger"
                        size="small"
                        @click="removeMemberFromRoom(member.userId)"
                      />
                    </div>
                    <Tag v-else :value="t(`rooms.roles.${member.role}`)" severity="secondary" size="small" />
                  </div>
                </div>
              </div>

              <!-- Families -->
              <div v-if="familyGroups.length" class="member-group">
                <h3 class="member-group-title">{{ t('rooms.families') }}</h3>
                <div v-for="family in familyGroups" :key="family.familyId" class="family-group">
                  <div class="family-group-header">
                    <i class="pi pi-users" />
                    <span class="family-group-name">{{ family.familyName }}</span>
                  </div>
                  <div class="members-list">
                    <div v-for="member in family.members" :key="member.userId" class="member-item">
                      <div class="member-avatar">
                        <img v-if="member.avatarUrl" :src="member.avatarUrl" alt="" class="member-avatar-img" />
                        <i v-else class="pi pi-user" />
                      </div>
                      <span class="member-name">{{ member.displayName }}</span>
                      <div v-if="canEditRoom && member.userId !== auth.user?.id" class="member-controls">
                        <Select
                          :modelValue="member.role"
                          :options="roomRoleOptions"
                          optionLabel="label"
                          optionValue="value"
                          size="small"
                          class="role-select"
                          @update:modelValue="changeMemberRole(member.userId, $event)"
                        />
                        <Button
                          icon="pi pi-times"
                          text rounded
                          severity="danger"
                          size="small"
                          @click="removeMemberFromRoom(member.userId)"
                        />
                      </div>
                      <Tag v-else :value="t(`rooms.roles.${member.role}`)" severity="secondary" size="small" />
                    </div>
                  </div>
                </div>
              </div>

              <!-- Other members -->
              <div v-if="otherMembers.length" class="member-group">
                <h3 class="member-group-title">{{ t('rooms.otherMembers') }}</h3>
                <div class="members-list">
                  <div v-for="member in otherMembers" :key="member.userId" class="member-item">
                    <div class="member-avatar">
                      <img v-if="member.avatarUrl" :src="member.avatarUrl" alt="" class="member-avatar-img" />
                      <i v-else class="pi pi-user" />
                    </div>
                    <span class="member-name">{{ member.displayName }}</span>
                    <div v-if="canEditRoom && member.userId !== auth.user?.id" class="member-controls">
                      <Select
                        :modelValue="member.role"
                        :options="roomRoleOptions"
                        optionLabel="label"
                        optionValue="value"
                        size="small"
                        class="role-select"
                        @update:modelValue="changeMemberRole(member.userId, $event)"
                      />
                      <Button
                        icon="pi pi-times"
                        text rounded
                        severity="danger"
                        size="small"
                        @click="removeMemberFromRoom(member.userId)"
                      />
                    </div>
                    <Tag v-else :value="t(`rooms.roles.${member.role}`)" severity="secondary" size="small" />
                  </div>
                </div>
              </div>
            </div>
            <p v-else class="text-muted">{{ t('rooms.noMembers') }}</p>
          </TabPanel>

          <!-- Discussions Tab -->
          <TabPanel value="2">
            <RoomDiscussions :roomId="id" />
          </TabPanel>

          <!-- Chat Tab -->
          <TabPanel v-if="chatEnabled" value="3">
            <RoomChat :roomId="id" />
          </TabPanel>

          <!-- Files Tab -->
          <TabPanel v-if="filesEnabled" value="4">
            <RoomFiles :roomId="id" />
          </TabPanel>

          <!-- Events Tab -->
          <TabPanel v-if="calendarEnabled" value="5">
            <RoomEvents :roomId="id" />
          </TabPanel>

          <!-- Fotobox Tab -->
          <TabPanel v-if="fotoboxEnabled" value="6">
            <RoomFotobox :room-id="id" :is-leader="isLeader" />
          </TabPanel>

          <!-- Tasks Tab -->
          <TabPanel v-if="tasksEnabled" value="7">
            <RoomTasks :room-id="id" :is-leader="isLeader" />
          </TabPanel>

          <!-- Wiki Tab -->
          <TabPanel v-if="wikiEnabled" value="8">
            <RoomWiki :room-id="id" :is-leader="isLeader" />
          </TabPanel>
        </TabPanels>
      </Tabs>
    </template>

    <!-- Join Request Dialog -->
    <Dialog v-model:visible="showJoinRequestDialog" :header="t('rooms.requestJoin')" modal :style="{ width: '450px', maxWidth: '90vw' }">
      <div class="flex flex-col gap-3">
        <p>{{ t('rooms.joinRequestMessage', { room: rooms.currentPublicRoom?.name ?? '' }) }}</p>
        <Textarea v-model="joinRequestMessage" :placeholder="t('rooms.joinRequestPlaceholder')"
                  class="w-full" rows="3" />
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" text @click="showJoinRequestDialog = false" />
        <Button :label="t('rooms.requestJoin')" icon="pi pi-send"
                :loading="joinRequestLoading" @click="submitJoinRequest" />
      </template>
    </Dialog>

    <!-- Add Member Dialog -->
    <Dialog v-model:visible="showAddMemberDialog" :header="isKlasse ? t('rooms.addTeacher') : t('rooms.addMember')" modal :style="{ width: '500px', maxWidth: '90vw' }">
      <div class="add-member-form">
        <Select
          v-if="!isKlasse"
          v-model="memberRoleFilter"
          :options="roleFilterOptions"
          optionLabel="label"
          optionValue="value"
          class="w-full"
          @change="onRoleFilterChange"
        />
        <InputText
          v-model="memberSearchQuery"
          :placeholder="t('rooms.searchMemberPlaceholder')"
          class="w-full"
          @input="onMemberSearch"
        />
        <div v-if="memberSearchLoading" class="text-muted text-sm" style="padding: 0.5rem 0;">
          <i class="pi pi-spinner pi-spin" /> {{ t('rooms.searchMember') }}...
        </div>
        <div v-else-if="filteredSearchResults.length" class="member-search-results">
          <div v-for="user in filteredSearchResults" :key="user.id" class="member-search-item">
            <div class="member-search-info">
              <strong>{{ user.displayName }}</strong>
              <span class="text-muted text-sm">{{ user.email }}</span>
              <Tag v-if="user.role" :value="user.role" severity="secondary" size="small" />
            </div>
            <Button
              v-if="!isMemberAlready(user.id)"
              :label="t('common.add')"
              icon="pi pi-plus"
              size="small"
              :loading="addingMemberId === user.id"
              @click="addMemberToRoom(user.id)"
            />
            <Tag v-else :value="t('rooms.alreadyMember')" severity="info" size="small" />
          </div>
        </div>
        <p v-else-if="memberSearchQuery.trim().length >= 2 && !memberSearchLoading" class="text-muted text-sm" style="padding: 0.5rem 0;">
          {{ t('common.noResults') }}
        </p>
      </div>
      <template #footer>
        <Button :label="t('common.close')" severity="secondary" text @click="showAddMemberDialog = false" />
      </template>
    </Dialog>

    <!-- Add Family Dialog -->
    <Dialog v-model:visible="showAddFamilyDialog" :header="t('rooms.addFamily')" modal :style="{ width: '450px', maxWidth: '90vw' }">
      <div v-if="families.length" class="add-family-form">
        <label>{{ t('rooms.selectFamily') }}</label>
        <Select
          v-model="selectedFamilyId"
          :options="families.map(f => ({ label: `${f.name} (${f.members.length} ${t('rooms.members')})`, value: f.id }))"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('rooms.selectFamily')"
          class="w-full"
          filter
        />
      </div>
      <p v-else class="text-muted">{{ t('rooms.noFamilies') }}</p>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" text @click="showAddFamilyDialog = false" />
        <Button
          :label="t('rooms.addFamily')"
          icon="pi pi-users"
          :loading="addingFamily"
          :disabled="!selectedFamilyId"
          @click="addFamilyToRoom"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.room-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.room-header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.room-info {
  margin-bottom: 1rem;
}

.room-public-info {
  margin-top: 1rem;
}

.public-desc {
  margin-bottom: 1rem;
  line-height: 1.5;
}

.room-public-meta {
  color: var(--mw-text-muted);
  font-size: var(--mw-font-size-sm);
  margin-bottom: 0.75rem;
}

.room-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.375rem;
  margin-bottom: 1rem;
}

.join-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 1.5rem;
  border-top: 1px solid var(--mw-border-light);
  margin-top: 1rem;
}

.mute-card {
  margin-bottom: 1rem;
  display: flex;
  justify-content: flex-end;
}

.public-desc-card {
  margin-bottom: 1rem;
}

.public-desc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.public-desc-header label {
  font-size: var(--mw-font-size-sm);
  font-weight: 600;
  color: var(--mw-text-secondary);
}

.public-desc-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.public-desc-text {
  color: var(--mw-text-secondary);
  font-size: var(--mw-font-size-sm);
}

.text-muted {
  color: var(--mw-text-muted);
}

.text-sm {
  font-size: var(--mw-font-size-sm);
}

.text-center {
  text-align: center;
  padding: 2rem;
}

.members-grouped {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.member-group-title {
  font-size: var(--mw-font-size-base, 1rem);
  font-weight: 600;
  color: var(--mw-text-secondary);
  margin-bottom: 0.5rem;
}

.family-group {
  margin-bottom: 0.75rem;
}

.family-group:last-child {
  margin-bottom: 0;
}

.family-group-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0.5rem;
  margin-bottom: 0.375rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
}

.family-group-name {
  font-weight: 500;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem;
  background: var(--mw-bg-card);
  border-radius: var(--mw-border-radius-sm);
  border: 1px solid var(--mw-border-light);
}

.member-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--mw-bg);
  color: var(--mw-text-muted);
  flex-shrink: 0;
}

.member-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.member-name {
  flex: 1;
  min-width: 0;
}

.member-controls {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-left: auto;
}

.role-select {
  width: 10rem;
}

.pending-requests {
  padding: 0.75rem;
  background: var(--mw-bg);
  border-radius: var(--mw-border-radius-sm);
  border: 1px solid var(--mw-border-light);
}

.request-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--mw-border-light);
}

.request-item:last-child {
  border-bottom: none;
}

.request-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.request-info i {
  color: var(--mw-text-muted);
}

.request-actions {
  display: flex;
  gap: 0.375rem;
  flex-shrink: 0;
}

.member-actions {
  display: flex;
  gap: 0.5rem;
}

.add-family-form {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.add-family-form label {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
}

.add-member-form {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.member-search-results {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  max-height: 300px;
  overflow-y: auto;
}

.member-search-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-sm);
}

.member-search-info {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.member-search-info strong {
  font-size: var(--mw-font-size-sm);
}

.member-search-info span {
  font-size: var(--mw-font-size-xs);
}

@media (max-width: 767px) {
  .member-item {
    flex-wrap: wrap;
  }
  .member-controls {
    width: 100%;
    margin-left: 0;
    margin-top: 0.25rem;
    padding-left: 2.75rem; /* aligned with name after avatar */
  }
  .role-select {
    width: auto;
    flex: 1;
  }
  .member-actions {
    flex-direction: column;
  }
  .member-actions :deep(.p-button) {
    width: 100%;
    justify-content: center;
  }
  .request-item {
    flex-direction: column;
    align-items: flex-start;
  }
  .request-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
