
# RTSBuilding

RTSBuilding is a Minecraft building mod that adds an RTS-style remote camera,
storage-aware placement, remote interaction, mining, crafting, and utility UI
for large base construction.

This branch is the Minecraft 1.21.1 NeoForge mainline.

## Build

Requirements:

- Java 21
- Git

Build from a fresh clone:

```powershell
.\gradlew.bat build --no-daemon --no-configuration-cache
```

Linux/macOS:

```bash
./gradlew build --no-daemon --no-configuration-cache
```

The built jar is written under `build/libs`.

## Branches

- `main`: Minecraft 1.21.1 / NeoForge
- `forge-1.20.1`: Minecraft 1.20.1 / Forge, published with the Forge project at
  the branch root

Local `sister-projects` folders are porting workspaces and are not part of the
main branch source release.

## Credits And Licenses

Minecraft mappings are provided under Mojang's mapping license. For details,
see the mapping license distributed by NeoForge/Mojang.
