package com.reinacraft.core.economy;

import com.reinacraft.core.database.DatabaseManager;
import com.reinacraft.core.player.PlayerData;
import com.reinacraft.core.player.PlayerDataService;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class EconomyService {

    public enum Currency { COIN, GEM }

    private final Plugin plugin;
    private final DatabaseManager db;
    private final PlayerDataService players;
    private final String serverId;

    public EconomyService(Plugin plugin, DatabaseManager db, PlayerDataService players, String serverId) {
        this.plugin = plugin;
        this.db = db;
        this.players = players;
        this.serverId = serverId;
    }

    public long balance(UUID uuid, Currency currency) {
        PlayerData data = players.getCached(uuid);
        if (data == null) return 0L;
        return switch (currency) {
            case COIN -> data.coins();
            case GEM -> data.gems();
        };
    }

    public CompletableFuture<Long> give(UUID uuid, Currency currency, long amount, String reason) {
        return mutate(uuid, currency, amount, reason);
    }

    public CompletableFuture<Long> take(UUID uuid, Currency currency, long amount, String reason) {
        return mutate(uuid, currency, -Math.abs(amount), reason);
    }

    public CompletableFuture<Long> set(UUID uuid, Currency currency, long amount, String reason) {
        PlayerData data = players.getCached(uuid);
        if (data == null) return CompletableFuture.completedFuture(0L);
        long current = currency == Currency.COIN ? data.coins() : data.gems();
        return mutate(uuid, currency, amount - current, reason);
    }

    private CompletableFuture<Long> mutate(UUID uuid, Currency currency, long delta, String reason) {
        PlayerData data = players.getCached(uuid);
        if (data == null) return CompletableFuture.completedFuture(0L);

        long newBalance;
        switch (currency) {
            case COIN -> {
                newBalance = Math.max(0L, data.coins() + delta);
                data.setCoins(newBalance);
            }
            case GEM -> {
                newBalance = Math.max(0L, data.gems() + delta);
                data.setGems(newBalance);
            }
            default -> { return CompletableFuture.completedFuture(0L); }
        }

        final long balanceFinal = newBalance;
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = db.connection();
                 PreparedStatement ps = c.prepareStatement(
                         "INSERT INTO economy_transactions (uuid, currency, delta, balance_after, reason, server) " +
                                 "VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, currency.name());
                ps.setLong(3, delta);
                ps.setLong(4, balanceFinal);
                ps.setString(5, reason);
                ps.setString(6, serverId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().warning("Economy transaction log failed: " + ex.getMessage());
            }
            return balanceFinal;
        });
    }
}
