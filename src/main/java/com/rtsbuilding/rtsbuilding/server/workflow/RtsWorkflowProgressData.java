package com.rtsbuilding.rtsbuilding.server.workflow;

import java.util.List;

/**
 * Structured progress data carrier — the unified output of
 * {@link RtsWorkflowProgressProcessor}.
 *
 * <p>This record packs all the information needed by the panel to render
 * a progress bar, including pre-computed derived values
 * ({@link #remainingBlocks()}, {@link #progress()}) so the UI never has
 * to re-derive them.</p>
 *
 * <p>It is intentionally lightweight and independent of any Minecraft
 * classes so it can be shared between server-side API methods and
 * client-side UI code.</p>
 *
 * @param type            the type of workflow
 * @param totalBlocks     total number of blocks to process (0 if unknown)
 * @param completedBlocks number of blocks successfully processed
 * @param remainingBlocks number of blocks still pending
 * @param failedBlocks    number of blocks that failed to process
 * @param progress        progress as a float in [0.0, 1.0] (0.0 when unknown)
 * @param suspended       {@code true} if the workflow is waiting for items
 * @param isComplete      {@code true} if all blocks have been processed
 * @param missingItems    item IDs that are needed but unavailable
 * @param detailMessage   optional human-readable detail
 */
public record RtsWorkflowProgressData(
        RtsWorkflowType type,
        int totalBlocks,
        int completedBlocks,
        int remainingBlocks,
        int failedBlocks,
        float progress,
        boolean suspended,
        boolean isComplete,
        List<String> missingItems,
        String detailMessage) {

    /**
     * Returns an idle (no active workflow) progress data.
     */
    public static RtsWorkflowProgressData idle() {
        return new RtsWorkflowProgressData(
                null, 0, 0, 0, 0, 0.0F, false, false, List.of(), "");
    }

    /**
     * Returns {@code true} if this represents an active (non-idle) workflow.
     */
    public boolean isActive() {
        return type != null;
    }

    /**
     * Returns {@code true} if this workflow has missing items.
     */
    public boolean hasMissingItems() {
        return !missingItems.isEmpty();
    }

    /**
     * Returns {@code true} if this workflow has any failures.
     */
    public boolean hasFailures() {
        return failedBlocks > 0;
    }

    /**
     * Returns a human-readable progress summary string,
     * e.g. "45/100" or "0/0".
     */
    public String progressText() {
        return completedBlocks + "/" + (totalBlocks > 0 ? totalBlocks : 0);
    }

    /**
     * Returns the display label for the workflow type.
     */
    public String typeLabel() {
        if (type == null) return "Idle";
        return switch (type) {
            case MINE_SINGLE  -> "Mine";
            case ULTIMINE     -> "Ultimine";
            case AREA_MINE    -> "Area Mine";
            case AREA_DESTROY -> "Destroy";
            case PLACE_SINGLE -> "Place";
            case PLACE_BATCH  -> "Place Batch";
            case QUICK_BUILD  -> "Quick Build";
        };
    }
}
