package com.rtsbuilding.rtsbuilding.client.bootstrap;

import com.mojang.blaze3d.platform.InputConstants;
import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RtsbuildingMod.MODID, value = Dist.CLIENT)
public final class ClientKeyMappings {
    private static final InputConstants.Key LEGACY_ROTATE_DRAG_DEFAULT =
            InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
    private static final InputConstants.Key LEGACY_PAN_DRAG_DEFAULT =
            InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    private static final InputConstants.Key DEFAULT_ROTATE_DRAG =
            InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    private static final InputConstants.Key DEFAULT_PAN_DRAG =
            InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);

    public static final KeyMapping TOGGLE_RTS = new KeyMapping(
            "key.rtsbuilding.toggle_rts",
            GLFW.GLFW_KEY_G,
            "key.categories.rtsbuilding");
    public static final KeyMapping QUICK_FUNNEL = new KeyMapping(
            "key.rtsbuilding.quick_funnel",
            GLFW.GLFW_KEY_F,
            "key.categories.rtsbuilding");
    public static final KeyMapping QUICK_DROP = new KeyMapping(
            "key.rtsbuilding.quick_drop",
            GLFW.GLFW_KEY_Q,
            "key.categories.rtsbuilding");
    public static final KeyMapping ROTATE_SHAPE = new KeyMapping(
            "key.rtsbuilding.rotate_shape",
            GLFW.GLFW_KEY_R,
            "key.categories.rtsbuilding");
    public static final KeyMapping OPEN_CRAFT_TERMINAL = new KeyMapping(
            "key.rtsbuilding.open_craft_terminal",
            GLFW.GLFW_KEY_C,
            "key.categories.rtsbuilding");
    public static final KeyMapping PIN_QUICK_SLOT = new KeyMapping(
            "key.rtsbuilding.pin_quick_slot",
            GLFW.GLFW_KEY_P,
            "key.categories.rtsbuilding");
    public static final KeyMapping BLUEPRINT_CANCEL = new KeyMapping(
            "key.rtsbuilding.blueprint_cancel",
            GLFW.GLFW_KEY_X,
            "key.categories.rtsbuilding");
    public static final KeyMapping DECREASE_SENSITIVITY = new KeyMapping(
            "key.rtsbuilding.decrease_sensitivity",
            GLFW.GLFW_KEY_LEFT_BRACKET,
            "key.categories.rtsbuilding");
    public static final KeyMapping INCREASE_SENSITIVITY = new KeyMapping(
            "key.rtsbuilding.increase_sensitivity",
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            "key.categories.rtsbuilding");
    public static final KeyMapping MODE_INTERACT = new KeyMapping(
            "key.rtsbuilding.mode_interact",
            GLFW.GLFW_KEY_I,
            "key.categories.rtsbuilding");
    public static final KeyMapping MODE_LINK_STORAGE = new KeyMapping(
            "key.rtsbuilding.mode_link_storage",
            GLFW.GLFW_KEY_L,
            "key.categories.rtsbuilding");
    public static final KeyMapping MODE_ROTATE = new KeyMapping(
            "key.rtsbuilding.mode_rotate",
            GLFW.GLFW_KEY_R,
            "key.categories.rtsbuilding");
    public static final KeyMapping MODE_FUNNEL = new KeyMapping(
            "key.rtsbuilding.mode_funnel",
            GLFW.GLFW_KEY_F,
            "key.categories.rtsbuilding");
    public static final KeyMapping ACTION_PRIMARY = new KeyMapping(
            "key.rtsbuilding.action_primary",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT,
            "key.categories.rtsbuilding");
    public static final KeyMapping MOVE_PLAYER = new KeyMapping(
            "key.rtsbuilding.move_player",
            KeyConflictContext.GUI,
            KeyModifier.CONTROL,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT,
            "key.categories.rtsbuilding");
    public static final KeyMapping ACTION_BREAK = new KeyMapping(
            "key.rtsbuilding.action_break",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            "key.categories.rtsbuilding");
    public static final KeyMapping CAMERA_ROTATE_DRAG = new KeyMapping(
            "key.rtsbuilding.camera_rotate_drag",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT,
            "key.categories.rtsbuilding");
    public static final KeyMapping CAMERA_PAN_DRAG = new KeyMapping(
            "key.rtsbuilding.camera_pan_drag",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            "key.categories.rtsbuilding");
    public static final KeyMapping PICK_BLOCK = new KeyMapping(
            "key.rtsbuilding.pick_block",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            "key.categories.rtsbuilding");
    public static final KeyMapping CAMERA_UP = new KeyMapping(
            "key.rtsbuilding.camera_up",
            GLFW.GLFW_KEY_SPACE,
            "key.categories.rtsbuilding");
    public static final KeyMapping CAMERA_UP_SECONDARY = new KeyMapping(
            "key.rtsbuilding.camera_up_secondary",
            GLFW.GLFW_KEY_UP,
            "key.categories.rtsbuilding");
    public static final KeyMapping CAMERA_DOWN = new KeyMapping(
            "key.rtsbuilding.camera_down_arrow",
            GLFW.GLFW_KEY_DOWN,
            "key.categories.rtsbuilding");
    public static final KeyMapping QUICK_BUILD = new KeyMapping(
            "key.rtsbuilding.quick_build",
            GLFW.GLFW_KEY_B,
            "key.categories.rtsbuilding");
    public static final KeyMapping TOGGLE_CHUNK_DISPLAY = new KeyMapping(
            "key.rtsbuilding.toggle_chunk_display",
            GLFW.GLFW_KEY_H,
            "key.categories.rtsbuilding");

    public static final KeyMapping GUI_BINDING_1 = new KeyMapping(
            "key.rtsbuilding.gui_binding_1",
            GLFW.GLFW_KEY_KP_1,
            "key.categories.rtsbuilding");
    public static final KeyMapping GUI_BINDING_2 = new KeyMapping(
            "key.rtsbuilding.gui_binding_2",
            GLFW.GLFW_KEY_KP_2,
            "key.categories.rtsbuilding");
    public static final KeyMapping GUI_BINDING_3 = new KeyMapping(
            "key.rtsbuilding.gui_binding_3",
            GLFW.GLFW_KEY_KP_3,
            "key.categories.rtsbuilding");
    public static final KeyMapping GUI_BINDING_4 = new KeyMapping(
            "key.rtsbuilding.gui_binding_4",
            GLFW.GLFW_KEY_KP_4,
            "key.categories.rtsbuilding");
    public static final KeyMapping GUI_BINDING_5 = new KeyMapping(
            "key.rtsbuilding.gui_binding_5",
            GLFW.GLFW_KEY_KP_5,
            "key.categories.rtsbuilding");
    public static final KeyMapping GUI_BINDING_6 = new KeyMapping(
            "key.rtsbuilding.gui_binding_6",
            GLFW.GLFW_KEY_KP_6,
            "key.categories.rtsbuilding");
    public static final KeyMapping GUI_BINDING_7 = new KeyMapping(
            "key.rtsbuilding.gui_binding_7",
            GLFW.GLFW_KEY_KP_7,
            "key.categories.rtsbuilding");
    public static final KeyMapping GUI_BINDING_8 = new KeyMapping(
            "key.rtsbuilding.gui_binding_8",
            GLFW.GLFW_KEY_KP_8,
            "key.categories.rtsbuilding");

    public static final KeyMapping[] GUI_BINDINGS = {
            GUI_BINDING_1, GUI_BINDING_2, GUI_BINDING_3, GUI_BINDING_4,
            GUI_BINDING_5, GUI_BINDING_6, GUI_BINDING_7, GUI_BINDING_8
    };

    public static final KeyMapping CYCLE_SHAPE = new KeyMapping(
            "key.rtsbuilding.cycle_shape",
            GLFW.GLFW_KEY_TAB,
            "key.categories.rtsbuilding");

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_RTS);
        event.register(QUICK_FUNNEL);
        event.register(QUICK_DROP);
        event.register(ROTATE_SHAPE);
        event.register(OPEN_CRAFT_TERMINAL);
        event.register(PIN_QUICK_SLOT);
        event.register(BLUEPRINT_CANCEL);
        event.register(DECREASE_SENSITIVITY);
        event.register(INCREASE_SENSITIVITY);
        event.register(MODE_INTERACT);
        event.register(MODE_LINK_STORAGE);
        event.register(MODE_ROTATE);
        event.register(MODE_FUNNEL);
        event.register(ACTION_PRIMARY);
        event.register(MOVE_PLAYER);
        event.register(ACTION_BREAK);
        event.register(CAMERA_ROTATE_DRAG);
        event.register(CAMERA_PAN_DRAG);
        event.register(PICK_BLOCK);
        event.register(CAMERA_UP);
        event.register(CAMERA_UP_SECONDARY);
        event.register(CAMERA_DOWN);
        event.register(QUICK_BUILD);
        event.register(TOGGLE_CHUNK_DISPLAY);
        event.register(CYCLE_SHAPE);
        for (KeyMapping binding : GUI_BINDINGS) {
            event.register(binding);
        }
        migrateLegacyDragDefaults();
    }

    private static void migrateLegacyDragDefaults() {
        if (CAMERA_ROTATE_DRAG.getKey().equals(LEGACY_ROTATE_DRAG_DEFAULT)
                && CAMERA_PAN_DRAG.getKey().equals(LEGACY_PAN_DRAG_DEFAULT)) {
            CAMERA_ROTATE_DRAG.setKey(DEFAULT_ROTATE_DRAG);
            CAMERA_PAN_DRAG.setKey(DEFAULT_PAN_DRAG);
            KeyMapping.resetMapping();
        }
    }
}
