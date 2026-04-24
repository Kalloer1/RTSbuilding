package com.rtsbuilding.rtsbuilding.forgecompat.client;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class GuiGraphics extends GuiComponent {
    private final Minecraft minecraft;
    private final PoseStack pose;

    public GuiGraphics(Minecraft minecraft, PoseStack pose) {
        this.minecraft = minecraft == null ? Minecraft.getInstance() : minecraft;
        this.pose = pose;
    }

    public PoseStack pose() {
        return this.pose;
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        GuiComponent.fill(this.pose, minX, minY, maxX, maxY, color);
    }

    public void hLine(int minX, int maxX, int y, int color) {
        super.hLine(this.pose, minX, maxX, y, color);
    }

    public void vLine(int x, int minY, int maxY, int color) {
        super.vLine(this.pose, x, minY, maxY, color);
    }

    public int drawString(Font font, String text, int x, int y, int color) {
        return this.drawString(font, text, x, y, color, true);
    }

    public int drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        if (text == null) {
            return 0;
        }
        return shadow ? font.drawShadow(this.pose, text, x, y, color) : font.draw(this.pose, text, x, y, color);
    }

    public int drawString(Font font, Component text, int x, int y, int color) {
        return this.drawString(font, text, x, y, color, true);
    }

    public int drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (text == null) {
            return 0;
        }
        return shadow ? font.drawShadow(this.pose, text, x, y, color) : font.draw(this.pose, text, x, y, color);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        this.drawString(font, text, x - font.width(text) / 2, y, color);
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        this.drawString(font, text, x - font.width(text) / 2, y, color);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        this.minecraft.getItemRenderer().renderAndDecorateItem(stack, x, y);
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        this.minecraft.getItemRenderer().renderGuiItemDecorations(font, stack, x, y);
    }

    public void renderTooltip(Font font, ItemStack stack, int x, int y) {
        // 1.19.2 keeps Screen's ItemStack tooltip renderer protected.
    }

    public void renderTooltip(Font font, Component text, int x, int y) {
        Screen screen = this.minecraft.screen;
        if (screen != null) {
            screen.renderComponentTooltip(this.pose, List.of(text), x, y);
        }
    }

    public void blit(ResourceLocation texture, int x, int y, int uOffset, int vOffset, int width, int height) {
        RenderSystem.setShaderTexture(0, texture);
        super.blit(this.pose, x, y, uOffset, vOffset, width, height);
    }
}
