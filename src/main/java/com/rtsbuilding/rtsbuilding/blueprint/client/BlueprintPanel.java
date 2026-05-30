package com.rtsbuilding.rtsbuilding.blueprint.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.rtsbuilding.rtsbuilding.Config;
import com.rtsbuilding.rtsbuilding.blueprint.BlueprintFormat;
import com.rtsbuilding.rtsbuilding.blueprint.BlueprintParseException;
import com.rtsbuilding.rtsbuilding.blueprint.RtsBlueprint;
import com.rtsbuilding.rtsbuilding.blueprint.format.BlueprintReaders;
import com.rtsbuilding.rtsbuilding.blueprint.network.C2SBlueprintPlacePayload;
import com.rtsbuilding.rtsbuilding.blueprint.network.S2CBlueprintStatusPayload;
import com.rtsbuilding.rtsbuilding.client.ClientRtsController;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BlueprintPanel {
    private static final int ROW_H = 24;
    private static final int BUTTON_H = 14;
    private static final int SEARCH_H = 14;
    private static final List<BlueprintEntry> ENTRIES = new ArrayList<>();
    private static boolean loaded = false;
    private static int selectedIndex = -1;
    private static int scroll = 0;
    private static boolean searchFocused = false;
    private static String search = "";
    private static Component statusText = Component.translatable("screen.rtsbuilding.blueprints.status.ready");
    private static int statusColor = 0xFFB8C7D6;

    private BlueprintPanel() {
    }

    public static void render(GuiGraphics g, Font font, ClientRtsController controller,
            int x, int y, int w, int h, int mouseX, int mouseY) {
        if (!Config.areBlueprintsEnabled()) {
            renderDisabled(g, font, x, y, w, h);
            return;
        }
        ensureLoaded();

        int buttonY = y;
        int openW = 74;
        int refreshW = 58;
        drawButton(g, font, x, buttonY, openW, BUTTON_H, text("screen.rtsbuilding.blueprints.open_folder"),
                inside(mouseX, mouseY, x, buttonY, openW, BUTTON_H));
        drawButton(g, font, x + openW + 4, buttonY, refreshW, BUTTON_H, text("screen.rtsbuilding.blueprints.refresh"),
                inside(mouseX, mouseY, x + openW + 4, buttonY, refreshW, BUTTON_H));

        int searchX = x + openW + refreshW + 12;
        int searchW = Math.max(80, Math.min(240, w - (searchX - x) - 4));
        drawFrame(g, searchX, buttonY, searchW, SEARCH_H, searchFocused ? 0xCC09111B : 0xAA111820, 0xFF6B8095, 0xFF0C1118);
        String searchLabel = search.isBlank() && !searchFocused
                ? text("screen.rtsbuilding.blueprints.search")
                : search + (searchFocused && (Util.getMillis() / 500L) % 2L == 0L ? "_" : "");
        g.drawString(font, trim(font, searchLabel, searchW - 8), searchX + 4, buttonY + 3,
                search.isBlank() && !searchFocused ? 0x8898A8B8 : 0xFFEAF2FF, false);

        int listY = y + 19;
        int statusY = y + h - 13;
        int listH = Math.max(24, statusY - listY - 4);
        int detailsW = Math.min(184, Math.max(126, w / 4));
        int listW = Math.max(120, w - detailsW - 8);
        renderList(g, font, controller, x, listY, listW, listH, mouseX, mouseY);
        renderDetails(g, font, controller, x + listW + 8, listY, detailsW, listH, mouseX, mouseY);
        g.drawString(font, trim(font, statusText.getString(), w - 8), x + 2, statusY, statusColor, false);
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int x, int y, int w, int h) {
        if (!Config.areBlueprintsEnabled()) {
            searchFocused = false;
            setStatus(S2CBlueprintStatusPayload.ERROR, "screen.rtsbuilding.blueprints.status.disabled", "");
            return true;
        }
        ensureLoaded();
        int openW = 74;
        int refreshW = 58;
        if (inside(mouseX, mouseY, x, y, openW, BUTTON_H)) {
            openBlueprintFolder();
            return true;
        }
        if (inside(mouseX, mouseY, x + openW + 4, y, refreshW, BUTTON_H)) {
            reload();
            setStatus(S2CBlueprintStatusPayload.INFO, "screen.rtsbuilding.blueprints.status.reloaded", "");
            return true;
        }

        int searchX = x + openW + refreshW + 12;
        int searchW = Math.max(80, Math.min(240, w - (searchX - x) - 4));
        searchFocused = inside(mouseX, mouseY, searchX, y, searchW, SEARCH_H);
        if (searchFocused) {
            return true;
        }

        int listY = y + 19;
        int statusY = y + h - 13;
        int listH = Math.max(24, statusY - listY - 4);
        int detailsW = Math.min(184, Math.max(126, w / 4));
        int listW = Math.max(120, w - detailsW - 8);
        if (inside(mouseX, mouseY, x, listY, listW, listH)) {
            List<BlueprintEntry> filtered = filteredEntries();
            int visible = Math.max(1, listH / ROW_H);
            scroll = Mth.clamp(scroll, 0, Math.max(0, filtered.size() - visible));
            int row = ((int) mouseY - listY) / ROW_H;
            int index = scroll + row;
            if (index >= 0 && index < filtered.size()) {
                BlueprintEntry entry = filtered.get(index);
                selectedIndex = ENTRIES.indexOf(entry);
                setStatus(
                        entry.error().isBlank() ? S2CBlueprintStatusPayload.INFO : S2CBlueprintStatusPayload.ERROR,
                        entry.error().isBlank()
                                ? "screen.rtsbuilding.blueprints.status.selected"
                                : "screen.rtsbuilding.blueprints.status.parse_failed",
                        entry.error().isBlank() ? entry.name() : entry.error());
            }
            return true;
        }
        return false;
    }

    public static boolean mouseScrolled(double mouseX, double mouseY, double scrollY, int x, int y, int w, int h) {
        if (!Config.areBlueprintsEnabled()) {
            return false;
        }
        int listY = y + 19;
        int statusY = y + h - 13;
        int listH = Math.max(24, statusY - listY - 4);
        int detailsW = Math.min(184, Math.max(126, w / 4));
        int listW = Math.max(120, w - detailsW - 8);
        if (!inside(mouseX, mouseY, x, listY, listW, listH)) {
            return false;
        }
        List<BlueprintEntry> filtered = filteredEntries();
        int visible = Math.max(1, listH / ROW_H);
        int maxScroll = Math.max(0, filtered.size() - visible);
        scroll = Mth.clamp(scroll + (scrollY > 0.0D ? -1 : 1), 0, maxScroll);
        return true;
    }

    public static boolean keyPressed(int keyCode) {
        if (!Config.areBlueprintsEnabled()) {
            searchFocused = false;
            return false;
        }
        if (!searchFocused) {
            return false;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            if (!search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                scroll = 0;
            }
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
            searchFocused = false;
            return true;
        }
        return false;
    }

    public static boolean charTyped(char codePoint) {
        if (!Config.areBlueprintsEnabled()) {
            return false;
        }
        if (!searchFocused || Character.isISOControl(codePoint)) {
            return false;
        }
        if (search.length() < 96) {
            search += codePoint;
            scroll = 0;
        }
        return true;
    }

    public static boolean hasSelectedBlueprint() {
        if (!Config.areBlueprintsEnabled()) {
            return false;
        }
        BlueprintEntry entry = selectedEntry();
        return entry != null && entry.error().isBlank();
    }

    public static boolean placeSelected(BlockPos anchor, int rotationSteps) {
        if (!Config.areBlueprintsEnabled()) {
            setStatus(S2CBlueprintStatusPayload.ERROR, "screen.rtsbuilding.blueprints.status.disabled", "");
            return true;
        }
        BlueprintEntry entry = selectedEntry();
        if (entry == null || !entry.error().isBlank()) {
            setStatus(S2CBlueprintStatusPayload.ERROR, "screen.rtsbuilding.blueprints.status.no_selection", "");
            return false;
        }
        try {
            byte[] data = Files.readAllBytes(entry.path());
            if (data.length > C2SBlueprintPlacePayload.MAX_FILE_BYTES) {
                setStatus(S2CBlueprintStatusPayload.ERROR, "screen.rtsbuilding.blueprints.status.too_large", "");
                return true;
            }
            PacketDistributor.sendToServer(new C2SBlueprintPlacePayload(
                    entry.fileName(),
                    data,
                    anchor,
                    (byte) Math.floorMod(rotationSteps, 4)));
            setStatus(S2CBlueprintStatusPayload.INFO, "screen.rtsbuilding.blueprints.status.uploading", entry.name());
            return true;
        } catch (IOException ex) {
            setStatus(S2CBlueprintStatusPayload.ERROR, "screen.rtsbuilding.blueprints.status.read_failed", ex.getMessage());
            return true;
        }
    }

    public static void setStatus(byte status, String messageKey, String detail) {
        Component base = detail == null || detail.isBlank()
                ? Component.translatable(messageKey)
                : Component.translatable(messageKey, detail);
        statusText = base;
        statusColor = switch (status) {
            case S2CBlueprintStatusPayload.SUCCESS -> 0xFF81E58E;
            case S2CBlueprintStatusPayload.ERROR -> 0xFFFF8A8A;
            default -> 0xFFB8C7D6;
        };
    }

    private static void renderDisabled(GuiGraphics g, Font font, int x, int y, int w, int h) {
        drawFrame(g, x, y, w, h, 0x8811161E, 0xFF415266, 0xFF0B0E13);
        Component title = Component.translatable("screen.rtsbuilding.blueprints.disabled");
        Component detail = Component.translatable("screen.rtsbuilding.blueprints.status.disabled");
        g.drawString(font, trim(font, title.getString(), w - 12), x + 6, y + 8, 0xFFEAF2FF, false);
        g.drawString(font, trim(font, detail.getString(), w - 12), x + 6, y + 22, 0xFF9EACB9, false);
    }

    private static void renderList(GuiGraphics g, Font font, ClientRtsController controller,
            int x, int y, int w, int h, int mouseX, int mouseY) {
        drawFrame(g, x, y, w, h, 0x8811161E, 0xFF415266, 0xFF0B0E13);
        List<BlueprintEntry> filtered = filteredEntries();
        int visible = Math.max(1, h / ROW_H);
        scroll = Mth.clamp(scroll, 0, Math.max(0, filtered.size() - visible));
        if (filtered.isEmpty()) {
            Component empty = ENTRIES.isEmpty()
                    ? Component.translatable("screen.rtsbuilding.blueprints.empty")
                    : Component.translatable("screen.rtsbuilding.blueprints.no_results");
            g.drawString(font, trim(font, empty.getString(), w - 12), x + 6, y + 8, 0xFF9EACB9, false);
            return;
        }
        for (int row = 0; row < visible; row++) {
            int index = scroll + row;
            if (index >= filtered.size()) {
                break;
            }
            BlueprintEntry entry = filtered.get(index);
            int rowY = y + row * ROW_H;
            boolean selected = selectedIndex >= 0 && selectedIndex < ENTRIES.size() && ENTRIES.get(selectedIndex) == entry;
            boolean hover = inside(mouseX, mouseY, x, rowY, w, ROW_H);
            boolean enough = hasEnoughMaterials(entry, controller);
            int bg = selected ? 0xCC2E654B : hover ? 0xAA2B3542 : enough ? 0x77253832 : 0x7731363E;
            if (!entry.error().isBlank()) {
                bg = selected ? 0xCC694238 : 0x77503A36;
            }
            g.fill(x + 1, rowY + 1, x + w - 1, rowY + ROW_H - 1, bg);
            g.drawString(font, trim(font, entry.name(), w - 84), x + 6, rowY + 4,
                    entry.error().isBlank() ? 0xFFEAF2FF : 0xFFFFB0A0, false);
            g.drawString(font, entry.blockCount() + " blocks", x + w - 76, rowY + 4,
                    enough ? 0xFF9BE6A5 : 0xFF9CA6B2, false);
            g.drawString(font, entry.sizeText(), x + 6, rowY + 14, 0xFF8FA2B7, false);
        }
    }

    private static void renderDetails(GuiGraphics g, Font font, ClientRtsController controller,
            int x, int y, int w, int h, int mouseX, int mouseY) {
        drawFrame(g, x, y, w, h, 0x8811161E, 0xFF415266, 0xFF0B0E13);
        BlueprintEntry entry = selectedEntry();
        if (entry == null) {
            g.drawString(font, trim(font, text("screen.rtsbuilding.blueprints.select_hint"), w - 12), x + 6, y + 8,
                    0xFF9EACB9, false);
            return;
        }
        g.drawString(font, trim(font, entry.name(), w - 12), x + 6, y + 6, 0xFFEAF2FF, false);
        g.drawString(font, entry.format().extension().toUpperCase(Locale.ROOT) + "  " + entry.sizeText(), x + 6, y + 18,
                0xFF9EACB9, false);
        String materialLine = hasEnoughMaterials(entry, controller)
                ? text("screen.rtsbuilding.blueprints.materials_ready")
                : text("screen.rtsbuilding.blueprints.materials_missing");
        g.drawString(font, trim(font, materialLine, w - 12), x + 6, y + 31,
                hasEnoughMaterials(entry, controller) ? 0xFF8EEA9B : 0xFF9CA6B2, false);
        int previewY = y + 48;
        int previewX = x + 6;
        for (int i = 0; i < entry.previewItems().size() && i < 18; i++) {
            int px = previewX + (i % 6) * 20;
            int py = previewY + (i / 6) * 20;
            if (py + 18 > y + h - 4) {
                break;
            }
            g.fill(px, py, px + 18, py + 18, 0xAA1A2029);
            g.renderItem(entry.previewItems().get(i), px + 1, py + 1);
        }
        if (!entry.error().isBlank()) {
            g.drawString(font, trim(font, entry.error(), w - 12), x + 6, y + h - 16, 0xFFFFA0A0, false);
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            reload();
        }
    }

    public static void reload() {
        loaded = true;
        ENTRIES.clear();
        selectedIndex = -1;
        scroll = 0;
        Path folder = blueprintFolder();
        try {
            Files.createDirectories(folder);
            try (var stream = Files.list(folder)) {
                stream.filter(Files::isRegularFile)
                        .filter(BlueprintPanel::isBlueprintFile)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                        .limit(512)
                        .forEach(BlueprintPanel::addEntry);
            }
        } catch (IOException ex) {
            setStatus(S2CBlueprintStatusPayload.ERROR, "screen.rtsbuilding.blueprints.status.folder_failed", ex.getMessage());
        }
    }

    private static void addEntry(Path path) {
        String fileName = path.getFileName().toString();
        try {
            byte[] data = Files.readAllBytes(path);
            RtsBlueprint blueprint = BlueprintReaders.parse(data, fileName, Minecraft.getInstance().level.registryAccess());
            ENTRIES.add(BlueprintEntry.from(path, fileName, blueprint, ""));
        } catch (Exception ex) {
            ENTRIES.add(BlueprintEntry.error(path, fileName, ex.getMessage()));
        }
    }

    private static boolean isBlueprintFile(Path path) {
        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return lower.endsWith(".nbt") || lower.endsWith(".schem") || lower.endsWith(".schematic");
    }

    private static void openBlueprintFolder() {
        Path folder = blueprintFolder();
        try {
            Files.createDirectories(folder);
            Util.getPlatform().openFile(folder.toFile());
            setStatus(S2CBlueprintStatusPayload.INFO, "screen.rtsbuilding.blueprints.status.folder_opened", "");
        } catch (Exception ex) {
            setStatus(S2CBlueprintStatusPayload.ERROR, "screen.rtsbuilding.blueprints.status.folder_failed", ex.getMessage());
        }
    }

    private static Path blueprintFolder() {
        return FMLPaths.GAMEDIR.get().resolve("rtsbuilding-blueprints");
    }

    private static List<BlueprintEntry> filteredEntries() {
        if (search == null || search.isBlank()) {
            return List.copyOf(ENTRIES);
        }
        String query = search.toLowerCase(Locale.ROOT).trim();
        return ENTRIES.stream()
                .filter(entry -> entry.name().toLowerCase(Locale.ROOT).contains(query)
                        || entry.fileName().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private static BlueprintEntry selectedEntry() {
        return selectedIndex >= 0 && selectedIndex < ENTRIES.size() ? ENTRIES.get(selectedIndex) : null;
    }

    private static boolean hasEnoughMaterials(BlueprintEntry entry, ClientRtsController controller) {
        if (entry == null || !entry.error().isBlank() || controller == null) {
            return false;
        }
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative()) {
            return true;
        }
        for (Map.Entry<ResourceLocation, Integer> material : entry.requiredItems().entrySet()) {
            if (controller.getStorageTotalCount(material.getKey().toString()) < material.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static void drawButton(GuiGraphics g, Font font, int x, int y, int w, int h, String label, boolean hover) {
        drawFrame(g, x, y, w, h, hover ? 0xCC334052 : 0xAA24303C, 0xFF64788E, 0xFF0D1015);
        g.drawCenteredString(font, trim(font, label, w - 6), x + w / 2, y + 3, 0xFFEAF2FF);
    }

    private static void drawFrame(GuiGraphics g, int x, int y, int w, int h, int fill, int light, int dark) {
        g.fill(x, y, x + w, y + h, fill);
        g.hLine(x, x + w, y, light);
        g.hLine(x, x + w, y + h, dark);
        g.vLine(x, y, y + h, light);
        g.vLine(x + w, y, y + h, dark);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    private static String trim(Font font, String text, int maxWidth) {
        if (font == null || text == null || font.width(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        String ellipsis = "...";
        int limit = Math.max(0, maxWidth - font.width(ellipsis));
        int cut = text.length();
        while (cut > 0 && font.width(text.substring(0, cut)) > limit) {
            cut--;
        }
        return text.substring(0, cut) + ellipsis;
    }

    private record BlueprintEntry(
            Path path,
            String fileName,
            String name,
            BlueprintFormat format,
            String sizeText,
            int blockCount,
            Map<ResourceLocation, Integer> requiredItems,
            List<ItemStack> previewItems,
            String error) {
        static BlueprintEntry from(Path path, String fileName, RtsBlueprint blueprint, String error) {
            Vec3i size = blueprint.size();
            List<ItemStack> preview = new ArrayList<>();
            for (ResourceLocation id : blueprint.requiredItems().keySet()) {
                if (!BuiltInRegistries.ITEM.containsKey(id)) {
                    continue;
                }
                Item item = BuiltInRegistries.ITEM.get(id);
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty()) {
                    preview.add(stack);
                }
                if (preview.size() >= 18) {
                    break;
                }
            }
            String sizeText = size.getX() + "x" + size.getY() + "x" + size.getZ();
            return new BlueprintEntry(
                    path,
                    fileName,
                    blueprint.name(),
                    blueprint.format(),
                    sizeText,
                    blueprint.blockCount(),
                    blueprint.requiredItems(),
                    List.copyOf(preview),
                    error == null ? "" : error);
        }

        static BlueprintEntry error(Path path, String fileName, String error) {
            String name = fileName;
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                name = name.substring(0, dot);
            }
            return new BlueprintEntry(
                    path,
                    fileName,
                    name,
                    BlueprintFormat.fromFileName(fileName),
                    "-",
                    0,
                    Map.of(),
                    List.of(),
                    error == null ? "Parse failed" : error);
        }
    }
}
