<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users.api'
import { usePushNotifications } from '@/composables/usePushNotifications'
import PageTitle from '@/components/common/PageTitle.vue'
import AvatarUpload from '@/components/common/AvatarUpload.vue'
import InputText from 'primevue/inputtext'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'
import Message from 'primevue/message'

const { t } = useI18n()
const auth = useAuthStore()
const { isSupported: pushSupported, isSubscribed: pushSubscribed, permission: pushPermission,
        checkSubscription, subscribe: pushSubscribe, unsubscribe: pushUnsubscribe } = usePushNotifications()

const pushEnabled = ref(false)

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

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
