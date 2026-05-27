package com.reinacraft.core.database;

import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class MigrationRunner {

    private final Plugin plugin;
    private final DatabaseManager db;

    MigrationRunner(Plugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    void run() throws SQLException {
        try (Connection c = db.connection()) {
            ensureMetaTable(c);
            List<String> migrations = listMigrations();
            List<String> applied = appliedVersions(c);

            for (String resource : migrations) {
                String version = resource.replaceFirst("V", "").replaceFirst("__.+", "");
                if (applied.contains(version)) continue;
                String sql = loadResource("db/migration/" + resource);
                executeScript(c, sql);
                recordApplied(c, version, resource);
                plugin.getLogger().info("Migration applied: " + resource);
            }
        }
    }

    private void ensureMetaTable(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS schema_history (" +
                    "version VARCHAR(32) NOT NULL PRIMARY KEY," +
                    "script VARCHAR(255) NOT NULL," +
                    "applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    private List<String> appliedVersions(Connection c) throws SQLException {
        List<String> out = new ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM schema_history")) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    private List<String> listMigrations() {
        // Hard-coded ordered list; reflection-free, simple, fine for our scale.
        List<String> out = new ArrayList<>();
        out.add("V1__initial_schema.sql");
        Collections.sort(out);
        return out;
    }

    private String loadResource(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Migration resource not found: " + path);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read migration: " + path, ex);
        }
    }

    private void executeScript(Connection c, String script) throws SQLException {
        for (String statement : script.split(";")) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty()) continue;
            try (Statement st = c.createStatement()) {
                st.execute(trimmed);
            }
        }
    }

    private void recordApplied(Connection c, String version, String script) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO schema_history (version, script) VALUES (?, ?)")) {
            ps.setString(1, version);
            ps.setString(2, script);
            ps.executeUpdate();
        }
    }
}
