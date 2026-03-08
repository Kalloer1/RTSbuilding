package com.rtsbuilding.rtsbuilding.server;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

public final class RtsCraftTerminalMenu extends CraftingMenu {
    public RtsCraftTerminalMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(containerId, inventory, access);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

