<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import client from '@/api/client'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Divider from 'primevue/divider'
import Message from 'primevue/message'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const isLogin = ref(true)
const error = ref('')
const pendingApproval = ref(false)
const oidcEnabled = ref(false)
const oidcAuthUri = ref('')

const form = ref({
  email: '',
  password: '',
  firstName: '',
  lastName: '',
  phone: '',
})

onMounted(async () => {
  try {
    const res = await client.get('/auth/oidc/config')
    if (res.data?.data?.enabled) {
      oidcEnabled.value = true
      oidcAuthUri.value = res.data.data.authorizationUri
    }
  } catch {
    // OIDC not available — ignore
  }
})

async function submit() {
  error.value = ''
  pendingApproval.value = false
  try {
    if (isLogin.value) {
      await auth.login({ email: form.value.email, password: form.value.password })
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } else {
      await auth.register({
        email: form.value.email,
        password: form.value.password,
        firstName: form.value.firstName,
        lastName: form.value.lastName,
        phone: form.value.phone || undefined,
      })
      pendingApproval.value = true
    }
  } catch (e: any) {
    const msg = e?.response?.data?.message || ''
    if (msg === 'PENDING_APPROVAL') {
      error.value = t('auth.pendingApproval')
    } else {
      error.value = msg || (isLogin.value ? t('auth.loginError') : t('auth.registerError'))
    }
  }
}

function toggleMode() {
  isLogin.value = !isLogin.value
  error.value = ''
}

function loginWithSso() {
  window.location.href = oidcAuthUri.value
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-title">MonteWeb</h1>
      <p class="login-subtitle">{{ isLogin ? t('auth.login') : t('auth.register') }}</p>

      <Message v-if="pendingApproval" severity="success" :closable="false" class="login-error">
        {{ t('auth.pendingApprovalSuccess') }}
      </Message>

      <Message v-if="error" severity="error" :closable="false" class="login-error">
        {{ error }}
      </Message>

      <form v-if="!pendingApproval" @submit.prevent="submit" class="login-form">
        <template v-if="!isLogin">
          <div class="form-field">
            <label for="firstName" class="required">{{ t('auth.firstName') }}</label>
            <InputText id="firstName" v-model="form.firstName" required class="w-full" />
          </div>
          <div class="form-field">
            <label for="lastName" class="required">{{ t('auth.lastName') }}</label>
            <InputText id="lastName" v-model="form.lastName" required class="w-full" />
          </div>
        </template>

        <div class="form-field">
          <label for="email" class="required">{{ t('auth.email') }}</label>
          <InputText id="email" v-model="form.email" type="email" required class="w-full" />
        </div>

        <div class="form-field">
          <label for="password" class="required">{{ t('auth.password') }}</label>
          <Password
            id="password"
            v-model="form.password"
            :feedback="!isLogin"
            toggleMask
            required
            class="w-full"
            inputClass="w-full"
          />
        </div>

        <template v-if="!isLogin">
          <div class="form-field">
            <label for="phone">{{ t('auth.phone') }}</label>
            <InputText id="phone" v-model="form.phone" class="w-full" />
          </div>
        </template>

        <Button
          type="submit"
          :label="isLogin ? t('auth.login') : t('auth.register')"
          :loading="auth.loading"
          class="w-full"
        />
      </form>

      <template v-if="oidcEnabled && isLogin">
        <Divider align="center">
          <span class="divider-text">{{ t('auth.or') }}</span>
        </Divider>
        <Button
          :label="t('auth.loginSso')"
          icon="pi pi-shield"
          severity="secondary"
          outlined
          class="w-full"
          @click="loginWithSso"
        />
      </template>

      <div v-if="!pendingApproval" class="login-toggle">
        <span v-if="isLogin">{{ t('auth.noAccount') }}</span>
        <span v-else>{{ t('auth.hasAccount') }}</span>
        <a href="#" @click.prevent="toggleMode">
          {{ isLogin ? t('auth.register') : t('auth.login') }}
        </a>
      </div>
      <div v-if="pendingApproval" class="login-toggle">
        <a href="#" @click.prevent="pendingApproval = false; isLogin = true">
          {{ t('auth.backToLogin') }}
        </a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--mw-bg);
  padding: 1rem;
}

.login-card {
  background: var(--mw-bg-card);
  border-radius: var(--mw-border-radius-lg);
  box-shadow: var(--mw-shadow-lg);
  padding: 2.5rem;
  width: 100%;
  max-width: 420px;
}

.login-title {
  font-size: var(--mw-font-size-3xl);
  font-weight: 700;
  color: var(--mw-primary);
  text-align: center;
  margin-bottom: 0.25rem;
}

.login-subtitle {
  text-align: center;
  color: var(--mw-text-secondary);
  margin-bottom: 1.5rem;
}

.login-error {
  margin-bottom: 1rem;
}

.login-form {
  display: flex;
  flex-direction: column;
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

.login-toggle {
  text-align: center;
  margin-top: 1.5rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
}

.login-toggle a {
  margin-left: 0.25rem;
  font-weight: 600;
}
</style>
