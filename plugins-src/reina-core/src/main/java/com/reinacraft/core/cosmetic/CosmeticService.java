package com.reinacraft.core.cosmetic;

import com.reinacraft.core.database.DatabaseManager;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class CosmeticService {

    private final Plugin plugin;
    private final DatabaseManager db;

    // Cache: uuid → owned cosmetics + currently-equipped per category
    private final Map<UUID, Set<Cosmetic>> ownedCache = new ConcurrentHashMap<>();
    private final Map<UUID, EnumMap<CosmeticCategory, Cosmetic>> equippedCache = new ConcurrentHashMap<>();

    public CosmeticService(Plugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public Set<Cosmetic> owned(UUID uuid) {
        return ownedCache.getOrDefault(uuid, EnumSet.noneOf(Cosmetic.class));
    }

    public boolean owns(UUID uuid, Cosmetic cosmetic) {
        return owned(uuid).contains(cosmetic);
    }

    public Cosmetic equipped(UUID uuid, CosmeticCategory category) {
        EnumMap<CosmeticCategory, Cosmetic> map = equippedCache.get(uuid);
        return map == null ? null : map.get(category);
    }

    /** Loads owned + equipped from DB into the cache. Call on join (async OK). */
    public void load(UUID uuid) {
        Set<Cosmetic> owned = EnumSet.noneOf(Cosmetic.class);
        EnumMap<CosmeticCategory, Cosmetic> equipped = new EnumMap<>(CosmeticCategory.class);
        try (Connection c = db.connection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT cosmetic_id FROM player_cosmetics WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Cosmetic cos = Cosmetic.byId(rs.getString(1));
                        if (cos != null) owned.add(cos);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT category, cosmetic_id FROM player_cosmetic_equipped WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            CosmeticCategory cat = CosmeticCategory.valueOf(rs.getString(1));
                            Cosmetic cos = Cosmetic.byId(rs.getString(2));
                            if (cos != null) equipped.put(cat, cos);
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Cosmetic load failed for " + uuid + ": " + ex.getMessage());
        }
        ownedCache.put(uuid, owned);
        equippedCache.put(uuid, equipped);
    }

    public void unload(UUID uuid) {
        ownedCache.remove(uuid);
        equippedCache.remove(uuid);
    }

    /** Adds a cosmetic to a player's collection (purchased). Returns true on success. */
    public CompletableFuture<Boolean> grant(UUID uuid, Cosmetic cosmetic) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = db.connection();
                 PreparedStatement ps = c.prepareStatement(
                         "INSERT IGNORE INTO player_cosmetics (uuid, cosmetic_id) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, cosmetic.id());
                ps.executeUpdate();
                ownedCache.computeIfAbsent(uuid, k -> EnumSet.noneOf(Cosmetic.class)).add(cosmetic);
                return true;
            } catch (SQLException ex) {
                plugin.getLogger().warning("Cosmetic grant failed: " + ex.getMessage());
                return false;
            }
        });
    }

    /** Equips a cosmetic (or unequips if `cosmetic == null`). Persists to DB. */
    public CompletableFuture<Void> equip(UUID uuid, CosmeticCategory category, Cosmetic cosmetic) {
        // Update cache first
        EnumMap<CosmeticCategory, Cosmetic> equipped =
                equippedCache.computeIfAbsent(uuid, k -> new EnumMap<>(CosmeticCategory.class));
        if (cosmetic == null) equipped.remove(category);
        else equipped.put(category, cosmetic);

        return CompletableFuture.runAsync(() -> {
            try (Connection c = db.connection()) {
                if (cosmetic == null) {
                    try (PreparedStatement del = c.prepareStatement(
                            "DELETE FROM player_cosmetic_equipped WHERE uuid = ? AND category = ?")) {
                        del.setString(1, uuid.toString());
                        del.setString(2, category.name());
                        del.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ups = c.prepareStatement(
                            "INSERT INTO player_cosmetic_equipped (uuid, category, cosmetic_id) " +
                                    "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE cosmetic_id = VALUES(cosmetic_id)")) {
                        ups.setString(1, uuid.toString());
                        ups.setString(2, category.name());
                        ups.setString(3, cosmetic.id());
                        ups.executeUpdate();
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("Cosmetic equip persist failed: " + ex.getMessage());
            }
        });
    }
}
