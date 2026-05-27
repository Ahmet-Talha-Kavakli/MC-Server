package com.reinacraft.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class MenuHolder implements InventoryHolder {

    private final Menu menu;

    public MenuHolder(Menu menu) {
        this.menu = menu;
    }

    public Menu menu() { return menu; }

    @Override
    public @NotNull Inventory getInventory() {
        return Bukkit.createInventory(this, menu.size(), menu.title());
    }
}
