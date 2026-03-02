# Rollenkonzept-Refactoring: Implementierungsplan

## Zusammenfassung der Änderungen

Basierend auf dem Handbuch-Review werden folgende Änderungen am Rollenkonzept implementiert.

---

## 1. Sonderrollen: ELTERNBEIRAT & PUTZORGA

**Was:** Zwei neue Sonderrollen als flexible Strings im `specialRoles`-Array.

**ELTERNBEIRAT-Rechte:**
- Events erstellen und bearbeiten
- Feed-Posts erstellen
- Putztermine erstellen, löschen und bearbeiten
- Formulare erstellen

**PUTZORGA-Rechte:**
- Putztermine erstellen, löschen, bearbeiten
- Zugriff auf Putz-Admin-Dashboard/Statistiken

**Dateien:**
- `CalendarService.java` → `checkCreatePermission()`: ELTERNBEIRAT darf SECTION/SCHOOL-Events erstellen
- `FormsService.java` → `checkCreatePermission()`: ELTERNBEIRAT darf Formulare erstellen
- `CleaningController.java` / `CleaningService.java` → Admin-Endpoints für ELTERNBEIRAT + PUTZORGA öffnen
- `JobboardController.java` → Posts erstellen für ELTERNBEIRAT
- Frontend: Menü-Sichtbarkeit anpassen

---

## 2. Raum-Beitrittspolitik (JoinPolicy)

**Was:** `discoverable` (Boolean) wird durch ein neues Enum `JoinPolicy` ersetzt.

**Enum-Werte:**
- `OPEN` — Jeder kann direkt beitreten
- `REQUEST` — Beitritt nur per Anfrage (LEADER genehmigt)
- `INVITE_ONLY` — Nur per Einladung durch LEADER

**Dateien:**
- Neues Enum: `JoinPolicy.java` im `room`-Paket
- `Room.java` → Feld `discoverable` ersetzen durch `joinPolicy`
- `RoomService.java` → Join-Logik anpassen
- `RoomController.java` → Browse-Endpoint anpassen
- Flyway-Migration V037: `ALTER TABLE rooms ADD COLUMN join_policy VARCHAR(20) DEFAULT 'REQUEST'; UPDATE rooms SET join_policy = CASE WHEN discoverable THEN 'OPEN' ELSE 'REQUEST' END; ALTER TABLE rooms DROP COLUMN discoverable;`
- Frontend: RoomSettings-UI anpassen

---

## 3. Diskussionsmodus (DiscussionMode)

**Was:** Neues Enum in den Raum-Einstellungen.

**Enum-Werte:**
- `FULL` — Threads + Antworten erlaubt
- `ANNOUNCEMENTS_ONLY` — Nur LEADER erstellt Threads, keine Antworten möglich
- `DISABLED` — Keine Diskussionen

**Dateien:**
- Neues Enum: `DiscussionMode.java`
- `RoomSettings.java` → Neues Feld `discussionMode` (Default: `FULL`)
- `DiscussionThreadService.java` → Prüfung vor Thread-Erstellung und Reply
- `DiscussionThreadController.java` → Endpoints prüfen Modus
- Flyway-Migration V037 (selbe Migration): Room-Settings-JSONB aktualisieren

---

## 4. Mitglieder-Themenerstellung (allowMemberThreadCreation)

**Was:** Neue Raum-Einstellung. Wenn aktiv, dürfen PARENTs Diskussionsthemen erstellen.

**Dateien:**
- `RoomSettings.java` → Neues Feld `allowMemberThreadCreation` (Default: `false`)
- `DiscussionThreadService.java` → `createThread()`: Nicht nur LEADER, sondern auch PARENT wenn Setting aktiv
- Frontend: Toggle in Raum-Einstellungen

---

## 5. Kinder-Diskussionsfreigabe (childDiscussionEnabled)

**Was:** Neue Raum-Einstellung. LEADER schaltet frei, ob STUDENT-User KINDER-Threads sehen/beantworten dürfen.

**Dateien:**
- `RoomSettings.java` → Neues Feld `childDiscussionEnabled` (Default: `false`)
- `DiscussionThreadService.java` → Thread-Sichtbarkeit: KINDER-Audience nur wenn `childDiscussionEnabled=true`
- Frontend: Toggle in Raum-Einstellungen

---

## 6. Raum-Feed-Abonnement (room_subscriptions)

**Was:** Neue Tabelle für Feed-Abonnements. User können Räume im Feed stumm schalten.

**Tabelle:**
```sql
CREATE TABLE room_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    room_id UUID NOT NULL REFERENCES rooms(id),
    feed_muted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, room_id)
);
```

**Dateien:**
- Neues Model: `RoomSubscription.java`
- Neues Repository: `RoomSubscriptionRepository.java`
- `RoomService.java` → Methoden für subscribe/unsubscribe
- `RoomController.java` → Endpoints `POST /rooms/{id}/mute`, `POST /rooms/{id}/unmute`
- `FeedService.java` → Feed-Query: gemutete Räume ausschließen
- Flyway-Migration V037
- Frontend: Mute-Button in Raum-Detail

---

## 7. Rollen-Einschränkungen: Keine Eltern-/Putzstunden

**Was:** SUPERADMIN, SECTION_ADMIN, TEACHER leisten keine Eltern- und Putzstunden.

**Backend-Änderungen:**
- `JobboardService.java` → `applyForJob()`: Verbieten für SUPERADMIN, SECTION_ADMIN, TEACHER
- `CleaningService.java` → `registerForSlot()`: Verbieten für SUPERADMIN, SECTION_ADMIN, TEACHER

**Frontend-Änderungen:**
- Dashboard: Job-/Putz-Widgets für diese Rollen ausblenden
- Jobbörse: "Bewerben"-Button für diese Rollen ausblenden
- Putz-Orga: "Anmelden"-Button für diese Rollen ausblenden

---

## 8. TEACHER → Auto-LEADER in KLASSE

**Was:** Wenn ein TEACHER zu einem KLASSE-Raum hinzugefügt wird, wird die Rolle automatisch auf LEADER gesetzt.

**Dateien:**
- `RoomService.java` → `addMember()`: Prüfung ob User TEACHER und Room.type == KLASSE → RoomRole.LEADER
- `RoomService.java` → `handleJoinRequestApproval()`: Gleiche Prüfung

---

## 9. Job-Berechtigungen

**IST → SOLL:**
- JEDER: Selbst erstellte Jobs dürfen gelöscht und geändert werden ✓ (bereits vorhanden)
- PARENT: Darf Jobs erstellen und erledigen ✓ (bereits vorhanden)
- SECTION_ADMIN: Darf Jobs löschen (NEU: nicht nur eigene), darf Jobs erstellen
- SUPERADMIN/SECTION_ADMIN/TEACHER: Keine Job-Bewerbung, keine Dashboard-Anzeige

**Dateien:**
- `JobboardService.java` → `deleteJob()`: SECTION_ADMIN darf beliebige Jobs löschen
- `JobboardService.java` → `applyForJob()`: Verbieten für SUPERADMIN, SECTION_ADMIN, TEACHER
- Frontend: Dashboard-Widget ausblenden

---

## 10. Familienverwaltung

**Was:** Nur SUPERADMIN und PARENT dürfen Familien verwalten. TEACHER/STUDENT darf keine Familie verwalten.

**Dateien:**
- `FamilyService.java` → `createFamily()`: Nur PARENT und SUPERADMIN
- `FamilyService.java` → Invite/Remove: Nur PARENT-Familienmitglieder und SUPERADMIN
- `FamilyController.java` → Endpoint-Guards
- Frontend: Familien-Menüpunkt nur für PARENT/SUPERADMIN

---

## 11. Messaging-Regeln

**IST → SOLL:**
- PARENT↔PARENT: Erlaubt (konfigurierbar, bleibt)
- PARENT↔TEACHER: Immer erlaubt ✓ (bereits vorhanden)
- TEACHER↔TEACHER: Immer erlaubt ✓ (bereits vorhanden)
- SUPERADMIN/SECTION_ADMIN: Uneingeschränkt ✓ (bereits vorhanden)
- STUDENT↔irgendwer: Standardmäßig verboten (konfigurierbar, bleibt)

**Ergebnis:** Keine Code-Änderung nötig. Bestehende Logik deckt dies bereits ab.

---

## 12. Diskussions-Berechtigungen

**IST → SOLL:**
- TEACHER: Darf Diskussionen in Räumen erstellen und verwalten (NEU: auch ohne LEADER-Rolle)
- LEADER: Darf Diskussionen erstellen/verwalten ✓
- PARENT: Darf Threads erstellen wenn `allowMemberThreadCreation=true` (NEU)

**Dateien:**
- `DiscussionThreadService.java` → `createThread()`: TEACHER darf auch ohne LEADER erstellen
- `DiscussionThreadService.java` → `archiveThread()`, `deleteThread()`: TEACHER darf auch ohne LEADER

---

## 13. Formulare-Berechtigungen

**IST → SOLL:**
- TEACHER: Darf Formulare erstellen ✓ (bereits für SECTION-Scope)
- SECTION_ADMIN: Darf Formulare erstellen ✓ (bereits vorhanden)
- LEADER: Darf Formulare in Raum erstellen ✓ (bereits vorhanden)
- ELTERNBEIRAT: Darf Formulare erstellen (NEU)

**Dateien:**
- `FormsService.java` → `checkCreatePermission()`: ELTERNBEIRAT-Check hinzufügen

---

## 14. Kalender/Events

**IST → SOLL:**
- ELTERNBEIRAT: Darf Events erstellen und bearbeiten (NEU)
- Bestehende Berechtigungen bleiben

**Dateien:**
- `CalendarService.java` → `checkCreatePermission()`: ELTERNBEIRAT-Check hinzufügen

---

## 15. Putz-Berechtigungen

**IST → SOLL:**
- ELTERNBEIRAT: Darf Putztermine erstellen, löschen, bearbeiten (NEU)
- PUTZORGA: Darf Putztermine verwalten + Admin-Dashboard (NEU)

**Dateien:**
- `CleaningController.java` → Admin-Endpoints für ELTERNBEIRAT/PUTZORGA öffnen
- `CleaningService.java` → Berechtigungsprüfungen anpassen

---

## Flyway-Migrationen (V037)

Eine kombinierte Migration für alle Schema-Änderungen:

```sql
-- 1. JoinPolicy (ersetzt discoverable)
ALTER TABLE rooms ADD COLUMN join_policy VARCHAR(20) NOT NULL DEFAULT 'REQUEST';
UPDATE rooms SET join_policy = CASE WHEN discoverable = true THEN 'OPEN' ELSE 'REQUEST' END;
ALTER TABLE rooms DROP COLUMN discoverable;

-- 2. Room Subscriptions
CREATE TABLE room_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    room_id UUID NOT NULL REFERENCES rooms(id),
    feed_muted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, room_id)
);
CREATE INDEX idx_room_subscriptions_user ON room_subscriptions(user_id);
CREATE INDEX idx_room_subscriptions_room ON room_subscriptions(room_id);
```

RoomSettings JSONB wird automatisch durch Hibernate verwaltet, neue Felder bekommen Defaults im Java-Code.

---

## Implementierungsreihenfolge

1. **Flyway-Migration V037** — Schema-Änderungen
2. **Enums & Models** — JoinPolicy, DiscussionMode, RoomSubscription
3. **RoomSettings erweitern** — discussionMode, allowMemberThreadCreation, childDiscussionEnabled
4. **Backend-Services** — Permission-Checks in allen Services anpassen
5. **Backend-Controller** — Neue Endpoints (mute/unmute), Guards aktualisieren
6. **Frontend-Stores** — Neue API-Calls
7. **Frontend-Views** — UI-Anpassungen (Settings, Sichtbarkeit, Buttons)
8. **Tests** — Backend- und Frontend-Tests anpassen
