package com.reinacraft.core.npc;

import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class NpcListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof Interaction interaction) {
            HubNpc npc = HubNpc.BY_INTERACTION.get(interaction.getUniqueId());
            if (npc == null) return;
            e.setCancelled(true);
            npc.onInteract(e.getPlayer(), HubNpc.HandType.RIGHT);
            return;
        }
        if (e.getRightClicked() instanceof Villager v) {
            String npcId = v.getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey("reinacraft", "npc_id"),
                    org.bukkit.persistence.PersistentDataType.STRING);
            if (npcId == null) return;
            e.setCancelled(true);
            for (HubNpc npc : HubNpc.BY_INTERACTION.values()) {
                if (npc.id().equals(npcId)) {
                    npc.onInteract(e.getPlayer(), HubNpc.HandType.RIGHT);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (e.getEntity() instanceof Interaction interaction) {
            HubNpc npc = HubNpc.BY_INTERACTION.get(interaction.getUniqueId());
            if (npc == null) return;
            e.setCancelled(true);
            npc.onInteract(p, HubNpc.HandType.LEFT);
            return;
        }
        if (e.getEntity() instanceof Villager v) {
            String npcId = v.getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey("reinacraft", "npc_id"),
                    org.bukkit.persistence.PersistentDataType.STRING);
            if (npcId != null) e.setCancelled(true);
        }
    }
}
