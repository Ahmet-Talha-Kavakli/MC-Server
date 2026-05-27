package com.reinacraft.core;

import com.reinacraft.core.command.EconomyCommands;
import com.reinacraft.core.command.RankCommand;
import com.reinacraft.core.database.DatabaseManager;
import com.reinacraft.core.economy.EconomyService;
import com.reinacraft.core.gui.MenuListener;
import com.reinacraft.core.listener.PlayerSessionListener;
import com.reinacraft.core.npc.NpcListener;
import com.reinacraft.core.npc.NpcRegistry;
import com.reinacraft.core.player.PlayerDataService;
import com.reinacraft.core.redis.RedisManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReinaCore extends JavaPlugin {

    private static ReinaCore instance;

    private DatabaseManager database;
    private RedisManager redis;
    private PlayerDataService playerData;
    private EconomyService economy;
    private NpcRegistry npcRegistry;
    private String serverId;
    private String serverDisplayName;

    public static ReinaCore get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();

        serverId = cfg.getString("server.id", "hub");
        serverDisplayName = cfg.getString("server.display-name", "HUB");

        try {
            database = new DatabaseManager(this, cfg.getConfigurationSection("database"));
            database.start();
            database.migrate();
        } catch (Throwable ex) {
            getLogger().severe("Database init failed: " + ex.getMessage());
            ex.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            redis = new RedisManager(this, cfg.getConfigurationSection("redis"));
            redis.start();
        } catch (Exception ex) {
            getLogger().severe("Redis init failed: " + ex.getMessage());
            ex.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        playerData = new PlayerDataService(this, database, redis);
        economy = new EconomyService(this, database, playerData, serverId);
        npcRegistry = new NpcRegistry(this);

        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this, playerData), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new NpcListener(), this);

        new RankCommand(this, playerData).register();
        new EconomyCommands(this, economy).register();

        getLogger().info("ReinaCore v" + getPluginMeta().getVersion()
                + " enabled (server=" + serverId + ", display=" + serverDisplayName + ")");
    }

    @Override
    public void onDisable() {
        if (npcRegistry != null) npcRegistry.shutdown();
        if (playerData != null) playerData.flushAllSync();
        if (redis != null) redis.stop();
        if (database != null) database.stop();
        instance = null;
        getLogger().info("ReinaCore disabled");
    }

    public DatabaseManager database() { return database; }
    public RedisManager redis() { return redis; }
    public PlayerDataService playerData() { return playerData; }
    public EconomyService economy() { return economy; }
    public NpcRegistry npcRegistry() { return npcRegistry; }
    public String serverId() { return serverId; }
    public String serverDisplayName() { return serverDisplayName; }
}
