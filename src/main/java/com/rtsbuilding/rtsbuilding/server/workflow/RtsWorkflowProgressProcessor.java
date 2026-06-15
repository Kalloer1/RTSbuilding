package com.rtsbuilding.rtsbuilding.server.workflow;

import com.rtsbuilding.rtsbuilding.server.storage.RtsStorageSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified API processor for workflow progress data.
 *
 * <p>This is the single entry point for converting raw workflow state into
 * structured {@link RtsWorkflowProgressData} that the UI panel can consume
 * directly.  Both server-side API methods and client-side rendering should
 * go through this processor instead of manually computing
 * {@code total - (completed + failed)} or checking for completeness.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Server-side: get progress for a specific workflow type
 * RtsWorkflowProgressData data =
 *     RtsWorkflowProgressProcessor.processByType(session, RtsWorkflowType.AREA_DESTROY);
 *
 * // Client-side: from an already-received status
 * RtsWorkflowProgressData data =
 *     RtsWorkflowProgressProcessor.process(status);
 *
 * // Then pass data to the panel renderer:
 * panel.renderRow(g, x, y, data, mouseX, mouseY);
 * }</pre>
 */
public final class RtsWorkflowProgressProcessor {

    private RtsWorkflowProgressProcessor() {
    }

    // ======================================================================
    //  Processing: RtsWorkflowStatus → RtsWorkflowProgressData
    // ======================================================================

    /**
     * Converts a single {@link RtsWorkflowStatus} snapshot into structured
     * progress data with pre-computed derived values.
     *
     * @param status the workflow status snapshot (may be {@code null})
     * @return the structured progress data, or {@link RtsWorkflowProgressData#idle()}
     *         if {@code status} is null or has no type
     */
    public static RtsWorkflowProgressData process(RtsWorkflowStatus status) {
        if (status == null || status.type() == null) {
            return RtsWorkflowProgressData.idle();
        }
        int total = status.totalBlocks();
        int completed = status.completedBlocks();
        int failed = status.failedBlocks();
        int remaining = status.remainingBlocks();
        float progress = status.progress();
        boolean isComplete = status.isComplete();

        return new RtsWorkflowProgressData(
                status.type(),
                total,
                completed,
                remaining,
                failed,
                progress,
                status.suspended(),
                isComplete,
                status.missingItems(),
                status.detailMessage());
    }

    // ======================================================================
    //  Processing: from session (server-side)
    // ======================================================================

    /**
     * Processes all occupied workflow entries from a session into a list
     * of structured progress data.
     *
     * @param session the player's storage session (may be {@code null})
     * @return list of progress data for all occupied entries; empty if none
     */
    public static List<RtsWorkflowProgressData> processAll(RtsStorageSession session) {
        if (session == null) {
            return List.of();
        }
        List<RtsWorkflowStatus> snapshots = session.workflow.snapshots();
        List<RtsWorkflowProgressData> result = new ArrayList<>(snapshots.size());
        for (RtsWorkflowStatus status : snapshots) {
            result.add(process(status));
        }
        return result;
    }

    /**
     * Finds and processes the first workflow matching the given type.
     *
     * @param session the player's storage session (may be {@code null})
     * @param type    the workflow type to look for
     * @return the structured progress data, or
     *         {@link RtsWorkflowProgressData#idle()} if not found
     */
    public static RtsWorkflowProgressData processByType(RtsStorageSession session, RtsWorkflowType type) {
        if (session == null || type == null) {
            return RtsWorkflowProgressData.idle();
        }
        RtsWorkflowStatus status = RtsWorkflowManager.findWorkflowStatus(session, type);
        return process(status);
    }

    /**
     * Processes the workflow entry at the given index.
     *
     * @param session the player's storage session (may be {@code null})
     * @param index   the 0-based entry index
     * @return the structured progress data, or
     *         {@link RtsWorkflowProgressData#idle()} if not found
     */
    public static RtsWorkflowProgressData processByIndex(RtsStorageSession session, int index) {
        if (session == null || index < 0) {
            return RtsWorkflowProgressData.idle();
        }
        RtsWorkflowState.Entry entry = session.workflow.getEntry(index);
        if (entry == null || !entry.isOccupied()) {
            return RtsWorkflowProgressData.idle();
        }
        return process(entry.snapshot());
    }

    // ======================================================================
    //  Panel rendering helpers
    // ======================================================================

    /**
     * Computes the fill width in pixels for a progress bar of the given width.
     *
     * @param data     the progress data
     * @param barWidth the total width of the progress bar in pixels
     * @return the fill width in pixels (clamped to [0, barWidth])
     */
    public static int computeFillWidth(RtsWorkflowProgressData data, int barWidth) {
        if (data == null || !data.isActive() || data.totalBlocks() <= 0 || barWidth <= 0) {
            return 0;
        }
        float fraction = (float) data.completedBlocks() / (float) data.totalBlocks();
        return Math.min(barWidth, Math.round(barWidth * Math.min(1.0F, fraction)));
    }

    /**
     * Returns a display string showing completed / total, e.g. "45/100".
     */
    public static String formatProgressText(RtsWorkflowProgressData data) {
        if (data == null || !data.isActive()) return "";
        return data.progressText();
    }

    /**
     * Returns the display label for this workflow entry, optionally
     * appending a "(suspended)" suffix.
     */
    public static String formatLabel(RtsWorkflowProgressData data) {
        if (data == null || !data.isActive()) return "";
        String label = data.typeLabel();
        if (data.suspended()) {
            label += " (搁置)";
        }
        return label;
    }
}
