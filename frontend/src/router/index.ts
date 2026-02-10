import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      component: () => import('@/components/layout/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
        },
        {
          path: 'rooms',
          name: 'rooms',
          component: () => import('@/views/RoomsView.vue'),
        },
        {
          path: 'rooms/discover',
          name: 'discover-rooms',
          component: () => import('@/views/DiscoverRoomsView.vue'),
        },
        {
          path: 'rooms/:id',
          name: 'room-detail',
          component: () => import('@/views/RoomDetailView.vue'),
          props: true,
        },
        {
          path: 'family',
          name: 'family',
          component: () => import('@/views/FamilyView.vue'),
        },
        {
          path: 'messages',
          name: 'messages',
          component: () => import('@/views/MessagesView.vue'),
        },
        {
          path: 'jobs',
          name: 'jobs',
          component: () => import('@/views/JobBoardView.vue'),
        },
        {
          path: 'jobs/create',
          name: 'job-create',
          component: () => import('@/views/JobCreateView.vue'),
        },
        {
          path: 'jobs/:id',
          name: 'job-detail',
          component: () => import('@/views/JobDetailView.vue'),
          props: true,
        },
        {
          path: 'cleaning',
          name: 'cleaning',
          component: () => import('@/views/CleaningView.vue'),
        },
        {
          path: 'cleaning/:id',
          name: 'cleaning-slot',
          component: () => import('@/views/CleaningSlotView.vue'),
          props: true,
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
        },
        // Admin routes
        {
          path: 'admin',
          meta: { requiresAdmin: true },
          children: [
            {
              path: '',
              name: 'admin-dashboard',
              component: () => import('@/views/admin/AdminDashboard.vue'),
            },
            {
              path: 'users',
              name: 'admin-users',
              component: () => import('@/views/admin/AdminUsers.vue'),
            },
            {
              path: 'rooms',
              name: 'admin-rooms',
              component: () => import('@/views/admin/AdminRooms.vue'),
            },
            {
              path: 'sections',
              name: 'admin-sections',
              component: () => import('@/views/admin/AdminSections.vue'),
            },
            {
              path: 'modules',
              name: 'admin-modules',
              component: () => import('@/views/admin/AdminModules.vue'),
            },
            {
              path: 'job-report',
              name: 'admin-job-report',
              component: () => import('@/views/admin/AdminJobReport.vue'),
            },
            {
              path: 'cleaning',
              name: 'admin-cleaning',
              component: () => import('@/views/admin/AdminCleaning.vue'),
            },
            {
              path: 'theme',
              name: 'admin-theme',
              component: () => import('@/views/admin/AdminTheme.vue'),
            },
          ],
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // Try to restore session on first load
  if (auth.isAuthenticated && !auth.user) {
    await auth.fetchUser()
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guest && auth.isAuthenticated) {
    return { name: 'dashboard' }
  }

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'dashboard' }
  }
})

export default router
