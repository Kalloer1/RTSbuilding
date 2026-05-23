package com.rtsbuilding.rtsbuilding.client;

import com.rtsbuilding.rtsbuilding.progression.RtsProgressionNodes;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class RtsHomeScreen extends Screen {
    private final Screen parent;
    private final ClientRtsController controller = ClientRtsController.get();

    public RtsHomeScreen(Screen parent) {
        super(Component.literal("RTS Home"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.controller.requestProgressionState();
        int panelW = Math.min(320, this.width - 32);
        int x = (this.width - panelW) / 2;
        int y = 112;
        String labelKey = this.controller.isProgressionHomeSet() ? "screen.rtsbuilding.home.change" : "screen.rtsbuilding.home.set";
        Button homeButton = Button.builder(Component.translatable(labelKey), btn -> {
            this.minecraft.setScreen(null);
            this.controller.beginHomeSelection();
        }).bounds(x + 10, y, panelW - 20, 20).build();
        homeButton.active = this.controller.isProgressionEnabled()
                && (!this.controller.isProgressionHomeSet() || canRelocateHome());
        addRenderableWidget(homeButton);
        addRenderableWidget(Button.builder(Component.translatable("gui.rtsbuilding.back"), btn -> this.minecraft.setScreen(this.parent))
                .bounds(x + panelW - 60, this.height - 28, 50, 18)
                .build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        int panelW = Math.min(390, this.width - 32);
        int x = (this.width - panelW) / 2;
        int y = 28;
        g.fill(x, y, x + panelW, y + 166, 0xEE101820);
        g.hLine(x, x + panelW, y, 0xFF6E8799);
        g.hLine(x, x + panelW, y + 166, 0xFF0D1218);
        g.drawString(this.font, Component.translatable("screen.rtsbuilding.home"), x + 10, y + 10, 0xFFFFFF);
        g.drawString(this.font, Component.translatable(this.controller.isProgressionEnabled() ? "screen.rtsbuilding.progression.survival_on" : "screen.rtsbuilding.progression.survival_off"), x + 10, y + 26, 0xCFE3F7);
        if (this.controller.isProgressionHomeSet()) {
            BlockPos pos = this.controller.getProgressionHomePos();
            g.drawString(this.font, Component.translatable("screen.rtsbuilding.home.current", pos.getX(), pos.getY(), pos.getZ()), x + 10, y + 44, 0xEAF2FF);
            g.drawString(this.font, Component.translatable("screen.rtsbuilding.home.dimension", this.controller.getProgressionHomeDimension()), x + 10, y + 58, 0xBFD2E6);
        } else {
            g.drawString(this.font, Component.translatable("screen.rtsbuilding.home.not_set"), x + 10, y + 44, 0xFFE7C46A);
        }
        g.drawString(this.font, Component.translatable("screen.rtsbuilding.home.radius", this.controller.getProgressionRadiusBlocks()), x + 10, y + 76, 0xD8E6F5);
        g.drawString(this.font, Component.translatable(canRelocateHome() ? "screen.rtsbuilding.home.relocation_unlocked" : "screen.rtsbuilding.home.relocation_locked"), x + 10, y + 90, canRelocateHome() ? 0xAEE8AE : 0xFFB0B0);
        drawWrapped(g, Component.translatable("screen.rtsbuilding.home.warning").getString(), x + 10, y + 108, panelW - 20, 0xFFE7C46A);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean canRelocateHome() {
        return this.controller.getUnlockedProgressionNodes().contains(RtsProgressionNodes.FIELD_DEPLOYMENT.toString());
    }

    private void drawWrapped(GuiGraphics g, String text, int x, int y, int width, int color) {
        for (var line : this.font.split(Component.literal(text), width)) {
            g.drawString(this.font, line, x, y, color);
            y += 10;
        }
    }
}
