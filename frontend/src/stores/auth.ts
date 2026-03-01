import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth.api'
import { usersApi } from '@/api/users.api'
import { useAdminStore } from '@/stores/admin'
import { useImageToken } from '@/composables/useImageToken'
import { useWebSocket } from '@/composables/useWebSocket'
import { resetTermsCache } from '@/utils/termsCache'
import type { UserInfo, UserRole, LoginRequest, RegisterRequest } from '@/types/user'

export interface TwoFactorChallenge {
  tempToken: string
  type: '2fa_verify' | '2fa_setup_required'
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null)
  const accessToken = ref<string | null>(sessionStorage.getItem('accessToken'))
  const loading = ref(false)

  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'SUPERADMIN')
  const isTeacher = computed(() =>
    user.value?.role === 'TEACHER' || user.value?.role === 'SUPERADMIN' || user.value?.role === 'SECTION_ADMIN'
  )
  const isStudent = computed(() => user.value?.role === 'STUDENT')
  const isPutzOrga = computed(() =>
    user.value?.specialRoles?.some((r: string) => r === 'PUTZORGA' || r.startsWith('PUTZORGA:')) ?? false
  )
  const isSectionAdmin = computed(() => user.value?.role === 'SECTION_ADMIN')
  const canHaveFamily = computed(() =>
    user.value?.role === 'PARENT' || user.value?.role === 'STUDENT' || user.value?.role === 'SUPERADMIN'
  )
  const assignedRoles = computed(() => user.value?.assignedRoles ?? [])
  const canSwitchRole = computed(() => assignedRoles.value.length > 1)

  /**
   * Login. Returns null on success (tokens stored), or a TwoFactorChallenge if 2FA is needed.
   */
  async function login(data: LoginRequest): Promise<TwoFactorChallenge | null> {
    loading.value = true
    try {
      const res = await authApi.login(data)
      const responseData = res.data.data

      // Check if 2FA verification is required
      if (responseData.requires2fa && responseData.tempToken) {
        return { tempToken: responseData.tempToken, type: '2fa_verify' }
      }

      // Check if 2FA setup is required (MANDATORY mode, grace passed)
      if (responseData.requires2faSetup && responseData.tempToken) {
        return { tempToken: responseData.tempToken, type: '2fa_setup_required' }
      }

      // Normal login — store tokens
      const { accessToken: token, refreshToken } = responseData
      setTokens(token, refreshToken)
      await fetchUser()
      const admin = useAdminStore()
      await admin.fetchConfig()
      const { fetchImageToken } = useImageToken()
      await fetchImageToken()
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Complete 2FA verification with temp token and code.
   */
  async function verify2fa(tempToken: string, code: string) {
    loading.value = true
    try {
      const res = await authApi.verify2fa(tempToken, code)
      const { accessToken: token, refreshToken } = res.data.data
      setTokens(token, refreshToken)
      await fetchUser()
      const admin = useAdminStore()
      await admin.fetchConfig()
      const { fetchImageToken } = useImageToken()
      await fetchImageToken()
    } finally {
      loading.value = false
    }
  }

  async function register(data: RegisterRequest): Promise<'PENDING_APPROVAL'> {
    loading.value = true
    try {
      await authApi.register(data)
      return 'PENDING_APPROVAL'
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    const refreshToken = sessionStorage.getItem('refreshToken')
    try {
      await authApi.logout(refreshToken)
    } finally {
      const { disconnect } = useWebSocket()
      disconnect()
      clearTokens()
      user.value = null
      const { clearImageToken } = useImageToken()
      clearImageToken()
      resetTermsCache()

      // Clear service worker caches to prevent data leakage on shared devices
      if ('caches' in window) {
        caches.keys().then(names => names.forEach(name => caches.delete(name)))
      }
    }
  }

  async function switchRole(role: UserRole) {
    loading.value = true
    try {
      const res = await usersApi.switchActiveRole(role)
      const { accessToken: token, refreshToken } = res.data.data
      setTokens(token, refreshToken)
      await fetchUser()
    } finally {
      loading.value = false
    }
  }

  async function fetchUser() {
    if (!accessToken.value) return
    try {
      const res = await usersApi.getMe()
      user.value = res.data.data
    } catch {
      clearTokens()
      user.value = null
    }
  }

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    sessionStorage.setItem('accessToken', access)
    sessionStorage.setItem('refreshToken', refresh)
  }

  function clearTokens() {
    accessToken.value = null
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
  }

  return {
    user,
    loading,
    isAuthenticated,
    isAdmin,
    isTeacher,
    isStudent,
    isPutzOrga,
    isSectionAdmin,
    canHaveFamily,
    assignedRoles,
    canSwitchRole,
    login,
    verify2fa,
    register,
    logout,
    fetchUser,
    switchRole,
  }
})
