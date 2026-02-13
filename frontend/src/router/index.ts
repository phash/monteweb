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
          meta: { breadcrumbLabel: 'rooms.title' },
        },
        {
          path: 'family',
          name: 'family',
          component: () => import('@/views/FamilyView.vue'),
        },
        {
          path: 'messages/:conversationId?',
          name: 'messages',
          component: () => import('@/views/MessagesView.vue'),
          props: true,
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
          meta: { breadcrumbLabel: 'jobboard.title' },
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
          path: 'calendar',
          name: 'calendar',
          component: () => import('@/views/CalendarView.vue'),
        },
        {
          path: 'calendar/create',
          name: 'calendar-create',
          component: () => import('@/views/EventCreateView.vue'),
        },
        {
          path: 'calendar/events/:id',
          name: 'event-detail',
          component: () => import('@/views/EventDetailView.vue'),
          props: true,
          meta: { breadcrumbLabel: 'calendar.title' },
        },
        {
          path: 'calendar/events/:id/edit',
          name: 'event-edit',
          component: () => import('@/views/EventCreateView.vue'),
          props: true,
        },
        {
          path: 'forms',
          name: 'forms',
          component: () => import('@/views/FormsView.vue'),
        },
        {
          path: 'forms/create',
          name: 'form-create',
          component: () => import('@/views/FormCreateView.vue'),
        },
        {
          path: 'forms/:id',
          name: 'form-detail',
          component: () => import('@/views/FormDetailView.vue'),
          props: true,
        },
        {
          path: 'forms/:id/edit',
          name: 'form-edit',
          component: () => import('@/views/FormCreateView.vue'),
          props: true,
        },
        {
          path: 'forms/:id/results',
          name: 'form-results',
          component: () => import('@/views/FormResultsView.vue'),
          props: true,
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
        },
        {
          path: 'section-admin',
          name: 'section-admin',
          component: () => import('@/views/admin/SectionAdminView.vue'),
          meta: { requiresSectionAdmin: true },
        },
        // Admin routes
        {
          path: 'admin',
          meta: { requiresAdmin: true, breadcrumbLabel: 'nav.admin' },
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
              meta: { breadcrumbLabel: 'admin.users' },
            },
            {
              path: 'rooms',
              name: 'admin-rooms',
              component: () => import('@/views/admin/AdminRooms.vue'),
              meta: { breadcrumbLabel: 'admin.rooms' },
            },
            {
              path: 'sections',
              name: 'admin-sections',
              component: () => import('@/views/admin/AdminSections.vue'),
              meta: { breadcrumbLabel: 'admin.sections' },
            },
            {
              path: 'modules',
              name: 'admin-modules',
              component: () => import('@/views/admin/AdminModules.vue'),
              meta: { breadcrumbLabel: 'admin.modules' },
            },
            {
              path: 'job-report',
              name: 'admin-job-report',
              component: () => import('@/views/admin/AdminJobReport.vue'),
              meta: { breadcrumbLabel: 'admin.jobReport' },
            },
            {
              path: 'cleaning',
              name: 'admin-cleaning',
              component: () => import('@/views/admin/AdminCleaning.vue'),
              meta: { breadcrumbLabel: 'cleaning.title', allowPutzOrga: true },
            },
            {
              path: 'theme',
              name: 'admin-theme',
              component: () => import('@/views/admin/AdminTheme.vue'),
              meta: { breadcrumbLabel: 'admin.themeTitle' },
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
    if (to.meta.allowPutzOrga && auth.isPutzOrga) {
      // PutzOrga users may access the cleaning admin page
    } else {
      return { name: 'dashboard' }
    }
  }

  if (to.meta.requiresSectionAdmin && !auth.isSectionAdmin && !auth.isAdmin) {
    return { name: 'dashboard' }
  }
})

export default router
