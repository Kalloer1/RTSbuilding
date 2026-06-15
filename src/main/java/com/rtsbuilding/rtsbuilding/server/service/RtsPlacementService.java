package com.rtsbuilding.rtsbuilding.server.service;

import com.rtsbuilding.rtsbuilding.progression.RtsFeature;
import com.rtsbuilding.rtsbuilding.server.progression.RtsProgressionManager;
import com.rtsbuilding.rtsbuilding.server.service.placement.RtsPlacementBatch;
import com.rtsbuilding.rtsbuilding.server.service.placement.RtsPlacementHelper;
import com.rtsbuilding.rtsbuilding.server.storage.RtsLinkedStorageResolver;
import com.rtsbuilding.rtsbuilding.server.storage.RtsStorageSession;
import com.rtsbuilding.rtsbuilding.server.workflow.RtsWorkflowHandle;
import com.rtsbuilding.rtsbuilding.server.workflow.RtsWorkflowManager;
import com.rtsbuilding.rtsbuilding.server.workflow.RtsWorkflowStatus;
import com.rtsbuilding.rtsbuilding.server.workflow.RtsWorkflowType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 放置服务——管理方块放置、批量放置和方块旋转。
 *
 * <p>职责范围：
 * <ul>
 *   <li>选中方块放置</li>
 *   <li>批量方块放置入队</li>
 *   <li>方块旋转</li>
 * </ul>
 */
public final class RtsPlacementService {

    private RtsPlacementService() {
    }

    /**
     * 放置选中方块。
     */
    public static void placeSelected(ServerPlayer player, BlockPos clickedPos, Direction face, double hitX, double hitY,
            double hitZ, byte rotateSteps, boolean forcePlace, boolean skipIfOccupied, String itemId,
            ItemStack itemPrototype, double rayOriginX, double rayOriginY, double rayOriginZ,
            double rayDirX, double rayDirY, double rayDirZ, boolean quickBuild, boolean forceEmptyHand) {
        double hitOffsetX = clickedPos == null ? 0.5D : hitX - clickedPos.getX();
        double hitOffsetY = clickedPos == null ? 0.5D : hitY - clickedPos.getY();
        double hitOffsetZ = clickedPos == null ? 0.5D : hitZ - clickedPos.getZ();
        RtsStorageSession session = player == null ? null : RtsSessionService.getIfPresent(player);

        // 用令牌启动工作流（容量检查内部自处理）
        int wfEntryId = -1;
        if (player != null && session != null && !forceEmptyHand) {
            RtsWorkflowHandle handle = RtsWorkflowHandle.startPlacement(player, session,
                    quickBuild ? RtsWorkflowType.QUICK_BUILD : RtsWorkflowType.PLACE_SINGLE,
                    1);
            if (handle != null) {
                wfEntryId = handle.getEntryId();
            }
        }
        final int finalWfEntryId = wfEntryId;
        RtsPlacementBatch.enqueuePlaceBatch(
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
                forceEmptyHand,
                true,
                finalWfEntryId);
    }

    /**
     * 批量方块放置入队。
     */
    public static void enqueuePlaceBatch(ServerPlayer player, List<BlockPos> clickedPositions, Direction face,
            double hitOffsetX, double hitOffsetY, double hitOffsetZ, byte rotateSteps,
            boolean forcePlace, boolean skipIfOccupied, String itemId,
            ItemStack itemPrototype, double rayOriginX, double rayOriginY, double rayOriginZ,
            double rayDirX, double rayDirY, double rayDirZ) {
        RtsStorageSession session = player == null ? null : RtsSessionService.getIfPresent(player);

        // 用令牌启动工作流（容量检查内部自处理）
        int wfEntryId = -1;
        if (player != null && session != null && clickedPositions != null && !clickedPositions.isEmpty()) {
            RtsWorkflowHandle handle = RtsWorkflowHandle.startPlacement(player, session,
                    RtsWorkflowType.PLACE_BATCH,
                    clickedPositions.size());
            if (handle != null) {
                wfEntryId = handle.getEntryId();
            }
        }
        final int finalWfEntryId = wfEntryId;
        RtsPlacementBatch.enqueuePlaceBatch(
                player,
                session,
                clickedPositions,
                face,
                hitOffsetX,
                hitOffsetY,
                hitOffsetZ,
                rotateSteps,
                forcePlace,
                skipIfOccupied,
                itemId == null ? "" : itemId,
                itemPrototype,
                rayOriginX,
                rayOriginY,
                rayOriginZ,
                rayDirX,
                rayDirY,
                rayDirZ,
                true,
                false,
                false,
                finalWfEntryId);
        // 新放置入队后尝试恢复挂起作业
        if (player != null) {
            RtsPendingPlacementService.tryResumeAfterStorageChange(player);
        }
    }

    /**
     * 提交挂起放置作业——尝试恢复所有因物品不足而暂停的放置任务。
     * 若库存中已有足够物品，挂起的作业将被移回活跃队列继续执行。
     *
     * @param player 玩家
     * @return 成功恢复的作业数
     */
    public static int submitPendingPlacement(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        RtsStorageSession session = RtsSessionService.getIfPresent(player);
        if (session == null || session.placement.pendingJobs.isEmpty()) {
            return 0;
        }
        int count = RtsPendingPlacementService.resumeAllPendingJobs(player, session);
        if (count > 0) {
            player.displayClientMessage(
                    Component.literal("Resumed " + count + " pending placement job(s)."), true);
        } else {
            player.displayClientMessage(
                    Component.literal("No pending placements can be resumed — insufficient items."), true);
        }
        return count;
    }

    /**
     * 旋转已放置的方块。
     */
    public static void rotateBlock(ServerPlayer player, BlockPos pos) {
        if (!RtsProgressionManager.canUse(player, RtsFeature.ROTATE_BLOCK)) {
            return;
        }
        RtsStorageSession session = RtsSessionService.getIfPresent(player);
        if (session == null || !RtsLinkedStorageResolver.canAccessWorldTarget(player, pos)) {
            return;
        }
        RtsPlacementHelper.rotatePlacedBlock(player.serverLevel(), pos, (byte) 1);
    }

    // =========================================================================
    //  Placement Progress Queries
    // =========================================================================

    /**
     * 获取当前批量范围放置的总方块数（放置方块总数）。
     *
     * @return 总方块数，如果没有进行中的批量放置则返回 0
     */
    public static int getPlaceBatchTotalBlocks(ServerPlayer player) {
        RtsStorageSession session = RtsSessionService.getIfPresent(player);
        if (session == null) return 0;
        RtsWorkflowStatus status = RtsWorkflowManager.getPlaceBatchProgress(session);
        if (status != null) return status.totalBlocks();
        // Also check QUICK_BUILD (shape placement)
        status = RtsWorkflowManager.getQuickBuildProgress(session);
        return status != null ? status.totalBlocks() : 0;
    }

    /**
     * 获取当前批量范围放置的已放置方块数量。
     *
     * @return 已放置方块数，如果没有进行中的批量放置则返回 0
     */
    public static int getPlaceBatchCompletedBlocks(ServerPlayer player) {
        RtsStorageSession session = RtsSessionService.getIfPresent(player);
        if (session == null) return 0;
        RtsWorkflowStatus status = RtsWorkflowManager.getPlaceBatchProgress(session);
        if (status != null) return status.completedBlocks();
        status = RtsWorkflowManager.getQuickBuildProgress(session);
        return status != null ? status.completedBlocks() : 0;
    }

    /**
     * 获取当前批量范围放置的未放置方块数（剩余待放置方块）。
     *
     * @return 未放置方块数，如果没有进行中的批量放置则返回 0
     */
    public static int getPlaceBatchRemainingBlocks(ServerPlayer player) {
        RtsStorageSession session = RtsSessionService.getIfPresent(player);
        if (session == null) return 0;
        RtsWorkflowStatus status = RtsWorkflowManager.getPlaceBatchProgress(session);
        if (status != null) return status.remainingBlocks();
        status = RtsWorkflowManager.getQuickBuildProgress(session);
        return status != null ? status.remainingBlocks() : 0;
    }

    /**
     * 获取当前批量范围放置的方块类型（物品 ID）。
     * 返回首个活跃或挂起的放置作业所使用的物品 ID，
     * 便于外部系统（如合成）知道当前在放置什么方块。
     *
     * @return 物品 ID 字符串（如 "minecraft:diamond_block"），
     *         如果没有进行中的批量放置则返回空字符串
     */
    public static String getPlaceBatchItemId(ServerPlayer player) {
        if (player == null) return "";
        RtsStorageSession session = RtsSessionService.getIfPresent(player);
        if (session == null) return "";
        // 先查活跃队列
        if (!session.placement.placeBatchJobs.isEmpty()) {
            return session.placement.placeBatchJobs.peekFirst().itemId();
        }
        // 再查挂起队列
        if (!session.placement.pendingJobs.isEmpty()) {
            return session.placement.pendingJobs.peekFirst().itemId();
        }
        return "";
    }
}
