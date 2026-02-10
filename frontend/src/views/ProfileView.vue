<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users.api'
import PageTitle from '@/components/common/PageTitle.vue'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Message from 'primevue/message'

const { t } = useI18n()
const auth = useAuthStore()

const form = ref({
  firstName: '',
  lastName: '',
  phone: '',
})
const saved = ref(false)

onMounted(() => {
  if (auth.user) {
    form.value.firstName = auth.user.firstName
    form.value.lastName = auth.user.lastName
    form.value.phone = auth.user.phone ?? ''
  }
})

async function save() {
  await usersApi.updateMe(form.value)
  await auth.fetchUser()
  saved.value = true
  setTimeout(() => { saved.value = false }, 3000)
}
</script>

<template>
  <div>
    <PageTitle :title="t('profile.title')" />

    <div class="card profile-card">
      <Message v-if="saved" severity="success" :closable="false">
        {{ t('profile.saved') }}
      </Message>

      <form @submit.prevent="save" class="profile-form">
        <div class="form-field">
          <label>{{ t('auth.email') }}</label>
          <InputText :model-value="auth.user?.email" disabled class="w-full" />
        </div>

        <div class="form-row">
          <div class="form-field">
            <label>{{ t('auth.firstName') }}</label>
            <InputText v-model="form.firstName" class="w-full" />
          </div>
          <div class="form-field">
            <label>{{ t('auth.lastName') }}</label>
            <InputText v-model="form.lastName" class="w-full" />
          </div>
        </div>

        <div class="form-field">
          <label>{{ t('auth.phone') }}</label>
          <InputText v-model="form.phone" class="w-full" />
        </div>

        <Button type="submit" :label="t('common.save')" />
      </form>
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

.w-full {
  width: 100%;
}

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
