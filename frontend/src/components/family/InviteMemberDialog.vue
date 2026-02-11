<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { usersApi } from '@/api/users.api'
import { familyApi } from '@/api/family.api'
import type { UserInfo } from '@/types/user'
import Dialog from 'primevue/dialog'
import AutoComplete from 'primevue/autocomplete'
import Select from 'primevue/select'
import Button from 'primevue/button'

const props = defineProps<{ visible: boolean; familyId: string }>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'invited': []
}>()

const { t } = useI18n()

const selectedUser = ref<UserInfo | null>(null)
const suggestions = ref<UserInfo[]>([])
const selectedRole = ref('PARENT')
const loading = ref(false)
const error = ref('')

const roleOptions = [
  { label: t('family.roles.PARENT'), value: 'PARENT' },
  { label: t('family.roles.CHILD'), value: 'CHILD' },
]

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

async function sendInvitation() {
  if (!selectedUser.value) return
  loading.value = true
  error.value = ''
  try {
    await familyApi.inviteMember(props.familyId, selectedUser.value.id, selectedRole.value)
    emit('invited')
    emit('update:visible', false)
    selectedUser.value = null
    selectedRole.value = 'PARENT'
  } catch (e: any) {
    error.value = e.response?.data?.message || t('error.unexpected')
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
    :header="t('family.inviteMember')"
    modal
    :style="{ width: '450px', maxWidth: '90vw' }"
    @update:visible="onHide"
  >
    <div class="invite-form">
      <label>{{ t('family.searchMember') }}</label>
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

      <label>{{ t('family.roles.PARENT') }} / {{ t('family.roles.CHILD') }}</label>
      <Select
        v-model="selectedRole"
        :options="roleOptions"
        optionLabel="label"
        optionValue="value"
        class="w-full"
      />

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
        :label="t('family.sendInvitation')"
        :disabled="!selectedUser || loading"
        :loading="loading"
        @click="sendInvitation"
      />
    </template>
  </Dialog>
</template>

<style scoped>
.invite-form {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.invite-form label {
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
  margin-top: 0.25rem;
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
