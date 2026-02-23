<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMessagingStore } from '@/stores/messaging'
import { useToast } from 'primevue/usetoast'
import { usersApi } from '@/api/users.api'
import { authApi } from '@/api/auth.api'
import { usePushNotifications } from '@/composables/usePushNotifications'
import { useConfirmDialog } from '@/composables/useConfirmDialog'
import { useLocaleDate } from '@/composables/useLocaleDate'
import PageTitle from '@/components/common/PageTitle.vue'
import AvatarUpload from '@/components/common/AvatarUpload.vue'
import InputText from 'primevue/inputtext'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import Password from 'primevue/password'
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue'
import type { UserRole } from '@/types/user'

const { t } = useI18n()
const auth = useAuthStore()
const messagingStore = useMessagingStore()
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

// 2FA state
const twoFactorEnabled = ref(false)
const twoFactorLoading = ref(false)
const showSetupDialog = ref(false)
const showDisableDialog = ref(false)
const showRecoveryCodes = ref(false)
const setupSecret = ref('')
const setupQrUri = ref('')
const setupCode = ref('')
const setupError = ref('')
const recoveryCodes = ref<string[]>([])
const disablePassword = ref('')
const disableError = ref('')

onMounted(async () => {
  if (auth.user) {
    form.value.firstName = auth.user.firstName
    form.value.lastName = auth.user.lastName
    form.value.phone = auth.user.phone ?? ''
  }
  await checkSubscription()
  pushEnabled.value = pushSubscribed.value
  await messagingStore.fetchConversations()

  // Check 2FA status
  try {
    const res = await authApi.get2faStatus()
    twoFactorEnabled.value = res.data.data.enabled
  } catch {
    // Ignore — 2FA status not critical
  }
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

const mutedConversations = computed(() =>
  messagingStore.conversations.filter(c => c.muted)
)

function getConversationName(conv: { title: string | null; participants: { displayName: string }[]; isGroup: boolean }) {
  if (conv.title) return conv.title
  if (!conv.isGroup && conv.participants.length > 0) {
    const other = conv.participants.find(p => p.displayName !== `${auth.user?.firstName} ${auth.user?.lastName}`)
    return other?.displayName ?? conv.participants[0]?.displayName ?? ''
  }
  return conv.participants.map(p => p.displayName).join(', ')
}

async function unmuteChatFromProfile(conversationId: string) {
  await messagingStore.unmuteConversation(conversationId)
}

async function togglePush() {
  if (pushEnabled.value) {
    const ok = await pushSubscribe()
    if (!ok) pushEnabled.value = false
  } else {
    await pushUnsubscribe()
  }
}

// --- 2FA Functions ---

async function startSetup2fa() {
  twoFactorLoading.value = true
  setupError.value = ''
  setupCode.value = ''
  try {
    const res = await authApi.setup2fa()
    setupSecret.value = res.data.data.secret
    setupQrUri.value = res.data.data.qrUri
    showSetupDialog.value = true
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e?.response?.data?.message || 'Error', life: 5000 })
  } finally {
    twoFactorLoading.value = false
  }
}

async function confirmSetup2fa() {
  setupError.value = ''
  twoFactorLoading.value = true
  try {
    const res = await authApi.confirm2fa(setupCode.value)
    recoveryCodes.value = res.data.data.recoveryCodes
    showSetupDialog.value = false
    showRecoveryCodes.value = true
    twoFactorEnabled.value = true
    toast.add({ severity: 'success', summary: t('twoFactor.setupSuccess'), life: 3000 })
  } catch (e: any) {
    setupError.value = e?.response?.data?.message || t('twoFactor.invalidCode')
  } finally {
    twoFactorLoading.value = false
  }
}

function closeRecoveryCodes() {
  showRecoveryCodes.value = false
  recoveryCodes.value = []
}

function openDisable2fa() {
  disablePassword.value = ''
  disableError.value = ''
  showDisableDialog.value = true
}

async function confirmDisable2fa() {
  disableError.value = ''
  twoFactorLoading.value = true
  try {
    await authApi.disable2fa(disablePassword.value)
    twoFactorEnabled.value = false
    showDisableDialog.value = false
    toast.add({ severity: 'success', summary: t('twoFactor.disableSuccess'), life: 3000 })
  } catch (e: any) {
    disableError.value = e?.response?.data?.message || 'Error'
  } finally {
    twoFactorLoading.value = false
  }
}

const qrCodeUrl = computed(() => {
  if (!setupQrUri.value) return ''
  return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(setupQrUri.value)}`
})

// DSGVO / Privacy
const { visible: confirmVisible, header: confirmHeader, message: confirmMessage, confirm, onConfirm, onCancel } = useConfirmDialog()
const { formatDate } = useLocaleDate()

const deletionStatus = ref<{
  deletionRequested: boolean
  deletionRequestedAt: string | null
  scheduledDeletionAt: string | null
}>({ deletionRequested: false, deletionRequestedAt: null, scheduledDeletionAt: null })
const exportingData = ref(false)

onMounted(async () => {
  try {
    const res = await usersApi.getDeletionStatus()
    deletionStatus.value = res.data.data
  } catch {
    // Ignore — deletion status not critical
  }
})

async function exportMyData() {
  exportingData.value = true
  try {
    const res = await usersApi.exportMyData()
    const blob = new Blob([JSON.stringify(res.data.data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `monteweb-data-export-${new Date().toISOString().slice(0, 10)}.json`
    link.click()
    URL.revokeObjectURL(url)
    toast.add({ severity: 'success', summary: t('privacy.exportSuccess'), life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: t('privacy.exportError'), life: 5000 })
  } finally {
    exportingData.value = false
  }
}

async function requestAccountDeletion() {
  const confirmed = await confirm({
    header: t('privacy.deleteConfirmTitle'),
    message: t('privacy.deleteConfirmMessage'),
  })
  if (!confirmed) return

  try {
    await usersApi.requestDeletion()
    const res = await usersApi.getDeletionStatus()
    deletionStatus.value = res.data.data
    toast.add({ severity: 'warn', summary: t('privacy.deletionRequested'), life: 5000 })
  } catch {
    toast.add({ severity: 'error', summary: t('privacy.deletionError'), life: 5000 })
  }
}

async function cancelAccountDeletion() {
  try {
    await usersApi.cancelDeletion()
    deletionStatus.value = { deletionRequested: false, deletionRequestedAt: null, scheduledDeletionAt: null }
    toast.add({ severity: 'success', summary: t('privacy.deletionCancelled'), life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: t('privacy.cancelError'), life: 5000 })
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

    <!-- Two-Factor Authentication -->
    <div class="card profile-card twofa-card">
      <h3>{{ t('twoFactor.title') }}</h3>
      <div class="twofa-status">
        <div>
          <Tag
            v-if="twoFactorEnabled"
            :value="t('twoFactor.enabled')"
            severity="success"
          />
          <span v-else class="twofa-disabled-text">{{ t('twoFactor.modes.DISABLED') }}</span>
        </div>
        <Button
          v-if="!twoFactorEnabled"
          :label="t('twoFactor.enable')"
          icon="pi pi-shield"
          size="small"
          :loading="twoFactorLoading"
          @click="startSetup2fa"
        />
        <Button
          v-else
          :label="t('twoFactor.disable')"
          icon="pi pi-times"
          size="small"
          severity="danger"
          :loading="twoFactorLoading"
          @click="openDisable2fa"
        />
      </div>
    </div>

    <!-- 2FA Setup Dialog -->
    <Dialog v-model:visible="showSetupDialog" :header="t('twoFactor.enable')" modal :style="{ width: '450px' }" :closable="true">
      <div class="twofa-setup">
        <p class="twofa-step">1. {{ t('twoFactor.scanQr') }}</p>
        <div class="twofa-qr">
          <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="QR Code" width="200" height="200" />
        </div>

        <p class="twofa-step">{{ t('twoFactor.manualEntry') }}:</p>
        <div class="twofa-secret">
          <code>{{ setupSecret }}</code>
        </div>

        <p class="twofa-step">2. {{ t('twoFactor.enterCode') }}</p>
        <Message v-if="setupError" severity="error" :closable="false" class="mb-2">
          {{ setupError }}
        </Message>
        <InputText
          v-model="setupCode"
          class="w-full twofa-code-input"
          maxlength="6"
          placeholder="123456"
          autocomplete="one-time-code"
          @keyup.enter="confirmSetup2fa"
        />
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showSetupDialog = false" />
        <Button :label="t('twoFactor.verify')" :loading="twoFactorLoading" @click="confirmSetup2fa" :disabled="setupCode.length < 6" />
      </template>
    </Dialog>

    <!-- Recovery Codes Dialog -->
    <Dialog v-model:visible="showRecoveryCodes" :header="t('twoFactor.recoveryCodes')" modal :style="{ width: '450px' }" :closable="false">
      <Message severity="warn" :closable="false" class="mb-3">
        {{ t('twoFactor.recoveryCodesInfo') }}
      </Message>
      <div class="recovery-codes-grid">
        <code v-for="code in recoveryCodes" :key="code" class="recovery-code">{{ code }}</code>
      </div>
      <template #footer>
        <Button :label="t('twoFactor.recoveryCodesSaved')" @click="closeRecoveryCodes" />
      </template>
    </Dialog>

    <!-- Disable 2FA Dialog -->
    <Dialog v-model:visible="showDisableDialog" :header="t('twoFactor.disable')" modal :style="{ width: '400px' }">
      <p class="mb-3">{{ t('twoFactor.disableConfirm') }}</p>
      <Message v-if="disableError" severity="error" :closable="false" class="mb-2">
        {{ disableError }}
      </Message>
      <Password
        v-model="disablePassword"
        :feedback="false"
        toggleMask
        class="w-full"
        inputClass="w-full"
        :placeholder="t('twoFactor.enterPassword')"
        @keyup.enter="confirmDisable2fa"
      />
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showDisableDialog = false" />
        <Button :label="t('twoFactor.disable')" severity="danger" :loading="twoFactorLoading" @click="confirmDisable2fa" />
      </template>
    </Dialog>

    <!-- Language -->
    <div class="card profile-card language-card">
      <h3>{{ t('profile.language') }}</h3>
      <LanguageSwitcher />
    </div>

    <!-- Muted Chats -->
    <div class="card profile-card muted-chats-card">
      <h3>{{ t('profile.mutedChats') }}</h3>
      <div v-if="mutedConversations.length === 0" class="empty-muted">
        {{ t('profile.noMutedChats') }}
      </div>
      <div v-else class="muted-list">
        <div v-for="conv in mutedConversations" :key="conv.id" class="muted-item">
          <span class="muted-name">{{ getConversationName(conv) }}</span>
          <Button
            icon="pi pi-volume-up"
            :label="t('profile.unmute')"
            text
            size="small"
            severity="secondary"
            @click="unmuteChatFromProfile(conv.id)"
          />
        </div>
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

    <!-- DSGVO / Privacy -->
    <div class="card profile-card privacy-card">
      <h3>{{ t('privacy.title') }}</h3>

      <!-- Data Export -->
      <div class="privacy-row">
        <div>
          <p class="privacy-label">{{ t('privacy.exportData') }}</p>
          <p class="privacy-desc">{{ t('privacy.exportDataDesc') }}</p>
        </div>
        <Button
          icon="pi pi-download"
          :label="t('privacy.exportData')"
          severity="secondary"
          size="small"
          :loading="exportingData"
          @click="exportMyData"
        />
      </div>

      <!-- Deletion Status -->
      <div v-if="deletionStatus.deletionRequested" class="privacy-row deletion-pending">
        <div>
          <p class="privacy-label deletion-warning">
            <i class="pi pi-exclamation-triangle" />
            {{ t('privacy.deletionScheduled', { date: deletionStatus.scheduledDeletionAt ? formatDate(deletionStatus.scheduledDeletionAt) : '—' }) }}
          </p>
        </div>
        <Button
          icon="pi pi-times"
          :label="t('privacy.cancelDeletion')"
          severity="warn"
          size="small"
          @click="cancelAccountDeletion"
        />
      </div>

      <!-- Delete Account -->
      <div v-else class="privacy-row">
        <div>
          <p class="privacy-label">{{ t('privacy.deleteAccount') }}</p>
          <p class="privacy-desc">{{ t('privacy.deleteAccountDesc') }}</p>
        </div>
        <Button
          icon="pi pi-trash"
          :label="t('privacy.deleteAccount')"
          severity="danger"
          size="small"
          @click="requestAccountDeletion"
        />
      </div>

      <!-- Privacy Policy Link -->
      <div class="privacy-row privacy-link-row">
        <router-link to="/privacy" class="privacy-link">
          <i class="pi pi-shield" />
          {{ t('privacy.viewPrivacyPolicy') }}
        </router-link>
      </div>
    </div>

    <!-- Confirm Dialog -->
    <Dialog v-model:visible="confirmVisible" :header="confirmHeader" modal :style="{ width: '400px' }">
      <p>{{ confirmMessage }}</p>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="onCancel" />
        <Button :label="t('common.confirm')" severity="danger" @click="onConfirm" />
      </template>
    </Dialog>
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

.twofa-card {
  margin-top: 1rem;
}

.twofa-card h3 {
  margin: 0 0 0.75rem 0;
  font-size: var(--mw-font-size-md);
}

.twofa-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.twofa-disabled-text {
  color: var(--mw-text-muted);
  font-size: var(--mw-font-size-sm);
}

.twofa-setup {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.twofa-step {
  font-weight: 500;
  margin: 0;
}

.twofa-qr {
  display: flex;
  justify-content: center;
  padding: 0.5rem;
}

.twofa-secret {
  background: var(--p-surface-100);
  padding: 0.5rem 0.75rem;
  border-radius: var(--p-border-radius);
  text-align: center;
  word-break: break-all;
  font-size: var(--mw-font-size-sm);
}

.twofa-code-input {
  font-size: 1.25rem;
  text-align: center;
  letter-spacing: 0.3rem;
}

.recovery-codes-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.recovery-code {
  background: var(--p-surface-100);
  padding: 0.5rem;
  border-radius: var(--p-border-radius);
  text-align: center;
  font-size: 0.9rem;
  font-family: monospace;
}

.language-card {
  margin-top: 1rem;
}

.language-card h3 {
  margin: 0 0 0.75rem 0;
  font-size: var(--mw-font-size-md);
}

.muted-chats-card {
  margin-top: 1rem;
}

.muted-chats-card h3 {
  margin: 0 0 0.75rem 0;
  font-size: var(--mw-font-size-md);
}

.empty-muted {
  color: var(--mw-text-muted);
  font-size: var(--mw-font-size-sm);
}

.muted-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.muted-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--p-surface-200);
}

.muted-item:last-child {
  border-bottom: none;
}

.muted-name {
  font-size: var(--mw-font-size-sm);
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

.privacy-card {
  margin-top: 1rem;
}

.privacy-card h3 {
  margin: 0 0 0.75rem 0;
  font-size: var(--mw-font-size-md);
}

.privacy-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--p-surface-200);
}

.privacy-row:last-child {
  border-bottom: none;
}

.privacy-label {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
  margin: 0;
}

.privacy-desc {
  font-size: var(--mw-font-size-xs, 0.75rem);
  color: var(--mw-text-muted);
  margin: 0.125rem 0 0;
}

.deletion-pending {
  background: color-mix(in srgb, var(--p-orange-500) 8%, transparent);
  padding: 0.75rem;
  border-radius: var(--p-border-radius);
  border-bottom: none;
}

.deletion-warning {
  color: var(--p-orange-600);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.privacy-link-row {
  justify-content: flex-start;
}

.privacy-link {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--mw-primary);
  font-size: var(--mw-font-size-sm);
  text-decoration: none;
}

.privacy-link:hover {
  text-decoration: underline;
}

@media (max-width: 767px) {
  .form-row {
    grid-template-columns: 1fr;
  }

  .privacy-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
