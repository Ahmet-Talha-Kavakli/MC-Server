package com.reinacraft.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public final class DatabaseManager {

    private final Plugin plugin;
    private final ConfigurationSection cfg;
    private HikariDataSource dataSource;

    public DatabaseManager(Plugin plugin, ConfigurationSection cfg) {
        this.plugin = plugin;
        this.cfg = Objects.requireNonNull(cfg, "database section missing in config.yml");
    }

    public void start() {
        String host = cfg.getString("host", "127.0.0.1");
        int port = cfg.getInt("port", 3306);
        String db = cfg.getString("database", "reinacraft");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci"
                + "&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(url);
        hc.setUsername(cfg.getString("username", "reinacraft"));
        hc.setPassword(cfg.getString("password", ""));
        hc.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hc.setMaximumPoolSize(cfg.getInt("pool.maximum-size", 10));
        hc.setMinimumIdle(cfg.getInt("pool.minimum-idle", 2));
        hc.setConnectionTimeout(cfg.getLong("pool.connection-timeout-ms", 10_000L));
        hc.setIdleTimeout(cfg.getLong("pool.idle-timeout-ms", 600_000L));
        hc.setMaxLifetime(cfg.getLong("pool.max-lifetime-ms", 1_800_000L));
        hc.setPoolName("ReinaCraft-Hikari");
        hc.setLeakDetectionThreshold(30_000L);

        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            this.dataSource = new HikariDataSource(hc);
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }

        try (Connection c = dataSource.getConnection()) {
            plugin.getLogger().info("Database connected: " + c.getMetaData().getURL());
        } catch (SQLException ex) {
            throw new IllegalStateException("Database connectivity check failed", ex);
        }
    }

    public void migrate() throws SQLException {
        new MigrationRunner(plugin, this).run();
        plugin.getLogger().info("Database migrations checked");
    }

    public void stop() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }
}
