# RTSBuilding Forge 1.19.2 Sister Project

This directory is the dedicated sister project for the `Minecraft 1.19.2 + Forge` port of RTSBuilding.

## Baseline

- Minecraft: `1.19.2`
- Forge: `43.4.0`
- Java: `17`
- JEI API/runtime: `11.6.0.1019`

## Recommended Layout

- `src/main/java/com/rtsbuilding/rtsbuilding/`
  The actual Forge 1.19.2 codebase. Keep the package identical to nearby RTSBuilding projects to minimize copy/edit churn.
- `src/main/resources/META-INF/mods.toml`
  Forge metadata entrypoint.
- `src/main/resources/assets/rtsbuilding/`
  Client assets, lang files, models, sounds, textures.
- `src/main/resources/data/rtsbuilding/`
  Recipes, tags, loot, advancements, other datapack content.
- `src/generated/resources/`
  Data-generator output.
- `docs/`
  Porting notes, dependency decisions, and API migration records.
## Architecture Snapshot

- `RtsbuildingMod` wires Forge lifecycle hooks, client render registration, and player/server events.
- `RtsStorageManager` owns the RTS session model and most gameplay logic:
  - linked storage set, linked dimension, link modes, and storage page aggregation
  - UI/session state such as page, search, category, sort, and sort direction
  - quick slots, GUI bindings, recent entries, internal fluid cache, funnel state, and mining state
  - persistence to player NBT so RTS state survives reconnects and world reloads
- `RtsCameraManager` handles camera activation and teardown.
- `network/` contains the C2S/S2C payloads that move RTS actions between client and server.
- `client/` contains UI, input gating, and rendering for the RTS overlay and terminals.
- `compat/` contains optional integration layers for AE2, FTB, and Sophisticated Storage.
- `mixin/` contains the Forge-side compatibility mixins that need direct screen/menu interception.
- `server/data/` contains world save data used by block tracking and other persistent server-side records.

## Local Build

PowerShell build example:

```powershell
.\gradlew.bat --no-daemon --no-watch-fs --console plain build
```

## Port Status

- Full Gradle `build` passes against Minecraft `1.19.2` and Forge `43.4.0`.
- The source includes small local compatibility shims for selected newer API shapes used by nearby RTSBuilding sources.
- Runtime QA in an actual 1.19.2 client/server is still recommended before release packaging.
