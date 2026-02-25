<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoomsStore } from '@/stores/rooms'
import { roomsApi } from '@/api/rooms.api'
import { useToast } from 'primevue/usetoast'
import type { RoomSettings as RoomSettingsType } from '@/types/room'
import ToggleSwitch from 'primevue/toggleswitch'
import Select from 'primevue/select'
import Button from 'primevue/button'

const props = defineProps<{ roomId: string }>()
const { t } = useI18n()
const rooms = useRoomsStore()
const toast = useToast()

const saving = ref(false)
const settings = ref<RoomSettingsType>({
  chatEnabled: true,
  filesEnabled: true,
  parentSpaceEnabled: false,
  visibility: 'MEMBERS_ONLY',
  discussionMode: 'FULL',
  allowMemberThreadCreation: true,
  childDiscussionEnabled: false,
})

const visibilityOptions = [
  { label: t('rooms.settings.visibilityMembersOnly'), value: 'MEMBERS_ONLY' },
  { label: t('rooms.settings.visibilitySection'), value: 'SECTION' },
  { label: t('rooms.settings.visibilityAll'), value: 'ALL' },
]

const discussionModeOptions = [
  { label: t('rooms.settings.discussionFull'), value: 'FULL' },
  { label: t('rooms.settings.discussionAnnouncementsOnly'), value: 'ANNOUNCEMENTS_ONLY' },
  { label: t('rooms.settings.discussionDisabled'), value: 'DISABLED' },
]

onMounted(() => {
  if (rooms.currentRoom?.settings) {
    settings.value = { ...rooms.currentRoom.settings }
  }
})

async function saveSettings() {
  saving.value = true
  try {
    await roomsApi.updateSettings(props.roomId, settings.value)
    await rooms.fetchRoom(props.roomId)
    toast.add({ severity: 'success', summary: t('rooms.settings.saved'), life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: t('common.error'), life: 3000 })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="room-settings">
    <h3>{{ t('rooms.settings.title') }}</h3>

    <div class="settings-section">
      <h4>{{ t('rooms.settings.modules') }}</h4>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">{{ t('chat.title') }}</span>
          <span class="setting-hint">{{ t('rooms.settings.chatHint') }}</span>
        </div>
        <ToggleSwitch v-model="settings.chatEnabled" />
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">{{ t('files.title') }}</span>
          <span class="setting-hint">{{ t('rooms.settings.filesHint') }}</span>
        </div>
        <ToggleSwitch v-model="settings.filesEnabled" />
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">{{ t('rooms.settings.parentSpace') }}</span>
          <span class="setting-hint">{{ t('rooms.settings.parentSpaceHint') }}</span>
        </div>
        <ToggleSwitch v-model="settings.parentSpaceEnabled" />
      </div>
    </div>

    <div class="settings-section">
      <h4>{{ t('rooms.settings.access') }}</h4>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">{{ t('rooms.settings.visibility') }}</span>
        </div>
        <Select
          v-model="settings.visibility"
          :options="visibilityOptions"
          optionLabel="label"
          optionValue="value"
          class="setting-select"
        />
      </div>
    </div>

    <div class="settings-section">
      <h4>{{ t('discussions.title') }}</h4>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">{{ t('rooms.settings.discussionMode') }}</span>
        </div>
        <Select
          v-model="settings.discussionMode"
          :options="discussionModeOptions"
          optionLabel="label"
          optionValue="value"
          class="setting-select"
        />
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">{{ t('rooms.settings.allowThreadCreation') }}</span>
          <span class="setting-hint">{{ t('rooms.settings.allowThreadCreationHint') }}</span>
        </div>
        <ToggleSwitch v-model="settings.allowMemberThreadCreation" />
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-label">{{ t('rooms.settings.childDiscussion') }}</span>
          <span class="setting-hint">{{ t('rooms.settings.childDiscussionHint') }}</span>
        </div>
        <ToggleSwitch v-model="settings.childDiscussionEnabled" />
      </div>
    </div>

    <div class="settings-actions">
      <Button
        :label="t('common.save')"
        :loading="saving"
        @click="saveSettings"
      />
    </div>
  </div>
</template>

<style scoped>
.room-settings {
  max-width: 600px;
}

.room-settings h3 {
  margin: 0 0 1.5rem;
}

.settings-section {
  margin-bottom: 1.5rem;
}

.settings-section h4 {
  margin: 0 0 0.75rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--mw-border-light);
}

.setting-row:last-child {
  border-bottom: none;
}

.setting-info {
  flex: 1;
  min-width: 0;
}

.setting-label {
  display: block;
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
}

.setting-hint {
  display: block;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.125rem;
}

.setting-select {
  min-width: 180px;
}

.settings-actions {
  margin-top: 1rem;
}
</style>
