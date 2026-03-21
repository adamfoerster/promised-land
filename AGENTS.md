# AGENTS.md — Promised Land

## Project Overview

**Promised Land** is a turn-based strategy board game built with **Kotlin Multiplatform** and **Compose Multiplatform**, targeting both **Android** and **iOS**. The game features a hexagonal tile map, multiplayer (local hot-seat), round/phase-based turn progression, and persistent game state via an SQLite database.

## Tech Stack

| Layer             | Technology                                       |
| ----------------- | ------------------------------------------------ |
| Language          | Kotlin 1.9.21                                    |
| UI Framework      | Jetpack Compose Multiplatform 1.5.11             |
| Build System      | Gradle (Kotlin DSL), AGP 8.0.2                   |
| Database          | SQLDelight 2.0.1 (SQLite)                        |
| Date/Time         | kotlinx-datetime 0.6.0                           |
| Coroutines        | kotlinx-coroutines (via SQLDelight coroutines)    |
| MVVM              | androidx.lifecycle-viewmodel 2.8.0               |
| Testing           | kotlin-test, kotlinx-coroutines-test 1.8.0, Turbine 1.1.0 |
| Android Min SDK   | 24 · Target/Compile SDK 34                       |
| JVM Toolchain     | Java 17                                          |
| iOS Targets       | iosX64, iosArm64, iosSimulatorArm64              |

## Repository Structure

```
promised-land/
├── build.gradle.kts              # Root Gradle build — plugin declarations
├── settings.gradle.kts           # Modules: :androidApp, :shared
├── gradle.properties             # Version catalog & Android SDK config
├── cleanup.sh                    # Convenience script to reset build artifacts
│
├── shared/                       # KMP shared module (UI + logic + DB)
│   ├── build.gradle.kts          # Multiplatform plugin config + SQLDelight
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/
│       │   │   ├── App.kt                                    # Root composable & screen router
│       │   │   └── com/adamfoerster/promisedland/
│       │   │       ├── db/DatabaseDriverFactory.kt            # expect class for DB driver
│       │   │       ├── game/
│       │   │       │   ├── GameManager.kt                     # Core game logic, state flows, persistence
│       │   │       │   └── HexagonData.kt                     # Hex tile data model
│       │   │       └── ui/
│       │   │           ├── viewmodel/
│       │   │           │   └── GameViewModel.kt               # ViewModel: owns GameManager, Screen nav state
│       │   │           ├── WelcomeScreen.kt                   # Title / main menu screen
│       │   │           ├── SetupScreen.kt                     # New game setup (players, colors)
│       │   │           ├── ContinueGameScreen.kt              # Saved-game picker
│       │   │           ├── GameScreen.kt                      # In-game HUD overlay
│       │   │           └── HexMap.kt                          # Hex grid rendering, pan/zoom, tap
│       │   ├── resources/
│       │   │   ├── hexagons.csv                               # Map tile definitions (CSV)
│       │   └── sqldelight/
│       │       └── com/adamfoerster/promisedland/
│       │           └── GameDatabase.sq                        # Schema & queries (Game, Player, GameState, Hexagon, Metadata)
│       ├── commonTest/kotlin/com/adamfoerster/promisedland/  # Common test stubs (empty; actual tests in androidUnitTest)
│       ├── androidUnitTest/kotlin/com/adamfoerster/promisedland/
│       │   ├── game/
│       │   │   └── GameManagerTest.kt                         # GameManager unit tests (JVM/SQLite in-memory)
│       │   └── ui/viewmodel/
│       │       └── GameViewModelTest.kt                       # GameViewModel unit tests
│       ├── androidMain/kotlin/
│       │   ├── main.android.kt                                # Android composable entry point
│       │   └── com/adamfoerster/promisedland/db/
│       │       └── DatabaseDriverFactory.kt                   # actual class — AndroidSqliteDriver
│       └── iosMain/kotlin/
│           ├── main.ios.kt                                    # iOS UIViewController entry point
│           └── com/adamfoerster/promisedland/db/
│               └── DatabaseDriverFactory.kt                   # actual class — NativeSqliteDriver
│
├── androidApp/                   # Android application module
│   ├── build.gradle.kts          # Android app plugin + dependency on :shared
│   └── src/androidMain/kotlin/com/myapplication/
│       └── MainActivity.kt       # Single Activity, delegates to shared MainView()
│
└── iosApp/                       # iOS Xcode project
    ├── Configuration/            # Xcode build configuration
    ├── iosApp/                   # Swift UI host (calls shared MainViewController())
    └── iosApp.xcodeproj/         # Xcode project file
```

## Architecture

### Screen Navigation

Navigation is handled manually via a `Screen` sealed class in `GameViewModel.kt`:

```
Welcome  →  Setup         →  Game
         →  ContinueGame  →  Game
```

- **WelcomeScreen** — Main menu with "New Game" and "Continue Game" buttons.
- **SetupScreen** — Configure game name, add 3–6 players with names and colors.
- **ContinueGameScreen** — Lists saved games from the DB; tap to resume.
- **GameScreen** — The main gameplay view with HexMap, HUD, and turn controls.

### ViewModel Layer (`GameViewModel`)

`GameViewModel` extends `androidx.lifecycle.ViewModel` and serves as the bridge between the UI and `GameManager`. It owns:

- **`currentScreen`** — Compose `mutableStateOf<Screen>`, drives the screen router in `App.kt`.
- **`state`** / **`savedGames`** — Delegated `StateFlow`s from `GameManager`.

Key operations (delegates to `GameManager` + manages navigation):
- `setupGame()` — Creates the game and navigates to `Screen.Game`.
- `loadGame()` — Loads a saved game and navigates to `Screen.Game`.
- `deleteGame()` — Deletes a saved game.
- `nextTurn()` — Advances the turn.
- `returnToWelcome()` — Clears the active game and navigates to `Screen.Welcome`.
- `navigateTo()` — Manual screen transitions.

`GameViewModel` is instantiated in `App()` via `viewModel { GameViewModel(driver) }` using the Compose lifecycle-viewmodel integration.

### Game Logic (`GameManager`)

`GameManager` is the single source of truth for game data. It owns:

- **`activeGameId`** — `MutableStateFlow<Long?>` tracking the current session.
- **`state`** — `StateFlow<GameUIState>` derived reactively from DB queries via `combine`/`flatMapLatest`.
- **`savedGames`** — `StateFlow<List<SavedGameSummary>>` for the continue screen.
- **`hexagons`** — `StateFlow<Map<Pair<Int,Int>, HexagonData>>` loaded from the DB.

Key operations:
- `setupGame()` — Creates a `Game` row, shuffles and inserts `Player` rows, initializes `GameState`.
- `loadGame()` — Sets `activeGameId` to resume.
- `nextTurn()` — Advances turn order with rotating start player per round; 7 phases per round.
- `syncMapData()` — Loads `hexagons.csv` into the DB on startup if the map version has changed (versioned via `Metadata` table).

Constructor accepts `SqlDriver`, `CoroutineScope`, an optional `CoroutineDispatcher`, and `skipMapSync: Boolean` (used by tests to skip CSV loading).

### Hex Map (`HexMap`)

- Renders a 10×25 grid of pointy-top hexagons on a Canvas.
- Supports **pan**, **pinch-to-zoom**, and **tap-to-select** via Compose gesture detectors.
- Three zoom presets (OUT → INTERMEDIATE → IN) cycled via a floating action button.
- Hexagons can be active/inactive, and typed as `"village"` or `"city"` (rendered with Material icons).
- Map background is an image loaded via `painterResource`.

### Database Schema (SQLDelight)

| Table       | Purpose                                           |
| ----------- | ------------------------------------------------- |
| `Game`      | Game sessions (id, name, startedAt)               |
| `Player`    | Players per game (name, color, turnOrder)         |
| `GameState` | Current round, phase, and active player per game  |
| `Hexagon`   | Map tile data (col, row, name, isActive, type)    |
| `Metadata`  | Key-value config (used for map data versioning)   |

Platform-specific `DatabaseDriverFactory` implementations provide `AndroidSqliteDriver` (Android) and `NativeSqliteDriver` (iOS).

## Build & Run

### Prerequisites

- **JDK 17**
- **Android Studio** (for Android builds)
- **Xcode** (for iOS builds, macOS only)

### Android

```bash
./gradlew :androidApp:installDebug
```

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.

### Clean

```bash
./cleanup.sh
```

## Coding Conventions

- **Kotlin style**: `kotlin.code.style=official` (enforced via `gradle.properties`).
- **Package**: `com.adamfoerster.promisedland` for shared code; `com.myapplication` for the Android app shell.
- **State management**: Reactive `StateFlow`s derived from SQLDelight query flows. `GameViewModel` (extending `androidx.lifecycle.ViewModel`) exposes state to the UI and delegates to `GameManager`.
- **Compose Material 1**: The project uses `compose.material` (Material Design 1), not Material 3.
- **expect/actual**: Used only for `DatabaseDriverFactory`. All other code lives in `commonMain`.
- **Map data**: Tile definitions live in `hexagons.csv` (a bundled resource). When updating the CSV, increment `MAP_DATA_VERSION` in `GameManager` to trigger a DB re-sync.
- **No dependency injection**: Dependencies are constructed directly. `GameViewModel` receives a `SqlDriver` and optional `CoroutineDispatcher`. `GameManager` receives a `SqlDriver`, `CoroutineScope`, dispatcher, and `skipMapSync` flag.

## Testing

### Test Infrastructure

- Tests live in `androidUnitTest` (JVM-based) and use an **in-memory SQLite** driver (`JdbcSqliteDriver.IN_MEMORY`) provided by `app.cash.sqldelight:sqlite-driver`.
- `commonTest` contains empty stub files to avoid redeclaration errors with `androidUnitTest`.
- **Turbine** (`app.cash.turbine`) is used for testing `StateFlow` emissions.
- **kotlinx-coroutines-test** provides `runTest`, `StandardTestDispatcher`, and `UnconfinedTestDispatcher`.

### Test Classes

| Test Class          | Covers                                                                   |
| ------------------- | ------------------------------------------------------------------------ |
| `GameManagerTest`   | `setupGame`, `nextTurn`, `deleteGame`, `loadGame`, `nextGameName`, round/phase progression |
| `GameViewModelTest` | Initial state, screen navigation, `setupGame` → navigate, `returnToWelcome` |

### Running Tests

```bash
./gradlew :shared:testDebugUnitTest
```
