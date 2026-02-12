import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';

const FRONTEND = 'http://localhost:5173';
const BACKEND = 'http://localhost:8090';
const DIR = './screenshots-handbuch';

const USERS = [
  { email: 'admin@monteweb.local', password: 'admin123', label: 'admin', displayName: 'Admin', role: 'SUPERADMIN' },
  { email: 'sectionadmin@monteweb.local', password: 'test1234', label: 'sectionadmin', displayName: 'Sarah Bereichs', role: 'SECTION_ADMIN' },
  { email: 'lehrer@monteweb.local', password: 'test1234', label: 'teacher', displayName: 'Thomas Lehmann', role: 'TEACHER' },
  { email: 'eltern@monteweb.local', password: 'test1234', label: 'parent', displayName: 'Maria Elterlich', role: 'PARENT' },
  { email: 'schueler@monteweb.local', password: 'test1234', label: 'student', displayName: 'Lukas Schultze', role: 'STUDENT' },
];

// Standard pages all users can see
const STANDARD_PAGES = [
  { path: '/', name: 'dashboard', label: 'Dashboard' },
  { path: '/rooms', name: 'rooms', label: 'Räume' },
  { path: '/rooms/discover', name: 'discover-rooms', label: 'Räume entdecken' },
  { path: '/family', name: 'family', label: 'Familie' },
  { path: '/messages', name: 'messages', label: 'Nachrichten' },
  { path: '/jobs', name: 'jobs', label: 'Jobbörse' },
  { path: '/cleaning', name: 'cleaning', label: 'Putz-Organisation' },
  { path: '/calendar', name: 'calendar', label: 'Kalender' },
  { path: '/forms', name: 'forms', label: 'Formulare' },
  { path: '/profile', name: 'profile', label: 'Profil' },
];

const ADMIN_PAGES = [
  { path: '/admin', name: 'admin-dashboard', label: 'Admin-Dashboard' },
  { path: '/admin/users', name: 'admin-users', label: 'Benutzerverwaltung' },
  { path: '/admin/rooms', name: 'admin-rooms', label: 'Raumverwaltung' },
  { path: '/admin/sections', name: 'admin-sections', label: 'Schulbereiche' },
  { path: '/admin/modules', name: 'admin-modules', label: 'Modulverwaltung' },
  { path: '/admin/theme', name: 'admin-theme', label: 'Design & Branding' },
  { path: '/admin/cleaning', name: 'admin-cleaning', label: 'Putz-Verwaltung' },
  { path: '/admin/job-report', name: 'admin-jobreport', label: 'Stundenbericht' },
];

async function loginViaApi(page, context, email, password) {
  // Login via API to get token, then set it in localStorage
  const response = await page.evaluate(async ({ email, password, backend }) => {
    const res = await fetch(`${backend}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    return res.json();
  }, { email, password, backend: BACKEND });

  if (!response.success) {
    throw new Error(`API login failed: ${response.message}`);
  }

  const { accessToken, refreshToken } = response.data;
  await page.evaluate(({ accessToken, refreshToken }) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
  }, { accessToken, refreshToken });

  return response;
}

async function loginViaForm(page, email, password) {
  await page.goto(`${FRONTEND}/login`, { timeout: 15000, waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  // Take login page screenshot (only once)
  return page;
}

async function goto(page, urlPath, waitMs = 2000) {
  await page.goto(`${FRONTEND}${urlPath}`, { timeout: 15000, waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(waitMs);
}

async function screenshotPage(page, filepath) {
  await page.screenshot({ path: filepath, fullPage: false });
}

async function main() {
  fs.mkdirSync(DIR, { recursive: true });

  const browser = await chromium.launch({ headless: true });

  // First, capture login page
  console.log('=== LOGIN PAGE ===');
  const loginCtx = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    serviceWorkers: 'block',
  });
  const loginPage = await loginCtx.newPage();
  await loginPage.goto(`${FRONTEND}/login`, { timeout: 15000, waitUntil: 'networkidle' });
  await loginPage.waitForTimeout(2000);
  await screenshotPage(loginPage, `${DIR}/login-page.png`);
  console.log('  login-page.png');

  // Mobile login
  await loginPage.setViewportSize({ width: 390, height: 844 });
  await loginPage.waitForTimeout(1000);
  await screenshotPage(loginPage, `${DIR}/login-page-mobile.png`);
  console.log('  login-page-mobile.png');
  await loginCtx.close();

  // Process each user
  for (const user of USERS) {
    console.log(`\n=== ${user.label.toUpperCase()} (${user.email}) ===`);

    const context = await browser.newContext({
      viewport: { width: 1440, height: 900 },
      serviceWorkers: 'block',
    });
    const page = await context.newPage();

    try {
      // Navigate to frontend first to set localStorage
      await page.goto(`${FRONTEND}/login`, { timeout: 15000, waitUntil: 'networkidle' });
      await page.waitForTimeout(500);

      // Login via form
      await page.fill('#email', user.email);
      await page.fill('#password input', user.password);
      await page.click('button[type="submit"]');
      await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 10000 });
      await page.waitForTimeout(2500);

      console.log('  Logged in successfully');
    } catch (e) {
      console.error(`  Login FAILED: ${e.message}`);
      await screenshotPage(page, `${DIR}/${user.label}-LOGIN-FAILED.png`);
      await context.close();
      continue;
    }

    // Standard pages
    const pages = [...STANDARD_PAGES];
    if (user.role === 'SUPERADMIN' || user.role === 'SECTION_ADMIN') {
      pages.push(...ADMIN_PAGES);
    }

    for (const pg of pages) {
      const filename = `${user.label}-${pg.name}.png`;
      try {
        await goto(page, pg.path);
        await screenshotPage(page, `${DIR}/${filename}`);
        console.log(`  ${filename}`);
      } catch (e) {
        console.log(`  ${filename} - FAILED: ${e.message}`);
      }
    }

    // Mobile screenshots for key pages
    await page.setViewportSize({ width: 390, height: 844 });

    const mobilePages = [
      { path: '/', name: 'mobile-dashboard' },
      { path: '/rooms', name: 'mobile-rooms' },
      { path: '/profile', name: 'mobile-profile' },
    ];

    for (const pg of mobilePages) {
      const filename = `${user.label}-${pg.name}.png`;
      try {
        await goto(page, pg.path);
        await screenshotPage(page, `${DIR}/${filename}`);
        console.log(`  ${filename}`);
      } catch (e) {
        console.log(`  ${filename} - FAILED: ${e.message}`);
      }
    }

    await context.close();
  }

  await browser.close();
  console.log(`\nDone! All screenshots saved to ${DIR}`);
}

main().catch(e => { console.error(e); process.exit(1); });
