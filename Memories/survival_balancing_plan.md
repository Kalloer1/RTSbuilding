# RTS Building Survival Balancing Plan

## Goal

Add an optional survival balancing module for RTS Building that works like a lightweight skill tree. The module must be toggleable, server-authoritative, and compatible with worlds that already have this mod installed.

Existing player storage/link data must not be cleared or migrated destructively. When balancing is disabled, the mod should behave like the current version.

## Core Principles

- Survival balancing is an independent progression layer.
- Existing RTS binding/session data remains untouched.
- The client only displays progression and sends unlock requests.
- The server validates dependencies, materials, feature access, range, and world permissions.
- Player progress stores unlocked nodes, not derived feature flags.
- Functional power should be controlled by node effects.

## Configuration

Add a common/server config flag:

```text
enableSurvivalProgression = false
```

Default should be `false` to avoid surprising existing worlds.

Behavior:

- Disabled:
  - All current RTS features remain available.
  - Action radius uses the current default behavior.
  - Existing linked storage and GUI bindings continue to work.
- Enabled:
  - Features and numeric capabilities are gated by player progression.
  - Existing linked data is preserved but may be temporarily unusable until related nodes are unlocked.

## Persistence

Do not modify or clear the existing storage session root:

```text
rtsbuilding_storage_session
```

Add a separate player persistent data root:

```text
rtsbuilding_progression
```

Suggested structure:

```text
rtsbuilding_progression:
  version: 1
  home_pos: 123456789
  home_dimension: minecraft:overworld
  home_set_day: 42
  unlocked_nodes:
    - rtsbuilding:camera_core
    - rtsbuilding:radius_1
    - rtsbuilding:storage_link
```

Store unlocked node IDs plus survival-only home anchor data. Runtime capabilities should still be derived from node effects.

The home anchor is only enforced when survival progression is enabled.

Optional team/shared progression now uses a separate world `SavedData` root:

```text
rtsbuilding_shared_progression
```

Shared records are keyed by FTB Teams when that mod is present, or by vanilla
scoreboard team as a fallback. They store unlocked node IDs plus the shared
home anchor. Personal progress remains the default; shared progress only
activates when `shareSurvivalProgressionWithTeams=true`.

## Progression Model

### Node

```java
record RtsProgressionNode(
    ResourceLocation id,
    String titleKey,
    String descriptionKey,
    List<ResourceLocation> dependencies,
    List<IngredientCost> costs,
    List<RtsUnlockEffect> effects,
    int x,
    int y
) {}
```

Rules:

- Empty `dependencies` means the node can be unlocked directly.
- Only upgrades that logically require order should have dependencies.
- Range upgrades should be sequential.
- Other feature branches can be independent when appropriate.

### Effects

Initial effect types:

```text
UNLOCK_FEATURE
SET_RADIUS_BLOCKS
SET_FLUID_CAPACITY_BUCKETS
SET_ULTIMINE_LIMIT
BYPASS_HOME_RADIUS
```

For numeric effects, use the highest unlocked value.

Examples:

```text
radius_1 -> SET_RADIUS_BLOCKS 16
radius_2 -> SET_RADIUS_BLOCKS 32
radius_3 -> SET_RADIUS_BLOCKS 48

fluid_buffer -> UNLOCK_FEATURE FLUID_HANDLING + SET_FLUID_CAPACITY_BUCKETS 100
```

`BYPASS_HOME_RADIUS` removes the survival home-radius restriction. It does not remove normal world permission checks.

## Feature Gates

Suggested feature enum:

```text
CAMERA
LINK_STORAGE
STORAGE_BROWSER
REMOTE_PLACE
REMOTE_BREAK
ROTATE_BLOCK
INTERACT
FUNNEL
AUTO_STORE_MINED_DROPS
REMOTE_GUI_BINDING
CRAFT_TERMINAL
JEI_TRANSFER
FLUID_HANDLING
ULTIMINE
```

Server code should query a progression API:

```java
RtsProgressionManager.canUse(player, RtsFeature.REMOTE_PLACE);
RtsProgressionManager.getActionRadius(player);
RtsProgressionManager.getFluidCapacityBuckets(player);
RtsProgressionManager.getUltimineLimit(player);
RtsProgressionManager.canAccessHomeRadius(player, pos);
RtsProgressionManager.canBypassHomeRadius(player);
```

If survival progression is disabled, these APIs should return current default behavior.

## Initial Skill Tree

### Foundation

```text
camera_core
- dependencies: none
- effects:
  - UNLOCK_FEATURE CAMERA
  - SET_RADIUS_BLOCKS 16
```

### Range

Range upgrades are sequential.

```text
radius_1
- dependencies: camera_core
- effects:
  - SET_RADIUS_BLOCKS 16

radius_2
- dependencies: radius_1
- effects:
  - SET_RADIUS_BLOCKS 32

radius_3
- dependencies: radius_2
- effects:
  - SET_RADIUS_BLOCKS 48
```

Later tiers can extend to 64 or 96 blocks if desired.

The same unlocked radius should be used for both RTS action range and the survival home-radius restriction.

### Storage

```text
storage_link
- dependencies: camera_core
- effects:
  - UNLOCK_FEATURE LINK_STORAGE
  - UNLOCK_FEATURE STORAGE_BROWSER
```

### Building

```text
remote_place
- dependencies: storage_link
- effects:
  - UNLOCK_FEATURE REMOTE_PLACE

remote_break
- dependencies: remote_place
- effects:
  - UNLOCK_FEATURE REMOTE_BREAK

rotate_block
- dependencies: camera_core
- effects:
  - UNLOCK_FEATURE ROTATE_BLOCK
```

### Automation

```text
auto_store_mined
- dependencies: storage_link
- effects:
  - UNLOCK_FEATURE AUTO_STORE_MINED_DROPS

funnel
- dependencies: storage_link
- effects:
  - UNLOCK_FEATURE FUNNEL
```

### Fluids

Fluid handling should be a separate unlock. Capacity can be generous once unlocked.

```text
fluid_buffer
- dependencies: storage_link
- effects:
  - UNLOCK_FEATURE FLUID_HANDLING
  - SET_FLUID_CAPACITY_BUCKETS 100
```

### Terminals

```text
remote_gui
- dependencies: storage_link
- effects:
  - UNLOCK_FEATURE REMOTE_GUI_BINDING

craft_terminal
- dependencies: storage_link
- effects:
  - UNLOCK_FEATURE CRAFT_TERMINAL

jei_transfer
- dependencies: craft_terminal
- effects:
  - UNLOCK_FEATURE JEI_TRANSFER
```

### Mining

```text
ultimine
- dependencies: auto_store_mined
- effects:
  - UNLOCK_FEATURE ULTIMINE
  - SET_ULTIMINE_LIMIT 64
```

### Home Mobility

```text
field_deployment
- dependencies: radius_3
- effects:
  - BYPASS_HOME_RADIUS
```

This node removes the requirement that RTS functions operate within the player's configured home radius. It should be expensive because it changes the survival pacing substantially.

## Unlock Flow

```text
Client clicks node
-> C2S unlock request(nodeId)
-> Server validates:
   - progression is enabled
   - node exists
   - node is not already unlocked
   - all dependencies are unlocked
   - required materials are available
-> Server consumes materials
-> Server writes unlocked node to player NBT
-> Server recalculates derived capabilities
-> Server sends S2C progression state
```

First version should consume materials only from the player's inventory. Linked storage material consumption can be added later after the core rules are stable.

## Home Anchor Rule

When survival progression is enabled, the player must set a home anchor before using normal RTS features. The natural entry point is the RTS toggle key, but the first press should enter a dedicated home selection mode instead of immediately binding the player's current block position.

```text
Player presses R
-> If progression is disabled: current behavior
-> If progression is enabled and no home is set:
   - check CAMERA unlock
   - enter home selection mode
   - create/select a temporary camera anchored near the player's current position
   - allow free camera movement within a 3x3 chunk area around that starting chunk
   - player clicks a target block/position and confirms Set Home
   - save that chosen position and dimension as home anchor
   - store current game day as home_set_day
   - start the normal RTS flow
-> If progression is enabled and home is set:
   - enforce home-radius checks unless BYPASS_HOME_RADIUS is unlocked
```

Home selection mode:

- It is only available before the first home anchor is set, or during an explicit home-change flow after cooldown.
- It should not expose normal RTS actions such as storage access, building, breaking, funnel, or fluid handling.
- The selectable region is a 3x3 chunk area centered on the chunk where the player entered selection mode.
- The camera can move freely inside that selection region so the player can inspect terrain and choose a meaningful home.
- The home anchor is committed only after the player deliberately confirms a target position.
- Canceling selection exits back to normal gameplay without setting home.

Home changes:

```text
The player cannot change home until 10 Minecraft days have passed.
10 days = 240000 game ticks.
```

Changing home should use the same selection mode, but only after the 10-day cooldown. It should be a deliberate UI action, not an accidental side effect of pressing R after the first home has been set.

Home-radius behavior:

- Use `RtsProgressionManager.getActionRadius(player)`.
- The home anchor and target position must be in the same dimension.
- All RTS world actions must be within this radius unless `BYPASS_HOME_RADIUS` is unlocked.
- Storage access is also constrained by this rule.
- If linked storage is outside the current home radius, it remains saved but is treated as inaccessible.

This means range nodes increase both:

- how far the player can operate from the RTS camera anchor
- how far from home RTS systems can access world targets and linked storage

## UI Plan

Entry point:

```text
Player opens inventory with E
Inventory screen has separate top RTS buttons/tabs:
- RTS Progression
- RTS Home
```

### RTS Progression Panel

This panel is only for feature progression and the skill tree.

- Shows nodes in a fixed skill tree layout.
- Draws dependency lines.
- Uses distinct states:
  - unlocked
  - available
  - locked
- Tooltip shows:
  - title
  - description
  - dependencies
  - material cost
  - unlock effects
- Unlock button sends a C2S request.

The client should not decide unlock success locally.

### RTS Home Panel

This panel is separate from the progression panel and owns all home-anchor viewing, setup, and adjustment actions.

Content:

- Current home position and dimension, if set.
- Current unlocked radius used by the home restriction.
- Whether home-radius bypass is unlocked.
- Remaining cooldown before home can be changed.
- First-time `Set Home` action when no home exists.
- `Adjust Home` / `Change Home` action when the 10-day cooldown has expired.

Behavior:

- `Set Home` enters home selection mode.
- `Adjust Home` / `Change Home` enters the same selection mode, but only when cooldown allows.
- Confirming an adjusted home replaces `home_pos`, `home_dimension`, and `home_set_day`.
- The home panel should explain status through compact labels and disabled button text, not through the skill tree.
- The progression panel should not contain home change controls.

## Server Integration Points

Feature gates should be added at the server-side action entry points:

```text
toggle camera -> CAMERA
linkStorage -> LINK_STORAGE
requestPage/storage browser -> STORAGE_BROWSER
placeSelected -> REMOTE_PLACE
breakPlaced -> REMOTE_BREAK
rotateBlock -> ROTATE_BLOCK
setFunnel/updateFunnelTarget/tickFunnel -> FUNNEL
autoStore mined drops -> AUTO_STORE_MINED_DROPS
openGuiBinding -> REMOTE_GUI_BINDING
openCraftTerminal/craftRecipe/refill -> CRAFT_TERMINAL
JEI transfer -> JEI_TRANSFER
placeFluid/storeFluid -> FLUID_HANDLING
ultimine -> ULTIMINE
```

Quest detection should not be part of the skill tree. Keep it as an independent compatibility/helper action outside survival progression.

Home-radius checks should be added to the same server-side validation path as action-radius checks. Storage handler resolution must filter out linked positions that are outside the player's home radius, while leaving those links persisted for later use.

Action radius should be centralized through progression:

```text
RtsCameraManager.start
RtsCameraManager.move
RtsCameraManager.isWithinActionRadius
S2CRtsCameraStatePayload maxRadius
```

Also fix the current multiplayer safety gap by ensuring server-side world action validation checks both:

```text
level.mayInteract(player, pos)
RtsCameraManager.isWithinActionRadius(player, pos)
RtsProgressionManager.canAccessHomeRadius(player, pos)
```

## Data-Driven Direction

The first implementation can use Java-defined nodes, but the API should be shaped so nodes can later move to datapack JSON:

```text
data/rtsbuilding/rts_progression/*.json
```

Example future JSON:

```json
{
  "title": "rtsbuilding.progression.camera_core",
  "description": "rtsbuilding.progression.camera_core.desc",
  "dependencies": [],
  "costs": [
    { "item": "minecraft:spyglass", "count": 1 },
    { "item": "minecraft:redstone", "count": 8 }
  ],
  "effects": [
    { "type": "unlock_feature", "feature": "camera" },
    { "type": "set_radius_blocks", "blocks": 16 }
  ],
  "position": { "x": 0, "y": 0 }
}
```

## Implementation Phases

### Phase 1: Server Skeleton

- Add config flag.
- Add `RtsFeature`.
- Add progression node/effect model.
- Add Java-defined default node registry.
- Add player NBT load/save for unlocked nodes.
- Add derived capability API.

### Phase 2: Gates and Radius

- Gate major server actions.
- Replace fixed action radius with progression radius.
- Keep current defaults when progression is disabled.
- Add fluid capacity lookup and connect it to the internal RTS fluid pool.
- Add multiplayer range validation to world action checks.
- Add home anchor persistence, 10-day home change cooldown, and home-radius validation.

### Phase 3: Networking

- Add C2S unlock node payload.
- Add S2C progression state payload.
- Sync state on login, unlock, and opening progression UI.

### Phase 4: Inventory UI

- Add top inventory RTS progression tab/button.
- Add fixed-layout skill tree panel.
- Add node states, tooltips, and unlock action.

### Phase 5: Polish and Balance

- Tune material costs.
- Add localization.
- Add optional datapack JSON support.
- Add linked-storage material consumption only if needed.

## Compatibility Notes

- Existing storage links are preserved.
- Turning progression on does not delete links; it only gates use.
- Turning progression off restores current behavior.
- Player progress is per-player by default.
- Optional team/shared progression is available behind
  `shareSurvivalProgressionWithTeams=false` by default.
- Team/shared progression uses FTB Teams first when available, then vanilla
  scoreboard teams. Existing personal unlocked nodes are merged into the
  shared record on login so enabling the option does not wipe solo progress.
- Saved internal RTS fluids are preserved even if the current progression
  capacity is lower than the saved amount; new insertion is blocked until the
  derived capacity allows room.
