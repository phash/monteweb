<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { useI18n } from 'vue-i18n'
import client from '@/api/client'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Divider from 'primevue/divider'
import Message from 'primevue/message'
import Checkbox from 'primevue/checkbox'
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const adminStore = useAdminStore()

const isLogin = ref(true)
const error = ref('')
const pendingApproval = ref(false)
const oidcEnabled = ref(false)
const oidcAuthUri = ref('')

const acceptedTerms = ref(false)

// 2FA state
const show2faInput = ref(false)
const show2faSetupRequired = ref(false)
const tempToken = ref('')
const twoFactorCode = ref('')

const form = ref({
  email: '',
  password: '',
  firstName: '',
  lastName: '',
  phone: '',
})

onMounted(async () => {
  if (!adminStore.config) {
    adminStore.fetchConfig()
  }
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
  if (!isLogin.value && !acceptedTerms.value) {
    error.value = t('auth.termsRequired')
    return
  }
  try {
    if (isLogin.value) {
      const challenge = await auth.login({ email: form.value.email, password: form.value.password })
      if (challenge) {
        tempToken.value = challenge.tempToken
        if (challenge.type === '2fa_verify') {
          show2faInput.value = true
        } else if (challenge.type === '2fa_setup_required') {
          show2faSetupRequired.value = true
        }
        return
      }
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

async function submit2fa() {
  error.value = ''
  try {
    await auth.verify2fa(tempToken.value, twoFactorCode.value)
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: any) {
    error.value = e?.response?.data?.message || t('twoFactor.invalidCode')
  }
}

function back2fa() {
  show2faInput.value = false
  show2faSetupRequired.value = false
  tempToken.value = ''
  twoFactorCode.value = ''
  error.value = ''
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

      <!-- 2FA Verification Step -->
      <template v-if="show2faInput">
        <p class="login-subtitle">{{ t('twoFactor.codeRequired') }}</p>

        <Message v-if="error" severity="error" :closable="false" class="login-error">
          {{ error }}
        </Message>

        <form @submit.prevent="submit2fa" class="login-form">
          <div class="form-field">
            <label for="twoFactorCode">{{ t('twoFactor.enterCode') }}</label>
            <InputText
              id="twoFactorCode"
              v-model="twoFactorCode"
              required
              class="w-full twofa-input"
              maxlength="8"
              autocomplete="one-time-code"
              placeholder="123456"
            />
            <small class="form-hint">{{ t('twoFactor.recoveryCodesInfo') }}</small>
          </div>

          <Button
            type="submit"
            :label="t('twoFactor.verify')"
            :loading="auth.loading"
            class="w-full"
          />
        </form>

        <div class="login-toggle">
          <a href="#" @click.prevent="back2fa">{{ t('common.back') }}</a>
        </div>
      </template>

      <!-- 2FA Setup Required (MANDATORY mode) -->
      <template v-else-if="show2faSetupRequired">
        <p class="login-subtitle">{{ t('twoFactor.setupRequired') }}</p>

        <Message severity="warn" :closable="false" class="login-error">
          {{ t('twoFactor.setupRequiredDesc') }}
        </Message>

        <p class="setup-info">{{ t('twoFactor.setupRequiredDesc') }}</p>

        <!-- For mandatory setup, we issue a temp token. The user must log in normally,
             then set up 2FA from their profile. We let them through with the temp token
             so they can access the profile page. -->
        <form @submit.prevent="submit2fa" class="login-form">
          <div class="form-field">
            <label for="twoFactorCode">{{ t('twoFactor.enterCode') }}</label>
            <InputText
              id="twoFactorCode"
              v-model="twoFactorCode"
              class="w-full twofa-input"
              maxlength="8"
              autocomplete="one-time-code"
              placeholder="123456"
            />
          </div>

          <Button
            type="submit"
            :label="t('twoFactor.verify')"
            :loading="auth.loading"
            class="w-full"
          />
        </form>

        <div class="login-toggle">
          <a href="#" @click.prevent="back2fa">{{ t('common.back') }}</a>
        </div>
      </template>

      <!-- Normal Login / Register -->
      <template v-else>
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
              inputId="password"
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
            <div class="terms-checkbox">
              <Checkbox v-model="acceptedTerms" :binary="true" inputId="acceptTerms" />
              <label for="acceptTerms" class="terms-label">
                {{ t('auth.acceptTermsLabel') }}
                <router-link to="/terms" target="_blank" class="terms-link">{{ t('auth.termsLink') }}</router-link>
              </label>
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

        <div class="login-lang">
          <LanguageSwitcher />
        </div>
      </template>
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

.form-hint {
  font-size: var(--mw-font-size-xs, 0.75rem);
  color: var(--mw-text-muted);
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

.terms-checkbox {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
}

.terms-label {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  line-height: 1.4;
  cursor: pointer;
}

.terms-link {
  color: var(--mw-primary);
  font-weight: 600;
  text-decoration: none;
}

.terms-link:hover {
  text-decoration: underline;
}

.login-lang {
  display: flex;
  justify-content: center;
  margin-top: 1rem;
}

.twofa-input {
  font-size: 1.5rem;
  text-align: center;
  letter-spacing: 0.5rem;
}

.setup-info {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  margin-bottom: 1rem;
  text-align: center;
}
</style>
