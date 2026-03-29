export interface TestAccount {
  email: string
  password: string
  role: 'SUPERADMIN' | 'SECTION_ADMIN' | 'TEACHER' | 'PARENT' | 'STUDENT'
  displayName: string
}

export const accounts: Record<string, TestAccount> = {
  admin: {
    email: 'admin@monteweb.local',
    password: 'admin123',
    role: 'SUPERADMIN',
    displayName: 'Admin User',
  },
  sectionAdmin: {
    email: 'sectionadmin@monteweb.local',
    password: 'test1234',
    role: 'SECTION_ADMIN',
    displayName: 'Section Admin',
  },
  teacher: {
    email: 'lehrer@monteweb.local',
    password: 'test1234',
    role: 'TEACHER',
    displayName: 'Test Lehrer',
  },
  parent: {
    email: 'eltern@monteweb.local',
    password: 'test1234',
    role: 'PARENT',
    displayName: 'Test Eltern',
  },
  student: {
    email: 'schueler@monteweb.local',
    password: 'test1234',
    role: 'STUDENT',
    displayName: 'Test Schueler',
  },
}
