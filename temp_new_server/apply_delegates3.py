filepath = r"E:\Minecraft\Minecraft Modding\RTSbuilding\src\main\java\com\rtsbuilding\rtsbuilding\server\RtsStorageManager.java"

with open(filepath, 'r', encoding='utf-8') as f:
    lines = f.readlines()

print(f"Read {len(lines)} lines")

# Build replacements keyed by pattern to match
# Each entry: (start_marker_line_contains, end_marker_line_contains, replacement_lines)
# Strategy: find lines containing specific patterns and replace blocks

replacements = []

# Helper: find a method start line
def find_method(lines, start_contains):
    for i, line in enumerate(lines):
        if start_contains in line:
            return i
    return -1

def find_method_end(lines, start_idx):
    """Find the closing brace of a method starting at start_idx"""
    brace_count = 0
    for i in range(start_idx, len(lines)):
        stripped = lines[i].strip()
        # Remove comments to not count braces in strings
        if '{' in stripped:
            brace_count += stripped.count('{')
        if '}' in stripped:
            brace_count -= stripped.count('}')
            if brace_count == 0:
                return i
    return len(lines) - 1

# === returnCarriedToLinked (lines 422-424, 0-indexed 421-423) ===
idx = find_method(lines, "returnCarriedToLinked")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void returnCarriedToLinked(ServerPlayer player, String itemId, int amount) {\n        RtsTransferService.returnCarriedToLinked(player, itemId, amount);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced returnCarriedToLinked at line {idx+1}")

# === quickDropLinkedItem ===
idx = find_method(lines, "quickDropLinkedItem")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void quickDropLinkedItem(ServerPlayer player, String itemId, byte amount, double dropX, double dropY,\n            double dropZ) {\n        RtsTransferService.quickDropLinkedItem(player, itemId, amount, dropX, dropY, dropZ);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced quickDropLinkedItem at line {idx+1}")

# === importMenuSlotToLinked ===
idx = find_method(lines, "importMenuSlotToLinked")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void importMenuSlotToLinked(ServerPlayer player, int menuSlot) {\n        RtsTransferService.importMenuSlotToLinked(player, menuSlot);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced importMenuSlotToLinked at line {idx+1}")

# === refillCurrentCraftGridFromBlueprintIds ===
idx = find_method(lines, "refillCurrentCraftGridFromBlueprintIds")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void refillCurrentCraftGridFromBlueprintIds(\n            ServerPlayer player,\n            List<String> blueprintIds,\n            String craftedItemId,\n            int craftedCount) {\n        RtsCraftingService.refillCurrentCraftGridFromBlueprintIds(player, blueprintIds, craftedItemId, craftedCount);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced refillCurrentCraftGridFromBlueprintIds at line {idx+1}")

# === refillCurrentCraftGridFromBlueprintStacks ===
idx = find_method(lines, "refillCurrentCraftGridFromBlueprintStacks")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void refillCurrentCraftGridFromBlueprintStacks(\n            ServerPlayer player,\n            List<ItemStack> blueprintStacks,\n            String craftedItemId,\n            int craftedCount) {\n        RtsCraftingService.refillCurrentCraftGridFromBlueprintStacks(player, blueprintStacks, craftedItemId, craftedCount);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced refillCurrentCraftGridFromBlueprintStacks at line {idx+1}")

# === applyJeiTransfer ===
idx = find_method(lines, "applyJeiTransfer")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void applyJeiTransfer(\n            ServerPlayer player,\n            String recipeId,\n            List<ItemStack> ingredientPrototypes,\n            boolean maxTransfer,\n            boolean clearGridFirst) {\n        RtsCraftingService.applyJeiTransfer(player, recipeId, ingredientPrototypes, maxTransfer, clearGridFirst);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced applyJeiTransfer at line {idx+1}")

# === pickupLinkedToCarried ===
idx = find_method(lines, "pickupLinkedToCarried")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void pickupLinkedToCarried(ServerPlayer player, ItemStack prototype, int amount) {\n        RtsTransferService.pickupLinkedToCarried(player, prototype, amount);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced pickupLinkedToCarried at line {idx+1}")

# === quickMoveLinkedItem ===
idx = find_method(lines, "quickMoveLinkedItem")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void quickMoveLinkedItem(ServerPlayer player, ItemStack prototype) {\n        RtsTransferService.quickMoveLinkedItem(player, prototype);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced quickMoveLinkedItem at line {idx+1}")

# === fillPlayerInventoryFromLinked ===
idx = find_method(lines, "fillPlayerInventoryFromLinked")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void fillPlayerInventoryFromLinked(ServerPlayer player) {\n        RtsTransferService.fillPlayerInventoryFromLinked(player);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced fillPlayerInventoryFromLinked at line {idx+1}")

# === mine ===
idx = find_method(lines, "public static void mine(ServerPlayer player")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void mine(ServerPlayer player, BlockPos pos, Direction face, boolean start, byte toolSlot,\n            String toolItemId, ItemStack toolPrototype, boolean allowPlacedBlockRecovery,\n            boolean toolProtectionEnabled) {\n        RtsMiningService.mine(player, pos, face, start, toolSlot, toolItemId, toolPrototype, allowPlacedBlockRecovery, toolProtectionEnabled);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced mine at line {idx+1}")

# === startUltimine ===
idx = find_method(lines, "public static void startUltimine(ServerPlayer player")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void startUltimine(ServerPlayer player, BlockPos pos, Direction face, byte toolSlot, String toolItemId,\n            ItemStack toolPrototype, int requestedLimit, byte mode, boolean toolProtectionEnabled) {\n        RtsMiningService.startUltimine(player, pos, face, toolSlot, toolItemId, toolPrototype, requestedLimit, mode, toolProtectionEnabled);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced startUltimine at line {idx+1}")

# === areaMine ===
idx = find_method(lines, "public static void areaMine(ServerPlayer player")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void areaMine(ServerPlayer player,\n            int minX, int maxX, int minY, int maxY, int minZ, int maxZ,\n            byte toolSlot, String toolItemId, ItemStack toolPrototype,\n            byte shapeType, byte fillType, boolean toolProtectionEnabled) {\n        RtsMiningService.areaMine(player, minX, maxX, minY, maxY, minZ, maxZ, toolSlot, toolItemId, toolPrototype, shapeType, fillType, toolProtectionEnabled);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced areaMine at line {idx+1}")

# === areaDestroy ===
idx = find_method(lines, "public static void areaDestroy(ServerPlayer player")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void areaDestroy(ServerPlayer player, List<BlockPos> positions,\n            byte toolSlot, String toolItemId, ItemStack toolPrototype, boolean toolProtectionEnabled) {\n        RtsMiningService.areaDestroy(player, positions, toolSlot, toolItemId, toolPrototype, toolProtectionEnabled);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced areaDestroy at line {idx+1}")

# === withTemporaryMainHandItem ===
idx = find_method(lines, "withTemporaryMainHandItem")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static <T> T withTemporaryMainHandItem(ServerPlayer player, ItemStack stack, Supplier<T> action) {\n        return RtsMiningService.withTemporaryMainHandItem(player, stack, action);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced withTemporaryMainHandItem at line {idx+1}")

# === snapshotCraftGridBlueprint ===
idx = find_method(lines, "snapshotCraftGridBlueprint")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static ItemStack[] snapshotCraftGridBlueprint(CraftingMenu menu) {\n        return RtsCraftingService.snapshotCraftGridBlueprint(menu);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced snapshotCraftGridBlueprint at line {idx+1}")

# === refillCraftGridFromBlueprint ===
idx = find_method(lines, "public static void refillCraftGridFromBlueprint(CraftingMenu")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void refillCraftGridFromBlueprint(CraftingMenu menu, List<IItemHandler> handlers, ServerPlayer player,\n            ItemStack[] blueprint, boolean fillAll, boolean includePlayerFallback) {\n        RtsCraftingService.refillCraftGridFromBlueprint(menu, handlers, player, blueprint, fillAll, includePlayerFallback);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced refillCraftGridFromBlueprint at line {idx+1}")

# === refillCraftGridFromLinked ===
idx = find_method(lines, "public static void refillCraftGridFromLinked(")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void refillCraftGridFromLinked(\n            ServerPlayer player,\n            CraftingMenu craftingMenu,\n            ItemStack[] blueprint,\n            CraftingRecipe recipe) {\n        RtsCraftingService.refillCraftGridFromLinked(player, craftingMenu, blueprint, recipe);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced refillCraftGridFromLinked at line {idx+1}")

# === recordCraftedOutput ===
idx = find_method(lines, "recordCraftedOutput")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void recordCraftedOutput(ServerPlayer player, ItemStack crafted) {\n        RtsCraftingService.recordCraftedOutput(player, crafted);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced recordCraftedOutput at line {idx+1}")

# === recordRecentItem ===
idx = find_method(lines, "public static void recordRecentItem")
if idx >= 0:
    end = find_method_end(lines, idx)
    repl = '    public static void recordRecentItem(RtsStorageSession session, String itemId, byte kind, long amount) {\n        RtsPageService.recordRecentItem(session, itemId, kind, amount);\n    }\n'
    lines[idx:end+1] = [repl]
    print(f"Replaced recordRecentItem at line {idx+1}")

with open(filepath, 'w', encoding='utf-8') as f:
    f.writelines(lines)

print(f"Final file: {len(lines)} lines")
