import type { UserRole } from '@/types/user'

export interface RoleHelp {
  actions: string[]
  tips: string[]
}

export interface PageHelp {
  pageTitle: string
  roles: Partial<Record<UserRole, RoleHelp>>
  general?: RoleHelp
}

export const helpContent: Record<string, PageHelp> = {
  dashboard: {
    pageTitle: 'help.pages.dashboard.title',
    roles: {
      PARENT: {
        actions: [
          'help.pages.dashboard.parent.action1',
          'help.pages.dashboard.parent.action2',
          'help.pages.dashboard.parent.action3',
          'help.pages.dashboard.parent.action4',
        ],
        tips: [
          'help.pages.dashboard.parent.tip1',
          'help.pages.dashboard.parent.tip2',
        ],
      },
      STUDENT: {
        actions: [
          'help.pages.dashboard.student.action1',
          'help.pages.dashboard.student.action2',
          'help.pages.dashboard.student.action3',
        ],
        tips: [
          'help.pages.dashboard.student.tip1',
        ],
      },
      TEACHER: {
        actions: [
          'help.pages.dashboard.teacher.action1',
          'help.pages.dashboard.teacher.action2',
          'help.pages.dashboard.teacher.action3',
          'help.pages.dashboard.teacher.action4',
        ],
        tips: [
          'help.pages.dashboard.teacher.tip1',
          'help.pages.dashboard.teacher.tip2',
        ],
      },
      SECTION_ADMIN: {
        actions: [
          'help.pages.dashboard.sectionAdmin.action1',
          'help.pages.dashboard.sectionAdmin.action2',
          'help.pages.dashboard.sectionAdmin.action3',
        ],
        tips: [
          'help.pages.dashboard.sectionAdmin.tip1',
        ],
      },
      SUPERADMIN: {
        actions: [
          'help.pages.dashboard.admin.action1',
          'help.pages.dashboard.admin.action2',
          'help.pages.dashboard.admin.action3',
          'help.pages.dashboard.admin.action4',
        ],
        tips: [
          'help.pages.dashboard.admin.tip1',
        ],
      },
    },
  },

  rooms: {
    pageTitle: 'help.pages.rooms.title',
    roles: {
      PARENT: {
        actions: [
          'help.pages.rooms.parent.action1',
          'help.pages.rooms.parent.action2',
        ],
        tips: [
          'help.pages.rooms.parent.tip1',
        ],
      },
      STUDENT: {
        actions: [
          'help.pages.rooms.student.action1',
          'help.pages.rooms.student.action2',
        ],
        tips: [
          'help.pages.rooms.student.tip1',
        ],
      },
      TEACHER: {
        actions: [
          'help.pages.rooms.teacher.action1',
          'help.pages.rooms.teacher.action2',
          'help.pages.rooms.teacher.action3',
        ],
        tips: [
          'help.pages.rooms.teacher.tip1',
        ],
      },
      SUPERADMIN: {
        actions: [
          'help.pages.rooms.admin.action1',
          'help.pages.rooms.admin.action2',
          'help.pages.rooms.admin.action3',
        ],
        tips: [
          'help.pages.rooms.admin.tip1',
        ],
      },
    },
  },

  'room-detail': {
    pageTitle: 'help.pages.roomDetail.title',
    roles: {
      PARENT: {
        actions: [
          'help.pages.roomDetail.parent.action1',
          'help.pages.roomDetail.parent.action2',
          'help.pages.roomDetail.parent.action3',
        ],
        tips: [
          'help.pages.roomDetail.parent.tip1',
        ],
      },
      STUDENT: {
        actions: [
          'help.pages.roomDetail.student.action1',
          'help.pages.roomDetail.student.action2',
        ],
        tips: [
          'help.pages.roomDetail.student.tip1',
        ],
      },
      TEACHER: {
        actions: [
          'help.pages.roomDetail.teacher.action1',
          'help.pages.roomDetail.teacher.action2',
          'help.pages.roomDetail.teacher.action3',
          'help.pages.roomDetail.teacher.action4',
        ],
        tips: [
          'help.pages.roomDetail.teacher.tip1',
        ],
      },
      SUPERADMIN: {
        actions: [
          'help.pages.roomDetail.admin.action1',
          'help.pages.roomDetail.admin.action2',
          'help.pages.roomDetail.admin.action3',
          'help.pages.roomDetail.admin.action4',
        ],
        tips: [
          'help.pages.roomDetail.admin.tip1',
        ],
      },
    },
  },

  'discover-rooms': {
    pageTitle: 'help.pages.discover.title',
    general: {
      actions: [
        'help.pages.discover.action1',
        'help.pages.discover.action2',
      ],
      tips: [
        'help.pages.discover.tip1',
      ],
    },
    roles: {},
  },

  family: {
    pageTitle: 'help.pages.family.title',
    roles: {
      PARENT: {
        actions: [
          'help.pages.family.parent.action1',
          'help.pages.family.parent.action2',
          'help.pages.family.parent.action3',
          'help.pages.family.parent.action4',
        ],
        tips: [
          'help.pages.family.parent.tip1',
          'help.pages.family.parent.tip2',
        ],
      },
      STUDENT: {
        actions: [
          'help.pages.family.student.action1',
          'help.pages.family.student.action2',
        ],
        tips: [
          'help.pages.family.student.tip1',
        ],
      },
      SUPERADMIN: {
        actions: [
          'help.pages.family.admin.action1',
          'help.pages.family.admin.action2',
        ],
        tips: [
          'help.pages.family.admin.tip1',
        ],
      },
    },
  },

  messages: {
    pageTitle: 'help.pages.messages.title',
    roles: {
      PARENT: {
        actions: [
          'help.pages.messages.parent.action1',
          'help.pages.messages.parent.action2',
          'help.pages.messages.parent.action3',
        ],
        tips: [
          'help.pages.messages.parent.tip1',
        ],
      },
      STUDENT: {
        actions: [
          'help.pages.messages.student.action1',
          'help.pages.messages.student.action2',
        ],
        tips: [
          'help.pages.messages.student.tip1',
        ],
      },
      TEACHER: {
        actions: [
          'help.pages.messages.teacher.action1',
          'help.pages.messages.teacher.action2',
          'help.pages.messages.teacher.action3',
        ],
        tips: [
          'help.pages.messages.teacher.tip1',
        ],
      },
      SUPERADMIN: {
        actions: [
          'help.pages.messages.admin.action1',
          'help.pages.messages.admin.action2',
        ],
        tips: [
          'help.pages.messages.admin.tip1',
        ],
      },
    },
  },

  jobs: {
    pageTitle: 'help.pages.jobs.title',
    roles: {
      PARENT: {
        actions: [
          'help.pages.jobs.parent.action1',
          'help.pages.jobs.parent.action2',
          'help.pages.jobs.parent.action3',
        ],
        tips: [
          'help.pages.jobs.parent.tip1',
          'help.pages.jobs.parent.tip2',
        ],
      },
      TEACHER: {
        actions: [
          'help.pages.jobs.teacher.action1',
          'help.pages.jobs.teacher.action2',
          'help.pages.jobs.teacher.action3',
        ],
        tips: [
          'help.pages.jobs.teacher.tip1',
        ],
      },
      SUPERADMIN: {
        actions: [
          'help.pages.jobs.admin.action1',
          'help.pages.jobs.admin.action2',
          'help.pages.jobs.admin.action3',
        ],
        tips: [
          'help.pages.jobs.admin.tip1',
        ],
      },
    },
  },

  cleaning: {
    pageTitle: 'help.pages.cleaning.title',
    roles: {
      PARENT: {
        actions: [
          'help.pages.cleaning.parent.action1',
          'help.pages.cleaning.parent.action2',
          'help.pages.cleaning.parent.action3',
        ],
        tips: [
          'help.pages.cleaning.parent.tip1',
          'help.pages.cleaning.parent.tip2',
        ],
      },
      TEACHER: {
        actions: [
          'help.pages.cleaning.teacher.action1',
          'help.pages.cleaning.teacher.action2',
        ],
        tips: [
          'help.pages.cleaning.teacher.tip1',
        ],
      },
      SUPERADMIN: {
        actions: [
          'help.pages.cleaning.admin.action1',
          'help.pages.cleaning.admin.action2',
          'help.pages.cleaning.admin.action3',
        ],
        tips: [
          'help.pages.cleaning.admin.tip1',
        ],
      },
    },
  },

  calendar: {
    pageTitle: 'help.pages.calendar.title',
    roles: {
      PARENT: {
        actions: [
          'help.pages.calendar.parent.action1',
          'help.pages.calendar.parent.action2',
        ],
        tips: [
          'help.pages.calendar.parent.tip1',
        ],
      },
      STUDENT: {
        actions: [
          'help.pages.calendar.student.action1',
          'help.pages.calendar.student.action2',
        ],
        tips: [
          'help.pages.calendar.student.tip1',
        ],
      },
      TEACHER: {
        actions: [
          'help.pages.calendar.teacher.action1',
          'help.pages.calendar.teacher.action2',
          'help.pages.calendar.teacher.action3',
        ],
        tips: [
          'help.pages.calendar.teacher.tip1',
        ],
      },
      SUPERADMIN: {
        actions: [
          'help.pages.calendar.admin.action1',
          'help.pages.calendar.admin.action2',
          'help.pages.calendar.admin.action3',
        ],
        tips: [
          'help.pages.calendar.admin.tip1',
        ],
      },
    },
  },

  forms: {
    pageTitle: 'help.pages.forms.title',
    roles: {
      PARENT: {
        actions: [
          'help.pages.forms.parent.action1',
          'help.pages.forms.parent.action2',
        ],
        tips: [
          'help.pages.forms.parent.tip1',
        ],
      },
      STUDENT: {
        actions: [
          'help.pages.forms.student.action1',
        ],
        tips: [
          'help.pages.forms.student.tip1',
        ],
      },
      TEACHER: {
        actions: [
          'help.pages.forms.teacher.action1',
          'help.pages.forms.teacher.action2',
          'help.pages.forms.teacher.action3',
        ],
        tips: [
          'help.pages.forms.teacher.tip1',
        ],
      },
      SUPERADMIN: {
        actions: [
          'help.pages.forms.admin.action1',
          'help.pages.forms.admin.action2',
          'help.pages.forms.admin.action3',
        ],
        tips: [
          'help.pages.forms.admin.tip1',
        ],
      },
    },
  },

  fundgrube: {
    pageTitle: 'help.pages.fundgrube.title',
    general: {
      actions: [
        'help.pages.fundgrube.action1',
        'help.pages.fundgrube.action2',
        'help.pages.fundgrube.action3',
      ],
      tips: [
        'help.pages.fundgrube.tip1',
      ],
    },
    roles: {},
  },

  profile: {
    pageTitle: 'help.pages.profile.title',
    general: {
      actions: [
        'help.pages.profile.action1',
        'help.pages.profile.action2',
        'help.pages.profile.action3',
        'help.pages.profile.action4',
      ],
      tips: [
        'help.pages.profile.tip1',
      ],
    },
    roles: {},
  },

  'admin-dashboard': {
    pageTitle: 'help.pages.adminDashboard.title',
    roles: {
      SUPERADMIN: {
        actions: [
          'help.pages.adminDashboard.action1',
          'help.pages.adminDashboard.action2',
          'help.pages.adminDashboard.action3',
        ],
        tips: [
          'help.pages.adminDashboard.tip1',
        ],
      },
    },
  },

  'admin-users': {
    pageTitle: 'help.pages.adminUsers.title',
    roles: {
      SUPERADMIN: {
        actions: [
          'help.pages.adminUsers.action1',
          'help.pages.adminUsers.action2',
          'help.pages.adminUsers.action3',
          'help.pages.adminUsers.action4',
        ],
        tips: [
          'help.pages.adminUsers.tip1',
        ],
      },
    },
  },

  'admin-rooms': {
    pageTitle: 'help.pages.adminRooms.title',
    roles: {
      SUPERADMIN: {
        actions: [
          'help.pages.adminRooms.action1',
          'help.pages.adminRooms.action2',
          'help.pages.adminRooms.action3',
        ],
        tips: [
          'help.pages.adminRooms.tip1',
        ],
      },
    },
  },

  'admin-sections': {
    pageTitle: 'help.pages.adminSections.title',
    roles: {
      SUPERADMIN: {
        actions: [
          'help.pages.adminSections.action1',
          'help.pages.adminSections.action2',
        ],
        tips: [
          'help.pages.adminSections.tip1',
        ],
      },
    },
  },

  'admin-families': {
    pageTitle: 'help.pages.adminFamilies.title',
    roles: {
      SUPERADMIN: {
        actions: [
          'help.pages.adminFamilies.action1',
          'help.pages.adminFamilies.action2',
          'help.pages.adminFamilies.action3',
        ],
        tips: [
          'help.pages.adminFamilies.tip1',
        ],
      },
    },
  },

  'admin-modules': {
    pageTitle: 'help.pages.adminModules.title',
    roles: {
      SUPERADMIN: {
        actions: [
          'help.pages.adminModules.action1',
          'help.pages.adminModules.action2',
        ],
        tips: [
          'help.pages.adminModules.tip1',
        ],
      },
    },
  },

  'admin-cleaning': {
    pageTitle: 'help.pages.adminCleaning.title',
    roles: {
      SUPERADMIN: {
        actions: [
          'help.pages.adminCleaning.action1',
          'help.pages.adminCleaning.action2',
          'help.pages.adminCleaning.action3',
          'help.pages.adminCleaning.action4',
        ],
        tips: [
          'help.pages.adminCleaning.tip1',
        ],
      },
    },
  },

  'admin-theme': {
    pageTitle: 'help.pages.adminTheme.title',
    roles: {
      SUPERADMIN: {
        actions: [
          'help.pages.adminTheme.action1',
          'help.pages.adminTheme.action2',
          'help.pages.adminTheme.action3',
        ],
        tips: [
          'help.pages.adminTheme.tip1',
        ],
      },
    },
  },

  'admin-billing': {
    pageTitle: 'help.pages.adminBilling.title',
    roles: {
      SUPERADMIN: {
        actions: [
          'help.pages.adminBilling.action1',
          'help.pages.adminBilling.action2',
          'help.pages.adminBilling.action3',
        ],
        tips: [
          'help.pages.adminBilling.tip1',
        ],
      },
    },
  },

  'admin-settings': {
    pageTitle: 'help.pages.adminSettings.title',
    roles: {
      SUPERADMIN: {
        actions: [
          'help.pages.adminSettings.action1',
          'help.pages.adminSettings.action2',
          'help.pages.adminSettings.action3',
        ],
        tips: [
          'help.pages.adminSettings.tip1',
        ],
      },
    },
  },

  'section-admin': {
    pageTitle: 'help.pages.sectionAdmin.title',
    roles: {
      SECTION_ADMIN: {
        actions: [
          'help.pages.sectionAdmin.action1',
          'help.pages.sectionAdmin.action2',
          'help.pages.sectionAdmin.action3',
        ],
        tips: [
          'help.pages.sectionAdmin.tip1',
        ],
      },
    },
  },
}

export interface HandbookSection {
  title: string
  content: string[]
}

export interface HandbookChapter {
  title: string
  sections: HandbookSection[]
}

export const handbookContent: Record<string, HandbookChapter[]> = {
  parent: [
    {
      title: 'help.handbook.parent.gettingStarted.title',
      sections: [
        { title: 'help.handbook.parent.gettingStarted.login.title', content: ['help.handbook.parent.gettingStarted.login.content', 'help.handbook.parent.gettingStarted.login.content2'] },
        { title: 'help.handbook.parent.gettingStarted.dashboard.title', content: ['help.handbook.parent.gettingStarted.dashboard.content', 'help.handbook.parent.gettingStarted.dashboard.content2'] },
        { title: 'help.handbook.parent.gettingStarted.navigation.title', content: ['help.handbook.parent.gettingStarted.navigation.content'] },
        { title: 'help.handbook.parent.gettingStarted.profile.title', content: ['help.handbook.parent.gettingStarted.profile.content', 'help.handbook.parent.gettingStarted.profile.content2'] },
      ],
    },
    {
      title: 'help.handbook.parent.family.title',
      sections: [
        { title: 'help.handbook.parent.family.create.title', content: ['help.handbook.parent.family.create.content', 'help.handbook.parent.family.create.content2'] },
        { title: 'help.handbook.parent.family.invite.title', content: ['help.handbook.parent.family.invite.content', 'help.handbook.parent.family.invite.content2'] },
        { title: 'help.handbook.parent.family.hours.title', content: ['help.handbook.parent.family.hours.content', 'help.handbook.parent.family.hours.content2'] },
      ],
    },
    {
      title: 'help.handbook.parent.rooms.title',
      sections: [
        { title: 'help.handbook.parent.rooms.overview.title', content: ['help.handbook.parent.rooms.overview.content', 'help.handbook.parent.rooms.overview.content2'] },
        { title: 'help.handbook.parent.rooms.feed.title', content: ['help.handbook.parent.rooms.feed.content', 'help.handbook.parent.rooms.feed.content2'] },
        { title: 'help.handbook.parent.rooms.files.title', content: ['help.handbook.parent.rooms.files.content', 'help.handbook.parent.rooms.files.content2'] },
        { title: 'help.handbook.parent.rooms.fotobox.title', content: ['help.handbook.parent.rooms.fotobox.content'] },
      ],
    },
    {
      title: 'help.handbook.parent.communication.title',
      sections: [
        { title: 'help.handbook.parent.communication.messages.title', content: ['help.handbook.parent.communication.messages.content', 'help.handbook.parent.communication.messages.content2'] },
        { title: 'help.handbook.parent.communication.notifications.title', content: ['help.handbook.parent.communication.notifications.content'] },
        { title: 'help.handbook.parent.communication.feed.title', content: ['help.handbook.parent.communication.feed.content'] },
      ],
    },
    {
      title: 'help.handbook.parent.services.title',
      sections: [
        { title: 'help.handbook.parent.services.jobs.title', content: ['help.handbook.parent.services.jobs.content', 'help.handbook.parent.services.jobs.content2'] },
        { title: 'help.handbook.parent.services.cleaning.title', content: ['help.handbook.parent.services.cleaning.content', 'help.handbook.parent.services.cleaning.content2'] },
        { title: 'help.handbook.parent.services.calendar.title', content: ['help.handbook.parent.services.calendar.content', 'help.handbook.parent.services.calendar.content2'] },
        { title: 'help.handbook.parent.services.forms.title', content: ['help.handbook.parent.services.forms.content', 'help.handbook.parent.services.forms.content2'] },
        { title: 'help.handbook.parent.services.fundgrube.title', content: ['help.handbook.parent.services.fundgrube.content', 'help.handbook.parent.services.fundgrube.content2'] },
      ],
    },
  ],
  student: [
    {
      title: 'help.handbook.student.gettingStarted.title',
      sections: [
        { title: 'help.handbook.student.gettingStarted.login.title', content: ['help.handbook.student.gettingStarted.login.content'] },
        { title: 'help.handbook.student.gettingStarted.dashboard.title', content: ['help.handbook.student.gettingStarted.dashboard.content'] },
        { title: 'help.handbook.student.gettingStarted.profile.title', content: ['help.handbook.student.gettingStarted.profile.content'] },
      ],
    },
    {
      title: 'help.handbook.student.rooms.title',
      sections: [
        { title: 'help.handbook.student.rooms.overview.title', content: ['help.handbook.student.rooms.overview.content'] },
        { title: 'help.handbook.student.rooms.feed.title', content: ['help.handbook.student.rooms.feed.content'] },
        { title: 'help.handbook.student.rooms.files.title', content: ['help.handbook.student.rooms.files.content'] },
        { title: 'help.handbook.student.rooms.discussions.title', content: ['help.handbook.student.rooms.discussions.content'] },
      ],
    },
    {
      title: 'help.handbook.student.tools.title',
      sections: [
        { title: 'help.handbook.student.tools.calendar.title', content: ['help.handbook.student.tools.calendar.content'] },
        { title: 'help.handbook.student.tools.forms.title', content: ['help.handbook.student.tools.forms.content'] },
        { title: 'help.handbook.student.tools.messages.title', content: ['help.handbook.student.tools.messages.content'] },
      ],
    },
  ],
  teacher: [
    {
      title: 'help.handbook.teacher.gettingStarted.title',
      sections: [
        { title: 'help.handbook.teacher.gettingStarted.login.title', content: ['help.handbook.teacher.gettingStarted.login.content'] },
        { title: 'help.handbook.teacher.gettingStarted.dashboard.title', content: ['help.handbook.teacher.gettingStarted.dashboard.content', 'help.handbook.teacher.gettingStarted.dashboard.content2'] },
      ],
    },
    {
      title: 'help.handbook.teacher.rooms.title',
      sections: [
        { title: 'help.handbook.teacher.rooms.manage.title', content: ['help.handbook.teacher.rooms.manage.content', 'help.handbook.teacher.rooms.manage.content2'] },
        { title: 'help.handbook.teacher.rooms.feed.title', content: ['help.handbook.teacher.rooms.feed.content', 'help.handbook.teacher.rooms.feed.content2'] },
        { title: 'help.handbook.teacher.rooms.files.title', content: ['help.handbook.teacher.rooms.files.content', 'help.handbook.teacher.rooms.files.content2'] },
        { title: 'help.handbook.teacher.rooms.fotobox.title', content: ['help.handbook.teacher.rooms.fotobox.content', 'help.handbook.teacher.rooms.fotobox.content2'] },
        { title: 'help.handbook.teacher.rooms.discussions.title', content: ['help.handbook.teacher.rooms.discussions.content'] },
        { title: 'help.handbook.teacher.rooms.members.title', content: ['help.handbook.teacher.rooms.members.content'] },
      ],
    },
    {
      title: 'help.handbook.teacher.communication.title',
      sections: [
        { title: 'help.handbook.teacher.communication.messages.title', content: ['help.handbook.teacher.communication.messages.content', 'help.handbook.teacher.communication.messages.content2'] },
        { title: 'help.handbook.teacher.communication.feed.title', content: ['help.handbook.teacher.communication.feed.content'] },
      ],
    },
    {
      title: 'help.handbook.teacher.tools.title',
      sections: [
        { title: 'help.handbook.teacher.tools.calendar.title', content: ['help.handbook.teacher.tools.calendar.content', 'help.handbook.teacher.tools.calendar.content2'] },
        { title: 'help.handbook.teacher.tools.forms.title', content: ['help.handbook.teacher.tools.forms.content', 'help.handbook.teacher.tools.forms.content2'] },
        { title: 'help.handbook.teacher.tools.jobs.title', content: ['help.handbook.teacher.tools.jobs.content', 'help.handbook.teacher.tools.jobs.content2'] },
      ],
    },
  ],
  admin: [
    {
      title: 'help.handbook.admin.overview.title',
      sections: [
        { title: 'help.handbook.admin.overview.dashboard.title', content: ['help.handbook.admin.overview.dashboard.content', 'help.handbook.admin.overview.dashboard.content2'] },
        { title: 'help.handbook.admin.overview.navigation.title', content: ['help.handbook.admin.overview.navigation.content'] },
      ],
    },
    {
      title: 'help.handbook.admin.users.title',
      sections: [
        { title: 'help.handbook.admin.users.manage.title', content: ['help.handbook.admin.users.manage.content', 'help.handbook.admin.users.manage.content2'] },
        { title: 'help.handbook.admin.users.roles.title', content: ['help.handbook.admin.users.roles.content', 'help.handbook.admin.users.roles.content2'] },
        { title: 'help.handbook.admin.users.approval.title', content: ['help.handbook.admin.users.approval.content'] },
      ],
    },
    {
      title: 'help.handbook.admin.structure.title',
      sections: [
        { title: 'help.handbook.admin.structure.sections.title', content: ['help.handbook.admin.structure.sections.content', 'help.handbook.admin.structure.sections.content2'] },
        { title: 'help.handbook.admin.structure.rooms.title', content: ['help.handbook.admin.structure.rooms.content', 'help.handbook.admin.structure.rooms.content2'] },
        { title: 'help.handbook.admin.structure.families.title', content: ['help.handbook.admin.structure.families.content', 'help.handbook.admin.structure.families.content2'] },
      ],
    },
    {
      title: 'help.handbook.admin.system.title',
      sections: [
        { title: 'help.handbook.admin.system.modules.title', content: ['help.handbook.admin.system.modules.content', 'help.handbook.admin.system.modules.content2'] },
        { title: 'help.handbook.admin.system.settings.title', content: ['help.handbook.admin.system.settings.content'] },
        { title: 'help.handbook.admin.system.theme.title', content: ['help.handbook.admin.system.theme.content'] },
        { title: 'help.handbook.admin.system.cleaning.title', content: ['help.handbook.admin.system.cleaning.content', 'help.handbook.admin.system.cleaning.content2'] },
        { title: 'help.handbook.admin.system.billing.title', content: ['help.handbook.admin.system.billing.content'] },
        { title: 'help.handbook.admin.system.errorReports.title', content: ['help.handbook.admin.system.errorReports.content'] },
      ],
    },
  ],
}
