import { chromium } from 'playwright';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

async function main() {
  const htmlPath = path.resolve(__dirname, 'docs', 'MonteWeb-Benutzerhandbuch.html');
  const pdfPath = path.resolve(__dirname, 'docs', 'MonteWeb-Benutzerhandbuch.pdf');

  console.log(`Converting: ${htmlPath}`);
  console.log(`Output:     ${pdfPath}`);

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  // Load the HTML file
  await page.goto(`file:///${htmlPath.replace(/\\/g, '/')}`, {
    waitUntil: 'networkidle',
    timeout: 60000,
  });

  // Wait for all images to load
  await page.waitForTimeout(3000);

  // Check how many images loaded vs failed
  const imageStats = await page.evaluate(() => {
    const imgs = document.querySelectorAll('img');
    let loaded = 0, failed = 0;
    imgs.forEach(img => {
      if (img.complete && img.naturalWidth > 0) loaded++;
      else failed++;
    });
    return { total: imgs.length, loaded, failed };
  });
  console.log(`Images: ${imageStats.loaded}/${imageStats.total} loaded, ${imageStats.failed} failed`);

  // Generate PDF
  await page.pdf({
    path: pdfPath,
    format: 'A4',
    printBackground: true,
    margin: {
      top: '15mm',
      bottom: '15mm',
      left: '12mm',
      right: '12mm',
    },
    displayHeaderFooter: true,
    headerTemplate: `
      <div style="font-size: 8px; color: #999; width: 100%; text-align: center; padding: 0 20mm;">
        MonteWeb Benutzerhandbuch
      </div>
    `,
    footerTemplate: `
      <div style="font-size: 8px; color: #999; width: 100%; text-align: center; padding: 0 20mm;">
        Seite <span class="pageNumber"></span> von <span class="totalPages"></span>
      </div>
    `,
  });

  console.log(`PDF generated: ${pdfPath}`);

  await browser.close();
}

main().catch(e => { console.error(e); process.exit(1); });
