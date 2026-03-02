# Issues #151–#154 Batch Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix SuperAdmin 403 bug, restrict email visibility by role, add PARTIALLY_ASSIGNED job status, and rework Admin Settings with Accordion groups.

**Architecture:** Four independent fixes touching backend services (Java/Spring) and frontend views (Vue 3/PrimeVue). Issues #153, #151, #154 are backend-first with minimal/no frontend changes. Issue #152 is frontend-only (template restructure). No database migrations needed.

**Tech Stack:** Java 21 + Spring Boot 3.4 + Spring Modulith | Vue 3.5 + TypeScript 5.9 + PrimeVue 4 Aura | Vitest

---

## Task 1: #153 — SuperAdmin 403 Fix (Backend)

Three services check room membership without bypassing for SUPERADMIN. The Fotobox module already does it correctly — replicate that pattern.

**Files:**
- Modify: `backend/src/main/java/com/monteweb/files/internal/service/FileService.java`
- Modify: `backend/src/main/java/com/monteweb/room/internal/service/RoomChatService.java`
- Modify: `backend/src/main/java/com/monteweb/calendar/internal/service/CalendarService.java`

**Reference pattern** (from `FotoboxPermissionService.java`):
```java
private boolean isSuperAdmin(UUID userId) {
    return userModule.findById(userId)
            .map(u -> u.role() == UserRole.SUPERADMIN)
            .orElse(false);
}
```

### Step 1: Fix FileService

`FileService` already has `UserModuleApi userModuleApi` injected (field, line 42). Add a private helper and update `requireRoomMembership`:

```java
// Add import at top:
import com.monteweb.user.UserRole;

// Add private helper (after line ~284):
private boolean isSuperAdmin(UUID userId) {
    return userModuleApi.findById(userId)
            .map(u -> u.role() == UserRole.SUPERADMIN)
            .orElse(false);
}

// Replace requireRoomMembership (lines 285-289):
private void requireRoomMembership(UUID userId, UUID roomId) {
    if (!isSuperAdmin(userId) && !roomModuleApi.isUserInRoom(userId, roomId)) {
        throw new ForbiddenException("You are not a member of this room");
    }
}
```

### Step 2: Fix RoomChatService

`RoomChatService` does NOT have `UserModuleApi` — only `RoomService` (internal) and `MessagingModuleApi`. Add the dependency and SUPERADMIN bypass.

```java
// Add imports:
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;

// Add field (alongside existing fields at line ~33):
private final UserModuleApi userModuleApi;
// @RequiredArgsConstructor will auto-inject it

// Add private helper:
private boolean isSuperAdmin(UUID userId) {
    return userModuleApi.findById(userId)
            .map(u -> u.role() == UserRole.SUPERADMIN)
            .orElse(false);
}
```

Update `getChannels` (lines 41-56) — bypass membership check AND channel filter for SUPERADMIN:

```java
@Transactional(readOnly = true)
public List<RoomChatChannelInfo> getChannels(UUID roomId, UUID userId) {
    boolean superAdmin = isSuperAdmin(userId);

    // Verify user is a member (SUPERADMIN bypasses)
    if (!superAdmin && !roomService.isUserInRoom(userId, roomId)) {
        throw new ForbiddenException("Not a member of this room");
    }

    List<RoomChatChannel> channels = channelRepository.findByRoomId(roomId);

    // SUPERADMIN sees all channels; others filtered by role
    if (superAdmin) {
        return channels.stream()
                .map(ch -> toChannelInfo(ch, userId))
                .toList();
    }

    RoomRole userRole = roomService.getUserRoleInRoom(userId, roomId).orElse(null);
    return channels.stream()
            .filter(ch -> canAccessChannel(ch.getChannelType(), userRole))
            .map(ch -> toChannelInfo(ch, userId))
            .toList();
}
```

Update `getOrCreateChannel` (lines 70-108) — bypass membership, channel access, and leader-only checks for SUPERADMIN:

```java
@Transactional
public RoomChatChannelInfo getOrCreateChannel(UUID roomId, UUID userId, ChannelType type) {
    boolean superAdmin = isSuperAdmin(userId);

    if (!superAdmin && !roomService.isUserInRoom(userId, roomId)) {
        throw new ForbiddenException("Not a member of this room");
    }

    Room room = roomService.findEntityById(roomId);
    if (!room.getSettings().chatEnabled()) {
        throw new BusinessException("Chat is not enabled for this room");
    }

    RoomRole userRole = roomService.getUserRoleInRoom(userId, roomId).orElse(null);
    if (!superAdmin && !canAccessChannel(type, userRole)) {
        throw new ForbiddenException("You don't have access to this channel");
    }

    // Check if channel exists
    Optional<RoomChatChannel> existing = channelRepository.findByRoomIdAndChannelType(roomId, type);
    if (existing.isPresent()) {
        return toChannelInfo(existing.get(), userId);
    }

    // Only leaders/SUPERADMIN can create parent/student channels
    if (type != ChannelType.MAIN && !superAdmin && userRole != RoomRole.LEADER) {
        throw new ForbiddenException("Only room leaders can create specialized channels");
    }

    // Create a real group conversation via the messaging module
    RoomChatChannel channel = new RoomChatChannel();
    channel.setRoomId(roomId);
    channel.setChannelType(type);

    String title = buildChannelTitle(room.getName(), type);
    List<UUID> memberIds = roomService.getMemberUserIds(roomId);
    var conversation = messagingModuleApi.createGroupConversation(title, userId, memberIds);
    channel.setConversationId(conversation.id());

    channel = channelRepository.save(channel);
    return toChannelInfo(channel, userId);
}
```

### Step 3: Fix CalendarService

`CalendarService` already has `UserModuleApi userModule` injected (field, line 38). Add helper and fix `getRoomEvents`:

```java
// Add import (if not present):
import com.monteweb.user.UserRole;

// Add private helper:
private boolean isSuperAdmin(UUID userId) {
    return userModule.findById(userId)
            .map(u -> u.role() == UserRole.SUPERADMIN)
            .orElse(false);
}

// Replace getRoomEvents (lines 75-81):
public Page<EventInfo> getRoomEvents(UUID roomId, UUID userId, LocalDate from, LocalDate to, Pageable pageable) {
    if (!isSuperAdmin(userId) && !roomModule.isUserInRoom(userId, roomId)) {
        throw new IllegalArgumentException("User is not a member of this room");
    }
    return eventRepository.findByRoomId(roomId, from, to, pageable)
            .map(e -> toEventInfo(e, userId));
}
```

### Step 4: Build and verify

```bash
cd backend && ./mvnw compile -q
```

### Step 5: Commit

```bash
git add backend/src/main/java/com/monteweb/files/internal/service/FileService.java \
       backend/src/main/java/com/monteweb/room/internal/service/RoomChatService.java \
       backend/src/main/java/com/monteweb/calendar/internal/service/CalendarService.java
git commit -m "fix(auth): #153 — bypass room membership check for SUPERADMIN in files, chat, calendar"
```

---

## Task 2: #151 — Email Visibility Filtering (Backend)

The `UserInfo` DTO always includes `email`. Directory/search endpoints expose it to all authenticated users. Fix: null out `email` when the caller is PARENT or STUDENT.

**Files:**
- Modify: `backend/src/main/java/com/monteweb/user/internal/service/UserService.java`
- Modify: `backend/src/main/java/com/monteweb/user/internal/controller/UserController.java`

### Step 1: Add filtered mapping method to UserService

The existing `toUserInfo` is private at line 610. Add a new public method that nulls email based on caller role. Also add a new public wrapper for directory and search that accepts the caller's role.

In `UserService.java`, add a new method after `toUserInfo` (after line 625):

```java
/**
 * Maps User to UserInfo, redacting email for non-elevated callers.
 * TEACHER, SECTION_ADMIN, SUPERADMIN see email; PARENT and STUDENT do not.
 */
private UserInfo toUserInfoFiltered(User user, UserRole callerRole) {
    boolean showContact = callerRole == UserRole.SUPERADMIN
            || callerRole == UserRole.SECTION_ADMIN
            || callerRole == UserRole.TEACHER;
    return new UserInfo(
            user.getId(),
            showContact ? user.getEmail() : null,
            user.getFirstName(),
            user.getLastName(),
            user.getDisplayName(),
            showContact ? user.getPhone() : null,
            user.getAvatarUrl(),
            user.getRole(),
            user.getSpecialRolesAsSet(),
            user.getAssignedRolesAsSet(),
            user.isActive(),
            user.getDarkMode()
    );
}
```

Then modify `findDirectory` and `searchUsers` to accept a `UserRole callerRole` parameter and use `toUserInfoFiltered`. Find these methods and add the parameter:

For `findDirectory` (around line ~130, returns `Page<UserInfo>`):
- Add `UserRole callerRole` parameter
- Change `.map(this::toUserInfo)` to `.map(u -> toUserInfoFiltered(u, callerRole))`

For `searchUsers` (around line ~150, returns `Page<UserInfo>`):
- Add `UserRole callerRole` parameter
- Change `.map(this::toUserInfo)` to `.map(u -> toUserInfoFiltered(u, callerRole))`

For `findById` (around line ~170, returns `Optional<UserInfo>`):
- Add overload `findById(UUID id, UserRole callerRole)` that uses `toUserInfoFiltered`
- Keep existing `findById(UUID id)` unchanged (used internally by ModuleApi)

### Step 2: Update UserController to pass caller role

In `UserController.java`, add a helper to extract the caller's UserRole:

```java
private UserRole getCallerRole() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return null;
    return auth.getAuthorities().stream()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .map(r -> { try { return UserRole.valueOf(r); } catch (Exception e) { return null; } })
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
}
```

Update the three endpoints:

**`GET /directory`** (line 99): Pass caller role to service:
```java
var page = userService.findDirectory(UserRole.fromStringOrNull(role), sectionId, roomId, q, pageable, getCallerRole());
```

**`GET /search`** (line 114): Pass caller role:
```java
var page = userService.searchUsers(q, pageable, getCallerRole());
```

**`GET /{id}`** (line 122): For self-access use unfiltered; for others use filtered:
```java
var user = id.equals(currentUserId)
    ? userService.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id))
    : userService.findById(id, getCallerRole()).orElseThrow(() -> new ResourceNotFoundException("User", id));
```

### Step 3: Build and verify

```bash
cd backend && ./mvnw compile -q
```

### Step 4: Commit

```bash
git add backend/src/main/java/com/monteweb/user/internal/service/UserService.java \
       backend/src/main/java/com/monteweb/user/internal/controller/UserController.java
git commit -m "fix(user): #151 — hide email/phone from PARENT and STUDENT in directory/search"
```

---

## Task 3: #154 — PARTIALLY_ASSIGNED Job Status (Backend)

Add intermediate `PARTIALLY_ASSIGNED` status when some but not all job slots are filled. No DB migration needed — status is stored as VARCHAR (`@Enumerated(EnumType.STRING)`).

**Files:**
- Modify: `backend/src/main/java/com/monteweb/jobboard/JobStatus.java`
- Modify: `backend/src/main/java/com/monteweb/jobboard/internal/service/JobboardService.java`

### Step 1: Add enum value

In `JobStatus.java`, add `PARTIALLY_ASSIGNED` between `OPEN` and `ASSIGNED`:

```java
public enum JobStatus {
    OPEN,
    PARTIALLY_ASSIGNED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
```

### Step 2: Fix applyForJob

In `JobboardService.java`, method `applyForJob` (~line 326):

**Fix 1 — Allow applying when PARTIALLY_ASSIGNED** (line ~343):
```java
// BEFORE:
if (job.getStatus() != JobStatus.OPEN) {
    throw new BusinessException("This job is no longer open for applications");
}

// AFTER:
if (job.getStatus() != JobStatus.OPEN && job.getStatus() != JobStatus.PARTIALLY_ASSIGNED) {
    throw new BusinessException("This job is no longer open for applications");
}
```

**Fix 2 — Set intermediate status** (lines ~403-407):
```java
// BEFORE:
if (currentAssignees + 1 >= job.getMaxAssignees()) {
    job.setStatus(JobStatus.ASSIGNED);
    jobRepository.save(job);
}

// AFTER:
long newCount = currentAssignees + 1;
if (newCount >= job.getMaxAssignees()) {
    job.setStatus(JobStatus.ASSIGNED);
} else {
    job.setStatus(JobStatus.PARTIALLY_ASSIGNED);
}
jobRepository.save(job);
```

### Step 3: Fix cancelAssignment

In `JobboardService.java`, method `cancelAssignment` (~line 514), replace the re-open block (lines ~556-561):

```java
// BEFORE:
if (activeAssignees < job.getMaxAssignees() && job.getStatus() != JobStatus.COMPLETED && job.getStatus() != JobStatus.CANCELLED) {
    job.setStatus(JobStatus.OPEN);
    jobRepository.save(job);
}

// AFTER:
if (activeAssignees < job.getMaxAssignees() && job.getStatus() != JobStatus.COMPLETED && job.getStatus() != JobStatus.CANCELLED) {
    job.setStatus(activeAssignees > 0 ? JobStatus.PARTIALLY_ASSIGNED : JobStatus.OPEN);
    jobRepository.save(job);
}
```

### Step 4: Fix rejectAssignment

In `JobboardService.java`, method `rejectAssignment` (~line 491), same pattern — replace the re-open block:

```java
// BEFORE:
if (activeAssignees < job.getMaxAssignees()
        && job.getStatus() != JobStatus.COMPLETED
        && job.getStatus() != JobStatus.CANCELLED) {
    job.setStatus(JobStatus.OPEN);
    jobRepository.save(job);
}

// AFTER:
if (activeAssignees < job.getMaxAssignees()
        && job.getStatus() != JobStatus.COMPLETED
        && job.getStatus() != JobStatus.CANCELLED) {
    job.setStatus(activeAssignees > 0 ? JobStatus.PARTIALLY_ASSIGNED : JobStatus.OPEN);
    jobRepository.save(job);
}
```

### Step 5: Build and run backend tests

```bash
cd backend && ./mvnw compile -q
cd backend && ./mvnw test -Dtest="JobboardServiceTest,JobboardServiceHoursTest" -q
```

Fix any test failures — existing tests may assert `JobStatus.OPEN` where it's now `PARTIALLY_ASSIGNED`. Update test expectations accordingly (e.g., `rejectAssignment_successReopensJob` may need to expect `PARTIALLY_ASSIGNED` instead of `OPEN` if there are remaining assignees).

### Step 6: Commit

```bash
git add backend/src/main/java/com/monteweb/jobboard/JobStatus.java \
       backend/src/main/java/com/monteweb/jobboard/internal/service/JobboardService.java
git commit -m "feat(jobboard): #154 — add PARTIALLY_ASSIGNED status for partially filled jobs"
```

---

## Task 4: #154 — PARTIALLY_ASSIGNED Job Status (Frontend)

**Files:**
- Modify: `frontend/src/types/jobboard.ts` (line 1)
- Modify: `frontend/src/i18n/de.ts` (jobboard.statuses section)
- Modify: `frontend/src/i18n/en.ts` (jobboard.statuses section)
- Modify: `frontend/src/views/JobBoardView.vue` (statusSeverity function)
- Modify: `frontend/src/views/JobDetailView.vue` (statusSeverity function)

### Step 1: Add type

In `frontend/src/types/jobboard.ts`, line 1:
```typescript
// BEFORE:
export type JobStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

// AFTER:
export type JobStatus = 'OPEN' | 'PARTIALLY_ASSIGNED' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
```

### Step 2: Add i18n keys

In `frontend/src/i18n/de.ts`, in `jobboard.statuses`:
```typescript
statuses: {
  OPEN: 'Offen',
  PARTIALLY_ASSIGNED: 'Teilweise vergeben',
  ASSIGNED: 'Vergeben',
  IN_PROGRESS: 'In Arbeit',
  COMPLETED: 'Abgeschlossen',
  CANCELLED: 'Abgesagt',
},
```

In `frontend/src/i18n/en.ts`, in `jobboard.statuses`:
```typescript
statuses: {
  OPEN: 'Open',
  PARTIALLY_ASSIGNED: 'Partially Assigned',
  ASSIGNED: 'Assigned',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
},
```

### Step 3: Add severity in both views

In `JobBoardView.vue` (lines 208-222), add case to `statusSeverity`:
```typescript
function statusSeverity(status: string) {
  switch (status) {
    case 'OPEN': return 'success'
    case 'PARTIALLY_ASSIGNED': return 'warn'
    case 'ASSIGNED': return 'info'
    case 'IN_PROGRESS': return 'warn'
    case 'COMPLETED': return 'secondary'
    case 'CANCELLED': return 'danger'
    default: return 'info'
  }
}
```

In `JobDetailView.vue` (lines 201-215), same change:
```typescript
function statusSeverity(status: string) {
  switch (status) {
    case 'OPEN': return 'success'
    case 'PARTIALLY_ASSIGNED': return 'warn'
    case 'ASSIGNED': return 'info'
    case 'IN_PROGRESS': return 'warn'
    case 'COMPLETED': return 'secondary'
    case 'CANCELLED': return 'danger'
    default: return 'info'
  }
}
```

### Step 4: TypeScript check and tests

```bash
cd frontend && npx vue-tsc --noEmit
cd frontend && npx vitest run --reporter=verbose 2>&1 | tail -5
```

### Step 5: Commit

```bash
git add frontend/src/types/jobboard.ts \
       frontend/src/i18n/de.ts frontend/src/i18n/en.ts \
       frontend/src/views/JobBoardView.vue \
       frontend/src/views/JobDetailView.vue
git commit -m "feat(jobboard): #154 — frontend support for PARTIALLY_ASSIGNED status"
```

---

## Task 5: #152 — Admin Settings Accordion UI Rework (Frontend)

Replace the flat `.settings-section` layout with PrimeVue Accordion grouped into 5 logical categories. Settings logic (store, API calls, refs) remains **completely unchanged** — only the template is restructured.

**Files:**
- Modify: `frontend/src/views/admin/AdminSettings.vue`

**PrimeVue Accordion API** (already used in `AdminSections.vue`):
```vue
<Accordion multiple :value="openPanels">
  <AccordionPanel value="key">
    <AccordionHeader>Header text</AccordionHeader>
    <AccordionContent>Content</AccordionContent>
  </AccordionPanel>
</Accordion>
```

### Step 1: Add Accordion imports

Add to the import section (after existing PrimeVue imports):
```typescript
import Accordion from 'primevue/accordion'
import AccordionPanel from 'primevue/accordionpanel'
import AccordionHeader from 'primevue/accordionheader'
import AccordionContent from 'primevue/accordioncontent'
```

### Step 2: Add panel state

In `<script setup>`, add (alongside existing refs):
```typescript
const openPanels = ref(['general', 'communication'])
```

This opens "Allgemein" and "Kommunikation" by default.

### Step 3: Remove ldapExpanded ref

Remove the `ldapExpanded` ref (line 67) — no longer needed since LDAP will be inside an AccordionPanel.

### Step 4: Restructure template

Replace everything between `<template>` tags. The page title stays at the top, then one `<Accordion>` with 5 `<AccordionPanel>` groups:

**Group mapping (14 sections → 5 groups):**

| Group | key | Icon | Sections included |
|-------|-----|------|-------------------|
| Allgemein | `general` | `pi-cog` | Language, Registration, Bundesland/Vacations |
| Kommunikation | `communication` | `pi-comments` | Directory, Communication rules, Jobboard, Family |
| Integrationen | `integration` | `pi-link` | Jitsi, WOPI, ClamAV, LDAP |
| Sicherheit | `security` | `pi-shield` | 2FA, Maintenance Mode |
| Stunden | `hours` | `pi-clock` | Hours configuration |

Template structure (pseudocode — keep all existing form controls verbatim, just rewrap):

```html
<template>
  <div class="p-4">
    <h1 class="text-2xl font-bold mb-6">{{ t('admin.settings.title') }}</h1>

    <Accordion multiple :value="openPanels">
      <!-- 1. Allgemein -->
      <AccordionPanel value="general">
        <AccordionHeader>
          <span class="flex items-center gap-2">
            <i class="pi pi-cog" />
            {{ t('admin.settings.groups.general') }}
          </span>
        </AccordionHeader>
        <AccordionContent>
          <!-- Language section content (lines 433-454 inner content) -->
          <!-- Registration section content (lines 459-466 inner content) -->
          <!-- Bundesland & Vacations section content (lines 664-708 inner content) -->
          <Button :label="t('common.save')" icon="pi pi-check" :loading="saving" @click="saveSettings" class="mt-4" />
        </AccordionContent>
      </AccordionPanel>

      <!-- 2. Kommunikation -->
      <AccordionPanel value="communication">
        <AccordionHeader>
          <span class="flex items-center gap-2">
            <i class="pi pi-comments" />
            {{ t('admin.settings.groups.communication') }}
          </span>
        </AccordionHeader>
        <AccordionContent>
          <!-- Directory section content (lines 471-478 inner content) -->
          <!-- Communication section content (lines 483-497, with v-if for messaging) -->
          <!-- Jobboard section content (lines 502-509 inner content) -->
          <!-- Family section content (lines 514-528 inner content) -->
          <Button :label="t('common.save')" icon="pi pi-check" :loading="saving" @click="saveSettings" class="mt-4" />
        </AccordionContent>
      </AccordionPanel>

      <!-- 3. Integrationen -->
      <AccordionPanel value="integration">
        <AccordionHeader>
          <span class="flex items-center gap-2">
            <i class="pi pi-link" />
            {{ t('admin.settings.groups.integration') }}
          </span>
        </AccordionHeader>
        <AccordionContent>
          <!-- Jitsi section (v-if jitsi enabled, lines 406-414, keep own Save) -->
          <!-- WOPI section (v-if wopi enabled, lines 419-428, keep own Save) -->
          <!-- ClamAV section (v-if clamav enabled, lines 387-401, keep own Save) -->
          <!-- LDAP section (lines 562-641, remove manual expand logic, keep own Save) -->
        </AccordionContent>
      </AccordionPanel>

      <!-- 4. Sicherheit -->
      <AccordionPanel value="security">
        <AccordionHeader>
          <span class="flex items-center gap-2">
            <i class="pi pi-shield" />
            {{ t('admin.settings.groups.security') }}
          </span>
        </AccordionHeader>
        <AccordionContent>
          <!-- Maintenance section content (lines 370-382, keep own Save) -->
          <!-- 2FA section content (lines 533-549 inner content) -->
          <Button :label="t('common.save')" icon="pi pi-check" :loading="saving" @click="saveSettings" class="mt-4" />
        </AccordionContent>
      </AccordionPanel>

      <!-- 5. Stunden -->
      <AccordionPanel value="hours">
        <AccordionHeader>
          <span class="flex items-center gap-2">
            <i class="pi pi-clock" />
            {{ t('admin.settings.groups.hours') }}
          </span>
        </AccordionHeader>
        <AccordionContent>
          <!-- Hours section content (lines 648-659, keep own Save) -->
        </AccordionContent>
      </AccordionPanel>
    </Accordion>
  </div>
</template>
```

**Important details:**
- Each section's inner form controls (ToggleSwitches, InputTexts, Selects, etc.) are moved verbatim — no changes to v-models, handlers, or bindings
- Sections that had their own Save buttons (Maintenance, ClamAV, Jitsi, WOPI, LDAP, Hours, Vacations) keep them inside their group
- The shared Save button (previously line 552) is duplicated into the "Allgemein" and "Kommunikation" groups since those groups contain settings saved by `saveSettings()`
- Remove the old `h2.text-lg.font-semibold` section headers — the AccordionHeader replaces them. Add `h3.text-md.font-medium` sub-headers within groups to separate sections (e.g., "Sprache", "Registrierung", "Ferien" within Allgemein)
- Remove the manual `ldapExpanded`/`ldapPasswordStored` expand logic — LDAP is naturally collapsed inside the Integrationen accordion. Keep the `ldapEnabled` toggle and conditional content
- Remove old `.settings-section` CSS class. Replace with `.settings-subsection` for separating items within a group

### Step 5: Add i18n keys for group names

In `frontend/src/i18n/de.ts`, inside `admin.settings`:
```typescript
groups: {
  general: 'Allgemein',
  communication: 'Kommunikation',
  integration: 'Integrationen',
  security: 'Sicherheit',
  hours: 'Stunden',
},
```

In `frontend/src/i18n/en.ts`, inside `admin.settings`:
```typescript
groups: {
  general: 'General',
  communication: 'Communication',
  integration: 'Integrations',
  security: 'Security',
  hours: 'Hours',
},
```

### Step 6: Update CSS

Replace the old `.settings-section` styles with:

```css
<style scoped>
.settings-subsection {
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--mw-border-light);
}

.settings-subsection:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.settings-subsection label {
  margin-bottom: 0.125rem;
}

.settings-subsection small {
  display: block;
  line-height: 1.4;
}

.subsection-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
  color: var(--mw-text-secondary);
}
</style>
```

### Step 7: TypeScript check

```bash
cd frontend && npx vue-tsc --noEmit
```

### Step 8: Commit

```bash
git add frontend/src/views/admin/AdminSettings.vue \
       frontend/src/i18n/de.ts frontend/src/i18n/en.ts
git commit -m "feat(admin): #152 — rework settings UI with grouped Accordion layout"
```

---

## Task 6: Final Integration & Verification

### Step 1: Run all frontend tests

```bash
cd frontend && npx vitest run --reporter=verbose 2>&1 | tail -20
```

### Step 2: TypeScript check

```bash
cd frontend && npx vue-tsc --noEmit
```

### Step 3: Vite production build

```bash
cd frontend && npx vite build
```

### Step 4: Docker build (full stack)

```bash
docker compose build
```

### Step 5: Verify running app

```bash
docker compose up -d
```

Test manually:
- Login as `admin@monteweb.local` (SUPERADMIN, password: admin123)
- Navigate to a room's files/chat/calendar — should work without 403
- Check directory — email should be visible
- Login as `eltern@monteweb.local` (PARENT, password: test1234)
- Check directory — email should be hidden
- Check jobboard — apply for a multi-slot job, verify "Teilweise vergeben" status
- Login as admin, go to Admin Settings — verify Accordion groups

### Step 6: Commit any remaining fixes and push

```bash
git push origin main
```
