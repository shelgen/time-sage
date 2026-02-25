# Time Sage – Copilot Instructions

Time Sage is a Kotlin Discord bot for planning weekly TTRPG sessions. It uses the JDA library, Maven, and targets JDK 21.

## Build & Test

```bash
# Build and run all tests
./mvnw verify

# Run tests only
./mvnw test

# Run a single test class
./mvnw test -Dtest=SerialIdGenerationTest

# Run a single test method
./mvnw test -Dtest="SerialIdGenerationTest#no collision of screen serial IDs"

# Package fat JAR
./mvnw package
# Output: target/time-sage-<version>-jar-with-dependencies.jar
```

## Architecture

### Entry point
`Main.kt` → `TimeSage` (extends `ListenerAdapter`) → registers JDA event handlers and cron jobs.

`JDAHolder` is a singleton object that reads the bot token from `time-sage-bot.token` (runtime file, not in repo) and blocks until JDA is ready.

### Screen / Component UI framework
All Discord interactive messages are modelled as `Screen` subclasses. Each screen renders Discord Components v2 (`useComponentsV2`). Interactive elements (buttons, select menus, modals) are nested inner classes inside their screen, implementing one of the sealed interfaces: `ScreenButton`, `ScreenStringSelectMenu`, `ScreenEntitySelectMenu`, or `ScreenModal`.

**State lives in Discord custom IDs.** `CustomIdSerialization` serializes the screen instance + component instance into the custom ID string. Format:
```
<screenSerialId>{field1,field2,...}|<typePrefix_componentSerialId>{field1,...}|<random>
```
On interaction, `CustomIdSerialization.deserialize<T>()` reconstructs the exact screen + component purely from the custom ID, so no server-side session state is needed.

`SerialIdGeneration` derives stable short IDs from class names (≤10 chars). **Serial IDs must not collide** — the test `SerialIdGenerationTest` enforces this. Run it whenever you add or rename a `Screen` or `ScreenComponent` subclass.

### Slash commands
`AbstractSlashCommand` is a sealed class. Every object that seals it is auto-discovered at startup via `sealedSubclasses` reflection. To add a command, create a new `object` subclassing `AbstractSlashCommand`.

### Repositories
Data is persisted as pretty-printed JSON files under `time-sage/servers/<guildId>/channels/<channelId>/`. `CachedJsonFileDao` wraps Jackson with a Caffeine cache (invalidated on write). Two repositories exist: `ConfigurationRepository` (bot settings per channel) and `AvailabilitiesWeekRepository` (weekly responses).

### Domain model
`Configuration` / `Scheduling` / `Activity` each have a mutable subclass (`MutableConfiguration`, etc.) used only during update operations inside `ConfigurationRepository.update { }` lambdas. Treat the base class as read-only everywhere else.

`OperationContext(guildId, channelId)` is threaded through the entire call stack to scope data to the correct channel.

### Scheduling / Planning
Quartz cron jobs (`CronJobScheduling`) fire `SendNextWeeksAvailabilityMessageJob` and `ReminderJob`. `Planner` generates all valid week plans using recursive permutations of activities × time slots, then ranks them by `Plan.Score` (more attendees > fewer "if need be" slots > sessions not on consecutive days).

## Key Conventions

- **Sealed class discovery via reflection** – `AbstractSlashCommand` and `Screen` subclasses are discovered at runtime with `sealedSubclasses`. New subclasses are picked up automatically; no registration needed.
- **`ScreenComponent` classes must be nested inside their `Screen`** – `CustomIdSerialization.getScreenComponents` only searches nested classes of the screen class.
- **Supported serializable field types in custom IDs**: `String`, `Int`, `Long`, `LocalDate`, `Instant`. Adding new field types requires extending `serializeField` / `deserializeField` in `serialization.kt`.
- **`OperationContext` fields are excluded from serialization** – they are injected from the live Discord event during deserialization.
- **Logging**: use the `logger` extension property (`inline val <reified T> T.logger`) for class-level loggers. MDC keys `guildId`, `channelId`, `userName` are set via `withContextAndUserMDC` at every event handler entry point.
- **Mutable vs immutable domain objects**: only mutate inside `ConfigurationRepository.update { }` where a `MutableConfiguration` is provided.
