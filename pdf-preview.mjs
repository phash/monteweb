import { chromium } from 'playwright';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

async function main() {
  const htmlPath = path.resolve(__dirname, 'docs', 'MonteWeb-Benutzerhandbuch.html');
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 900, height: 1270 } });
  await page.goto(`file:///${htmlPath.replace(/\\/g, '/')}`, { waitUntil: 'networkidle', timeout: 30000 });
  await page.waitForTimeout(2000);
  await page.screenshot({ path: path.resolve(__dirname, 'docs', 'handbuch-seite1.png'), clip: { x: 0, y: 0, width: 900, height: 1270 } });
  console.log('Screenshot saved');
  await browser.close();
}

main().catch(e => { console.error(e); process.exit(1); });
