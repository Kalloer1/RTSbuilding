package com.rtsbuilding.rtsbuilding.server.workflow;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of the current workflow progress.
 *
 * <p>This record is sent to the client and used by the UI to display progress
 * bars, missing-item warnings, and priority indicators.</p>
 *
 * @param type           the type of workflow currently active
 * @param priority       the priority level of the active workflow
 * @param totalBlocks    total number of blocks to process (0 if unknown)
 * @param completedBlocks number of blocks successfully processed so far
 * @param failedBlocks   number of blocks that failed to process
 * @param missingItems   item IDs that are needed but currently unavailable in storage; empty list if none
 * @param detailMessage  optional human-readable detail about the current workflow (e.g. "Waiting for tools")
 * @param suspended      {@code true} if this workflow is suspended (waiting for items to become available)
 * @param entryId        immutable workflow entry ID for linking with pending jobs
 */
public record RtsWorkflowStatus(
        RtsWorkflowType type,
        RtsWorkflowPriority priority,
        int totalBlocks,
        int completedBlocks,
        int failedBlocks,
        List<String> missingItems,
        String detailMessage,
        boolean suspended,
        int entryId) {

    /**
     * Returns the overall progress as a float in [0.0, 1.0].
     * Returns 0.0 when total is 0 or unknown.
     */
    public float progress() {
        if (totalBlocks <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) (completedBlocks + failedBlocks) / (float) totalBlocks);
    }

    /**
     * Returns {@code true} if this workflow has missing items that need
     * attention.
     */
    public boolean hasMissingItems() {
        return !this.missingItems.isEmpty();
    }

    /**
     * Returns {@code true} if this workflow has any failures.
     */
    public boolean hasFailures() {
        return this.failedBlocks > 0;
    }

    /**
     * Returns the number of remaining (not yet processed) blocks.
     * Returns 0 when total is 0 or unknown, or when all blocks have been processed.
     */
    public int remainingBlocks() {
        if (totalBlocks <= 0) {
            return 0;
        }
        return Math.max(0, totalBlocks - (completedBlocks + failedBlocks));
    }

    /**
     * Returns {@code true} if the workflow is complete (all blocks processed).
     */
    public boolean isComplete() {
        return totalBlocks > 0 && (completedBlocks + failedBlocks) >= totalBlocks;
    }

    /**
     * Creates an empty (no active workflow) status.
     */
    public static RtsWorkflowStatus idle() {
        return new RtsWorkflowStatus(null, RtsWorkflowPriority.NORMAL, 0, 0, 0, List.of(), "", false, -1);
    }
}
