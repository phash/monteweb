# Issue #157 — Kalender Scope-Filter Design

**Datum:** 2026-03-02
**Issue:** #157 — Raum/Bereich auswählen bei Kalender
**Scope:** Frontend-only, CalendarView.vue

## Problem

CalendarView zeigt alle Events ohne Filtermöglichkeit nach Scope (Schulweit / Bereich / Raum). User können nicht gezielt nur Events eines bestimmten Raums oder Bereichs sehen.

**Hinweis:** EventCreateView hat den Section-Dropdown bereits implementiert (`sectionsApi.getAll()` + `sectionOptions` computed + `v-if="scope === 'SECTION'"` Select). Dieser Teil ist fertig.

## Lösung

PrimeVue MultiSelect mit gruppierten Optionen in CalendarView. Rein clientseitige Filterung der bereits geladenen Events.

## Änderungen

### CalendarView.vue

**Imports hinzufügen:**
- `MultiSelect` von `primevue/multiselect`
- `sectionsApi` von `@/api/sections.api`
- `useRoomsStore` von `@/stores/rooms`

**Neue Refs/Computeds:**
- `sections` ref: Array von `{ id, name }`, geladen in `onMounted`
- `rooms` store: `rooms.myRooms` (bereits vorhanden im Projekt)
- `filterOptions` computed: Gruppierte Optionen für MultiSelect
  ```
  [
    { label: 'Schulweit', items: [{ key: 'SCHOOL', label: 'Schulweit' }] },
    { label: 'Bereiche', items: [{ key: 'SECTION:uuid', label: 'Kinderhaus' }, ...] },
    { label: 'Räume', items: [{ key: 'ROOM:uuid', label: 'Sonnengruppe' }, ...] },
  ]
  ```
- `selectedFilters` ref: `string[]`, default = alle Keys (alles sichtbar)
- `allFilterKeys` computed: alle möglichen Keys (für Reset)

**Filterlogik:**
Erweitere das bestehende `filteredEvents` computed:
```typescript
const filteredEvents = computed(() => {
  let result = calendar.events
  // Bestehend: Cleaning-Toggle
  if (!showCleaning.value) {
    result = result.filter(e => e.eventType !== 'CLEANING')
  }
  // NEU: Scope-Filter
  if (selectedFilters.value.length < allFilterKeys.value.length) {
    result = result.filter(e => {
      const key = e.scope === 'SCHOOL' ? 'SCHOOL' : `${e.scope}:${e.scopeId}`
      return selectedFilters.value.includes(key)
    })
  }
  return result
})
```

**Template:**
MultiSelect neben dem ViewMode-SelectButton im Header-Bereich:
```html
<MultiSelect
  v-model="selectedFilters"
  :options="filterOptions"
  optionLabel="label"
  optionValue="key"
  optionGroupLabel="label"
  optionGroupChildren="items"
  :placeholder="t('calendar.filterPlaceholder')"
  display="chip"
  :maxSelectedLabels="2"
  class="scope-filter"
/>
```

### i18n

**de.ts:**
- `calendar.filterPlaceholder`: `'Bereiche & Räume filtern'`
- `calendar.scopeSchool`: `'Schulweit'`
- `calendar.scopeSections`: `'Bereiche'`
- `calendar.scopeRooms`: `'Räume'`

**en.ts:**
- `calendar.filterPlaceholder`: `'Filter scopes & rooms'`
- `calendar.scopeSchool`: `'School-wide'`
- `calendar.scopeSections`: `'Sections'`
- `calendar.scopeRooms`: `'Rooms'`

### Tests

CalendarView.test.ts: Scope-Filterlogik (Events werden korrekt nach Scope gefiltert, Default zeigt alle).

## Nicht im Scope

- Backend-Filterung (API bleibt unverändert)
- Pinia Store-Änderungen (Filter lebt lokal in CalendarView)
- EventCreateView-Änderungen (bereits fertig)
