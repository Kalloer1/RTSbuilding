package com.rtsbuilding.rtsbuilding.server.service;

import com.rtsbuilding.rtsbuilding.server.storage.session.RtsStorageSession;
import net.minecraft.server.level.ServerPlayer;

/**
 * 服务操作模板——封装 Service 方法尾部常见的重复操作模式。
 *
 * <p>通过 ServiceRegistry 获取实例，支持构造器注入以便测试。
 */
public final class ServiceOperationTemplate {

    private final ServiceRegistry registry;

    public ServiceOperationTemplate(ServiceRegistry registry) {
        this.registry = registry;
    }

    /**
     * 完整三板斧——存储变更后的标准操作。
     *
     * <ol>
     *   <li>强制刷新存储缓存（{@link RtsStorageTickService#forceRefresh}）</li>
     *   <li>递增数据版本（{@code session.transfer.pageDataVersion}）</li>
     *   <li>推送刷新页面到客户端（{@link com.rtsbuilding.rtsbuilding.server.service.api.PageService#requestPage}）</li>
     *   <li>持久化会话（{@link com.rtsbuilding.rtsbuilding.server.service.api.SessionService#saveToPlayerNbt}）</li>
     * </ol>
     */
    public void afterModification(ServerPlayer player, RtsStorageSession session) {
        RtsStorageTickService.INSTANCE.forceRefresh(player);
        session.transfer.pageDataVersion.incrementAndGet();
        registry.page().requestPage(player, session.browser.page, session.browser.search,
                session.browser.category, session.browser.sort, session.browser.ascending);
        registry.session().saveToPlayerNbt(player, session);
    }

    /**
     * 简化的保存模式——无 forceRefresh，适用于仅变更浏览器状态等非存储数据的场景。
     */
    public void simpleSave(ServerPlayer player, RtsStorageSession session) {
        registry.page().requestPage(player, session.browser.page, session.browser.search,
                session.browser.category, session.browser.sort, session.browser.ascending);
        registry.session().saveToPlayerNbt(player, session);
    }

    /**
     * 仅标记脏 + bump 数据版本——适用于不需要立即刷新页面的场景
     * （页面将在下一次 tick 或显式请求时自动刷新）。
     */
    public void markDirty(ServerPlayer player, RtsStorageSession session) {
        RtsStorageTickService.INSTANCE.forceRefresh(player);
        session.transfer.pageDataVersion.incrementAndGet();
    }

    /**
     * 直接刷新页面——不 bump 版本也不保存，适用于页面版本已由外部递增过的场景。
     */
    public void refreshPage(ServerPlayer player, RtsStorageSession session) {
        registry.page().requestPage(player, session.browser.page, session.browser.search,
                session.browser.category, session.browser.sort, session.browser.ascending);
    }
}
