<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useFamilyStore } from '@/stores/family'
import type { FamilyInvitationInfo } from '@/types/family'
import PageTitle from '@/components/common/PageTitle.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import FamilyHoursWidget from '@/components/family/FamilyHoursWidget.vue'
import InviteMemberDialog from '@/components/family/InviteMemberDialog.vue'
import AvatarUpload from '@/components/common/AvatarUpload.vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import { familyApi } from '@/api/family.api'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const family = useFamilyStore()
const toast = useToast()

const showCreateDialog = ref(false)
const showJoinDialog = ref(false)
const showInviteDialog = ref(false)
const inviteFamilyId = ref('')
const familyName = ref('')
const inviteCode = ref('')
const generatedCode = ref('')

// Invitations
const myInvitations = ref<FamilyInvitationInfo[]>([])
const sentInvitations = ref<FamilyInvitationInfo[]>([])

onMounted(async () => {
  await family.fetchFamilies()
  await loadInvitations()
})

async function loadInvitations() {
  try {
    const res = await familyApi.getMyInvitations()
    myInvitations.value = res.data.data
  } catch {
    myInvitations.value = []
  }

  // Load sent invitations for each family
  sentInvitations.value = []
  for (const fam of family.families) {
    try {
      const res = await familyApi.getFamilyInvitations(fam.id)
      sentInvitations.value.push(...res.data.data)
    } catch {
      // ignore
    }
  }
}

async function createFamily() {
  if (!familyName.value.trim()) return
  await family.createFamily(familyName.value.trim())
  familyName.value = ''
  showCreateDialog.value = false
}

async function joinFamily() {
  if (!inviteCode.value.trim()) return
  await family.joinFamily(inviteCode.value.trim())
  inviteCode.value = ''
  showJoinDialog.value = false
}

async function generateCode(familyId: string) {
  const res = await familyApi.generateInviteCode(familyId)
  generatedCode.value = res.data.data.inviteCode
}

function copyCode() {
  window.navigator.clipboard.writeText(generatedCode.value)
}

function openInviteDialog(familyId: string) {
  inviteFamilyId.value = familyId
  showInviteDialog.value = true
}

async function onInviteSent() {
  toast.add({ severity: 'success', summary: t('family.invitationSent'), life: 3000 })
  await loadInvitations()
}

async function acceptInvitation(id: string) {
  try {
    await familyApi.acceptInvitation(id)
    toast.add({ severity: 'success', summary: t('family.invitationAccepted'), life: 3000 })
    await family.fetchFamilies()
    await loadInvitations()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function declineInvitation(id: string) {
  try {
    await familyApi.declineInvitation(id)
    toast.add({ severity: 'success', summary: t('family.invitationDeclined'), life: 3000 })
    await loadInvitations()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function handleFamilyAvatarUpload(familyId: string, file: File) {
  await familyApi.uploadAvatar(familyId, file)
  await family.fetchFamilies()
}

async function handleFamilyAvatarRemove(familyId: string) {
  await familyApi.removeAvatar(familyId)
  await family.fetchFamilies()
}
</script>

<template>
  <div>
    <PageTitle :title="t('family.title')" />

    <!-- Pending invitations for current user -->
    <div v-if="myInvitations.length > 0" class="invitations-card card">
      <h3>{{ t('family.pendingInvitations') }}</h3>
      <div v-for="inv in myInvitations" :key="inv.id" class="invitation-item">
        <div class="invitation-info">
          <i class="pi pi-envelope" />
          <div>
            <span>{{ t('family.invitedBy', { name: inv.inviterName }) }}</span>
            <p class="text-sm text-muted">{{ inv.familyName }} &middot; {{ t(`family.roles.${inv.role}`) }}</p>
          </div>
        </div>
        <div class="invitation-actions">
          <Button :label="t('family.acceptInvitation')" icon="pi pi-check" size="small"
                  @click="acceptInvitation(inv.id)" />
          <Button :label="t('family.declineInvitation')" icon="pi pi-times" size="small"
                  severity="secondary" text @click="declineInvitation(inv.id)" />
        </div>
      </div>
    </div>

    <LoadingSpinner v-if="family.loading" />

    <template v-else-if="family.hasFamily">
      <div v-for="fam in family.families" :key="fam.id" class="family-card card">
        <AvatarUpload
          :image-url="fam.avatarUrl"
          size="md"
          icon="pi-users"
          :editable="true"
          @upload="(file: File) => handleFamilyAvatarUpload(fam.id, file)"
          @remove="() => handleFamilyAvatarRemove(fam.id)"
        />
        <FamilyHoursWidget :familyId="fam.id" />
        <div class="family-header">
          <h2>{{ fam.name }}</h2>
          <div class="family-header-actions">
            <Button
              icon="pi pi-user-plus"
              :label="t('family.inviteMember')"
              severity="secondary"
              size="small"
              @click="openInviteDialog(fam.id)"
            />
            <Button
              icon="pi pi-link"
              :label="t('family.generateCode')"
              severity="secondary"
              size="small"
              @click="generateCode(fam.id)"
            />
          </div>
        </div>

        <div v-if="generatedCode" class="invite-code-display">
          <span class="code">{{ generatedCode }}</span>
          <Button
            icon="pi pi-copy"
            severity="secondary"
            text
            size="small"
            @click="copyCode"
          />
        </div>

        <!-- Sent invitations -->
        <div v-if="sentInvitations.filter(i => i.familyId === fam.id).length > 0" class="sent-invitations">
          <h4>{{ t('family.sentInvitations') }}</h4>
          <div v-for="inv in sentInvitations.filter(i => i.familyId === fam.id)" :key="inv.id" class="sent-item">
            <i class="pi pi-send" />
            <span>{{ inv.inviteeName }}</span>
            <Tag :value="t(`family.roles.${inv.role}`)" severity="secondary" size="small" />
            <Tag :value="t('family.pending')" severity="warn" size="small" />
          </div>
        </div>

        <h3>{{ t('family.members') }}</h3>
        <div class="members-list">
          <div v-for="member in fam.members" :key="member.userId" class="member-item">
            <i class="pi pi-user" />
            <span>{{ member.displayName }}</span>
            <Tag :value="t(`family.roles.${member.role}`)" :severity="member.role === 'PARENT' ? 'info' : 'secondary'" size="small" />
          </div>
        </div>
      </div>
    </template>

    <EmptyState
      v-else
      icon="pi pi-users"
      :message="t('family.noFamily')"
    >
      <div class="empty-actions">
        <Button :label="t('family.create')" @click="showCreateDialog = true" />
        <Button :label="t('family.join')" severity="secondary" @click="showJoinDialog = true" />
      </div>
    </EmptyState>

    <!-- Create Dialog -->
    <Dialog v-model:visible="showCreateDialog" :header="t('family.create')" modal :style="{ width: '400px', maxWidth: '90vw' }">
      <div class="form-field">
        <label for="family-name">{{ t('family.name') }}</label>
        <InputText id="family-name" v-model="familyName" class="w-full" />
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showCreateDialog = false" />
        <Button :label="t('common.create')" @click="createFamily" />
      </template>
    </Dialog>

    <!-- Join Dialog -->
    <Dialog v-model:visible="showJoinDialog" :header="t('family.join')" modal :style="{ width: '400px', maxWidth: '90vw' }">
      <div class="form-field">
        <label for="invite-code">{{ t('family.inviteCode') }}</label>
        <InputText id="invite-code" v-model="inviteCode" class="w-full" />
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showJoinDialog = false" />
        <Button :label="t('family.join')" @click="joinFamily" />
      </template>
    </Dialog>

    <!-- Invite Member Dialog -->
    <InviteMemberDialog
      v-model:visible="showInviteDialog"
      :familyId="inviteFamilyId"
      @invited="onInviteSent"
    />
  </div>
</template>

<style scoped>
.family-card {
  margin-bottom: 1.5rem;
}

.family-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.family-header h2 {
  font-size: var(--mw-font-size-xl);
}

.family-header-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.invite-code-display {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--mw-bg);
  border-radius: var(--mw-border-radius-sm);
  margin-bottom: 1rem;
}

.invite-code-display .code {
  font-family: monospace;
  font-size: var(--mw-font-size-lg);
  font-weight: 600;
  letter-spacing: 0.1em;
}

.invitations-card {
  margin-bottom: 1.5rem;
  border: 2px solid var(--mw-primary, #3b82f6);
}

.invitations-card h3 {
  font-size: var(--mw-font-size-md);
  margin-bottom: 0.75rem;
}

.invitation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--mw-border-light);
  flex-wrap: wrap;
}

.invitation-item:last-child {
  border-bottom: none;
}

.invitation-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.invitation-info i {
  color: var(--mw-primary, #3b82f6);
}

.invitation-actions {
  display: flex;
  gap: 0.375rem;
}

.sent-invitations {
  margin-bottom: 1rem;
  padding: 0.5rem 0;
}

.sent-invitations h4 {
  font-size: var(--mw-font-size-sm);
  font-weight: 600;
  color: var(--mw-text-secondary);
  margin-bottom: 0.375rem;
}

.sent-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0;
  font-size: var(--mw-font-size-sm);
}

.sent-item i {
  color: var(--mw-text-muted);
}

h3 {
  font-size: var(--mw-font-size-md);
  margin-bottom: 0.5rem;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.375rem 0;
}

.member-item i {
  color: var(--mw-text-muted);
}

.empty-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  margin-bottom: 1rem;
}

.form-field label {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
}

.text-sm {
  font-size: var(--mw-font-size-sm);
}

.text-muted {
  color: var(--mw-text-muted);
}
</style>
