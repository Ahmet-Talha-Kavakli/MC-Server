package com.reinacraft.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public final class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof MenuHolder mh)) return;
        Menu menu = mh.menu();
        if (menu.cancelClicks()) e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Menu.ClickHandler handler = menu.handler(e.getSlot());
        if (handler != null) handler.onClick(p, e.getClick());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (holder instanceof MenuHolder mh && mh.menu().cancelClicks()) {
            e.setCancelled(true);
        }
    }
}
