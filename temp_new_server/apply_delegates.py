import re
import sys

filepath = r"E:\Minecraft\Minecraft Modding\RTSbuilding\src\main\java\com\rtsbuilding\rtsbuilding\server\RtsStorageManager.java"

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

original_len = len(content)

# Define replacements as (old, new) pairs
replacements = [
    # === openCraftTerminal ===
    (
        'public static void openCraftTerminal(ServerPlayer player) {\n        RtsStorageCrafting.openCraftTerminal(player, RtsSessionService.getIfPresent(player));\n    }',
        'public static void openCraftTerminal(ServerPlayer player) {\n        RtsCraftingService.openCraftTerminal(player);\n    }',
    ),
    # === rotateBlock ===
    (
        'public static void rotateBlock(ServerPlayer player, BlockPos pos) {\n        if (!RtsProgressionManager.canUse(player, RtsFeature.ROTATE_BLOCK)) {\n            return;\n        }\n        RtsStorageSession session = RtsSessionService.getIfPresent(player);\n        if (session == null || !RtsLinkedStorageResolver.canAccessWorldTarget(player, pos)) {\n            return;\n        }\n        RtsPlacementHelper.rotatePlacedBlock(player.serverLevel(), pos, (byte) 1);\n    }',
        'public static void rotateBlock(ServerPlayer player, BlockPos pos) {\n        RtsPlacementService.rotateBlock(player, pos);\n    }',
    ),
    # === countBlueprintMaterial ===
    (
        'public static long countBlueprintMaterial(ServerPlayer player, Item item) {\n        if (player == null || item == null || item == Items.AIR) {\n            return 0L;\n        }\n        RtsStorageSession session = RtsSessionService.getIfPresent(player);\n        if (session == null) {\n            return 0L;\n        }\n\n        long total = 0L;\n        for (LinkedHandler linkedHandler : RtsLinkedStorageResolver.resolveLinkedHandlers(player, session)) {\n            IItemHandler handler = linkedHandler.handler();\n            for (int slot = 0; slot < handler.getSlots(); slot++) {\n                ItemStack stack = handler.getStackInSlot(slot);\n                if (!stack.isEmpty() && stack.getItem() == item) {\n                    total = RtsCountUtil.saturatedAdd(total, RtsStoragePageBuilder.getHandlerReportedCount(handler, slot, stack));\n                }\n            }\n        }\n\n        int start = RtsStoragePageBuilder.getPlayerMainInventoryStart(player);\n        int end = RtsStoragePageBuilder.getPlayerMainInventoryEndExclusive(player);\n        for (int slot = start; slot < end; slot++) {\n            ItemStack stack = player.getInventory().getItem(slot);\n            if (!stack.isEmpty() && stack.getItem() == item) {\n                total = RtsCountUtil.saturatedAdd(total, stack.getCount());\n            }\n        }\n        return total;\n    }',
        'public static long countBlueprintMaterial(ServerPlayer player, Item item) {\n        return RtsBlueprintService.countBlueprintMaterial(player, item);\n    }',
    ),
    # === extractBlueprintMaterial ===
    (
        'public static ItemStack extractBlueprintMaterial(ServerPlayer player, Item item, int count) {\n        if (player == null || item == null || item == Items.AIR || count <= 0) {\n            return ItemStack.EMPTY;\n        }\n        RtsStorageSession session = RtsSessionService.getIfPresent(player);\n        if (session == null) {\n            return ItemStack.EMPTY;\n        }\n        List<LinkedHandler> activeLinked = RtsLinkedStorageResolver.resolveLinkedHandlers(player, session);\n        List<IItemHandler> handlers = RtsLinkedStorageResolver.itemHandlersForExtract(activeLinked);\n        return RtsStorageTransfers.extractMatchingFromNetwork(handlers, player, item, count);\n    }',
        'public static ItemStack extractBlueprintMaterial(ServerPlayer player, Item item, int count) {\n        return RtsBlueprintService.extractBlueprintMaterial(player, item, count);\n    }',
    ),
    # === countBlueprintFluidMb ===
    (
        'public static long countBlueprintFluidMb(ServerPlayer player, Fluid fluid) {\n        if (player == null || fluid == null) {\n            return 0L;\n        }\n        RtsStorageSession session = RtsSessionService.getIfPresent(player);\n        if (session == null) {\n            return 0L;\n        }\n        return RtsStorageFluids.countFluidInNetwork(session, RtsLinkedStorageResolver.resolveLinkedFluidHandlers(player, session), fluid);\n    }',
        'public static long countBlueprintFluidMb(ServerPlayer player, Fluid fluid) {\n        return RtsBlueprintService.countBlueprintFluidMb(player, fluid);\n    }',
    ),
    # === extractBlueprintFluid ===
    (
        'public static boolean extractBlueprintFluid(ServerPlayer player, Fluid fluid, int amountMb) {\n        if (player == null || fluid == null || amountMb <= 0) {\n            return false;\n        }\n        RtsStorageSession session = RtsSessionService.getIfPresent(player);\n        if (session == null) {\n            return false;\n        }\n        return RtsStorageFluids.extractFluidFromNetwork(\n                session,\n                RtsLinkedStorageResolver.resolveLinkedFluidHandlers(player, session),\n                fluid,\n                amountMb,\n                true) >= amountMb;\n    }',
        'public static boolean extractBlueprintFluid(ServerPlayer player, Fluid fluid, int amountMb) {\n        return RtsBlueprintService.extractBlueprintFluid(player, fluid, amountMb);\n    }',
    ),
    # === refundBlueprintMaterial ===
    (
        'public static void refundBlueprintMaterial(ServerPlayer player, ItemStack stack) {\n        if (player == null || stack == null || stack.isEmpty()) {\n            return;\n        }\n        RtsStorageSession session = RtsSessionService.getIfPresent(player);\n        List<IItemHandler> handlers = session == null\n                ? List.of()\n                : RtsLinkedStorageResolver.resolveLinkedHandlers(player, session).stream().map(LinkedHandler::handler).toList();\n        RtsStorageTransfers.refundToLinked(handlers, player, stack);\n    }',
        'public static void refundBlueprintMaterial(ServerPlayer player, ItemStack stack) {\n        RtsBlueprintService.refundBlueprintMaterial(player, stack);\n    }',
    ),
    # === noteBlueprintBlockPlaced ===
    (
        'public static void noteBlueprintBlockPlaced(ServerPlayer player, BlockPos pos, String itemId) {\n        if (player == null || pos == null) {\n            return;\n        }\n        RtsStorageSession session = RtsSessionService.getIfPresent(player);\n        if (session == null) {\n            return;\n        }\n        RtsPlacementSound.playRemotePlacedBlockSound(player, player.serverLevel(), pos);\n        recordRecentItem(session, itemId, S2CRtsStoragePagePayload.RECENT_ITEM_PLACED, 1L);\n    }',
        'public static void noteBlueprintBlockPlaced(ServerPlayer player, BlockPos pos, String itemId) {\n        RtsBlueprintService.noteBlueprintBlockPlaced(player, pos, itemId);\n    }',
    ),
    # === refreshBlueprintStoragePage ===
    (
        'public static void refreshBlueprintStoragePage(ServerPlayer player) {\n        RtsStorageSession session = player == null ? null : RtsSessionService.getIfPresent(player);\n        if (session == null) {\n            return;\n        }\n        requestPage(player, session.page, session.search, session.category, session.sort, session.ascending);\n    }',
        'public static void refreshBlueprintStoragePage(ServerPlayer player) {\n        RtsBlueprintService.refreshBlueprintStoragePage(player);\n    }',
    ),
    # === requestPage 4 overloads ===
    (
        'public static void requestPage(ServerPlayer player, int page, String search, String category, RtsStorageSort sort,\n            boolean ascending) {\n        requestPage(player, page, search, category, sort, ascending, currentPinyinSearchEnabled(player));\n    }',
        'public static void requestPage(ServerPlayer player, int page, String search, String category, RtsStorageSort sort,\n            boolean ascending) {\n        RtsPageService.requestPage(player, page, search, category, sort, ascending);\n    }',
    ),
    (
        'public static void requestPage(ServerPlayer player, int page, String search, String category, RtsStorageSort sort,\n            boolean ascending, boolean pinyinSearchEnabled) {\n        requestPage(\n                player,\n                page,\n                search,\n                category,\n                sort,\n                ascending,\n                pinyinSearchEnabled,\n                currentLocalizedSearchMatches(player));\n    }',
        'public static void requestPage(ServerPlayer player, int page, String search, String category, RtsStorageSort sort,\n            boolean ascending, boolean pinyinSearchEnabled) {\n        RtsPageService.requestPage(player, page, search, category, sort, ascending, pinyinSearchEnabled);\n    }',
    ),
    (
        'public static void requestPage(ServerPlayer player, int page, String search, String category, RtsStorageSort sort,\n            boolean ascending, boolean pinyinSearchEnabled, List<String> localizedSearchMatches) {\n        requestPage(player, page, search, category, sort, ascending, sessionPageSize(player), pinyinSearchEnabled, localizedSearchMatches);\n    }',
        'public static void requestPage(ServerPlayer player, int page, String search, String category, RtsStorageSort sort,\n            boolean ascending, boolean pinyinSearchEnabled, List<String> localizedSearchMatches) {\n        RtsPageService.requestPage(player, page, search, category, sort, ascending, pinyinSearchEnabled, localizedSearchMatches);\n    }',
    ),
    # requestPage final implementation - lots of lines
    (
        'public static void requestPage(ServerPlayer player, int page, String search, String category, RtsStorageSort sort,\n            boolean ascending, int pageSize, boolean pinyinSearchEnabled, List<String> localizedSearchMatches) {\n        if (!RtsProgressionManager.canUse(player, RtsFeature.STORAGE_BROWSER)) {\n            return;\n        }\n        RtsStorageSession session = RtsSessionService.getOrCreate(player);\n        refreshMissingGuiBindingIcons(player, session);\n        session.search = search == null ? "" : search;\n        session.category = normalizeCategory(category);\n        session.sort = sort == null ? RtsStorageSort.QUANTITY : sort;\n        session.ascending = ascending;\n        session.pageSize = RtsStoragePageBuilder.sanitizePageSize(pageSize);\n        session.pinyinSearchEnabled = pinyinSearchEnabled;\n        session.localizedSearchMatches.clear();\n        session.localizedSearchMatches.addAll(RtsStoragePageBuilder.sanitizeLocalizedSearchMatches(localizedSearchMatches));\n\n        RtsLinkedStorageResolver.sanitizeSessionDimension(player, session);\n        session.cachedBdHandler = null;\n        session.cachedBdFluidHandler = null;\n\n        List<LinkedHandler> activeHandlers = RtsLinkedStorageResolver.resolveLinkedHandlers(player, session);\n        List<LinkedFluidHandler> activeFluidHandlers = RtsLinkedStorageResolver.resolveLinkedFluidHandlers(player, session);\n        var result = RtsStoragePageBuilder.build(\n                player,\n                session,\n                page,\n                session.pageSize,\n                activeHandlers,\n                activeFluidHandlers);\n        PacketDistributor.sendToPlayer(player, result.payload());\n        session.storageViewDirty = false;\n        session.page = result.safePage();\n        RtsSessionService.saveToPlayerNbt(player, session);\n    }',
        'public static void requestPage(ServerPlayer player, int page, String search, String category, RtsStorageSort sort,\n            boolean ascending, int pageSize, boolean pinyinSearchEnabled, List<String> localizedSearchMatches) {\n        RtsPageService.requestPage(player, page, search, category, sort, ascending, pageSize, pinyinSearchEnabled, localizedSearchMatches);\n    }',
    ),
    # === markStorageViewDirty ===
    (
        'public static void markStorageViewDirty(ServerPlayer player, RtsStorageSession session) {\n        if (player == null || session == null) {\n            return;\n        }\n        if (!RtsProgressionManager.canUse(player, RtsFeature.STORAGE_BROWSER)) {\n            return;\n        }\n        if (session.storageViewDirty) {\n            return;\n        }\n        session.storageViewDirty = true;\n        PacketDistributor.sendToPlayer(player, new S2CRtsStorageDirtyPayload(true));\n    }',
        'public static void markStorageViewDirty(ServerPlayer player, RtsStorageSession session) {\n        RtsPageService.markStorageViewDirty(player, session);\n    }',
    ),
    # === requestCraftables 3 overloads ===
    (
        'public static void requestCraftables(ServerPlayer player, String search, boolean showUnavailable, int offset, int limit) {\n        requestCraftables(player, search, showUnavailable, offset, limit, currentCraftPinyinSearchEnabled(player));\n    }',
        'public static void requestCraftables(ServerPlayer player, String search, boolean showUnavailable, int offset, int limit) {\n        RtsCraftingService.requestCraftables(player, search, showUnavailable, offset, limit);\n    }',
    ),
    (
        'public static void requestCraftables(ServerPlayer player, String search, boolean showUnavailable, int offset, int limit,\n            boolean pinyinSearchEnabled) {\n        requestCraftables(\n                player,\n                search,\n                showUnavailable,\n                offset,\n                limit,\n                pinyinSearchEnabled,\n                currentCraftLocalizedSearchMatches(player));\n    }',
        'public static void requestCraftables(ServerPlayer player, String search, boolean showUnavailable, int offset, int limit,\n            boolean pinyinSearchEnabled) {\n        RtsCraftingService.requestCraftables(player, search, showUnavailable, offset, limit, pinyinSearchEnabled);\n    }',
    ),
    (
        'public static void requestCraftables(ServerPlayer player, String search, boolean showUnavailable, int offset, int limit,\n            boolean pinyinSearchEnabled, List<String> localizedSearchMatches) {\n        if (!RtsProgressionManager.canUse(player, RtsFeature.CRAFT_TERMINAL)) {\n            return;\n        }\n        RtsStorageCrafting.requestCraftables(\n                player,\n                RtsSessionService.getOrCreate(player),\n                search,\n                showUnavailable,\n                offset,\n                limit,\n                pinyinSearchEnabled,\n                localizedSearchMatches);\n    }',
        'public static void requestCraftables(ServerPlayer player, String search, boolean showUnavailable, int offset, int limit,\n            boolean pinyinSearchEnabled, List<String> localizedSearchMatches) {\n        RtsCraftingService.requestCraftables(player, search, showUnavailable, offset, limit, pinyinSearchEnabled, localizedSearchMatches);\n    }',
    ),
    # === craftRecipeToLinked ===
    (
        'public static void craftRecipeToLinked(ServerPlayer player, String recipeId, int craftCount) {\n        if (!RtsProgressionManager.canUse(player, RtsFeature.CRAFT_TERMINAL)) {\n            return;\n        }\n        RtsStorageCrafting.craftRecipeToLinked(player, RtsSessionService.getOrCreate(player), recipeId, craftCount);\n    }',
        'public static void craftRecipeToLinked(ServerPlayer player, String recipeId, int craftCount) {\n        RtsCraftingService.craftRecipeToLinked(player, recipeId, craftCount);\n    }',
    ),
    # === placeSelected ===
    (
        'public static void placeSelected(ServerPlayer player, BlockPos clickedPos, Direction face, double hitX, double hitY,\n            double hitZ, byte rotateSteps, boolean forcePlace, boolean skipIfOccupied, String itemId,\n            ItemStack itemPrototype, double rayOriginX, double rayOriginY, double rayOriginZ,\n            double rayDirX, double rayDirY, double rayDirZ, boolean quickBuild, boolean forceEmptyHand) {\n        double hitOffsetX = clickedPos == null ? 0.5D : hitX - clickedPos.getX();\n        double hitOffsetY = clickedPos == null ? 0.5D : hitY - clickedPos.getY();\n        double hitOffsetZ = clickedPos == null ? 0.5D : hitZ - clickedPos.getZ();\n        RtsPlacementBatch.enqueuePlaceBatch(\n                player,\n                player == null ? null : RtsSessionService.getIfPresent(player),\n                clickedPos == null ? List.of() : List.of(clickedPos),\n                face,\n                hitOffsetX,\n                hitOffsetY,\n                hitOffsetZ,\n                rotateSteps,\n                forcePlace,\n                skipIfOccupied,\n                itemId,\n                itemPrototype,\n                rayOriginX,\n                rayOriginY,\n                rayOriginZ,\n                rayDirX,\n                rayDirY,\n                rayDirZ,\n                quickBuild,\n                forceEmptyHand,\n                true);\n    }',
        'public static void placeSelected(ServerPlayer player, BlockPos clickedPos, Direction face, double hitX, double hitY,\n            double hitZ, byte rotateSteps, boolean forcePlace, boolean skipIfOccupied, String itemId,\n            ItemStack itemPrototype, double rayOriginX, double rayOriginY, double rayOriginZ,\n            double rayDirX, double rayDirY, double rayDirZ, boolean quickBuild, boolean forceEmptyHand) {\n        RtsPlacementService.placeSelected(player, clickedPos, face, hitX, hitY, hitZ, rotateSteps, forcePlace, skipIfOccupied,\n                itemId, itemPrototype, rayOriginX, rayOriginY, rayOriginZ, rayDirX, rayDirY, rayDirZ, quickBuild, forceEmptyHand);\n    }',
    ),
    # === enqueuePlaceBatch ===
    (
        'public static void enqueuePlaceBatch(ServerPlayer player, List<BlockPos> clickedPositions, Direction face,\n            double hitOffsetX, double hitOffsetY, double hitOffsetZ, byte rotateSteps,\n            boolean forcePlace, boolean skipIfOccupied, String itemId,\n            ItemStack itemPrototype, double rayOriginX, double rayOriginY, double rayOriginZ,\n            double rayDirX, double rayDirY, double rayDirZ) {\n        RtsPlacementBatch.enqueuePlaceBatch(\n                player,\n                player == null ? null : RtsSessionService.getIfPresent(player),\n                clickedPositions,\n                face,\n                hitOffsetX,\n                hitOffsetY,\n                hitOffsetZ,\n                rotateSteps,\n                forcePlace,\n                skipIfOccupied,\n                itemId == null ? "" : itemId,\n                itemPrototype,\n                rayOriginX,\n                rayOriginY,\n                rayOriginZ,\n                rayDirX,\n                rayDirY,\n                rayDirZ,\n                true,\n                false,\n                false);\n    }',
        'public static void enqueuePlaceBatch(ServerPlayer player, List<BlockPos> clickedPositions, Direction face,\n            double hitOffsetX, double hitOffsetY, double hitOffsetZ, byte rotateSteps,\n            boolean forcePlace, boolean skipIfOccupied, String itemId,\n            ItemStack itemPrototype, double rayOriginX, double rayOriginY, double rayOriginZ,\n            double rayDirX, double rayDirY, double rayDirZ) {\n        RtsPlacementService.enqueuePlaceBatch(player, clickedPositions, face, hitOffsetX, hitOffsetY, hitOffsetZ, rotateSteps,\n                forcePlace, skipIfOccupied, itemId, itemPrototype, rayOriginX, rayOriginY, rayOriginZ, rayDirX, rayDirY, rayDirZ);\n    }',
    ),
    # === storeFluidFromContainer ===
    (
        'public static void storeFluidFromContainer(ServerPlayer player, byte sourceType, byte toolSlot, String itemId) {\n        if (!RtsProgressionManager.canUse(player, RtsFeature.FLUID_HANDLING)) {\n            return;\n        }\n        RtsStorageSession session = RtsSessionService.getOrCreate(player);\n        if (!RtsCameraManager.isActive(player)) {\n            return;\n        }\n        RtsLinkedStorageResolver.sanitizeSessionDimension(player, session);\n\n        List<LinkedHandler> activeItemHandlers = RtsLinkedStorageResolver.resolveLinkedHandlers(player, session);\n        List<LinkedFluidHandler> activeFluidHandlers = RtsLinkedStorageResolver.resolveLinkedFluidHandlers(player, session);\n        List<IItemHandler> extractItemHandlers = RtsLinkedStorageResolver.itemHandlersForExtract(activeItemHandlers);\n        List<IItemHandler> insertItemHandlers = RtsLinkedStorageResolver.itemHandlersForInsert(activeItemHandlers);\n\n        boolean changed = RtsStorageFluids.storeFluidFromContainer(\n                player,\n                session,\n                extractItemHandlers,\n                insertItemHandlers,\n                activeFluidHandlers,\n                sourceType,\n                toolSlot,\n                itemId);\n        if (changed) {\n            RtsSessionService.saveToPlayerNbt(player, session);\n            requestPage(player, session.page, session.search, session.category, session.sort, session.ascending);\n        }\n    }',
        'public static void storeFluidFromContainer(ServerPlayer player, byte sourceType, byte toolSlot, String itemId) {\n        RtsFluidService.storeFluidFromContainer(player, sourceType, toolSlot, itemId);\n    }',
    ),
    # === placeFluid ===
    (
        'public static void placeFluid(ServerPlayer player, BlockPos clickedPos, Direction face, double hitX, double hitY,\n            double hitZ, boolean forcePlace, String fluidId,\n            double rayOriginX, double rayOriginY, double rayOriginZ,\n            double rayDirX, double rayDirY, double rayDirZ) {\n        if (!RtsProgressionManager.canUse(player, RtsFeature.FLUID_HANDLING)) {\n            return;\n        }\n        RtsStorageSession session = RtsSessionService.getIfPresent(player);\n        if (session == null || !canAccessFluidPlacementTarget(player, clickedPos)) {\n            return;\n        }\n        RtsLinkedStorageResolver.sanitizeSessionDimension(player, session);\n        List<LinkedFluidHandler> activeFluidHandlers = RtsLinkedStorageResolver.resolveLinkedFluidHandlers(player, session);\n        if (RtsStorageFluids.placeFluid(player, session, activeFluidHandlers, clickedPos, face, hitX, hitY, hitZ, fluidId)) {\n            RtsSessionService.saveToPlayerNbt(player, session);\n            requestPage(player, session.page, session.search, session.category, session.sort, session.ascending);\n        }\n    }',
        'public static void placeFluid(ServerPlayer player, BlockPos clickedPos, Direction face, double hitX, double hitY,\n            double hitZ, boolean forcePlace, String fluidId,\n            double rayOriginX, double rayOriginY, double rayOriginZ,\n            double rayDirX, double rayDirY, double rayDirZ) {\n        RtsFluidService.placeFluid(player, clickedPos, face, hitX, hitY, hitZ, forcePlace, fluidId, rayOriginX, rayOriginY, rayOriginZ, rayDirX, rayDirY, rayDirZ);\n    }',
    ),
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
]

count = 0
for old, new in replacements:
    if old in content:
        content = content.replace(old, new)
        count += 1
    else:
        print(f"MISS: {old[:80]}...")

print(f"Applied {count}/{len(replacements)} replacements")
print(f"File size: {original_len} -> {len(content)} chars ({len(content) - original_len} net change)")

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("File saved successfully")
