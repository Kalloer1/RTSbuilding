# RTSBuilding Carry-Over - 2026-05-28

This file is a handoff note for continuing work in a new Codex chat.

## User / Project Context

- User prefers Chinese conversation.
- Workspace root: `E:\RTSbuilding`.
- Root project is the main Minecraft `1.21.1 NeoForge` project.
- Do not confuse root with `sister-projects`.
- Important rule already in `AGENT.md`: read `detailed_project.txt` before running or changing things.
- After changes, update `detailed_project.txt` and `Journal.txt`.
- Official Java 21 path:
  - `C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot`

## Version / Repository Decisions

- Main project:
  - Minecraft `1.21.1`
  - NeoForge
  - root `E:\RTSbuilding`
  - should become GitHub default/mainline branch.
- Forge `1.20.1`:
  - should become a separate branch, probably `forge-1.20.1`.
  - branch root should be the Forge project itself, not nested under `sister-projects`.
- `sister-projects` is local-only porting/comparison workspace.
- Do not upload local `sister-projects` wholesale to GitHub.
- `1.19.x` is out of scope for first GitHub publication pass.
- `sister-projects\rtsbuilding-neoforge-26.1` is experimental/deleted/ignored:
  - do not restore
  - do not upload
  - do not treat as 1.21.1

## Recent Important Code Change

The latest code work was a small multiplayer camera compatibility mitigation in:

- `src/main/java/com/rtsbuilding/rtsbuilding/client/ClientRtsController.java`

Reason:

- CurseForge tester reported multiplayer RTS-mode issues:
  - visual stuttering
  - character POV flashing
  - iffy placement
  - garbled audio
- Code inspection found the camera/network path is main-thread based:
  - network handlers use `context.enqueueWork(...)`
  - client input/camera runs on client tick
  - server camera movement runs through packet work on server logic thread
- Likely issue is not "missing worker thread".
- More likely issue is aggressive camera ownership:
  - server creates a real `RtsCameraEntity`
  - client receives `serverCameraEntityId`
  - rendering currently uses client-only `localMirrorCamera`
  - `syncVisualCameraFrame()` restores local mirror camera if another system changes camera entity
  - previous code sent `C2SRtsCameraMovePayload` every client tick even with zero input

Implemented mitigation:

- Added idle heartbeat:
  - camera move packets send immediately only when meaningful camera input exists
  - if idle, send one zero-input heartbeat every 20 ticks
  - tiny decayed rotation values are clamped to zero using `CAMERA_INPUT_EPSILON`
- Added camera restore throttling:
  - if another mod/system changes `minecraft.getCameraEntity()`, local mirror restore is attempted at most once every 10 ticks
  - reduces high-frequency camera ownership fighting

Constants added:

- `CAMERA_INPUT_EPSILON = 1.0e-4F`
- `CAMERA_IDLE_HEARTBEAT_TICKS = 20`
- `CAMERA_RESTORE_COOLDOWN_TICKS = 10`

Validation:

- `git diff --check` was run for touched files and produced no error output.
- Gradle build was not run because user currently prefers to build locally.

Longer-term camera direction:

- Prefer the server-synced `RtsCameraEntity` when available.
- Use `localMirrorCamera` only as short fallback / prediction aid.
- Keep client prediction for smoothness.
- Reduce camera state fighting with other camera/audio/POV mods.

## Build Commands User Wants To Run Locally

Main 1.21.1:

```powershell
cd E:\RTSbuilding
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME='E:\RTSbuilding\.gradle-user-home'
java -version
.\gradlew.bat build --no-daemon --no-configuration-cache
```

Forge 1.20.1 local sister build:

```powershell
cd E:\RTSbuilding\sister-projects\rtsbuilding-forge-1.20.1
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME='E:\RTSbuilding\sister-projects\rtsbuilding-forge-1.20.1\.gradle-user-home'
java -version
.\gradlew.bat build --no-daemon --no-configuration-cache
```

## Important Files

- `AGENT.md`
  - contains project rules and GitHub/branch strategy.
- `detailed_project.txt`
  - project map, current architecture, historical records.
- `Journal.txt`
  - high-granularity change log.
- `src/main/java/com/rtsbuilding/rtsbuilding/client/ClientRtsController.java`
  - RTS client state, camera, prediction, storage UI state.
- `src/main/java/com/rtsbuilding/rtsbuilding/server/RtsCameraManager.java`
  - server-authoritative RTS camera session/entity movement.
- `src/main/java/com/rtsbuilding/rtsbuilding/server/RtsStorageManager.java`
  - remote storage, placement, mining, crafting, interactions.
- `src/main/java/com/rtsbuilding/rtsbuilding/client/BuilderScreen.java`
  - main RTS UI.
- `src/main/java/com/rtsbuilding/rtsbuilding/client/RtsBoundaryRenderer.java`
  - RTS boundary / target / ghost rendering.

## Public Launch / Video Context

Bilibili video:

- Title was lightly improved with help:
  - `【MC模组发布】RTS俯视角建造，改变你的整合包基地体验`
- The video started slowly, then received several recommendation waves.
- It reached at least:
  - about `1460` plays
  - `138` likes
  - `60` coins
  - `146` favorites
  - `121` shares
  - `23` concurrent viewers
- Like rate dipped under 10%, but favorite rate climbed to about 10%.
- User considers launch purpose achieved:
  - community discovered the mod
  - people entered group / gave feedback
  - vertical growth potential is strong

Important user sentiment:

- This was a major comeback after years away from Bilibili.
- The user is excited but wants to return to practical work.

## Community Feedback / Feature Requests

High-priority feedback:

1. Camera start / closeness option
   - CurseForge user asked for camera closer to player and start at player head position.
   - Possible next setting:
     - RTS camera start: overhead vs player/head position
     - remove or relax minimum distance push-away behavior

2. Block replacement
   - Bilibili user specifically mentioned GregTech packs:
     - replacing input/output buses/hatches directly on structures would be very useful.
   - This is probably high-value and very aligned with the mod.

3. Range / chain demolition
   - User mentioned large-area breaking, similar to chain building.
   - Needs caution:
     - permissions
     - accidental demolition
     - tool durability
     - drops
     - server safety
   - Better framed as shape demolition with confirmation and limits.

4. Favorites / grouped quick slots
   - Current quick slots exist, but grouped favorites do not.

5. Multiplayer compatibility
   - Current camera mitigation is first step.
   - Need real multiplayer modpack testing.

## Suggested Next Work

Immediate:

- Port the camera heartbeat / restore-throttle mitigation to Forge `1.20.1` if user asks.
- Let user run local builds.
- Consider packaging / GitHub publication plan.

GitHub publication planning:

- First inspect `.gitignore` and dirty files.
- Decide whether to:
  - clean current repo and push root as main/default branch, or
  - create a new clean repo and import root files.
- For `forge-1.20.1`, create a separate branch where branch root equals the Forge project.
- Do not publish `build/`, `run/`, `.gradle/`, `.gradle-user-home/`, `outputs/`, `staging/`, crash logs, replay logs.

Feature roadmap:

1. Stabilize multiplayer camera.
2. Camera start/head-position and close-camera settings.
3. Block replacement.
4. Grouped favorites / quick-slot organization.
5. Shape demolition / controlled range breaking.

## Important Caution

- The working tree is dirty and contains many prior changes.
- Do not revert user or previous-session changes.
- When changing code, inspect local diffs and preserve unrelated modifications.
- The user said not to worry about Git commit for now; "pretend commit" language appeared earlier, but actual Git actions should only happen when explicitly requested.
