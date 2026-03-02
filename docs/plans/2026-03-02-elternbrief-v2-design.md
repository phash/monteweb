# Elternbrief-Modul v2 — Design

## Zusammenfassung

Erweiterung des Elternbrief-Moduls (#150) um vier Bereiche: Markdown-Editor mit Live-Preview, PDF-Export (Einzelbrief + Rücklauf-Liste), Datei-Anhänge, und Inline-Reporting in der Briefliste.

## 1. Markdown-Editor mit Live-Preview

### Problem
Die aktuelle Textarea bietet keine Formatierungsmöglichkeiten und keine Vorschau der Variablen-Auflösung.

### Lösung
Split-Panel: links Markdown-Editor (Textarea), rechts Live-Preview (gerendert mit `markdown-it`).

### Details
- **Variablen-Vorschau:** Platzhalter werden live mit Beispieldaten aufgelöst:
  - `{Familie}` → "Familie Müller"
  - `{NameKind}` → "Max"
  - `{Anrede}` → "Sehr geehrte Frau Müller"
  - `{LehrerName}` → aktueller Benutzername
- **Komponenten:**
  - `MarkdownLetterEditor.vue` — Split-View Komponente
  - Bestehende `VariableHelpMenu.vue` bleibt, Klick fügt Variable an Cursor-Position ein
  - `markdown-it` ist bereits im Projekt (Wiki-Modul)
- **Briefvorlagen:** Dropdown "Vorlage laden" mit 2-3 Standard-Templates (Allgemein, Einladung, Information), gespeichert als Markdown-Strings in der Config

### Entscheidung
Markdown+Preview statt WYSIWYG — konsistent mit Wiki-Modul, kein neues Dependency.

## 2. PDF-Export

### Problem
Briefe können nicht ausgedruckt oder als Dokument archiviert werden. Rücklauf-Status ist nur online einsehbar.

### Lösung
Zwei PDF-Typen über Backend-Endpoints.

### Einzelbrief-PDF
- **Endpoint:** `GET /api/v1/parent-letters/{id}/pdf?studentId=...`
- **Layout:** Briefkopf-Logo oben (aus Config), Datum, Empfänger (Familie), Betreff, Inhalt (Variablen aufgelöst), Signatur
- **Ohne `studentId`:** Multi-Page-PDF mit allen Empfängern
- **Technologie:** Bestehender `PdfService` (OpenPDF/iText), Markdown → HTML → PDF

### Rücklauf-PDF
- **Endpoint:** `GET /api/v1/parent-letters/{id}/tracking-pdf`
- **Layout:** Tabelle (Schüler, Elternteil, Familie, Status, Bestätigt am), Header mit Brief-Metadaten, Footer mit Zusammenfassung (X/Y bestätigt, Rücklaufquote)

### Backend
- `ParentLetterPdfService.java` — nutzt bestehenden `PdfService`
- Markdown-Rendering via `commonmark-java`

### Frontend
- Download-Buttons in der Detail-View: "Brief als PDF" + "Rücklauf-Liste als PDF"

## 3. Datei-Anhänge

### Problem
Lehrer können keine ergänzenden Dokumente (Formulare, Infoblätter) an den Brief anhängen.

### Lösung
Datei-Upload via MinIO, analog Feed-Attachments.

### Datenmodell
Neue Tabelle `parent_letter_attachments` (Migration V105):
- `id` UUID PK
- `letter_id` FK → parent_letters ON DELETE CASCADE
- `original_filename` VARCHAR(500) NOT NULL
- `storage_path` VARCHAR(500) NOT NULL
- `file_size` BIGINT NOT NULL
- `content_type` VARCHAR(100) NOT NULL
- `uploaded_by` UUID FK → users ON DELETE SET NULL
- `sort_order` INTEGER NOT NULL DEFAULT 0
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()

### Limits
- Max 5 Anhänge pro Brief
- Max 10 MB pro Datei (konfigurierbar über `tenant_config.max_upload_size_mb`)

### MinIO-Pfad
`parentletter/{letterId}/attachments/{attachmentId}.{ext}`

### Endpoints
- `POST /{id}/attachments` — multipart upload
- `GET /{id}/attachments/{attachmentId}` — download
- `DELETE /{id}/attachments/{attachmentId}` — löschen (nur DRAFT)

### Sicherheit
- ClamAV-Scan beim Upload (wenn aktiviert)
- Content-Type via Magic-Bytes (nicht Client-Header)

## 4. Inline-Reporting in Briefliste

### Problem
Lehrer haben keinen schnellen Überblick über den Bestätigungsstatus aller Briefe.

### Lösung
Summary-Bar und erweiterte Statistiken direkt in der Briefliste.

### Summary-Bar (oben)
Drei Karten:
- **Aktive Briefe** — Anzahl Briefe mit Status SENT
- **Gesamt-Rücklaufquote** — Prozent mit Farbindikator (grün >80%, gelb 50-80%, rot <50%)
- **Überfällig** — Anzahl Briefe nach Deadline mit offenen Bestätigungen

### Pro Brief in der Liste
- Bestehend: Titel, Raum, Status-Tag, Deadline
- Neu: ProgressBar (grün=bestätigt, gelb=gelesen, rot=offen) mit Zahlen "12/15 bestätigt"
- Neu: Überfällig-Badge (rot) wenn Deadline überschritten
- Neu: "Frist in X Tagen"-Badge (orange) wenn Deadline naht

### Backend
Separater Stats-Endpoint: `GET /api/v1/parent-letters/stats`
```json
{
  "activeCount": 5,
  "totalRecipients": 120,
  "totalConfirmed": 98,
  "overdueCount": 1
}
```

## Nicht im Scope

- WYSIWYG-Editor (TipTap/Quill)
- Mehrstufige Bestätigung / digitale Unterschrift
- Eltern-Rückfragen / Kommentare
- Separates Dashboard (Statistiken sind in der Briefliste integriert)
- Admin-weite Schulstatistiken (kann später ergänzt werden)

## Technische Abhängigkeiten

- `markdown-it` — bereits im Projekt (Wiki-Modul)
- `PdfService` — bereits in `com.monteweb.shared`
- `commonmark-java` — für serverseitiges Markdown→HTML (Maven-Dependency)
- MinIO — bereits konfiguriert

## Migration

- V105: `parent_letter_attachments` Tabelle
