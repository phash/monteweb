const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

const BASE_URL = 'http://localhost';
const SCREENSHOT_DIR = path.join(__dirname, 'screenshots');

if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

const ACCOUNTS = {
  parent: { email: 'eltern@monteweb.local', password: 'test1234' },
  teacher: { email: 'lehrer@monteweb.local', password: 'test1234' },
  admin: { email: 'admin@monteweb.local', password: 'test1234' },
  sectionadmin: { email: 'sectionadmin@monteweb.local', password: 'test1234' },
  student: { email: 'schueler@monteweb.local', password: 'test1234' },
};

// Screenshots to take, grouped by role
const SCREENSHOT_PLAN = [
  // === PARENT (Eltern-Handbuch) ===
  { role: 'parent', name: 'login', path: '/login', waitFor: 'form', desc: 'Login-Seite' },
  { role: 'parent', name: 'eltern_dashboard', path: '/', wait: 2000, desc: 'Dashboard (Eltern)' },
  { role: 'parent', name: 'eltern_familie', path: '/family', wait: 2000, desc: 'Familienverbund' },
  { role: 'parent', name: 'eltern_raeume', path: '/rooms', wait: 2000, desc: 'Meine Raeume' },
  { role: 'parent', name: 'eltern_nachrichten', path: '/messages', wait: 2000, desc: 'Nachrichten' },
  { role: 'parent', name: 'eltern_jobboerse', path: '/jobs', wait: 2000, desc: 'Jobboerse' },
  { role: 'parent', name: 'eltern_putzorga', path: '/cleaning', wait: 2000, desc: 'Putz-Orga' },
  { role: 'parent', name: 'eltern_kalender', path: '/calendar', wait: 2000, desc: 'Kalender' },
  { role: 'parent', name: 'eltern_formulare', path: '/forms', wait: 2000, desc: 'Formulare' },
  { role: 'parent', name: 'eltern_fundgrube', path: '/fundgrube', wait: 2000, desc: 'Fundgrube' },
  { role: 'parent', name: 'eltern_profil', path: '/profile', wait: 2000, desc: 'Profil' },
  { role: 'parent', name: 'eltern_entdecken', path: '/rooms/discover', wait: 2000, desc: 'Raeume entdecken' },

  // === TEACHER (Lehrer-Handbuch) ===
  { role: 'teacher', name: 'lehrer_dashboard', path: '/', wait: 2000, desc: 'Dashboard (Lehrer)' },
  { role: 'teacher', name: 'lehrer_raeume', path: '/rooms', wait: 2000, desc: 'Meine Raeume (Lehrer)' },
  { role: 'teacher', name: 'lehrer_nachrichten', path: '/messages', wait: 2000, desc: 'Nachrichten (Lehrer)' },
  { role: 'teacher', name: 'lehrer_jobboerse', path: '/jobs', wait: 2000, desc: 'Jobboerse (Lehrer)' },
  { role: 'teacher', name: 'lehrer_kalender', path: '/calendar', wait: 2000, desc: 'Kalender (Lehrer)' },
  { role: 'teacher', name: 'lehrer_formulare', path: '/forms', wait: 2000, desc: 'Formulare (Lehrer)' },
  { role: 'teacher', name: 'lehrer_putzorga', path: '/cleaning', wait: 2000, desc: 'Putz-Orga (Lehrer)' },
  { role: 'teacher', name: 'lehrer_entdecken', path: '/rooms/discover', wait: 2000, desc: 'Raeume entdecken (Lehrer)' },

  // === SECTION ADMIN ===
  { role: 'sectionadmin', name: 'sectionadmin_panel', path: '/section-admin', wait: 2000, desc: 'Bereichsverwaltung' },
  { role: 'sectionadmin', name: 'sectionadmin_dashboard', path: '/', wait: 2000, desc: 'Dashboard (Section Admin)' },

  // === SUPERADMIN ===
  { role: 'admin', name: 'admin_dashboard', path: '/admin', wait: 2000, desc: 'Admin-Dashboard' },
  { role: 'admin', name: 'admin_benutzer', path: '/admin/users', wait: 2000, desc: 'Benutzerverwaltung' },
  { role: 'admin', name: 'admin_raeume', path: '/admin/rooms', wait: 2000, desc: 'Raumverwaltung' },
  { role: 'admin', name: 'admin_bereiche', path: '/admin/sections', wait: 2000, desc: 'Schulbereiche' },
  { role: 'admin', name: 'admin_familien', path: '/admin/families', wait: 2000, desc: 'Familienverwaltung' },
  { role: 'admin', name: 'admin_module', path: '/admin/modules', wait: 2000, desc: 'Module verwalten' },
  { role: 'admin', name: 'admin_putzorga', path: '/admin/cleaning', wait: 2000, desc: 'Putz-Orga Verwaltung' },
  { role: 'admin', name: 'admin_stundenbericht', path: '/admin/job-report', wait: 2000, desc: 'Stundenbericht' },
  { role: 'admin', name: 'admin_jahresabrechnung', path: '/admin/billing', wait: 2000, desc: 'Jahresabrechnung' },
  { role: 'admin', name: 'admin_design', path: '/admin/theme', wait: 2000, desc: 'Design & Branding' },
  { role: 'admin', name: 'admin_fehlermeldungen', path: '/admin/error-reports', wait: 2000, desc: 'Fehlermeldungen' },
  { role: 'admin', name: 'admin_hauptdashboard', path: '/', wait: 2000, desc: 'Dashboard (Admin)' },
];

async function login(page, account) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle2', timeout: 30000 });
  await page.waitForSelector('input[type="email"], input[id*="email"], input[placeholder*="mail"]', { timeout: 10000 });

  // Clear and fill email
  const emailInput = await page.$('input[type="email"]') || await page.$('input[id*="email"]');
  if (emailInput) {
    await emailInput.click({ clickCount: 3 });
    await emailInput.type(account.email, { delay: 20 });
  }

  // Clear and fill password
  const passwordInput = await page.$('input[type="password"]');
  if (passwordInput) {
    await passwordInput.click({ clickCount: 3 });
    await passwordInput.type(account.password, { delay: 20 });
  }

  // Find and click submit button
  const submitBtn = await page.$('button[type="submit"]') || await page.$('button.p-button');
  if (submitBtn) {
    await submitBtn.click();
  }

  // Wait for navigation to dashboard
  await page.waitForNavigation({ waitUntil: 'networkidle2', timeout: 15000 }).catch(() => {});
  await new Promise(r => setTimeout(r, 2000));
}

async function takeScreenshot(page, screenshot) {
  const filePath = path.join(SCREENSHOT_DIR, `${screenshot.name}.png`);

  try {
    // Navigate if not login page (login is taken before auth)
    if (screenshot.name !== 'login') {
      await page.goto(`${BASE_URL}${screenshot.path}`, {
        waitUntil: 'networkidle2',
        timeout: 20000,
      });
    }

    // Wait for content to render
    const waitTime = screenshot.wait || 1500;
    await new Promise(r => setTimeout(r, waitTime));

    // Dismiss any toast notifications that might be visible
    try {
      const toastClose = await page.$('.p-toast-close-icon, .p-toast-message-close');
      if (toastClose) await toastClose.click();
    } catch (e) { /* no toast */ }

    // Wait a bit more for any animations to complete
    await new Promise(r => setTimeout(r, 500));

    await page.screenshot({
      path: filePath,
      fullPage: false,
      type: 'png',
    });

    console.log(`  OK: ${screenshot.name}.png — ${screenshot.desc}`);
  } catch (err) {
    console.error(`  FAIL: ${screenshot.name}.png — ${err.message}`);
  }
}

async function main() {
  console.log('MonteWeb Screenshot Generator');
  console.log('=============================\n');

  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu'],
    defaultViewport: { width: 1440, height: 900 },
  });

  const page = await browser.newPage();
  // Set German locale
  await page.setExtraHTTPHeaders({ 'Accept-Language': 'de-DE,de;q=0.9' });

  let currentRole = null;

  for (const screenshot of SCREENSHOT_PLAN) {
    // Login if role changed
    if (screenshot.role !== currentRole) {
      if (screenshot.name === 'login') {
        // Special case: screenshot the login page before logging in
        await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle2', timeout: 30000 });
        await new Promise(r => setTimeout(r, 1500));
        await takeScreenshot(page, screenshot);
        // Now login
        const account = ACCOUNTS[screenshot.role];
        console.log(`\n  Logging in as ${account.email}...`);
        await login(page, account);
        currentRole = screenshot.role;
        continue;
      }

      const account = ACCOUNTS[screenshot.role];
      console.log(`\n  Logging in as ${account.email}...`);

      // Clear cookies/storage for fresh login
      const client = await page.createCDPSession();
      await client.send('Storage.clearDataForOrigin', {
        origin: BASE_URL,
        storageTypes: 'all',
      });
      await client.detach();

      await login(page, account);
      currentRole = screenshot.role;
    }

    await takeScreenshot(page, screenshot);
  }

  await browser.close();

  // List all generated screenshots
  const files = fs.readdirSync(SCREENSHOT_DIR).filter(f => f.endsWith('.png'));
  console.log(`\n${'='.repeat(40)}`);
  console.log(`Generated ${files.length} screenshots in ${SCREENSHOT_DIR}`);
}

main().catch(console.error);
