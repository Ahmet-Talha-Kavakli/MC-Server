package com.reinacraft.core.companion;

import com.reinacraft.core.database.DatabaseManager;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class CompanionService {

    private final Plugin plugin;
    private final DatabaseManager db;
    private final Map<UUID, Set<Companion>> ownedCache = new ConcurrentHashMap<>();
    private final Map<UUID, Companion> equippedCache = new ConcurrentHashMap<>();

    public CompanionService(Plugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public Set<Companion> owned(UUID uuid) {
        return ownedCache.getOrDefault(uuid, EnumSet.noneOf(Companion.class));
    }

    public boolean owns(UUID uuid, Companion c) {
        return owned(uuid).contains(c);
    }

    public Companion equipped(UUID uuid) {
        return equippedCache.get(uuid);
    }

    public void load(UUID uuid) {
        Set<Companion> owned = EnumSet.noneOf(Companion.class);
        Companion equipped = null;
        try (Connection c = db.connection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT companion_id FROM player_companions WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Companion cc = Companion.byId(rs.getString(1));
                        if (cc != null) owned.add(cc);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT companion_id FROM player_companion_equipped WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) equipped = Companion.byId(rs.getString(1));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Companion load failed for " + uuid + ": " + ex.getMessage());
        }
        ownedCache.put(uuid, owned);
        if (equipped != null) equippedCache.put(uuid, equipped);
        else equippedCache.remove(uuid);
    }

    public void unload(UUID uuid) {
        ownedCache.remove(uuid);
        equippedCache.remove(uuid);
    }

    public CompletableFuture<Boolean> grant(UUID uuid, Companion c) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = db.connection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT IGNORE INTO player_companions (uuid, companion_id) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, c.id());
                ps.executeUpdate();
                ownedCache.computeIfAbsent(uuid, k -> EnumSet.noneOf(Companion.class)).add(c);
                return true;
            } catch (SQLException ex) {
                plugin.getLogger().warning("Companion grant failed: " + ex.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Void> equip(UUID uuid, Companion c) {
        if (c == null) equippedCache.remove(uuid);
        else equippedCache.put(uuid, c);
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = db.connection()) {
                if (c == null) {
                    try (PreparedStatement del = conn.prepareStatement(
                            "DELETE FROM player_companion_equipped WHERE uuid = ?")) {
                        del.setString(1, uuid.toString());
                        del.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ups = conn.prepareStatement(
                            "INSERT INTO player_companion_equipped (uuid, companion_id) VALUES (?, ?) " +
                                    "ON DUPLICATE KEY UPDATE companion_id = VALUES(companion_id)")) {
                        ups.setString(1, uuid.toString());
                        ups.setString(2, c.id());
                        ups.executeUpdate();
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("Companion equip persist failed: " + ex.getMessage());
            }
        });
    }
}
