const { mdToPdf } = require('md-to-pdf');
const path = require('path');
const fs = require('fs');

const docsDir = __dirname;
const outDir = path.join(docsDir, 'pdf');

if (!fs.existsSync(outDir)) {
  fs.mkdirSync(outDir, { recursive: true });
}

const documents = [
  {
    input: 'handbuch_eltern.md',
    output: 'MonteWeb_Handbuch_Eltern.pdf',
    headerTitle: 'MonteWeb — Handbuch f\u00fcr Eltern',
    accentColor: '#4CAF50',
  },
  {
    input: 'handbuch_lehrer.md',
    output: 'MonteWeb_Handbuch_Lehrkraefte.pdf',
    headerTitle: 'MonteWeb — Handbuch f\u00fcr Lehrkr\u00e4fte',
    accentColor: '#2196F3',
  },
  {
    input: 'handbuch_admin.md',
    output: 'MonteWeb_Handbuch_Administration.pdf',
    headerTitle: 'MonteWeb — Handbuch f\u00fcr Administration',
    accentColor: '#9C27B0',
  },
  {
    input: 'cheatsheet_eltern.md',
    output: 'MonteWeb_CheatSheet_Eltern.pdf',
    headerTitle: 'MonteWeb — Cheat-Sheet Eltern',
    accentColor: '#4CAF50',
  },
  {
    input: 'cheatsheet_lehrer.md',
    output: 'MonteWeb_CheatSheet_Lehrkraefte.pdf',
    headerTitle: 'MonteWeb — Cheat-Sheet Lehrkr\u00e4fte',
    accentColor: '#2196F3',
  },
  {
    input: 'cheatsheet_section_admin.md',
    output: 'MonteWeb_CheatSheet_Bereichsleitung.pdf',
    headerTitle: 'MonteWeb — Cheat-Sheet Bereichsleitung',
    accentColor: '#FF9800',
  },
  {
    input: 'cheatsheet_superadmin.md',
    output: 'MonteWeb_CheatSheet_Superadmin.pdf',
    headerTitle: 'MonteWeb — Cheat-Sheet Superadmin',
    accentColor: '#9C27B0',
  },
];

function buildCss(accentColor) {
  return `
    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

    :root {
      --accent: ${accentColor};
      --accent-light: ${accentColor}18;
      --text: #1a1a2e;
      --text-secondary: #555;
      --border: #e0e0e0;
      --bg-code: #f5f7fa;
      --bg-table-header: ${accentColor}12;
    }

    body {
      font-family: 'Inter', 'Segoe UI', system-ui, -apple-system, sans-serif;
      color: var(--text);
      line-height: 1.65;
      font-size: 10.5pt;
      max-width: 100%;
    }

    /* Title page styling */
    h1 {
      font-size: 24pt;
      font-weight: 700;
      color: var(--accent);
      border-bottom: 3px solid var(--accent);
      padding-bottom: 12px;
      margin-top: 0;
      margin-bottom: 8px;
      letter-spacing: -0.5px;
    }

    h1 + p {
      font-size: 10pt;
      color: var(--text-secondary);
      margin-bottom: 20px;
    }

    h2 {
      font-size: 15pt;
      font-weight: 600;
      color: var(--accent);
      margin-top: 28px;
      margin-bottom: 10px;
      padding-bottom: 5px;
      border-bottom: 1.5px solid var(--border);
      page-break-after: avoid;
    }

    h3 {
      font-size: 12pt;
      font-weight: 600;
      color: #2c3e50;
      margin-top: 18px;
      margin-bottom: 8px;
      page-break-after: avoid;
    }

    h4 {
      font-size: 11pt;
      font-weight: 600;
      color: var(--text-secondary);
      margin-top: 14px;
      margin-bottom: 6px;
    }

    p {
      margin: 6px 0;
    }

    /* Horizontal rules */
    hr {
      border: none;
      height: 1px;
      background: linear-gradient(to right, var(--accent), transparent);
      margin: 20px 0;
    }

    /* Tables */
    table {
      width: 100%;
      border-collapse: collapse;
      margin: 12px 0;
      font-size: 9.5pt;
      page-break-inside: auto;
    }

    thead {
      page-break-after: avoid;
    }

    th {
      background: var(--bg-table-header);
      color: var(--text);
      font-weight: 600;
      text-align: left;
      padding: 8px 10px;
      border: 1px solid var(--border);
      font-size: 9pt;
      text-transform: uppercase;
      letter-spacing: 0.3px;
    }

    td {
      padding: 7px 10px;
      border: 1px solid var(--border);
      vertical-align: top;
    }

    tr:nth-child(even) {
      background: #fafbfc;
    }

    tr:hover {
      background: var(--accent-light);
    }

    /* Status cells */
    td:last-child {
      text-align: center;
    }

    /* Code blocks */
    pre {
      background: var(--bg-code);
      border: 1px solid var(--border);
      border-left: 3px solid var(--accent);
      border-radius: 4px;
      padding: 12px 14px;
      font-size: 9pt;
      line-height: 1.5;
      overflow-x: auto;
      page-break-inside: avoid;
    }

    code {
      font-family: 'Cascadia Code', 'Fira Code', 'JetBrains Mono', 'Consolas', monospace;
      font-size: 9pt;
    }

    p code, li code, td code {
      background: var(--bg-code);
      padding: 1px 5px;
      border-radius: 3px;
      border: 1px solid #e8e8e8;
      font-size: 8.5pt;
    }

    /* Lists */
    ul, ol {
      margin: 6px 0;
      padding-left: 22px;
    }

    li {
      margin: 3px 0;
    }

    li::marker {
      color: var(--accent);
    }

    /* Bold text in tables for emphasis */
    strong {
      font-weight: 600;
      color: #1a1a2e;
    }

    /* Blockquotes for tips/notes */
    blockquote {
      border-left: 3px solid var(--accent);
      background: var(--accent-light);
      margin: 12px 0;
      padding: 10px 14px;
      border-radius: 0 4px 4px 0;
      font-size: 9.5pt;
    }

    blockquote p {
      margin: 2px 0;
    }

    /* Images / Screenshots */
    img {
      max-width: 100%;
      height: auto;
      border: 1px solid var(--border);
      border-radius: 6px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      margin: 12px 0;
      display: block;
    }

    /* Links */
    a {
      color: var(--accent);
      text-decoration: none;
    }

    /* Page break helpers */
    h2 {
      page-break-before: auto;
    }

    /* Emoji-like symbols */
    .emoji {
      font-size: 12pt;
    }

    /* Print optimizations */
    @media print {
      body {
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
      }
    }
  `;
}

async function generatePdf(doc) {
  const inputPath = path.join(docsDir, doc.input);
  const outputPath = path.join(outDir, doc.output);

  if (!fs.existsSync(inputPath)) {
    console.error(`  SKIP: ${doc.input} not found`);
    return;
  }

  console.log(`  Generating: ${doc.output} ...`);

  try {
    const pdf = await mdToPdf(
      { path: inputPath },
      {
        css: buildCss(doc.accentColor),
        document_title: doc.headerTitle,
        pdf_options: {
          format: 'A4',
          margin: {
            top: '20mm',
            right: '18mm',
            bottom: '22mm',
            left: '18mm',
          },
          printBackground: true,
          displayHeaderFooter: true,
          headerTemplate: `
            <div style="width:100%; font-size:7pt; font-family:'Inter','Segoe UI',sans-serif; color:#999; padding:0 18mm; display:flex; justify-content:space-between;">
              <span>${doc.headerTitle}</span>
              <span>Version 1.0</span>
            </div>
          `,
          footerTemplate: `
            <div style="width:100%; font-size:7pt; font-family:'Inter','Segoe UI',sans-serif; color:#999; padding:0 18mm; display:flex; justify-content:space-between;">
              <span>\u00a9 2026 MonteWeb</span>
              <span>Seite <span class="pageNumber"></span> von <span class="totalPages"></span></span>
            </div>
          `,
        },
        launch_options: {
          args: ['--no-sandbox', '--disable-setuid-sandbox'],
        },
      }
    );

    if (pdf && pdf.content) {
      fs.writeFileSync(outputPath, pdf.content);
      const sizeMB = (fs.statSync(outputPath).size / 1024).toFixed(0);
      console.log(`  OK: ${doc.output} (${sizeMB} KB)`);
    }
  } catch (err) {
    console.error(`  ERROR generating ${doc.output}:`, err.message);
  }
}

async function main() {
  console.log('MonteWeb PDF Generator');
  console.log('======================\n');
  console.log(`Output: ${outDir}\n`);

  for (const doc of documents) {
    await generatePdf(doc);
  }

  console.log('\nDone!');
}

main().catch(console.error);
