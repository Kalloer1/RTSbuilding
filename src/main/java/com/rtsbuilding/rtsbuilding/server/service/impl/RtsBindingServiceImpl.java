package com.rtsbuilding.rtsbuilding.server.service.impl;

import com.rtsbuilding.rtsbuilding.common.build.BuilderMode;
import com.rtsbuilding.rtsbuilding.server.progression.RtsFeature;
import com.rtsbuilding.rtsbuilding.server.progression.RtsProgressionManager;
import com.rtsbuilding.rtsbuilding.server.service.QuestService;
import com.rtsbuilding.rtsbuilding.server.service.RtsRemoteMenuService;
import com.rtsbuilding.rtsbuilding.server.service.ServiceRegistry;
import com.rtsbuilding.rtsbuilding.server.service.api.BindingService;
import com.rtsbuilding.rtsbuilding.server.service.transfer.RtsTransferExtractor;
import com.rtsbuilding.rtsbuilding.server.service.transfer.RtsTransferInserter;
import com.rtsbuilding.rtsbuilding.server.storage.RtsStorageBindings;
import com.rtsbuilding.rtsbuilding.server.storage.model.LinkedStorageRef;
import com.rtsbuilding.rtsbuilding.server.storage.resolver.RtsLinkedStorageResolver;
import com.rtsbuilding.rtsbuilding.server.storage.session.RtsStorageSession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * {@link BindingService} 的默认实现——处理所有存储绑定相关的服务端逻辑。
 *
 * <p>该实现类通过 {@link ServiceRegistry} 调用其他子服务：
 * <ul>
 *   <li>使用 {@code registry.funnel()} 管理漏斗生命周期</li>
 *   <li>使用 {@code registry.session()} 获取/保存玩家会话</li>
 *   <li>使用 {@code registry.page()} 刷新储存页面</li>
 *   <li>使用 {@code registry.serviceOp()} 执行修改后操作</li>
 * </ul>
 *
 * <p>Phase 2 服务解耦的一部分。从静态方法 {@code RtsStorageBindings} 迁移而来。
 */
public final class RtsBindingServiceImpl implements BindingService {

    private final ServiceRegistry registry = ServiceRegistry.getInstance();

    @Override
    public void setMode(ServerPlayer player, BuilderMode mode) {
        RtsStorageSession session = registry.session().getOrCreate(player);
        if (RtsStorageBindings.setMode(session, mode)) {
            registry.funnel().disableAndFlush(player, session);
            registry.session().saveToPlayerNbt(player, session);
            registry.serviceOp().refreshPage(player, session);
        }
    }

    @Override
    public void linkStorage(ServerPlayer player, BlockPos pos, byte linkMode) {
        if (!RtsProgressionManager.canUse(player, RtsFeature.LINK_STORAGE)) return;
        if (!RtsLinkedStorageResolver.canAccessWorldTarget(player, pos)) return;
        RtsStorageSession session = registry.session().getOrCreate(player);
        applyUpdate(player, session, RtsStorageBindings.linkStorage(player, session, pos, linkMode));
    }

    @Override
    public void unlinkStorage(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) return;
        RtsStorageSession session = registry.session().getOrCreate(player);
        if (removeLinkedRef(session, player.serverLevel().dimension(), pos)) {
            registry.serviceOp().afterModification(player, session);
        }
    }

    private boolean removeLinkedRef(RtsStorageSession session, ResourceKey<Level> dimension, BlockPos pos) {
        if (session == null || dimension == null || pos == null || session.linkedStorageInfo.isEmpty()) {
            return false;
        }
        LinkedStorageRef ref = new LinkedStorageRef(dimension, pos.immutable());
        return session.linkedStorageInfo.remove(ref);
    }

    @Override
    public void updateLinkedStorageSettings(ServerPlayer player, BlockPos pos, byte linkMode, int priority) {
        if (player == null || pos == null) return;
        RtsStorageSession session = registry.session().getOrCreate(player);
        applyUpdate(player, session,
                RtsStorageBindings.updateLinkedStorageSettings(player, session, pos, linkMode, priority));
    }

    @Override
    public void setFunnelEnabled(ServerPlayer player, boolean enabled) {
        if (enabled && !RtsProgressionManager.canUse(player, RtsFeature.FUNNEL)) return;
        RtsStorageSession session = registry.session().getOrCreate(player);
        if (session.funnel.funnelEnabled == enabled) return;
        if (enabled) {
            registry.funnel().enable(player, session);
        } else {
            registry.funnel().disableAndFlush(player, session);
        }
        registry.serviceOp().refreshPage(player, session);
    }

    @Override
    public void updateFunnelTarget(ServerPlayer player, BlockPos target) {
        if (!RtsProgressionManager.canUse(player, RtsFeature.FUNNEL)) return;
        RtsStorageSession session = registry.session().getOrCreate(player);
        registry.funnel().updateTarget(player, session, target);
    }

    @Override
    public void setAutoStoreMinedDrops(ServerPlayer player, boolean enabled) {
        if (enabled && !RtsProgressionManager.canUse(player, RtsFeature.AUTO_STORE_MINED_DROPS)) return;
        RtsStorageSession session = registry.session().getOrCreate(player);
        session.sessionFlags.autoStoreMinedDrops = enabled;
        registry.serviceOp().simpleSave(player, session);
    }

    @Override
    public void setBdNetworkEnabled(ServerPlayer player, boolean enabled) {
        RtsStorageSession session = registry.session().getOrCreate(player);
        if (session.sessionFlags.useBdNetwork == enabled) return;
        session.sessionFlags.useBdNetwork = enabled;
        session.bdCache.handler = null;
        session.bdCache.fluidHandler = null;
        registry.serviceOp().afterModification(player, session);
    }

    @Override
    public void setQuickSlot(ServerPlayer player, byte slotId, String itemId, ItemStack previewStack) {
        RtsStorageSession session = registry.session().getOrCreate(player);
        applyUpdate(player, session, RtsStorageBindings.setQuickSlot(session, slotId, itemId, previewStack));
    }

    @Override
    public void setGuiBinding(ServerPlayer player, byte slotId, boolean clear, BlockPos pos, Direction face, String itemIdHint) {
        if (!clear && !RtsProgressionManager.canUse(player, RtsFeature.REMOTE_GUI_BINDING)) return;
        RtsStorageSession session = registry.session().getOrCreate(player);
        applyUpdate(player, session, RtsStorageBindings.setGuiBinding(player, session, slotId, clear, pos, face, itemIdHint));
    }

    @Override
    public void openGuiBinding(ServerPlayer player, byte slotId) {
        RtsStorageSession session = registry.session().getIfPresent(player);
        if (session == null) return;
        RtsStorageBindings.UpdateResult result = RtsStorageBindings.openGuiBinding(
                player, session, slotId, 4.0D);
        if (result != null && result.refreshPage()) {
            registry.page().requestPage(player, result.page(), session.browser.search, session.browser.category, session.browser.sort, session.browser.ascending);
        }
    }

    @Override
    public void storeHotbarSlot(ServerPlayer player, byte slotId) {
        RtsStorageSession session = registry.session().getIfPresent(player);
        if (session == null) return;
        RtsLinkedStorageResolver.sanitizeSessionDimension(player, session);
        if (!RtsLinkedStorageResolver.hasAnyStorage(player, session)) return;
        var activeLinked = RtsLinkedStorageResolver.resolveLinkedHandlers(player, session);
        if (activeLinked.isEmpty()) return;
        var handlers = RtsLinkedStorageResolver.itemHandlersForInsert(activeLinked);

        int slot = Math.max(0, Math.min(8, slotId));
        ItemStack inSlot = player.getInventory().getItem(slot);
        if (inSlot.isEmpty()) return;

        ItemStack remaining = RtsTransferInserter.storeToLinkedOnlyPreferExisting(handlers, inSlot.copy());
        if (remaining.getCount() == inSlot.getCount()) return;

        player.getInventory().setItem(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
        player.containerMenu.broadcastChanges();
        registry.serviceOp().afterModification(player, session);
        QuestService.runQuestDetect(player, session, false);
    }

    @Override
    public void swapHotbarSlot(ServerPlayer player, byte slotId, String targetItemId) {
        RtsStorageSession session = registry.session().getIfPresent(player);
        if (session == null) return;
        RtsLinkedStorageResolver.sanitizeSessionDimension(player, session);

        int slot = Math.max(0, Math.min(8, slotId));
        ItemStack inSlot = player.getInventory().getItem(slot);

        if (inSlot.isEmpty() && (targetItemId == null || targetItemId.isBlank())) {
            return;
        }

        boolean hasLinkedStorage = RtsLinkedStorageResolver.hasAnyStorage(player, session);
        
        if (hasLinkedStorage) {
            var activeLinked = RtsLinkedStorageResolver.resolveLinkedHandlers(player, session);
            if (activeLinked.isEmpty()) return;
            var insertHandlers = RtsLinkedStorageResolver.itemHandlersForInsert(activeLinked);
            var extractHandlers = RtsLinkedStorageResolver.itemHandlersForExtract(activeLinked);

            ItemStack toStore = inSlot.isEmpty() ? ItemStack.EMPTY : inSlot.copy();
            ItemStack remaining = ItemStack.EMPTY;
            
            if (!toStore.isEmpty()) {
                remaining = RtsTransferInserter.storeToLinkedOnlyPreferExisting(insertHandlers, toStore);
            }

            ItemStack extracted = ItemStack.EMPTY;
            if (targetItemId != null && !targetItemId.isBlank()) {
                ResourceLocation itemRl = ResourceLocation.tryParse(targetItemId);
                if (itemRl != null) {
                    Item targetItem = BuiltInRegistries.ITEM.get(itemRl);
                    if (targetItem != Items.AIR) {
                        int extractCount = inSlot.isEmpty() ? 64 : inSlot.getCount();
                        extracted = RtsTransferExtractor.extractMatchingFromLinked(
                                extractHandlers, targetItem, extractCount);
                    }
                }
            }

            if (!extracted.isEmpty()) {
                player.getInventory().setItem(slot, extracted);
            } else if (!remaining.isEmpty()) {
                player.getInventory().setItem(slot, remaining);
            } else {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
            
            player.containerMenu.broadcastChanges();
            registry.serviceOp().afterModification(player, session);
            QuestService.runQuestDetect(player, session, false);
        } else {
            if (targetItemId != null && !targetItemId.isBlank()) {
                ResourceLocation itemRl = ResourceLocation.tryParse(targetItemId);
                if (itemRl != null) {
                    Item targetItem = BuiltInRegistries.ITEM.get(itemRl);
                    if (targetItem != Items.AIR) {
                        int extractCount = inSlot.isEmpty() ? 64 : inSlot.getCount();
                        
                        int foundIndex = -1;
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            if (i == slot) continue;
                            ItemStack stack = player.getInventory().getItem(i);
                            if (!stack.isEmpty() && stack.is(targetItem)) {
                                foundIndex = i;
                                break;
                            }
                        }
                        
                        if (foundIndex >= 0) {
                            ItemStack foundStack = player.getInventory().getItem(foundIndex);
                            int count = Math.min(foundStack.getCount(), extractCount);
                            
                            if (!inSlot.isEmpty()) {
                                player.getInventory().setItem(foundIndex, inSlot.copy());
                            } else {
                                player.getInventory().setItem(foundIndex, ItemStack.EMPTY);
                            }
                            
                            ItemStack toMove = foundStack.split(count);
                            player.getInventory().setItem(slot, toMove);
                        } else if (!inSlot.isEmpty()) {
                            player.getInventory().setItem(slot, ItemStack.EMPTY);
                            player.getInventory().add(inSlot);
                        }
                    }
                }
            }
            player.containerMenu.broadcastChanges();
        }
    }

    @Override
    public void closeRemoteMenu(ServerPlayer player) {
        RtsStorageSession session = registry.session().getIfPresent(player);
        if (session == null || session.transfer.remoteMenuContainerId < 0) return;
        RtsRemoteMenuService.closeTracked(player, session);
        RtsRemoteMenuService.clearValidation(player, session);
    }

    // ────────────────────────────────────────────────────────────────
    //  Internal helpers
    // ────────────────────────────────────────────────────────────────

    private void applyUpdate(ServerPlayer player, RtsStorageSession session, RtsStorageBindings.UpdateResult update) {
        if (player == null || session == null || update == null) return;
        if (update.saveSession()) {
            registry.session().saveToPlayerNbt(player, session);
        }
        if (update.refreshPage()) {
            registry.serviceOp().markDirty(player, session);
            registry.page().requestPage(player, update.page(), session.browser.search, session.browser.category, session.browser.sort, session.browser.ascending);
        }
    }
}
