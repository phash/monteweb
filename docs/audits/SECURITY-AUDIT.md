# Security Audit Report - MonteWeb

**Datum:** 2026-02-26
**Scope:** Vollstaendige Codebase-Analyse (Backend, Frontend, Infrastruktur)
**Methodik:** Statische Code-Analyse, manuelle Code-Reviews

---

## Zusammenfassung

| Schweregrad | Anzahl |
|-------------|--------|
| KRITISCH    | 2      |
| HOCH        | 4      |
| MITTEL      | 6      |
| NIEDRIG     | 4      |

**Gesamtbewertung:** Die Anwendung zeigt solide Security-Grundlagen (BCrypt, JWT mit HS256, Rate Limiting, parameterisierte Queries). Es wurden jedoch einige Schwachstellen identifiziert, die behoben werden sollten.

---

## KRITISCHE SCHWACHSTELLEN

### K1: SSRF (Server-Side Request Forgery) via Link-Preview

**Datei:** `backend/src/main/java/com/monteweb/feed/internal/service/LinkPreviewService.java:76-115`

**Problem:** Der Endpunkt `GET /api/v1/feed/link-preview?url=...` nimmt eine beliebige URL vom authentifizierten Benutzer entgegen und fuehrt serverseitig HTTP-Requests damit aus. Es findet nur eine Pruefung des Schemas (`http`/`https`) statt, aber keine Validierung gegen interne Netzwerke.

```java
public LinkPreviewInfo fetchPreview(String url) {
    var uri = URI.create(url);
    var scheme = uri.getScheme();
    if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
        return null;
    }
    // Kein Check gegen interne IPs/Hosts!
    var request = HttpRequest.newBuilder().uri(uri).timeout(FETCH_TIMEOUT).GET().build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
}
```

**Angriffsszenario:**
- `GET /api/v1/feed/link-preview?url=http://169.254.169.254/latest/meta-data/` (Cloud-Metadaten)
- `GET /api/v1/feed/link-preview?url=http://localhost:5432/` (interne Dienste: Postgres, Redis, MinIO)
- `GET /api/v1/feed/link-preview?url=http://minio:9000/` (Docker-interne Services)

**Auswirkung:** Angreifer koennen interne Netzwerk-Dienste scannen, Cloud-Metadaten auslesen, und potenziell interne APIs ansprechen.

**Empfohlene Behebung:**
```java
private boolean isAllowedUrl(URI uri) {
    try {
        InetAddress addr = InetAddress.getByName(uri.getHost());
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()
            || addr.isSiteLocalAddress() || addr.isAnyLocalAddress()) {
            return false;
        }
        // Block bekannte interne Docker-Hostnamen
        String host = uri.getHost().toLowerCase();
        if (Set.of("localhost", "minio", "redis", "postgres", "solr", "backend")
                .contains(host)) {
            return false;
        }
        return true;
    } catch (UnknownHostException e) {
        return false;
    }
}
```

---

### K2: HTTP Header Injection via Content-Disposition Dateinamen

**Dateien:**
- `backend/.../feed/internal/controller/FeedController.java:174`
- `backend/.../files/internal/controller/FileController.java:72`
- `backend/.../jobboard/internal/controller/JobboardController.java:288`
- `backend/.../messaging/internal/controller/MessagingController.java:174`

**Problem:** Dateinamen werden unkodiert in `Content-Disposition`-Header eingefuegt. Der Dateiname stammt vom Upload des Benutzers.

```java
// FeedController.java:174
.header(HttpHeaders.CONTENT_DISPOSITION,
    "attachment; filename=\"" + attachment.getFileName() + "\"")
```

**Angriffsszenario:** Ein Benutzer laedt eine Datei mit dem Namen `test.pdf\r\nX-Injected: value` hoch. Beim Download wird ein zusaetzlicher HTTP-Header injiziert.

**Auswirkung:** HTTP Response Splitting, Cache Poisoning, Session Fixation.

**Empfohlene Behebung:**
```java
// Verwende RFC 5987 Encoding fuer Dateinamen
private String buildContentDisposition(String filename) {
    String sanitized = filename.replaceAll("[\\r\\n\";]", "_");
    String encoded = URLEncoder.encode(sanitized, StandardCharsets.UTF_8)
        .replace("+", "%20");
    return "attachment; filename=\"" + sanitized + "\"; filename*=UTF-8''" + encoded;
}
```

---

## HOHE SCHWACHSTELLEN

### H1: XSS via v-html mit benutzerkontrollierten Daten

**Dateien:**
- `frontend/src/views/PrivacyPolicyView.vue:26` - Admin-kontrollierter HTML-Text
- `frontend/src/views/TermsView.vue:57` - Admin-kontrollierter HTML-Text
- `frontend/src/components/rooms/RoomWiki.vue:504,568,650` - Benutzer-Wiki-Inhalte
- `frontend/src/components/common/GlobalSearch.vue:250,254` - Solr-Suchergebnisse

**Problem:** `v-html` rendert unkontrollierten HTML-Content direkt im DOM.

```vue
<!-- PrivacyPolicyView.vue:26 -->
<div v-html="policy.text" />

<!-- RoomWiki.vue:504 -->
<div v-html="renderMarkdown(currentPage.content)" />

<!-- GlobalSearch.vue:254 -->
<div v-html="result.snippet"></div>
```

**Risikobewertung:**
- **Wiki (HOCH):** Jedes Raum-Mitglied kann Wiki-Seiten bearbeiten. Obwohl `renderMarkdown()` HTML-Entities escaped (Zeile 304), werden danach wieder HTML-Tags eingefuegt. Ein Angreifer koennte ueber den Markdown-Link-Syntax (`[text](javascript:alert(1))`) oder durch Edge-Cases im Regex-Parser XSS ausfuehren.
- **Search (MITTEL):** Solr-Snippets werden direkt gerendert. Vergiftete Solr-Daten koennten XSS ausloesen.
- **Privacy/Terms (NIEDRIG):** Nur Admin-kontrolliert.

**Empfohlene Behebung:** DOMPurify fuer alle `v-html`-Nutzungen:
```bash
npm install dompurify @types/dompurify
```
```typescript
import DOMPurify from 'dompurify'
const sanitized = DOMPurify.sanitize(htmlContent)
```

---

### H2: 2FA-Secret-Leak an externen QR-Code-Service

**Datei:** `frontend/src/views/ProfileView.vue:311-314`

```typescript
const qrCodeUrl = computed(() => {
  return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(setupQrUri.value)}`
})
```

**Problem:** Der TOTP-Setup-URI (einschliesslich des geheimen Schluessels) wird an einen externen Dienst (`api.qrserver.com`) gesendet, um einen QR-Code zu generieren. Der geheime Schluessel wird damit an einen Drittanbieter uebertragen.

**Auswirkung:** Der 2FA-Secret des Benutzers wird an `qrserver.com` uebermittelt und in deren Server-Logs/Caches gespeichert. Ein Angreifer, der Zugriff auf deren Logs hat, koennte 2FA umgehen.

**Empfohlene Behebung:** Client-seitige QR-Code-Generierung:
```bash
npm install qrcode
```
```typescript
import QRCode from 'qrcode'
const qrCanvas = ref<HTMLCanvasElement | null>(null)
watch(setupQrUri, async (uri) => {
  if (uri && qrCanvas.value) {
    await QRCode.toCanvas(qrCanvas.value, uri)
  }
})
```

---

### H3: Fehlende Dateigroessen- und Dateityp-Validierung im Files-Modul

**Datei:** `backend/src/main/java/com/monteweb/files/internal/service/FileService.java:118-156`

**Problem:** Im Gegensatz zu anderen Modulen (Fotobox: 50MB + Magic-Byte-Pruefung, Messaging: 20MB + Whitelist) hat das Files-Modul:
- Keine Dateigroessenbegrenzung
- Keine Dateityp-Validierung
- Content-Type wird vom Client vertraut (`file.getContentType()`)

```java
public FileInfo uploadFile(UUID roomId, UUID folderId, UUID userId,
                          MultipartFile file, String audience) {
    requireRoomMembership(userId, roomId);
    // KEINE Groessenpr ufung!
    // KEINE Typ-Pruefung!
    String storedName = UUID.randomUUID() + "_" + sanitizeFileName(file.getOriginalFilename());
    String storagePath = storageService.upload(roomId, folderId, storedName, file);
    roomFile.setContentType(file.getContentType()); // Vertraut Client!
}
```

**Auswirkung:**
- DoS durch unbegrenzt grosse Uploads (Storage-Erschoepfung)
- Upload gefaehrlicher Dateitypen (.html, .svg mit Script)
- XSS bei Download mit vertrautem `Content-Type: text/html`

**Empfohlene Behebung:**
```java
private static final long MAX_FILE_SIZE = 100L * 1024 * 1024; // 100 MB

public FileInfo uploadFile(...) {
    if (file.getSize() > MAX_FILE_SIZE) {
        throw new BusinessException("File too large. Maximum size: 100 MB.");
    }
    // Content-Type aus Magic Bytes bestimmen (wie FotoboxStorageService)
    String detectedType = detectContentType(file);
    // Bei Download: immer application/octet-stream erzwingen
}
```

---

### H4: Open Redirect im Login-Flow

**Datei:** `frontend/src/views/LoginView.vue:78-79, 104-105`

```typescript
const redirect = (route.query.redirect as string) || '/'
router.push(redirect)
```

**Problem:** Der `redirect`-Query-Parameter wird ohne Validierung an `router.push()` weitergegeben.

**Angriffsszenario:** `https://monteweb.schule.de/login?redirect=https://evil.com/phishing`

**Empfohlene Behebung:**
```typescript
function safeRedirect(path: string): string {
  if (!path || !path.startsWith('/') || path.startsWith('//') || path.includes('://')) {
    return '/'
  }
  return path
}
const redirect = safeRedirect(route.query.redirect as string)
router.push(redirect)
```

---

## MITTLERE SCHWACHSTELLEN

### M1: JWT-Tokens in localStorage (XSS-angreifbar)

**Dateien:** `frontend/src/api/client.ts:13`, `frontend/src/stores/auth.ts:139-140`

Tokens in localStorage sind bei XSS-Schwachstellen (siehe H1) direkt auslesbar. Mildernde Faktoren: Access-Token nur 15 Minuten gueltig, Refresh-Token-Rotation.

**Empfehlung:** Langfristig auf HttpOnly-Cookies umstellen oder zumindest XSS-Schwachstellen priorisiert beheben.

---

### M2: Image-Token in URL-Query-Parametern

**Datei:** `frontend/src/composables/useImageToken.ts:58`

```typescript
export function authenticatedImageUrl(path: string): string {
  const token = imageToken.value
  return token ? `${path}?token=${encodeURIComponent(token)}` : path
}
```

Token erscheint in Browser-History, Server-Logs, Referrer-Headern. 5-Minuten-Ablauf mildert das Risiko.

---

### M3: Fehlende Input-Validierung in Update-DTOs

**Dateien:**
- `backend/.../feed/internal/dto/UpdatePostRequest.java` - Keine `@NotBlank`/`@Size`
- `backend/.../user/internal/dto/UpdateProfileRequest.java` - Fehlende Laengen-Validierung
- `backend/.../files/internal/controller/FileController.java:144` - `CreateFolderRequest` ohne Validierung

**Empfehlung:** `@NotBlank`, `@Size`, `@Valid` auf alle Request-DTOs anwenden.

---

### M4: Content-Type bei File-Downloads nicht validiert

**Datei:** `backend/.../files/internal/controller/FileController.java:73-74`

```java
.contentType(MediaType.parseMediaType(
    metadata.getContentType() != null ? metadata.getContentType() : "application/octet-stream"))
```

Der Content-Type stammt aus der DB (vom Upload gesetzt mit Client-deklariertem Typ). Bei `text/html` wuerde der Browser die Datei als HTML rendern (XSS).

**Empfehlung:** Fuer Downloads immer `application/octet-stream` erzwingen.

---

### M5: Service Worker cached Benutzerdaten ohne Logout-Bereinigung

**Datei:** `frontend/src/sw.ts:28-62`

```typescript
registerRoute(
  ({ url }) => /\/api\/v1\/(families\/mine|users\/me|notifications)/.test(url.pathname),
  new NetworkFirst({ cacheName: 'user-data-cache',
    plugins: [new ExpirationPlugin({ maxEntries: 20, maxAgeSeconds: 86400 })],
  }),
)
```

Nach Logout bleiben Daten bis zu 24h im Cache. Auf geteilten Geraeten ein Datenschutzproblem.

**Empfehlung:** Caches bei Logout loeschen:
```typescript
const cacheNames = await caches.keys()
await Promise.all(cacheNames.map(name => caches.delete(name)))
```

---

### M6: Refresh-Token-Replay-Detection fehlt

**Datei:** `backend/.../auth/internal/service/RefreshTokenService.java:48-61`

Token-Rotation ist implementiert (altes Token wird geloescht), aber es fehlt die Erkennung von Token-Wiederverwendung. Wenn ein gestohlenes Refresh-Token vor dem legitimen Benutzer verwendet wird, erhaelt der Angreifer neue Tokens.

**Empfehlung:** Token-Familie tracken und bei Wiederverwendung alle Sessions invalidieren.

---

## NIEDRIGE SCHWACHSTELLEN

### N1: CORS AllowedHeaders zu permissiv

**Datei:** `backend/.../shared/config/CorsConfig.java:23`

```java
config.setAllowedHeaders(List.of("*"));
```

**Empfehlung:** Auf spezifische Header einschraenken: `Authorization`, `Content-Type`, `Accept`.

---

### N2: Redis-Passwort in Backup-Script als CLI-Argument

**Datei:** `backup/backup.sh:109`

Passwort mit `-a` Flag sichtbar in `ps`-Ausgabe.

---

### N3: Backups nicht verschluesselt

**Datei:** `backup/backup.sh`

Backups enthalten Datenbank-Dumps mit persoenlichen Daten, werden aber unverschluesselt gespeichert.

---

### N4: Fehlende Content Security Policy im Frontend

Die nginx-Konfiguration enthaelt eine CSP, aber sie ist relativ permissiv. Eine strengere CSP wuerde XSS-Angriffe weiter erschweren.

---

## POSITIVES (Gut implementiert)

| Bereich | Bewertung |
|---------|-----------|
| Passwort-Hashing (BCrypt) | Exzellent |
| JWT-Implementierung (HS256, Token-Typen, Ablaufzeiten) | Exzellent |
| SQL-Injection-Schutz (parameterisierte Queries) | Exzellent |
| LDAP-Injection-Schutz (RFC 4515 Escaping) | Exzellent |
| Rate Limiting (Auth-Endpunkte) | Gut |
| IDOR-Schutz (Room-Membership + Ownership-Checks) | Exzellent |
| Admin-Endpunkte (@PreAuthorize + Security Config) | Exzellent |
| Passwort-Reset (UUID, 24h Ablauf, Einmalverwendung) | Exzellent |
| 2FA/TOTP (RFC 6238, Recovery Codes) | Exzellent |
| Keine hardcodierten Secrets | Exzellent |
| Docker-Security (Non-Root, Multi-Stage, Netzwerk-Segmentierung) | Exzellent |
| Security-Headers (HSTS, X-Frame-Options, Referrer-Policy) | Gut |
| Fehlerbehandlung (keine Stack-Traces in API-Responses) | Exzellent |
| WebSocket-Authentifizierung (JWT-Pruefung bei CONNECT) | Gut |
| Pagination (max 100 Eintraege) | Gut |
| Keine Command Injection | Exzellent |
| Keine Template Injection | Exzellent |
| Kein Path Traversal (UUID-basierte Speicherpfade) | Exzellent |
| CI/CD (SHA-gepinnte Actions, Trivy-Scanning) | Exzellent |

---

## Empfohlene Priorisierung

### Sofort beheben (Sprint 1)
1. **K1:** SSRF in LinkPreviewService - IP/Host-Blockliste hinzufuegen
2. **K2:** Header Injection - Dateinamen in Content-Disposition sanitieren
3. **H2:** 2FA-Secret-Leak - QR-Code lokal generieren (einfacher Fix)

### Kurzfristig (Sprint 2)
4. **H1:** XSS via v-html - DOMPurify einbinden
5. **H3:** File Upload Limits im Files-Modul
6. **H4:** Open Redirect im Login validieren

### Mittelfristig (Sprint 3-4)
7. **M3:** Input-Validierung vervollstaendigen
8. **M4:** Content-Type bei Downloads erzwingen
9. **M5:** Cache-Bereinigung bei Logout
10. **M6:** Refresh-Token-Replay-Detection
