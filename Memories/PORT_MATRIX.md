# RTSBuilding Port Matrix

Last updated: 2026-05-27

This file is the working queue for keeping the Forge sister projects aligned
with the `1.21.1 NeoForge` mainline.

## Source Of Truth

- Mainline: `Minecraft 1.21.1 + NeoForge 21.1.219`
- Current release line: `1.0.0-beta`
- Product name: `RTS Building: Build From Above`
- Primary down-port target: `Minecraft 1.20.1 + Forge 47.4.16`
- Secondary down-port target: `Minecraft 1.19.2 + Forge 43.4.0`

## Divergence Timeline

| Date | Project | Event | Porting meaning |
| --- | --- | --- | --- |
| 2026-04-04 | Forge 1.20.1 | Sister scaffold created in `sister-projects/rtsbuilding-forge-1.20.1`. | Bootstrap only. |
| 2026-04-05 | Forge 1.20.1 | First full bulk migration reached `compileJava` and `build` success. | Last known broad sync point for 1.20.1. |
| 2026-04-24 | Forge 1.19.2 | Sister created from the 1.20.1 Forge baseline and retargeted to 1.19.2. | Last known broad sync point for 1.19.2. |
| 2026-05-22 to 2026-05-24 | Mainline | Progression, RTS Home, settings, storage snapshots, UI scale, topbar assets, chunk view, ultimine, and localization landed. | Main feature wave that both sister projects are missing or only partially carrying. |

## Current State

| Track | Status | Notes |
| --- | --- | --- |
| Mainline 1.21.1 NeoForge | Release candidate beta | Build artifact exists as `build/libs/rtsbuilding-build-from-above-1.0.0-beta.jar`. |
| Forge 1.20.1 | Active catch-up target | Public metadata and current mainline resource assets have been refreshed. Latest humble Forge beta artifact is `rtsbuilding-build-from-above-forge-1.20.1-0.0.1-beta-forge.jar`. |
| Forge 1.19.2 | Deferred follower port | Very close to the 1.20.1 sister, with more compatibility shims. Leave it frozen until 1.20.1 is stable. |

## Feature Matrix

Legend:

- `Done`: present and recently validated on that track.
- `Old`: present from the earlier sister snapshot, but should be revalidated.
- `Missing`: absent from the sister source/resource tree.
- `Check`: likely partial or API-sensitive; compare before copying.

| Mainline feature | Main 1.21.1 | Forge 1.20.1 | Forge 1.19.2 | Porting notes |
| --- | --- | --- | --- | --- |
| RTS camera, bounded remote placement, remote interaction, mining | Done | Old | Old | Core loop exists in both sisters; re-test after newer session/progression changes. |
| Linked storage browsing, categories, sorting, recent entries, quick slots | Done | Old | Old | Newer persistence and pinyin search are missing. |
| Craft panel, craft terminal, recipe viewer handoff | Done | Check | Check | Replay May 23 recipe-viewer key handoff and screenless remote-menu recovery. |
| JEI compat | Done | Old | Old | 1.20.1 has runtime reflection fallback for JEI 15.x; 1.19.2 uses JEI 11.x. Revalidate in real packs. |
| AE2 compat | Done | Old | Old | Reflection path exists, but runtime QA is still required per target version. |
| FTB Quests detect bridge | Done | Old | Old | New quest detect status payload/topbar feedback needs porting. |
| Sophisticated Storage still-valid bypass | Done | Old | Old | Verify mixin target names and optional dependency versions before release. |
| Crash-resilient world-level RTS session snapshots | Done | Done | Missing | 1.20.1 now writes `rtsbuilding/storage_sessions.dat` while retaining player persistent NBT as migration fallback. |
| Client-local UI state store | Done | Missing | Missing | Port `client/RtsClientUiStateStore.java` for sensitivity and RTS GUI scale. |
| Pinyin storage search | Done | Missing | Missing | Port `server/RtsPinyinSearch.java`, PinIn dependency, and `META-INF/licenses/PinIn-LICENSE.txt`. |
| Survival progression backend | Done | Missing | Missing | Port `progression/*`, `server/data/RtsSharedProgressionData.java`, and progression network payloads. |
| RTS Progression screen and operator cost editor | Done | Missing | Missing | Requires progression payloads and config/runtime sync first. |
| RTS Home / Field Deployment | Done | Missing | Missing | Port `client/RtsHomeScreen.java`, home-selection payloads, and May 24 Field Deployment rule changes. |
| Mature FTB-style ultimine flow | Done | Missing | Missing | Port `C2SRtsUltiminePayload` and the current seed/highlight/release behavior. |
| Chunk View edge-cell highlight renderer | Done | Check | Check | Compare `RtsBoundaryRenderer`; May 24 mainline renderer changed the visual model. |
| Topbar icon refresh and quick-build assets | Done | Done | Missing | 1.20.1 now carries the current mainline texture assets. |
| Compact topic guide and localization pass | Done | Done | Missing | 1.20.1 now carries the current `en_us.json` and `zh_cn.json`; strings may still refer to features whose Java port is pending. |
| Runtime settings modal, survival toggle, cost override UI | Done | Missing | Missing | Depends on progression/config payload port. |
| UI scale setting | Done | Missing | Missing | Depends on `RtsClientUiStateStore` and BuilderScreen input/render scaling changes. |
| Metadata for public release | Done | Done | Missing | 1.20.1 now uses the public beta name, version, archive name, and description. |

## Forge 1.20.1 Catch-up Queue

1. Build hygiene
   - Use `tools/build_all_ports.ps1` so the sisters do not run under Java 25.
   - Confirm `compileJava` and `build` still pass from the current snapshot.

2. Metadata and public-facing resources
   - Done: display name is `RTS Building: Build From Above`.
   - Done: description is `Build, manage storage, and interact with your base from an overhead RTS-style building panel.`
   - Done: copied current language files, topbar textures, quick-build textures, and PinIn license text.

3. Low-risk persistent state
   - Pending: port `RtsClientUiStateStore`.
   - Done: port `RtsStorageSessionStore`.
   - Done: save current 1.20.1 session data into both player persistent NBT and the world-level session snapshot store.

4. Search and storage UX
   - Port pinyin search and the PinIn license/dependency.
   - Reconcile storage browser state fields with the mainline payloads.

5. Progression and Home
   - Port `progression/*`.
   - Port `RtsSharedProgressionData`.
   - Port progression payloads and `RtsProgressionScreen`.
   - Port `RtsHomeScreen`, home selection payloads, and Field Deployment behavior.

6. Action polish
   - Port mature ultimine.
   - Port quest detect status feedback.
   - Port recipe-viewer handoff and remote-menu recovery fixes.
   - Port latest Chunk View renderer if the current sister renderer is older.

7. Compat validation
   - Smoke test JEI, AE2, FTB Quests/Teams, Sophisticated Storage, and Create/Ponder in a real 1.20.1 Forge pack.

8. Release package
   - Current humble Forge beta version: `0.0.1-beta-forge`.
   - Latest artifact: `sister-projects/rtsbuilding-forge-1.20.1/build/libs/rtsbuilding-build-from-above-forge-1.20.1-0.0.1-beta-forge.jar`.
   - Rebuilt and inspected jar metadata on 2026-05-27; upload as a separate 1.20.1 Forge file.

## Forge 1.19.2 Catch-up Queue

1. Wait until the 1.20.1 catch-up is stable.
2. Replay the 1.20.1 patches into 1.19.2 in the same order.
3. Keep local compatibility shims small and explicit:
   - GUI rendering wrapper.
   - Registry helpers.
   - Network payload facade.
   - Older crafting/menu signatures.
4. Runtime-test earlier than usual because 1.19.2 has more API shape drift.
5. Version suggestion: `1.0.0-beta-forge.1.19.2`.

## Management Rules

- Treat the 1.21.1 NeoForge mainline as the design source of truth.
- Port to 1.20.1 first, then replay to 1.19.2.
- Do not make product changes only in a sister project unless they are
  strictly version-compatibility fixes.
- After every mainline release, add a short row here before starting the next
  sister catch-up.
- Keep each porting commit grouped by feature, not by massive file copy.
- Always inspect `mods.toml` / jar metadata before uploading a sister build.
