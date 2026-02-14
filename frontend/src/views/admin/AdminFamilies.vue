<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { familyApi } from '@/api/family.api'
import { billingApi } from '@/api/billing.api'
import { useAdminStore } from '@/stores/admin'
import { useConfirmDialog } from '@/composables/useConfirmDialog'
import type { FamilyInfo } from '@/types/family'
import type { FamilyBillingEntry } from '@/types/billing'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import ToggleSwitch from 'primevue/toggleswitch'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'

const { t } = useI18n()
const toast = useToast()
const adminStore = useAdminStore()
const { visible: confirmVisible, header: confirmHeader, message: confirmMessage, confirm, onConfirm, onCancel } = useConfirmDialog()

const families = ref<FamilyInfo[]>([])
const hoursMap = ref<Map<string, FamilyBillingEntry>>(new Map())
const loading = ref(false)
const searchQuery = ref('')

// Edit dialog
const showEdit = ref(false)
const editFamily = ref<FamilyInfo | null>(null)
const editTab = ref('0')
const editLoading = ref(false)
const editName = ref('')
const editExempt = ref(false)

const jobboardEnabled = computed(() => adminStore.isModuleEnabled('jobboard'))

const filteredFamilies = computed(() => {
  if (!searchQuery.value.trim()) return families.value
  const q = searchQuery.value.toLowerCase()
  return families.value.filter(f => {
    if (f.name.toLowerCase().includes(q)) return true
    return f.members.some(m => m.displayName.toLowerCase().includes(q))
  })
})

function parentCount(family: FamilyInfo): number {
  return family.members.filter(m => m.role === 'PARENT').length
}

function childCount(family: FamilyInfo): number {
  return family.members.filter(m => m.role === 'CHILD').length
}

function getHoursEntry(familyId: string): FamilyBillingEntry | undefined {
  return hoursMap.value.get(familyId)
}

function trafficLightSeverity(light?: string): string {
  if (light === 'GREEN') return 'success'
  if (light === 'YELLOW') return 'warn'
  if (light === 'RED') return 'danger'
  return 'secondary'
}

function trafficLightLabel(light?: string): string {
  if (light === 'GREEN') return t('admin.trafficLight.green')
  if (light === 'YELLOW') return t('admin.trafficLight.yellow')
  if (light === 'RED') return t('admin.trafficLight.red')
  return '-'
}

async function loadData() {
  loading.value = true
  try {
    const [famRes] = await Promise.all([
      familyApi.getAll(),
    ])
    families.value = famRes.data.data

    if (jobboardEnabled.value) {
      try {
        const activePeriodRes = await billingApi.getActivePeriod()
        const activePeriod = activePeriodRes.data.data
        if (activePeriod) {
          const reportRes = await billingApi.getReport(activePeriod.id)
          const report = reportRes.data.data
          const map = new Map<string, FamilyBillingEntry>()
          for (const entry of report.families) {
            map.set(entry.familyId, entry)
          }
          hoursMap.value = map
        }
      } catch {
        // Billing data not available - ignore
      }
    }
  } finally {
    loading.value = false
  }
}

function openEdit(family: FamilyInfo) {
  editFamily.value = family
  editTab.value = '0'
  editName.value = family.name
  editExempt.value = family.hoursExempt
  showEdit.value = true
}

async function saveFamily() {
  if (!editFamily.value) return
  editLoading.value = true
  try {
    let updated = false
    if (editName.value !== editFamily.value.name) {
      await familyApi.updateName(editFamily.value.id, editName.value)
      updated = true
    }
    if (editExempt.value !== editFamily.value.hoursExempt) {
      await familyApi.setHoursExempt(editFamily.value.id, editExempt.value)
      updated = true
    }
    if (updated) {
      toast.add({ severity: 'success', summary: t('admin.familyUpdated'), life: 3000 })
    }
    showEdit.value = false
    await loadData()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  } finally {
    editLoading.value = false
  }
}

async function removeMember(memberId: string, memberName: string) {
  if (!editFamily.value) return
  const ok = await confirm({
    header: t('admin.removeMember'),
    message: t('admin.removeMemberConfirm', { name: memberName }),
  })
  if (!ok) return
  try {
    await familyApi.removeMember(editFamily.value.id, memberId)
    toast.add({ severity: 'success', summary: t('admin.memberRemoved'), life: 3000 })
    // Reload family data
    const res = await familyApi.getAll()
    families.value = res.data.data
    const updatedFamily = families.value.find(f => f.id === editFamily.value!.id)
    if (updatedFamily) {
      editFamily.value = updatedFamily
    } else {
      showEdit.value = false
    }
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  }
}

async function deleteFamily(family: FamilyInfo) {
  const ok = await confirm({
    header: t('admin.deleteFamily'),
    message: t('admin.deleteFamilyConfirm', { name: family.name }),
  })
  if (!ok) return
  try {
    await familyApi.deleteFamily(family.id)
    toast.add({ severity: 'success', summary: t('admin.familyDeleted'), life: 3000 })
    await loadData()
  } catch {
    toast.add({ severity: 'error', summary: t('error.unexpected'), life: 5000 })
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div>
    <PageTitle :title="t('admin.familiesTitle')" :subtitle="t('admin.familiesSubtitle')" />

    <div class="filter-bar">
      <IconField class="filter-search">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model="searchQuery"
          :placeholder="t('admin.searchFamilies')"
          class="w-full"
        />
      </IconField>
    </div>

    <LoadingSpinner v-if="loading && families.length === 0" />

    <DataTable
      v-else
      :value="filteredFamilies"
      :loading="loading"
      :paginator="filteredFamilies.length > 20"
      :rows="20"
      stripedRows
      scrollable
      class="card"
    >
      <template #empty>
        <div class="empty-table">{{ searchQuery ? t('admin.noFamiliesFound') : t('admin.noFamilies') }}</div>
      </template>
      <Column field="name" :header="t('common.name')" sortable />
      <Column :header="t('admin.parents')">
        <template #body="{ data }">
          {{ parentCount(data) }}
        </template>
      </Column>
      <Column :header="t('admin.children')">
        <template #body="{ data }">
          {{ childCount(data) }}
        </template>
      </Column>
      <Column v-if="jobboardEnabled" :header="t('admin.columnHours')">
        <template #body="{ data }">
          <Tag
            v-if="getHoursEntry(data.id)"
            :value="trafficLightLabel(getHoursEntry(data.id)?.trafficLight)"
            :severity="trafficLightSeverity(getHoursEntry(data.id)?.trafficLight) as any"
          />
          <span v-else class="text-muted">-</span>
        </template>
      </Column>
      <Column :header="t('common.status')">
        <template #body="{ data }">
          <Tag v-if="data.hoursExempt" :value="t('admin.hoursExempt')" severity="secondary" />
        </template>
      </Column>
      <Column :header="t('common.actions')" style="width: 120px">
        <template #body="{ data }">
          <Button icon="pi pi-pencil" severity="secondary" text size="small" @click="openEdit(data)" :aria-label="t('common.edit')" />
          <Button icon="pi pi-trash" severity="danger" text size="small" @click="deleteFamily(data)" :aria-label="t('common.delete')" />
        </template>
      </Column>
    </DataTable>

    <!-- Edit Dialog -->
    <Dialog
      v-model:visible="showEdit"
      :header="t('admin.editFamily')"
      modal
      :style="{ width: '600px', maxWidth: '95vw' }"
    >
      <Tabs :value="editTab">
        <TabList>
          <Tab value="0">{{ t('admin.tabInfo') }}</Tab>
          <Tab value="1">{{ t('admin.tabMembers') }}</Tab>
          <Tab v-if="jobboardEnabled" value="2">{{ t('admin.tabHours') }}</Tab>
        </TabList>
        <TabPanels>
          <!-- Info Tab -->
          <TabPanel value="0">
            <form @submit.prevent="saveFamily" class="dialog-form">
              <div class="form-field">
                <label>{{ t('admin.familyName') }}</label>
                <InputText v-model="editName" class="w-full" />
              </div>
              <div class="form-field toggle-field">
                <label>{{ t('admin.hoursExempt') }}</label>
                <ToggleSwitch v-model="editExempt" />
              </div>
              <div class="form-actions">
                <Button :label="t('common.save')" type="submit" :loading="editLoading" />
              </div>
            </form>
          </TabPanel>

          <!-- Members Tab -->
          <TabPanel value="1">
            <div v-if="!editFamily?.members.length" class="empty-tab">{{ t('admin.noFamilyMemberships') }}</div>
            <div v-else class="item-list">
              <div v-for="member in editFamily?.members" :key="member.userId" class="item-row">
                <span class="item-name">{{ member.displayName }}</span>
                <Tag :value="member.role === 'PARENT' ? t('admin.parents') : t('admin.children')" :severity="member.role === 'PARENT' ? 'info' : 'secondary'" />
                <Button icon="pi pi-trash" severity="danger" text size="small" @click="removeMember(member.userId, member.displayName)" :aria-label="t('admin.removeMember')" />
              </div>
            </div>
          </TabPanel>

          <!-- Hours Tab -->
          <TabPanel v-if="jobboardEnabled" value="2">
            <template v-if="editFamily && getHoursEntry(editFamily.id)">
              <div class="hours-grid">
                <div class="hours-item">
                  <span class="hours-label">{{ t('admin.jobHours') }}</span>
                  <span class="hours-value">{{ getHoursEntry(editFamily.id)!.jobHours }}h</span>
                </div>
                <div class="hours-item">
                  <span class="hours-label">{{ t('admin.cleaningHours') }}</span>
                  <span class="hours-value">{{ getHoursEntry(editFamily.id)!.cleaningHours }}h</span>
                </div>
                <div class="hours-item">
                  <span class="hours-label">{{ t('admin.totalHours') }}</span>
                  <span class="hours-value hours-total">{{ getHoursEntry(editFamily.id)!.totalHours }}h</span>
                </div>
                <div class="hours-item">
                  <span class="hours-label">{{ t('admin.targetHours') }}</span>
                  <span class="hours-value">{{ getHoursEntry(editFamily.id)!.targetHours }}h</span>
                </div>
                <div class="hours-item">
                  <span class="hours-label">{{ t('admin.balance') }}</span>
                  <span class="hours-value" :class="{ 'hours-negative': getHoursEntry(editFamily.id)!.balance < 0 }">
                    {{ getHoursEntry(editFamily.id)!.balance >= 0 ? '+' : '' }}{{ getHoursEntry(editFamily.id)!.balance }}h
                  </span>
                </div>
                <div class="hours-item">
                  <span class="hours-label">{{ t('admin.trafficLightCol') }}</span>
                  <Tag
                    :value="trafficLightLabel(getHoursEntry(editFamily.id)!.trafficLight)"
                    :severity="trafficLightSeverity(getHoursEntry(editFamily.id)!.trafficLight) as any"
                  />
                </div>
              </div>
            </template>
            <div v-else class="empty-tab">
              {{ editFamily?.hoursExempt ? t('admin.exemptFamiliesHint') : t('common.noData') }}
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>
    </Dialog>

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
.filter-bar {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.filter-search {
  flex: 1;
  min-width: 200px;
  max-width: 400px;
}

.empty-table {
  padding: 2rem 0;
  text-align: center;
  color: var(--p-text-muted-color);
}

.text-muted {
  color: var(--p-text-muted-color);
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding-top: 0.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-field label {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
}

.toggle-field {
  flex-direction: row;
  align-items: center;
  gap: 0.75rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 0.5rem;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding-top: 0.5rem;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: var(--p-border-radius);
}

.item-row:hover {
  background-color: var(--p-surface-100);
}

.item-name {
  font-weight: 500;
  flex: 1;
  min-width: 0;
}

.empty-tab {
  padding: 2rem 0;
  text-align: center;
  color: var(--p-text-muted-color);
}

.hours-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  padding: 0.5rem 0;
}

.hours-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.hours-label {
  font-size: var(--mw-font-size-sm);
  color: var(--p-text-muted-color);
}

.hours-value {
  font-size: var(--mw-font-size-md);
  font-weight: 600;
}

.hours-total {
  color: var(--mw-primary);
}

.hours-negative {
  color: var(--p-red-500);
}
</style>
