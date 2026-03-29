# Business Rules

1. **Familienverbund = Abrechnungseinheit.** Stunden aus Jobboerse/Putz werden Familie gutgeschrieben (Putzstunden: Sonder-Unterkonto)
2. **Ein Elternteil = ein Familienverbund.** Kind kann mehreren zugeordnet sein (getrennte Eltern)
3. **Putz-Orga ist Opt-in**, nicht Rotation. Einmalig (mit Datum) oder wiederkehrend (Wochentag). DatePicker zeigt Feiertage (rot) und Schulferien (orange) je Bundesland
4. **Feed-Banner:** kontextabhaengig (Putz-Banner nur fuer betroffene Eltern)
5. **Module abschaltbar:** Backend via `@ConditionalOnProperty`, Frontend: Menue nur wenn Modul aktiv
6. **Kommunikationsregeln:** Lehrer<->Eltern immer erlaubt. Eltern<->Eltern / Schueler<->Schueler: konfigurierbar
7. **Kalender-Berechtigungen:** ROOM->LEADER/SUPERADMIN, SECTION->TEACHER/SUPERADMIN, SCHOOL->SUPERADMIN. Absage->Feed fuer alle, Loeschung->Feed nur fuer Zusager
8. **Raum-Beitrittsanfragen:** Non-Members anfragen, LEADER genehmigt/lehnt ab, auto-MEMBER bei Genehmigung
9. **Familien-Einladungen:** Per User-Suche mit Rollenwahl (PARENT/CHILD), Annehmen/Ablehnen via Notification
10. **Fotobox:** VIEW_ONLY < POST_IMAGES < CREATE_THREADS. LEADER/SUPERADMIN = CREATE_THREADS. MinIO, Thumbnails auto, Content-Type aus Magic Bytes, max 20 Dateien/Upload
11. **Targeted Feed Posts:** `feed_posts.target_user_ids UUID[]` -- NULL=fuer alle sichtbar, gefuellt=nur fuer diese User
12. **Audience-Sichtbarkeit:** Ordner und Fotobox-Threads haben `audience` (ALL, PARENTS_ONLY, STUDENTS_ONLY). Parents erstellen automatisch PARENTS_ONLY; Teachers/Leaders/Admins waehlen
13. **Multi-Section Forms:** SECTION-scoped Formulare koennen mehrere Schulbereiche via `section_ids UUID[]` targeten. Dashboard-Widget zeigt offene Formulare
14. **Auto-Folder Creation:** When a KLASSE room is created (`RoomCreatedEvent`), the files module automatically creates a default folder for the room
15. **Error Reporting:** Frontend errors are reported via `/api/v1/error-reports` with fingerprint-based deduplication. Admin can view, manage status (NEW/REPORTED/RESOLVED/IGNORED), and optionally create GitHub Issues via configured `github_repo` + `github_pat`
16. **Fundgrube (Lost & Found):** Schulweite Fundgrube mit Fotos, optionalem Bereichsfilter. Claim-Workflow (expires +24h via `@Scheduled` cleanup). MinIO image storage mit Thumbnails
17. **Chat-Bilder & Antworten:** Nachrichten koennen Bilder enthalten (multipart upload, MinIO, Thumbnails). Reply-Threading via `reply_to_id`. 90-Tage Auto-Cleanup fuer Bilder
18. **PWA:** Installierbar als Progressive Web App. Workbox Service Worker mit NetworkFirst-Caching fuer API-Calls. Install-Banner mit 7-Tage-Dismiss
19. **Mehrsprachigkeit:** `available_languages TEXT[]` bestimmt waehlbare Sprachen. LanguageSwitcher in Profil + Login (nicht Header). Nur sichtbar wenn >1 Sprache aktiviert
20. **Familien-Deaktivierung:** Familien koennen deaktiviert werden (`is_active`). Stundenkonto-Befreiung via `is_hours_exempt`
21. **Chat-Stummschaltung:** Conversations koennen stummgeschaltet werden (`conversation_participants.muted`). Mute-Toggle in DM-View und RoomChat-Header. Profilseite zeigt alle stummgeschalteten Chats mit Unmute-Buttons
22. **Feed-Anhaenge:** Posts koennen Datei-Anhaenge haben (MinIO upload, multi-file). Zwei-Schritt: Post erstellen -> Dateien hochladen
23. **Solr-Volltextsuche:** Apache Solr 9.8 mit deutscher Sprachanalyse (Stemming, Stopwords). 7 Dokumenttypen (USER, ROOM, POST, EVENT, FILE, WIKI, TASK). Echtzeit-Indexierung via Spring Events. Tika-Extraktion fuer Dateiinhalte (PDF, DOCX, etc.). Admin-Reindex via `POST /api/v1/admin/search/reindex`. Fallback auf DB-Suche wenn Solr deaktiviert
24. **Dark Mode:** Drei Modi (SYSTEM/LIGHT/DARK), gespeichert in `users.dark_mode`. CSS Custom Properties `--mw-*` schalten um. `useDarkMode` Composable. Auch auf Login-Seite waehlbar
25. **2FA (TOTP):** Drei Modi (DISABLED/OPTIONAL/MANDATORY). Bei MANDATORY 7-Tage Grace Period. Recovery Codes. Admin steuert Modus + Deadline
26. **Backup:** Docker-Profile `backup`. Taeglich pg_dump + MinIO mirror. Rotation: 7 taegliche, 4 woechentliche, 3 monatliche. Optional S3-Remote-Upload. Siehe `BACKUP.md`
27. **iCal-Subscriptions:** Externe Kalender via URL abonnieren, Events automatisch importieren (RFC 5545)
28. **CSV-Import:** Admin kann Benutzer per CSV importieren. Jeder User bekommt ein zufaelliges Passwort (24 Bytes SecureRandom, Base64) und `forcePasswordChange=true`. User muss Password-Reset nutzen, Flag wird bei Passwortaenderung zurueckgesetzt
29. **TOTP-Verschluesselung:** TOTP-Secrets werden mit AES-256-GCM verschluesselt gespeichert. `AesEncryptionService` behandelt Legacy-Klartext automatisch (Passthrough wenn kein `ENC(`-Prefix)
