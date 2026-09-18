# Pandora6ix

Pandora6ix is a software-development practicum project. This repository currently contains shared engineering foundation only; product requirements and business functions arrive through reviewed OpenSpec changes.

## Technology stack

- Android: Kotlin and Jetpack Compose
- Web administration: React, TypeScript, and Vite
- Backend: Java 21, Spring Boot, and Maven
- Database: MySQL is under evaluation; no database connection or schema is configured yet

## Repository layout

```text
android/   Android client (future LoFi work is on the LoFi branch)
web/       React administration client
backend/   Spring Boot API foundation
docs/      Requirements, architecture, API, database, meeting, and test documents
openspec/  Specification-driven change records and main specifications
.github/   Pull-request collaboration template
```

## Start the modules

### Android

Open `android/` in Android Studio. Configure a local Android SDK (not committed) and run `app` on an emulator or device. The project includes a Gradle wrapper, so no global Gradle installation is needed.

### Web

```powershell
cd web
npm install
npm run dev
```

Use `npm run build` for a production build.

### Backend

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

Liveness is available at `GET /health`; Actuator exposes `GET /actuator/health`. Neither requires MySQL configuration.

## OpenSpec

OpenSpec is initialized in `openspec/`. In Codex use `/opsx:propose`; in a terminal use `openspec new change <kebab-case-name>`. [AGENTS.md](AGENTS.md) defines proposal, implementation, validation, review, and archive workflow.

## Branches and pull requests

`main` is the stable project branch. `LoFi` is the Android low-fidelity prototype branch. Work in focused feature or documentation branches, then open a PR. Except for urgent fixes, a non-author must review before merging. The PR template records linked issues and OpenSpec changes, testing, interface/requirement impact, and review results.
