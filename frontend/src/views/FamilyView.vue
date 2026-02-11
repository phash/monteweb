<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useFamilyStore } from '@/stores/family'
import PageTitle from '@/components/common/PageTitle.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import FamilyHoursWidget from '@/components/family/FamilyHoursWidget.vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import { familyApi } from '@/api/family.api'

const { t } = useI18n()
const family = useFamilyStore()

const showCreateDialog = ref(false)
const showJoinDialog = ref(false)
const familyName = ref('')
const inviteCode = ref('')
const generatedCode = ref('')

onMounted(() => {
  family.fetchFamilies()
})

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
</script>

<template>
  <div>
    <PageTitle :title="t('family.title')" />

    <LoadingSpinner v-if="family.loading" />

    <template v-else-if="family.hasFamily">
      <div v-for="fam in family.families" :key="fam.id" class="family-card card">
        <FamilyHoursWidget :familyId="fam.id" />
        <div class="family-header">
          <h2>{{ fam.name }}</h2>
          <Button
            icon="pi pi-link"
            :label="t('family.generateCode')"
            severity="secondary"
            size="small"
            @click="generateCode(fam.id)"
          />
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
    <Dialog v-model:visible="showCreateDialog" :header="t('family.create')" modal style="width: 400px">
      <div class="form-field">
        <label>{{ t('family.name') }}</label>
        <InputText v-model="familyName" class="w-full" />
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showCreateDialog = false" />
        <Button :label="t('common.create')" @click="createFamily" />
      </template>
    </Dialog>

    <!-- Join Dialog -->
    <Dialog v-model:visible="showJoinDialog" :header="t('family.join')" modal style="width: 400px">
      <div class="form-field">
        <label>{{ t('family.inviteCode') }}</label>
        <InputText v-model="inviteCode" class="w-full" />
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showJoinDialog = false" />
        <Button :label="t('family.join')" @click="joinFamily" />
      </template>
    </Dialog>
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
}

.family-header h2 {
  font-size: var(--mw-font-size-xl);
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

.w-full {
  width: 100%;
}
</style>
