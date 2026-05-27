package com.reinacraft.core.cosmetic;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gadgets are inventory items. We track an item's cosmetic id via PersistentDataContainer
 * and trigger an effect on right-click. Cooldowns prevent spam.
 */
public final class CosmeticGadgetListener implements Listener {

    public static final org.bukkit.NamespacedKey GADGET_KEY =
            new org.bukkit.NamespacedKey("reinacraft", "gadget_id");

    private final Plugin plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public CosmeticGadgetListener(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Build an inventory item representing a gadget (sets PDC tag so we recognise it on click). */
    public static ItemStack buildGadgetItem(Cosmetic gadget) {
        ItemStack stack = new ItemStack(gadget.icon());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(gadget.displayName());
        meta.lore(java.util.List.of(gadget.description()));
        meta.getPersistentDataContainer().set(GADGET_KEY, PersistentDataType.STRING, gadget.id());
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String gadgetId = meta.getPersistentDataContainer().get(GADGET_KEY, PersistentDataType.STRING);
        if (gadgetId == null) return;
        Cosmetic gadget = Cosmetic.byId(gadgetId);
        if (gadget == null || gadget.category() != CosmeticCategory.GADGET) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        if (onCooldown(p.getUniqueId(), gadgetId)) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.7f);
            return;
        }
        markCooldown(p.getUniqueId(), gadgetId);
        triggerGadget(p, gadget);
    }

    private void triggerGadget(Player p, Cosmetic gadget) {
        Location at = p.getLocation();
        switch (gadget) {
            case GADGET_JUMP_BOOTS -> {
                p.setVelocity(new Vector(0, 1.5, 0).add(p.getLocation().getDirection().multiply(0.3)));
                p.playSound(at, Sound.ENTITY_RABBIT_JUMP, 1.0f, 1.2f);
                p.getWorld().spawnParticle(Particle.CLOUD, at, 12, 0.4, 0.0, 0.4, 0.05);
            }
            case GADGET_FIREWORK -> {
                Firework fw = (Firework) p.getWorld().spawnEntity(at, org.bukkit.entity.EntityType.FIREWORK_ROCKET);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.STAR)
                        .withColor(Color.fromRGB(0xFF, 0x17, 0x44), Color.fromRGB(0xFF, 0xD7, 0x00))
                        .withFade(Color.fromRGB(0x00, 0xB0, 0xFF))
                        .withFlicker().withTrail().build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            }
            case GADGET_PAINT -> {
                int[] palette = {0xFF1744, 0xFFD700, 0x00E676, 0x00B0FF, 0xD500F9};
                for (int i = 0; i < palette.length; i++) {
                    int rgb = palette[i];
                    Particle.DustOptions opts = new Particle.DustOptions(
                            Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF), 2.0f);
                    p.getWorld().spawnParticle(Particle.DUST,
                            at.clone().add(Math.cos(i) * 2, 1.5, Math.sin(i) * 2),
                            30, 1.5, 1.0, 1.5, 0, opts);
                }
                p.playSound(at, Sound.ENTITY_SPLASH_POTION_BREAK, 0.8f, 1.2f);
            }
            case GADGET_SNOWBALL -> {
                Vector dir = at.getDirection().normalize();
                for (int i = 0; i < 8; i++) {
                    Snowball s = (Snowball) p.getWorld().spawnEntity(at.clone().add(0, 1.5, 0), org.bukkit.entity.EntityType.SNOWBALL);
                    Vector v = dir.clone().add(new Vector(Math.random() * 0.4 - 0.2, Math.random() * 0.3, Math.random() * 0.4 - 0.2));
                    s.setVelocity(v.multiply(1.5));
                    s.setShooter(p);
                }
                p.playSound(at, Sound.ENTITY_SNOW_GOLEM_SHOOT, 1.0f, 1.1f);
            }
            case GADGET_LIGHTNING -> {
                Location target = at.clone().add(at.getDirection().multiply(8));
                LightningStrike strike = p.getWorld().strikeLightningEffect(target);
                p.playSound(at, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.0f);
            }
            case GADGET_TROLL -> {
                // Random fake sounds around the player
                Sound[] fakes = {Sound.ENTITY_CREEPER_PRIMED, Sound.ENTITY_GHAST_SCREAM, Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.ENTITY_WITHER_SPAWN};
                for (org.bukkit.entity.Player target : p.getWorld().getPlayers()) {
                    Sound s = fakes[(int) (Math.random() * fakes.length)];
                    target.playSound(target.getLocation(), s, 0.3f, (float) (0.6 + Math.random() * 0.8));
                }
                p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, at.clone().add(0, 2, 0), 20, 1.0, 1.0, 1.0, 0);
            }
            default -> {}
        }
    }

    private boolean onCooldown(UUID uuid, String gadgetId) {
        Map<String, Long> map = cooldowns.get(uuid);
        if (map == null) return false;
        Long last = map.get(gadgetId);
        return last != null && System.currentTimeMillis() - last < 3000;
    }

    private void markCooldown(UUID uuid, String gadgetId) {
        cooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(gadgetId, System.currentTimeMillis());
    }
}
