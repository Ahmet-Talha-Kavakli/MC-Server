package com.reinacraft.hub.npc;

import com.reinacraft.core.ReinaCore;
import com.reinacraft.core.companion.Companion;
import com.reinacraft.core.companion.CompanionManager;
import com.reinacraft.core.companion.CompanionService;
import com.reinacraft.core.cosmetic.Cosmetic;
import com.reinacraft.core.cosmetic.CosmeticCategory;
import com.reinacraft.core.cosmetic.CosmeticService;
import com.reinacraft.core.economy.EconomyService;
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
                .item(10, Material.NAME_TAG, "<gradient:#FFD700:#E89028><bold>Companions</bold></gradient>",
                        List.of("<gray>Özel 3D modellerle",
                                "<gray>seni takip eden arkadaşlar",
                                "",
                                "<dark_gray>● <gray>Toplam: <yellow>" + Companion.values().length + " companion",
                                "",
                                "<yellow>▶ Tıkla ve gör"),
                        (pl, click) -> openCompanionMenu(pl))
                .item(12, Material.WOLF_SPAWN_EGG, "<gradient:#FF8A65:#FFD700><bold>Pet'ler</bold></gradient>",
                        List.of("<gray>Vanilla mob arkadaşlar",
                                "",
                                "<dark_gray>● <gray>Toplam: <yellow>6 pet",
                                "",
                                "<yellow>▶ Tıkla ve gör"),
                        (pl, click) -> openCategoryMenu(pl, CosmeticCategory.PET))
                .item(14, Material.FIREWORK_ROCKET, "<gradient:#48D1CC:#FFEA00><bold>Trails</bold></gradient>",
                        List.of("<gray>Arkanda partikül izleri",
                                "",
                                "<dark_gray>● <gray>Toplam: <yellow>6 trail",
                                "",
                                "<yellow>▶ Tıkla ve gör"),
                        (pl, click) -> openCategoryMenu(pl, CosmeticCategory.TRAIL))
                .item(16, Material.HEART_OF_THE_SEA, "<gradient:#D500F9:#FF1744><bold>Gadgets</bold></gradient>",
                        List.of("<gray>Eğlenceli yan aletler",
                                "",
                                "<dark_gray>● <gray>Toplam: <yellow>6 gadget",
                                "",
                                "<yellow>▶ Tıkla ve gör"),
                        (pl, click) -> openCategoryMenu(pl, CosmeticCategory.GADGET))
                .item(31, Material.BARRIER, "<red>Kapat", (pl, click) -> pl.closeInventory())
                .build().open(p);
    }

    private void openCompanionMenu(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
        CompanionService cs = ReinaCore.get().companionService();
        long gems = ReinaCore.get().playerData().getCached(p.getUniqueId()) != null
                ? ReinaCore.get().playerData().getCached(p.getUniqueId()).gems() : 0;

        MenuBuilder mb = MenuBuilder.of("<dark_gray>» <gradient:#FFD700:#E89028>Companions</gradient> <dark_gray>«").rows(6);

        // Layout: 16 companions in rows 1-3 (slots 10-16, 19-25, 28-34)
        int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29};
        Companion[] all = Companion.values();
        for (int i = 0; i < all.length && i < slots.length; i++) {
            Companion cc = all[i];
            boolean owned = cs.owns(p.getUniqueId(), cc);
            Companion eq = cs.equipped(p.getUniqueId());
            boolean isEquipped = owned && eq == cc;

            List<String> lore = new java.util.ArrayList<>();
            lore.add(cc.rawDescriptionMm());
            lore.add("");
            if (isEquipped) {
                lore.add("<green><bold>✓ KULLANILIYOR</bold></green>");
                lore.add("");
                lore.add("<yellow>▶ Tıkla, çıkar");
            } else if (owned) {
                lore.add("<aqua>✓ <gray>Sahipsin");
                lore.add("");
                lore.add("<yellow>▶ Tıkla, kullan");
            } else {
                lore.add("<gray>Fiyat: <aqua>" + cc.gemPrice() + " ❖");
                lore.add("<dark_gray>Bakiyen: <aqua>" + gems + " ❖");
                lore.add("");
                if (gems >= cc.gemPrice()) lore.add("<yellow>▶ Tıkla, satın al");
                else lore.add("<red>✗ Yetersiz gem");
            }

            final Companion captured = cc;
            mb.item(slots[i], Material.NAME_TAG, cc.rawDisplayMm(), lore,
                    (pl, click) -> handleCompanionClick(pl, captured));
        }

        mb.item(45, Material.ARROW, "<gray>← Geri", (pl, click) -> openCosmeticMenu(pl))
          .item(49, Material.BARRIER, "<red>Kapat", (pl, click) -> pl.closeInventory())
          .build().open(p);
    }

    private void handleCompanionClick(Player p, Companion cc) {
        CompanionService cs = ReinaCore.get().companionService();
        CompanionManager cm = ReinaCore.get().companionManager();
        EconomyService economy = ReinaCore.get().economy();
        boolean owned = cs.owns(p.getUniqueId(), cc);
        Companion eq = cs.equipped(p.getUniqueId());

        if (owned && eq == cc) {
            cs.equip(p.getUniqueId(), null);
            cm.dismiss(p.getUniqueId());
            p.sendMessage(MM.deserialize("<yellow>" + cc.rawDisplayMm() + " <gray>gönderildi."));
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.9f);
            openCompanionMenu(p);
            return;
        }
        if (owned) {
            cs.equip(p.getUniqueId(), cc);
            cm.summon(p, cc);
            p.sendMessage(MM.deserialize("<green>" + cc.rawDisplayMm() + " <gray>çağırıldı!"));
            openCompanionMenu(p);
            return;
        }
        long balance = economy.balance(p.getUniqueId(), EconomyService.Currency.GEM);
        if (balance < cc.gemPrice()) {
            p.sendMessage(MM.deserialize("<red>Yetersiz gem. <gray>Gerekli: <aqua>" + cc.gemPrice() + " ❖"));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.7f);
            return;
        }
        economy.take(p.getUniqueId(), EconomyService.Currency.GEM, cc.gemPrice(), "companion:" + cc.id())
                .thenCompose(newBal -> cs.grant(p.getUniqueId(), cc))
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    p.sendMessage(MM.deserialize("<green>Satın aldın: " + cc.rawDisplayMm()));
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
                    cs.equip(p.getUniqueId(), cc);
                    cm.summon(p, cc);
                    openCompanionMenu(p);
                }));
    }

    private void openCategoryMenu(Player p, CosmeticCategory category) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
        CosmeticService cosmetics = ReinaCore.get().cosmetics();
        EconomyService economy = ReinaCore.get().economy();
        long gems = ReinaCore.get().playerData().getCached(p.getUniqueId()) != null
                ? ReinaCore.get().playerData().getCached(p.getUniqueId()).gems() : 0;

        String catName = switch (category) {
            case PET -> "Pet'ler";
            case TRAIL -> "Trails";
            case GADGET -> "Gadgets";
        };
        String catGradient = switch (category) {
            case PET -> "<gradient:#FF8A65:#FFD700>";
            case TRAIL -> "<gradient:#48D1CC:#FFEA00>";
            case GADGET -> "<gradient:#D500F9:#FF1744>";
        };

        MenuBuilder mb = MenuBuilder.of("<dark_gray>» " + catGradient + catName + "</gradient> <dark_gray>«").rows(5);

        // Layout: 6 items in slots 10-15 with surrounding glass
        int[] slots = {10, 12, 14, 11, 13, 15};
        int idx = 0;
        for (Cosmetic cos : Cosmetic.values()) {
            if (cos.category() != category) continue;
            if (idx >= slots.length) break;

            boolean owned = cosmetics.owns(p.getUniqueId(), cos);
            Cosmetic eq = cosmetics.equipped(p.getUniqueId(), category);
            boolean isEquipped = owned && eq == cos;

            List<String> lore = new java.util.ArrayList<>();
            lore.add(cos.rawDescriptionMm());
            lore.add("");
            if (isEquipped) {
                lore.add("<green><bold>✓ KULLANILIYOR</bold></green>");
                lore.add("");
                lore.add("<yellow>▶ Tıkla, çıkar");
            } else if (owned) {
                lore.add("<aqua>✓ <gray>Sahipsin");
                lore.add("");
                lore.add("<yellow>▶ Tıkla, kullan");
            } else {
                lore.add("<gray>Fiyat: <aqua>" + cos.gemPrice() + " ❖");
                lore.add("<dark_gray>Bakiyen: <aqua>" + gems + " ❖");
                lore.add("");
                if (gems >= cos.gemPrice()) {
                    lore.add("<yellow>▶ Tıkla, satın al");
                } else {
                    lore.add("<red>✗ Yetersiz gem");
                }
            }

            final Cosmetic captured = cos;
            mb.item(slots[idx], cos.icon(), cos.rawDisplayMm(), lore,
                    (pl, click) -> handleCosmeticClick(pl, captured, category));
            idx++;
        }

        mb.item(36, Material.ARROW, "<gray>← Geri", (pl, click) -> openCosmeticMenu(pl))
          .item(40, Material.BARRIER, "<red>Kapat", (pl, click) -> pl.closeInventory())
          .build().open(p);
    }

    private void handleCosmeticClick(Player p, Cosmetic cos, CosmeticCategory category) {
        CosmeticService cosmetics = ReinaCore.get().cosmetics();
        EconomyService economy = ReinaCore.get().economy();
        boolean owned = cosmetics.owns(p.getUniqueId(), cos);
        Cosmetic eq = cosmetics.equipped(p.getUniqueId(), category);

        if (owned && eq == cos) {
            // Unequip
            cosmetics.equip(p.getUniqueId(), category, null);
            p.sendMessage(MM.deserialize("<yellow>" + cos.rawDisplayMm() + " <gray>çıkarıldı."));
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.9f);
            applyImmediately(p, category, null);
            openCategoryMenu(p, category);
            return;
        }
        if (owned) {
            // Equip
            cosmetics.equip(p.getUniqueId(), category, cos);
            p.sendMessage(MM.deserialize("<green>" + cos.rawDisplayMm() + " <gray>kullanılıyor!"));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);
            applyImmediately(p, category, cos);
            openCategoryMenu(p, category);
            return;
        }
        // Purchase
        long balance = economy.balance(p.getUniqueId(), EconomyService.Currency.GEM);
        if (balance < cos.gemPrice()) {
            p.sendMessage(MM.deserialize("<red>Yetersiz gem. <gray>Gerekli: <aqua>" + cos.gemPrice() + " ❖"));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.7f);
            return;
        }
        economy.take(p.getUniqueId(), EconomyService.Currency.GEM, cos.gemPrice(), "cosmetic:" + cos.id())
                .thenCompose(newBal -> cosmetics.grant(p.getUniqueId(), cos))
                .thenRun(() -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        p.sendMessage(MM.deserialize("<green>Satın aldın: " + cos.rawDisplayMm()));
                        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
                        // Auto-equip on first purchase
                        cosmetics.equip(p.getUniqueId(), category, cos);
                        applyImmediately(p, category, cos);
                        openCategoryMenu(p, category);
                    });
                });
    }

    private void applyImmediately(Player p, CosmeticCategory category, Cosmetic cos) {
        if (category == CosmeticCategory.PET) {
            ReinaCore.get().cosmeticEffects().applyPet(p, cos);
        }
        if (category == CosmeticCategory.GADGET) {
            // Put a gadget item in slot 7 of the hotbar (right of compass at slot 4)
            if (cos == null) {
                p.getInventory().setItem(7, null);
            } else {
                p.getInventory().setItem(7,
                        com.reinacraft.core.cosmetic.CosmeticGadgetListener.buildGadgetItem(cos));
            }
        }
        // TRAIL: no immediate action — the per-tick task reads equipped state on each tick
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
