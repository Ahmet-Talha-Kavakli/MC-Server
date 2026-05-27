package com.reinacraft.hub;

import com.reinacraft.core.ReinaCore;
import com.reinacraft.core.gui.Menu;
import com.reinacraft.core.gui.MenuBuilder;
import com.reinacraft.hub.listener.ChatListener;
import com.reinacraft.hub.npc.HubNpcs;
import com.reinacraft.hub.ui.ScoreboardManager;
import com.reinacraft.hub.ui.TabListManager;
import com.reinacraft.hub.world.CastleBuilder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public final class HubPlugin extends JavaPlugin implements Listener {

    public static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int SELECTOR_SLOT = 4;

    private Location spawn;
    private ScoreboardManager scoreboardManager;
    private TabListManager tabListManager;
    private HubNpcs hubNpcs;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSpawn();
        buildSpawnPlatformIfNeeded();
        buildCastleIfNeeded();

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);

        scoreboardManager = new ScoreboardManager(this);
        scoreboardManager.start();
        tabListManager = new TabListManager(this);
        tabListManager.start();

        hubNpcs = new HubNpcs(this, ReinaCore.get().npcRegistry());
        getServer().getScheduler().runTaskLater(this, () -> {
            // Wipe any leftover villager/interaction/text_display entities near spawn (from prior boots).
            org.bukkit.World world = spawn.getWorld();
            int cx = spawn.getBlockX() >> 4;
            int cz = spawn.getBlockZ() >> 4;
            for (int dx = -3; dx <= 3; dx++)
                for (int dz = -3; dz <= 3; dz++)
                    world.getChunkAt(cx + dx, cz + dz).load(true);
            int removed = 0;
            for (org.bukkit.entity.Entity e : world.getEntities()) {
                if (spawn.distance(e.getLocation()) > 40) continue;
                if (e.getType() == org.bukkit.entity.EntityType.VILLAGER
                        || e.getType() == org.bukkit.entity.EntityType.INTERACTION
                        || e.getType() == org.bukkit.entity.EntityType.TEXT_DISPLAY) {
                    e.remove();
                    removed++;
                }
            }
            if (removed > 0) getLogger().info("Wiped " + removed + " stale NPC entities before fresh spawn");
            hubNpcs.spawnAll(world, spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
        }, 20L);

        for (World w : Bukkit.getWorlds()) {
            w.setTime(6000);
            w.setStorm(false);
            w.setThundering(false);
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            w.setGameRule(GameRule.DO_FIRE_TICK, false);
            w.setGameRule(GameRule.MOB_GRIEFING, false);
            w.setGameRule(GameRule.KEEP_INVENTORY, true);
            w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
            w.setGameRule(GameRule.NATURAL_REGENERATION, false);
            w.setGameRule(GameRule.FALL_DAMAGE, false);
            w.setGameRule(GameRule.DROWNING_DAMAGE, false);
            w.setGameRule(GameRule.FIRE_DAMAGE, false);
            w.setGameRule(GameRule.DO_INSOMNIA, false);
            w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
            w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        }

        getLogger().info("ReinaHub v" + getPluginMeta().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (hubNpcs != null) hubNpcs.shutdown();
        if (scoreboardManager != null) scoreboardManager.stop();
        if (tabListManager != null) tabListManager.stop();
        getLogger().info("ReinaHub disabled");
    }

    @SuppressWarnings("unused")
    private void cleanupLegacyVillagerNpcs_DISABLED() {
        org.bukkit.NamespacedKey npcKey = new org.bukkit.NamespacedKey("reinacraft", "npc_id");
        int removed = 0;
        if (spawn == null || spawn.getWorld() == null) return;

        org.bukkit.World world = spawn.getWorld();
        // Force-load chunks in a 64×64 area around spawn so we can sweep their entities.
        int cx = spawn.getBlockX() >> 4;
        int cz = spawn.getBlockZ() >> 4;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                world.getChunkAt(cx + dx, cz + dz).load(true);
            }
        }

        for (org.bukkit.entity.Entity e : world.getEntities()) {
            if (spawn.distance(e.getLocation()) > 40) continue;

            // 1) Anything tagged with our PDC marker
            if (e.getPersistentDataContainer().has(npcKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                e.remove();
                removed++;
                continue;
            }
            // 2) Stale villagers, interactions, or text displays near spawn (pre-Citizens build leftovers).
            //    Citizens uses NPC entities (real Player-type) and registers them via its own metadata —
            //    a true Citizens NPC has CitizensAPI.getNPCRegistry().isNPC(entity) true.
            if (e.getType() == org.bukkit.entity.EntityType.VILLAGER
                    || e.getType() == org.bukkit.entity.EntityType.INTERACTION
                    || e.getType() == org.bukkit.entity.EntityType.TEXT_DISPLAY) {
                e.remove();
                removed++;
            }
        }
        if (removed > 0) getLogger().info("Cleaned up " + removed + " legacy NPC entities near spawn");
        else getLogger().info("No legacy NPC entities found near spawn");
    }

    private void buildCastleIfNeeded() {
        FileConfiguration cfg = getConfig();
        if (cfg.getBoolean("hub.castle-built", false)) return;
        CastleBuilder.build(spawn.getWorld(), spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
        cfg.set("hub.castle-built", true);
        saveConfig();
        getLogger().info("Castle built around spawn @ " + spawn.getBlockX() + "," + spawn.getBlockY() + "," + spawn.getBlockZ());
    }

    private void loadSpawn() {
        FileConfiguration cfg = getConfig();
        String worldName = cfg.getString("spawn.world", "hub");
        World world = Bukkit.getWorld(worldName);
        if (world == null) world = Bukkit.getWorlds().get(0);
        spawn = new Location(
                world,
                cfg.getDouble("spawn.x", 0.5),
                cfg.getDouble("spawn.y", 80.0),
                cfg.getDouble("spawn.z", 0.5),
                (float) cfg.getDouble("spawn.yaw", 0.0),
                (float) cfg.getDouble("spawn.pitch", 0.0)
        );
        world.setSpawnLocation(spawn);
    }

    private void buildSpawnPlatformIfNeeded() {
        FileConfiguration cfg = getConfig();
        if (cfg.getBoolean("hub.platform-built", false)) return;

        World world = spawn.getWorld();
        int cx = spawn.getBlockX();
        int cz = spawn.getBlockZ();
        int floorY = spawn.getBlockY() - 1;

        int radius = 7;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distSq = x * x + z * z;
                if (distSq > radius * radius) continue;
                int chebyshev = Math.max(Math.abs(x), Math.abs(z));

                Material floorMat;
                if (chebyshev == radius || chebyshev == radius - 1) {
                    floorMat = ((x + z) & 1) == 0 ? Material.GOLD_BLOCK : Material.REDSTONE_BLOCK;
                } else if (chebyshev == 0) {
                    floorMat = Material.SEA_LANTERN;
                } else if (chebyshev == 1) {
                    floorMat = Material.WHITE_STAINED_GLASS;
                } else if (chebyshev == 2) {
                    floorMat = Material.LIGHT_BLUE_STAINED_GLASS;
                } else if (chebyshev == 3) {
                    floorMat = Material.PINK_STAINED_GLASS;
                } else {
                    floorMat = Material.WHITE_STAINED_GLASS;
                }

                world.getBlockAt(cx + x, floorY, cz + z).setType(floorMat, false);

                for (int yClear = 0; yClear < 4; yClear++) {
                    Block above = world.getBlockAt(cx + x, floorY + 1 + yClear, cz + z);
                    if (above.getType() != Material.AIR) above.setType(Material.AIR, false);
                }
            }
        }

        for (int[] corner : new int[][]{{-radius, -radius}, {-radius, radius}, {radius, -radius}, {radius, radius}}) {
            int x = corner[0], z = corner[1];
            world.getBlockAt(cx + x, floorY + 1, cz + z).setType(Material.GOLD_BLOCK, false);
            world.getBlockAt(cx + x, floorY + 2, cz + z).setType(Material.SEA_LANTERN, false);
            world.getBlockAt(cx + x, floorY + 3, cz + z).setType(Material.GOLD_BLOCK, false);
        }

        cfg.set("hub.platform-built", true);
        saveConfig();
        getLogger().info("Spawn platform built at " + cx + "," + floorY + "," + cz + " (radius " + radius + ")");
    }

    private void saveSpawn(Location loc) {
        spawn = loc.clone();
        FileConfiguration cfg = getConfig();
        cfg.set("spawn.world", loc.getWorld().getName());
        cfg.set("spawn.x", loc.getX());
        cfg.set("spawn.y", loc.getY());
        cfg.set("spawn.z", loc.getZ());
        cfg.set("spawn.yaw", (double) loc.getYaw());
        cfg.set("spawn.pitch", (double) loc.getPitch());
        saveConfig();
        loc.getWorld().setSpawnLocation(loc);
    }

    private void resetPlayer(Player p) {
        p.setGameMode(GameMode.ADVENTURE);
        p.setAllowFlight(true);
        p.setFlying(false);
        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setExp(0f);
        p.setLevel(0);
        p.getInventory().clear();
        p.getInventory().setItem(SELECTOR_SLOT, createSelectorItem());
        p.getInventory().setHeldItemSlot(SELECTOR_SLOT);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        e.joinMessage(null);

        p.teleport(spawn);
        resetPlayer(p);
        scoreboardManager.register(p);
        tabListManager.apply(p);

        p.sendMessage(MM.deserialize(
                "\n<dark_gray><st>                                        </st>\n" +
                        "<gradient:#FF1744:#FFD700><bold>          R E I N A C R A F T          </bold></gradient>\n" +
                        "\n" +
                        "<gray>Hoş geldin <gradient:#FFD700:#FF1744><bold>" + p.getName() + "</bold></gradient>!\n" +
                        "<gray>Oyun seçmek için <gold>Pusulayı</gold> kullan.\n" +
                        "<dark_gray><st>                                        </st>\n"
        ));

        p.showTitle(Title.title(
                MM.deserialize("<gradient:#FF1744:#FFD700><bold>ReinaCraft</bold></gradient>"),
                MM.deserialize("<gray>Kraliçenin Diyarı")
        ));

        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.0f);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.quitMessage(null);
        if (scoreboardManager != null) scoreboardManager.unregister(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        e.setCancelled(true);
        if (e.getEntity() instanceof Player p) {
            p.setFoodLevel(20);
            p.setSaturation(20f);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (!e.getPlayer().hasPermission("reinahub.build")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        if (!e.getPlayer().hasPermission("reinahub.build")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!e.getPlayer().hasPermission("reinahub.build")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().hasPermission("reinahub.build")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeather(WeatherChangeEvent e) {
        if (e.toWeatherState()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.COMPASS) return;
        e.setCancelled(true);
        openSelector(e.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        // Prevent inventory manipulation in hub world unless builder.
        // Selector menu clicks are handled by reina-core's MenuListener (its handler runs first).
        if (e.getInventory().getHolder() instanceof com.reinacraft.core.gui.MenuHolder) return;
        if (p.getWorld().equals(spawn.getWorld()) && !p.hasPermission("reinahub.build")) {
            e.setCancelled(true);
        }
    }

    private void openSelector(Player p) {
        Menu menu = MenuBuilder.of("<dark_gray>» <gradient:#FF1744:#FFD700>Oyun Seçimi</gradient> <dark_gray>«")
                .rows(3)
                .item(11, Material.RED_BED,
                        "<gradient:#FF1744:#FFD700><bold>BedWars</bold></gradient>",
                        List.of(
                                "<gray>Yatağını koru, düşmanlarınkini yık!",
                                "<gray>Solo • Doubles • Trios • Squads",
                                "",
                                "<dark_gray>● <gray>Durum: <green>Açık",
                                "",
                                "<yellow>▶ Tıkla ve katıl!"
                        ),
                        (player, click) -> {
                            player.closeInventory();
                            sendToServer(player, "bedwars");
                        })
                .item(13, Material.NETHER_STAR,
                        "<gold><bold>Hub</bold></gold>",
                        List.of(
                                "<gray>ReinaCraft Lobby",
                                "",
                                "<dark_gray>● <gray>Zaten buradasın!"
                        ),
                        (player, click) -> {
                            player.closeInventory();
                            player.sendMessage(MM.deserialize("<gray>Zaten <gold>Hub</gold>'dasın!"));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.0f);
                        })
                .item(15, Material.BARRIER,
                        "<dark_gray><bold>Yakında...</bold>",
                        List.of(
                                "<gray>SkyWars, MurderMystery,",
                                "<gray>BuildBattle ve daha fazlası",
                                "<gray>yakında geliyor!"
                        ),
                        (player, click) -> {
                            player.sendMessage(MM.deserialize("<gray>Bu oyun modu <gold>yakında</gold> eklenecek!"));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
                        })
                .build();
        menu.open(p);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    private ItemStack createSelectorItem() {
        return MenuBuilder.namedItem(Material.COMPASS,
                "<gradient:#FF1744:#FFD700><bold>Oyun Seçici</bold></gradient> <dark_gray>(Sağ tık)",
                List.of(
                        "<gray>ReinaCraft minigame'lerini",
                        "<gray>buradan seçebilirsin!",
                        "",
                        "<yellow>▶ Sağ tıkla"
                ));
    }

    private void sendToServer(Player p, String server) {
        p.sendMessage(MM.deserialize("<gray>Geçiliyor: <gold>" + server + "</gold>..."));
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(stream)) {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (IOException ex) {
            getLogger().severe("Failed to encode server transfer: " + ex.getMessage());
            return;
        }
        p.sendPluginMessage(this, "BungeeCord", stream.toByteArray());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(MM.deserialize("<red>Bu komut sadece oyuncular için."));
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "spawn" -> {
                p.teleport(spawn);
                resetPlayer(p);
                p.sendMessage(MM.deserialize("<gray>Spawn'a ışınlandın."));
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
                return true;
            }
            case "setspawn" -> {
                if (!p.hasPermission("reinahub.setspawn")) {
                    p.sendMessage(MM.deserialize("<red>Yetkin yok."));
                    return true;
                }
                saveSpawn(p.getLocation());
                p.sendMessage(MM.deserialize("<green>Spawn ayarlandı: <gray>" +
                        String.format("%.1f, %.1f, %.1f", p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ())));
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);
                return true;
            }
            case "selector" -> {
                openSelector(p);
                return true;
            }
        }
        return false;
    }
}
