package com.rtsbuilding.rtsbuilding.server.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable state container for workflow tracking within a player's
 * {@link com.rtsbuilding.rtsbuilding.server.storage.RtsStorageSession}.
 *
 * <p>Each player can have up to {@link #MAX_WORKFLOWS} concurrent workflow
 * entries (mining, placement, etc.).  Entries are managed as a stack — new
 * workflows are pushed, and the most recent one is updated or popped.</p>
 *
 * <p>An entry may be <em>suspended</em>, meaning it still occupies a slot in
 * the 8-thread limit but is waiting for items to become available (e.g.
 * placement ran out of blocks).  Suspended entries can be resumed later by
 * submitting the required items.</p>
 *
 * <p>This class is deliberately a simple data holder — all business logic
 * lives in {@link RtsWorkflowManager}.</p>
 */
public class RtsWorkflowState {

    /** Maximum number of concurrent workflows per player. */
    public static final int MAX_WORKFLOWS = 8;

    /** Active workflow entries, ordered from oldest to newest. */
    public final List<Entry> entries = new ArrayList<>(MAX_WORKFLOWS);

    /**
     * A single workflow entry holding mutable progress data.
     */
    public static final class Entry {
        public RtsWorkflowType activeType;
        public RtsWorkflowPriority priority = RtsWorkflowPriority.NORMAL;
        public int totalBlocks;
        public int completedBlocks;
        public int failedBlocks;
        public final List<String> missingItems = new ArrayList<>();
        public String detailMessage = "";
        /** Unique auto-incrementing ID for this entry within the session. */
        public int id;
        /** {@code true} when this entry is suspended (waiting for items). */
        public boolean suspended;

        Entry(int id) {
            this.id = id;
        }

        /**
         * Resets this entry to its default state.
         */
        void reset() {
            this.activeType = null;
            this.priority = RtsWorkflowPriority.NORMAL;
            this.totalBlocks = 0;
            this.completedBlocks = 0;
            this.failedBlocks = 0;
            this.missingItems.clear();
            this.detailMessage = "";
            this.suspended = false;
        }

        /**
         * Creates an immutable snapshot of this entry.
         */
        public RtsWorkflowStatus snapshot() {
            if (activeType == null) {
                return RtsWorkflowStatus.idle();
            }
            return new RtsWorkflowStatus(
                    activeType,
                    priority,
                    totalBlocks,
                    completedBlocks,
                    failedBlocks,
                    List.copyOf(missingItems),
                    detailMessage,
                    suspended,
                    this.id);
        }

        /**
         * Returns {@code true} if this entry represents a running (non-suspended) workflow.
         */
        public boolean hasActiveWorkflow() {
            return activeType != null && !suspended;
        }

        /**
         * Returns {@code true} if this entry occupies a slot (active or suspended).
         */
        public boolean isOccupied() {
            return activeType != null;
        }
    }

    private int nextId;

    // ======================================================================
    //  Entry management
    // ======================================================================

    /**
     * Adds a new active workflow entry.  If the list is already at
     * {@link #MAX_WORKFLOWS}, the entry is rejected and no changes are made.
     *
     * @return the index of the newly added entry (0-based), or -1 if the
     *         maximum number of workflows is already active
     */
    public int addEntry() {
        if (entries.size() >= MAX_WORKFLOWS) {
            return -1;
        }
        Entry entry = new Entry(nextId++);
        entry.activeType = RtsWorkflowType.MINE_SINGLE; // temp placeholder, caller will set
        entries.add(entry);
        return entries.size() - 1;
    }

    /**
     * Removes the entry at the given index.
     */
    public void removeEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
        }
    }

    /**
     * Returns the entry at the given index, or {@code null} if out of range.
     */
    public Entry getEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            return entries.get(index);
        }
        return null;
    }

    /**
     * Returns the index of the most recent (last) active entry, or -1 if none.
     */
    public int lastActiveIndex() {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).hasActiveWorkflow()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the most recent active entry, or {@code null} if none.
     */
    public Entry lastActive() {
        int idx = lastActiveIndex();
        return idx >= 0 ? entries.get(idx) : null;
    }

    /**
     * Returns an immutable list of snapshots for all active entries.
     */
    public List<RtsWorkflowStatus> snapshots() {
        List<RtsWorkflowStatus> result = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (entry.isOccupied()) {
                result.add(new RtsWorkflowStatus(
                        entry.activeType,
                        entry.priority,
                        entry.totalBlocks,
                        entry.completedBlocks,
                        entry.failedBlocks,
                        List.copyOf(entry.missingItems),
                        entry.detailMessage,
                        entry.suspended,
                        entry.id));
            }
        }
        return result;
    }

    /**
     * Returns the total number of active (non-suspended) workflow entries.
     */
    public int activeCount() {
        int count = 0;
        for (Entry e : entries) {
            if (e.hasActiveWorkflow()) count++;
        }
        return count;
    }

    /**
     * Returns the total number of occupied slots (active + suspended).
     */
    public int occupiedCount() {
        int count = 0;
        for (Entry e : entries) {
            if (e.isOccupied()) count++;
        }
        return count;
    }

    /**
     * Returns {@code true} if all {@link #MAX_WORKFLOWS} slots are occupied.
     */
    public boolean isFull() {
        return entries.size() >= MAX_WORKFLOWS;
    }

    /**
     * Returns {@code true} if any workflow entry is active (non-suspended).
     */
    public boolean hasActiveWorkflow() {
        for (Entry e : entries) {
            if (e.hasActiveWorkflow()) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if any workflow entry is suspended.
     */
    public boolean hasSuspendedWorkflow() {
        for (Entry e : entries) {
            if (e.isOccupied() && e.suspended) return true;
        }
        return false;
    }

    /**
     * Returns the index of the most recent suspended entry, or -1 if none.
     */
    public int lastSuspendedIndex() {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).isOccupied() && entries.get(i).suspended) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the most recent suspended entry, or {@code null} if none.
     */
    public Entry lastSuspended() {
        int idx = lastSuspendedIndex();
        return idx >= 0 ? entries.get(idx) : null;
    }

    /**
     * Clears all workflow entries.
     */
    public void reset() {
        entries.clear();
    }
}
