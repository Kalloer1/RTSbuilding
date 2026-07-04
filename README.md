# RTS Building Changelog
Full project introduction, feature descriptions, credits and build instructions can be found at the original repository: https://github.com/Hcrab/RTSbuilding

## Change Log
### Latest Version Changes
Below are the major changes compared to the initial release:
#### 1. Optional Client-side Support
- Feature: The RTS mod can be configured as client-only, no mandatory server installation required
- Modified Files: `neoforge.mods.toml`
- Description: Adjust mod metadata to enable independent client-side usage of all RTS functions

#### 2. RTS Storage Hotbar Swap
- Feature: Right-click items in the RTS storage overlay to quickly swap held items with hotbar entries
- Modified Files: `RtsClientInputGate.java`, `OverlayInteraction.java`
- Description: Supports fast item exchange between player hand and overlay hotbar via right click

#### 3. Blueprint Dual Point Adjustment
- Feature: Both capture points (Point A and Point B) can be edited when creating blueprints
- Modified Files: `BlueprintCaptureController.java`, `BlueprintWindowPanel.java`
- Description: Previously only single point adjustment was available; now both coordinates can be modified separately via input boxes or directional keys

#### 4. GUI Slot Hotkeys
- Feature: 8 numpad hotkeys (KP_1 to KP_8) bound for mapped GUI inventory slots
- Modified Files: `ClientKeyMappings.java`, `zh_cn.json`, `en_us.json`
- Description: Customizable key bindings with complete English and Chinese translation entries

#### 5. Chunk Display & Quick Build Hotkeys
- Feature: Dedicated bindable hotkeys for chunk overlay toggle (H key) and quick build panel (B key)
- Modified Files: `ClientKeyMappings.java`, `zh_cn.json`, `en_us.json`
- Description: One-click shortcut to toggle chunk visualization and open the rapid construction panel

#### 6. Tab Shape Cycle Toggle
- Feature: Press Tab in quick build mode to cycle through shape templates
- Modified Files: `ClientKeyMappings.java`, `QuickBuildPanel.java`, `zh_cn.json`, `en_us.json`
- Description: Cycle through block, line, rectangle, wall, circle, cube and more; Tab key rebindable in controls

#### 7. RTS View Entity Attack
- Feature: Attack mobs and entities directly while in top-down RTS camera mode
- Modified Files: `BuilderScreen.java`, `ClientRtsController.java`
- Description: Enables entity targeting and combat without exiting the overhead building interface