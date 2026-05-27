package com.reinacraft.core.companion;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * One companion attached to one owning player. Holds the ItemDisplay (visual) + Interaction (hitbox).
 *
 * Tick-driven externally by CompanionEffectManager; no per-instance scheduler.
 */
public final class CompanionEntity {

    public static final NamespacedKey OWNER_KEY = new NamespacedKey("reinacraft", "companion_owner");
    public static final NamespacedKey COMPANION_KEY = new NamespacedKey("reinacraft", "companion_id");

    private final UUID ownerUuid;
    private final Companion companion;
    private ItemDisplay display;
    private Interaction hitbox;
    private boolean valid = true;
    private long lastHardTeleport = 0L;

    public CompanionEntity(Player owner, Companion companion) {
        this.ownerUuid = owner.getUniqueId();
        this.companion = companion;
        spawn(owner.getLocation());
        // Greeting puff at the owner's feet
        owner.getWorld().spawnParticle(Particle.CLOUD, owner.getLocation().add(0, 0.5, 0), 12, 0.4, 0.2, 0.4, 0.02);
        owner.playSound(owner.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.4f);
    }

    public UUID ownerUuid() { return ownerUuid; }
    public Companion companion() { return companion; }
    public ItemDisplay display() { return display; }
    public Interaction hitbox() { return hitbox; }
    public boolean isValid() { return valid && display != null && !display.isDead(); }

    private void spawn(Location at) {
        World world = at.getWorld();
        if (world == null) return;

        Location spawnLoc = computeTarget(at, companion);

        display = world.spawn(spawnLoc, ItemDisplay.class, d -> {
            ItemStack item = ItemStack.of(Material.PAPER);
            try {
                item.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.fromString(companion.modelKey()));
            } catch (Throwable t) {
                // Fallback: clients without our resource pack will just see a paper item.
                Bukkit.getLogger().warning("[companion] item_model set failed: " + t.getMessage());
            }
            d.setItemStack(item);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setBillboard(Display.Billboard.FIXED);
            d.setViewRange(0.6f); // ~48 blocks
            d.setShadowRadius(0.4f);
            d.setShadowStrength(0.6f);
            float s = companion.scale();
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(),
                    new Vector3f(s, s, s),
                    new AxisAngle4f()
            ));
            d.setTeleportDuration(5); // smooth position interp on the client
            d.setInterpolationDuration(5);
            d.setPersistent(false);
            d.getPersistentDataContainer().set(OWNER_KEY, PersistentDataType.STRING, ownerUuid.toString());
            d.getPersistentDataContainer().set(COMPANION_KEY, PersistentDataType.STRING, companion.id());
        });

        hitbox = world.spawn(spawnLoc, Interaction.class, h -> {
            h.setInteractionWidth(Math.max(0.6f, companion.scale()));
            h.setInteractionHeight(Math.max(0.6f, companion.scale() * 1.2f));
            h.setResponsive(true);
            h.setPersistent(false);
            h.getPersistentDataContainer().set(OWNER_KEY, PersistentDataType.STRING, ownerUuid.toString());
            h.getPersistentDataContainer().set(COMPANION_KEY, PersistentDataType.STRING, companion.id());
        });
        display.addPassenger(hitbox);
    }

    /** Called every few ticks by the manager. Moves toward a position just behind/beside the owner. */
    public void tick(Player owner) {
        if (!isValid()) return;
        if (owner == null || !owner.isOnline() || owner.getWorld() != display.getWorld()) {
            despawn();
            return;
        }
        Location target = computeTarget(owner.getLocation(), companion);

        double distSq = target.distanceSquared(display.getLocation());
        if (distSq > 144) { // > 12 blocks → hard teleport with poof
            long now = System.currentTimeMillis();
            if (now - lastHardTeleport > 500) {
                lastHardTeleport = now;
                display.getWorld().spawnParticle(Particle.CLOUD, display.getLocation(), 10, 0.3, 0.3, 0.3, 0.02);
            }
            display.teleport(target);
            return;
        }

        // Make the companion face the player so the model isn't pointing away
        Vector dir = owner.getEyeLocation().toVector().subtract(display.getLocation().toVector());
        if (dir.lengthSquared() > 0.01) {
            float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
            target.setYaw(yaw);
        }

        display.teleport(target);
    }

    private static Location computeTarget(Location ownerLoc, Companion c) {
        // Stand 1.2 blocks behind + 0.6 to the right of the owner
        Vector behind = ownerLoc.getDirection().clone().setY(0).normalize().multiply(-1.2);
        Vector side = new Vector(-behind.getZ(), 0, behind.getX()).normalize().multiply(0.6);
        Location target = ownerLoc.clone().add(behind).add(side);

        switch (c.behavior()) {
            case FLY -> target.setY(ownerLoc.getY() + 1.4);
            case HOP -> target.setY(ownerLoc.getY() + Math.abs(Math.sin(System.currentTimeMillis() * 0.005)) * 0.3);
            case WALK -> target.setY(ownerLoc.getY());
        }
        return target;
    }

    public void despawn() {
        valid = false;
        if (hitbox != null && !hitbox.isDead()) hitbox.remove();
        if (display != null && !display.isDead()) display.remove();
        hitbox = null;
        display = null;
    }
}
