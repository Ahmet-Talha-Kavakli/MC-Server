package com.reinacraft.core.companion;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Hypixel-style custom-modelled companion that follows the player.
 *
 * Each value's modelKey() points to an item_model definition in the resource pack
 * (e.g. "reinacraft:companion_magic_dog"). The pack provides the 3D model + texture.
 *
 * On clients that don't have the pack loaded the companion appears as a plain PAPER
 * item — that's by design, the system degrades gracefully.
 */
public enum Companion {

    MAGIC_DOG     ("magic_dog",     "<gradient:#F5C842:#E89028><bold>Sihirli Köpek</bold></gradient>",  500,  Behavior.WALK, 0.6f, "<gray>Yanında zıplayarak gezer."),
    WHITE_PUG     ("white_pug",     "<gradient:#F5F0E6:#E89AA8><bold>Beyaz Mops</bold></gradient>",     500,  Behavior.WALK, 0.55f,"<gray>Küçük ve sevimli."),
    SHIBE         ("shibe",         "<gradient:#D89656:#F2D9B0><bold>Doge</bold></gradient>",          500,  Behavior.WALK, 0.55f,"<gray>Wow. Such pet."),
    GORILLA       ("gorilla",       "<gradient:#2A2A2E:#4A4A50><bold>Goril</bold></gradient>",          900,  Behavior.WALK, 0.75f,"<gray>Ormanın gerçek kralı."),
    PENGUIN       ("penguin",       "<gradient:#1A1A22:#F4F4F0><bold>Penguen</bold></gradient>",        1200, Behavior.WALK, 0.55f,"<gray>Antarktika'dan kaçtı."),
    PANDA         ("panda",         "<gradient:#F2F2EE:#1C1C20><bold>Panda</bold></gradient>",          500,  Behavior.WALK, 0.65f,"<gray>Bambu sever."),
    TURTLE        ("turtle",        "<gradient:#5BA84A:#3D6E2E><bold>Kaplumbağa</bold></gradient>",     1200, Behavior.WALK, 0.55f,"<gray>Yavaş ama emin adımlarla."),
    DUCK          ("duck",          "<gradient:#FFD23F:#F08020><bold>Ördek</bold></gradient>",          500,  Behavior.WALK, 0.5f, "<gray>Vak vak!"),
    FROG          ("frog",          "<gradient:#7BC043:#F4F4F0><bold>Kurbağa</bold></gradient>",        500,  Behavior.HOP,  0.45f,"<gray>Etrafta zıplar."),
    SLOTH         ("sloth",         "<gradient:#B89968:#6B4A2C><bold>Tembel Hayvan</bold></gradient>",  900,  Behavior.WALK, 0.65f,"<gray>Acelesi yok."),
    RED_FOX       ("red_fox",       "<gradient:#D86A2C:#F2D9B0><bold>Kızıl Tilki</bold></gradient>",    700,  Behavior.WALK, 0.55f,"<gray>Çevik ve hızlı."),
    CAT_CALICO    ("cat_calico",    "<gradient:#F5F0E6:#D86A2C:#1A1A22><bold>Tekir Kedi</bold></gradient>", 700, Behavior.WALK, 0.55f, "<gray>Tüm renkleri var."),
    WOLF_CUB      ("wolf_cub",      "<gradient:#A8A8B0:#F4F4F0><bold>Kurt Yavrusu</bold></gradient>",   600,  Behavior.WALK, 0.55f,"<gray>Sadık ufaklık."),
    MINI_DRAGON   ("mini_dragon",   "<gradient:#7D3FC9:#D500F9><bold>Mini Ejder</bold></gradient>",     5000, Behavior.FLY,  0.7f, "<gray>Krallığın koruyucusu."),
    MINI_WITHER   ("mini_wither",   "<gradient:#1A1A1E:#3A3A3E><bold>Mini Wither</bold></gradient>",    3000, Behavior.FLY,  0.6f, "<gray>Üç başlı, küçük ama heybetli."),
    ROYAL_CORGI   ("royal_corgi",   "<gradient:#E8A858:#FFD700><bold>Kraliyet Corgi</bold></gradient>", 2500, Behavior.WALK, 0.55f,"<gray>Kraliçenin gözdesi. Altın taçlı.");

    public enum Behavior { WALK, HOP, FLY }

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final String id;
    private final String mmDisplayName;
    private final long gemPrice;
    private final Behavior behavior;
    private final float scale;
    private final String mmDescription;

    Companion(String id, String mmDisplayName, long gemPrice, Behavior behavior, float scale, String mmDescription) {
        this.id = id;
        this.mmDisplayName = mmDisplayName;
        this.gemPrice = gemPrice;
        this.behavior = behavior;
        this.scale = scale;
        this.mmDescription = mmDescription;
    }

    public String id() { return id; }
    public long gemPrice() { return gemPrice; }
    public Behavior behavior() { return behavior; }
    public float scale() { return scale; }
    public String modelKey() { return "reinacraft:companion_" + id; }

    public Component displayName() {
        return MM.deserialize(mmDisplayName).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }
    public Component description() {
        return MM.deserialize(mmDescription).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }
    public String rawDisplayMm() { return mmDisplayName; }
    public String rawDescriptionMm() { return mmDescription; }

    public static Companion byId(String id) {
        if (id == null) return null;
        for (Companion c : values()) if (c.id.equalsIgnoreCase(id)) return c;
        return null;
    }
}
