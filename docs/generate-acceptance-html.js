#!/usr/bin/env node
/**
 * Generates an interactive HTML acceptance test report from the markdown source.
 * Features: Checkboxes with localStorage persistence, progress bars per module,
 * role filter, search, print-friendly.
 *
 * Usage: node generate-acceptance-html.js
 * Output: acceptance-tests.html
 */

const fs = require('fs');
const path = require('path');

const md = fs.readFileSync(path.join(__dirname, 'acceptance-tests.md'), 'utf-8');
const lines = md.split('\n');

// Parse modules and user stories
const modules = [];
let currentModule = null;
let currentStory = null;
let inTable = false;
let tableRows = [];
let inCriteria = false;
let criteria = [];
let storyMeta = { pre: '', roles: '' };

for (let i = 0; i < lines.length; i++) {
  const line = lines[i];

  // Module header (## Module: ... or ## Modul: ...)
  if (/^## (?:Module?|Modul):?\s+(.+)/i.test(line)) {
    if (currentStory) {
      currentStory.criteria = [...criteria];
      currentStory.tableRows = [...tableRows];
    }
    currentStory = null;
    inTable = false;
    inCriteria = false;
    criteria = [];
    tableRows = [];
    const name = line.replace(/^## (?:Module?|Modul):?\s+/i, '').trim();
    currentModule = { name, stories: [] };
    modules.push(currentModule);
    continue;
  }

  // Also catch "## N. Name" pattern (e.g. "## 1. Auth")
  if (/^## \d+\.\s+(.+)/.test(line) && !currentModule) {
    const name = line.replace(/^## \d+\.\s+/, '').trim();
    currentModule = { name, stories: [] };
    modules.push(currentModule);
    continue;
  }

  // User Story header
  if (/^### (US-\d+):?\s+(.+)/.test(line)) {
    if (currentStory) {
      currentStory.criteria = [...criteria];
      currentStory.tableRows = [...tableRows];
    }
    const match = line.match(/^### (US-\d+):?\s+(.+)/);
    criteria = [];
    tableRows = [];
    inTable = false;
    inCriteria = false;
    storyMeta = { pre: '', roles: '' };
    currentStory = {
      id: match[1],
      title: match[2],
      als: '',
      pre: '',
      tableRows: [],
      criteria: [],
    };
    if (currentModule) currentModule.stories.push(currentStory);
    continue;
  }

  if (!currentStory) continue;

  // "Als" line
  if (/^\*\*Als\*\*/.test(line)) {
    currentStory.als = line;
    // Extract role
    const roleMatch = line.match(/\*\*Als\*\*\s+(\w[\w\s/,]*?)[\s,]*\*\*möchte/i);
    if (roleMatch) currentStory.roles = roleMatch[1].trim();
    continue;
  }

  // Preconditions
  if (/^\*\*Vorbedingungen?:?\*\*/.test(line)) {
    currentStory.pre = line.replace(/^\*\*Vorbedingungen?:?\*\*\s*/, '');
    continue;
  }

  // Table header detection
  if (/^\|\s*#\s*\|/.test(line)) {
    inTable = true;
    inCriteria = false;
    continue;
  }

  // Table separator
  if (/^\|[-\s|]+\|$/.test(line)) continue;

  // Table rows
  if (inTable && /^\|\s*\d+/.test(line)) {
    const cells = line.split('|').filter(c => c.trim()).map(c => c.trim());
    if (cells.length >= 3) {
      tableRows.push({ step: cells[0], action: cells[1], expected: cells[2] });
    }
    continue;
  }

  // End of table
  if (inTable && !/^\|/.test(line)) {
    inTable = false;
  }

  // Acceptance criteria
  if (/^\*\*Akzeptanzkriterien:?\*\*/.test(line)) {
    inCriteria = true;
    continue;
  }

  if (inCriteria && /^- \[[ x]\]/.test(line)) {
    criteria.push(line.replace(/^- \[[ x]\]\s*/, ''));
    continue;
  }

  if (inCriteria && !/^- \[/.test(line) && line.trim() !== '') {
    inCriteria = false;
  }
}

// Flush last story
if (currentStory) {
  currentStory.criteria = [...criteria];
  currentStory.tableRows = [...tableRows];
}

// Count totals
const totalStories = modules.reduce((s, m) => s + m.stories.length, 0);
const totalCriteria = modules.reduce((s, m) => s + m.stories.reduce((s2, st) => s2 + st.criteria.length, 0), 0);

// Generate HTML
let html = `<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>MonteWeb Akzeptanztest-Suite</title>
<style>
:root {
  --primary: #2563eb;
  --success: #16a34a;
  --warn: #d97706;
  --danger: #dc2626;
  --bg: #f8fafc;
  --card: #ffffff;
  --border: #e2e8f0;
  --text: #1e293b;
  --muted: #64748b;
}
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: var(--bg); color: var(--text); line-height: 1.5; }
.container { max-width: 1200px; margin: 0 auto; padding: 1rem; }
header { background: var(--primary); color: white; padding: 1.5rem; margin-bottom: 1.5rem; border-radius: 8px; }
header h1 { font-size: 1.5rem; margin-bottom: 0.25rem; }
header p { opacity: 0.85; font-size: 0.9rem; }
.stats { display: flex; gap: 1rem; margin-top: 1rem; flex-wrap: wrap; }
.stat { background: rgba(255,255,255,0.15); padding: 0.5rem 1rem; border-radius: 6px; }
.stat strong { font-size: 1.25rem; }
.controls { display: flex; gap: 0.75rem; margin-bottom: 1rem; flex-wrap: wrap; align-items: center; }
.controls input { padding: 0.5rem 0.75rem; border: 1px solid var(--border); border-radius: 6px; font-size: 0.9rem; flex: 1; min-width: 200px; }
.controls select { padding: 0.5rem; border: 1px solid var(--border); border-radius: 6px; font-size: 0.9rem; }
.controls button { padding: 0.5rem 1rem; border: 1px solid var(--border); border-radius: 6px; background: var(--card); cursor: pointer; font-size: 0.9rem; }
.controls button:hover { background: #f1f5f9; }
.progress-bar { height: 8px; background: #e2e8f0; border-radius: 4px; overflow: hidden; margin-top: 0.5rem; }
.progress-fill { height: 100%; background: var(--success); transition: width 0.3s; border-radius: 4px; }
.module { background: var(--card); border: 1px solid var(--border); border-radius: 8px; margin-bottom: 1rem; overflow: hidden; }
.module-header { padding: 1rem 1.25rem; cursor: pointer; display: flex; justify-content: space-between; align-items: center; user-select: none; }
.module-header:hover { background: #f8fafc; }
.module-header h2 { font-size: 1.1rem; display: flex; align-items: center; gap: 0.5rem; }
.module-header .badge { font-size: 0.75rem; background: #e2e8f0; padding: 0.125rem 0.5rem; border-radius: 99px; font-weight: normal; color: var(--muted); }
.module-header .progress-info { font-size: 0.85rem; color: var(--muted); min-width: 80px; text-align: right; }
.module-body { display: none; border-top: 1px solid var(--border); }
.module.open .module-body { display: block; }
.story { padding: 1rem 1.25rem; border-bottom: 1px solid var(--border); }
.story:last-child { border-bottom: none; }
.story-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.5rem; }
.story-id { font-weight: 700; color: var(--primary); font-size: 0.85rem; }
.story-title { font-weight: 600; font-size: 1rem; }
.story-als { font-size: 0.85rem; color: var(--muted); margin-bottom: 0.75rem; font-style: italic; }
.story-pre { font-size: 0.85rem; color: var(--muted); margin-bottom: 0.5rem; }
.story-pre strong { color: var(--text); }
table { width: 100%; border-collapse: collapse; font-size: 0.85rem; margin: 0.5rem 0; }
th { background: #f1f5f9; padding: 0.5rem; text-align: left; border: 1px solid var(--border); font-weight: 600; }
td { padding: 0.5rem; border: 1px solid var(--border); vertical-align: top; }
td:first-child { width: 30px; text-align: center; }
td:last-child { width: 40px; text-align: center; }
.criteria { margin-top: 0.75rem; }
.criteria h4 { font-size: 0.85rem; margin-bottom: 0.25rem; color: var(--muted); }
.criteria label { display: flex; align-items: flex-start; gap: 0.5rem; font-size: 0.85rem; padding: 0.25rem 0; cursor: pointer; }
.criteria input[type=checkbox] { margin-top: 3px; flex-shrink: 0; width: 16px; height: 16px; }
.criteria label.checked { text-decoration: line-through; color: var(--muted); }
.story.completed { background: #f0fdf4; }
.story.completed .story-id { color: var(--success); }
.chevron { transition: transform 0.2s; font-size: 0.75rem; }
.module.open .chevron { transform: rotate(90deg); }
.role-tag { font-size: 0.7rem; padding: 0.1rem 0.4rem; border-radius: 3px; font-weight: 600; margin-left: 0.25rem; }
.role-SA { background: #fee2e2; color: #991b1b; }
.role-T { background: #dbeafe; color: #1e40af; }
.role-P { background: #dcfce7; color: #166534; }
.role-S { background: #f1f5f9; color: #475569; }
.role-SECADMIN { background: #fef3c7; color: #92400e; }
@media print {
  .controls, .chevron, header .stats { display: none; }
  .module-body { display: block !important; }
  .module { break-inside: avoid; }
  .story { break-inside: avoid; }
  body { font-size: 10pt; }
}
@media (max-width: 768px) {
  .stats { flex-direction: column; }
  .controls { flex-direction: column; }
  .controls input { min-width: 100%; }
}
</style>
</head>
<body>
<div class="container">
<header>
  <h1>MonteWeb Akzeptanztest-Suite</h1>
  <p>Erstellt: 2026-03-02 | ${modules.length} Module | ${totalStories} User Stories | ${totalCriteria} Akzeptanzkriterien</p>
  <div class="stats">
    <div class="stat"><strong id="total-done">0</strong> / ${totalCriteria} Kriterien</div>
    <div class="stat"><strong id="stories-done">0</strong> / ${totalStories} Stories</div>
    <div class="stat"><strong id="percent-done">0</strong>% abgeschlossen</div>
  </div>
  <div class="progress-bar" style="margin-top:0.75rem;height:10px"><div class="progress-fill" id="global-progress" style="width:0%"></div></div>
</header>

<div class="controls">
  <input type="text" id="search" placeholder="Suche nach User Story, Modul oder Schlagwort..." />
  <select id="role-filter">
    <option value="">Alle Rollen</option>
    <option value="SUPERADMIN">SUPERADMIN</option>
    <option value="SECTION_ADMIN">SECTION_ADMIN</option>
    <option value="TEACHER">TEACHER</option>
    <option value="PARENT">PARENT</option>
    <option value="STUDENT">STUDENT</option>
  </select>
  <select id="status-filter">
    <option value="">Alle Status</option>
    <option value="open">Offen</option>
    <option value="done">Erledigt</option>
  </select>
  <button onclick="resetAll()">Alles zurücksetzen</button>
</div>

<div id="modules">
`;

for (const mod of modules) {
  const modId = mod.name.replace(/[^a-zA-Z0-9]/g, '_');
  html += `<div class="module" data-module="${modId}">
  <div class="module-header" onclick="toggleModule('${modId}')">
    <h2><span class="chevron">&#9654;</span> ${escHtml(mod.name)} <span class="badge">${mod.stories.length} Stories</span></h2>
    <div>
      <span class="progress-info" id="prog-${modId}">0/${mod.stories.reduce((s, st) => s + st.criteria.length, 0)}</span>
      <div class="progress-bar" style="width:120px"><div class="progress-fill" id="bar-${modId}" style="width:0%"></div></div>
    </div>
  </div>
  <div class="module-body">
`;

  for (const story of mod.stories) {
    const storyId = story.id.replace('-', '_');
    html += `    <div class="story" id="story-${storyId}" data-story="${story.id}" data-roles="${escHtml(story.roles || '')}">
      <div class="story-header">
        <div><span class="story-id">${escHtml(story.id)}</span> <span class="story-title">${escHtml(story.title)}</span></div>
      </div>
`;
    if (story.als) {
      html += `      <div class="story-als">${escHtml(story.als).replace(/\*\*/g, '')}</div>\n`;
    }
    if (story.pre) {
      html += `      <div class="story-pre"><strong>Vorbedingungen:</strong> ${escHtml(story.pre)}</div>\n`;
    }

    if (story.tableRows.length > 0) {
      html += `      <table>
        <thead><tr><th>#</th><th>Testschritt</th><th>Erwartetes Ergebnis</th><th>OK</th></tr></thead>
        <tbody>
`;
      for (const row of story.tableRows) {
        const cbId = `step_${storyId}_${row.step}`;
        html += `          <tr><td>${escHtml(row.step)}</td><td>${escHtml(row.action)}</td><td>${escHtml(row.expected)}</td><td><input type="checkbox" data-cb="${cbId}" onchange="onCheck()"></td></tr>\n`;
      }
      html += `        </tbody></table>\n`;
    }

    if (story.criteria.length > 0) {
      html += `      <div class="criteria"><h4>Akzeptanzkriterien:</h4>\n`;
      story.criteria.forEach((c, ci) => {
        const cbId = `crit_${storyId}_${ci}`;
        html += `        <label><input type="checkbox" data-cb="${cbId}" data-crit="1" data-module="${modId}" onchange="onCheck()"> ${escHtml(c)}</label>\n`;
      });
      html += `      </div>\n`;
    }

    html += `    </div>\n`;
  }

  html += `  </div>\n</div>\n`;
}

html += `</div>
</div>

<script>
const STORAGE_KEY = 'monteweb-acceptance-tests';

function loadState() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}'); } catch { return {}; }
}

function saveState(state) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function initCheckboxes() {
  const state = loadState();
  document.querySelectorAll('input[data-cb]').forEach(cb => {
    cb.checked = !!state[cb.dataset.cb];
    updateLabel(cb);
  });
  updateProgress();
}

function updateLabel(cb) {
  const label = cb.closest('label');
  if (label) label.classList.toggle('checked', cb.checked);
}

function onCheck() {
  const state = loadState();
  document.querySelectorAll('input[data-cb]').forEach(cb => {
    state[cb.dataset.cb] = cb.checked;
    updateLabel(cb);
  });
  saveState(state);
  updateProgress();
}

function updateProgress() {
  // Per module
  document.querySelectorAll('.module').forEach(mod => {
    const modId = mod.dataset.module;
    const crits = mod.querySelectorAll('input[data-crit]');
    const done = [...crits].filter(c => c.checked).length;
    const total = crits.length;
    const el = document.getElementById('prog-' + modId);
    const bar = document.getElementById('bar-' + modId);
    if (el) el.textContent = done + '/' + total;
    if (bar) bar.style.width = (total ? (done/total*100) : 0) + '%';
  });

  // Stories completion
  document.querySelectorAll('.story').forEach(s => {
    const allCb = s.querySelectorAll('input[data-cb]');
    const allChecked = allCb.length > 0 && [...allCb].every(c => c.checked);
    s.classList.toggle('completed', allChecked);
  });

  // Global
  const allCrits = document.querySelectorAll('input[data-crit]');
  const totalDone = [...allCrits].filter(c => c.checked).length;
  const totalAll = allCrits.length;
  document.getElementById('total-done').textContent = totalDone;
  document.getElementById('global-progress').style.width = (totalAll ? (totalDone/totalAll*100) : 0) + '%';
  document.getElementById('percent-done').textContent = totalAll ? Math.round(totalDone/totalAll*100) : 0;

  const allStories = document.querySelectorAll('.story');
  const doneStories = [...allStories].filter(s => {
    const cbs = s.querySelectorAll('input[data-cb]');
    return cbs.length > 0 && [...cbs].every(c => c.checked);
  }).length;
  document.getElementById('stories-done').textContent = doneStories;
}

function toggleModule(modId) {
  const el = document.querySelector('[data-module="' + modId + '"]');
  el.classList.toggle('open');
}

function resetAll() {
  if (!confirm('Wirklich alle Checkboxen zurücksetzen?')) return;
  localStorage.removeItem(STORAGE_KEY);
  document.querySelectorAll('input[data-cb]').forEach(cb => { cb.checked = false; updateLabel(cb); });
  updateProgress();
}

// Search
document.getElementById('search').addEventListener('input', applyFilters);
document.getElementById('role-filter').addEventListener('change', applyFilters);
document.getElementById('status-filter').addEventListener('change', applyFilters);

function applyFilters() {
  const q = document.getElementById('search').value.toLowerCase();
  const role = document.getElementById('role-filter').value;
  const status = document.getElementById('status-filter').value;

  document.querySelectorAll('.story').forEach(s => {
    const text = s.textContent.toLowerCase();
    const roles = (s.dataset.roles || '').toUpperCase();
    const matchQ = !q || text.includes(q);
    const matchRole = !role || roles.includes(role);
    const isComplete = s.classList.contains('completed');
    const matchStatus = !status || (status === 'done' && isComplete) || (status === 'open' && !isComplete);
    s.style.display = (matchQ && matchRole && matchStatus) ? '' : 'none';
  });

  // Show/hide modules
  document.querySelectorAll('.module').forEach(mod => {
    const visible = [...mod.querySelectorAll('.story')].some(s => s.style.display !== 'none');
    mod.style.display = visible ? '' : 'none';
    if (q || role || status) mod.classList.add('open');
  });
}

initCheckboxes();
</script>
</body>
</html>`;

fs.writeFileSync(path.join(__dirname, 'acceptance-tests.html'), html, 'utf-8');
console.log(`Generated: acceptance-tests.html`);
console.log(`Modules: ${modules.length}`);
console.log(`User Stories: ${totalStories}`);
console.log(`Acceptance Criteria: ${totalCriteria}`);

function escHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
