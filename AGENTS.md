# RTSBuilding Agent Notes

This repository is the public mainline for RTSBuilding.

## User / Conversation

- Prefer Chinese when talking with the user.
- The workspace root is `E:\RTSbuilding`.
- Before changing code, inspect the relevant local files first.
- Do not revert user changes or unrelated local changes.

## Project Layout

- `main` is the Minecraft 1.21.1 NeoForge line.
- `forge-1.20.1` is the Minecraft 1.20.1 Forge branch.
- The local Forge working copy lives at `sister-projects\rtsbuilding-forge-1.20.1`.
- Do not publish `sister-projects` as a nested directory in `main`.
- Use the Forge working copy only as the work-tree for the `forge-1.20.1` branch.

## Local Memory Files

- `detailed_project.txt` and `Journal.txt` are local working-memory files.
- Keep them updated after meaningful project changes.
- They are intentionally local-only and should not be committed or pushed.
- They are ignored through `.git/info/exclude`, not the public `.gitignore`.

## Build Commands

Main 1.21.1:

```powershell
cd E:\RTSbuilding
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME='E:\RTSbuilding\.gradle-user-home'
.\gradlew.bat build --no-daemon --no-configuration-cache
```

Forge 1.20.1:

```powershell
cd E:\RTSbuilding\sister-projects\rtsbuilding-forge-1.20.1
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME='E:\RTSbuilding\sister-projects\rtsbuilding-forge-1.20.1\.gradle-user-home'
.\gradlew.bat build --no-daemon --no-configuration-cache
```

## Forge Branch Commit Pattern

When committing sister-project changes to `forge-1.20.1`, use a temporary index so the root `main` index is not polluted:

```powershell
$repo = 'E:\RTSbuilding'
$work = 'E:\RTSbuilding\sister-projects\rtsbuilding-forge-1.20.1'
$tmpIndex = Join-Path $repo '.git\forge-1.20.1.work.index'
Remove-Item -LiteralPath $tmpIndex -Force -ErrorAction SilentlyContinue
$env:GIT_INDEX_FILE = $tmpIndex
try {
    git --git-dir "$repo\.git" --work-tree "$work" read-tree forge-1.20.1
    git --git-dir "$repo\.git" --work-tree "$work" add <paths>
    $tree = git --git-dir "$repo\.git" --work-tree "$work" write-tree
    $parent = git --git-dir "$repo\.git" rev-parse forge-1.20.1
    $commit = git --git-dir "$repo\.git" commit-tree $tree -p $parent -m '<message>'
    git --git-dir "$repo\.git" update-ref refs/heads/forge-1.20.1 $commit
} finally {
    Remove-Item Env:\GIT_INDEX_FILE -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $tmpIndex -Force -ErrorAction SilentlyContinue
}
```
