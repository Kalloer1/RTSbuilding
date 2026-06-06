package com.rtsbuilding.rtsbuilding.server.storage;

import java.util.ArrayList;
import java.util.List;

import com.rtsbuilding.rtsbuilding.network.builder.C2SRtsPlaceBatchPayload;
import com.rtsbuilding.rtsbuilding.network.builder.S2CRtsPlaceAnimationPayload;
import com.rtsbuilding.rtsbuilding.network.storage.S2CRtsStoragePagePayload;
import com.rtsbuilding.rtsbuilding.progression.RtsFeature;
import com.rtsbuilding.rtsbuilding.server.RtsStorageManager;
import com.rtsbuilding.rtsbuilding.server.data.PlacedBlockTrackerData;

import com.rtsbuilding.rtsbuilding.server.progression.RtsProgressionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Executes RTS world block placement for the storage-powered builder tools.
 *
 * <p>This service owns the player-facing placement state machine: single
 * remote placement, queued placement batches, batch cursor progress,
 * material extraction/refund call boundaries, placed-block detection/tracking,
 * placement rotation, and quick-build placement sounds. It deliberately does
 * not build storage browser pages, resolve crafting recipes, move fluids, run
 * remote mining or Ultimine, persist sessions, or implement item insertion
 * fallbacks. Those responsibilities stay in their dedicated helpers; this class
 * calls {@link RtsStorageTransfers} only at the extraction/refund boundary so
 * NBT-heavy and capability-backed stacks keep the same behavior as before.
 */
public final class RtsStoragePlacement {
    private static final double REMOTE_POV_BLOCK_REACH = 4.0D;
    private static final int BUILD_BATCH_BLOCKS_PER_TICK = 64;
    private static final int BUILD_BATCH_MAX_QUEUED_JOBS = 4;
    private static final int QUICK_BUILD_COMPLETION_SOUND_DELAY_TICKS = 3;

    private RtsStoragePlacement() {
    }

    public static void placeSelected(ServerPlayer player, RtsStorageSession session, BlockPos clickedPos, Direction face,
            double hitX, double hitY, double hitZ, byte rotateSteps, boolean forcePlace, boolean skipIfOccupied,
            String itemId, ItemStack itemPrototype, double rayOriginX, double rayOriginY, double rayOriginZ,
            double rayDirX, double rayDirY, double rayDirZ, boolean quickBuild) {
        double hitOffsetX = clickedPos == null ? 0.5D : hitX - clickedPos.getX();
        double hitOffsetY = clickedPos == null ? 0.5D : hitY - clickedPos.getY();
        double hitOffsetZ = clickedPos == null ? 0.5D : hitZ - clickedPos.getZ();
        enqueuePlaceBatch(
                player,
                session,
                clickedPos == null ? List.of() : List.of(clickedPos),
                face,
                hitOffsetX,
                hitOffsetY,
                hitOffsetZ,
                rotateSteps,
                forcePlace,
                skipIfOccupied,
                itemId,
                itemPrototype,
                rayOriginX,
                rayOriginY,
                rayOriginZ,
                rayDirX,
                rayDirY,
                rayDirZ,
                quickBuild,
                true);
    }

    public static void enqueuePlaceBatch(ServerPlayer player, RtsStorageSession session, List<BlockPos> clickedPositions,
            Direction face, double hitOffsetX, double hitOffsetY, double hitOffsetZ, byte rotateSteps,
            boolean forcePlace, boolean skipIfOccupied, String itemId, ItemStack itemPrototype,
            double rayOriginX, double rayOriginY, double rayOriginZ, double rayDirX, double rayDirY,
            double rayDirZ, boolean quickBuild, boolean sendRemoteHint) {
        if (!RtsProgressionManager.canUse(player, RtsFeature.REMOTE_PLACE)) {
            return;
        }
        if (session == null || clickedPositions == null || clickedPositions.isEmpty() || face == null) {
            return;
        }
        RtsLinkedStorageResolver.sanitizeSessionDimension(player, session);
        List<BlockPos> positions = new ArrayList<>(Math.min(clickedPositions.size(), C2SRtsPlaceBatchPayload.MAX_POSITIONS));
        for (BlockPos pos : clickedPositions) {
            if (pos == null || !RtsLinkedStorageResolver.canAccessWorldTarget(player, pos)) {
                continue;
            }
            positions.add(pos.immutable());
            if (positions.size() >= C2SRtsPlaceBatchPayload.MAX_POSITIONS) {
                break;
            }
        }
        if (positions.isEmpty()) {
            return;
        }
        while (session.placeBatchJobs.size() >= BUILD_BATCH_MAX_QUEUED_JOBS) {
            session.placeBatchJobs.removeFirst();
        }
        session.placeBatchJobs.addLast(new PlaceBatchJob(
                positions,
                face,
                sanitizeHitOffset(hitOffsetX, face, Direction.Axis.X),
                sanitizeHitOffset(hitOffsetY, face, Direction.Axis.Y),
                sanitizeHitOffset(hitOffsetZ, face, Direction.Axis.Z),
                rotateSteps,
                forcePlace,
                skipIfOccupied,
                itemId == null ? "" : itemId,
                sanitizePrototype(itemId, itemPrototype),
                rayOriginX,
                rayOriginY,
                rayOriginZ,
                rayDirX,
                rayDirY,
                rayDirZ,
                quickBuild,
                sendRemoteHint));
    }

    public static void tickPlaceBatchJobs(ServerPlayer player, RtsStorageSession session) {
        if (player == null || session == null) {
            return;
        }
        int remaining = BUILD_BATCH_BLOCKS_PER_TICK;
        boolean finishedJob = false;
        while (remaining > 0 && !session.placeBatchJobs.isEmpty()) {
            PlaceBatchJob job = session.placeBatchJobs.peekFirst();
            while (remaining > 0 && job.hasNext()) {
                BlockPos clickedPos = job.next();
                StatePlacementPlan statePlan = job.quickBuild() ? job.statePlacementPlan(player) : null;
                boolean keepGoing;
                if (statePlan != null) {
                    keepGoing = placeStateBatchEntry(player, session, clickedPos, statePlan);
                } else {
                    Vec3 hitLocation = new Vec3(
                            clickedPos.getX() + job.hitOffsetX(),
                            clickedPos.getY() + job.hitOffsetY(),
                            clickedPos.getZ() + job.hitOffsetZ());
                    keepGoing = placeSelectedInternal(
                            player,
                            session,
                            clickedPos,
                            job.face(),
                            hitLocation.x,
                            hitLocation.y,
                            hitLocation.z,
                            job.rotateSteps(),
                            job.forcePlace(),
                            job.skipIfOccupied(),
                            job.itemId(),
                            job.itemPrototype(),
                            job.rayOriginX(),
                            job.rayOriginY(),
                            job.rayOriginZ(),
                            job.rayDirX(),
                            job.rayDirY(),
                            job.rayDirZ(),
                            job.quickBuild(),
                            false,
                            job.sendRemoteHint());
                }
                remaining--;
                if (!keepGoing) {
                    session.placeBatchJobs.removeFirst();
                    finishedJob = true;
                    break;
                }
            }
            if (!session.placeBatchJobs.isEmpty() && session.placeBatchJobs.peekFirst() == job && !job.hasNext()) {
                session.placeBatchJobs.removeFirst();
                finishedJob = true;
            }
        }
        if (finishedJob) {
            RtsStorageManager.saveSessionToPlayerNbt(player, session);
            RtsStorageManager.requestPage(player, session.page, session.search, session.category, session.sort, session.ascending);
        }
    }

    private static StatePlacementPlan resolveStatePlacementPlan(ServerPlayer player, PlaceBatchJob job) {
        if (player == null || job == null || !job.quickBuild()) {
            return null;
        }

        boolean useSelectedStorageItem = job.itemId() != null && !job.itemId().isBlank();
        Item item;
        ItemStack templateStack;
        if (useSelectedStorageItem) {
            ResourceLocation id = ResourceLocation.tryParse(job.itemId());
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                return null;
            }
            item = BuiltInRegistries.ITEM.get(id);
            templateStack = job.itemPrototype();
            if (templateStack.isEmpty()) {
                templateStack = new ItemStack(item);
            }
        } else {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.isEmpty()) {
                return null;
            }
            item = mainHand.getItem();
            templateStack = mainHand.copy();
        }

        if (!(item instanceof BlockItem blockItem)) {
            return null;
        }

        BlockPos templatePos = job.templatePosition();
        if (templatePos == null || job.face() == null || !player.serverLevel().hasChunkAt(templatePos)) {
            return null;
        }
        templateStack.setCount(1);
        BlockPlaceContext context = new BlockPlaceContext(
                player.serverLevel(),
                player,
                InteractionHand.MAIN_HAND,
                templateStack,
                job.templateHit(templatePos));
        BlockState state = blockItem.getBlock().getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        ResourceLocation sourceId = BuiltInRegistries.ITEM.getKey(item);
        if (sourceId == null) {
            return null;
        }
        return new StatePlacementPlan(
                item,
                templateStack,
                rotateState(state, job.rotateSteps()),
                useSelectedStorageItem,
                sourceId.toString());
    }

    private static boolean placeStateBatchEntry(ServerPlayer player, RtsStorageSession session, BlockPos targetPos,
            StatePlacementPlan plan) {
        if (!RtsProgressionManager.canUse(player, RtsFeature.REMOTE_PLACE)) {
            return false;
        }
        if (session == null || targetPos == null || plan == null) {
            return false;
        }
        if (!RtsLinkedStorageResolver.canAccessWorldTarget(player, targetPos)) {
            return false;
        }
        RtsLinkedStorageResolver.sanitizeSessionDimension(player, session);

        ServerLevel level = player.serverLevel();
        if (!canPlaceStateAt(level, player, targetPos, plan.state())) {
            return true;
        }

        ItemStack placementStack = plan.templateStack();
        ItemStack extracted = ItemStack.EMPTY;
        boolean refundExtractedOnFailure = false;
        List<IItemHandler> insertHandlers = List.of();
        if (plan.selectedStorageItem()) {
            List<LinkedHandler> activeLinked = RtsLinkedStorageResolver.resolveLinkedHandlers(player, session);
            boolean includePlayerMainInventory = RtsStoragePageBuilder.shouldIncludePlayerMainInventoryInStorageView(player, session);
            boolean creativeSource = player.isCreative();
            if (activeLinked.isEmpty() && !includePlayerMainInventory && !creativeSource) {
                return false;
            }
            List<IItemHandler> extractHandlers = RtsLinkedStorageResolver.itemHandlersForExtract(activeLinked);
            insertHandlers = RtsLinkedStorageResolver.itemHandlersForInsert(activeLinked);
            extracted = creativeSource
                    ? creativeStack(plan.item(), plan.templateStack())
                    : includePlayerMainInventory
                            ? extractSelectedFromNetwork(extractHandlers, player, plan.item(), plan.templateStack())
                            : extractSelectedFromLinked(extractHandlers, plan.item(), plan.templateStack());
            if (extracted.isEmpty()) {
                return false;
            }
            refundExtractedOnFailure = !creativeSource;
            placementStack = extracted.copy();
            placementStack.setCount(1);
        } else if (!player.isCreative()) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.isEmpty() || !ItemStack.isSameItemSameComponents(mainHand, plan.templateStack())) {
                return false;
            }
            placementStack = mainHand.copy();
            placementStack.setCount(1);
        }

        boolean placed = level.setBlock(targetPos, plan.state(), 3);
        if (!placed) {
            if (refundExtractedOnFailure && !extracted.isEmpty()) {
                RtsStorageTransfers.refundToLinked(insertHandlers, player, extracted);
            }
            return true;
        }

        BlockState placedState = level.getBlockState(targetPos);
        if (placedState.is(plan.state().getBlock())) {
            BlockItem.updateCustomBlockEntityTag(level, player, targetPos, placementStack);
            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (blockEntity != null) {
                blockEntity.applyComponentsFromItemStack(placementStack);
                blockEntity.setChanged();
            }
            placedState.getBlock().setPlacedBy(level, targetPos, placedState, player, placementStack);
        }
        if (!plan.selectedStorageItem() && !player.isCreative()) {
            player.getMainHandItem().shrink(1);
        }
        PlacedBlockTrackerData.get(level).mark(targetPos);
        playRemotePlacedBlockAnimation(player, targetPos);
        playRemotePlacedBlockSound(player, level, session, targetPos, true);
        RtsStorageManager.recordRecentItem(session, plan.itemId(), S2CRtsStoragePagePayload.RECENT_ITEM_PLACED, 1L);
        return true;
    }

    private static boolean canPlaceStateAt(ServerLevel level, ServerPlayer player, BlockPos targetPos, BlockState state) {
        if (level == null || targetPos == null || state == null || !level.hasChunkAt(targetPos)) {
            return false;
        }
        BlockState current = level.getBlockState(targetPos);
        if (!current.isAir() && !current.canBeReplaced()) {
            return false;
        }
        CollisionContext collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        return state.canSurvive(level, targetPos) && level.isUnobstructed(state, targetPos, collision);
    }

    private static BlockState rotateState(BlockState state, byte rotateSteps) {
        int turns = rotateSteps & 3;
        BlockState rotated = state;
        for (int i = 0; i < turns; i++) {
            rotated = rotated.rotate(Rotation.CLOCKWISE_90);
        }
        return rotated;
    }

    private static boolean placeSelectedInternal(ServerPlayer player, RtsStorageSession session, BlockPos clickedPos,
            Direction face, double hitX, double hitY, double hitZ, byte rotateSteps, boolean forcePlace,
            boolean skipIfOccupied, String itemId, ItemStack itemPrototype, double rayOriginX, double rayOriginY,
            double rayOriginZ, double rayDirX, double rayDirY, double rayDirZ, boolean quickBuild,
            boolean refreshStoragePage, boolean sendRemoteHint) {
        if (!RtsProgressionManager.canUse(player, RtsFeature.REMOTE_PLACE)) {
            return false;
        }
        if (session == null || !RtsLinkedStorageResolver.canAccessWorldTarget(player, clickedPos) || face == null) {
            return false;
        }
        RtsLinkedStorageResolver.sanitizeSessionDimension(player, session);
        boolean useSelectedStorageItem = itemId != null && !itemId.isBlank();

        ServerLevel level = player.serverLevel();
        Vec3 hitLocation = new Vec3(hitX, hitY, hitZ);
        BlockHitResult hit = new BlockHitResult(hitLocation, face, clickedPos, false);
        Vec3 interactionPos = RtsStorageManager.resolveInteractionPosition(null, hit, hitLocation);
        RtsStorageManager.RayContext rayContext = RtsStorageManager.parseRayContext(
                rayOriginX, rayOriginY, rayOriginZ,
                rayDirX, rayDirY, rayDirZ);
        if (sendRemoteHint) {
            RtsStorageManager.sendRemoteMenuOpenHint(player, clickedPos);
        }

        if (!useSelectedStorageItem) {
            ItemStack sourceSnapshot = player.getMainHandItem().copy();
            boolean sourcePlacesBlock = sourceSnapshot.getItem() instanceof BlockItem;
            if (skipIfOccupied && player.getMainHandItem().getItem() instanceof BlockItem) {
                if (!level.hasChunkAt(clickedPos) || !level.getBlockState(clickedPos).canBeReplaced()) {
                    requestSessionPage(player, session, refreshStoragePage);
                    return true;
                }
            }

            BlockState beforeClicked = level.getBlockState(clickedPos);
            BlockPos adjacentPos = clickedPos.relative(face);
            BlockState beforeAdjacent = level.hasChunkAt(adjacentPos) ? level.getBlockState(adjacentPos) : null;

            AbstractContainerMenu menuBeforeMainHandUse = player.containerMenu;
            InteractionResult mainHandUse = RtsStorageManager.withTemporaryUseItemContext(
                    player,
                    interactionPos,
                    hitLocation,
                    rayContext,
                    REMOTE_POV_BLOCK_REACH,
                    () -> RtsStorageManager.withTemporaryShiftKey(player, forcePlace, () -> player.gameMode.useItemOn(
                            player,
                            level,
                            player.getMainHandItem(),
                            InteractionHand.MAIN_HAND,
                            hit)));
            AbstractContainerMenu menuAfterMainHandUse = player.containerMenu;
            if (menuAfterMainHandUse != menuBeforeMainHandUse) {
                RtsStorageManager.markRemoteMenuOpen(player, session, menuAfterMainHandUse, clickedPos);
                return false;
            }

            if (mainHandUse.consumesAction()) {
                BlockPos placedPos = detectPlacedPos(level, clickedPos, beforeClicked, adjacentPos, beforeAdjacent);
                if (placedPos != null) {
                    PlacedBlockTrackerData.get(level).mark(placedPos);
                    if (sourcePlacesBlock) {
                        playRemotePlacedBlockAnimation(player, placedPos);
                        playRemotePlacedBlockSound(player, level, session, placedPos, quickBuild);
                    } else {
                        RtsStorageManager.playRemoteUseSound(player, level, null, placedPos, sourceSnapshot);
                    }
                    ResourceLocation sourceId = BuiltInRegistries.ITEM.getKey(sourceSnapshot.getItem());
                    if (sourceId != null) {
                        RtsStorageManager.recordRecentItem(
                                session,
                                sourceId.toString(),
                                S2CRtsStoragePagePayload.RECENT_ITEM_PLACED,
                                1L);
                    }
                } else if (!sourceSnapshot.isEmpty()) {
                    RtsStorageManager.playRemoteUseSound(player, level, null, clickedPos, sourceSnapshot);
                    ResourceLocation sourceId = BuiltInRegistries.ITEM.getKey(sourceSnapshot.getItem());
                    if (sourceId != null) {
                        RtsStorageManager.recordRecentItem(
                                session,
                                sourceId.toString(),
                                S2CRtsStoragePagePayload.RECENT_ITEM_USED,
                                1L);
                    }
                }
                RtsStorageManager.saveSessionToPlayerNbt(player, session);
                return true;
            }

            // Some items (e.g. bucket) work via "use in air" fallback instead of use-on-block.
            AbstractContainerMenu menuBeforeUseFallback = player.containerMenu;
            InteractionResult mainHandUseFallback = RtsStorageManager.withTemporaryUseItemContext(
                    player,
                    interactionPos,
                    hitLocation,
                    rayContext,
                    REMOTE_POV_BLOCK_REACH,
                    () -> RtsStorageManager.withTemporaryShiftKey(player, forcePlace, () -> player.gameMode.useItem(
                            player,
                            level,
                            player.getMainHandItem(),
                            InteractionHand.MAIN_HAND)));
            AbstractContainerMenu menuAfterUseFallback = player.containerMenu;
            if (menuAfterUseFallback != menuBeforeUseFallback) {
                RtsStorageManager.markRemoteMenuOpen(player, session, menuAfterUseFallback, clickedPos);
                return false;
            }
            if (mainHandUseFallback.consumesAction()) {
                if (!sourceSnapshot.isEmpty()) {
                    RtsStorageManager.playRemoteUseSound(player, level, null, clickedPos, sourceSnapshot);
                    ResourceLocation sourceId = BuiltInRegistries.ITEM.getKey(sourceSnapshot.getItem());
                    if (sourceId != null) {
                        RtsStorageManager.recordRecentItem(
                                session,
                                sourceId.toString(),
                                S2CRtsStoragePagePayload.RECENT_ITEM_USED,
                                1L);
                    }
                }
                RtsStorageManager.saveSessionToPlayerNbt(player, session);
                return true;
            }

            return false;
        }

        List<LinkedHandler> activeLinked = RtsLinkedStorageResolver.resolveLinkedHandlers(player, session);
        boolean includePlayerMainInventory = RtsStoragePageBuilder.shouldIncludePlayerMainInventoryInStorageView(player, session);
        // Creative RTS picking uses the item id as a creative source, while survival still extracts
        // the real stack from linked storage/player inventory so storage balance remains unchanged.
        boolean creativeSource = player.isCreative();
        if (activeLinked.isEmpty() && !includePlayerMainInventory && !creativeSource) {
            return false;
        }

        List<IItemHandler> extractHandlers = RtsLinkedStorageResolver.itemHandlersForExtract(activeLinked);
        List<IItemHandler> insertHandlers = RtsLinkedStorageResolver.itemHandlersForInsert(activeLinked);

        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return false;
        }

        Item item = BuiltInRegistries.ITEM.get(id);
        ItemStack preferredStack = sanitizePrototype(itemId, itemPrototype);
        if (skipIfOccupied && item instanceof BlockItem) {
            if (!level.hasChunkAt(clickedPos) || !level.getBlockState(clickedPos).canBeReplaced()) {
                requestSessionPage(player, session, refreshStoragePage);
                return true;
            }
        }
        ItemStack extracted = creativeSource
                ? creativeStack(item, preferredStack)
                : includePlayerMainInventory
                        ? extractSelectedFromNetwork(extractHandlers, player, item, preferredStack)
                        : extractSelectedFromLinked(extractHandlers, item, preferredStack);
        if (extracted.isEmpty()) {
            requestSessionPage(player, session, refreshStoragePage);
            return false;
        }
        ItemStack selectedSoundStack = extracted.copy();
        boolean selectedPlacesBlock = item instanceof BlockItem;

        BlockState beforeClicked = level.getBlockState(clickedPos);
        BlockPos adjacentPos = clickedPos.relative(face);
        BlockState beforeAdjacent = level.hasChunkAt(adjacentPos) ? level.getBlockState(adjacentPos) : null;

        AbstractContainerMenu menuBeforeSelectedUse = player.containerMenu;
        RtsStorageManager.UseOnOutcome selectedOutcome = RtsStorageManager.withTemporaryUseItemContext(
                player,
                interactionPos,
                hitLocation,
                rayContext,
                REMOTE_POV_BLOCK_REACH,
                () -> RtsStorageManager.useItemOnWithMainHand(player, level, extracted, hit, forcePlace));
        AbstractContainerMenu menuAfterSelectedUse = player.containerMenu;
        if (menuAfterSelectedUse != menuBeforeSelectedUse) {
            RtsStorageManager.markRemoteMenuOpen(player, session, menuAfterSelectedUse, clickedPos);
        }

        RtsStorageManager.UseOnOutcome finalOutcome = selectedOutcome;
        if (!selectedOutcome.result().consumesAction()) {
            ItemStack fallbackStack = selectedOutcome.remainder().isEmpty() ? extracted.copy() : selectedOutcome.remainder().copy();
            finalOutcome = RtsStorageManager.withTemporaryUseItemContext(
                    player,
                    interactionPos,
                    hitLocation,
                    rayContext,
                    REMOTE_POV_BLOCK_REACH,
                    () -> RtsStorageManager.useItemWithMainHand(player, level, fallbackStack, forcePlace));
        }
        if (!creativeSource && !finalOutcome.remainder().isEmpty()) {
            RtsStorageTransfers.refundToLinked(insertHandlers, player, finalOutcome.remainder());
        }

        if (!finalOutcome.result().consumesAction()) {
            requestSessionPage(player, session, refreshStoragePage);
            return false;
        }

        BlockPos placedPos = detectPlacedPos(level, clickedPos, beforeClicked, adjacentPos, beforeAdjacent);
        if (placedPos != null) {
            rotatePlacedBlock(level, placedPos, rotateSteps);
            PlacedBlockTrackerData.get(level).mark(placedPos);
            if (selectedPlacesBlock) {
                playRemotePlacedBlockAnimation(player, placedPos);
                playRemotePlacedBlockSound(player, level, session, placedPos, quickBuild);
            } else {
                RtsStorageManager.playRemoteUseSound(player, level, null, placedPos, selectedSoundStack);
            }
            RtsStorageManager.recordRecentItem(session, itemId, S2CRtsStoragePagePayload.RECENT_ITEM_PLACED, 1L);
        } else {
            RtsStorageManager.playRemoteUseSound(player, level, null, clickedPos, selectedSoundStack);
            RtsStorageManager.recordRecentItem(session, itemId, S2CRtsStoragePagePayload.RECENT_ITEM_USED, 1L);
        }

        requestSessionPage(player, session, refreshStoragePage);
        return true;
    }

    private static ItemStack sanitizePrototype(String itemId, ItemStack itemPrototype) {
        if (itemId == null || itemId.isBlank() || itemPrototype == null || itemPrototype.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation expectedId = ResourceLocation.tryParse(itemId);
        ResourceLocation actualId = BuiltInRegistries.ITEM.getKey(itemPrototype.getItem());
        if (expectedId == null || actualId == null || !expectedId.equals(actualId)) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = itemPrototype.copy();
        copy.setCount(1);
        return copy;
    }

    private static double sanitizeHitOffset(double offset, Direction face, Direction.Axis axis) {
        if (Double.isFinite(offset)) {
            return offset;
        }
        double fallback = 0.5D;
        if (face != null && face.getAxis() == axis) {
            fallback += face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0.5D : -0.5D;
        }
        return fallback;
    }

    private static ItemStack creativeStack(Item item, ItemStack preferredStack) {
        if (preferredStack != null && !preferredStack.isEmpty()) {
            ItemStack copy = preferredStack.copy();
            copy.setCount(1);
            return copy;
        }
        return new ItemStack(item);
    }

    private static ItemStack extractSelectedFromNetwork(List<IItemHandler> handlers, ServerPlayer player, Item item,
            ItemStack preferredStack) {
        if (preferredStack != null && !preferredStack.isEmpty()) {
            return RtsStorageTransfers.extractMatchingFromNetwork(handlers, player, item, preferredStack, 1);
        }
        return RtsStorageTransfers.extractOneFromNetwork(handlers, player, item);
    }

    private static ItemStack extractSelectedFromLinked(List<IItemHandler> handlers, Item item, ItemStack preferredStack) {
        if (preferredStack != null && !preferredStack.isEmpty()) {
            return RtsStorageTransfers.extractMatchingFromLinked(handlers, item, preferredStack, 1);
        }
        return RtsStorageTransfers.extractOneFromLinked(handlers, item);
    }

    private static void requestSessionPage(ServerPlayer player, RtsStorageSession session, boolean refreshStoragePage) {
        if (refreshStoragePage) {
            RtsStorageManager.requestPage(player, session.page, session.search, session.category, session.sort, session.ascending);
        }
    }

    public static void playRemotePlacedBlockAnimation(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new S2CRtsPlaceAnimationPayload(pos.immutable()));
    }

    public static void playRemotePlacedBlockSound(ServerPlayer player, ServerLevel level, RtsStorageSession session, BlockPos pos,
            boolean quickBuild) {
        if (player == null || level == null || pos == null || !level.hasChunkAt(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        long gameTime = level.getGameTime();
        if (quickBuild && session != null) {
            noteQuickBuildPlacement(session, pos, gameTime);
            if (session.lastQuickBuildPlaceSoundTick == gameTime) {
                return;
            }
            session.lastQuickBuildPlaceSoundTick = gameTime;
        }
        SoundType soundType = state.getSoundType(level, pos, player);
        RtsStorageManager.sendDirectSound(
                player,
                soundType.getPlaceSound(),
                SoundSource.BLOCKS,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F);
    }

    private static void noteQuickBuildPlacement(RtsStorageSession session, BlockPos pos, long gameTime) {
        session.quickBuildSoundPlacedCount++;
        session.quickBuildCompletionSoundTick = gameTime + QUICK_BUILD_COMPLETION_SOUND_DELAY_TICKS;
        session.quickBuildSoundX = pos.getX() + 0.5D;
        session.quickBuildSoundY = pos.getY() + 0.5D;
        session.quickBuildSoundZ = pos.getZ() + 0.5D;
    }

    public static void tickQuickBuildCompletionSound(ServerPlayer player, RtsStorageSession session) {
        if (player == null || session == null || session.quickBuildSoundPlacedCount <= 0) {
            return;
        }
        long gameTime = player.serverLevel().getGameTime();
        if (gameTime < session.quickBuildCompletionSoundTick) {
            return;
        }
        RtsStorageManager.sendDirectSound(
                player,
                SoundEvents.NOTE_BLOCK_HARP.value(),
                SoundSource.PLAYERS,
                session.quickBuildSoundX,
                session.quickBuildSoundY,
                session.quickBuildSoundZ,
                0.35F,
                1.12F);
        session.quickBuildSoundPlacedCount = 0;
        session.quickBuildCompletionSoundTick = -1L;
        session.lastQuickBuildPlaceSoundTick = Long.MIN_VALUE;
    }

    public static BlockPos detectPlacedPos(ServerLevel level, BlockPos clickedPos, BlockState beforeClicked, BlockPos adjacentPos,
            BlockState beforeAdjacent) {
        if (!level.hasChunkAt(clickedPos)) {
            return null;
        }
        BlockState afterClicked = level.getBlockState(clickedPos);
        if (!afterClicked.equals(beforeClicked) && !afterClicked.isAir()) {
            return clickedPos;
        }

        if (beforeAdjacent == null || !level.hasChunkAt(adjacentPos)) {
            return null;
        }
        BlockState afterAdjacent = level.getBlockState(adjacentPos);
        if (!afterAdjacent.equals(beforeAdjacent) && !afterAdjacent.isAir()) {
            return adjacentPos;
        }
        return null;
    }

    public static void rotatePlacedBlock(ServerLevel level, BlockPos pos, byte rotateSteps) {
        int turns = rotateSteps & 3;
        if (turns == 0 || !level.hasChunkAt(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        BlockState rotated = rotateState(state, rotateSteps);
        if (rotated != state) {
            level.setBlock(pos, rotated, 3);
        }
    }

    private record StatePlacementPlan(
            Item item,
            ItemStack templateStack,
            BlockState state,
            boolean selectedStorageItem,
            String itemId) {
        private StatePlacementPlan {
            templateStack = templateStack == null ? ItemStack.EMPTY : templateStack.copy();
            if (!templateStack.isEmpty()) {
                templateStack.setCount(1);
            }
        }
    }

    public static final class PlaceBatchJob {
        private final List<BlockPos> clickedPositions;
        private final Direction face;
        private final double hitOffsetX;
        private final double hitOffsetY;
        private final double hitOffsetZ;
        private final byte rotateSteps;
        private final boolean forcePlace;
        private final boolean skipIfOccupied;
        private final String itemId;
        private final ItemStack itemPrototype;
        private final double rayOriginX;
        private final double rayOriginY;
        private final double rayOriginZ;
        private final double rayDirX;
        private final double rayDirY;
        private final double rayDirZ;
        private final boolean quickBuild;
        private final boolean sendRemoteHint;
        private int index;
        private boolean statePlanResolved;
        private StatePlacementPlan statePlan;

        private PlaceBatchJob(List<BlockPos> clickedPositions, Direction face, double hitOffsetX, double hitOffsetY,
                double hitOffsetZ, byte rotateSteps, boolean forcePlace, boolean skipIfOccupied, String itemId,
                ItemStack itemPrototype, double rayOriginX, double rayOriginY, double rayOriginZ, double rayDirX,
                double rayDirY, double rayDirZ, boolean quickBuild, boolean sendRemoteHint) {
            this.clickedPositions = clickedPositions;
            this.face = face;
            this.hitOffsetX = hitOffsetX;
            this.hitOffsetY = hitOffsetY;
            this.hitOffsetZ = hitOffsetZ;
            this.rotateSteps = rotateSteps;
            this.forcePlace = forcePlace;
            this.skipIfOccupied = skipIfOccupied;
            this.itemId = itemId;
            this.itemPrototype = itemPrototype == null ? ItemStack.EMPTY : itemPrototype.copy();
            this.rayOriginX = rayOriginX;
            this.rayOriginY = rayOriginY;
            this.rayOriginZ = rayOriginZ;
            this.rayDirX = rayDirX;
            this.rayDirY = rayDirY;
            this.rayDirZ = rayDirZ;
            this.quickBuild = quickBuild;
            this.sendRemoteHint = sendRemoteHint;
        }

        private boolean hasNext() {
            return this.index < this.clickedPositions.size();
        }

        private BlockPos next() {
            return this.clickedPositions.get(this.index++);
        }

        private BlockPos templatePosition() {
            return this.clickedPositions.isEmpty() ? null : this.clickedPositions.get(0);
        }

        private BlockHitResult templateHit(BlockPos templatePos) {
            return new BlockHitResult(
                    new Vec3(
                            templatePos.getX() + this.hitOffsetX,
                            templatePos.getY() + this.hitOffsetY,
                            templatePos.getZ() + this.hitOffsetZ),
                    this.face,
                    templatePos,
                    false);
        }

        private StatePlacementPlan statePlacementPlan(ServerPlayer player) {
            if (!this.statePlanResolved) {
                this.statePlan = RtsStoragePlacement.resolveStatePlacementPlan(player, this);
                this.statePlanResolved = true;
            }
            return this.statePlan;
        }

        private Direction face() {
            return this.face;
        }

        private double hitOffsetX() {
            return this.hitOffsetX;
        }

        private double hitOffsetY() {
            return this.hitOffsetY;
        }

        private double hitOffsetZ() {
            return this.hitOffsetZ;
        }

        private byte rotateSteps() {
            return this.rotateSteps;
        }

        private boolean forcePlace() {
            return this.forcePlace;
        }

        private boolean skipIfOccupied() {
            return this.skipIfOccupied;
        }

        private String itemId() {
            return this.itemId;
        }

        private ItemStack itemPrototype() {
            return this.itemPrototype.copy();
        }

        private double rayOriginX() {
            return this.rayOriginX;
        }

        private double rayOriginY() {
            return this.rayOriginY;
        }

        private double rayOriginZ() {
            return this.rayOriginZ;
        }

        private double rayDirX() {
            return this.rayDirX;
        }

        private double rayDirY() {
            return this.rayDirY;
        }

        private double rayDirZ() {
            return this.rayDirZ;
        }

        private boolean quickBuild() {
            return this.quickBuild;
        }

        private boolean sendRemoteHint() {
            return this.sendRemoteHint;
        }
    }
}
