# Porting Notes

## Why This Is a Sister Project

The `1.19.2 Forge` down-port diverges from newer RTSBuilding code on GUI APIs, registries, crafting signatures, entity spawn packets, creative tabs, and some player/server helpers. Keeping the port in a separate project prevents compatibility shims from polluting newer branches.

## Recommended Migration Order

1. Keep `compileJava` and full `build` passing after each compatibility adjustment.
2. Prefer narrow local shims when a newer API is only a facade over an older 1.19.2 API.
3. Patch behavior directly where the underlying 1.19.2 model is materially different.
4. Run client/server smoke tests after compile-time porting, especially for UI overlays and crafting flows.

## High-Risk Areas From Newer Sources

- `client/`
  1.19.2 renders with `PoseStack`; this project keeps a small `GuiGraphics` wrapper to reduce UI churn.
- `server/RtsStorageManager.java`
  Several crafting and block-state helpers use older 1.19.2 signatures.
- `network/`
  The Forge `SimpleChannel` path is retained through the local compatibility package.
- `mixin/`
  Verify mixin targets against the actual 1.19.2 optional dependency versions before release.

## Dependency Strategy

- Forge: `43.4.0`
- Minecraft mappings: official `1.19.2`
- JEI: `11.6.0.1019`
- Optional compat layers compile through reflection or optional integrations; runtime QA still needs the target mods installed.
