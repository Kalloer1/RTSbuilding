package com.rtsbuilding.rtsbuilding.server.service.transfer;

import com.rtsbuilding.rtsbuilding.server.storage.OverflowOutcome;
import com.rtsbuilding.rtsbuilding.server.storage.RtsLinkedHandlerViews;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

/**
 * Handles insertion of items into linked storage handlers and player inventory.
 */
public final class RtsTransferInserter {

    private RtsTransferInserter() {
    }

    // ---- handler-level insert ---------------------------------------------------

    public static ItemStack insertToHandler(IItemHandler handler, ItemStack stack) {
        return RtsLinkedHandlerViews.insertItemAnywhere(handler, stack, false);
    }

    public static ItemStack insertToHandlerPreferExisting(IItemHandler handler, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack anySlotRemain = RtsLinkedHandlerViews.insertItemAnywhereIfSupported(handler, stack, false);
        if (anySlotRemain != null) {
            return anySlotRemain;
        }
        ItemStack remain = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remain.isEmpty(); slot++) {
            ItemStack slotStack = handler.getStackInSlot(slot);
            if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(slotStack, remain)) {
                continue;
            }
            remain = handler.insertItem(slot, remain, false);
        }
        for (int slot = 0; slot < handler.getSlots() && !remain.isEmpty(); slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) {
                continue;
            }
            remain = handler.insertItem(slot, remain, false);
        }
        return remain;
    }

    // ---- multi-handler store ----------------------------------------------------

    public static ItemStack storeToLinkedOnly(List<IItemHandler> handlers, ItemStack stack) {
        ItemStack remain = stack.copy();
        for (IItemHandler handler : handlers) {
            if (remain.isEmpty()) {
                break;
            }
            remain = insertToHandler(handler, remain);
        }
        return remain;
    }

    public static ItemStack storeToLinkedOnlyPreferExisting(List<IItemHandler> handlers, ItemStack stack) {
        ItemStack remain = stack.copy();
        for (IItemHandler handler : handlers) {
            if (remain.isEmpty()) {
                break;
            }
            remain = insertToHandlerPreferExisting(handler, remain);
        }
        return remain;
    }

    // ---- with fallback ----------------------------------------------------------

    public static OverflowOutcome storeToLinkedWithFallback(
            List<IItemHandler> handlers, ServerPlayer player, ItemStack stack) {
        ItemStack remain = stack.copy();
        for (IItemHandler handler : handlers) {
            if (remain.isEmpty()) {
                break;
            }
            remain = insertToHandler(handler, remain);
        }
        int movedToInventory = 0;
        if (!remain.isEmpty()) {
            ItemStack invStack = remain.copy();
            int before = invStack.getCount();
            player.getInventory().add(invStack);
            movedToInventory = before - invStack.getCount();
            remain = invStack;
        }
        int dropped = 0;
        if (!remain.isEmpty()) {
            dropped = remain.getCount();
            player.drop(remain, false);
        }
        return new OverflowOutcome(movedToInventory, dropped);
    }

    public static OverflowOutcome storeToLinkedWithFallbackPreferExisting(
            List<IItemHandler> handlers, ServerPlayer player, ItemStack stack) {
        ItemStack remain = stack.copy();
        for (IItemHandler handler : handlers) {
            if (remain.isEmpty()) {
                break;
            }
            remain = insertToHandlerPreferExisting(handler, remain);
        }
        int movedToInventory = 0;
        if (!remain.isEmpty()) {
            ItemStack invStack = remain.copy();
            int before = invStack.getCount();
            player.getInventory().add(invStack);
            movedToInventory = before - invStack.getCount();
            remain = invStack;
        }
        int dropped = 0;
        if (!remain.isEmpty()) {
            dropped = remain.getCount();
            player.drop(remain, false);
        }
        return new OverflowOutcome(movedToInventory, dropped);
    }

    // ---- refund / move helpers --------------------------------------------------

    public static void refundToLinked(List<IItemHandler> handlers, ServerPlayer player, ItemStack stack) {
        storeToLinkedWithFallback(handlers, player, stack);
    }

    public static void refundItem(IItemHandler handler, ServerPlayer player, ItemStack stack) {
        ItemStack remain = insertToHandler(handler, stack);
        if (!remain.isEmpty()) {
            player.drop(remain, false);
        }
    }

    public static ItemStack moveToPlayerInventoryOnly(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remain = stack.copy();
        player.getInventory().add(remain);
        return remain;
    }

    public static ItemStack moveLinkedStackIntoOpenMenu(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return stack.copy();
        }
        ItemStack remain = stack.copy();
        for (int pass = 0; pass < 2 && !remain.isEmpty(); pass++) {
            boolean fillExisting = pass == 0;
            for (Slot slot : menu.slots) {
                if (slot == null || slot.container == player.getInventory() || !slot.isActive() || !slot.mayPlace(remain)) {
                    continue;
                }
                ItemStack inSlot = slot.getItem();
                if (fillExisting) {
                    if (inSlot.isEmpty() || !ItemStack.isSameItemSameComponents(inSlot, remain)) {
                        continue;
                    }
                    int max = Math.min(slot.getMaxStackSize(remain), remain.getMaxStackSize());
                    int free = Math.max(0, max - inSlot.getCount());
                    if (free <= 0) {
                        continue;
                    }
                    int move = Math.min(free, remain.getCount());
                    if (move <= 0) {
                        continue;
                    }
                    inSlot.grow(move);
                    slot.setChanged();
                    remain.shrink(move);
                    continue;
                }
                if (!inSlot.isEmpty()) {
                    continue;
                }
                int move = Math.min(slot.getMaxStackSize(remain), remain.getCount());
                if (move <= 0) {
                    continue;
                }
                ItemStack placed = remain.copyWithCount(move);
                slot.set(placed);
                slot.setChanged();
                remain.shrink(move);
            }
        }
        return remain;
    }

    // ---- feedback ---------------------------------------------------------------

    public static void sendStorageOverflowHint(ServerPlayer player, String context, OverflowOutcome overflow) {
        if (!overflow.hasOverflow()) {
            return;
        }
        String message;
        if (overflow.movedToInventory() > 0 && overflow.dropped() > 0) {
            message = context + ": linked storage full, moved " + overflow.movedToInventory()
                    + " to inventory, dropped " + overflow.dropped() + ".";
        } else if (overflow.movedToInventory() > 0) {
            message = context + ": linked storage full, moved " + overflow.movedToInventory() + " to inventory.";
        } else {
            message = context + ": linked+inventory full, dropped " + overflow.dropped() + ".";
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
    }
}
