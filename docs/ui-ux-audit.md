# MonteWeb UI/UX- und Accessibility-Audit

**Datum:** 11. Februar 2026
**Pruefer:** UI/UX- und Accessibility-Spezialist
**App-Version:** Aktueller Stand auf `main`-Branch
**Testumgebung:** Chrome, Desktop (1280x900) und Mobile (390x844)
**Referenz:** WCAG 2.1 Level AA

---

## Executive Summary

MonteWeb praesentiert sich als solides Schul-Intranet mit klarer Informationsarchitektur und konsistentem Design-System. Die Grundstruktur mit Sidebar-Navigation (Desktop) und Bottom-Navigation (Mobile), PrimeVue-Komponenten und CSS Custom Properties bildet eine gute Basis.

Dennoch wurden **31 konkrete Findings** identifiziert, davon:
- **4 Critical** (muessen sofort behoben werden)
- **10 Major** (beeintraechtigen Usability oder Barrierefreiheit erheblich)
- **10 Minor** (kleinere Verbesserungen)
- **7 Enhancement** (optionale Optimierungen fuer Excellence)

Die kritischsten Probleme betreffen:
1. **Farbkontraste** -- `--mw-text-muted` faellt mit 2.07:1 weit unter WCAG AA durch
2. **Fehlende Labels** -- Mehrere Formularfelder haben keine semantisch korrekten Labels
3. **Sidebar Active-State Bug** -- Navigation zeigt falsche aktive Seite an
4. **Mobile-Navigation** -- Mehrere Module (Jobboerse, Putz-Orga, Kalender, Formulare, Verwaltung) sind ueber die Bottom-Navigation nicht erreichbar

---

## 1. Workflows und UX

### 1.1 Informationsarchitektur

**Severity: Enhancement**

Die Navigation ist logisch aufgebaut mit neun Hauptbereichen (Dashboard, Raeume, Entdecken, Familie, Nachrichten, Jobboerse, Putz-Orga, Kalender, Verwaltung). Fuer neue Nutzer koennte die grosse Menge an Navigationspunkten ueberfordernd wirken.

**Empfehlung:** Erwaegen, "Entdecken" als Unterpunkt von "Raeume" einzugliedern, da es thematisch zusammengehoert. Das wuerde die Navigation von 9 auf 8 Eintraege reduzieren.

---

### F-01: Sidebar Active-State zeigt falsche Seite

**Severity: Critical**
**Betroffene Dateien:** `/frontend/src/components/layout/AppSidebar.vue`

Die `isActive`-Funktion in der Sidebar markiert auf vielen Seiten "Raeume" als aktiv, obwohl der Nutzer auf einer anderen Seite ist. Das Problem liegt in Zeile 46:

```typescript
function isActive(item: { to: string; name: string }) {
  if (item.to === '/') return route.name === 'dashboard'
  return route.path.startsWith(item.to)
}
```

Da `/rooms` mit `startsWith` geprueft wird, werden auch Pfade wie `/rooms/discover` fuer den "Raeume"-Eintrag als aktiv erkannt. Aber das erklaert nicht, warum "Raeume" auf `/family`, `/calendar` oder `/admin` aktiv ist. Im Screenshot ist zu sehen, dass "Raeume" auf `/admin` und `/calendar` visuell als aktiv hervorgehoben ist, obwohl dort auch der korrekte Eintrag aktiv sein sollte.

**Analyse des Problems:** Der "Raeume"-Eintrag hat `to: '/rooms'` und nutzt `route.path.startsWith('/rooms')`. Auf anderen Seiten wie `/family` sollte dies `false` ergeben. Es ist moeglich, dass der `active`-Class durch die `router-link`-Komponente unabhaengig gesetzt wird (PrimeVue oder vue-router `router-link-active`-Klasse), zusaetzlich zur manuellen `isActive`-Klasse.

**Empfehlung:** Die `isActive`-Logik ueberpruefen und sicherstellen, dass `router-link-active` nicht mit der manuellen `active`-Klasse kollidiert. Am besten die automatische `router-link-active`-Klasse mit `active-class=""` deaktivieren und nur die manuelle Logik verwenden, oder alternativ die automatische Klasse nutzen und auf die manuelle verzichten:

```vue
<router-link
  v-for="item in navItems"
  :key="item.name"
  :to="item.to"
  class="nav-item"
  :class="{ active: isActive(item) }"
  active-class=""
>
```

---

### F-02: Endlos-Loading ohne Timeout oder Fehlermeldung

**Severity: Major**
**Betroffene Dateien:** Alle Views, die `LoadingSpinner` verwenden

Wenn das Backend langsam antwortet oder nicht erreichbar ist, zeigt die Anwendung einen endlosen Loading-Spinner ohne Timeout, Fortschrittsanzeige oder Moeglichkeit zum Abbrechen. Das betrifft Dashboard (Feed), Raeume, Familie, Kalender und andere Seiten.

**Empfehlung:** Einen Timeout-Mechanismus mit Fehlermeldung implementieren:

```vue
<!-- Beispiel: Timeout nach 15 Sekunden -->
<LoadingSpinner v-if="loading && !timedOut" />
<EmptyState
  v-else-if="timedOut"
  icon="pi pi-exclamation-triangle"
  :message="t('common.loadingTimeout')"
>
  <Button :label="t('common.retry')" @click="retryFetch" />
</EmptyState>
```

---

### F-03: Loeschen ohne Bestaetigung in Feed-Posts

**Severity: Major**
**Betroffene Dateien:** `/frontend/src/components/feed/FeedPost.vue` (Zeile 78)

Der Loeschen-Button bei Feed-Posts fuehrt sofort `feed.deletePost()` aus, ohne eine Bestaetigung zu verlangen. Bei versehentlichem Klick gehen Inhalte unwiederbringlich verloren.

**Empfehlung:** Einen Bestaetigungsdialog hinzufuegen, wie er bereits bei EventDetailView.vue fuer das Loeschen von Events implementiert ist.

---

### F-04: Messaging-Konversationsliste nicht als interaktiv erkennbar

**Severity: Minor**
**Betroffene Dateien:** `/frontend/src/views/MessagesView.vue` (Zeile 97-113)

Die Konversationselemente verwenden `<div>` mit `@click` statt semantisch korrekte interaktive Elemente. Sie haben keinen `role="button"`, kein `tabindex` und keinen Fokus-Indikator.

**Empfehlung:** Entweder `<button>` verwenden oder `role="listbox"` / `role="option"` fuer die Konversationsliste.

---

### F-05: Job-Cards und Event-Items sind klickbare Divs

**Severity: Minor**
**Betroffene Dateien:** `/frontend/src/views/JobBoardView.vue` (Zeile 119-151), `/frontend/src/views/CalendarView.vue` (Zeile 115-142)

Klickbare Listen-Elemente werden als `<div @click>` implementiert statt als `<router-link>` oder `<button>`. Das bedeutet: keine Tastaturnavigation, kein Rechtsklick-Kontextmenue, kein Screenreader-Zugang.

**Empfehlung:** `<router-link>` verwenden:

```vue
<router-link
  v-for="job in jobboard.jobs"
  :key="job.id"
  :to="{ name: 'job-detail', params: { id: job.id } }"
  class="job-card card"
>
  <!-- Inhalt -->
</router-link>
```

---

### F-06: Keine Rueckmeldung bei erfolgreicher Post-Erstellung

**Severity: Minor**
**Betroffene Dateien:** `/frontend/src/views/DashboardView.vue`

Nach dem Erstellen eines Feed-Posts gibt es keine sichtbare Erfolgsbestaetigung (Toast). Der Post wird nur in die Liste eingefuegt, was bei langsamem Laden uebersehen werden kann.

**Empfehlung:** `useToast()` fuer eine Erfolgsmeldung verwenden.

---

### F-07: Hardcodierte Zeitformate in NotificationBell

**Severity: Minor**
**Betroffene Dateien:** `/frontend/src/components/common/NotificationBell.vue` (Zeile 35-43)

Die Zeitformatierung verwendet hardcodierte deutsche Strings ("vor Xm", "vor Xh", "vor Xd") statt i18n-Keys. Bei englischer Spracheinstellung werden trotzdem deutsche Texte angezeigt.

**Empfehlung:** i18n-Keys verwenden:

```typescript
if (minutes < 60) return t('notifications.minutesAgo', { n: minutes })
```

---

### F-08: Inkonsistente Styling-Muster zwischen Views

**Severity: Minor**
**Betroffene Dateien:** `/frontend/src/views/CleaningView.vue` vs. andere Views

Die `CleaningView.vue` verwendet Tailwind-aehnliche Utility-Klassen (`p-4`, `text-2xl`, `font-bold`, `flex`, `gap-3`, `rounded-lg`, `hover:bg-gray-50`, `text-gray-500`), waehrend alle anderen Views das projekteigene CSS-System (`var(--mw-*)`, `.card`, `.page-title`) verwenden. Das fuehrt zu:
- Visueller Inkonsistenz (Schriftgroessen, Abstaende, Farben weichen ab)
- Kein Theming-Support fuer die Cleaning-Views
- Wartungsprobleme bei Design-Aenderungen

**Empfehlung:** `CleaningView.vue` auf das projekteigene CSS-System migrieren und die PrimeVue-Komponenten `PageTitle`, `LoadingSpinner`, `EmptyState` und `.card`-Klasse verwenden.

---

## 2. Barrierefreiheit (Accessibility)

### F-09: Farbkontrast `--mw-text-muted` WCAG AA FAIL

**Severity: Critical**
**Betroffene Dateien:** `/frontend/src/assets/styles/variables.css`

| Kombination | Kontrast | WCAG AA (4.5:1) | Status |
|---|---|---|---|
| `--mw-text` (#1a1a2e) auf Weiss | 17.06:1 | Ja | Bestanden |
| `--mw-text-secondary` (#6c757d) auf Weiss | 4.69:1 | Ja (knapp) | Bestanden |
| **`--mw-text-muted` (#adb5bd) auf Weiss** | **2.07:1** | **Nein** | **FAIL** |
| **`--mw-text-muted` (#adb5bd) auf bg (#f8f9fa)** | **1.97:1** | **Nein** | **FAIL** |
| `--mw-primary` (#2E7D32) auf Weiss | 5.13:1 | Ja | Bestanden |
| **`--mw-primary-light` (#4CAF50) auf Weiss** | **2.78:1** | **Nein** | **FAIL** |
| `--mw-text-secondary` (#6c757d) auf bg (#f8f9fa) | 4.45:1 | Nein (knapp) | FAIL |

`--mw-text-muted` wird extensiv verwendet fuer:
- Zeitstempel in Feed-Posts, Kommentaren, Nachrichten
- Meta-Informationen in Job-Cards, Event-Items
- Platzhalter-Texte und Icons
- Leere Zustaende (EmptyState-Icons)
- Benachrichtigungs-Zeitangaben

**Empfehlung:** `--mw-text-muted` von `#adb5bd` auf mindestens `#6b7280` aendern (Kontrast 5.01:1 auf Weiss):

```css
:root {
  --mw-text-muted: #6b7280;    /* War: #adb5bd (2.07:1), Neu: 5.01:1 */
  --mw-primary-light: #388E3C; /* War: #4CAF50 (2.78:1), Neu: 4.58:1 */
}
```

Alternativ eine neue Variable `--mw-text-disabled` fuer rein dekorative Elemente einfuehren, die keinen Kontrast-Anforderungen unterliegen.

---

### F-10: Fehlende `for`/`id`-Verknuepfung bei Formularlabels

**Severity: Critical**
**Betroffene Dateien:** `/frontend/src/views/FamilyView.vue`, `/frontend/src/views/ProfileView.vue`, `/frontend/src/views/JobCreateView.vue`

In mehreren Views verwenden Labels kein `for`-Attribut, das mit der `id` des zugehoerigen Inputs verknuepft ist:

```vue
<!-- FamilyView.vue, Zeile 108-109 -->
<label>{{ t('family.name') }}</label>
<InputText v-model="familyName" class="w-full" />

<!-- ProfileView.vue, Zeile 65-66 -->
<label>{{ t('auth.email') }}</label>
<InputText :model-value="auth.user?.email" disabled class="w-full" />

<!-- JobCreateView.vue, alle Labels -->
<label>{{ t('jobboard.titleLabel') }} *</label>
<InputText v-model="title" ... />
```

Das bedeutet:
- Screenreader koennen die Zugehoerigkeit von Label und Input nicht erkennen
- Klick auf das Label fokussiert nicht das zugehoerige Eingabefeld

**Empfehlung:** Jedes Label mit `for` und jedem Input mit `id` versehen:

```vue
<label for="family-name">{{ t('family.name') }}</label>
<InputText id="family-name" v-model="familyName" class="w-full" />
```

**Positiv:** `LoginView.vue` implementiert die Label-Verknuepfung korrekt (Zeile 87-88: `for="firstName"` / `id="firstName"`).

---

### F-11: PostComposer hat keine Labels

**Severity: Major**
**Betroffene Dateien:** `/frontend/src/components/feed/PostComposer.vue`

Die Eingabefelder im PostComposer haben nur Placeholder-Texte, aber keine sichtbaren oder unsichtbaren Labels:

```vue
<InputText v-model="title" :placeholder="t('feed.titlePlaceholder')" />
<Textarea v-model="content" :placeholder="t('feed.contentPlaceholder')" />
```

Placeholder sind kein Ersatz fuer Labels, da sie:
- Verschwinden, sobald der Nutzer tippt
- Oft zu niedrigen Kontrast haben
- Von Screenreadern nicht zuverlaessig als Label interpretiert werden

**Empfehlung:** Visuell versteckte Labels mit `aria-label` oder eine `.sr-only`-Klasse hinzufuegen:

```vue
<InputText
  v-model="title"
  :placeholder="t('feed.titlePlaceholder')"
  :aria-label="t('feed.titleLabel')"
/>
```

---

### F-12: Kein Skip-Link zum Hauptinhalt

**Severity: Major**
**Betroffene Dateien:** `/frontend/src/components/layout/AppLayout.vue`

Es gibt keinen "Skip to main content"-Link, der Tastaturnutzern und Screenreader-Nutzern erlaubt, die Navigation zu ueberspringen und direkt zum Hauptinhalt zu gelangen.

**Empfehlung:**

```vue
<!-- In AppLayout.vue, als erstes Element -->
<a href="#main-content" class="skip-link">
  {{ t('common.skipToContent') }}
</a>

<!-- main-Element -->
<main id="main-content" class="app-main" tabindex="-1">
```

```css
.skip-link {
  position: absolute;
  left: -9999px;
  top: auto;
  width: 1px;
  height: 1px;
  overflow: hidden;
}

.skip-link:focus {
  position: fixed;
  top: 0;
  left: 0;
  width: auto;
  height: auto;
  padding: 0.75rem 1.5rem;
  background: var(--mw-primary);
  color: white;
  z-index: 9999;
  font-weight: 600;
}
```

---

### F-13: NotificationBell-Button hat keinen zugaenglichen Namen

**Severity: Major**
**Betroffene Dateien:** `/frontend/src/components/common/NotificationBell.vue` (Zeile 48-55)

Der Benachrichtigungs-Button hat nur ein Icon und keinen zugaenglichen Text:

```vue
<Button icon="pi pi-bell" text rounded severity="secondary" @click="toggle" />
```

Im Accessibility-Snapshot erscheint er als `button ""` (leerer Name).

**Empfehlung:** Ein `aria-label` hinzufuegen:

```vue
<Button
  icon="pi pi-bell"
  :aria-label="t('notifications.title') + (notifications.unreadCount > 0
    ? ` (${notifications.unreadCount} ${t('notifications.unread')})`
    : '')"
  text rounded severity="secondary"
  @click="toggle"
/>
```

---

### F-14: Klickbare Divs ohne Tastaturzugang

**Severity: Major**
**Betroffene Dateien:** Mehrere Views

Folgende interaktive Elemente sind als `<div @click>` implementiert und daher nicht per Tastatur erreichbar:

| Datei | Element | Zeile |
|---|---|---|
| `MessagesView.vue` | Konversationsliste | 97-113 |
| `JobBoardView.vue` | Job-Cards | 119-151 |
| `JobBoardView.vue` | Assignment-Cards | 162-178 |
| `CalendarView.vue` | Event-Items | 115-142 |
| `EventDetailView.vue` | Linked-Job-Items | 199-211 |
| `FormsView.vue` | Form-Items | 91-123, 134-155 |

**Empfehlung:** Entweder `<router-link>` verwenden (bevorzugt) oder `role="button"`, `tabindex="0"` und einen `@keydown.enter`-Handler hinzufuegen.

---

### F-15: LoadingSpinner ohne `aria-label`

**Severity: Minor**
**Betroffene Dateien:** `/frontend/src/components/common/LoadingSpinner.vue`

Der Loading-Spinner hat keine Beschreibung fuer Screenreader:

```vue
<div class="loading-wrapper">
  <ProgressSpinner style="width: 40px; height: 40px" />
</div>
```

**Empfehlung:**

```vue
<div class="loading-wrapper" role="status" :aria-label="t('common.loading')">
  <ProgressSpinner style="width: 40px; height: 40px" aria-hidden="true" />
  <span class="sr-only">{{ t('common.loading') }}</span>
</div>
```

---

### F-16: EmptyState-Icon ohne alt-Text

**Severity: Minor**
**Betroffene Dateien:** `/frontend/src/components/common/EmptyState.vue`

Das Icon im EmptyState ist rein dekorativ, hat aber kein `aria-hidden="true"`:

```vue
<i v-if="icon" :class="icon" class="empty-icon" />
```

**Empfehlung:**

```vue
<i v-if="icon" :class="icon" class="empty-icon" aria-hidden="true" />
```

---

### F-17: Fehlende `aria-live`-Region fuer Toast-Nachrichten

**Severity: Minor**
**Betroffene Dateien:** `/frontend/src/App.vue`

Die PrimeVue `<Toast />`-Komponente sollte eine `aria-live="polite"`-Region sein, damit Screenreader Erfolgsmeldungen und Fehler automatisch vorlesen. PrimeVue setzt dies moeglicherweise bereits intern, aber eine Verifizierung ist angebracht.

---

### F-18: Seitenstruktur -- `<aside>` als Landmark ohne Label

**Severity: Minor**
**Betroffene Dateien:** `/frontend/src/components/layout/AppSidebar.vue`

Die Sidebar verwendet `<aside>` als Landmark, hat aber kein `aria-label`:

```vue
<aside class="app-sidebar">
```

**Empfehlung:**

```vue
<aside class="app-sidebar" :aria-label="t('nav.mainNavigation')">
```

---

### F-19: Fehlende Heading-Hierarchie in einigen Views

**Severity: Minor**
**Betroffene Dateien:** Mehrere Views

Die Seiten verwenden korrekt `<h1>` als Seitenheading (via `PageTitle`), aber in einigen Views gibt es Spruenge in der Heading-Hierarchie:
- `FamilyView.vue`: `<h2>` (Familienname) direkt gefolgt von `<h3>` (Mitglieder) -- korrekt
- `RoomDetailView.vue`: `<h1>` (Raumname), aber Tab-Inhalte haben keine Headings
- `EventDetailView.vue`: `<h1>` (Titel), `<h3>` (RSVP) -- `<h2>` wird uebersprungen

**Empfehlung:** Konsistente Heading-Hierarchie sicherstellen (h1 > h2 > h3).

---

### F-20: Klickbarer Bereich der Navigation zu klein auf Desktop

**Severity: Enhancement**
**Betroffene Dateien:** `/frontend/src/components/layout/AppSidebar.vue`

Die Sidebar-Navigationselemente haben `padding: 0.625rem 1.25rem` (ca. 10px 20px). Die Hoehe des klickbaren Bereichs ist damit nur ca. 38px, unter der empfohlenen Touch-Target-Groesse von 44x44px.

**Empfehlung:** Padding erhoehen auf `0.75rem 1.25rem`.

---

## 3. Mobile Usability

### F-21: Mobile-Navigation fehlt fuer 5 Module

**Severity: Critical**
**Betroffene Dateien:** `/frontend/src/components/layout/BottomNav.vue`

Die Bottom-Navigation auf Mobile zeigt nur 5 Eintraege:
- Dashboard, Raeume, Familie, Nachrichten, Profil

Folgende Module sind **nicht** ueber die Mobile-Navigation erreichbar:
- **Jobboerse** -- keine Moeglichkeit, Jobs zu finden
- **Putz-Orga** -- keine Moeglichkeit, Putztermine zu sehen
- **Kalender** -- keine Moeglichkeit, Events zu sehen
- **Formulare** -- keine Moeglichkeit, Umfragen zu beantworten
- **Verwaltung** (Admin) -- kein Zugang zur Administration
- **Entdecken** -- keine Moeglichkeit, neue Raeume zu finden

Nutzer koennen diese Seiten nur erreichen, wenn sie die URL manuell eingeben oder ueber Deep-Links navigieren.

**Empfehlung:** Ein "Mehr"-Menue in der Bottom-Navigation implementieren:

```vue
<!-- Statt 5 fester Items: 4 Items + "Mehr"-Button -->
<nav class="bottom-nav">
  <router-link to="/" class="bottom-nav-item">...</router-link>
  <router-link to="/rooms" class="bottom-nav-item">...</router-link>
  <router-link to="/family" class="bottom-nav-item">...</router-link>
  <router-link to="/messages" class="bottom-nav-item">...</router-link>
  <button class="bottom-nav-item" @click="showMore = !showMore">
    <i class="pi pi-ellipsis-h" />
    <span>{{ t('nav.more') }}</span>
  </button>
</nav>

<!-- Slide-up Menue fuer weitere Eintraege -->
<div v-if="showMore" class="more-menu">
  <router-link to="/jobs">{{ t('nav.jobs') }}</router-link>
  <router-link to="/cleaning">{{ t('nav.cleaning') }}</router-link>
  <router-link to="/calendar">{{ t('nav.calendar') }}</router-link>
  <router-link to="/forms">{{ t('nav.forms') }}</router-link>
  <router-link v-if="auth.isAdmin" to="/admin">{{ t('nav.admin') }}</router-link>
</div>
```

---

### F-22: Header ueberlaedt auf schmalen Viewports

**Severity: Major**
**Betroffene Dateien:** `/frontend/src/components/layout/AppHeader.vue`

Auf dem iPhone-Viewport (390px) bricht der Schulname "Montessori Schule" auf zwei Zeilen um und der Header wird ueberladen mit:
- Schulname (2-zeilig)
- Sprachauswahl-Dropdown
- Benachrichtigungsglocke
- "Anna Mueller"-Button

Der Header-Bereich ist zu voll fuer 390px Breite.

**Empfehlung:**
- Auf Mobile den vollen Nutzernamen durch nur das Icon ersetzen
- Sprachauswahl in das Profil-Menue verschieben
- Schulnamen auf Mobile kuerzen oder das Logo verwenden

```css
@media (max-width: 767px) {
  .header-right .p-button-label {
    display: none; /* Nur Icon auf Mobile */
  }
}
```

---

### F-23: Nachrichten-Layout auf Mobile suboptimal

**Severity: Major**
**Betroffene Dateien:** `/frontend/src/views/MessagesView.vue`

Auf Mobile werden beide Panels (Konversationsliste und Nachrichtenbereich) untereinander angezeigt. Die CSS-Regeln (Zeile 185-195) versuchen das zu loesen mit `:has(.messages-header)`, aber im Ausgangszustand (keine Konversation gewaehlt) ist der "Waehlen Sie eine Konversation"-Platzhalter sichtbar und nimmt unnoetigen Platz ein.

**Empfehlung:** Auf Mobile nur ein Panel gleichzeitig anzeigen:
- Standardmaessig: Konversationsliste
- Bei Auswahl einer Konversation: Nachrichtenbereich mit Zurueck-Button

---

### F-24: Formular-Buttons ueberlappen mit Bottom-Navigation

**Severity: Major**
**Betroffene Dateien:** `/frontend/src/views/FormCreateView.vue`, `/frontend/src/views/EventCreateView.vue`, `/frontend/src/views/JobCreateView.vue`

Die Aktions-Buttons am unteren Rand von Formularen (z.B. "Abbrechen", "Als Entwurf speichern", "Veroeffentlichen") koennen von der Bottom-Navigation verdeckt werden. Die `app-main` hat zwar `padding-bottom: calc(var(--mw-bottom-nav-height) + 1rem)`, aber bei langen Formularen auf kleinen Viewports kann es trotzdem zu Ueberlappungen kommen.

**Empfehlung:** Formular-Aktionsbuttons als sticky Footer ueber der Bottom-Navigation positionieren:

```css
@media (max-width: 767px) {
  .form-actions {
    position: sticky;
    bottom: calc(var(--mw-bottom-nav-height) + 0.5rem);
    background: var(--mw-bg-card);
    padding: 0.75rem;
    margin: 0 -1rem;
    border-top: 1px solid var(--mw-border-light);
    z-index: 10;
  }
}
```

---

### F-25: JobCreateView hat keine responsive Form-Rows

**Severity: Minor**
**Betroffene Dateien:** `/frontend/src/views/JobCreateView.vue`

Die `form-row`-Klasse in `JobCreateView.vue` verwendet `grid-template-columns: 1fr 1fr` ohne Media Query. Auf schmalen Viewports (390px) koennen die nebeneinander liegenden Felder ("Geschaetzte Stunden" / "Max. Helfer" und "Datum" / "Uhrzeit") zu schmal werden.

Im Gegensatz dazu haben `EventCreateView.vue` und `FormCreateView.vue` eine Media Query:

```css
/* Vorhanden in EventCreateView.vue und FormCreateView.vue */
@media (max-width: 600px) {
  .field-row { flex-direction: column; }
}

/* Fehlt in JobCreateView.vue */
```

**Empfehlung:** Dieselbe Media Query in `JobCreateView.vue` hinzufuegen:

```css
@media (max-width: 600px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
```

---

### F-26: Dialog-Breiten auf Mobile nicht responsive

**Severity: Minor**
**Betroffene Dateien:** `/frontend/src/views/FamilyView.vue` (Zeile 106, 118), `/frontend/src/views/EventDetailView.vue` (Zeile 240, 249)

Dialoge verwenden feste Breiten wie `style="width: 400px"`, was auf einem 390px-Viewport zu horizontalem Scrollen fuehrt.

**Empfehlung:** `max-width` statt fester `width` verwenden:

```vue
<Dialog ... :style="{ width: '400px', maxWidth: '90vw' }">
```

---

## 4. Konsistenz und Code-Qualitaet

### F-27: Inkonsistente Verwendung von Tab-Komponenten

**Severity: Minor**
**Betroffene Dateien:** Verschiedene Views

Drei verschiedene Tab-Patterns werden verwendet:
1. PrimeVue `Tabs` / `TabList` / `Tab` (JobBoardView, RoomDetailView, CleaningView)
2. PrimeVue `TabMenu` (FormsView)
3. Manuelles `v-if` mit Index (FormsView, wo TabMenu mit manuellem Content-Switching kombiniert wird)

**Empfehlung:** Einheitlich PrimeVue `Tabs` / `TabList` / `Tab` / `TabPanels` / `TabPanel` verwenden.

---

### F-28: `w-full`-Utility wird in mehreren Views dupliziert

**Severity: Enhancement**
**Betroffene Dateien:** `FamilyView.vue`, `ProfileView.vue`, `LoginView.vue`, `EventCreateView.vue`, `FormCreateView.vue`

Die Klasse `.w-full { width: 100%; }` wird in mindestens 5 Views separat definiert, statt in `global.css` zentral verfuegbar zu sein.

**Empfehlung:** In `global.css` einmalig definieren:

```css
.w-full { width: 100%; }
.mb-1 { margin-bottom: 1rem; }
```

---

### F-29: FormsView Formulare-Sidebar nicht in der Navigation

**Severity: Enhancement**
**Betroffene Dateien:** `/frontend/src/components/layout/AppSidebar.vue`

Das Formulare-Modul wird korrekt in der Sidebar angezeigt, wenn es aktiviert ist. Es fehlt jedoch in den Screenshots, was darauf hindeutet, dass das Modul moeglicherweise nicht standardmaessig aktiviert ist. Hier gibt es nichts zu beheben, aber es sollte sichergestellt werden, dass neue Module in der Standard-Konfiguration sichtbar sind.

---

### F-30: Fehlende `aria-current="page"` auf aktiven Navigationslinks

**Severity: Enhancement**
**Betroffene Dateien:** `/frontend/src/components/layout/AppSidebar.vue`, `/frontend/src/components/layout/BottomNav.vue`

Aktive Navigationslinks sollten `aria-current="page"` erhalten, damit Screenreader die aktuelle Position kommunizieren koennen.

**Empfehlung:**

```vue
<router-link
  :to="item.to"
  :aria-current="isActive(item) ? 'page' : undefined"
>
```

---

### F-31: `setInterval` in NotificationBell ohne Cleanup

**Severity: Enhancement**
**Betroffene Dateien:** `/frontend/src/components/common/NotificationBell.vue` (Zeile 18)

Das `setInterval` fuer den Unread-Count-Polling wird im `onMounted`-Hook gestartet, aber nie aufgeraeumt:

```typescript
onMounted(() => {
  notifications.fetchUnreadCount()
  setInterval(() => notifications.fetchUnreadCount(), 30000)
})
```

Dies fuehrt zu Memory-Leaks und laufenden Requests, auch wenn die Komponente nicht mehr sichtbar ist.

**Empfehlung:**

```typescript
import { onMounted, onUnmounted } from 'vue'

let pollInterval: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  notifications.fetchUnreadCount()
  pollInterval = setInterval(() => notifications.fetchUnreadCount(), 30000)
})

onUnmounted(() => {
  if (pollInterval) clearInterval(pollInterval)
})
```

---

## Zusammenfassung der Prioritaeten

### Sofort beheben (Critical)

| ID | Finding | Aufwand |
|---|---|---|
| F-09 | Farbkontrast `--mw-text-muted` FAIL | Klein (1 CSS-Variable) |
| F-10 | Fehlende Label-Verknuepfungen | Mittel (alle Formulare) |
| F-01 | Sidebar Active-State Bug | Klein (Logik-Fix) |
| F-21 | Mobile-Navigation unvollstaendig | Mittel (Mehr-Menue) |

### Naechste Iteration (Major)

| ID | Finding | Aufwand |
|---|---|---|
| F-02 | Endlos-Loading ohne Timeout | Mittel |
| F-03 | Loeschen ohne Bestaetigung | Klein |
| F-11 | PostComposer ohne Labels | Klein |
| F-12 | Fehlender Skip-Link | Klein |
| F-13 | NotificationBell ohne aria-label | Klein |
| F-14 | Klickbare Divs ohne Tastaturzugang | Mittel |
| F-22 | Header ueberlaedt auf Mobile | Mittel |
| F-23 | Nachrichten-Layout auf Mobile | Mittel |
| F-24 | Buttons ueberlappen Bottom-Nav | Klein |
| F-08 | Inkonsistentes Styling (CleaningView) | Mittel |

### Spaeter (Minor + Enhancement)

| ID | Finding | Aufwand |
|---|---|---|
| F-04 bis F-07 | Diverse UX-Verbesserungen | Klein-Mittel |
| F-15 bis F-20 | Weitere A11y-Optimierungen | Klein |
| F-25 bis F-31 | Konsistenz und Code-Qualitaet | Klein |

---

## Test-Empfehlungen

Fuer zukuenftige Qualitaetssicherung wird empfohlen:

1. **axe-core Integration** in Vitest fuer automatisierte A11y-Tests
2. **Lighthouse CI** in der GitHub Actions Pipeline fuer Performance- und A11y-Scores
3. **Manueller Screenreader-Test** (VoiceOver auf macOS, NVDA auf Windows) fuer die wichtigsten User-Flows
4. **Tastaturnavigation-Test** bei jedem neuen Feature
5. **Kontrastpruefung** bei jeder Aenderung am Farbschema

---

## Anhang: Screenshots

Die Screenshots befinden sich unter `docs/audit-screenshots/`:

| Datei | Beschreibung |
|---|---|
| `01-dashboard-desktop.png` | Dashboard Desktop-Ansicht |
| `01b-dashboard-loading-state.png` | Dashboard mit dauerhaftem Loading-Spinner |
| `02-rooms-desktop.png` | Raeume mit Loading-State |
| `03-family-desktop.png` | Familie mit falschem Active-State |
| `04-calendar-desktop.png` | Kalender Desktop |
| `05-admin-desktop.png` | Admin-Dashboard |
| `06-messages-desktop.png` | Nachrichten Desktop (Two-Panel) |
| `07-dashboard-mobile.png` | Dashboard Mobile (390x844) |
| `08-calendar-mobile.png` | Kalender Mobile |
| `09-jobs-mobile.png` | Jobboerse Mobile (zeigt geladene Daten) |
| `10-messages-mobile.png` | Nachrichten Mobile (Layout-Problem) |
| `11-profile-mobile.png` | Profil Mobile |
| `12-cleaning-mobile.png` | Putz-Orga Mobile |
| `13-form-create-mobile.png` | Formular erstellen Mobile |
