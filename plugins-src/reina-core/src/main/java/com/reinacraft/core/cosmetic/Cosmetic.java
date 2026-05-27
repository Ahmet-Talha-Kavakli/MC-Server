package com.reinacraft.core.cosmetic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;

/**
 * A purchasable cosmetic item. Each enum value is one product (Pet/Trail/Gadget).
 *
 * id() is the DB key — never change once a player has purchased it.
 */
public enum Cosmetic {

    // ---- Pets (id prefix "pet_") ----
    PET_WOLF      (CosmeticCategory.PET,   "pet_wolf",      "<gradient:#FF8A65:#FFD700>Sadık Köpek</gradient>",        Material.WOLF_SPAWN_EGG,         500, "<gray>Sana her yerde eşlik eder."),
    PET_FOX       (CosmeticCategory.PET,   "pet_fox",       "<gradient:#FFAB00:#FF1744>Kızıl Tilki</gradient>",        Material.FOX_SPAWN_EGG,          750, "<gray>Çevik ve hızlı."),
    PET_BAT       (CosmeticCategory.PET,   "pet_bat",       "<gradient:#9B59B6:#5DADE2>Gece Yarasası</gradient>",      Material.BAT_SPAWN_EGG,          400, "<gray>Etrafında uçar."),
    PET_PARROT    (CosmeticCategory.PET,   "pet_parrot",    "<gradient:#00B0FF:#D500F9>Rengarenk Papağan</gradient>",  Material.PARROT_SPAWN_EGG,       900, "<gray>Omzunda durur."),
    PET_RABBIT    (CosmeticCategory.PET,   "pet_rabbit",    "<gradient:#FFEA00:#FF8A65>Şirin Tavşan</gradient>",       Material.RABBIT_SPAWN_EGG,       350, "<gray>Yanında zıplar."),
    PET_AXOLOTL   (CosmeticCategory.PET,   "pet_axolotl",   "<gradient:#FF1744:#D500F9>Pembe Aksolotl</gradient>",     Material.AXOLOTL_SPAWN_EGG,     1200, "<gray>Sevimli su yaratığı."),

    // ---- Trails (id prefix "trail_") ----
    TRAIL_FLAME      (CosmeticCategory.TRAIL, "trail_flame",      "<gradient:#FF1744:#FFD700>Alev İzi</gradient>",         Material.BLAZE_POWDER,    300, "<gray>Arkanda alev partikülleri."),
    TRAIL_HEART      (CosmeticCategory.TRAIL, "trail_heart",      "<gradient:#FF1744:#FF8A65>Kalp İzi</gradient>",         Material.POPPY,           300, "<gray>Sevgi dolu."),
    TRAIL_ENCHANT    (CosmeticCategory.TRAIL, "trail_enchant",    "<gradient:#9B59B6:#48D1CC>Büyü İzi</gradient>",         Material.ENCHANTED_BOOK,  500, "<gray>Mistik enchant parçacıkları."),
    TRAIL_END_ROD    (CosmeticCategory.TRAIL, "trail_end_rod",    "<gradient:#FFFFFF:#5DADE2>Yıldız İzi</gradient>",       Material.END_ROD,         450, "<gray>Beyaz yıldız tozu."),
    TRAIL_SOUL_FIRE  (CosmeticCategory.TRAIL, "trail_soul_fire",  "<gradient:#00B0FF:#5DADE2>Ruh Ateşi İzi</gradient>",    Material.SOUL_TORCH,      600, "<gray>Mavi alev partikülleri."),
    TRAIL_RAINBOW    (CosmeticCategory.TRAIL, "trail_rainbow",    "<gradient:#FF1744:#FFD700:#00E676:#00B0FF:#D500F9>Gökkuşağı İzi</gradient>", Material.FIREWORK_STAR, 1500, "<gray>En şaşalı."),

    // ---- Gadgets (id prefix "gadget_") ----
    GADGET_JUMP_BOOTS    (CosmeticCategory.GADGET, "gadget_jump_boots",    "<gradient:#48D1CC:#FFEA00>Zıplama Botları</gradient>",       Material.RABBIT_FOOT,         400, "<gray>Sağ tıkla, yüksek zıpla."),
    GADGET_FIREWORK      (CosmeticCategory.GADGET, "gadget_firework",      "<gradient:#FF1744:#FFD700>Havai Fişek</gradient>",            Material.FIREWORK_ROCKET,    250, "<gray>Sağ tıkla, gökyüzüne!"),
    GADGET_PAINT         (CosmeticCategory.GADGET, "gadget_paint",         "<gradient:#D500F9:#1ABC9C>Boya Tabancası</gradient>",         Material.SPLASH_POTION,      300, "<gray>Sağ tıkla, etraf renklenir."),
    GADGET_SNOWBALL      (CosmeticCategory.GADGET, "gadget_snowball",      "<gradient:#FFFFFF:#5DADE2>Kar Topu Yağmuru</gradient>",       Material.SNOWBALL,           200, "<gray>Sağ tıkla, kar topu fırlat."),
    GADGET_LIGHTNING     (CosmeticCategory.GADGET, "gadget_lightning",     "<gradient:#FFEA00:#FFFFFF>Şimşek Çağırma</gradient>",        Material.TRIDENT,            800, "<gray>Sağ tıkla, gök gürler."),
    GADGET_TROLL         (CosmeticCategory.GADGET, "gadget_troll",         "<gradient:#27AE60:#F1C40F>Yanıltıcı Salata</gradient>",       Material.SLIME_BALL,         500, "<gray>Sağ tıkla, etrafta yanıltıcı sesler.");

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final CosmeticCategory category;
    private final String id;
    private final String mmDisplayName;
    private final Material icon;
    private final long gemPrice;
    private final String mmDescription;

    Cosmetic(CosmeticCategory category, String id, String mmDisplayName, Material icon, long gemPrice, String mmDescription) {
        this.category = category;
        this.id = id;
        this.mmDisplayName = mmDisplayName;
        this.icon = icon;
        this.gemPrice = gemPrice;
        this.mmDescription = mmDescription;
    }

    public CosmeticCategory category() { return category; }
    public String id() { return id; }
    public Material icon() { return icon; }
    public long gemPrice() { return gemPrice; }

    public Component displayName() {
        return MM.deserialize(mmDisplayName).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }
    public Component description() {
        return MM.deserialize(mmDescription).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }
    public String rawDisplayMm() { return mmDisplayName; }
    public String rawDescriptionMm() { return mmDescription; }

    public static Cosmetic byId(String id) {
        for (Cosmetic c : values()) if (c.id.equals(id)) return c;
        return null;
    }
}
