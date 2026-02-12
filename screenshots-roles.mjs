import { chromium } from 'playwright';
import fs from 'node:fs';

const FRONTEND = 'http://localhost:5173';
const DIR = './screenshots-roles';

const USERS = [
  { email: 'lehrer@monteweb.local', password: 'test1234', label: 'teacher' },
  { email: 'eltern@monteweb.local', password: 'test1234', label: 'parent' },
  { email: 'schueler@monteweb.local', password: 'test1234', label: 'student' },
  { email: 'sectionadmin@monteweb.local', password: 'test1234', label: 'sectionadmin' },
  { email: 'admin@monteweb.local', password: 'admin123', label: 'admin' },
];

const PAGES = [
  { path: '/', name: 'dashboard' },
  { path: '/rooms', name: 'rooms' },
  { path: '/family', name: 'family' },
  { path: '/messages', name: 'messages' },
  { path: '/jobs', name: 'jobs' },
  { path: '/cleaning', name: 'cleaning' },
  { path: '/calendar', name: 'calendar' },
  { path: '/forms', name: 'forms' },
  { path: '/profile', name: 'profile' },
];

const ADMIN_PAGES = [
  { path: '/admin', name: 'admin-dashboard' },
  { path: '/admin/users', name: 'admin-users' },
  { path: '/admin/rooms', name: 'admin-rooms' },
  { path: '/admin/sections', name: 'admin-sections' },
  { path: '/admin/modules', name: 'admin-modules' },
  { path: '/admin/theme', name: 'admin-theme' },
  { path: '/admin/cleaning', name: 'admin-cleaning' },
  { path: '/admin/job-report', name: 'admin-jobreport' },
];

async function loginViaForm(page, email, password) {
  await page.goto(`${FRONTEND}/login`, { timeout: 15000, waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  // Fill in the login form
  await page.fill('#email', email);
  // PrimeVue Password wraps <input> inside a <div id="password">, target the inner input
  await page.fill('#password input', password);

  // Submit the form
  await page.click('button[type="submit"]');

  // Wait for navigation away from login page
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 10000 });
  await page.waitForTimeout(2000);
}

async function goto(page, path) {
  await page.goto(`${FRONTEND}${path}`, { timeout: 15000, waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(2500);
}

async function main() {
  fs.mkdirSync(DIR, { recursive: true });
  const browser = await chromium.launch({ headless: true });

  for (const user of USERS) {
    console.log(`\n=== ${user.label.toUpperCase()} (${user.email}) ===`);

    const context = await browser.newContext({
      viewport: { width: 1440, height: 900 },
      serviceWorkers: 'block'
    });
    const page = await context.newPage();

    try {
      await loginViaForm(page, user.email, user.password);
      console.log(`  Logged in successfully`);
    } catch (e) {
      console.error(`  Login failed: ${e.message}`);
      // Take a screenshot of the failure state
      await page.screenshot({ path: `${DIR}/${user.label}-LOGIN-FAILED.png` });
      await context.close();
      continue;
    }

    // Desktop pages
    const pages = [...PAGES];
    if (user.label === 'admin' || user.label === 'sectionadmin') {
      pages.push(...ADMIN_PAGES);
    }

    for (const pg of pages) {
      const filename = `${user.label}-${pg.name}.png`;
      try {
        await goto(page, pg.path);
        await page.screenshot({ path: `${DIR}/${filename}` });
        console.log(`  ${filename}`);
      } catch (e) {
        console.log(`  ${filename} - FAILED: ${e.message}`);
      }
    }

    // Mobile dashboard
    await page.setViewportSize({ width: 390, height: 844 });
    await goto(page, '/');
    await page.screenshot({ path: `${DIR}/${user.label}-mobile-dashboard.png` });
    console.log(`  ${user.label}-mobile-dashboard.png`);

    // Mobile rooms
    await goto(page, '/rooms');
    await page.screenshot({ path: `${DIR}/${user.label}-mobile-rooms.png` });
    console.log(`  ${user.label}-mobile-rooms.png`);

    await context.close();
  }

  await browser.close();
  console.log('\nDone! All screenshots saved to', DIR);
}

main().catch(e => { console.error(e); process.exit(1); });
