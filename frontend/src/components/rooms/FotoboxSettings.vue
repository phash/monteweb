<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useFotoboxStore } from '@/stores/fotobox'
import { useToast } from 'primevue/usetoast'
import Dialog from 'primevue/dialog'
import ToggleSwitch from 'primevue/toggleswitch'
import Select from 'primevue/select'
import InputNumber from 'primevue/inputnumber'
import Button from 'primevue/button'

const props = defineProps<{ roomId: string; visible: boolean }>()
const emit = defineEmits<{ (e: 'update:visible', value: boolean): void }>()

const { t } = useI18n()
const fotobox = useFotoboxStore()
const toast = useToast()

const enabled = ref(false)
const defaultPermission = ref('VIEW_ONLY')
const maxFileSizeMb = ref(10)
const maxImagesPerThread = ref<number | null>(null)
const saving = ref(false)

const permissionOptions = [
  { label: t('fotobox.permissionViewOnly'), value: 'VIEW_ONLY' },
  { label: t('fotobox.permissionPostImages'), value: 'POST_IMAGES' },
  { label: t('fotobox.permissionCreateThreads'), value: 'CREATE_THREADS' },
]

onMounted(async () => {
  await fotobox.fetchSettings(props.roomId)
  if (fotobox.settings) {
    enabled.value = fotobox.settings.enabled
    defaultPermission.value = fotobox.settings.defaultPermission
    maxFileSizeMb.value = fotobox.settings.maxFileSizeMb
    maxImagesPerThread.value = fotobox.settings.maxImagesPerThread
  }
})

async function save() {
  saving.value = true
  try {
    await fotobox.updateSettings(props.roomId, {
      enabled: enabled.value,
      defaultPermission: defaultPermission.value as any,
      maxFileSizeMb: maxFileSizeMb.value,
      maxImagesPerThread: maxImagesPerThread.value,
    })
    toast.add({ severity: 'success', summary: t('common.save'), life: 3000 })
    emit('update:visible', false)
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="emit('update:visible', $event)"
    :header="t('fotobox.settings')"
    modal
    :style="{ width: '500px', maxWidth: '90vw' }"
  >
    <div class="settings-form">
      <div class="field">
        <label>{{ t('fotobox.enabled') }}</label>
        <ToggleSwitch v-model="enabled" />
      </div>
      <div class="field">
        <label>{{ t('fotobox.permission') }}</label>
        <Select
          v-model="defaultPermission"
          :options="permissionOptions"
          optionLabel="label"
          optionValue="value"
          class="w-full"
        />
      </div>
      <div class="field">
        <label>{{ t('fotobox.maxFileSize') }}</label>
        <InputNumber v-model="maxFileSizeMb" :min="1" :max="50" suffix=" MB" class="w-full" />
      </div>
      <div class="field">
        <label>{{ t('fotobox.maxImagesPerThread') }}</label>
        <InputNumber
          v-model="maxImagesPerThread"
          :min="0"
          :max="1000"
          :placeholder="t('fotobox.unlimited')"
          class="w-full"
        />
        <small class="text-muted">{{ t('fotobox.unlimited') }}: 0</small>
      </div>
    </div>
    <template #footer>
      <Button :label="t('common.cancel')" severity="secondary" text @click="emit('update:visible', false)" />
      <Button :label="t('common.save')" :loading="saving" @click="save" />
    </template>
  </Dialog>
</template>

<style scoped>
.settings-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.field label {
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
}

.text-muted {
  color: var(--mw-text-muted);
}
</style>
