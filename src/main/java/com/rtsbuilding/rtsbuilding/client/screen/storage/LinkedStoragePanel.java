package com.rtsbuilding.rtsbuilding.client.screen.storage;

import com.rtsbuilding.rtsbuilding.client.controller.ClientRtsController;
import com.rtsbuilding.rtsbuilding.client.screen.BuilderScreen;
import com.rtsbuilding.rtsbuilding.client.screen.panel.RtsWindowPanel;
import com.rtsbuilding.rtsbuilding.client.util.RtsClientUiUtil;
import com.rtsbuilding.rtsbuilding.network.storage.C2SRtsLinkStoragePayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import org.lwjgl.glfw.GLFW;

import java.util.List;

import static com.rtsbuilding.rtsbuilding.client.screen.BuilderScreenConstants.TOP_H;

/**
 * Movable window for inspecting and unlinking RTS storage bindings.
 *
 * <p>This panel is intentionally a thin client-side management view. It owns
 * only the window layout, row hover/click handling, and scroll state. The
 * authoritative list of linked storage blocks comes from the latest server
 * storage-page payload through {@link ClientRtsController}; the server remains
 * responsible for validating and applying an unlink request. Keeping that
 * boundary explicit prevents this UI from becoming a second storage resolver or
 * inventing client-only binding state.
 *
 * <p>The player-facing goal is issue #41: when storage binding looks confusing,
 * the player should be able to open a small Windows-style RTS panel, see each
 * bound block by icon/name/coordinate/mode, and remove a bad binding directly.
 * This deliberately replaces the earlier long-tooltip idea with a real,
 * reviewable window using the same {@link RtsWindowPanel} infrastructure as
 * Quick Build and Ultimine.
 */
public final class LinkedStoragePanel extends RtsWindowPanel {
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 178;
    private static final int ROW_H = 28;
    private static final int HEADER_H = 18;
    private static final int UNLINK_W = 48;
    private static final int UNLINK_H = 16;

    private int scroll;

    public void openNear(int anchorX, int anchorY) {
        setOpen(true);
        int x = Mth.clamp(anchorX, 4, Math.max(4, this.screen.width - PANEL_W - 4));
        int y = Mth.clamp(anchorY, TOP_H + 2, Math.max(TOP_H + 2, this.screen.getBottomY() - PANEL_H - 4));
        setBounds(x, y, PANEL_W, PANEL_H);
        markBroughtToFront();
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        List<ClientRtsController.LinkedStorageEntry> entries = this.controller.getLinkedStorageEntries();
        this.scroll = Mth.clamp(this.scroll, 0, maxScroll(entries));

        int x = contentX() + 8;
        int y = contentY() + 8;
        int w = contentWidth() - 16;
        g.drawString(this.screen.font(), Component.translatable("screen.rtsbuilding.storage_links.header"),
                x, y, 0xFFD8E3EE, false);

        if (entries.isEmpty()) {
            int emptyY = y + HEADER_H + 12;
            g.drawString(this.screen.font(), Component.translatable("screen.rtsbuilding.storage_links.empty"),
                    x, emptyY, 0xFFFFD480, false);
            g.drawString(this.screen.font(),
                    RtsClientUiUtil.trimToWidth(this.screen.font(),
                            Component.translatable("screen.rtsbuilding.storage_links.empty_detail").getString(), w),
                    x, emptyY + 12, 0xFFBFD0E0, false);
            return;
        }

        int firstY = y + HEADER_H;
        int visibleRows = visibleRows();
        int end = Math.min(entries.size(), this.scroll + visibleRows);
        for (int i = this.scroll; i < end; i++) {
            int rowY = firstY + (i - this.scroll) * ROW_H;
            renderRow(g, mouseX, mouseY, entries.get(i), x, rowY, w);
        }
        renderScrollbar(g, entries.size(), x + w - 4, firstY, visibleRows * ROW_H);
    }

    private void renderRow(GuiGraphics g, int mouseX, int mouseY, ClientRtsController.LinkedStorageEntry entry,
            int x, int y, int w) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + ROW_H - 2;
        int fill = hovered ? 0xCC243244 : 0xAA1A222D;
        RtsClientUiUtil.drawPanelFrame(g, x, y, w, ROW_H - 2, fill, 0xFF566D83, 0xFF0D1117);

        ItemStack preview = entry.preview();
        if (preview != null && !preview.isEmpty()) {
            g.renderItem(preview, x + 5, y + 5);
        } else {
            g.fill(x + 5, y + 5, x + 21, y + 21, 0xAA101820);
            g.hLine(x + 5, x + 21, y + 5, 0xFF566D83);
            g.vLine(x + 5, y + 5, y + 21, 0xFF566D83);
        }

        String name = RtsClientUiUtil.trimToWidth(this.screen.font(), entry.label(), Math.max(30, w - 152));
        g.drawString(this.screen.font(), name, x + 26, y + 4, 0xFFEAF2FF, false);
        g.drawString(this.screen.font(), formatPos(entry.pos()), x + 26, y + 15, 0xFF9FB3C8, false);

        String mode = Component.translatable(entry.mode() == C2SRtsLinkStoragePayload.MODE_EXTRACT_ONLY
                ? "screen.rtsbuilding.storage_links.mode_extract"
                : "screen.rtsbuilding.storage_links.mode_both").getString();
        int modeX = x + w - UNLINK_W - 60;
        g.drawString(this.screen.font(), RtsClientUiUtil.trimToWidth(this.screen.font(), mode, 54),
                modeX, y + 10, 0xFFCDE7D2, false);

        int buttonX = unlinkButtonX(x, w);
        int buttonY = unlinkButtonY(y);
        boolean buttonHover = inside(mouseX, mouseY, buttonX, buttonY, UNLINK_W, UNLINK_H);
        RtsClientUiUtil.drawPanelFrame(g, buttonX, buttonY, UNLINK_W, UNLINK_H,
                buttonHover ? 0xCC5A2B34 : 0xAA2A2228,
                buttonHover ? 0xFFE28A96 : 0xFF7B5660,
                0xFF180B0E);
        g.drawCenteredString(this.screen.font(), Component.translatable("screen.rtsbuilding.storage_links.unlink"),
                buttonX + UNLINK_W / 2, buttonY + 4, 0xFFFFF0F0);
    }

    @Override
    protected void handleContentClick(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        List<ClientRtsController.LinkedStorageEntry> entries = this.controller.getLinkedStorageEntries();
        if (entries.isEmpty()) {
            return;
        }
        int x = contentX() + 8;
        int w = contentWidth() - 16;
        int firstY = contentY() + 8 + HEADER_H;
        int row = (int) ((mouseY - firstY) / ROW_H);
        if (row < 0 || row >= visibleRows()) {
            return;
        }
        int index = this.scroll + row;
        if (index < 0 || index >= entries.size()) {
            return;
        }
        int rowY = firstY + row * ROW_H;
        if (inside(mouseX, mouseY, unlinkButtonX(x, w), unlinkButtonY(rowY), UNLINK_W, UNLINK_H)) {
            BlockPos pos = entries.get(index).pos();
            this.controller.unlinkLinkedStorage(pos);
        }
    }

    @Override
    protected boolean handleContentScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        int delta = scrollY > 0.0D ? -1 : 1;
        this.scroll = Mth.clamp(this.scroll + delta, 0, maxScroll(this.controller.getLinkedStorageEntries()));
        return true;
    }

    @Override
    protected Component getTitle() {
        return Component.translatable("screen.rtsbuilding.storage_links.title");
    }

    @Override
    protected int getDefaultWidth() {
        return PANEL_W;
    }

    @Override
    protected int getDefaultHeight() {
        return PANEL_H;
    }

    @Override
    protected int getMinWindowWidth() {
        return PANEL_W;
    }

    @Override
    protected int getMinWindowHeight() {
        return PANEL_H;
    }

    @Override
    protected void computeDefaultPosition() {
        this.windowX = 8;
        this.windowY = TOP_H + 6;
    }

    private int visibleRows() {
        return Math.max(1, (contentHeight() - HEADER_H - 16) / ROW_H);
    }

    private int maxScroll(List<ClientRtsController.LinkedStorageEntry> entries) {
        return Math.max(0, entries.size() - visibleRows());
    }

    private static int unlinkButtonX(int rowX, int rowW) {
        return rowX + rowW - UNLINK_W - 6;
    }

    private static int unlinkButtonY(int rowY) {
        return rowY + 5;
    }

    private static String formatPos(BlockPos pos) {
        if (pos == null) {
            return "? ? ?";
        }
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private void renderScrollbar(GuiGraphics g, int totalRows, int x, int y, int h) {
        int maxScroll = maxScroll(this.controller.getLinkedStorageEntries());
        if (maxScroll <= 0) {
            return;
        }
        g.fill(x, y, x + 3, y + h, 0xAA101820);
        int thumbH = Math.max(10, h * visibleRows() / Math.max(1, totalRows));
        int thumbY = y + (h - thumbH) * this.scroll / maxScroll;
        g.fill(x, thumbY, x + 3, thumbY + thumbH, 0xFF8EA9C4);
    }
}
