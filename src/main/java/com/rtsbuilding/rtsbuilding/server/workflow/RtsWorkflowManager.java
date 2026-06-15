package com.rtsbuilding.rtsbuilding.server.workflow;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import com.rtsbuilding.rtsbuilding.network.builder.S2CRtsWorkflowProgressPayload;
import com.rtsbuilding.rtsbuilding.server.storage.RtsStorageSession;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Central workflow manager that tracks and reports the progress of mining,
 * placement, and other remote operations.
 *
 * <p>Every mining/placement operation should go through this manager so that
 * the UI can display current progress, missing items, and priority information
 * in a unified way.  Each player can have up to {@link RtsWorkflowState#MAX_WORKFLOWS}
 * concurrent workflow entries.</p>
 *
 * <h3>Usage</h3>
 * <ol>
 *   <li>Call {@link #startWorkflow} when an operation begins.</li>
 *   <li>Call {@link #updateProgress} periodically as blocks are processed.</li>
 *   <li>Call {@link #completeWorkflow} when the operation ends.</li>
 * </ol>
 *
 * <p>Convenience methods ({@link #startMining}, {@link #startUltimine},
 * {@link #startPlacement}) encapsulate the common workflow setup patterns.</p>
 */
public final class RtsWorkflowManager {

    private RtsWorkflowManager() {
    }

    // ======================================================================
    //  Capacity check
    // ======================================================================

    /**
     * Returns {@code true} if the session's workflow slots are all occupied
     * (active or suspended), meaning no new workflow can be started.
     */
    public static boolean isWorkflowFull(RtsStorageSession session) {
        return session != null && session.workflow.isFull();
    }

    // ======================================================================
    //  Convenience: workflow starters
    // ======================================================================

    /**
     * Starts a mining workflow for the given session.
     *
     * @return the workflow index, or -1 if at capacity
     */
    public static int startMining(ServerPlayer player, RtsStorageSession session, int totalBlocks) {
        return startWorkflow(player, session, RtsWorkflowType.MINE_SINGLE, RtsWorkflowPriority.NORMAL, totalBlocks);
    }

    /**
     * Starts an ultimine (connected-block) workflow.
     *
     * @return the workflow index, or -1 if at capacity
     */
    public static int startUltimine(ServerPlayer player, RtsStorageSession session, int totalBlocks) {
        return startWorkflow(player, session, RtsWorkflowType.ULTIMINE, RtsWorkflowPriority.HIGH, totalBlocks);
    }

    /**
     * Starts an area-mine workflow.
     *
     * @return the workflow index, or -1 if at capacity
     */
    public static int startAreaMine(ServerPlayer player, RtsStorageSession session, int totalBlocks) {
        return startWorkflow(player, session, RtsWorkflowType.AREA_MINE, RtsWorkflowPriority.HIGH, totalBlocks);
    }

    /**
     * Starts an area-destroy workflow.
     *
     * @return the workflow index, or -1 if at capacity
     */
    public static int startAreaDestroy(ServerPlayer player, RtsStorageSession session, int totalBlocks) {
        return startWorkflow(player, session, RtsWorkflowType.AREA_DESTROY, RtsWorkflowPriority.HIGH, totalBlocks);
    }

    /**
     * Starts a placement workflow for the given session.
     *
     * @return the workflow index, or -1 if at capacity
     */
    public static int startPlacement(ServerPlayer player, RtsStorageSession session, RtsWorkflowType type, int totalBlocks) {
        return startWorkflow(player, session, type, RtsWorkflowPriority.NORMAL, totalBlocks);
    }

    /**
     * Starts a quick-build (shape placement) workflow.
     *
     * @return the workflow index, or -1 if at capacity
     */
    public static int startQuickBuild(ServerPlayer player, RtsStorageSession session, int totalBlocks) {
        return startWorkflow(player, session, RtsWorkflowType.QUICK_BUILD, RtsWorkflowPriority.HIGH, totalBlocks);
    }

    // ======================================================================
    //  Core workflow lifecycle
    // ======================================================================

    /**
     * Initialises a new workflow in the session by pushing a new entry onto
     * the workflow stack.  If the stack is already at
     * {@link RtsWorkflowState#MAX_WORKFLOWS}, the request is rejected and
     * the operation is blocked (mining / placement is prevented).
     *
     * <p>Sends an initial progress update to the client for all entries.</p>
     *
     * @param player      the server-side player
     * @param session     the player's storage session
     * @param type        the type of workflow
     * @param priority    the priority level
     * @param totalBlocks total blocks to process; 0 if unknown
     * @return the 0-based index of the newly created workflow entry,
     *         or -1 if the session is at capacity and the operation was blocked
     */
    public static int startWorkflow(ServerPlayer player, RtsStorageSession session,
            RtsWorkflowType type, RtsWorkflowPriority priority, int totalBlocks) {
        if (session == null) {
            return -1;
        }
        RtsWorkflowState state = session.workflow;
        int index = state.addEntry();
        if (index < 0) {
            String name = player != null ? player.getGameProfile().getName() : "?";
            RtsbuildingMod.LOGGER.warn("[Workflow] {} 的工作流已满 ({}), 拒绝新工作流 {}",
                    name, RtsWorkflowState.MAX_WORKFLOWS, type);
            if (player != null) {
                player.displayClientMessage(
                        Component.literal("§c工作流已满 (" + RtsWorkflowState.MAX_WORKFLOWS + "/" + RtsWorkflowState.MAX_WORKFLOWS + "), 无法开始新的操作！"),
                        true);
            }
            return -1;
        }
        RtsWorkflowState.Entry entry = state.getEntry(index);
        entry.activeType = type;
        entry.priority = priority;
        entry.totalBlocks = totalBlocks;
        String name = player != null ? player.getGameProfile().getName() : "?";
        RtsbuildingMod.LOGGER.info("[Workflow] {} 开始工作流 #{}: {} (共 {} 方块)",
                name, entry.id, type, totalBlocks);
        notifyClientAll(player, state);
        return index;
    }

    // ======================================================================
    //  Suspend / Resume
    // ======================================================================

    /**
     * Suspends the most recent active workflow entry, marking it as waiting
     * for items.  Suspended entries keep occupying their workflow slot.
     */
    public static void suspendWorkflow(ServerPlayer player, RtsStorageSession session) {
        if (session == null) {
            return;
        }
        RtsWorkflowState.Entry entry = session.workflow.lastActive();
        if (entry == null) {
            return;
        }
        entry.suspended = true;
        entry.detailMessage = "等待物品...";
        String name = player != null ? player.getGameProfile().getName() : "?";
        RtsbuildingMod.LOGGER.info("[Workflow] {} 搁置工作流 #{}: {} (缺少物品)",
                name, entry.id, entry.activeType);
        notifyClientAll(player, session.workflow);
    }

    /**
     * Suspends the workflow entry at the given index, marking it as waiting
     * for items.  Suspended entries keep occupying their workflow slot.
     */
    public static void suspendWorkflow(ServerPlayer player, RtsStorageSession session, int workflowIndex) {
        if (session == null || player == null) {
            return;
        }
        RtsWorkflowState.Entry entry = session.workflow.getEntry(workflowIndex);
        if (entry == null || !entry.isOccupied()) {
            return;
        }
        entry.suspended = true;
        entry.detailMessage = "等待物品...";
        String name = player.getGameProfile().getName();
        RtsbuildingMod.LOGGER.info("[Workflow] {} 搁置工作流 #{}: {} (缺少物品, 索引 {})",
                name, entry.id, entry.activeType, workflowIndex);
        notifyClientAll(player, session.workflow);
    }

    /**
     * Resumes the most recent suspended workflow entry, moving it back to
     * active processing.
     *
     * @return {@code true} if a workflow was resumed
     */
    public static boolean resumeWorkflow(ServerPlayer player, RtsStorageSession session) {
        if (session == null) {
            return false;
        }
        RtsWorkflowState.Entry entry = session.workflow.lastSuspended();
        if (entry == null) {
            return false;
        }
        entry.suspended = false;
        entry.detailMessage = "";
        String name = player != null ? player.getGameProfile().getName() : "?";
        RtsbuildingMod.LOGGER.info("[Workflow] {} 恢复工作流 #{}: {}",
                name, entry.id, entry.activeType);
        notifyClientAll(player, session.workflow);
        return true;
    }

    /**
     * Resumes the suspended workflow entry at the given index, moving it back
     * to active processing.
     *
     * @return {@code true} if the workflow was resumed
     */
    public static boolean resumeWorkflow(ServerPlayer player, RtsStorageSession session, int workflowIndex) {
        if (session == null || player == null) {
            return false;
        }
        RtsWorkflowState.Entry entry = session.workflow.getEntry(workflowIndex);
        if (entry == null || !entry.isOccupied() || !entry.suspended) {
            return false;
        }
        entry.suspended = false;
        entry.detailMessage = "";
        String name = player.getGameProfile().getName();
        RtsbuildingMod.LOGGER.info("[Workflow] {} 恢复工作流 #{}: {} (索引 {})",
                name, entry.id, entry.activeType, workflowIndex);
        notifyClientAll(player, session.workflow);
        return true;
    }

    /**
     * Updates the progress of the most recent active workflow entry —
     * increments the completed count and optionally reports missing items.
     *
     * @param player         the server-side player
     * @param session        the player's storage session
     * @param completedDelta number of blocks completed since last update
     * @param missingItems   (nullable) item IDs that were missing; null or empty = none
     */
    public static void updateProgress(ServerPlayer player, RtsStorageSession session,
            int completedDelta, List<String> missingItems) {
        if (session == null) {
            return;
        }
        RtsWorkflowState.Entry entry = session.workflow.lastActive();
        if (entry == null) {
            return;
        }
        entry.completedBlocks += completedDelta;
        if (missingItems != null && !missingItems.isEmpty()) {
            for (String item : missingItems) {
                if (!entry.missingItems.contains(item)) {
                    entry.missingItems.add(item);
                }
            }
        }
        notifyClientAll(player, session.workflow);
    }

    /**
     * Records a failure for the most recent active workflow entry.
     */
    public static void recordFailure(ServerPlayer player, RtsStorageSession session) {
        if (session == null) {
            return;
        }
        RtsWorkflowState.Entry entry = session.workflow.lastActive();
        if (entry == null) {
            return;
        }
        entry.failedBlocks++;
        notifyClientAll(player, session.workflow);
    }

    /**
     * Sets a detail message for the most recent active workflow entry.
     */
    public static void setDetailMessage(ServerPlayer player, RtsStorageSession session, String message) {
        if (session == null) {
            return;
        }
        RtsWorkflowState.Entry entry = session.workflow.lastActive();
        if (entry == null) {
            return;
        }
        entry.detailMessage = message;
        notifyClientAll(player, session.workflow);
    }

    /**
     * Completes the most recent active workflow entry — sends a final progress
     * update for that entry and removes it from the stack.
     */
    public static void completeWorkflow(ServerPlayer player, RtsStorageSession session) {
        if (session == null) {
            return;
        }
        RtsWorkflowState state = session.workflow;
        int idx = state.lastActiveIndex();
        if (idx < 0) {
            return;
        }
        RtsWorkflowState.Entry entry = state.getEntry(idx);
        if (entry == null) {
            return;
        }
        // Send final snapshot for this entry before removing it
        if (player != null) {
            RtsWorkflowStatus status = entry.snapshot();
            byte wt = status.type() != null ? (byte) status.type().ordinal() : (byte) -1;
            int remainingCount = state.activeCount() - 1;
            PacketDistributor.sendToPlayer(player, new S2CRtsWorkflowProgressPayload(
                    (byte) idx, (byte) remainingCount, wt,
                    (byte) status.priority().rank(),
                    status.totalBlocks(),
                    status.completedBlocks(),
                    status.failedBlocks(),
                    status.missingItems(),
                    status.detailMessage(),
                    (byte) 0,
                    entry.id));
        }
        int entryId = entry.id;
        state.removeEntry(idx);
        String name = player != null ? player.getGameProfile().getName() : "?";
        RtsbuildingMod.LOGGER.info("[Workflow] {} 结束工作流 #{}: {} (完成 {} / 失败 {})",
                name, entryId, entry.activeType, entry.completedBlocks, entry.failedBlocks);
        // Notify with updated state if any entries remain (active or suspended)
        if (state.occupiedCount() > 0) {
            if (player != null) {
                notifyClientAll(player, state);
            }
        } else if (player != null) {
            PacketDistributor.sendToPlayer(player, S2CRtsWorkflowProgressPayload.idle());
        }
    }

    /**
     * Aborts all active workflows and clears the stack.
     */
    public static void abortWorkflow(ServerPlayer player, RtsStorageSession session) {
        if (session == null) {
            return;
        }
        if (player != null) {
            PacketDistributor.sendToPlayer(player, S2CRtsWorkflowProgressPayload.idle());
        }
        session.workflow.reset();
    }

    /**
     * Deletes a single workflow entry by its index.  Removes the entry
     * from the session's workflow list and notifies the client.
     *
     * @param player  the server-side player
     * @param session the player's storage session
     * @param index   the 0-based index of the workflow to delete
     */
    public static void deleteWorkflow(ServerPlayer player, RtsStorageSession session, int index) {
        if (session == null || player == null) {
            return;
        }
        RtsWorkflowState state = session.workflow;
        if (index < 0 || index >= state.entries.size()) {
            return;
        }
        RtsWorkflowState.Entry entry = state.entries.get(index);
        if (!entry.isOccupied()) {
            return;
        }
        String name = player.getGameProfile().getName();
        RtsbuildingMod.LOGGER.info("[Workflow] {} 删除工作流 #{}: {}",
                name, entry.id, entry.activeType);
        state.removeEntry(index);
        if (state.occupiedCount() > 0) {
            notifyClientAll(player, state);
        } else {
            PacketDistributor.sendToPlayer(player, S2CRtsWorkflowProgressPayload.idle());
        }
    }

    // ======================================================================
    //  Targeted (index-based) API — 按索引定位，支持多工作流精确操作
    // ======================================================================

    /**
     * Finds the index of the first workflow entry matching the given type.
     * Searches active entries first, then suspended.
     *
     * @param session the player's storage session
     * @param type    the workflow type to find
     * @return the 0-based index, or -1 if no matching entry exists
     */
    public static int findWorkflowIndex(RtsStorageSession session, RtsWorkflowType type) {
        if (session == null || type == null) {
            return -1;
        }
        RtsWorkflowState state = session.workflow;
        for (int i = 0; i < state.entries.size(); i++) {
            RtsWorkflowState.Entry e = state.entries.get(i);
            if (e.isOccupied() && e.activeType == type) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the index of the first active (non-suspended) workflow entry
     * matching the given type.
     *
     * @param session the player's storage session
     * @param type    the workflow type to find
     * @return the 0-based index, or -1 if no matching active entry exists
     */
    public static int findActiveWorkflowIndex(RtsStorageSession session, RtsWorkflowType type) {
        if (session == null || type == null) {
            return -1;
        }
        RtsWorkflowState state = session.workflow;
        for (int i = 0; i < state.entries.size(); i++) {
            RtsWorkflowState.Entry e = state.entries.get(i);
            if (e.hasActiveWorkflow() && e.activeType == type) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Updates the progress of the workflow entry at the given index.
     *
     * @param player         the server-side player
     * @param session        the player's storage session
     * @param workflowIndex  the 0-based index of the workflow to update
     * @param completedDelta number of blocks completed since last update
     * @param missingItems   (nullable) item IDs that were missing
     */
    public static void updateProgress(ServerPlayer player, RtsStorageSession session,
            int workflowIndex, int completedDelta, List<String> missingItems) {
        if (session == null) {
            return;
        }
        RtsWorkflowState.Entry entry = session.workflow.getEntry(workflowIndex);
        if (entry == null || !entry.isOccupied()) {
            return;
        }
        entry.completedBlocks += completedDelta;
        if (missingItems != null && !missingItems.isEmpty()) {
            for (String item : missingItems) {
                if (!entry.missingItems.contains(item)) {
                    entry.missingItems.add(item);
                }
            }
        }
        notifyClientAll(player, session.workflow);
    }

    /**
     * Records a failure for the workflow entry at the given index.
     */
    public static void recordFailure(ServerPlayer player, RtsStorageSession session, int workflowIndex) {
        if (session == null) {
            return;
        }
        RtsWorkflowState.Entry entry = session.workflow.getEntry(workflowIndex);
        if (entry == null || !entry.isOccupied()) {
            return;
        }
        entry.failedBlocks++;
        notifyClientAll(player, session.workflow);
    }

    /**
     * Sets a detail message for the workflow entry at the given index.
     */
    public static void setDetailMessage(ServerPlayer player, RtsStorageSession session,
            int workflowIndex, String message) {
        if (session == null) {
            return;
        }
        RtsWorkflowState.Entry entry = session.workflow.getEntry(workflowIndex);
        if (entry == null || !entry.isOccupied()) {
            return;
        }
        entry.detailMessage = message;
        notifyClientAll(player, session.workflow);
    }

    /**
     * Completes the workflow entry at the given index — sends a final
     * progress update and removes it.
     */
    public static void completeWorkflow(ServerPlayer player, RtsStorageSession session, int workflowIndex) {
        if (session == null) {
            return;
        }
        RtsWorkflowState state = session.workflow;
        RtsWorkflowState.Entry entry = state.getEntry(workflowIndex);
        if (entry == null || !entry.isOccupied()) {
            return;
        }
        // Send final snapshot for this entry before removing it
        if (player != null) {
            RtsWorkflowStatus status = entry.snapshot();
            byte wt = status.type() != null ? (byte) status.type().ordinal() : (byte) -1;
            int remainingCount = state.activeCount() - 1;
            PacketDistributor.sendToPlayer(player, new S2CRtsWorkflowProgressPayload(
                    (byte) workflowIndex, (byte) remainingCount, wt,
                    (byte) status.priority().rank(),
                    status.totalBlocks(),
                    status.completedBlocks(),
                    status.failedBlocks(),
                    status.missingItems(),
                    status.detailMessage(),
                    (byte) 0,
                    entry.id));
        }
        int entryId = entry.id;
        state.removeEntry(workflowIndex);
        String name = player != null ? player.getGameProfile().getName() : "?";
        RtsbuildingMod.LOGGER.info("[Workflow] {} 结束工作流 #{}: {} (完成 {} / 失败 {})",
                name, entryId, entry.activeType, entry.completedBlocks, entry.failedBlocks);
        if (state.occupiedCount() > 0) {
            if (player != null) {
                notifyClientAll(player, state);
            }
        } else if (player != null) {
            PacketDistributor.sendToPlayer(player, S2CRtsWorkflowProgressPayload.idle());
        }
    }

    /**
     * Convenience: marks a single unit of progress for the workflow at the
     * given index.  Equivalent to calling
     * {@link #updateProgress(ServerPlayer, RtsStorageSession, int, int, List)}
     * with a delta of 1 and no missing items.
     * <p>
     * Features that simply want to say "I did one unit of work" can call this
     * without worrying about the full parameter list.
     *
     * @param player        the server-side player
     * @param session       the player's storage session
     * @param workflowIndex the 0-based index of the workflow to mark
     */
    public static void markProgress(ServerPlayer player, RtsStorageSession session, int workflowIndex) {
        updateProgress(player, session, workflowIndex, 1, null);
    }

    /**
     * Refreshes the progress of a placement workflow by scanning the actual
     * world state of the first pending (or active) batch job.  Updates both
     * active and suspended workflows whose type is PLACE_BATCH.
     *
     * <p>This is intended to be called after blocks are broken externally
     * (e.g. player mines a placed block), so the progress bar and the
     * needed-items count reflect the current world state.</p>
     *
     * @param player  the server-side player
     * @param session the player's storage session
     * @param scannedCompletedBlocks the number of blocks actually still placed
     */
    public static void setPlacementCompletedBlocks(ServerPlayer player, RtsStorageSession session,
            int scannedCompletedBlocks) {
        if (session == null || player == null) {
            return;
        }
        // Find a PLACE_BATCH workflow entry (active first, then suspended)
        RtsWorkflowState state = session.workflow;
        RtsWorkflowState.Entry target = null;
        for (int i = state.entries.size() - 1; i >= 0; i--) {
            RtsWorkflowState.Entry e = state.entries.get(i);
            if (e.isOccupied() && e.activeType == RtsWorkflowType.PLACE_BATCH) {
                target = e;
                break;
            }
        }
        if (target == null) {
            return;
        }
        int oldCompleted = target.completedBlocks;
        if (oldCompleted == scannedCompletedBlocks) {
            return; // 没有变化，不通知
        }
        target.completedBlocks = scannedCompletedBlocks;
        String name = player.getGameProfile().getName();
        RtsbuildingMod.LOGGER.debug("[Workflow] {} 放置进度已同步: {} → {} (完成块数)",
                name, oldCompleted, scannedCompletedBlocks);
        notifyClientAll(player, state);
    }

    /**
     * Sets the completed-blocks count for the workflow entry identified by
     * its immutable entry ID, used when scanning the world to get the real
     * placed-count.  The entry is looked up by {@link RtsWorkflowState.Entry#id}
     * so it survives index shifts from earlier entry removals.
     */
    public static void setPlacementCompletedBlocks(ServerPlayer player, RtsStorageSession session,
            int workflowEntryId, int scannedCompletedBlocks) {
        if (session == null || player == null) {
            return;
        }
        int idx = findWorkflowIndexByEntryId(session, workflowEntryId);
        if (idx < 0) {
            return;
        }
        RtsWorkflowState state = session.workflow;
        RtsWorkflowState.Entry entry = state.getEntry(idx);
        if (entry == null || !entry.isOccupied()) {
            return;
        }
        int oldCompleted = entry.completedBlocks;
        if (oldCompleted == scannedCompletedBlocks) {
            return;
        }
        entry.completedBlocks = scannedCompletedBlocks;
        String name = player.getGameProfile().getName();
        RtsbuildingMod.LOGGER.debug("[Workflow] {} 放置进度已同步 (条目 {}): {} → {}",
                name, workflowEntryId, oldCompleted, scannedCompletedBlocks);
        notifyClientAll(player, state);
    }

    // ======================================================================
    //  Queries
    // ======================================================================

    /**
     * Returns an immutable list of snapshots for all active workflow entries.
     */
    public static List<RtsWorkflowStatus> getAllStatuses(RtsStorageSession session) {
        if (session == null) {
            return List.of();
        }
        return session.workflow.snapshots();
    }

    /**
     * Returns the {@link RtsWorkflowStatus} of the first workflow matching
     * the given type, or {@code null} if no matching entry is found.
     */
    public static RtsWorkflowStatus findWorkflowStatus(RtsStorageSession session, RtsWorkflowType type) {
        if (session == null || type == null) {
            return null;
        }
        RtsWorkflowState state = session.workflow;
        for (int i = 0; i < state.entries.size(); i++) {
            RtsWorkflowState.Entry e = state.entries.get(i);
            if (e.isOccupied() && e.activeType == type) {
                return e.snapshot();
            }
        }
        return null;
    }

    /**
     * Convenience: returns the progress snapshot for the current
     * {@link RtsWorkflowType#AREA_DESTROY AREA_DESTROY} workflow.
     *
     * @return the status, or {@code null} if no AREA_DESTROY workflow is active
     */
    public static RtsWorkflowStatus getAreaDestroyProgress(RtsStorageSession session) {
        return findWorkflowStatus(session, RtsWorkflowType.AREA_DESTROY);
    }

    /**
     * Convenience: returns the progress snapshot for the current
     * {@link RtsWorkflowType#PLACE_BATCH PLACE_BATCH} workflow.
     *
     * @return the status, or {@code null} if no PLACE_BATCH workflow is active
     */
    public static RtsWorkflowStatus getPlaceBatchProgress(RtsStorageSession session) {
        return findWorkflowStatus(session, RtsWorkflowType.PLACE_BATCH);
    }

    /**
     * Convenience: returns the progress snapshot for the current
     * {@link RtsWorkflowType#QUICK_BUILD QUICK_BUILD} workflow.
     *
     * @return the status, or {@code null} if no QUICK_BUILD workflow is active
     */
    public static RtsWorkflowStatus getQuickBuildProgress(RtsStorageSession session) {
        return findWorkflowStatus(session, RtsWorkflowType.QUICK_BUILD);
    }

    /**
     * Returns {@code true} if the session currently has any active workflows.
     */
    public static boolean hasActiveWorkflow(RtsStorageSession session) {
        return session != null && session.workflow.hasActiveWorkflow();
    }

    /**
     * Returns the number of active workflow entries.
     */
    public static int activeWorkflowCount(RtsStorageSession session) {
        if (session == null) {
            return 0;
        }
        return session.workflow.activeCount();
    }

    /**
     * Returns the structured progress data for the first workflow matching
     * the given type.  This is the primary API method for external consumers
     * that need total / completed / remaining counts without manually
     * computing derived values.
     *
     * <p>Internally delegates to {@link RtsWorkflowProgressProcessor#processByType}.
     *
     * @return the structured progress data, or idle data if not found
     */
    public static RtsWorkflowProgressData getProgressData(RtsStorageSession session, RtsWorkflowType type) {
        return RtsWorkflowProgressProcessor.processByType(session, type);
    }

    /**
     * Returns structured progress data for <b>all</b> occupied workflow
     * entries in the session.
     *
     * <p>Internally delegates to {@link RtsWorkflowProgressProcessor#processAll}.
     */
    public static List<RtsWorkflowProgressData> getAllProgressData(RtsStorageSession session) {
        return RtsWorkflowProgressProcessor.processAll(session);
    }

    /**
     * Returns structured progress data for the workflow entry at the
     * given index.
     */
    public static RtsWorkflowProgressData getProgressDataByIndex(RtsStorageSession session, int index) {
        return RtsWorkflowProgressProcessor.processByIndex(session, index);
    }

    /**
     * Finds the current index of a workflow entry by its unique entry ID.
     * The entry ID ({@link RtsWorkflowState.Entry#id}) is immutable and
     * survives entry removals, unlike the positional index which shifts.
     *
     * @param session the player's storage session
     * @param entryId the immutable entry ID to search for
     * @return the 0-based index, or -1 if not found
     */
    public static int findWorkflowIndexByEntryId(RtsStorageSession session, int entryId) {
        if (session == null) {
            return -1;
        }
        java.util.List<RtsWorkflowState.Entry> entries = session.workflow.entries;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id == entryId) {
                return i;
            }
        }
        return -1;
    }

    // ======================================================================
    //  Internal helpers
    // ======================================================================

    /**
     * Sends all active workflow entries to the client as individual payloads.
     */
    private static void notifyClientAll(ServerPlayer player, RtsWorkflowState state) {
        if (player == null || state == null) {
            return;
        }
        // Use occupiedCount() so suspended entries are included in the total
        int totalCount = state.occupiedCount();
        byte totalCountByte = (byte) Math.min(totalCount, 255);
        // Send one payload per occupied entry (active + suspended)
        for (int i = 0; i < state.entries.size(); i++) {
            RtsWorkflowState.Entry entry = state.entries.get(i);
            if (!entry.isOccupied()) {
                continue;
            }
            RtsWorkflowStatus status = entry.snapshot();
            PacketDistributor.sendToPlayer(player, new S2CRtsWorkflowProgressPayload(
                    (byte) i,
                    totalCountByte,
                    status.type() != null ? (byte) status.type().ordinal() : (byte) -1,
                    (byte) status.priority().rank(),
                    status.totalBlocks(),
                    status.completedBlocks(),
                    status.failedBlocks(),
                    status.missingItems(),
                    status.detailMessage(),
                    status.suspended() ? (byte) 1 : (byte) 0,
                    entry.id));
        }
    }
}
