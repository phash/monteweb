# Elternbrief v2 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enhance the parent letters module with markdown editor + live preview, PDF export (letter + tracking), file attachments, and inline reporting statistics.

**Architecture:** Four independent features layered onto the existing `parentletter` module. The markdown editor extracts the existing `renderMarkdown()` function from `RoomWiki.vue` into a shared composable. PDF export uses the existing `PdfService` (openhtmltopdf) with `commonmark-java` for server-side markdown rendering. Attachments follow the feed module's upload pattern. Reporting adds a stats endpoint and summary UI to the existing list view.

**Tech Stack:** Vue 3 + PrimeVue 4, DOMPurify (existing), openhtmltopdf-pdfbox (existing), commonmark-java (new Maven dep), MinIO (existing)

---

## Task 1: Extract renderMarkdown to shared composable

**Files:**
- Create: `frontend/src/composables/useMarkdown.ts`
- Modify: `frontend/src/components/rooms/RoomWiki.vue` — replace inline `renderMarkdown()` with composable import
- Test: `frontend/src/composables/__tests__/useMarkdown.test.ts`

**Step 1: Write the failing test**

```typescript
// frontend/src/composables/__tests__/useMarkdown.test.ts
import { describe, it, expect } from 'vitest'
import { useMarkdown } from '@/composables/useMarkdown'

describe('useMarkdown', () => {
  const { renderMarkdown } = useMarkdown()

  it('renders headings', () => {
    expect(renderMarkdown('# Hello')).toContain('<h1>Hello</h1>')
  })

  it('renders bold text', () => {
    expect(renderMarkdown('**bold**')).toContain('<strong>bold</strong>')
  })

  it('renders italic text', () => {
    expect(renderMarkdown('*italic*')).toContain('<em>italic</em>')
  })

  it('renders links', () => {
    expect(renderMarkdown('[link](http://example.com)')).toContain('<a href="http://example.com"')
  })

  it('renders unordered lists', () => {
    expect(renderMarkdown('- item 1\n- item 2')).toContain('<li>item 1</li>')
  })

  it('renders code blocks', () => {
    expect(renderMarkdown('`code`')).toContain('<code>code</code>')
  })

  it('sanitizes HTML output', () => {
    expect(renderMarkdown('<script>alert(1)</script>')).not.toContain('<script>')
  })

  it('resolves letter variables with sample data', () => {
    const { resolveVariablesPreview } = useMarkdown()
    const result = resolveVariablesPreview('Liebe {Anrede}, Ihr Kind {NameKind} aus {Familie}. Grüße, {LehrerName}', 'Max Mustermann')
    expect(result).toContain('Familie Müller')
    expect(result).toContain('Max')
    expect(result).toContain('Max Mustermann')
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd frontend && npx vitest run src/composables/__tests__/useMarkdown.test.ts`
Expected: FAIL — module not found

**Step 3: Write the composable**

```typescript
// frontend/src/composables/useMarkdown.ts
import { sanitizeHtml } from '@/utils/sanitize'

export function useMarkdown() {
  function renderMarkdown(md: string): string {
    if (!md) return ''
    let html = md
    // Code blocks (``` ... ```)
    html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    // Inline code
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
    // Headings
    html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
    html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
    html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')
    // Bold
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    // Italic
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
    // Links
    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
    // Blockquotes
    html = html.replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>')
    // Unordered lists
    html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
    html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')
    // Horizontal rules
    html = html.replace(/^---$/gm, '<hr>')
    // Paragraphs (double newlines)
    html = html.replace(/\n\n/g, '</p><p>')
    html = '<p>' + html + '</p>'
    // Single newlines → <br>
    html = html.replace(/\n/g, '<br>')
    // Cleanup
    html = html.replace(/<p><\/p>/g, '')
    html = html.replace(/<p>(<h[1-6]>)/g, '$1')
    html = html.replace(/(<\/h[1-6]>)<\/p>/g, '$1')
    html = html.replace(/<p>(<ul>)/g, '$1')
    html = html.replace(/(<\/ul>)<\/p>/g, '$1')
    html = html.replace(/<p>(<blockquote>)/g, '$1')
    html = html.replace(/(<\/blockquote>)<\/p>/g, '$1')
    html = html.replace(/<p>(<hr>)<\/p>/g, '$1')
    html = html.replace(/<p>(<pre>)/g, '$1')
    html = html.replace(/(<\/pre>)<\/p>/g, '$1')
    return sanitizeHtml(html)
  }

  function resolveVariablesPreview(content: string, currentUserName: string): string {
    return content
      .replace(/\{Familie\}/g, 'Familie Müller')
      .replace(/\{NameKind\}/g, 'Max')
      .replace(/\{Anrede\}/g, 'Sehr geehrte Frau Müller')
      .replace(/\{LehrerName\}/g, currentUserName)
  }

  return { renderMarkdown, resolveVariablesPreview }
}
```

**Step 4: Run test to verify it passes**

Run: `cd frontend && npx vitest run src/composables/__tests__/useMarkdown.test.ts`
Expected: PASS

**Step 5: Update RoomWiki.vue to use the composable**

Replace the inline `renderMarkdown()` function (lines 298–365 of `frontend/src/components/rooms/RoomWiki.vue`) with:
```typescript
import { useMarkdown } from '@/composables/useMarkdown'
const { renderMarkdown } = useMarkdown()
```

**Step 6: Run all tests**

Run: `cd frontend && npx vitest run`
Expected: All tests pass

**Step 7: Commit**

```bash
git add frontend/src/composables/useMarkdown.ts frontend/src/composables/__tests__/useMarkdown.test.ts frontend/src/components/rooms/RoomWiki.vue
git commit -m "refactor: extract renderMarkdown to shared useMarkdown composable"
```

---

## Task 2: Markdown editor with live preview in ParentLetterCreateView

**Files:**
- Create: `frontend/src/components/parentletter/MarkdownLetterEditor.vue`
- Modify: `frontend/src/views/ParentLetterCreateView.vue` — replace Textarea with MarkdownLetterEditor
- Test: `frontend/src/components/parentletter/__tests__/MarkdownLetterEditor.test.ts`

**Step 1: Write the failing test**

```typescript
// frontend/src/components/parentletter/__tests__/MarkdownLetterEditor.test.ts
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import MarkdownLetterEditor from '../MarkdownLetterEditor.vue'

describe('MarkdownLetterEditor', () => {
  it('renders textarea and preview panes', () => {
    const wrapper = mount(MarkdownLetterEditor, {
      props: { modelValue: '# Test', userName: 'Lehrer' },
    })
    expect(wrapper.find('.editor-pane').exists()).toBe(true)
    expect(wrapper.find('.preview-pane').exists()).toBe(true)
  })

  it('renders markdown in preview', () => {
    const wrapper = mount(MarkdownLetterEditor, {
      props: { modelValue: '**bold**', userName: 'Lehrer' },
    })
    expect(wrapper.find('.preview-pane').html()).toContain('<strong>bold</strong>')
  })

  it('resolves variables in preview', () => {
    const wrapper = mount(MarkdownLetterEditor, {
      props: { modelValue: 'Hallo {NameKind}', userName: 'Herr Schmidt' },
    })
    expect(wrapper.find('.preview-pane').html()).toContain('Max')
  })

  it('emits update:modelValue on input', async () => {
    const wrapper = mount(MarkdownLetterEditor, {
      props: { modelValue: '', userName: 'Lehrer' },
    })
    await wrapper.find('textarea').setValue('new content')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['new content'])
  })

  it('exposes insertAtCursor method', () => {
    const wrapper = mount(MarkdownLetterEditor, {
      props: { modelValue: 'hello', userName: 'Lehrer' },
    })
    expect(typeof wrapper.vm.insertAtCursor).toBe('function')
  })
})
```

**Step 2: Run test → FAIL**

**Step 3: Create MarkdownLetterEditor component**

```vue
<!-- frontend/src/components/parentletter/MarkdownLetterEditor.vue -->
<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMarkdown } from '@/composables/useMarkdown'

const props = defineProps<{
  modelValue: string
  userName: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t } = useI18n()
const { renderMarkdown, resolveVariablesPreview } = useMarkdown()
const textareaRef = ref<HTMLTextAreaElement | null>(null)

const previewHtml = computed(() => {
  const resolved = resolveVariablesPreview(props.modelValue, props.userName)
  return renderMarkdown(resolved)
})

function onInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
}

function insertAtCursor(text: string) {
  const textarea = textareaRef.value
  if (!textarea) return
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const before = props.modelValue.substring(0, start)
  const after = props.modelValue.substring(end)
  emit('update:modelValue', before + text + after)
  requestAnimationFrame(() => {
    textarea.selectionStart = textarea.selectionEnd = start + text.length
    textarea.focus()
  })
}

defineExpose({ insertAtCursor, textareaRef })
</script>

<template>
  <div class="markdown-editor">
    <div class="editor-toolbar">
      <span class="toolbar-label">{{ t('parentLetters.content') }}</span>
      <span class="toolbar-preview-label">{{ t('parentLetters.variables.title') }}</span>
    </div>
    <div class="editor-split">
      <div class="editor-pane">
        <textarea
          ref="textareaRef"
          :value="modelValue"
          :placeholder="placeholder"
          class="editor-textarea"
          rows="15"
          @input="onInput"
        />
      </div>
      <div class="preview-pane" v-html="previewHtml" />
    </div>
  </div>
</template>

<style scoped>
.markdown-editor { border: 1px solid var(--mw-border-light); border-radius: 6px; overflow: hidden; }
.editor-toolbar { display: flex; justify-content: space-between; padding: 0.5rem 1rem; background: var(--mw-bg-subtle); border-bottom: 1px solid var(--mw-border-light); font-size: var(--mw-font-size-sm); color: var(--mw-text-secondary); }
.editor-split { display: grid; grid-template-columns: 1fr 1fr; min-height: 300px; }
.editor-pane { border-right: 1px solid var(--mw-border-light); }
.editor-textarea { width: 100%; height: 100%; border: none; outline: none; resize: none; padding: 1rem; font-family: monospace; font-size: var(--mw-font-size-sm); background: var(--mw-bg); color: var(--mw-text); }
.preview-pane { padding: 1rem; overflow-y: auto; max-height: 500px; line-height: 1.6; }
.preview-pane :deep(h1) { font-size: 1.5rem; margin: 0 0 0.5rem; }
.preview-pane :deep(h2) { font-size: 1.25rem; margin: 0 0 0.5rem; }
.preview-pane :deep(h3) { font-size: 1.1rem; margin: 0 0 0.5rem; }
.preview-pane :deep(p) { margin: 0 0 0.75rem; }
.preview-pane :deep(ul) { padding-left: 1.5rem; }
.preview-pane :deep(blockquote) { border-left: 3px solid var(--mw-primary); padding-left: 1rem; color: var(--mw-text-secondary); }

@media (max-width: 768px) {
  .editor-split { grid-template-columns: 1fr; }
  .editor-pane { border-right: none; border-bottom: 1px solid var(--mw-border-light); }
}
</style>
```

**Step 4: Modify ParentLetterCreateView.vue**

Replace the Textarea section (around lines 268–282) with:
```vue
<MarkdownLetterEditor
  ref="editorRef"
  v-model="content"
  :user-name="auth.user?.displayName ?? ''"
  :placeholder="t('parentLetters.contentPlaceholder')"
/>
<VariableHelpMenu @insert="(v: string) => editorRef?.insertAtCursor(v)" />
```

Add imports:
```typescript
import MarkdownLetterEditor from '@/components/parentletter/MarkdownLetterEditor.vue'
const editorRef = ref<InstanceType<typeof MarkdownLetterEditor> | null>(null)
```

Remove the old `contentTextareaRef` and `insertVariable` function.

**Step 5: Update ParentLetterDetailView.vue content display**

Replace `{{ letter.content }}` (line 196) with rendered markdown:
```vue
<div class="content-text" v-html="renderedContent" />
```

Add to script:
```typescript
import { useMarkdown } from '@/composables/useMarkdown'
const { renderMarkdown } = useMarkdown()
const renderedContent = computed(() => renderMarkdown(letter.value?.content ?? ''))
```

**Step 6: Run tests → PASS**

**Step 7: Commit**

```bash
git add frontend/src/components/parentletter/MarkdownLetterEditor.vue frontend/src/components/parentletter/__tests__/MarkdownLetterEditor.test.ts frontend/src/views/ParentLetterCreateView.vue frontend/src/views/ParentLetterDetailView.vue
git commit -m "feat(parentletter): add markdown editor with live preview"
```

---

## Task 3: Add commonmark-java dependency and ParentLetterPdfService

**Files:**
- Modify: `backend/pom.xml` — add commonmark-java dependency
- Create: `backend/src/main/java/com/monteweb/parentletter/internal/service/ParentLetterPdfService.java`
- Modify: `backend/src/main/java/com/monteweb/parentletter/internal/controller/ParentLetterController.java` — add PDF endpoints

**Step 1: Add Maven dependency**

Add to `backend/pom.xml` in the `<dependencies>` section:
```xml
<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark</artifactId>
    <version>0.24.0</version>
</dependency>
```

**Step 2: Create ParentLetterPdfService**

```java
// backend/src/main/java/com/monteweb/parentletter/internal/service/ParentLetterPdfService.java
package com.monteweb.parentletter.internal.service;

import com.monteweb.parentletter.ParentLetterDetailInfo;
import com.monteweb.parentletter.ParentLetterRecipientInfo;
import com.monteweb.parentletter.RecipientStatus;
import com.monteweb.shared.util.PdfService;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "parentletter.enabled", havingValue = "true")
public class ParentLetterPdfService {

    private final PdfService pdfService;
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Berlin"));

    public ParentLetterPdfService(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    public byte[] generateLetterPdf(ParentLetterDetailInfo letter, String resolvedContent, String letterheadHtml) {
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
        sb.append("<style>body{font-family:sans-serif;font-size:12pt;margin:2cm;line-height:1.6}");
        sb.append(".letterhead{text-align:center;margin-bottom:1cm}");
        sb.append(".meta{margin-bottom:1cm;color:#666;font-size:10pt}");
        sb.append("h1{font-size:16pt}h2{font-size:14pt}h3{font-size:12pt}");
        sb.append("table{width:100%;border-collapse:collapse}th,td{border:1px solid #ccc;padding:4pt 8pt;text-align:left}th{background:#f5f5f5}");
        sb.append("</style></head><body>");

        if (letterheadHtml != null && !letterheadHtml.isEmpty()) {
            sb.append("<div class=\"letterhead\">").append(letterheadHtml).append("</div>");
        }

        sb.append("<div class=\"meta\">");
        sb.append("<div>").append(PdfService.escapeXml(letter.roomName())).append("</div>");
        sb.append("<div>").append(PdfService.escapeXml(letter.creatorName())).append("</div>");
        if (letter.sendDate() != null) {
            sb.append("<div>Datum: ").append(DATE_FMT.format(letter.sendDate())).append("</div>");
        }
        sb.append("</div>");

        sb.append("<h1>").append(PdfService.escapeXml(letter.title())).append("</h1>");

        // Render markdown to HTML
        Node doc = markdownParser.parse(resolvedContent);
        sb.append(htmlRenderer.render(doc));

        sb.append("</body></html>");
        return pdfService.renderHtmlToPdf(sb.toString());
    }

    public byte[] generateTrackingPdf(ParentLetterDetailInfo letter) {
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
        sb.append("<style>body{font-family:sans-serif;font-size:10pt;margin:2cm}");
        sb.append("h1{font-size:14pt}table{width:100%;border-collapse:collapse;margin-top:1cm}");
        sb.append("th,td{border:1px solid #ccc;padding:4pt 8pt;text-align:left}th{background:#f0f0f0;font-weight:bold}");
        sb.append(".summary{margin-top:1cm;padding:8pt;background:#f9f9f9;border:1px solid #ddd}");
        sb.append(".confirmed{color:#22c55e}.open{color:#ef4444}.read{color:#3b82f6}");
        sb.append("</style></head><body>");

        sb.append("<h1>Rücklauf: ").append(PdfService.escapeXml(letter.title())).append("</h1>");
        sb.append("<div>Raum: ").append(PdfService.escapeXml(letter.roomName())).append("</div>");
        sb.append("<div>Erstellt von: ").append(PdfService.escapeXml(letter.creatorName())).append("</div>");
        if (letter.deadline() != null) {
            sb.append("<div>Frist: ").append(DATE_FMT.format(letter.deadline())).append("</div>");
        }

        sb.append("<table><thead><tr>");
        sb.append("<th>Schüler/in</th><th>Elternteil</th><th>Familie</th><th>Status</th><th>Bestätigt am</th>");
        sb.append("</tr></thead><tbody>");

        for (var r : letter.recipients()) {
            sb.append("<tr>");
            sb.append("<td>").append(PdfService.escapeXml(r.studentName())).append("</td>");
            sb.append("<td>").append(PdfService.escapeXml(r.parentName())).append("</td>");
            sb.append("<td>").append(PdfService.escapeXml(r.familyName())).append("</td>");
            String statusClass = switch (r.status()) {
                case CONFIRMED -> "confirmed";
                case READ -> "read";
                case OPEN -> "open";
            };
            String statusLabel = switch (r.status()) {
                case CONFIRMED -> "Bestätigt";
                case READ -> "Gelesen";
                case OPEN -> "Offen";
            };
            sb.append("<td class=\"").append(statusClass).append("\">").append(statusLabel).append("</td>");
            sb.append("<td>").append(r.confirmedAt() != null ? DATE_FMT.format(r.confirmedAt()) : "-").append("</td>");
            sb.append("</tr>");
        }

        sb.append("</tbody></table>");

        long confirmed = letter.recipients().stream().filter(r -> r.status() == RecipientStatus.CONFIRMED).count();
        int total = letter.totalRecipients();
        double rate = total > 0 ? (confirmed * 100.0 / total) : 0;

        sb.append("<div class=\"summary\">");
        sb.append("<strong>").append(confirmed).append(" von ").append(total).append(" bestätigt</strong>");
        sb.append(" (Rücklaufquote: ").append(String.format("%.0f", rate)).append("%)");
        sb.append("</div>");

        sb.append("</body></html>");
        return pdfService.renderHtmlToPdf(sb.toString());
    }
}
```

**Step 3: Add PDF endpoints to ParentLetterController**

Add to `ParentLetterController.java`:
```java
private final ParentLetterPdfService pdfService;
// add to constructor

@GetMapping("/{id}/pdf")
public ResponseEntity<byte[]> downloadLetterPdf(@PathVariable UUID id,
        @RequestParam(required = false) UUID studentId) {
    UUID userId = SecurityUtils.requireCurrentUserId();
    var detail = parentLetterService.getLetterDetail(id, userId);
    String resolved = parentLetterService.getResolvedContent(id, studentId, userId);
    String letterheadHtml = parentLetterService.getLetterheadHtml(detail.roomId());
    byte[] pdf = pdfService.generateLetterPdf(detail, resolved, letterheadHtml);
    return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "attachment; filename=\"Elternbrief-" + detail.title().replaceAll("[^a-zA-Z0-9äöüÄÖÜß\\-]", "_") + ".pdf\"")
            .body(pdf);
}

@GetMapping("/{id}/tracking-pdf")
public ResponseEntity<byte[]> downloadTrackingPdf(@PathVariable UUID id) {
    UUID userId = SecurityUtils.requireCurrentUserId();
    var detail = parentLetterService.getLetterDetail(id, userId);
    byte[] pdf = pdfService.generateTrackingPdf(detail);
    return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "attachment; filename=\"Ruecklauf-" + detail.title().replaceAll("[^a-zA-Z0-9äöüÄÖÜß\\-]", "_") + ".pdf\"")
            .body(pdf);
}
```

**Step 4: Add helper methods to ParentLetterService**

Add `getResolvedContent(UUID letterId, UUID studentId, UUID userId)` — resolves variables for a specific student or returns raw content if no studentId. Add `getLetterheadHtml(UUID roomId)` — loads config, returns `<img>` tag if letterhead exists.

**Step 5: Add frontend PDF download buttons**

In `frontend/src/api/parentletter.api.ts` add:
```typescript
downloadLetterPdf(id: string, studentId?: string) {
  const params = studentId ? { studentId } : {}
  return client.get(`/parent-letters/${id}/pdf`, { responseType: 'blob', params })
},
downloadTrackingPdf(id: string) {
  return client.get(`/parent-letters/${id}/tracking-pdf`, { responseType: 'blob' })
},
```

In `frontend/src/views/ParentLetterDetailView.vue` add download buttons in the management-actions section (after line ~180):
```vue
<Button
  :label="t('parentLetters.downloadPdf')"
  icon="pi pi-file-pdf"
  severity="secondary"
  @click="downloadPdf"
/>
<Button
  :label="t('parentLetters.downloadTracking')"
  icon="pi pi-list"
  severity="secondary"
  @click="downloadTracking"
/>
```

Add i18n keys: `downloadPdf: 'Brief als PDF'`, `downloadTracking: 'Rücklauf-Liste als PDF'`

**Step 6: Docker build to verify backend compiles**

Run: `cd /e/claude/montessori && docker compose build backend 2>&1 | grep -E "BUILD SUCCESS|ERROR"`
Expected: BUILD SUCCESS

**Step 7: Commit**

```bash
git add backend/pom.xml backend/src/main/java/com/monteweb/parentletter/internal/service/ParentLetterPdfService.java backend/src/main/java/com/monteweb/parentletter/internal/controller/ParentLetterController.java backend/src/main/java/com/monteweb/parentletter/internal/service/ParentLetterService.java frontend/src/api/parentletter.api.ts frontend/src/views/ParentLetterDetailView.vue frontend/src/i18n/de.ts frontend/src/i18n/en.ts
git commit -m "feat(parentletter): add PDF export for letters and tracking"
```

---

## Task 4: File attachments — database migration and backend

**Files:**
- Create: `backend/src/main/resources/db/migration/V105__parent_letter_attachments.sql`
- Create: `backend/src/main/java/com/monteweb/parentletter/ParentLetterAttachmentInfo.java`
- Create: `backend/src/main/java/com/monteweb/parentletter/internal/model/ParentLetterAttachment.java`
- Create: `backend/src/main/java/com/monteweb/parentletter/internal/repository/ParentLetterAttachmentRepository.java`
- Modify: `backend/src/main/java/com/monteweb/parentletter/internal/service/ParentLetterService.java`
- Modify: `backend/src/main/java/com/monteweb/parentletter/internal/service/ParentLetterStorageService.java`
- Modify: `backend/src/main/java/com/monteweb/parentletter/internal/controller/ParentLetterController.java`

**Step 1: Create migration V105**

```sql
-- V105: Parent letter file attachments
CREATE TABLE parent_letter_attachments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    letter_id         UUID NOT NULL REFERENCES parent_letters(id) ON DELETE CASCADE,
    original_filename VARCHAR(500) NOT NULL,
    storage_path      VARCHAR(500) NOT NULL,
    file_size         BIGINT NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    uploaded_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    sort_order        INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_pla_letter_id ON parent_letter_attachments(letter_id);
```

**Step 2: Create entity, repository, DTO**

Follow the feed attachment pattern exactly:
- `ParentLetterAttachment.java` — JPA entity with ManyToOne to ParentLetter
- `ParentLetterAttachmentRepository.java` — extends JpaRepository, `findByLetterIdOrderBySortOrder`, `countByLetterId`
- `ParentLetterAttachmentInfo.java` — public record DTO

**Step 3: Add storage methods to ParentLetterStorageService**

Add `uploadAttachment(UUID letterId, UUID attachmentId, MultipartFile file, String contentType)` returning MinIO object key `parentletter/{letterId}/attachments/{attachmentId}.{ext}`. Add `downloadAttachment(String path)` and `deleteAttachment(String path)`.

**Step 4: Add attachment service methods to ParentLetterService**

- `uploadAttachments(UUID letterId, List<MultipartFile> files, UUID userId)` — max 5 attachments, ClamAV scan if available, content-type via magic bytes
- `downloadAttachment(UUID attachmentId, UUID userId)` — verify access
- `deleteAttachment(UUID attachmentId, UUID userId)` — only DRAFT letters

**Step 5: Add controller endpoints**

```java
@PostMapping("/{id}/attachments")
// multipart upload, returns list of ParentLetterAttachmentInfo

@GetMapping("/{id}/attachments/{attachmentId}")
// stream download with correct Content-Type and Content-Disposition

@DeleteMapping("/{id}/attachments/{attachmentId}")
// delete (DRAFT only)
```

**Step 6: Docker build → BUILD SUCCESS**

**Step 7: Commit**

```bash
git commit -m "feat(parentletter): add file attachment support with MinIO storage"
```

---

## Task 5: File attachments — frontend

**Files:**
- Modify: `frontend/src/types/parentletter.ts` — add ParentLetterAttachmentInfo
- Modify: `frontend/src/api/parentletter.api.ts` — add attachment endpoints
- Modify: `frontend/src/views/ParentLetterCreateView.vue` — add file upload after save
- Modify: `frontend/src/views/ParentLetterDetailView.vue` — show attachments with download
- Add i18n keys for attachments

**Step 1: Add TypeScript types**

```typescript
// in frontend/src/types/parentletter.ts
export interface ParentLetterAttachmentInfo {
  id: string
  originalFilename: string
  storagePath: string
  fileSize: number
  contentType: string
  sortOrder: number
  createdAt: string
}
```

Add `attachments: ParentLetterAttachmentInfo[]` to `ParentLetterDetailInfo`.

**Step 2: Add API methods**

```typescript
uploadAttachments(letterId: string, files: File[]) {
  const formData = new FormData()
  files.forEach(f => formData.append('files', f))
  return client.post<ApiResponse<ParentLetterAttachmentInfo[]>>(`/parent-letters/${letterId}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
},
getAttachmentDownloadUrl(letterId: string, attachmentId: string) {
  return `/api/v1/parent-letters/${letterId}/attachments/${attachmentId}`
},
deleteAttachment(letterId: string, attachmentId: string) {
  return client.delete<ApiResponse<void>>(`/parent-letters/${letterId}/attachments/${attachmentId}`)
},
```

**Step 3: Add file upload to ParentLetterCreateView**

After saving a DRAFT letter, show a file upload section:
- PrimeVue `FileUpload` component (mode="advanced", multiple, accept any)
- Max 5 files, max 10 MB each
- Upload button calls `parentLetterApi.uploadAttachments(letterId, files)`

**Step 4: Show attachments in ParentLetterDetailView**

List attachments with filename, size, download icon. Same pattern as `FeedPost.vue` attachment display.

**Step 5: Run all tests, TypeScript check**

**Step 6: Commit**

```bash
git commit -m "feat(parentletter): add file attachment UI with upload and download"
```

---

## Task 6: Inline reporting statistics

**Files:**
- Create: `backend/src/main/java/com/monteweb/parentletter/ParentLetterStatsInfo.java`
- Modify: `backend/src/main/java/com/monteweb/parentletter/internal/service/ParentLetterService.java` — add stats method
- Modify: `backend/src/main/java/com/monteweb/parentletter/internal/controller/ParentLetterController.java` — add stats endpoint
- Modify: `frontend/src/types/parentletter.ts` — add stats type
- Modify: `frontend/src/api/parentletter.api.ts` — add stats endpoint
- Modify: `frontend/src/stores/parentletter.ts` — add stats state/action
- Modify: `frontend/src/views/ParentLettersView.vue` — add summary bar and enhanced progress

**Step 1: Create stats DTO**

```java
// backend/src/main/java/com/monteweb/parentletter/ParentLetterStatsInfo.java
package com.monteweb.parentletter;

public record ParentLetterStatsInfo(
    int activeCount,
    int totalRecipients,
    int totalConfirmed,
    int totalRead,
    int overdueCount
) {}
```

**Step 2: Add backend stats method and endpoint**

Service: `getStats(UUID userId)` — count SENT letters by user, sum recipients/confirmed/read, count overdue (deadline < now with open recipients).

Controller: `GET /api/v1/parent-letters/stats` → `ApiResponse<ParentLetterStatsInfo>`

**Step 3: Add frontend types and API**

```typescript
export interface ParentLetterStatsInfo {
  activeCount: number
  totalRecipients: number
  totalConfirmed: number
  totalRead: number
  overdueCount: number
}

// API
getStats() {
  return client.get<ApiResponse<ParentLetterStatsInfo>>('/parent-letters/stats')
},
```

**Step 4: Add stats to store**

```typescript
const stats = ref<ParentLetterStatsInfo | null>(null)
async function fetchStats() {
  const res = await parentLetterApi.getStats()
  stats.value = res.data.data
}
```

**Step 5: Enhance ParentLettersView**

Add summary cards between header and tabs:
```vue
<div v-if="canCreate && store.stats" class="stats-bar">
  <div class="stat-card">
    <span class="stat-value">{{ store.stats.activeCount }}</span>
    <span class="stat-label">{{ t('parentLetters.stats.active') }}</span>
  </div>
  <div class="stat-card">
    <span class="stat-value" :class="rateClass">{{ confirmRate }}%</span>
    <span class="stat-label">{{ t('parentLetters.stats.confirmRate') }}</span>
  </div>
  <div class="stat-card" v-if="store.stats.overdueCount > 0">
    <span class="stat-value overdue">{{ store.stats.overdueCount }}</span>
    <span class="stat-label">{{ t('parentLetters.stats.overdue') }}</span>
  </div>
</div>
```

Enhance the per-letter progress bar with color and text:
```vue
<div class="confirm-info">
  <span>{{ letter.confirmedCount }}/{{ letter.totalRecipients }}</span>
  <ProgressBar :value="confirmProgress(letter)" :showValue="false"
    :class="progressClass(letter)" style="height: 0.5rem" />
</div>
```

Add deadline badges: red "Überfällig" if deadline passed, orange "X Tage" if soon.

**Step 6: Add i18n keys**

```
stats.active: 'Aktive Briefe' / 'Active Letters'
stats.confirmRate: 'Rücklaufquote' / 'Confirmation Rate'
stats.overdue: 'Überfällig' / 'Overdue'
```

**Step 7: Run all frontend tests + TypeScript check**

**Step 8: Docker build backend**

**Step 9: Commit**

```bash
git commit -m "feat(parentletter): add inline reporting statistics and summary bar"
```

---

## Task 7: Final integration test and cleanup

**Step 1: Run full frontend test suite**

Run: `cd frontend && npx vitest run`
Expected: All tests pass

**Step 2: TypeScript check**

Run: `cd frontend && npx vue-tsc --noEmit`
Expected: No errors

**Step 3: Vite production build**

Run: `cd frontend && npx vite build`
Expected: Build succeeds

**Step 4: Docker compose full rebuild**

Run: `docker compose build && docker compose up -d`
Expected: All containers healthy, V105 migration applied

**Step 5: Final commit with all remaining changes**

```bash
git commit -m "feat(parentletter): complete v2 — markdown editor, PDF export, attachments, reporting (#150)"
```
