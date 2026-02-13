import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth.api'
import { usersApi } from '@/api/users.api'
import { useAdminStore } from '@/stores/admin'
import type { UserInfo, LoginRequest, RegisterRequest } from '@/types/user'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null)
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))
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

  async function login(data: LoginRequest) {
    loading.value = true
    try {
      const res = await authApi.login(data)
      const { accessToken: token, refreshToken } = res.data.data
      setTokens(token, refreshToken)
      await fetchUser()
      const admin = useAdminStore()
      await admin.fetchConfig()
    } finally {
      loading.value = false
    }
  }

  async function register(data: RegisterRequest) {
    loading.value = true
    try {
      const res = await authApi.register(data)
      const { accessToken: token, refreshToken } = res.data.data
      setTokens(token, refreshToken)
      await fetchUser()
      const admin = useAdminStore()
      await admin.fetchConfig()
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    const refreshToken = localStorage.getItem('refreshToken')
    try {
      await authApi.logout(refreshToken)
    } finally {
      clearTokens()
      user.value = null
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
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
  }

  function clearTokens() {
    accessToken.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
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
    login,
    register,
    logout,
    fetchUser,
  }
})
