import { chromium } from 'playwright';
import http from 'node:http';

const BASE = 'http://localhost:4173';
const DIR = './screenshots';

function getToken() {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify({ email: 'admin@monteweb.local', password: 'Admin12345' });
    const req = http.request({
      hostname: '127.0.0.1',
      port: 8080,
      path: '/api/v1/auth/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Content-Length': data.length }
    }, (res) => {
      let body = '';
      res.on('data', c => body += c);
      res.on('end', () => resolve(JSON.parse(body)));
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

async function goto(page, path) {
  await page.goto(`${BASE}${path}`, { timeout: 15000, waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(2500);
}

async function shot(page, name) {
  await page.screenshot({ path: `${DIR}/${name}` });
  console.log(name);
}

async function main() {
  const loginResp = await getToken();
  const token = loginResp.data;
  console.log(`Logged in as ${token.email} (${token.role})`);

  const browser = await chromium.launch({ headless: true });

  // 1. Login page - fresh context (no token), disable SW
  const loginCtx = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    serviceWorkers: 'block'
  });
  const loginPage = await loginCtx.newPage();
  await loginPage.goto(`${BASE}/login`, { timeout: 15000, waitUntil: 'domcontentloaded' });
  await loginPage.waitForTimeout(4000);
  await loginPage.screenshot({ path: `${DIR}/01-login.png` });
  console.log('01-login.png');
  await loginCtx.close();

  // Authenticated context
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    serviceWorkers: 'block'
  });
  await context.addInitScript((t) => {
    localStorage.setItem('accessToken', t.accessToken);
    localStorage.setItem('refreshToken', t.refreshToken);
  }, token);
  const page = await context.newPage();

  // 2. Dashboard
  await goto(page, '/');
  await shot(page, '02-dashboard.png');

  // 3. Rooms
  await goto(page, '/rooms');
  await shot(page, '03-rooms.png');

  // 4. Discover Rooms
  await goto(page, '/rooms/discover');
  await shot(page, '04-discover.png');

  // 5. Room Detail
  const roomsResp = await page.evaluate(async () => {
    const t = localStorage.getItem('accessToken');
    const r = await fetch('/api/v1/rooms/mine', { headers: { Authorization: `Bearer ${t}` } });
    return r.json();
  });
  const firstRoomId = roomsResp?.data?.[0]?.id;
  if (firstRoomId) {
    await goto(page, `/rooms/${firstRoomId}`);
    await shot(page, '05-room-detail.png');
  } else {
    console.log('05-room-detail.png - SKIPPED');
  }

  // 6. Family
  await goto(page, '/family');
  await shot(page, '06-family.png');

  // 7. Messages
  await goto(page, '/messages');
  await shot(page, '07-messages.png');

  // 8. Job Board
  await goto(page, '/jobs');
  await shot(page, '08-jobs.png');

  // 9. Cleaning
  await goto(page, '/cleaning');
  await shot(page, '09-cleaning.png');

  // 10. Calendar
  await goto(page, '/calendar');
  await shot(page, '10-calendar.png');

  // 11. Calendar Create Event
  await goto(page, '/calendar/create');
  await shot(page, '11-calendar-create.png');

  // 12. Calendar Event Detail
  const evResp = await page.evaluate(async () => {
    const t = localStorage.getItem('accessToken');
    const r = await fetch('/api/v1/calendar/events?from=2026-01-01&to=2026-12-31', { headers: { Authorization: `Bearer ${t}` } });
    return r.json();
  });
  const firstEventId = evResp?.data?.content?.[0]?.id;
  if (firstEventId) {
    await goto(page, `/calendar/events/${firstEventId}`);
    await shot(page, '12-event-detail.png');
  } else {
    console.log('12-event-detail.png - SKIPPED');
  }

  // 13. Profile
  await goto(page, '/profile');
  await shot(page, '13-profile.png');

  // 14-19. Admin pages
  await goto(page, '/admin');
  await shot(page, '14-admin-dashboard.png');

  await goto(page, '/admin/users');
  await shot(page, '15-admin-users.png');

  await goto(page, '/admin/rooms');
  await shot(page, '16-admin-rooms.png');

  await goto(page, '/admin/sections');
  await shot(page, '17-admin-sections.png');

  await goto(page, '/admin/modules');
  await shot(page, '18-admin-modules.png');

  await goto(page, '/admin/theme');
  await shot(page, '19-admin-theme.png');

  // 20-21. Mobile
  await page.setViewportSize({ width: 390, height: 844 });
  await goto(page, '/');
  await shot(page, '20-mobile-dashboard.png');

  await goto(page, '/calendar');
  await shot(page, '21-mobile-calendar.png');

  await browser.close();
  console.log('Done! All screenshots saved.');
}

main().catch(e => { console.error(e); process.exit(1); });
