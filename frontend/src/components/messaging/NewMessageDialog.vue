<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessagingStore } from '@/stores/messaging'
import { usersApi } from '@/api/users.api'
import type { UserInfo } from '@/types/user'
import Dialog from 'primevue/dialog'
import AutoComplete from 'primevue/autocomplete'
import Button from 'primevue/button'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'conversation-started': [conversationId: string]
}>()

const { t } = useI18n()
const messaging = useMessagingStore()

const selectedUser = ref<UserInfo | null>(null)
const suggestions = ref<UserInfo[]>([])
const loading = ref(false)
const error = ref('')

async function searchUsers(event: { query: string }) {
  if (event.query.length < 2) {
    suggestions.value = []
    return
  }
  try {
    const res = await usersApi.search(event.query)
    suggestions.value = res.data.data.content
  } catch {
    suggestions.value = []
  }
}

async function startConversation() {
  if (!selectedUser.value) return
  loading.value = true
  error.value = ''
  try {
    const conv = await messaging.startDirectConversation(selectedUser.value.id)
    emit('conversation-started', conv.id)
    emit('update:visible', false)
    selectedUser.value = null
  } catch (e: any) {
    error.value = e.response?.data?.message || t('messages.communicationNotAllowed')
  } finally {
    loading.value = false
  }
}

function onHide() {
  emit('update:visible', false)
  selectedUser.value = null
  error.value = ''
}
</script>

<template>
  <Dialog
    :visible="props.visible"
    :header="t('messages.newMessage')"
    modal
    :style="{ width: '450px' }"
    @update:visible="onHide"
  >
    <div class="new-message-form">
      <label>{{ t('messages.searchUser') }}</label>
      <AutoComplete
        v-model="selectedUser"
        :suggestions="suggestions"
        optionLabel="displayName"
        :placeholder="t('messages.searchUserPlaceholder')"
        @complete="searchUsers"
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
        :disabled="!selectedUser || loading"
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
  gap: 0.5rem;
}

.new-message-form label {
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
}

.w-full {
  width: 100%;
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

.error-text {
  color: var(--p-red-500, #ef4444);
}
</style>
