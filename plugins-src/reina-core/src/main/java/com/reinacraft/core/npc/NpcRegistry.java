package com.reinacraft.core.npc;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NpcRegistry {

    private final Plugin plugin;
    private final Map<String, HubNpc> npcs = new LinkedHashMap<>();

    public NpcRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(HubNpc npc) {
        if (npcs.containsKey(npc.id())) {
            npcs.get(npc.id()).despawn();
        }
        npcs.put(npc.id(), npc);
        npc.spawn();
    }

    public HubNpc get(String id) { return npcs.get(id); }

    public List<HubNpc> all() { return new ArrayList<>(npcs.values()); }

    public void shutdown() {
        for (HubNpc npc : npcs.values()) npc.despawn();
        npcs.clear();
    }

    /** Sweep the world for stale reina NPC entities that aren't tracked. Called on init. */
    public void cleanupStale() {
        NamespacedKey key = new NamespacedKey("reinacraft", "npc_id");
        for (var world : plugin.getServer().getWorlds()) {
            for (Entity e : world.getEntities()) {
                String tag = e.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (tag == null) continue;
                if (!npcs.containsKey(tag)) {
                    e.remove();
                }
            }
        }
    }
}
