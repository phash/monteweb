import { chromium } from 'playwright';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

async function main() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  const htmlPath = path.join(__dirname, 'presentation.html');
  await page.goto(`file://${htmlPath}`, { waitUntil: 'load' });
  await page.waitForTimeout(2000);

  await page.pdf({
    path: path.join(__dirname, 'MonteWeb-Produktpraesentation.pdf'),
    width: '297mm',
    height: '210mm',
    margin: { top: 0, right: 0, bottom: 0, left: 0 },
    printBackground: true,
    preferCSSPageSize: true,
  });

  console.log('PDF generated: MonteWeb-Produktpraesentation.pdf');
  await browser.close();
}

main().catch(e => { console.error(e); process.exit(1); });
