package com.rtsbuilding.rtsbuilding.forgecompat.registry;

import java.util.Iterator;
import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public final class BuiltInRegistries {
    public static final Registry<Item> ITEM = Registry.ITEM;
    public static final Registry<Block> BLOCK = Registry.BLOCK;
    public static final Registry<Fluid> FLUID = Registry.FLUID;
    public static final CreativeTabRegistry CREATIVE_MODE_TAB = new CreativeTabRegistry();

    private BuiltInRegistries() {
    }

    public static final class CreativeTabRegistry implements Iterable<CreativeModeTab> {
        private static final List<CreativeModeTab> TABS = List.of(CreativeModeTab.TABS);

        @Override
        public Iterator<CreativeModeTab> iterator() {
            return TABS.iterator();
        }

        public ResourceLocation getKey(CreativeModeTab tab) {
            int index = TABS.indexOf(tab);
            if (index < 0) {
                return null;
            }
            String label = tab.getRecipeFolderName();
            if (label == null || label.isBlank()) {
                label = "tab_" + index;
            }
            return new ResourceLocation("minecraft", sanitize(label));
        }

        private static String sanitize(String value) {
            StringBuilder out = new StringBuilder(value.length());
            for (int i = 0; i < value.length(); i++) {
                char c = Character.toLowerCase(value.charAt(i));
                out.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '/' || c == '.'
                        ? c
                        : '_');
            }
            return out.toString();
        }
    }
}
