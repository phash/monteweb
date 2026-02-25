<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessagingStore } from '@/stores/messaging'
import { usersApi } from '@/api/users.api'
import type { UserInfo } from '@/types/user'
import Dialog from 'primevue/dialog'
import AutoComplete from 'primevue/autocomplete'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import ToggleSwitch from 'primevue/toggleswitch'
import Tag from 'primevue/tag'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'conversation-started': [conversationId: string]
}>()

const { t } = useI18n()
const messaging = useMessagingStore()

const selectedUser = ref<UserInfo | null>(null)
const selectedUsers = ref<UserInfo[]>([])
const suggestions = ref<UserInfo[]>([])
const loading = ref(false)
const error = ref('')
const isGroup = ref(false)
const groupName = ref('')

const canStart = computed(() => {
  if (isGroup.value) {
    return selectedUsers.value.length >= 2 && groupName.value.trim().length > 0
  }
  return selectedUser.value !== null
})

async function searchUsers(event: { query: string }) {
  if (event.query.length < 2) {
    suggestions.value = []
    return
  }
  try {
    const res = await usersApi.search(event.query)
    // Filter out already selected users
    const selectedIds = new Set(selectedUsers.value.map(u => u.id))
    suggestions.value = res.data.data.content.filter(u => !selectedIds.has(u.id))
  } catch {
    suggestions.value = []
  }
}

function onGroupUserSelect(event: { value: UserInfo }) {
  if (!selectedUsers.value.find(u => u.id === event.value.id)) {
    selectedUsers.value.push(event.value)
  }
  selectedUser.value = null
}

function removeUser(userId: string) {
  selectedUsers.value = selectedUsers.value.filter(u => u.id !== userId)
}

async function startConversation() {
  if (!canStart.value) return
  loading.value = true
  error.value = ''
  try {
    if (isGroup.value) {
      const participantIds = selectedUsers.value.map(u => u.id)
      const conv = await messaging.startGroupConversation(groupName.value.trim(), participantIds)
      emit('conversation-started', conv.id)
    } else {
      const conv = await messaging.startDirectConversation(selectedUser.value!.id)
      emit('conversation-started', conv.id)
    }
    emit('update:visible', false)
    resetForm()
  } catch (e: any) {
    error.value = e.response?.data?.message || t('messages.communicationNotAllowed')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  selectedUser.value = null
  selectedUsers.value = []
  groupName.value = ''
  error.value = ''
}

function onHide() {
  emit('update:visible', false)
  resetForm()
}
</script>

<template>
  <Dialog
    :visible="props.visible"
    :header="t('messages.newMessage')"
    modal
    :style="{ width: '500px', maxWidth: '90vw' }"
    @update:visible="onHide"
  >
    <div class="new-message-form">
      <div class="group-toggle">
        <label class="toggle-label">
          <ToggleSwitch v-model="isGroup" />
          <span>{{ t('messages.groupConversation') }}</span>
        </label>
      </div>

      <!-- Group name -->
      <div v-if="isGroup" class="form-field">
        <label>{{ t('messages.groupName') }}</label>
        <InputText
          v-model="groupName"
          :placeholder="t('messages.groupNamePlaceholder')"
          class="w-full"
        />
      </div>

      <!-- User search -->
      <div class="form-field">
        <label>{{ t('messages.searchUser') }}</label>
        <AutoComplete
          v-model="selectedUser"
          :suggestions="suggestions"
          optionLabel="displayName"
          :placeholder="t('messages.searchUserPlaceholder')"
          @complete="searchUsers"
          @item-select="isGroup ? onGroupUserSelect($event) : undefined"
          class="w-full"
        >
          <template #option="{ option }">
            <div class="user-option">
              <i class="pi pi-user" />
              <div>
                <div>{{ option.displayName }}</div>
                <small class="text-muted">{{ option.email }}</small>
              </div>
            </div>
          </template>
        </AutoComplete>
      </div>

      <!-- Selected group members -->
      <div v-if="isGroup && selectedUsers.length" class="selected-users">
        <Tag
          v-for="u in selectedUsers"
          :key="u.id"
          :value="u.displayName"
          severity="info"
          class="user-tag"
        >
          <template #default>
            <span class="tag-content">
              {{ u.displayName }}
              <button class="tag-remove" @click="removeUser(u.id)">
                <i class="pi pi-times" />
              </button>
            </span>
          </template>
        </Tag>
      </div>

      <small v-if="error" class="error-text">{{ error }}</small>
    </div>

    <template #footer>
      <Button
        :label="t('common.cancel')"
        severity="secondary"
        text
        @click="onHide"
      />
      <Button
        :label="t('messages.startConversation')"
        :disabled="!canStart || loading"
        :loading="loading"
        @click="startConversation"
      />
    </template>
  </Dialog>
</template>

<style scoped>
.new-message-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.group-toggle {
  margin-bottom: 0.25rem;
}

.toggle-label {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: var(--mw-font-size-sm);
  cursor: pointer;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-field label {
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
}

.user-option {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-option i {
  color: var(--mw-text-muted);
}

.text-muted {
  color: var(--mw-text-muted);
}

.selected-users {
  display: flex;
  flex-wrap: wrap;
  gap: 0.375rem;
}

.user-tag {
  font-size: var(--mw-font-size-xs);
}

.tag-content {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
}

.tag-remove {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  color: inherit;
  opacity: 0.7;
  font-size: 0.625rem;
  line-height: 1;
}

.tag-remove:hover {
  opacity: 1;
}

.error-text {
  color: var(--p-red-500, #ef4444);
}
</style>
