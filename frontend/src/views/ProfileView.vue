<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'primevue/usetoast'
import { usersApi } from '@/api/users.api'
import { usePushNotifications } from '@/composables/usePushNotifications'
import PageTitle from '@/components/common/PageTitle.vue'
import AvatarUpload from '@/components/common/AvatarUpload.vue'
import InputText from 'primevue/inputtext'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import type { UserRole } from '@/types/user'

const { t } = useI18n()
const auth = useAuthStore()
const router = useRouter()
const toast = useToast()
const { isSupported: pushSupported, isSubscribed: pushSubscribed, permission: pushPermission,
        checkSubscription, subscribe: pushSubscribe, unsubscribe: pushUnsubscribe } = usePushNotifications()

const pushEnabled = ref(false)
const switching = ref(false)

const form = ref({
  firstName: '',
  lastName: '',
  phone: '',
})
const saved = ref(false)

onMounted(async () => {
  if (auth.user) {
    form.value.firstName = auth.user.firstName
    form.value.lastName = auth.user.lastName
    form.value.phone = auth.user.phone ?? ''
  }
  await checkSubscription()
  pushEnabled.value = pushSubscribed.value
})

async function save() {
  await usersApi.updateMe(form.value)
  await auth.fetchUser()
  saved.value = true
  setTimeout(() => { saved.value = false }, 3000)
}

async function handleAvatarUpload(file: File) {
  await usersApi.uploadAvatar(file)
  await auth.fetchUser()
}

async function handleAvatarRemove() {
  await usersApi.removeAvatar()
  await auth.fetchUser()
}

function roleSeverity(role: string): string {
  const map: Record<string, string> = {
    SUPERADMIN: 'danger',
    SECTION_ADMIN: 'warn',
    TEACHER: 'info',
    PARENT: 'success',
    STUDENT: 'secondary',
  }
  return map[role] ?? 'secondary'
}

function specialRoleSeverity(role: string): string {
  if (role.startsWith('PUTZORGA')) return 'warn'
  if (role.startsWith('ELTERNBEIRAT')) return 'info'
  return 'secondary'
}

async function onSwitchRole(role: string) {
  switching.value = true
  try {
    await auth.switchRole(role as UserRole)
    toast.add({ severity: 'success', summary: t('profile.roleSwitched', { role: t('profile.roleLabels.' + role) }), life: 3000 })
    router.go(0)
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  } finally {
    switching.value = false
  }
}

async function togglePush() {
  if (pushEnabled.value) {
    const ok = await pushSubscribe()
    if (!ok) pushEnabled.value = false
  } else {
    await pushUnsubscribe()
  }
}
</script>

<template>
  <div>
    <PageTitle :title="t('profile.title')" />

    <div class="card profile-card">
      <AvatarUpload
        :image-url="auth.user?.avatarUrl"
        size="lg"
        icon="pi-user"
        :editable="true"
        @upload="handleAvatarUpload"
        @remove="handleAvatarRemove"
      />

      <Message v-if="saved" severity="success" :closable="false">
        {{ t('profile.saved') }}
      </Message>

      <form @submit.prevent="save" class="profile-form">
        <div class="form-field">
          <label for="profile-email">{{ t('auth.email') }}</label>
          <InputText id="profile-email" :model-value="auth.user?.email" disabled class="w-full" />
        </div>

        <div class="form-row">
          <div class="form-field">
            <label for="profile-firstName">{{ t('auth.firstName') }}</label>
            <InputText id="profile-firstName" v-model="form.firstName" class="w-full" />
          </div>
          <div class="form-field">
            <label for="profile-lastName">{{ t('auth.lastName') }}</label>
            <InputText id="profile-lastName" v-model="form.lastName" class="w-full" />
          </div>
        </div>

        <div class="form-field">
          <label for="profile-phone">{{ t('auth.phone') }}</label>
          <InputText id="profile-phone" v-model="form.phone" class="w-full" />
        </div>

        <Button type="submit" :label="t('common.save')" />
      </form>
    </div>

    <!-- Active Role Switcher -->
    <div v-if="auth.canSwitchRole" class="card profile-card role-switcher-card">
      <h3>{{ t('profile.activeRole') }}</h3>
      <div class="role-switcher-buttons">
        <button
          v-for="role in auth.assignedRoles"
          :key="role"
          class="role-switch-btn"
          :class="{ active: role === auth.user?.role }"
          :disabled="switching || role === auth.user?.role"
          @click="onSwitchRole(role)"
        >
          <Tag
            :value="t('profile.roleLabels.' + role)"
            :severity="roleSeverity(role) as any"
            class="role-switch-tag"
          />
        </button>
      </div>
    </div>

    <!-- Roles -->
    <div class="card profile-card roles-card">
      <h3>{{ t('profile.roles') }}</h3>
      <div class="roles-list">
        <Tag
          v-if="auth.user?.role"
          :value="t('profile.roleLabels.' + auth.user.role)"
          :severity="roleSeverity(auth.user.role) as any"
        />
        <Tag
          v-for="sr in (auth.user?.specialRoles || [])"
          :key="sr"
          :value="sr"
          :severity="specialRoleSeverity(sr) as any"
        />
      </div>
    </div>

    <!-- Push Notifications -->
    <div v-if="pushSupported" class="card profile-card push-card">
      <h3>{{ t('profile.pushNotifications') }}</h3>
      <div class="push-toggle">
        <span>{{ t('profile.enablePush') }}</span>
        <ToggleSwitch v-model="pushEnabled" @update:model-value="togglePush" />
      </div>
      <p v-if="pushPermission === 'denied'" class="push-denied">
        {{ t('profile.pushDenied') }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.profile-card {
  max-width: 600px;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 1rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-field label {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
  color: var(--mw-text-secondary);
}

.role-switcher-card {
  margin-top: 1rem;
}

.role-switcher-card h3 {
  margin: 0 0 0.75rem 0;
  font-size: var(--mw-font-size-md);
}

.role-switcher-buttons {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.role-switch-btn {
  background: none;
  border: 2px solid transparent;
  padding: 0.25rem;
  cursor: pointer;
  border-radius: var(--p-border-radius);
  transition: border-color 0.15s;
}

.role-switch-btn:hover:not(:disabled) {
  border-color: var(--mw-primary);
}

.role-switch-btn.active {
  border-color: var(--mw-primary);
  cursor: default;
}

.role-switch-btn:disabled:not(.active) {
  opacity: 0.5;
  cursor: not-allowed;
}

.roles-card {
  margin-top: 1rem;
}

.roles-card h3 {
  margin: 0 0 0.75rem 0;
  font-size: var(--mw-font-size-md);
}

.roles-list {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.push-card {
  margin-top: 1rem;
}

.push-card h3 {
  margin: 0 0 0.75rem 0;
  font-size: var(--mw-font-size-md);
}

.push-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.push-denied {
  color: var(--mw-text-muted);
  font-size: var(--mw-font-size-sm);
  margin-top: 0.5rem;
}

@media (max-width: 767px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
