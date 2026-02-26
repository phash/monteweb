<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { roomsApi } from '@/api/rooms.api'
import type { RoomMember, RoomInfo } from '@/types/room'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Select from 'primevue/select'
import Message from 'primevue/message'

const { t } = useI18n()
const toast = useToast()

const props = defineProps<{
  visible: boolean
  roomId: string
  members: RoomMember[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'migrated'): void
}>()

const step = ref(1)
const selectedMembers = ref<string[]>([])
const action = ref<'move' | 'leave'>('move')
const targetRoomId = ref<string | null>(null)
const klasseRooms = ref<RoomInfo[]>([])
const loading = ref(false)
const loadingRooms = ref(false)

const studentMembers = computed(() =>
  props.members.filter(m => m.role === 'MEMBER')
)

const selectedMemberNames = computed(() =>
  studentMembers.value
    .filter(m => selectedMembers.value.includes(m.userId))
    .map(m => m.displayName)
)

const targetRoomName = computed(() =>
  klasseRooms.value.find(r => r.id === targetRoomId.value)?.name ?? ''
)

const canProceed = computed(() => {
  if (step.value === 1) return selectedMembers.value.length > 0
  if (step.value === 2) return true
  if (step.value === 3 && action.value === 'move') return targetRoomId.value !== null
  return true
})

watch(() => props.visible, (val) => {
  if (val) {
    step.value = 1
    selectedMembers.value = []
    action.value = 'move'
    targetRoomId.value = null
  }
})

async function loadKlasseRooms() {
  loadingRooms.value = true
  try {
    const res = await roomsApi.getAll({ type: 'KLASSE' })
    klasseRooms.value = (res.data.data.content as RoomInfo[]).filter(r => r.id !== props.roomId && !r.archived)
  } catch {
    klasseRooms.value = []
  } finally {
    loadingRooms.value = false
  }
}

function next() {
  if (step.value === 2 && action.value === 'move') {
    loadKlasseRooms()
  }
  step.value++
}

function back() {
  step.value--
}

async function executeMigration() {
  loading.value = true
  try {
    const res = await roomsApi.migrateMembers(
      props.roomId,
      selectedMembers.value,
      action.value === 'move' ? targetRoomId.value : null,
      action.value === 'leave',
    )
    const count = res.data.data.migrated
    toast.add({
      severity: 'success',
      summary: action.value === 'leave'
        ? t('rooms.migration.leaveSuccess', { count })
        : t('rooms.migration.moveSuccess', { count }),
      life: 5000,
    })
    emit('update:visible', false)
    emit('migrated')
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="emit('update:visible', $event)"
    :header="t('rooms.migration.title')"
    modal
    :style="{ width: '550px', maxWidth: '95vw' }"
  >
    <!-- Step 1: Select children -->
    <div v-if="step === 1">
      <p class="mb-3">{{ t('rooms.migration.selectChildren') }}</p>
      <div v-if="studentMembers.length === 0" class="text-muted">
        {{ t('rooms.migration.noStudents') }}
      </div>
      <div v-else class="member-select-list">
        <label
          v-for="member in studentMembers"
          :key="member.userId"
          class="member-checkbox"
        >
          <Checkbox
            v-model="selectedMembers"
            :value="member.userId"
          />
          <span>{{ member.displayName }}</span>
        </label>
      </div>
    </div>

    <!-- Step 2: Choose action -->
    <div v-else-if="step === 2">
      <p class="mb-3">{{ t('rooms.migration.chooseAction') }}</p>
      <div class="action-options">
        <label class="action-option" :class="{ active: action === 'move' }">
          <input type="radio" v-model="action" value="move" />
          <div>
            <strong>{{ t('rooms.migration.moveToClass') }}</strong>
            <p class="text-sm text-muted">{{ t('rooms.migration.moveDesc') }}</p>
          </div>
        </label>
        <label class="action-option" :class="{ active: action === 'leave' }">
          <input type="radio" v-model="action" value="leave" />
          <div>
            <strong>{{ t('rooms.migration.leaveSchool') }}</strong>
            <p class="text-sm text-muted">{{ t('rooms.migration.leaveDesc') }}</p>
          </div>
        </label>
      </div>
    </div>

    <!-- Step 3: Target room or leave confirmation -->
    <div v-else-if="step === 3">
      <div v-if="action === 'move'">
        <p class="mb-3">{{ t('rooms.migration.selectTarget') }}</p>
        <Select
          v-model="targetRoomId"
          :options="klasseRooms"
          optionLabel="name"
          optionValue="id"
          :placeholder="t('rooms.migration.selectTargetPlaceholder')"
          :loading="loadingRooms"
          class="w-full"
        />
      </div>
      <div v-else>
        <Message severity="warn" :closable="false">
          {{ t('rooms.migration.leaveWarning') }}
        </Message>
        <p class="mt-3">{{ t('rooms.migration.leaveConfirm', { count: selectedMembers.length }) }}</p>
        <ul class="member-names-list">
          <li v-for="name in selectedMemberNames" :key="name">{{ name }}</li>
        </ul>
      </div>
    </div>

    <!-- Step 4: Summary -->
    <div v-else-if="step === 4">
      <h4>{{ t('rooms.migration.summary') }}</h4>
      <div class="summary-section">
        <p><strong>{{ t('rooms.migration.selectedCount', { count: selectedMembers.length }) }}</strong></p>
        <ul class="member-names-list">
          <li v-for="name in selectedMemberNames" :key="name">{{ name }}</li>
        </ul>
      </div>
      <div class="summary-section">
        <p v-if="action === 'move'">
          <strong>{{ t('rooms.migration.actionLabel') }}:</strong>
          {{ t('rooms.migration.moveToTarget', { name: targetRoomName }) }}
        </p>
        <p v-else>
          <strong>{{ t('rooms.migration.actionLabel') }}:</strong>
          {{ t('rooms.migration.leaveSchool') }}
        </p>
      </div>
      <Message v-if="action === 'leave'" severity="error" :closable="false">
        {{ t('rooms.migration.leaveIrreversible') }}
      </Message>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <Button
          v-if="step > 1"
          :label="t('common.back')"
          severity="secondary"
          @click="back"
        />
        <div class="footer-right">
          <Button
            :label="t('common.cancel')"
            severity="secondary"
            text
            @click="emit('update:visible', false)"
          />
          <Button
            v-if="step < 4"
            :label="t('common.next')"
            :disabled="!canProceed"
            @click="next"
          />
          <Button
            v-else
            :label="t('rooms.migration.execute')"
            :severity="action === 'leave' ? 'danger' : 'primary'"
            :loading="loading"
            @click="executeMigration"
          />
        </div>
      </div>
    </template>
  </Dialog>
</template>

<style scoped>
.member-select-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 300px;
  overflow-y: auto;
}

.member-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.5rem;
  border-radius: var(--mw-border-radius-sm);
  cursor: pointer;
}

.member-checkbox:hover {
  background: var(--mw-bg);
}

.action-options {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.action-option {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 2px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-sm);
  cursor: pointer;
  transition: border-color 0.15s;
}

.action-option.active {
  border-color: var(--mw-primary, #3b82f6);
}

.action-option input[type="radio"] {
  margin-top: 0.25rem;
}

.member-names-list {
  list-style: disc;
  margin-left: 1.25rem;
  margin-top: 0.5rem;
}

.member-names-list li {
  font-size: var(--mw-font-size-sm);
  padding: 0.125rem 0;
}

.summary-section {
  margin-bottom: 1rem;
}

.text-sm {
  font-size: var(--mw-font-size-sm);
}

.text-muted {
  color: var(--mw-text-muted);
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.footer-right {
  display: flex;
  gap: 0.5rem;
  margin-left: auto;
}
</style>
