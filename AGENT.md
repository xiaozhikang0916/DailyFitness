# AGENT.md

Guide for AI coding agents working on the **DailyFitness** Android project.

## Project Overview

DailyFitness is a local-first Android app for recording daily fitness workouts and body data.
Users define their own train parts (muscle groups) and actions (exercises), log workout sets and
body metrics (weight, etc.), and view history/statistics. An optional, opt-in **AI Coach**
feature recommends today's plan or the next set using an LLM (DeepSeek by default).

- **Language:** Kotlin 2.4.20
- **UI:** Jetpack Compose (Material 3, Compose BOM `2026.08.00`)
- **Build:** Gradle (Kotlin DSL) + AGP `9.4.0`, JDK 17, `compileSdk = 37`, `minSdk = 29`
- **DI:** Hilt 2.60.1 (KSP)
- **Persistence:** Room 2.8.4 + DataStore Preferences
- **Navigation:** Navigation 3 (`androidx.navigation3`) with type-safe `NavKey` routes
- **Async:** Kotlin Coroutines / Flow
- **State machines:** FlowRedux 2.x
- **LLM:** Koog (`ai.koog`) + Ktor client
- **Charts:** Vico
- **License:** GPLv3

Version catalog: `gradle/libs.versions.toml`. Dependency versions **must** be added there, never
hardcoded in module build files.

## Module Structure

Gradle modules are declared in `settings.gradle`:

```
:app            Android application entry point (UI pages, navigation host, DI wiring)
:session        Workout session feature (foreground service + ongoing notification)
:database       Room database, DAOs, repository implementations, Hilt DB providers
:repository     Pure interfaces + domain models shared across modules (no Android deps)
:calendar       Reusable Compose calendar widgets
:chart          Reusable Compose line-chart widgets (Vico wrapper)
:settings       Settings logic + data definitions (import/export, settings bindings)
:settings-ui    Settings Compose UI + ViewModels (AI settings, data export)
:ai-coach       AI Coach domain layer (engine, prompts, LLM plan executor, config store)
:ai-coach-ui    AI Coach Compose UI + FlowRedux state machine
```

### Dependency direction

```
                    :app
   ┌─────────┬───────────┬────────────┬──────────┬─────────┬──────────┐
   ▼         ▼           ▼            ▼          ▼         ▼          ▼
:session :ai-coach-ui :settings-ui :settings :database :calendar  :chart
   │         │            │            │          │         │
   │         ▼            ▼            │          ▼         │
   │      :ai-coach     :settings      │      :repository ◄─┘
   │         │            │            │          ▲
   └─────────┴────────────┴────────────┴──────────┘
```

Rules of thumb:

- `:repository` is dependency-free (interfaces + models only) and is depended on by feature/data modules.
- `:database` **implements** the `:repository` interfaces and provides them via Hilt.
- `:settings` holds the data definitions and export/import logic only; its Compose UI
  (including the ViewModels) lives in `:settings-ui`, mirroring `:ai-coach` / `:ai-coach-ui`.
- `:settings-ui` never touches the platform file picker: the app injects an
  `ExportDirectoryProvider` (implemented with the Storage Access Framework in `:app`).
- `:app` is the only module that knows about `MainActivity`, `NavKey`s, and concrete DI graphs.
- Feature modules (`:session`, `:ai-coach`) expose interfaces/facades; `:app` wires entry points only.
- Never introduce an upward dependency (e.g. `:ai-coach` must not depend on `:app` or `:ai-coach-ui`).

## Key Directories

```
app/src/main/java/site/xiaozk/dailyfitness/
├── MainActivity.kt              # ComponentActivity, forwards intents to NavIntentBus
├── AppHost.kt                   # Root Compose: NavDisplay + composition locals
├── FitnessApplication.kt        # @HiltAndroidApp
├── nav/                         # Navigation3 routes, intent bus, app-side nav mappers
├── page/                        # Screen composables grouped by feature
│   ├── training/                # Home workout, monthly view, day detail, add-set flow
│   ├── body/                    # Body data pages
│   ├── action/                  # Train part/action library pages
│   ├── aicoach/                 # AI Coach host page
│   └── settings/                # Settings tab, AI settings + data export pages
├── widget/                      # Reusable app widgets (scaffold, FAB, dropdown, chips…)
├── theme/                       # Compose theme (Color/Type/Theme)
├── di/                          # App-level Hilt modules (AI Coach HTTP/DataStore/locale)
├── base/                        # BaseIntent / IntentResult / ActionStatus contracts
└── utils/                       # Formatting and unit helpers
```

Test sources mirror the main package under `src/test/java/...`. `src/debug` holds
`@PreviewParameterProvider`s and a seeded debug database (`database/src/debug/assets/fitness.db`).

## Architecture & Conventions

### Layering

`UI (Composable)` → `ViewModel / StateMachine` → `Repository interface` → `Room DAO / DataStore`.
Composables never touch DAOs directly; ViewModels never reference Android UI types.

### Navigation (Navigation 3)

- All destinations are `@Serializable` `NavKey` subtypes in `app/.../nav/Routes.kt`.
  Arguments are typed properties on the route (no string URLs / JSON encoding).
- `AppHost.kt` owns the `NavBackStack<NavKey>` and the `NavDisplay` entry provider.
  Start destination is `HomeTraining`; the back stack is flat.
- Bottom tabs are modeled by the `HomeTab` enum.
- Dialogs are local UI state, **not** nav destinations.
- The back stack must survive process death: keep routes `@Serializable` and register new
  ones in `NavKeySerializersModule` if the explicit serializers path is needed.
- Cross-module navigation is decoupled via capabilities:
  - `:session` defines `WorkoutSessionNavProvider` (PendingIntents), implemented by `:app`
    (`AppSessionNavProvider`).
  - Notification intents are routed through `NavIntentBus` and mapped to routes in
    `SessionNavMapper.kt`.

### State management

Two patterns coexist:

1. **Plain MVVM** for simple/read-mostly screens: `@HiltViewModel` + `Flow`/`StateFlow`
   (e.g. `HomeWorkoutPageViewModel`, `BodyViewModel`). UI state is often a `data class`
   with an `ActionStatus` (`Idle`/`Loading`/`Done`/`Failed`) from `base/BaseIntent.kt`.
2. **FlowRedux 2.x state machines** for stateful flows: implement
   `FlowReduxStateMachineFactory<State, Action>`. The machine instance is **stateless** —
   all durable state travels in the immutable state object (e.g. `AddWorkoutUiState`,
   `AiCoachUiState`). Use `@AssistedInject` + `@AssistedFactory` when the machine needs
   constructor parameters (e.g. an AI suggestion prefill). The older hand-rolled
   `IIntent`/`IntentResult` + reducer pattern (`base/BaseIntent.kt`) is legacy;
   new stateful flows should prefer FlowRedux.

State machines have focused unit tests (see `*StateMachineTest.kt`).

### Dependency injection (Hilt)

- `@HiltAndroidApp` on `FitnessApplication`; `@AndroidEntryPoint` on `MainActivity`.
- Interface bindings live in `@Module @InstallIn(SingletonComponent::class)` classes:
  - `:database` `RepoProvider` binds `I*Repository` → Room-backed implementations.
  - `:ai-coach` `AiCoachModule` binds config store, LLM session factory, plan executor, engine.
  - `:app` `di/` binds the app-provided implementations (HTTP client, DataStore, locale).
- Use `@Binds` for interfaces, `@Provides` for constructed values; annotate singletons.

### Persistence

- **Room** in `:database` (`AppDataBase`, version 2, auto-migration 1→2). Entities are `DB*`
  and map to `:repository` models. Schemas are exported to `database/schemas/` — commit them.
- Add a schema/migration whenever entities change; never bump the version without one.
- **DataStore** holds lightweight prefs: workout session state (`session/SessionStore.kt`) and
  the AI Coach config/API key (`ai-coach/config/DataStoreAiCoachConfigStore.kt`).
- The AI Coach API key is deliberately excluded from cloud backup and device transfer
  (`app/src/main/res/xml/data_extraction_rules.xml` + `backup_rules.xml`). Preserve this.

### Workout session

`:session` owns `WorkoutSessionController` (facade), `WorkoutSessionStateMachine`,
`WorkoutSessionService` (foreground service, `specialUse` type) and the ongoing notification.
`:app` only starts/finishes the session and provides notification navigation. Set counts are
derived from repositories; only `active`/`startedAt` are persisted.

### AI Coach

- `:ai-coach` — domain only, no Compose. `IAiCoach.recommendToday` is stateless: the caller
  passes in-memory `CoachMessage` history and receives new turns in the result.
  - Case A: no workout today → full plan (`TodayPlan`).
  - Case B: workout exists today → next-set advice (`NextAdvice`).
  - `PlanExecutor` abstracts the LLM; `AiCoachEngine` is unit-tested with a fake executor (no network).
- `:ai-coach-ui` — Compose UI + `AiCoachStateMachine` (FlowRedux). Assistant content is a
  **UI-agnostic descriptor** (`CoachMessageContent`); localization happens in UI via string
  resources. Do not hardcode display strings in the domain/engine.
- Privacy invariant: **no network call unless the user actively triggers a recommendation.**
  Only training summaries (names/sets/weight/reps/duration) are sent; never notes or body data.
- Real-LLM tests are opt-in: `./gradlew :ai-coach:testDebugUnitTest -Pdeepseek.apiKey=sk-...`
  (or `DEEPSEEK_API_KEY`). Default domain tests use fakes and run offline.

### UI conventions

- One Composable per file where practical, grouped under `page/<feature>/`.
- Each screen hosts its own `Scaffold` (top bar / bottom bar / FAB); `AppHost` only hosts
  `NavDisplay`.
- Shared UI state is passed via composition locals: `LocalNavBackStack`,
  `LocalAppSnackbarHostState` (`nav/AppLocals.kt`).
- Reusable widgets live in `app/.../widget/`; reusable visual components in `:calendar` / `:chart`.
- All user-facing strings go to `res/values/strings.xml` **and** `res/values-zh-rCN/strings.xml`
  (the app ships English + Simplified Chinese). Keep both in sync.
- Use Material 3 theming from `theme/`; do not introduce a second theme.
- Preview sample data belongs in `src/debug` `PreviewParameterProvider`s.

## Build & Test Commands

```bash
# Build all modules (debug)
./gradlew assembleDebug

# Build every variant (release requires signing env vars, see CI below)
./gradlew assemble

# Run all unit tests (aggregate task across modules)
./gradlew test

# Single module tests
./gradlew :app:testDebugUnitTest
./gradlew :ai-coach:testDebugUnitTest
./gradlew :session:testDebugUnitTest

# AI Coach real-LLM tests (opt-in, network + API key required)
./gradlew :ai-coach:testDebugUnitTest -Pdeepseek.apiKey=sk-...
```

- Always run `./gradlew test` (and `./gradlew assembleDebug` if build files/DI changed) before
  finishing a task.
- **Pre-push / pre-MR gate:** before pushing a branch or opening a merge request, verify that
  the project **builds** and that the **unit tests of every module touched by the change pass**.
  At minimum run `./gradlew assembleDebug` plus the affected modules' tests, e.g.
  `./gradlew :app:testDebugUnitTest :ai-coach-ui:testDebugUnitTest`. When in doubt, run the full
  `./gradlew test`. Do not push or open an MR with a failing build or failing tests.
- Release signing is read from env vars: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_ALIAS_PASSWORD`. Never commit `release.keystore` or `local.properties`.

## CI/CD

`.github/workflows/ci.yml`:

- **Pull requests:** `assembleDebug` + `test` (parallel jobs).
- **Push to `main`:** `assemble` (all variants, needs signing secrets) + `test`.
- **Tags `v*`:** `assembleRelease`, then publishes the APK + `mapping.txt` to a GitHub Release.

## Testing Guidelines

- Prefer JVM unit tests (`src/test`) with fakes over instrumentation tests; the project has
  almost no `androidTest`.
- Test state machines by driving actions and asserting emitted states (see
  `AddWorkoutStateMachineTest`, `AiCoachStateMachineTest`, `WorkoutSessionStateMachineTest`).
- Use `kotlinx-coroutines-test` and keep tests deterministic (inject clocks/ids, avoid real
  time or network).
- Name tests `*Test.kt` and mirror the class under test.

## Code Style

- Kotlin official code style (`kotlin.code.style=official`), 4-space indentation.
- Keep the existing KDoc style: explain *why*, and note invariants (privacy, process death,
  statelessness). Match nearby conventions before adding new patterns.
- Comments/KDoc are English; user-facing strings are localized (EN + zh-rCN).
- Avoid new dependencies without adding them to `gradle/libs.versions.toml`.
- Do not edit generated/build outputs (`build/`, `.gradle/`, `app/release/*.apk`).

## Generated Files & Planning Artifacts

- **Do not commit AI-generated plan/spec documents.** When implementing a requirement, planning
  files produced by the agent (e.g. `plan.md`, `*_plan.md`, `implementation-plan.md`, scratch
  design notes) are working artifacts only. Keep them out of commits, or delete them before
  finishing the task. The repo history reflects this convention (see the earlier
  "Remove ai plan doc" commit).
- The only documentation that belongs in the repo is intentional, reviewed docs such as
  `README.md`, `README_cn.md`, `AGENT.md`, and `docs/`.
- If you created a temporary planning/scratch file during a task, remove it (or add it to
  `.gitignore`) before committing so it never lands in the repository.
