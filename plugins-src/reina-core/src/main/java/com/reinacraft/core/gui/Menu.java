package com.reinacraft.core.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class Menu {

    public interface ClickHandler {
        void onClick(Player player, ClickType clickType);
    }

    private final Component title;
    private final int size;
    private final Map<Integer, ItemStack> items = new HashMap<>();
    private final Map<Integer, ClickHandler> handlers = new HashMap<>();
    private final boolean cancelClicks;

    Menu(Component title, int size, Map<Integer, ItemStack> items, Map<Integer, ClickHandler> handlers, boolean cancelClicks) {
        this.title = title;
        this.size = size;
        this.items.putAll(items);
        this.handlers.putAll(handlers);
        this.cancelClicks = cancelClicks;
    }

    public Inventory build(Player viewer) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(this), size, title);
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            inv.setItem(entry.getKey(), entry.getValue());
        }
        return inv;
    }

    public void open(Player player) {
        player.openInventory(build(player));
    }

    public Component title() { return title; }
    public int size() { return size; }
    public boolean cancelClicks() { return cancelClicks; }
    public ClickHandler handler(int slot) { return handlers.get(slot); }
}
