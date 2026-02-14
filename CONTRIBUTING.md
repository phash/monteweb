# Contributing to MonteWeb

Vielen Dank fuer dein Interesse an MonteWeb! Beitraege sind willkommen — ob Bugfixes, Verbesserungen oder neue Features.

## Grundsaetze

- **Ich entscheide, welche Features passen.** MonteWeb hat eine klare Vision als Schul-Intranet fuer Montessori-Schulkomplexe. Nicht jede Idee passt in dieses Konzept. Bitte nimm es nicht persoenlich, wenn ein Beitrag abgelehnt wird.
- **Qualitaet vor Quantitaet.** Lieber ein gut durchdachter Beitrag als zehn halbfertige.
- **Issue zuerst.** Bevor du an einem groesseren Feature arbeitest, erstelle bitte zuerst ein Issue und warte auf Feedback. So vermeidest du unnoetige Arbeit.

## Workflow

1. **Fork** das Repository
2. **Branch** erstellen: `git checkout -b feature/mein-feature` oder `fix/mein-bugfix`
3. **Aenderungen** vornehmen (siehe Coding-Standards unten)
4. **Tests** schreiben/aktualisieren und sicherstellen, dass alle Tests bestehen
5. **Pull Request** gegen `main` erstellen

### Branch-Namenskonventionen

- `feature/beschreibung` — Neue Features
- `fix/beschreibung` — Bugfixes
- `docs/beschreibung` — Dokumentation
- `refactor/beschreibung` — Refactoring ohne Funktionsaenderung

## Coding-Standards

### Backend (Java / Spring Boot)

- Java 21, Spring Boot 3.4, Spring Modulith
- **Module-Grenzen respektieren:** Niemals aus dem `internal/`-Package eines anderen Moduls importieren
- Cross-Modul-Kommunikation nur ueber `*ModuleApi` Facades (synchron) oder Spring Events (asynchron)
- Records fuer DTOs, Lombok fuer Entities
- UUIDs als Primary Keys, `Instant` fuer Timestamps
- Bean Validation auf Request-DTOs
- Tests mit Testcontainers + MockMvc

### Frontend (Vue / TypeScript)

- Vue 3.5 Composition API mit `<script setup lang="ts">`
- PrimeVue 4 (Aura Theme) fuer UI-Komponenten
- Pinia Composition Stores (ein Store pro Domaene)
- Alle User-facing Texte ueber `vue-i18n` (Deutsch + Englisch)
- `@/` Path-Alias fuer Imports
- Tests mit Vitest + @vue/test-utils

### Allgemein

- **Commits:** Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`)
- **Code:** Englisch. **UI-Texte:** Deutsch + Englisch (i18n)
- **API:** REST unter `/api/v1/`, `ApiResponse<T>` Wrapper

## Tests

Vor dem PR muessen alle Tests bestehen:

```bash
# Frontend (917+ Tests)
cd frontend && npm test

# Backend (Testcontainers, Docker erforderlich)
cd backend && ./mvnw test
```

## Contributor License Agreement (CLA)

Mit dem Einreichen eines Pull Requests stimmst du zu, dass:

1. **Du der Urheber** deiner Beitraege bist (oder die Erlaubnis hast, sie beizutragen)
2. **Du die Rechte an deinem Code** dem Projekt unter der Apache License 2.0 uebertraegst
3. **Der Maintainer das Recht hat**, die Lizenz des Gesamtprojekts in Zukunft zu aendern, einschliesslich deiner Beitraege
4. **Du keine Verguetunsansprueche** fuer deine Beitraege geltend machst

Diese Vereinbarung gilt automatisch mit dem Erstellen eines Pull Requests. Wenn du damit nicht einverstanden bist, reiche bitte keinen Pull Request ein.

## Code of Conduct

- Sei respektvoll und konstruktiv
- Konzentriere dich auf den Code, nicht auf die Person
- Hilf anderen, besser zu werden

## Fragen?

Erstelle ein [Issue](https://github.com/phash/monteweb/issues) — ich antworte so schnell wie moeglich.

---

Danke, dass du MonteWeb besser machst!
