package com.rtsbuilding.rtsbuilding;


import com.mojang.logging.LogUtils;
import com.rtsbuilding.rtsbuilding.entity.RtsCameraEntity;
import com.rtsbuilding.rtsbuilding.server.api.impl.RtsAPIImpl;
import com.rtsbuilding.rtsbuilding.server.camera.RtsCameraManager;
import com.rtsbuilding.rtsbuilding.server.feedback.RtsDamageFeedbackManager;
import com.rtsbuilding.rtsbuilding.server.history.ServerHistoryManager;
import com.rtsbuilding.rtsbuilding.server.pipeline.core.RtsPipelineRegistration;
import com.rtsbuilding.rtsbuilding.server.plugin.RtsPluginItem;
import com.rtsbuilding.rtsbuilding.server.plugin.RtsPluginService;
import com.rtsbuilding.rtsbuilding.server.progression.RtsProgressionManager;
import com.rtsbuilding.rtsbuilding.server.service.RtsStorageTickService;
import com.rtsbuilding.rtsbuilding.server.service.ServerTickOrchestrator;
import com.rtsbuilding.rtsbuilding.server.service.ServiceRegistry;
import com.rtsbuilding.rtsbuilding.server.workflow.core.RtsWorkflowEngine;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(RtsbuildingMod.MODID)
public class RtsbuildingMod {
    public static final String MODID = "rtsbuilding";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<RtsCameraEntity>> RTS_CAMERA_ENTITY = ENTITY_TYPES.register(
            "rts_camera",
            () -> EntityType.Builder.<RtsCameraEntity>of(RtsCameraEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "rts_camera").toString()));

    public static final DeferredHolder<Item, Item> RTS_CONTROL_CORE = pluginItem("rts_control_core");
    public static final DeferredHolder<Item, Item> REMOTE_CONTROL_PLUGIN = pluginItem("remote_control_plugin");
    public static final DeferredHolder<Item, Item> STORAGE_INTEGRATION_PLUGIN = pluginItem("storage_integration_plugin");
    public static final DeferredHolder<Item, Item> CRAFT_TERMINAL_PLUGIN = pluginItem("craft_terminal_plugin");
    public static final DeferredHolder<Item, Item> CHAIN_BREAK_PLUGIN = pluginItem("chain_break_plugin");
    public static final DeferredHolder<Item, Item> AREA_DESTROY_PLUGIN = pluginItem("area_destroy_plugin");
    public static final DeferredHolder<Item, Item> BLUEPRINT_PLUGIN = pluginItem("blueprint_plugin");
    public static final DeferredHolder<Item, Item> FIELD_DEPLOYMENT_PLUGIN = pluginItem("field_deployment_plugin");
    public static final DeferredHolder<Item, Item> RANGE_EXTENSION_I = pluginItem("range_extension_i");
    public static final DeferredHolder<Item, Item> RANGE_EXTENSION_II = pluginItem("range_extension_ii");
    public static final DeferredHolder<Item, Item> RANGE_EXTENSION_III = pluginItem("range_extension_iii");
    public static final DeferredHolder<Item, Item> RANGE_EXTENSION_MAX = pluginItem("range_extension_max");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RTSBUILDING_TAB = CREATIVE_TABS.register(
            "rtsbuilding",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rtsbuilding"))
                    .icon(() -> new ItemStack(RTS_CONTROL_CORE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(RTS_CONTROL_CORE.get());
                        output.accept(REMOTE_CONTROL_PLUGIN.get());
                        output.accept(STORAGE_INTEGRATION_PLUGIN.get());
                        output.accept(CRAFT_TERMINAL_PLUGIN.get());
                        output.accept(CHAIN_BREAK_PLUGIN.get());
                        output.accept(AREA_DESTROY_PLUGIN.get());
                        output.accept(BLUEPRINT_PLUGIN.get());
                        output.accept(FIELD_DEPLOYMENT_PLUGIN.get());
                        output.accept(RANGE_EXTENSION_I.get());
                        output.accept(RANGE_EXTENSION_II.get());
                        output.accept(RANGE_EXTENSION_III.get());
                        output.accept(RANGE_EXTENSION_MAX.get());
                    })
                    .build());

    public RtsbuildingMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.rtsbuilding.rtsbuilding.client.bootstrap.RtsClientBootstrap.registerConfigUi(modContainer);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Initialise the central service registry first
        ServiceRegistry.init();

        // Initialise the RTS API so addons can access it via RtsAPI.get()
        RtsAPIImpl.init();

        // Register all workflow pipelines
        RtsPipelineRegistration.registerAll();

        LOGGER.info("RTSBuilding common setup complete");
    }

    private static DeferredHolder<Item, Item> pluginItem(String id) {
        return ITEMS.register(id, () -> new RtsPluginItem(new Item.Properties().stacksTo(64)));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @EventBusSubscriber(modid = RtsbuildingMod.MODID)
    static class GameEvents {
        @SubscribeEvent
        static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                RtsCameraManager.cleanupOrphanCameras(serverPlayer.getServer());
                RtsDamageFeedbackManager.remember(serverPlayer);
                RtsProgressionManager.onPlayerLogin(serverPlayer);
                RtsPluginService.syncRelatedPlayers(serverPlayer);

                // Restore any persisted workflow entries from the world save file.
                // This lets the player continue their previous threads after reconnecting.
                // Passing the ServerPlayer allows the engine to notify the client
                // of restored entries immediately.
                RtsWorkflowEngine.getInstance().loadPlayerFromStore(
                        serverPlayer.getServer(), serverPlayer);
            }
        }

        @SubscribeEvent
        static void onServerStarted(ServerStartedEvent event) {
            ServerTickOrchestrator.getInstance().warmCreativeTabCaches(event.getServer());
            RtsCameraManager.cleanupOrphanCameras(event.getServer());
        }

        @SubscribeEvent
        static void onServerStopped(ServerStoppedEvent event) {
            // Persist all workflow entries before fully resetting the engine.
            // This ensures that when the player reloads this save, their
            // previous threads are restored from the world save file.
            RtsWorkflowEngine.getInstance().saveAll(event.getServer());

            // Fully reset the workflow engine when the server stops.
            // This ensures workflows from one save (world) do not leak
            // into the next save when switching worlds in singleplayer.
            RtsWorkflowEngine.getInstance().clearAllData();
        }

        @SubscribeEvent
        static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                RtsCameraManager.stopIfActive(serverPlayer);
                RtsDamageFeedbackManager.forget(serverPlayer);
                ServiceRegistry.getInstance().session().onPlayerLogout(serverPlayer);
                RtsProgressionManager.onPlayerLogout(serverPlayer);
                RtsPluginService.syncRelatedPlayers(serverPlayer);
                // Clear this player's undo history to prevent stale BlockPos entries when switching worlds
                ServerHistoryManager.clear(serverPlayer.getUUID());
            }
        }

        @SubscribeEvent
        static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                RtsCameraManager.stopIfActive(serverPlayer);
                ServiceRegistry.getInstance().pathfinding().cancel(serverPlayer);
                // Clear stale storage cache entries from the old dimension
                RtsStorageTickService.INSTANCE.unregisterPlayer(serverPlayer);
            }
        }

        @SubscribeEvent
        static void onPlayerTickPost(PlayerTickEvent.Post event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                ServerTickOrchestrator.getInstance().onPlayerTickPost(serverPlayer);
                RtsDamageFeedbackManager.tick(serverPlayer);
            }
        }

        @SubscribeEvent
        static void onServerTick(ServerTickEvent.Post event) {
            ServerTickOrchestrator.getInstance().tickMining(event.getServer());
        }
    }
}
