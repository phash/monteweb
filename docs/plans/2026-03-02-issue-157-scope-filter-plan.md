# Issue #157 — Calendar Scope Filter Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a MultiSelect scope filter to CalendarView so users can filter events by school-wide, section, or room.

**Architecture:** Frontend-only change. A PrimeVue MultiSelect with grouped options (School / Sections / Rooms) filters the already-loaded events client-side. No backend or API changes needed. EventCreateView section dropdown already exists — no changes there.

**Tech Stack:** Vue 3.5 + TypeScript 5.9 + PrimeVue 4 (MultiSelect component) + Vitest

---

## Task 1: Add i18n keys for scope filter

**Files:**
- Modify: `frontend/src/i18n/de.ts:1099` (before `showJobs`)
- Modify: `frontend/src/i18n/en.ts:1099` (before `showJobs`)

**Step 1: Add German i18n keys**

In `frontend/src/i18n/de.ts`, inside the `calendar:` object, add before the `showJobs` line (line 1099):

```typescript
    filterPlaceholder: 'Bereiche & Räume filtern',
    scopeSchool: 'Schulweit',
    scopeSections: 'Bereiche',
    scopeRooms: 'Räume',
```

**Step 2: Add English i18n keys**

In `frontend/src/i18n/en.ts`, inside the `calendar:` object, add before the `showJobs` line (line 1099):

```typescript
    filterPlaceholder: 'Filter scopes & rooms',
    scopeSchool: 'School-wide',
    scopeSections: 'Sections',
    scopeRooms: 'Rooms',
```

**Step 3: Verify TypeScript compiles**

Run: `cd frontend && npx vue-tsc --noEmit`
Expected: No errors

**Step 4: Commit**

```bash
git add frontend/src/i18n/de.ts frontend/src/i18n/en.ts
git commit -m "feat(calendar): #157 — add i18n keys for scope filter"
```

---

## Task 2: Add scope filter logic to CalendarView script

**Files:**
- Modify: `frontend/src/views/CalendarView.vue` (script section)

**Step 1: Add imports**

At the top of `<script setup>` (around line 10, after existing imports), add:

```typescript
import MultiSelect from 'primevue/multiselect'
import { sectionsApi } from '@/api/sections.api'
import { useRoomsStore } from '@/stores/rooms'
```

**Step 2: Add rooms store and sections ref**

After `const admin = useAdminStore()` (line 31), add:

```typescript
const rooms = useRoomsStore()
const sections = ref<{ id: string; name: string }[]>([])
```

**Step 3: Add filter option types and refs**

After `const openJobs = ref<JobInfo[]>([])` (line 39), add:

```typescript
interface FilterOption {
  key: string
  label: string
}
interface FilterGroup {
  label: string
  items: FilterOption[]
}

const selectedFilters = ref<string[]>([])
```

**Step 4: Add filterOptions computed**

After the `jobboardEnabled` computed (around line 43), add:

```typescript
const filterGroups = computed<FilterGroup[]>(() => {
  const groups: FilterGroup[] = [
    { label: t('calendar.scopeSchool'), items: [{ key: 'SCHOOL', label: t('calendar.scopeSchool') }] },
  ]
  if (sections.value.length > 0) {
    groups.push({
      label: t('calendar.scopeSections'),
      items: sections.value.map(s => ({ key: `SECTION:${s.id}`, label: s.name })),
    })
  }
  if (rooms.myRooms.length > 0) {
    groups.push({
      label: t('calendar.scopeRooms'),
      items: rooms.myRooms.map(r => ({ key: `ROOM:${r.id}`, label: r.name })),
    })
  }
  return groups
})

const allFilterKeys = computed(() => filterGroups.value.flatMap(g => g.items.map(i => i.key)))
```

**Step 5: Update filteredEvents computed**

Replace the existing `filteredEvents` computed (lines ~125-128):

```typescript
// FROM:
const filteredEvents = computed(() => {
  if (showCleaning.value) return calendar.events
  return calendar.events.filter(e => e.eventType !== 'CLEANING')
})

// TO:
const filteredEvents = computed(() => {
  let result = calendar.events
  if (!showCleaning.value) {
    result = result.filter(e => e.eventType !== 'CLEANING')
  }
  // Scope filter — skip if all selected (no filtering needed)
  if (selectedFilters.value.length > 0 && selectedFilters.value.length < allFilterKeys.value.length) {
    result = result.filter(e => {
      const key = e.scope === 'SCHOOL' ? 'SCHOOL' : `${e.scope}:${e.scopeId}`
      return selectedFilters.value.includes(key)
    })
  }
  return result
})
```

**Step 6: Load sections and rooms in onMounted, init filter**

Find the existing `onMounted` (around line ~360). Add sections + rooms loading and filter initialization. The onMounted currently calls `loadEvents()` and `loadJobs()`. Add before those calls:

```typescript
// Inside onMounted, add at the beginning:
await rooms.fetchMyRooms()
try {
  const secRes = await sectionsApi.getAll()
  sections.value = secRes.data.data
} catch { /* ignore */ }
```

Then add a `watch` after the `onMounted` block to initialize `selectedFilters` once options are available:

```typescript
// Initialize filter with all options selected (after filterGroups is computed)
watch(allFilterKeys, (keys) => {
  if (selectedFilters.value.length === 0 && keys.length > 0) {
    selectedFilters.value = [...keys]
  }
}, { immediate: true })
```

**Step 7: Verify TypeScript compiles**

Run: `cd frontend && npx vue-tsc --noEmit`
Expected: No errors

**Step 8: Commit**

```bash
git add frontend/src/views/CalendarView.vue
git commit -m "feat(calendar): #157 — add scope filter logic to CalendarView"
```

---

## Task 3: Add scope filter UI to CalendarView template

**Files:**
- Modify: `frontend/src/views/CalendarView.vue` (template section)

**Step 1: Add MultiSelect to filter-toggle area**

Find the `<div class="filter-toggle">` block (around line ~548). Add the MultiSelect after the existing checkboxes, before the closing `</div>`:

```html
      <!-- Add after the showJobs template block, before </div> -->
      <span class="filter-separator" />
      <MultiSelect
        v-model="selectedFilters"
        :options="filterGroups"
        optionLabel="label"
        optionValue="key"
        optionGroupLabel="label"
        optionGroupChildren="items"
        :placeholder="t('calendar.filterPlaceholder')"
        display="chip"
        :maxSelectedLabels="2"
        :selectedItemsLabel="'{0}'"
        class="scope-filter"
      />
```

**Step 2: Add CSS for the scope filter**

In the `<style scoped>` section, add:

```css
.scope-filter {
  min-width: 200px;
  max-width: 350px;
}

@media (max-width: 600px) {
  .scope-filter {
    min-width: unset;
    width: 100%;
  }
}
```

**Step 3: Verify TypeScript compiles**

Run: `cd frontend && npx vue-tsc --noEmit`
Expected: No errors

**Step 4: Commit**

```bash
git add frontend/src/views/CalendarView.vue
git commit -m "feat(calendar): #157 — add MultiSelect scope filter to CalendarView UI"
```

---

## Task 4: Add tests for scope filter

**Files:**
- Modify: `frontend/src/views/__tests__/CalendarView.test.ts` (or create if not exists)

**Step 1: Check if CalendarView test exists**

Run: `ls frontend/src/views/__tests__/CalendarView.test.ts 2>/dev/null || echo "NOT FOUND"`

If it exists, add test cases. If not, create a new test file.

**Step 2: Write scope filter tests**

Add tests for the filtering logic. The key test: events are filtered based on scope+scopeId matching the selected filter keys.

```typescript
// Add to existing test file or create new:
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import de from '@/i18n/de'

// Test the filter logic in isolation
describe('CalendarView scope filter logic', () => {
  // Helper: simulate the filter function
  function filterEvents(
    events: Array<{ scope: string; scopeId: string | null; eventType: string }>,
    selectedFilters: string[],
    allFilterKeys: string[],
    showCleaning: boolean
  ) {
    let result = events
    if (!showCleaning) {
      result = result.filter(e => e.eventType !== 'CLEANING')
    }
    if (selectedFilters.length > 0 && selectedFilters.length < allFilterKeys.length) {
      result = result.filter(e => {
        const key = e.scope === 'SCHOOL' ? 'SCHOOL' : `${e.scope}:${e.scopeId}`
        return selectedFilters.includes(key)
      })
    }
    return result
  }

  const events = [
    { scope: 'SCHOOL', scopeId: null, eventType: 'GENERAL' },
    { scope: 'SECTION', scopeId: 'sec-1', eventType: 'GENERAL' },
    { scope: 'SECTION', scopeId: 'sec-2', eventType: 'GENERAL' },
    { scope: 'ROOM', scopeId: 'room-1', eventType: 'GENERAL' },
    { scope: 'ROOM', scopeId: 'room-2', eventType: 'CLEANING' },
  ]

  const allKeys = ['SCHOOL', 'SECTION:sec-1', 'SECTION:sec-2', 'ROOM:room-1', 'ROOM:room-2']

  it('shows all events when all filters selected', () => {
    const result = filterEvents(events, allKeys, allKeys, true)
    expect(result).toHaveLength(5)
  })

  it('filters to school-only events', () => {
    const result = filterEvents(events, ['SCHOOL'], allKeys, true)
    expect(result).toHaveLength(1)
    expect(result[0].scope).toBe('SCHOOL')
  })

  it('filters to specific section', () => {
    const result = filterEvents(events, ['SECTION:sec-1'], allKeys, true)
    expect(result).toHaveLength(1)
    expect(result[0].scopeId).toBe('sec-1')
  })

  it('filters to specific room', () => {
    const result = filterEvents(events, ['ROOM:room-1'], allKeys, true)
    expect(result).toHaveLength(1)
    expect(result[0].scopeId).toBe('room-1')
  })

  it('combines scope filter with cleaning toggle', () => {
    const result = filterEvents(events, ['ROOM:room-2'], allKeys, false)
    expect(result).toHaveLength(0) // room-2 event is CLEANING type
  })

  it('combines multiple filters', () => {
    const result = filterEvents(events, ['SCHOOL', 'ROOM:room-1'], allKeys, true)
    expect(result).toHaveLength(2)
  })

  it('shows all when selectedFilters is empty (no filter active yet)', () => {
    const result = filterEvents(events, [], allKeys, true)
    expect(result).toHaveLength(5)
  })
})
```

**Step 3: Run tests**

Run: `cd frontend && npx vitest run src/views/__tests__/CalendarView.test.ts`
Expected: All tests PASS

**Step 4: Run full test suite**

Run: `cd frontend && npx vitest run`
Expected: All existing tests still pass

**Step 5: Commit**

```bash
git add frontend/src/views/__tests__/CalendarView.test.ts
git commit -m "test(calendar): #157 — add scope filter unit tests"
```

---

## Task 5: Final verification and squash commit

**Step 1: TypeScript check**

Run: `cd frontend && npx vue-tsc --noEmit`
Expected: No errors

**Step 2: Full test suite**

Run: `cd frontend && npx vitest run`
Expected: All tests pass

**Step 3: Visual check (optional)**

Start dev server: `cd frontend && npm run dev`
Open http://localhost:5173, navigate to Calendar, verify:
- MultiSelect appears next to existing filter checkboxes
- Default: all scopes selected
- Deselecting a scope hides matching events
- Works in all 4 view modes (agenda, month, quarter, year)
