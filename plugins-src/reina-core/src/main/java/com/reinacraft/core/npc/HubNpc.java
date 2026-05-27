package com.reinacraft.core.npc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class HubNpc {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static final ConcurrentHashMap<UUID, HubNpc> BY_INTERACTION = new ConcurrentHashMap<>();

    public enum HandType { LEFT, RIGHT, BOTH }

    private final Plugin plugin;
    private final String id;
    private final Location location;
    private final Villager.Profession profession;
    private final List<String> hologramLines;
    private final BiConsumer<Player, HandType> onInteract;

    private Villager villager;
    private Interaction interactionEntity;
    private TextDisplay textDisplay;

    public HubNpc(Plugin plugin, String id, Location location, Villager.Profession profession,
                  List<String> hologramLines, BiConsumer<Player, HandType> onInteract) {
        this.plugin = plugin;
        this.id = id;
        this.location = location;
        this.profession = profession;
        this.hologramLines = hologramLines;
        this.onInteract = onInteract;
    }

    public String id() { return id; }
    public Location location() { return location; }

    public void spawn() {
        despawnExisting();

        // 1) Villager body
        villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setProfession(profession);
        villager.setAdult();
        villager.setAI(false);
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.setCollidable(false);
        villager.setRemoveWhenFarAway(false);
        villager.setPersistent(true);
        villager.customName(Component.empty());
        villager.setCustomNameVisible(false);
        villager.getPersistentDataContainer().set(
                new NamespacedKey("reinacraft", "npc_id"),
                PersistentDataType.STRING, id);

        // 2) Interaction entity (clickable hitbox above the villager)
        interactionEntity = (Interaction) location.getWorld().spawnEntity(location, EntityType.INTERACTION);
        interactionEntity.setInteractionWidth(1.2f);
        interactionEntity.setInteractionHeight(2.4f);
        interactionEntity.setResponsive(true);
        interactionEntity.setPersistent(true);
        interactionEntity.getPersistentDataContainer().set(
                new NamespacedKey("reinacraft", "npc_id"),
                PersistentDataType.STRING, id);
        BY_INTERACTION.put(interactionEntity.getUniqueId(), this);

        // 3) Hologram
        Location holoLoc = location.clone().add(0, 2.4, 0);
        textDisplay = (TextDisplay) location.getWorld().spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setSeeThrough(true);
        textDisplay.setShadowed(false);
        textDisplay.setDefaultBackground(false);
        textDisplay.setBackgroundColor(Color.fromARGB(120, 0, 0, 0));
        textDisplay.setPersistent(true);
        textDisplay.getPersistentDataContainer().set(
                new NamespacedKey("reinacraft", "npc_id"),
                PersistentDataType.STRING, id);
        updateHologram(hologramLines);
    }

    public void updateHologram(List<String> mmLines) {
        if (textDisplay == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mmLines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(mmLines.get(i));
        }
        textDisplay.text(MM.deserialize(sb.toString()));
    }

    public void onInteract(Player player, HandType hand) {
        if (onInteract != null) onInteract.accept(player, hand);
    }

    public void despawn() {
        if (interactionEntity != null) {
            BY_INTERACTION.remove(interactionEntity.getUniqueId());
            interactionEntity.remove();
            interactionEntity = null;
        }
        if (textDisplay != null) { textDisplay.remove(); textDisplay = null; }
        if (villager != null) { villager.remove(); villager = null; }
    }

    private void despawnExisting() {
        NamespacedKey key = new NamespacedKey("reinacraft", "npc_id");
        for (org.bukkit.entity.Entity e : location.getWorld().getNearbyEntities(location, 4, 4, 4)) {
            String tag = e.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (id.equals(tag)) e.remove();
        }
    }
}
