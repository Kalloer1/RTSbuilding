package com.rtsbuilding.rtsbuilding.server.storage.category;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义物品分类配置管理器。
 * <p>
 * 允许玩家通过配置文件自定义物品分类规则。配置文件位于
 * {@code config/rtsbuilding/item_categories.json}。
 * <p>
 * 每个分类可以包含：
 * <ul>
 *   <li>物品ID列表（精确匹配）</li>
 *   <li>模组命名空间（匹配该模组的所有物品）</li>
 *   <li>物品标签（匹配具有该标签的所有物品）</li>
 * </ul>
 */
public final class CustomCategoryManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("rtsbuilding").resolve("item_categories.json");

    private static final Map<String, CustomCategory> categories = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> itemToCategoryCache = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    private CustomCategoryManager() {}

    /**
     * 加载自定义分类配置。如果配置文件不存在，则创建默认配置。
     */
    public static synchronized void loadCategories() {
        categories.clear();
        itemToCategoryCache.clear();

        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                Type listType = new TypeToken<List<CustomCategory>>() {}.getType();
                List<CustomCategory> loaded = GSON.fromJson(json, listType);
                if (loaded != null) {
                    for (CustomCategory category : loaded) {
                        categories.put(category.id(), category);
                    }
                }
            } else {
                createDefaultConfig();
            }
        } catch (IOException e) {
            RtsbuildingMod.LOGGER.warn("Failed to load custom item categories, using defaults", e);
            createDefaultConfig();
        }

        rebuildItemCache();
        loaded = true;
    }

    /**
     * 保存当前分类配置到文件。
     */
    public static synchronized void saveCategories() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            List<CustomCategory> categoryList = new ArrayList<>(categories.values());
            categoryList.sort(Comparator.comparing(CustomCategory::order));
            String json = GSON.toJson(categoryList);
            Files.writeString(CONFIG_PATH, json);
        } catch (IOException e) {
            RtsbuildingMod.LOGGER.error("Failed to save custom item categories", e);
        }
    }

    /**
     * 获取所有自定义分类。
     */
    public static Collection<CustomCategory> getCategories() {
        if (!loaded) {
            loadCategories();
        }
        return Collections.unmodifiableCollection(categories.values());
    }

    /**
     * 根据ID获取分类。
     */
    public static CustomCategory getCategory(String id) {
        if (!loaded) {
            loadCategories();
        }
        return categories.get(id);
    }

    /**
     * 检查物品是否属于指定分类。
     */
    public static boolean isItemInCategory(String itemId, String categoryId) {
        if (!loaded) {
            loadCategories();
        }
        Set<String> catIds = itemToCategoryCache.get(itemId);
        return catIds != null && catIds.contains(categoryId);
    }

    /**
     * 获取物品所属的所有自定义分类ID。
     */
    public static Set<String> getCategoriesForItem(String itemId) {
        if (!loaded) {
            loadCategories();
        }
        Set<String> catIds = itemToCategoryCache.get(itemId);
        return catIds != null ? Collections.unmodifiableSet(catIds) : Collections.emptySet();
    }

    /**
     * 添加或更新一个分类。
     */
    public static void putCategory(CustomCategory category) {
        categories.put(category.id(), category);
        rebuildItemCache();
        saveCategories();
    }

    /**
     * 删除指定ID的分类。
     */
    public static void removeCategory(String id) {
        if (categories.remove(id) != null) {
            rebuildItemCache();
            saveCategories();
        }
    }

    /**
     * 重新加载配置文件。
     */
    public static void reload() {
        loadCategories();
    }

    // ---- Internal helpers ----

    private static void createDefaultConfig() {
        categories.put("building_blocks", new CustomCategory(
                "building_blocks",
                "Building Blocks",
                0,
                List.of(),
                List.of("minecraft"),
                List.of("minecraft:stone", "minecraft:cobblestone", "minecraft:stone_bricks"),
                List.of("minecraft:wooden_door", "minecraft:iron_door")
        ));
        categories.put("tools", new CustomCategory(
                "tools",
                "Tools",
                1,
                List.of(),
                List.of(),
                List.of("minecraft:iron_pickaxe", "minecraft:iron_shovel", "minecraft:iron_axe"),
                List.of()
        ));
        categories.put("food", new CustomCategory(
                "food",
                "Food",
                2,
                List.of("minecraft:food"),
                List.of(),
                List.of(),
                List.of()
        ));
        saveCategories();
    }

    private static void rebuildItemCache() {
        itemToCategoryCache.clear();

        for (CustomCategory category : categories.values()) {
            Set<String> matchedItems = new HashSet<>();

            for (String itemId : category.itemIds()) {
                matchedItems.add(itemId);
            }

            for (String namespace : category.modNamespaces()) {
                for (Item item : BuiltInRegistries.ITEM) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    if (id != null && id.getNamespace().equals(namespace)) {
                        matchedItems.add(id.toString());
                    }
                }
            }

            for (String tagId : category.itemTags()) {
                ResourceLocation tagRl = ResourceLocation.tryParse(tagId);
                if (tagRl != null) {
                    var tagKey = net.minecraft.tags.ItemTags.create(tagRl);
                    for (Item item : BuiltInRegistries.ITEM) {
                        if (item.builtInRegistryHolder().is(tagKey)) {
                            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                            if (id != null) {
                                matchedItems.add(id.toString());
                            }
                        }
                    }
                }
            }

            for (String pattern : category.itemPatterns()) {
                for (Item item : BuiltInRegistries.ITEM) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    if (id != null && id.getPath().matches(pattern)) {
                        matchedItems.add(id.toString());
                    }
                }
            }

            for (String itemId : matchedItems) {
                itemToCategoryCache.computeIfAbsent(itemId, k -> new HashSet<>()).add(category.id());
            }
        }
    }

    /**
     * 自定义分类记录。
     *
     * @param id           分类的唯一标识符
     * @param name         显示名称
     * @param order        排序顺序
     * @param itemTags     物品标签列表（如 "minecraft:food"）
     * @param modNamespaces 模组命名空间列表（如 "minecraft"）
     * @param itemIds      具体物品ID列表
     * @param itemPatterns 物品ID路径的正则表达式模式列表
     */
    public record CustomCategory(
            String id,
            String name,
            int order,
            List<String> itemTags,
            List<String> modNamespaces,
            List<String> itemIds,
            List<String> itemPatterns
    ) {}
}
