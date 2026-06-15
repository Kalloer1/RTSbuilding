package com.rtsbuilding.rtsbuilding.server.workflow;

import com.rtsbuilding.rtsbuilding.server.storage.RtsStorageSession;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Lightweight handle that encapsulates a single workflow lifecycle.
 *
 * <p>This is the recommended entry point for <b>any new feature</b> that wants
 * to report progress through the workflow system.  Instead of manually
 * juggling sessions, indices, and entry IDs, callers obtain a handle via one
 * of the factory methods and then simply call {@link #markProgress()},
 * {@link #complete()}, etc.</p>
 *
 * <p>The handle internally resolves the current index from the immutable
 * entry ID, so it survives index shifts caused by earlier entries being
 * removed.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Start a workflow and get a handle
 * RtsWorkflowHandle handle = RtsWorkflowHandle.start(player, session,
 *         RtsWorkflowType.MINE_SINGLE, RtsWorkflowPriority.NORMAL, 100);
 * if (handle == null) {
 *     // Session was null or workflow queue was full
 *     return;
 * }
 *
 * // During processing
 * for (BlockPos pos : targets) {
 *     if (processBlock(pos)) {
 *         handle.markProgress();   // "I did one unit of work"
 *     } else {
 *         handle.recordFailure();
 *     }
 * }
 *
 * // When done
 * handle.complete();
 * }</pre>
 *
 * <p>The handle is intended for server-side use only.  It holds a reference
 * to the {@link ServerPlayer} and {@link RtsStorageSession} for its entire
 * lifetime.  Create a new handle for each distinct workflow.</p>
 */
public final class RtsWorkflowHandle {

    private final ServerPlayer player;
    private final RtsStorageSession session;
    private final int entryId;

    // ======================================================================
    //  Construction
    // ======================================================================

    private RtsWorkflowHandle(ServerPlayer player, RtsStorageSession session, int entryId) {
        this.player = player;
        this.session = session;
        this.entryId = entryId;
    }

    // ======================================================================
    //  Factory methods — "拿令牌"
    // ======================================================================

    /**
     * Starts a new workflow and returns a handle for it.
     *
     * <p>Internally delegates to {@link RtsWorkflowManager#startWorkflow},
     * which already handles capacity checks and user-facing error messages.
     * If the queue is full or the player/session is {@code null}, this
     * method returns {@code null}.</p>
     *
     * @param player      the server-side player
     * @param session     the player's storage session
     * @param type        the type of workflow
     * @param priority    the priority level
     * @param totalBlocks total blocks to process; 0 if unknown
     * @return the handle, or {@code null} if starting failed
     */
    public static RtsWorkflowHandle start(ServerPlayer player, RtsStorageSession session,
            RtsWorkflowType type, RtsWorkflowPriority priority, int totalBlocks) {
        if (player == null || session == null) {
            return null;
        }
        int index = RtsWorkflowManager.startWorkflow(player, session, type, priority, totalBlocks);
        if (index < 0) {
            return null;
        }
        RtsWorkflowState.Entry entry = session.workflow.getEntry(index);
        return new RtsWorkflowHandle(player, session, entry.id);
    }

    /**
     * Convenience: starts a mining workflow and returns a handle.
     *
     * @see RtsWorkflowManager#startMining(ServerPlayer, RtsStorageSession, int)
     */
    public static RtsWorkflowHandle startMining(ServerPlayer player, RtsStorageSession session, int totalBlocks) {
        return start(player, session, RtsWorkflowType.MINE_SINGLE, RtsWorkflowPriority.NORMAL, totalBlocks);
    }

    /**
     * Convenience: starts an ultimine workflow and returns a handle.
     *
     * @see RtsWorkflowManager#startUltimine(ServerPlayer, RtsStorageSession, int)
     */
    public static RtsWorkflowHandle startUltimine(ServerPlayer player, RtsStorageSession session, int totalBlocks) {
        return start(player, session, RtsWorkflowType.ULTIMINE, RtsWorkflowPriority.HIGH, totalBlocks);
    }

    /**
     * Convenience: starts an area-mine workflow and returns a handle.
     *
     * @see RtsWorkflowManager#startAreaMine(ServerPlayer, RtsStorageSession, int)
     */
    public static RtsWorkflowHandle startAreaMine(ServerPlayer player, RtsStorageSession session, int totalBlocks) {
        return start(player, session, RtsWorkflowType.AREA_MINE, RtsWorkflowPriority.HIGH, totalBlocks);
    }

    /**
     * Convenience: starts an area-destroy workflow and returns a handle.
     *
     * @see RtsWorkflowManager#startAreaDestroy(ServerPlayer, RtsStorageSession, int)
     */
    public static RtsWorkflowHandle startAreaDestroy(ServerPlayer player, RtsStorageSession session, int totalBlocks) {
        return start(player, session, RtsWorkflowType.AREA_DESTROY, RtsWorkflowPriority.HIGH, totalBlocks);
    }

    /**
     * Convenience: starts a placement workflow and returns a handle.
     *
     * @see RtsWorkflowManager#startPlacement(ServerPlayer, RtsStorageSession, RtsWorkflowType, int)
     */
    public static RtsWorkflowHandle startPlacement(ServerPlayer player, RtsStorageSession session,
            RtsWorkflowType type, int totalBlocks) {
        return start(player, session, type, RtsWorkflowPriority.NORMAL, totalBlocks);
    }

    /**
     * Convenience: starts a quick-build workflow and returns a handle.
     *
     * @see RtsWorkflowManager#startQuickBuild(ServerPlayer, RtsStorageSession, int)
     */
    public static RtsWorkflowHandle startQuickBuild(ServerPlayer player, RtsStorageSession session, int totalBlocks) {
        return start(player, session, RtsWorkflowType.QUICK_BUILD, RtsWorkflowPriority.HIGH, totalBlocks);
    }

    // ======================================================================
    //  Reconstruct from existing entry — 从已有 entryId 重建令牌
    // ======================================================================

    /**
     * Reconstructs a handle for an already-started workflow identified by its
     * immutable entry ID.  Useful when a workflow entry ID has been passed
     * through a job record ({@code PlaceBatchJob}) and needs to be updated
     * later from a different code location.
     *
     * @param player  the server-side player
     * @param session the player's storage session
     * @param entryId the immutable entry ID obtained from a previous handle
     * @return the handle, or {@code null} if no such entry exists
     */
    public static RtsWorkflowHandle from(ServerPlayer player, RtsStorageSession session, int entryId) {
        if (player == null || session == null) {
            return null;
        }
        if (RtsWorkflowManager.findWorkflowIndexByEntryId(session, entryId) < 0) {
            return null;
        }
        return new RtsWorkflowHandle(player, session, entryId);
    }

    /**
     * Creates a handle for the most recent active (non-suspended) workflow
     * entry.  Equivalent to the legacy {@code lastActive()} pattern used by
     * mining operations where there is only one active workflow at a time.
     *
     * @param player  the server-side player
     * @param session the player's storage session
     * @return the handle, or {@code null} if no active workflow exists
     */
    public static RtsWorkflowHandle lastActive(ServerPlayer player, RtsStorageSession session) {
        if (player == null || session == null) {
            return null;
        }
        RtsWorkflowState.Entry entry = session.workflow.lastActive();
        if (entry == null) {
            return null;
        }
        return new RtsWorkflowHandle(player, session, entry.id);
    }

    // ======================================================================
    //  Internal helpers
    // ======================================================================

    /**
     * Resolves the current index of this handle's workflow entry.
     * Uses the immutable entry ID so it survives index shifts.
     *
     * @return the 0-based index, or -1 if the entry no longer exists
     */
    private int resolveIndex() {
        return RtsWorkflowManager.findWorkflowIndexByEntryId(session, entryId);
    }

    // ======================================================================
    //  Lifecycle — "凭令牌办事"
    // ======================================================================

    /**
     * Marks one unit of progress for this workflow.
     * Equivalent to calling {@link #updateProgress(int, List)} with delta=1
     * and no missing items.
     */
    public void markProgress() {
        int idx = resolveIndex();
        if (idx >= 0) {
            RtsWorkflowManager.markProgress(player, session, idx);
        }
    }

    /**
     * Updates the progress of this workflow.
     *
     * @param completedDelta number of units completed since last update
     * @param missingItems   (nullable) item IDs that were missing; null or empty = none
     */
    public void updateProgress(int completedDelta, List<String> missingItems) {
        int idx = resolveIndex();
        if (idx >= 0) {
            RtsWorkflowManager.updateProgress(player, session, idx, completedDelta, missingItems);
        }
    }

    /**
     * Records a single failure for this workflow.
     */
    public void recordFailure() {
        int idx = resolveIndex();
        if (idx >= 0) {
            RtsWorkflowManager.recordFailure(player, session, idx);
        }
    }

    /**
     * Sets a detail message for this workflow.
     */
    public void setDetailMessage(String message) {
        int idx = resolveIndex();
        if (idx >= 0) {
            RtsWorkflowManager.setDetailMessage(player, session, idx, message);
        }
    }

    /**
     * Suspends this workflow (marks it as waiting for items).
     */
    public void suspend() {
        int idx = resolveIndex();
        if (idx >= 0) {
            RtsWorkflowManager.suspendWorkflow(player, session, idx);
        }
    }

    /**
     * Resumes this workflow if it was suspended.
     *
     * @return {@code true} if the workflow was successfully resumed
     */
    public boolean resume() {
        int idx = resolveIndex();
        if (idx < 0) {
            return false;
        }
        return RtsWorkflowManager.resumeWorkflow(player, session, idx);
    }

    /**
     * Completes this workflow — sends a final progress update and removes
     * the entry from the workflow stack.
     */
    public void complete() {
        int idx = resolveIndex();
        if (idx >= 0) {
            RtsWorkflowManager.completeWorkflow(player, session, idx);
        }
    }

    // ======================================================================
    //  Queries
    // ======================================================================

    /**
     * Returns the immutable entry ID for this workflow handle.
     * This ID survives index shifts and can be passed to job records
     * for later resolution.
     */
    public int getEntryId() {
        return entryId;
    }

    /**
     * Returns structured progress data for this workflow.
     *
     * @return the progress data, or {@link RtsWorkflowProgressData#idle()} if
     *         the entry no longer exists
     */
    public RtsWorkflowProgressData getProgressData() {
        int idx = resolveIndex();
        if (idx < 0) {
            return RtsWorkflowProgressData.idle();
        }
        return RtsWorkflowManager.getProgressDataByIndex(session, idx);
    }

    /**
     * Returns {@code true} if this handle still refers to a valid workflow
     * entry (i.e. it has not been completed or removed).
     */
    public boolean isValid() {
        return resolveIndex() >= 0;
    }

    /**
     * Returns the player associated with this handle.
     */
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the session associated with this handle.
     */
    public RtsStorageSession getSession() {
        return session;
    }
}
