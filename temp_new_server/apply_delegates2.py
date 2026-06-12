import re

filepath = r"E:\Minecraft\Minecraft Modding\RTSbuilding\src\main\java\com\rtsbuilding\rtsbuilding\server\RtsStorageManager.java"

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Remaining replacements with exact text from the file
replacements = [
    # === returnCarriedToLinked ===
    (
        'public static void returnCarriedToLinked(ServerPlayer player, String itemId, int amount) {\n        RtsStorageTransfers.returnCarriedToLinked(player, RtsSessionService.getIfPresent(player), itemId, amount);\n    }',
        'public static void returnCarriedToLinked(ServerPlayer player, String itemId, int amount) {\n        RtsTransferService.returnCarriedToLinked(player, itemId, amount);\n    }',
    ),
    # === quickDropLinkedItem ===
    (
        'public static void quickDropLinkedItem(ServerPlayer player, String itemId, byte amount, double dropX, double dropY,\n            double dropZ) {\n        RtsStorageTransfers.quickDropLinkedItem(player, RtsSessionService.getIfPresent(player), itemId, amount, dropX, dropY, dropZ);\n    }',
        'public static void quickDropLinkedItem(ServerPlayer player, String itemId, byte amount, double dropX, double dropY,\n            double dropZ) {\n        RtsTransferService.quickDropLinkedItem(player, itemId, amount, dropX, dropY, dropZ);\n    }',
    ),
    # === importMenuSlotToLinked ===
    (
        'public static void importMenuSlotToLinked(ServerPlayer player, int menuSlot) {\n        RtsStorageTransfers.importMenuSlotToLinked(player, RtsSessionService.getIfPresent(player), menuSlot);\n    }',
        'public static void importMenuSlotToLinked(ServerPlayer player, int menuSlot) {\n        RtsTransferService.importMenuSlotToLinked(player, menuSlot);\n    }',
    ),
    # === refillCurrentCraftGridFromBlueprintIds ===
    (
        'public static void refillCurrentCraftGridFromBlueprintIds(\n            ServerPlayer player,\n            List<String> blueprintIds,\n            String craftedItemId,\n            int craftedCount) {\n        RtsStorageCrafting.refillCurrentCraftGridFromBlueprintIds(\n                player,\n                RtsSessionService.getIfPresent(player),\n                blueprintIds,\n                craftedItemId,\n                craftedCount);\n    }',
        'public static void refillCurrentCraftGridFromBlueprintIds(\n            ServerPlayer player,\n            List<String> blueprintIds,\n            String craftedItemId,\n            int craftedCount) {\n        RtsCraftingService.refillCurrentCraftGridFromBlueprintIds(player, blueprintIds, craftedItemId, craftedCount);\n    }',
    ),
    # === refillCurrentCraftGridFromBlueprintStacks ===
    (
        'public static void refillCurrentCraftGridFromBlueprintStacks(\n            ServerPlayer player,\n            List<ItemStack> blueprintStacks,\n            String craftedItemId,\n            int craftedCount) {\n        RtsStorageCrafting.refillCurrentCraftGridFromBlueprintStacks(\n                player,\n                RtsSessionService.getIfPresent(player),\n                blueprintStacks,\n                craftedItemId,\n                craftedCount);\n    }',
        'public static void refillCurrentCraftGridFromBlueprintStacks(\n            ServerPlayer player,\n            List<ItemStack> blueprintStacks,\n            String craftedItemId,\n            int craftedCount) {\n        RtsCraftingService.refillCurrentCraftGridFromBlueprintStacks(player, blueprintStacks, craftedItemId, craftedCount);\n    }',
    ),
    # === applyJeiTransfer ===
    (
        'public static void applyJeiTransfer(\n            ServerPlayer player,\n            String recipeId,\n            List<ItemStack> ingredientPrototypes,\n            boolean maxTransfer,\n            boolean clearGridFirst) {\n        if (!RtsProgressionManager.canUse(player, RtsFeature.JEI_TRANSFER)) {\n            return;\n        }\n        RtsStorageCrafting.applyJeiTransfer(\n                player,\n                RtsSessionService.getOrCreate(player),\n                recipeId,\n                ingredientPrototypes,\n                maxTransfer,\n                clearGridFirst);\n    }',
        'public static void applyJeiTransfer(\n            ServerPlayer player,\n            String recipeId,\n            List<ItemStack> ingredientPrototypes,\n            boolean maxTransfer,\n            boolean clearGridFirst) {\n        RtsCraftingService.applyJeiTransfer(player, recipeId, ingredientPrototypes, maxTransfer, clearGridFirst);\n    }',
    ),
    # === pickupLinkedToCarried ===
    (
        'public static void pickupLinkedToCarried(ServerPlayer player, ItemStack prototype, int amount) {\n        RtsStorageTransfers.pickupLinkedToCarried(player, RtsSessionService.getIfPresent(player), prototype, amount);\n    }',
        'public static void pickupLinkedToCarried(ServerPlayer player, ItemStack prototype, int amount) {\n        RtsTransferService.pickupLinkedToCarried(player, prototype, amount);\n    }',
    ),
    # === quickMoveLinkedItem ===
    (
        'public static void quickMoveLinkedItem(ServerPlayer player, ItemStack prototype) {\n        RtsStorageTransfers.quickMoveLinkedItem(player, RtsSessionService.getIfPresent(player), prototype);\n    }',
        'public static void quickMoveLinkedItem(ServerPlayer player, ItemStack prototype) {\n        RtsTransferService.quickMoveLinkedItem(player, prototype);\n    }',
    ),
    # === fillPlayerInventoryFromLinked ===
    (
        'public static void fillPlayerInventoryFromLinked(ServerPlayer player) {\n        RtsStorageTransfers.fillPlayerInventoryFromLinked(player, RtsSessionService.getIfPresent(player));\n    }',
        'public static void fillPlayerInventoryFromLinked(ServerPlayer player) {\n        RtsTransferService.fillPlayerInventoryFromLinked(player);\n    }',
    ),
    # === mine ===
    (
        'public static void mine(ServerPlayer player, BlockPos pos, Direction face, boolean start, byte toolSlot,\n            String toolItemId, ItemStack toolPrototype, boolean allowPlacedBlockRecovery,\n            boolean toolProtectionEnabled) {\n        RtsStorageMining.mine(\n                player,\n                RtsSessionService.getIfPresent(player),\n                pos,\n                face,\n                start,\n                toolSlot,\n                toolItemId,\n                toolPrototype,\n                allowPlacedBlockRecovery,\n                toolProtectionEnabled);\n    }',
        'public static void mine(ServerPlayer player, BlockPos pos, Direction face, boolean start, byte toolSlot,\n            String toolItemId, ItemStack toolPrototype, boolean allowPlacedBlockRecovery,\n            boolean toolProtectionEnabled) {\n        RtsMiningService.mine(player, pos, face, start, toolSlot, toolItemId, toolPrototype, allowPlacedBlockRecovery, toolProtectionEnabled);\n    }',
    ),
    # === startUltimine ===
    (
        'public static void startUltimine(ServerPlayer player, BlockPos pos, Direction face, byte toolSlot, String toolItemId,\n            ItemStack toolPrototype, int requestedLimit, byte mode, boolean toolProtectionEnabled) {\n        RtsStorageMining.startUltimine(\n                player,\n                RtsSessionService.getIfPresent(player),\n                pos,\n                face,\n                toolSlot,\n                toolItemId,\n                toolPrototype,\n                requestedLimit,\n                mode,\n                toolProtectionEnabled);\n    }',
        'public static void startUltimine(ServerPlayer player, BlockPos pos, Direction face, byte toolSlot, String toolItemId,\n            ItemStack toolPrototype, int requestedLimit, byte mode, boolean toolProtectionEnabled) {\n        RtsMiningService.startUltimine(player, pos, face, toolSlot, toolItemId, toolPrototype, requestedLimit, mode, toolProtectionEnabled);\n    }',
    ),
    # === areaMine ===
    (
        'public static void areaMine(ServerPlayer player,\n            int minX, int maxX, int minY, int maxY, int minZ, int maxZ,\n            byte toolSlot, String toolItemId, ItemStack toolPrototype,\n            byte shapeType, byte fillType, boolean toolProtectionEnabled) {\n        RtsStorageMining.areaMine(\n                player,\n                RtsSessionService.getIfPresent(player),\n                minX, maxX, minY, maxY, minZ, maxZ,\n                toolSlot,\n                toolItemId,\n                toolPrototype,\n                shapeType,\n                fillType,\n                toolProtectionEnabled);\n    }',
        'public static void areaMine(ServerPlayer player,\n            int minX, int maxX, int minY, int maxY, int minZ, int maxZ,\n            byte toolSlot, String toolItemId, ItemStack toolPrototype,\n            byte shapeType, byte fillType, boolean toolProtectionEnabled) {\n        RtsMiningService.areaMine(player, minX, maxX, minY, maxY, minZ, maxZ, toolSlot, toolItemId, toolPrototype, shapeType, fillType, toolProtectionEnabled);\n    }',
    ),
    # === areaDestroy ===
    (
        'public static void areaDestroy(ServerPlayer player, List<BlockPos> positions,\n            byte toolSlot, String toolItemId, ItemStack toolPrototype, boolean toolProtectionEnabled) {\n        RtsStorageMining.areaDestroy(\n                player,\n                RtsSessionService.getIfPresent(player),\n                positions,\n                toolSlot,\n                toolItemId,\n                toolPrototype,\n                toolProtectionEnabled);\n    }',
        'public static void areaDestroy(ServerPlayer player, List<BlockPos> positions,\n            byte toolSlot, String toolItemId, ItemStack toolPrototype, boolean toolProtectionEnabled) {\n        RtsMiningService.areaDestroy(player, positions, toolSlot, toolItemId, toolPrototype, toolProtectionEnabled);\n    }',
    ),
    # === withTemporaryMainHandItem ===
    (
        'public static <T> T withTemporaryMainHandItem(ServerPlayer player, ItemStack stack, Supplier<T> action) {\n        return RtsMiningStateMachine.withTemporaryMainHandItem(player, stack, action);\n    }',
        'public static <T> T withTemporaryMainHandItem(ServerPlayer player, ItemStack stack, Supplier<T> action) {\n        return RtsMiningService.withTemporaryMainHandItem(player, stack, action);\n    }',
    ),
    # === snapshotCraftGridBlueprint ===
    (
        'public static ItemStack[] snapshotCraftGridBlueprint(CraftingMenu menu) {\n        return RtsStorageCrafting.snapshotCraftGridBlueprint(menu);\n    }',
        'public static ItemStack[] snapshotCraftGridBlueprint(CraftingMenu menu) {\n        return RtsCraftingService.snapshotCraftGridBlueprint(menu);\n    }',
    ),
    # === refillCraftGridFromBlueprint ===
    (
        'public static void refillCraftGridFromBlueprint(CraftingMenu menu, List<IItemHandler> handlers, ServerPlayer player,\n            ItemStack[] blueprint, boolean fillAll, boolean includePlayerFallback) {\n        RtsStorageCrafting.refillCraftGridFromBlueprint(menu, handlers, player, blueprint, fillAll, includePlayerFallback);\n    }',
        'public static void refillCraftGridFromBlueprint(CraftingMenu menu, List<IItemHandler> handlers, ServerPlayer player,\n            ItemStack[] blueprint, boolean fillAll, boolean includePlayerFallback) {\n        RtsCraftingService.refillCraftGridFromBlueprint(menu, handlers, player, blueprint, fillAll, includePlayerFallback);\n    }',
    ),
    # === refillCraftGridFromLinked ===
    (
        'public static void refillCraftGridFromLinked(\n            ServerPlayer player,\n            CraftingMenu craftingMenu,\n            ItemStack[] blueprint,\n            CraftingRecipe recipe) {\n        RtsStorageCrafting.refillCraftGridFromLinked(player, RtsSessionService.getIfPresent(player), craftingMenu, blueprint, recipe);\n    }',
        'public static void refillCraftGridFromLinked(\n            ServerPlayer player,\n            CraftingMenu craftingMenu,\n            ItemStack[] blueprint,\n            CraftingRecipe recipe) {\n        RtsCraftingService.refillCraftGridFromLinked(player, craftingMenu, blueprint, recipe);\n    }',
    ),
    # === recordCraftedOutput ===
    (
        'public static void recordCraftedOutput(ServerPlayer player, ItemStack crafted) {\n        RtsStorageCrafting.recordCraftedOutput(player, RtsSessionService.getIfPresent(player), crafted);\n    }',
        'public static void recordCraftedOutput(ServerPlayer player, ItemStack crafted) {\n        RtsCraftingService.recordCraftedOutput(player, crafted);\n    }',
    ),
    # === recordRecentItem ===
    (
        'public static void recordRecentItem(RtsStorageSession session, String itemId, byte kind, long amount) {\n        RtsStorageRecentEntries.recordRecentItem(session, itemId, kind, amount);\n    }',
        'public static void recordRecentItem(RtsStorageSession session, String itemId, byte kind, long amount) {\n        RtsPageService.recordRecentItem(session, itemId, kind, amount);\n    }',
    ),
]

count = 0
for old, new in replacements:
    if old in content:
        content = content.replace(old, new)
        count += 1
    else:
        print(f"MISS: {old[:80]}...")

print(f"Applied {count}/{len(replacements)} replacements")

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("File saved successfully")
