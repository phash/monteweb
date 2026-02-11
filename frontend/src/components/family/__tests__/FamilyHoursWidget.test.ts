import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FamilyHoursWidget from '../FamilyHoursWidget.vue'
import { useAdminStore } from '@/stores/admin'

vi.mock('@/api/jobboard.api', () => ({
  jobboardApi: {
    getFamilyHours: vi.fn().mockResolvedValue({
      data: {
        data: {
          familyId: 'family-1',
          familyName: 'Mueller',
          targetHours: 20,
          completedHours: 8,
          pendingHours: 2,
          cleaningHours: 3,
          totalHours: 13,
          remainingHours: 7,
          trafficLight: 'YELLOW',
          targetCleaningHours: 5,
          remainingCleaningHours: 2,
          cleaningTrafficLight: 'GREEN',
        },
      },
    }),
    listJobs: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true } } }),
    getCategories: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getMyAssignments: vi.fn().mockResolvedValue({ data: { data: [] } }),
  },
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({
      data: {
        data: {
          modules: { jobboard: true },
          schoolName: 'Montessori Schule',
        },
      },
    }),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      family: {
        hours: 'Stundenkonto',
        hoursUnit: 'Stunden',
        jobHours: 'Jobstunden',
        cleaningHours: 'Putzstunden',
        cleaningProgress: 'Putz-Fortschritt',
        pending: 'Ausstehend',
        remaining: 'Verbleibend',
      },
      admin: {
        trafficLight: {
          green: 'Im Plan',
          yellow: 'Achtung',
          red: 'Kritisch',
        },
      },
    },
  },
})

const stubs = {
  Tag: {
    template: '<span class="tag-stub" :class="severity">{{ value }}</span>',
    props: ['value', 'severity', 'size'],
  },
}

function mountWithJobboard(props = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)

  // Pre-configure admin store with jobboard enabled
  const adminStore = useAdminStore()
  adminStore.config = {
    modules: { jobboard: true },
    schoolName: 'Montessori Schule',
  } as any

  return mount(FamilyHoursWidget, {
    props: { familyId: 'family-1', ...props },
    global: {
      plugins: [i18n, pinia],
      stubs,
    },
  })
}

describe('FamilyHoursWidget', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should not render when jobboard module is disabled', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const adminStore = useAdminStore()
    adminStore.config = { modules: { jobboard: false }, schoolName: 'Test' } as any

    const wrapper = mount(FamilyHoursWidget, {
      props: { familyId: 'family-1' },
      global: {
        plugins: [i18n, pinia],
        stubs,
      },
    })
    await flushPromises()

    expect(wrapper.find('.hours-widget').exists()).toBe(false)
  })

  it('should render the hours widget when jobboard is enabled and data is loaded', async () => {
    const wrapper = mountWithJobboard()
    await flushPromises()

    expect(wrapper.find('.hours-widget').exists()).toBe(true)
  })

  it('should display the title "Stundenkonto"', async () => {
    const wrapper = mountWithJobboard()
    await flushPromises()

    expect(wrapper.text()).toContain('Stundenkonto')
  })

  it('should show the traffic light tag with correct severity', async () => {
    const wrapper = mountWithJobboard()
    await flushPromises()

    const tags = wrapper.findAll('.tag-stub')
    const mainTag = tags.find(t => t.text() === 'Achtung')
    expect(mainTag).toBeDefined()
    expect(mainTag!.classes()).toContain('warn')
  })

  it('should display progress bar with correct percentage', async () => {
    const wrapper = mountWithJobboard()
    await flushPromises()

    // totalHours=13, targetHours=20, so 65%
    const fill = wrapper.find('.progress-fill')
    expect(fill.exists()).toBe(true)
    expect(fill.attributes('style')).toContain('65%')
  })

  it('should display hours breakdown (job, cleaning, pending, remaining)', async () => {
    const wrapper = mountWithJobboard()
    await flushPromises()

    expect(wrapper.text()).toContain('8h')   // completedHours
    expect(wrapper.text()).toContain('3h')   // cleaningHours
    expect(wrapper.text()).toContain('2h')   // pendingHours
    expect(wrapper.text()).toContain('7h')   // remainingHours
  })

  it('should display cleaning sub-progress when targetCleaningHours > 0', async () => {
    const wrapper = mountWithJobboard()
    await flushPromises()

    expect(wrapper.find('.cleaning-progress').exists()).toBe(true)
    expect(wrapper.text()).toContain('Putz-Fortschritt')
  })

  it('should not display cleaning sub-progress when targetCleaningHours is 0', async () => {
    const { jobboardApi } = await import('@/api/jobboard.api')
    vi.mocked(jobboardApi.getFamilyHours).mockResolvedValueOnce({
      data: {
        data: {
          familyId: 'family-1', familyName: 'Mueller', targetHours: 20,
          completedHours: 10, pendingHours: 0, cleaningHours: 0, totalHours: 10,
          remainingHours: 10, trafficLight: 'GREEN',
          targetCleaningHours: 0, remainingCleaningHours: 0, cleaningTrafficLight: 'GREEN',
        },
      },
    } as any)

    const pinia = createPinia()
    setActivePinia(pinia)
    const adminStore = useAdminStore()
    adminStore.config = { modules: { jobboard: true }, schoolName: 'Test' } as any

    const wrapper = mount(FamilyHoursWidget, {
      props: { familyId: 'family-1' },
      global: {
        plugins: [i18n, pinia],
        stubs,
      },
    })
    await flushPromises()

    expect(wrapper.find('.cleaning-progress').exists()).toBe(false)
  })

  it('should show total/target hours in progress label', async () => {
    const wrapper = mountWithJobboard()
    await flushPromises()

    expect(wrapper.text()).toContain('13/20')
    expect(wrapper.text()).toContain('Stunden')
  })

  it('should handle 100% capped progress', async () => {
    const { jobboardApi } = await import('@/api/jobboard.api')
    vi.mocked(jobboardApi.getFamilyHours).mockResolvedValueOnce({
      data: {
        data: {
          familyId: 'family-1', familyName: 'Mueller', targetHours: 20,
          completedHours: 25, pendingHours: 0, cleaningHours: 5, totalHours: 30,
          remainingHours: 0, trafficLight: 'GREEN',
          targetCleaningHours: 5, remainingCleaningHours: 0, cleaningTrafficLight: 'GREEN',
        },
      },
    } as any)

    const pinia = createPinia()
    setActivePinia(pinia)
    const adminStore = useAdminStore()
    adminStore.config = { modules: { jobboard: true }, schoolName: 'Test' } as any

    const wrapper = mount(FamilyHoursWidget, {
      props: { familyId: 'family-1' },
      global: {
        plugins: [i18n, pinia],
        stubs,
      },
    })
    await flushPromises()

    // Progress should be capped at 100%
    const fill = wrapper.find('.progress-fill')
    expect(fill.attributes('style')).toContain('100%')
  })
})
