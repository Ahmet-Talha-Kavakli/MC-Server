package com.reinacraft.hub.npc;

import com.reinacraft.core.ReinaCore;
import com.reinacraft.core.gui.MenuBuilder;
import com.reinacraft.core.npc.HubNpc;
import com.reinacraft.core.npc.NpcRegistry;
import com.reinacraft.core.player.PlayerData;
import com.reinacraft.core.rank.Rank;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public final class HubNpcs {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Plugin plugin;
    private final NpcRegistry registry;
    private BukkitTask hologramTask;

    public HubNpcs(Plugin plugin, NpcRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void spawnAll(World world, int cx, int cy, int cz) {
        registry.shutdown();

        Location bedwarsLoc  = new Location(world, cx + 11.5, cy, cz + 0.5,  -90, 0);
        Location cosmeticLoc = new Location(world, cx - 11.5, cy, cz + 0.5,   90, 0);
        Location profileLoc  = new Location(world, cx + 0.5,  cy, cz + 11.5, 180, 0);
        Location tutorialLoc = new Location(world, cx + 0.5,  cy, cz - 11.5,   0, 0);

        registry.register(new HubNpc(plugin, "bedwars",
                bedwarsLoc, Villager.Profession.WEAPONSMITH,
                bedwarsHologram(),
                (player, hand) -> sendToServer(player, "bedwars")));

        registry.register(new HubNpc(plugin, "cosmetic",
                cosmeticLoc, Villager.Profession.LEATHERWORKER,
                cosmeticHologram(),
                (player, hand) -> openCosmeticMenu(player)));

        registry.register(new HubNpc(plugin, "profile",
                profileLoc, Villager.Profession.LIBRARIAN,
                profileHologram(),
                (player, hand) -> openProfile(player)));

        registry.register(new HubNpc(plugin, "tutorial",
                tutorialLoc, Villager.Profession.CLERIC,
                tutorialHologram(),
                (player, hand) -> openTutorial(player)));

        startHologramAnimation();
    }

    public void shutdown() {
        if (hologramTask != null) { hologramTask.cancel(); hologramTask = null; }
        registry.shutdown();
    }

    private void startHologramAnimation() {
        if (hologramTask != null) hologramTask.cancel();
        hologramTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            HubNpc bw = registry.get("bedwars");
            if (bw != null) bw.updateHologram(bedwarsHologram());

            // Themed NPC particle pulse — every tick that this runs (1 Hz).
            if (Bukkit.getOnlinePlayers().isEmpty()) return;
            for (String id : new String[]{"bedwars", "cosmetic", "profile", "tutorial"}) {
                HubNpc npc = registry.get(id);
                if (npc != null && npc.location().getWorld() != null) {
                    com.reinacraft.hub.world.HubAtmosphere.pulseNpc(
                            npc.location().getWorld(),
                            npc.location(),
                            id);
                }
            }
        }, 0L, 20L);
    }

    // ---- Hologram builders ----

    private List<String> bedwarsHologram() {
        int online = Bukkit.getOnlinePlayers().size();
        return List.of(
                "<gradient:#FF1744:#FFD700><bold>BedWars</bold></gradient>",
                "<gray>Yatağını koru, düşmanlarınkini yık!",
                "<dark_gray>● <yellow>" + online + " <gray>oyuncu çevrimiçi",
                "<yellow>▶ Sağ tıkla & Katıl"
        );
    }
    private List<String> cosmeticHologram() {
        return List.of(
                "<gradient:#9B59B6:#1ABC9C><bold>Cosmetic Shop</bold></gradient>",
                "<gray>Pet • Trail • Gadget",
                "<aqua>❖ <gray>Gem ile satın al",
                "<yellow>▶ Sağ tıkla"
        );
    }
    private List<String> profileHologram() {
        return List.of(
                "<gradient:#5DADE2:#48D1CC><bold>Profil & Stats</bold></gradient>",
                "<gray>Rank, level, coin, gem",
                "<yellow>▶ Sağ tıkla"
        );
    }
    private List<String> tutorialHologram() {
        return List.of(
                "<gradient:#27AE60:#F1C40F><bold>Yardım & Tutorial</bold></gradient>",
                "<gray>Nasıl oynanır, komutlar",
                "<yellow>▶ Sağ tıkla"
        );
    }

    // ---- Interaction handlers ----

    private void sendToServer(Player p, String server) {
        p.sendMessage(MM.deserialize("<gray>Geçiliyor: <gold>" + server + "</gold>..."));
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(stream)) {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (IOException ex) {
            plugin.getLogger().severe("transfer encode failed: " + ex.getMessage());
            return;
        }
        p.sendPluginMessage(plugin, "BungeeCord", stream.toByteArray());
    }

    private void openCosmeticMenu(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
        MenuBuilder.of("<dark_gray>» <gradient:#9B59B6:#1ABC9C>Cosmetic Shop</gradient> <dark_gray>«")
                .rows(5)
                .item(11, Material.WOLF_SPAWN_EGG, "<gradient:#FF8A65:#FFD700><bold>Pet'ler</bold></gradient>",
                        List.of("<gray>Yanında dolaşan arkadaşlar", "", "<dark_gray>● <gray>Mevcut: <yellow>Yakında"),
                        (pl, click) -> pl.sendMessage(MM.deserialize("<gray>Pet sistemi <gold>yakında</gold>!")))
                .item(13, Material.FIREWORK_ROCKET, "<gradient:#48D1CC:#FFEA00><bold>Trails</bold></gradient>",
                        List.of("<gray>Arkanda partikül izleri", "", "<dark_gray>● <gray>Mevcut: <yellow>Yakında"),
                        (pl, click) -> pl.sendMessage(MM.deserialize("<gray>Trail sistemi <gold>yakında</gold>!")))
                .item(15, Material.HEART_OF_THE_SEA, "<gradient:#D500F9:#FF1744><bold>Gadgets</bold></gradient>",
                        List.of("<gray>Eğlenceli yan aletler", "", "<dark_gray>● <gray>Mevcut: <yellow>Yakında"),
                        (pl, click) -> pl.sendMessage(MM.deserialize("<gray>Gadget sistemi <gold>yakında</gold>!")))
                .item(31, Material.BARRIER, "<red>Kapat", (pl, click) -> pl.closeInventory())
                .build().open(p);
    }

    private void openProfile(Player p) {
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
        PlayerData data = ReinaCore.get().playerData().getCached(p.getUniqueId());
        Rank rank = data != null ? data.rank() : Rank.MEMBER;
        long coins = data != null ? data.coins() : 0;
        long gems = data != null ? data.gems() : 0;
        int level = data != null ? data.level() : 1;
        long xp = data != null ? data.xp() : 0;
        MenuBuilder.of("<dark_gray>» <gradient:#5DADE2:#48D1CC>" + p.getName() + " - Profil</gradient> <dark_gray>«")
                .rows(5)
                .item(13, Material.PLAYER_HEAD, "<gradient:#FFD700:#FF1744><bold>" + p.getName() + "</bold></gradient>",
                        List.of("<gray>Rank: <" + (rank == Rank.MEMBER ? "#AAAAAA" : "white") + ">" + (rank == Rank.MEMBER ? "Üye" : rank.name()),
                                "<gray>Level: <yellow>" + level + " <dark_gray>(<gray>" + xp + " XP<dark_gray>)"),
                        (pl, click) -> {})
                .item(20, Material.GOLD_INGOT, "<gold><bold>Coins</bold></gold>",
                        List.of("<gray>Bakiye: <gold>" + coins + " ⛁"),
                        (pl, click) -> {})
                .item(22, Material.EMERALD, "<gradient:#1ABC9C:#FFD700><bold>Gems</bold></gradient>",
                        List.of("<gray>Bakiye: <aqua>" + gems + " ❖"),
                        (pl, click) -> {})
                .item(24, Material.RED_BED, "<gradient:#FF1744:#FFD700><bold>BedWars</bold></gradient>",
                        List.of("<gray>Win: <yellow>0", "<gray>Kill: <yellow>0", "<gray>Bed Broken: <yellow>0"),
                        (pl, click) -> {})
                .item(40, Material.BARRIER, "<red>Kapat", (pl, click) -> pl.closeInventory())
                .build().open(p);
    }

    private void openTutorial(Player p) {
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
        MenuBuilder.of("<dark_gray>» <gradient:#27AE60:#F1C40F>Yardım & Tutorial</gradient> <dark_gray>«")
                .rows(5)
                .item(11, Material.COMPASS, "<gradient:#FF1744:#FFD700><bold>Oyun Seçimi</bold></gradient>",
                        List.of("<gray>• Pusula", "<gray>• Spawn'daki NPC", "<gray>• /server bedwars"),
                        (pl, click) -> {})
                .item(13, Material.WRITABLE_BOOK, "<gradient:#48D1CC:#5DADE2><bold>Komutlar</bold></gradient>",
                        List.of("<gold>/spawn", "<gold>/selector", "<gold>/server"),
                        (pl, click) -> {})
                .item(15, Material.DIAMOND, "<gradient:#D500F9:#FF1744><bold>Rank</bold></gradient>",
                        List.of("<gray>VIP → MVP → MVP+ → OWNER"),
                        (pl, click) -> {})
                .item(40, Material.BARRIER, "<red>Kapat", (pl, click) -> pl.closeInventory())
                .build().open(p);
    }
}
