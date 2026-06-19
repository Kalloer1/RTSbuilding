package com.rtsbuilding.rtsbuilding.client.state;


import com.rtsbuilding.rtsbuilding.client.controller.ClientRtsController;
import com.rtsbuilding.rtsbuilding.client.screen.handler.ScreenShapeController;
import com.rtsbuilding.rtsbuilding.client.screen.panel.RtsWindowPanel;
import com.rtsbuilding.rtsbuilding.client.screen.quickbuild.BuildShape;
import com.rtsbuilding.rtsbuilding.client.screen.quickbuild.QuickBuildPanel;
import com.rtsbuilding.rtsbuilding.client.screen.standalone.BuilderScreen;
import com.rtsbuilding.rtsbuilding.common.persist.PersistableProperty;
import com.rtsbuilding.rtsbuilding.common.persist.RtsClientUiStateStore;
import com.rtsbuilding.rtsbuilding.common.persist.UiStateCache;
import com.rtsbuilding.rtsbuilding.common.shape.model.ShapeFillMode;
import net.minecraft.util.Mth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.rtsbuilding.rtsbuilding.client.screen.standalone.BuilderScreenConstants.*;

/**
 * 管理 {@link BuilderScreen} 的持久化 UI 偏好设置——业务管理层。
 *
 * <p>通过双向同步桥接 {@link RtsClientUiStateStore.UiState} 与运行时 UI 组件
 *（面板、形状控制器、相机控制器等）。
 *
 * <h3>架构</h3>
 * <ul>
 *   <li><b>Manager</b>——桥接 {@link RtsClientUiStateStore Store} 与 UI 组件</li>
 *   <li><b>批量加载</b>——{@link #applyStoredUiState()} 从缓存读取并分发到各组件</li>
 *   <li><b>批量持久化</b>——{@link #persistUiState()} 收集组件状态并标记脏</li>
 *   <li><b>窗口面板边界</b>——注册/注销可拖拽面板并持久化其边界</li>
 *   <li><b>缩放管理</b>——GUI 缩放调整和格式化标签生成</li>
 * </ul>
 *
 * <p>此类不直接执行 I/O；所有持久化通过 {@link UiStateCache} 延迟写入。
 *
 * @see UiStateCache
 * @see RtsClientUiStateStore
 * @see BuilderScreen
 */
public final class RtsScreenUiStateManager {
    /** 客户端 RTS 控制器，用于读写相机、输入和视觉偏好 */
    private final ClientRtsController controller;
    /** 形状控制器，用于读写形状模式、填充模式和旋转 */
    private final ScreenShapeController shapeController;
    /** 快速建造面板，持久化其打开状态 */
    private final QuickBuildPanel quickBuildPanel;

    /** 已注册的可持久化窗口面板（键 → 面板） */
    private final Map<String, RtsWindowPanel> persistablePanels = new LinkedHashMap<>();

    /** UI 状态缓存（延迟写入，消除冗余 I/O） */
    private final UiStateCache cache;

    /** 控制器级别设置的声明式双向绑定列表 */
    private final List<CtrlBind> ctrlBindings;

    /** 调试按钮可见性（运行时状态，仅由此类管理） */
    private boolean debugButtonVisible = false;
    /** 缓存的固定 RTS GUI 缩放值 */
    private double fixedRtsGuiScale = DEFAULT_RTS_GUI_SCALE;

    // =============== 覆盖层与存储 UI 偏好（运行时字段，通过 CtrlBind 与 UiState 同步） ===============
    private boolean containerOverlayEnabled;
    private boolean overlayShiftImportEnabled;
    private boolean showStorageReadyPopup;
    private boolean showWorkflowPanel;
    private boolean storageRefreshQuietEnabled;
    private boolean storageAutoRefreshEnabled;

    /**
     * 构造 UI 状态管理器。
     *
     * @param controller      客户端 RTS 控制器
     * @param shapeController 形状控制器
     * @param quickBuildPanel 快速建造面板（自动注册为 "quick_build"）
     */
    public RtsScreenUiStateManager(
            ClientRtsController controller,
            ScreenShapeController shapeController,
            QuickBuildPanel quickBuildPanel) {
        this.controller = controller;
        this.shapeController = shapeController;
        this.quickBuildPanel = quickBuildPanel;
        this.cache = RtsClientUiStateStore.cache();

        // ===== 声明式控制器绑定 =====
        this.ctrlBindings = List.of(
                // ---- 简单布尔：直接 controller ↔ UiState ----
                CtrlBind.bool(controller::isChunkCurtainVisible, controller::setChunkCurtainVisible,
                        s -> s.chunkCurtainVisible, (s, v) -> s.chunkCurtainVisible = v),
                CtrlBind.bool(controller::isStartCameraAtPlayerHead, controller::setStartCameraAtPlayerHead,
                        s -> s.startCameraAtPlayerHead, (s, v) -> s.startCameraAtPlayerHead = v),
                CtrlBind.bool(controller::isAllowPlacedBlockRecovery, controller::setAllowPlacedBlockRecovery,
                        s -> s.allowPlacedBlockRecovery, (s, v) -> s.allowPlacedBlockRecovery = v),
                CtrlBind.bool(controller::isToolProtectionEnabled, controller::setToolProtectionEnabled,
                        s -> s.toolProtectionEnabled, (s, v) -> s.toolProtectionEnabled = v),
                CtrlBind.bool(controller::isPlayerStatusOverlayEnabled, controller::setPlayerStatusOverlayEnabled,
                        s -> s.playerStatusOverlayEnabled, (s, v) -> s.playerStatusOverlayEnabled = v),
                CtrlBind.bool(controller::isInvertPanDragX, v -> controller.setInvertPanDragX(v),
                        s -> s.invertPanDragX, (s, v) -> s.invertPanDragX = v),
                CtrlBind.bool(controller::isInvertPanDragY, v -> controller.setInvertPanDragY(v),
                        s -> s.invertPanDragY, (s, v) -> s.invertPanDragY = v),
                CtrlBind.bool(controller::isSmoothCamera, controller::setSmoothCamera,
                        s -> s.smoothCamera, (s, v) -> s.smoothCamera = v),
                CtrlBind.bool(controller::isDamageSoundEnabled, controller::setDamageSoundEnabled,
                        s -> s.damageSoundEnabled, (s, v) -> s.damageSoundEnabled = v),
                CtrlBind.bool(controller::isDamageAutoReturnEnabled, controller::setDamageAutoReturnEnabled,
                        s -> s.damageAutoReturnEnabled, (s, v) -> s.damageAutoReturnEnabled = v),
                // lineConnected 走 shapeController 而非 controller
                CtrlBind.bool(shapeController::isLineConnected, shapeController::setLineConnected,
                        s -> s.lineConnected, (s, v) -> s.lineConnected = v),
                // debugButtonVisible 是本地字段
                CtrlBind.bool(() -> this.debugButtonVisible, v -> this.debugButtonVisible = v,
                        s -> s.debugButtonVisible, (s, v) -> s.debugButtonVisible = v),

                // ---- 覆盖层与存储 UI 偏好（本地字段 ↔ UiState） ----
                CtrlBind.bool(() -> this.containerOverlayEnabled, v -> this.containerOverlayEnabled = v,
                        s -> s.containerOverlayEnabled, (s, v) -> s.containerOverlayEnabled = v),
                CtrlBind.bool(() -> this.overlayShiftImportEnabled, v -> this.overlayShiftImportEnabled = v,
                        s -> s.overlayShiftImportEnabled, (s, v) -> s.overlayShiftImportEnabled = v),
                CtrlBind.bool(() -> this.showStorageReadyPopup, v -> this.showStorageReadyPopup = v,
                        s -> s.showStorageReadyPopup, (s, v) -> s.showStorageReadyPopup = v),
                CtrlBind.bool(() -> this.showWorkflowPanel, v -> this.showWorkflowPanel = v,
                        s -> s.showWorkflowPanel, (s, v) -> s.showWorkflowPanel = v),
                CtrlBind.bool(() -> this.storageRefreshQuietEnabled, v -> this.storageRefreshQuietEnabled = v,
                        s -> s.storageRefreshQuietEnabled, (s, v) -> s.storageRefreshQuietEnabled = v),
                CtrlBind.bool(() -> this.storageAutoRefreshEnabled, v -> this.storageAutoRefreshEnabled = v,
                        s -> s.storageAutoRefreshEnabled, (s, v) -> s.storageAutoRefreshEnabled = v),

                // ---- 建造形状（collect 取 QuickBuildPanel，apply 连带加载和兜底） ----
                new CtrlBind(
                        state -> state.buildShape = this.quickBuildPanel.getBuildModeShape().name(),
                        state -> {
                            parseAndSetBuildShape(state.buildShape);
                            this.quickBuildPanel.loadStoredShapes(
                                    this.controller.getBuildShape(), this.controller.getAreaMineShape());
                            this.shapeController.ensureFillModeForShape(this.controller.getBuildShape());
                        }
                ),
                // ---- 填充模式 ----
                new CtrlBind(
                        state -> state.fillMode = this.shapeController.getShapeFillMode().name(),
                        state -> parseAndSetFillMode(state.fillMode)
                ),
                // ---- 旋转角度 ----
                new CtrlBind(
                        state -> state.rotationDegrees = this.shapeController.getShapeRotateDegrees(),
                        state -> this.shapeController.rotateToDegrees(Math.floorMod(state.rotationDegrees, 360))
                ),
                // ---- GUI 缩放（带 sanitize 兜底） ----
                new CtrlBind(
                        state -> state.rtsGuiScale = sanitizeRtsGuiScale(this.fixedRtsGuiScale),
                        state -> this.fixedRtsGuiScale = sanitizeRtsGuiScale(state.rtsGuiScale)
                ),
                // ---- 输入灵敏度（int → fraction 转换） ----
                new CtrlBind(
                        state -> state.inputSensitivityIndex = this.controller.getInputSensitivityIndex(),
                        state -> applyInputSensitivity(state.inputSensitivityIndex)
                )
        );

        // 注册可持久化位置的窗口面板
        registerWindowPanel("quick_build", quickBuildPanel);
    }

    /**
     * 注册窗口面板，使其位置/大小可被持久化。
     * 键必须稳定且唯一，用作 JSON 存储键。重复注册会覆盖旧值。
     */
    public void registerWindowPanel(String key, RtsWindowPanel panel) {
        this.persistablePanels.put(key, panel);
    }

    /** 注销窗口面板，停止其位置持久化。 */
    public void unregisterWindowPanel(String key) {
        this.persistablePanels.remove(key);
    }

    // ======================== Flush（由 BuilderScreen.tick 调用） ========================

    /**
     * 将缓存的脏状态刷入磁盘。仅在标记为脏时执行实际写入。
     * <p>应在每 tick 调用一次（由 {@link BuilderScreen#tick()} 驱动）。
     */
    public void flush() {
        this.cache.flushIfDirty();
    }

    // ======================== Debug 按钮 ========================

    /** 调试按钮是否可见。 */
    public boolean isDebugButtonVisible() {
        return this.debugButtonVisible;
    }

    /** 切换调试按钮可见性。 */
    public void toggleDebugButton() {
        this.debugButtonVisible = !this.debugButtonVisible;
    }

    // ======================== 覆盖层与存储 UI 偏好 ========================

    public boolean isContainerOverlayEnabled() { return this.containerOverlayEnabled; }
    public void toggleContainerOverlayEnabled() { this.containerOverlayEnabled = !this.containerOverlayEnabled; persistUiState(); }

    public boolean isOverlayShiftImportEnabled() { return this.overlayShiftImportEnabled; }
    public void toggleOverlayShiftImportEnabled() { this.overlayShiftImportEnabled = !this.overlayShiftImportEnabled; persistUiState(); }

    public boolean isShowStorageReadyPopupEnabled() { return this.showStorageReadyPopup; }
    public void toggleShowStorageReadyPopup() { this.showStorageReadyPopup = !this.showStorageReadyPopup; persistUiState(); }

    public boolean isShowWorkflowPanelEnabled() { return this.showWorkflowPanel; }
    public void toggleShowWorkflowPanelEnabled() { this.showWorkflowPanel = !this.showWorkflowPanel; persistUiState(); }

    public boolean isStorageRefreshQuietEnabled() { return this.storageRefreshQuietEnabled; }
    public void toggleStorageRefreshQuietEnabled() { this.storageRefreshQuietEnabled = !this.storageRefreshQuietEnabled; persistUiState(); }

    public boolean isStorageAutoRefreshEnabled() { return this.storageAutoRefreshEnabled; }
    public void toggleStorageAutoRefreshEnabled() { this.storageAutoRefreshEnabled = !this.storageAutoRefreshEnabled; persistUiState(); }

    // ======================== GUI 缩放 ========================

    /** 返回当前固定的 RTS GUI 缩放值。 */
    public double fixedRtsGuiScale() {
        return this.fixedRtsGuiScale;
    }

    /**
     * 按给定增量调整 GUI 缩放并立即标记持久化。
     */
    public void adjustRtsGuiScale(double delta) {
        this.fixedRtsGuiScale = sanitizeRtsGuiScale(this.fixedRtsGuiScale + delta);
        persistUiState();
    }

    /**
     * 返回格式化的缩放标签。
     * <p>整数值显示为 "2x"，半值显示为 "2.5x"。
     */
    public String rtsGuiScaleLabel() {
        double scale = sanitizeRtsGuiScale(this.fixedRtsGuiScale);
        if (Math.abs(scale - Math.rint(scale)) < 0.001D) {
            return String.format(Locale.ROOT, "%.0fx", scale);
        }
        return String.format(Locale.ROOT, "%.1fx", scale);
    }

    // ======================== 加载 / 持久化 ========================

    /**
     * 从缓存读取所有 UI 状态并应用到对应的控制器/面板。
     */
    public void applyStoredUiState() {
        RtsClientUiStateStore.UiState state = this.cache.get();
        // 先遍历面板自声明属性（含 BoundsProperty），再遍历控制器绑定
        applyPanelProperties(state);
        for (var bind : this.ctrlBindings) {
            bind.apply.accept(state);
        }
    }

    /**
     * 从当前运行状态收集所有 UI 偏好并写入缓存（标记为脏，不立即写盘）。
     * <p>在状态变更时调用（如面板切换、形状切换、缩放调整）。
     * 实际的 I/O 将在下次 tick 通过 {@link #flush()} 执行。
     */
    public void persistUiState() {
        RtsClientUiStateStore.UiState state = this.cache.get();
        // 遍历控制器绑定（收集运行时值到 UiState）
        for (var bind : this.ctrlBindings) {
            bind.collect.accept(state);
        }
        // 遍历面板自声明属性（收集边界及其他面板专属状态）
        collectPanelProperties(state);
        this.cache.markDirty();
    }

    // ====== 面板自声明属性迭代 ======

    /** 遍历所有注册面板的自声明属性，将 UiState 的值应用到运行时。 */
    private void applyPanelProperties(RtsClientUiStateStore.UiState state) {
        for (RtsWindowPanel panel : this.persistablePanels.values()) {
            for (PersistableProperty prop : panel.persistableProperties()) {
                prop.applyToRuntime(state);
            }
        }
    }

    /** 遍历所有注册面板的自声明属性，将运行时值收集到 UiState。 */
    private void collectPanelProperties(RtsClientUiStateStore.UiState state) {
        for (RtsWindowPanel panel : this.persistablePanels.values()) {
            for (PersistableProperty prop : panel.persistableProperties()) {
                prop.collectFromRuntime(state);
            }
        }
    }

    // ====== 帮助方法（被 CtrlBind lambda 引用） ======

    /**
     * 将存储的灵敏度索引转换为 [0, 1] 分数值并应用到控制器。
     */
    private void applyInputSensitivity(int index) {
        int presetCount = Math.max(1, this.controller.getInputSensitivityPresetCount());
        if (presetCount <= 1) {
            this.controller.setInputSensitivityByFraction(0.0D);
            return;
        }
        int clamped = Mth.clamp(index, 0, presetCount - 1);
        double fraction = (double) clamped / (double) (presetCount - 1);
        this.controller.setInputSensitivityByFraction(fraction);
    }

    /** 尝试解析并设置建造形状字符串。 */
    private void parseAndSetBuildShape(String name) {
        try {
            this.controller.setBuildShape(BuildShape.valueOf(name));
        } catch (IllegalArgumentException ignored) {
            this.controller.setBuildShape(BuildShape.BLOCK);
        }
    }

    /** 尝试解析并设置填充模式字符串。 */
    private void parseAndSetFillMode(String name) {
        try {
            this.shapeController.setShapeFillMode(ShapeFillMode.valueOf(name));
        } catch (IllegalArgumentException ignored) {
            this.shapeController.setShapeFillMode(ShapeFillMode.FILL);
        }
    }

    // ====== 缩放工具方法 ======

    /** 将缩放值限制到合法范围并按配置步长取整。 */
    private static double sanitizeRtsGuiScale(double scale) {
        if (!Double.isFinite(scale)) {
            return DEFAULT_RTS_GUI_SCALE;
        }
        double snapped = Math.round(scale / RTS_GUI_SCALE_STEP) * RTS_GUI_SCALE_STEP;
        return Math.max(MIN_RTS_GUI_SCALE, Math.min(MAX_RTS_GUI_SCALE, snapped));
    }

    // ========================================================================
    //  CtrlBind —— 控制器级别设置的声明式双向绑定
    // ========================================================================

    /**
     * 控制器级别设置的声明式双向绑定。
     * <p>每个绑定封装了从运行时收集值到 UiState 的 {@link #collect} 操作，
     * 以及从 UiState 恢复到运行时的 {@link #apply} 操作。
     * 由 {@link #ctrlBindings} 列表统一管理，取代旧式逐行 hardcode。
     */
    private record CtrlBind(
            Consumer<RtsClientUiStateStore.UiState> collect,
            Consumer<RtsClientUiStateStore.UiState> apply
    ) {
        /**
         * 创建简单的布尔字段绑定。
         *
         * @param runtimeGet  运行时布尔获取器（如 {@code controller::isXxx}）
         * @param runtimeSet  运行时布尔设置器（如 {@code controller::setXxx}）
         * @param stateRead   从 UiState 读取布尔值（如 {@code s -> s.xxx}）
         * @param stateWrite  向 UiState 写入布尔值（如 {@code (s, v) -> s.xxx = v}）
         */
        static CtrlBind bool(
                BooleanSupplier runtimeGet,
                Consumer<Boolean> runtimeSet,
                Function<RtsClientUiStateStore.UiState, Boolean> stateRead,
                BiConsumer<RtsClientUiStateStore.UiState, Boolean> stateWrite
        ) {
            return new CtrlBind(
                    state -> stateWrite.accept(state, runtimeGet.getAsBoolean()),
                    state -> runtimeSet.accept(stateRead.apply(state))
            );
        }
    }
}
