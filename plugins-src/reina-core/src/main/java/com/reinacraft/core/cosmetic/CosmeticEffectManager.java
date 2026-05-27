package com.reinacraft.core.cosmetic;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies/removes Pet entities and Trail particles for online players.
 *
 * Pet: a tame, AI-less LivingEntity that teleports to follow the owner.
 * Trail: per-tick particle spawn at the player's feet.
 *
 * Gadgets are handled separately via item-based interaction (CosmeticGadgetListener).
 */
public final class CosmeticEffectManager implements Listener {

    private final Plugin plugin;
    private final CosmeticService service;

    // Active pet entity per player
    private final Map<UUID, Entity> activePets = new HashMap<>();
    // Single shared task per tick that updates pet positions + emits trail particles
    private BukkitTask tickTask;

    public CosmeticEffectManager(Plugin plugin, CosmeticService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void start() {
        if (tickTask != null) return;
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L); // 10 Hz
    }

    public void stop() {
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        for (Entity pet : activePets.values()) {
            if (pet != null && !pet.isDead()) pet.remove();
        }
        activePets.clear();
    }

    /** Apply (or refresh) all equipped cosmetics for a player. Called on join + on equip changes. */
    public void applyAll(Player player) {
        applyPet(player, service.equipped(player.getUniqueId(), CosmeticCategory.PET));
        // Trail is per-tick, no setup needed beyond the equipped flag.
    }

    public void clearAll(UUID uuid) {
        Entity pet = activePets.remove(uuid);
        if (pet != null && !pet.isDead()) pet.remove();
    }

    /** Spawn / despawn the pet entity for the given player. */
    public void applyPet(Player player, Cosmetic pet) {
        UUID uuid = player.getUniqueId();
        Entity existing = activePets.remove(uuid);
        if (existing != null && !existing.isDead()) existing.remove();

        if (pet == null) return;

        EntityType type = petType(pet);
        if (type == null) return;

        Location loc = player.getLocation().clone().add(1.5, 0, 0);
        Entity entity = player.getWorld().spawnEntity(loc, type);
        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setInvulnerable(true);
            living.setSilent(true);
            living.setCollidable(false);
            living.setRemoveWhenFarAway(false);
            living.setPersistent(false);
            living.setCustomNameVisible(false);
            if (entity instanceof org.bukkit.entity.Wolf wolf) {
                wolf.setTamed(true);
                wolf.setOwner(player);
            }
        }
        activePets.put(uuid, entity);
    }

    private EntityType petType(Cosmetic pet) {
        return switch (pet) {
            case PET_WOLF    -> EntityType.WOLF;
            case PET_FOX     -> EntityType.FOX;
            case PET_BAT     -> EntityType.BAT;
            case PET_PARROT  -> EntityType.PARROT;
            case PET_RABBIT  -> EntityType.RABBIT;
            case PET_AXOLOTL -> EntityType.AXOLOTL;
            default -> null;
        };
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            // ---- Pet follow ----
            Entity pet = activePets.get(uuid);
            if (pet != null && !pet.isDead()) {
                Location target = player.getLocation().clone().add(
                        Math.cos(System.currentTimeMillis() * 0.001 + uuid.hashCode() * 0.0001) * 1.3,
                        0.0,
                        Math.sin(System.currentTimeMillis() * 0.001 + uuid.hashCode() * 0.0001) * 1.3);
                target.setY(player.getLocation().getY());
                if (target.distanceSquared(pet.getLocation()) > 0.25) {
                    Vector dir = target.toVector().subtract(pet.getLocation().toVector());
                    if (dir.lengthSquared() > 25) {
                        // Too far — teleport
                        pet.teleport(target);
                    } else if (pet instanceof LivingEntity) {
                        // Smooth glide via short teleport
                        Location next = pet.getLocation().clone().add(dir.multiply(0.25));
                        next.setYaw(player.getLocation().getYaw());
                        pet.teleport(next);
                    }
                }
            }

            // ---- Trail particles ----
            Cosmetic trail = service.equipped(uuid, CosmeticCategory.TRAIL);
            if (trail != null) emitTrail(player, trail);
        }
    }

    private void emitTrail(Player player, Cosmetic trail) {
        Location at = player.getLocation().clone().add(0, 0.1, 0);
        switch (trail) {
            case TRAIL_FLAME -> player.getWorld().spawnParticle(Particle.FLAME, at, 3, 0.2, 0.05, 0.2, 0.005);
            case TRAIL_HEART -> player.getWorld().spawnParticle(Particle.HEART, at.add(0, 0.5, 0), 1, 0.3, 0.3, 0.3, 0);
            case TRAIL_ENCHANT -> player.getWorld().spawnParticle(Particle.ENCHANT, at.add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0.5);
            case TRAIL_END_ROD -> player.getWorld().spawnParticle(Particle.END_ROD, at, 2, 0.2, 0.1, 0.2, 0.005);
            case TRAIL_SOUL_FIRE -> player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, at, 3, 0.2, 0.05, 0.2, 0.005);
            case TRAIL_RAINBOW -> {
                int[] palette = {0xFF1744, 0xFFD700, 0x00E676, 0x00B0FF, 0xD500F9};
                int idx = (int) ((System.currentTimeMillis() / 100) % palette.length);
                int rgb = palette[idx];
                Particle.DustOptions opts = new Particle.DustOptions(
                        Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF), 1.0f);
                player.getWorld().spawnParticle(Particle.DUST, at, 4, 0.25, 0.1, 0.25, 0, opts);
            }
            default -> {}
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clearAll(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDamageToPet(EntityDamageEvent e) {
        // (no-op; pets are setInvulnerable(true), but defense in depth)
    }
}
