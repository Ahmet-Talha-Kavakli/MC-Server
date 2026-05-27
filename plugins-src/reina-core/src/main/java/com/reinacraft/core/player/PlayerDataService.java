package com.reinacraft.core.player;

import com.reinacraft.core.database.DatabaseManager;
import com.reinacraft.core.rank.Rank;
import com.reinacraft.core.redis.RedisManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataService {

    private static final String RANK_CHANGE_CHANNEL = "reinacraft:rank-change";

    private final Plugin plugin;
    private final DatabaseManager db;
    private final RedisManager redis;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataService(Plugin plugin, DatabaseManager db, RedisManager redis) {
        this.plugin = plugin;
        this.db = db;
        this.redis = redis;

        redis.subscribe(RANK_CHANGE_CHANNEL, (channel, message) -> {
            String[] parts = message.split(":", 2);
            if (parts.length != 2) return;
            UUID uuid;
            try {
                uuid = UUID.fromString(parts[0]);
            } catch (IllegalArgumentException ex) {
                return;
            }
            Rank newRank = Rank.fromId(parts[1]);
            PlayerData data = cache.get(uuid);
            if (data != null) {
                data.setRank(newRank);
            }
        });
    }

    public PlayerData getCached(UUID uuid) {
        return cache.get(uuid);
    }

    public CompletableFuture<PlayerData> loadOrCreate(UUID uuid, String name) {
        PlayerData cached = cache.get(uuid);
        if (cached != null) {
            cached.setName(name);
            cached.setLastLogin(Instant.now());
            return CompletableFuture.completedFuture(cached);
        }
        return CompletableFuture.supplyAsync(() -> loadOrCreateSync(uuid, name));
    }

    private PlayerData loadOrCreateSync(UUID uuid, String name) {
        try (Connection c = db.connection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT name, rank_id, coins, gems, level, xp, first_login, last_login, last_server " +
                            "FROM player_data WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        PlayerData data = new PlayerData(uuid, rs.getString("name"));
                        data.setRank(Rank.fromId(rs.getString("rank_id")));
                        data.setCoins(rs.getLong("coins"));
                        data.setGems(rs.getLong("gems"));
                        data.setLevel(rs.getInt("level"));
                        data.setXp(rs.getLong("xp"));
                        data.setFirstLogin(rs.getTimestamp("first_login").toInstant());
                        data.setLastLogin(Instant.now());
                        data.setLastServer(rs.getString("last_server"));
                        if (!name.equals(data.name())) data.setName(name);
                        data.clearDirty();
                        cache.put(uuid, data);
                        return data;
                    }
                }
            }

            PlayerData fresh = new PlayerData(uuid, name);
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO player_data (uuid, name, rank_id, coins, gems, level, xp, last_server) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ins.setString(1, uuid.toString());
                ins.setString(2, fresh.name());
                ins.setString(3, fresh.rank().name());
                ins.setLong(4, fresh.coins());
                ins.setLong(5, fresh.gems());
                ins.setInt(6, fresh.level());
                ins.setLong(7, fresh.xp());
                ins.setString(8, "hub");
                ins.executeUpdate();
            }
            fresh.clearDirty();
            cache.put(uuid, fresh);
            return fresh;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load player data for " + uuid, ex);
        }
    }

    public CompletableFuture<Void> save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> saveSync(data));
    }

    public void saveSync(PlayerData data) {
        if (!data.clearDirty()) return;
        try (Connection c = db.connection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE player_data SET name = ?, rank_id = ?, coins = ?, gems = ?, level = ?, xp = ?, " +
                             "last_login = ?, last_server = ? WHERE uuid = ?")) {
            ps.setString(1, data.name());
            ps.setString(2, data.rank().name());
            ps.setLong(3, data.coins());
            ps.setLong(4, data.gems());
            ps.setInt(5, data.level());
            ps.setLong(6, data.xp());
            ps.setTimestamp(7, Timestamp.from(data.lastLogin()));
            ps.setString(8, data.lastServer());
            ps.setString(9, data.uuid().toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to save player " + data.uuid() + ": " + ex.getMessage());
        }
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data == null) return;
        // Save synchronously if dirty (player quit, can be off-main-thread)
        if (data.isDirty()) saveSync(data);
    }

    public void flushAllSync() {
        for (PlayerData data : cache.values()) {
            if (data.isDirty()) saveSync(data);
        }
    }

    public CompletableFuture<Void> setRank(UUID uuid, Rank rank) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.setRank(rank);
        }
        return CompletableFuture.runAsync(() -> {
            try (Connection c = db.connection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE player_data SET rank_id = ? WHERE uuid = ?")) {
                ps.setString(1, rank.name());
                ps.setString(2, uuid.toString());
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    // Player has never joined — create row
                    try (PreparedStatement ins = c.prepareStatement(
                            "INSERT IGNORE INTO player_data (uuid, name, rank_id) VALUES (?, ?, ?)")) {
                        String name = Bukkit.getOfflinePlayer(uuid).getName();
                        ins.setString(1, uuid.toString());
                        ins.setString(2, name == null ? uuid.toString() : name);
                        ins.setString(3, rank.name());
                        ins.executeUpdate();
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed to update rank for " + uuid + ": " + ex.getMessage());
            }
            redis.publish(RANK_CHANGE_CHANNEL, uuid + ":" + rank.name());
        });
    }

    public CompletableFuture<PlayerData> loadOfflineByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = db.connection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT uuid FROM player_data WHERE name = ? LIMIT 1")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        return loadOrCreateSync(uuid, name);
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("loadOfflineByName failed: " + ex.getMessage());
            }
            return null;
        });
    }
}
