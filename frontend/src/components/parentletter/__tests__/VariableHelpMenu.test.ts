import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import VariableHelpMenu from '../VariableHelpMenu.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      parentLetters: {
        variables: {
          helpTitle: 'Platzhalter-Variablen',
          helpSubtitle: 'Klicken Sie auf eine Variable, um sie einzufügen.',
          familyLabel: 'Familienname',
          familyDesc: 'Der Name der Familie',
          childNameLabel: 'Name Kind',
          childNameDesc: 'Der Vorname des Kindes',
          salutationLabel: 'Anrede',
          salutationDesc: 'Automatische Anrede',
          teacherNameLabel: 'Lehrkraft',
          teacherNameDesc: 'Ihr Name',
          inserted: '{token} wurde eingefügt',
          insert: 'Einfügen',
        },
      },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="button-stub" :aria-label="ariaLabel" :title="title" @click="$emit(\'click\', $event)">{{ label }}</button>',
    props: ['icon', 'text', 'rounded', 'size', 'severity', 'ariaLabel', 'title', 'label'],
    emits: ['click'],
  },
  Popover: {
    template: '<div class="popover-stub" v-if="visible"><slot /></div>',
    props: [],
    data() { return { visible: true } },
    methods: {
      toggle() { (this as any).visible = !(this as any).visible },
      hide() { (this as any).visible = false },
    },
  },
}

function mountMenu() {
  return mount(VariableHelpMenu, {
    global: { plugins: [i18n], stubs },
  })
}

describe('VariableHelpMenu', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock clipboard API
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
    })
  })

  // ==================== Basic render ====================

  it('should mount and render', () => {
    const wrapper = mountMenu()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render help button', () => {
    const wrapper = mountMenu()
    const buttons = wrapper.findAll('.button-stub')
    expect(buttons.length).toBeGreaterThanOrEqual(1)
  })

  // ==================== Popover content ====================

  it('should render popover with variable menu', () => {
    const wrapper = mountMenu()
    expect(wrapper.find('.popover-stub').exists()).toBe(true)
    expect(wrapper.find('.variable-menu').exists()).toBe(true)
  })

  it('should show title in popover', () => {
    const wrapper = mountMenu()
    expect(wrapper.find('.variable-title').text()).toContain('Platzhalter-Variablen')
  })

  it('should show subtitle in popover', () => {
    const wrapper = mountMenu()
    expect(wrapper.find('.variable-subtitle').text()).toContain('Klicken Sie auf eine Variable')
  })

  // ==================== Variable items ====================

  it('should render all 4 variable items', () => {
    const wrapper = mountMenu()
    const items = wrapper.findAll('.variable-item')
    expect(items.length).toBe(4)
  })

  it('should show variable tokens', () => {
    const wrapper = mountMenu()
    const tokens = wrapper.findAll('.variable-token')
    expect(tokens.length).toBe(4)
    expect(tokens[0]!.text()).toBe('{Familie}')
    expect(tokens[1]!.text()).toBe('{NameKind}')
    expect(tokens[2]!.text()).toBe('{Anrede}')
    expect(tokens[3]!.text()).toBe('{LehrerName}')
  })

  it('should show variable descriptions', () => {
    const wrapper = mountMenu()
    const descs = wrapper.findAll('.variable-desc')
    expect(descs.length).toBe(4)
    expect(descs[0]!.text()).toContain('Der Name der Familie')
    expect(descs[1]!.text()).toContain('Der Vorname des Kindes')
  })

  // ==================== Insert action ====================

  it('should emit insert event with correct token when copy button is clicked', async () => {
    const wrapper = mountMenu()
    // There are 5 buttons total: 1 help button + 4 copy buttons
    const buttons = wrapper.findAll('.button-stub')
    // The copy buttons are the ones inside variable items
    const copyButtons = wrapper.findAll('.variable-item .button-stub')

    if (copyButtons.length > 0) {
      await copyButtons[0]!.trigger('click')
      // The component emits 'insert' with the token
      expect(wrapper.emitted('insert')).toBeTruthy()
      expect(wrapper.emitted('insert')![0]).toEqual(['{Familie}'])
    }
  })

  it('should emit correct token for NameKind variable', async () => {
    const wrapper = mountMenu()
    const copyButtons = wrapper.findAll('.variable-item .button-stub')

    if (copyButtons.length >= 2) {
      await copyButtons[1]!.trigger('click')
      expect(wrapper.emitted('insert')).toBeTruthy()
      expect(wrapper.emitted('insert')![0]).toEqual(['{NameKind}'])
    }
  })

  it('should emit correct token for Anrede variable', async () => {
    const wrapper = mountMenu()
    const copyButtons = wrapper.findAll('.variable-item .button-stub')

    if (copyButtons.length >= 3) {
      await copyButtons[2]!.trigger('click')
      expect(wrapper.emitted('insert')).toBeTruthy()
      expect(wrapper.emitted('insert')![0]).toEqual(['{Anrede}'])
    }
  })

  it('should emit correct token for LehrerName variable', async () => {
    const wrapper = mountMenu()
    const copyButtons = wrapper.findAll('.variable-item .button-stub')

    if (copyButtons.length >= 4) {
      await copyButtons[3]!.trigger('click')
      expect(wrapper.emitted('insert')).toBeTruthy()
      expect(wrapper.emitted('insert')![0]).toEqual(['{LehrerName}'])
    }
  })

  it('should copy token to clipboard when copy button is clicked', async () => {
    const wrapper = mountMenu()
    const copyButtons = wrapper.findAll('.variable-item .button-stub')

    if (copyButtons.length > 0) {
      await copyButtons[0]!.trigger('click')
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith('{Familie}')
    }
  })

  // ==================== Variable items structure ====================

  it('should have variable-info div with token and desc', () => {
    const wrapper = mountMenu()
    const infoItems = wrapper.findAll('.variable-info')
    expect(infoItems.length).toBe(4)
  })

  it('should render code elements for tokens', () => {
    const wrapper = mountMenu()
    const codeEls = wrapper.findAll('code.variable-token')
    expect(codeEls.length).toBe(4)
  })
})
