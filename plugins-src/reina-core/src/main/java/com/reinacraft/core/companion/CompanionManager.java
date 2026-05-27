package com.reinacraft.core.companion;

import org.bukkit.Bukkit;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lifecycle + per-tick driver for all active companions in the world.
 *
 * - One BukkitTask runs at 4 Hz and ticks every active CompanionEntity
 * - Listens for PlayerInteractEntityEvent on companion hitboxes (right-click dismiss for owner)
 * - Removes companions on player quit
 */
public final class CompanionManager implements Listener {

    private final Plugin plugin;
    private final Map<UUID, CompanionEntity> active = new HashMap<>();
    private BukkitTask task;

    public CompanionManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 5L, 5L); // 4 Hz
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
        for (CompanionEntity ce : active.values()) ce.despawn();
        active.clear();
    }

    public CompanionEntity active(UUID owner) { return active.get(owner); }

    public void summon(Player owner, Companion companion) {
        dismiss(owner.getUniqueId());
        active.put(owner.getUniqueId(), new CompanionEntity(owner, companion));
    }

    public void dismiss(UUID owner) {
        CompanionEntity ce = active.remove(owner);
        if (ce != null) ce.despawn();
    }

    public boolean has(UUID owner) {
        CompanionEntity ce = active.get(owner);
        return ce != null && ce.isValid();
    }

    private void tickAll() {
        if (active.isEmpty()) return;
        for (Map.Entry<UUID, CompanionEntity> entry : active.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            CompanionEntity ce = entry.getValue();
            if (owner == null || !owner.isOnline()) {
                ce.despawn();
                continue;
            }
            ce.tick(owner);
        }
        active.entrySet().removeIf(e -> !e.getValue().isValid());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction interaction)) return;
        String ownerStr = interaction.getPersistentDataContainer().get(
                CompanionEntity.OWNER_KEY, PersistentDataType.STRING);
        if (ownerStr == null) return;
        e.setCancelled(true);

        UUID interactionOwner;
        try { interactionOwner = UUID.fromString(ownerStr); } catch (Exception ex) { return; }

        if (!e.getPlayer().getUniqueId().equals(interactionOwner)) {
            // Other players' companions ignore right-clicks
            return;
        }

        // Owner right-clicks their own companion → friendly pulse + sound (no menu yet)
        e.getPlayer().playSound(e.getPlayer().getLocation(),
                org.bukkit.Sound.ENTITY_VILLAGER_AMBIENT, 0.5f, 1.4f);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        // ItemDisplay/Interaction can't be damaged by default but guard anyway.
        if (e.getEntity().getPersistentDataContainer().has(
                CompanionEntity.OWNER_KEY, PersistentDataType.STRING)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        dismiss(e.getPlayer().getUniqueId());
    }
}
