package com.reinacraft.core.rank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public enum Rank {

    MEMBER       (0,  "<gray>",                                              "",                       false),
    VIP          (1,  "<#5DADE2>",                                           "[VIP] ",                 false),
    VIP_PLUS     (2,  "<gradient:#5DADE2:#48D1CC>",                          "[VIP+] ",                false),
    MVP          (3,  "<#1ABC9C>",                                           "[MVP] ",                 false),
    MVP_PLUS     (4,  "<gradient:#1ABC9C:#9B59B6>",                          "[MVP+] ",                false),
    MVP_PLUS_PLUS(5,  "<gradient:#FFD700:#FF8C00>",                          "[MVP++] ",               true),
    YOUTUBE      (6,  "<gradient:#FFFFFF:#FF0000>",                          "[✪ YT] ",          false),
    STAFF        (7,  "<gradient:#27AE60:#1ABC9C>",                          "[⚑ STAFF] ",       false),
    ADMIN        (8,  "<gradient:#E74C3C:#F1C40F>",                          "[⚡ ADMIN] ",       true),
    OWNER        (9,  "<gradient:#FF1744:#FF9100:#FFEA00:#00E676:#00B0FF:#D500F9>", "[★ OWNER] ", true);

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final int priority;
    private final String colorOpen;
    private final String prefixText;
    private final boolean animated;

    Rank(int priority, String colorOpen, String prefixText, boolean animated) {
        this.priority = priority;
        this.colorOpen = colorOpen;
        this.prefixText = prefixText;
        this.animated = animated;
    }

    public int priority() { return priority; }
    public boolean animated() { return animated; }
    public boolean isStaff() { return priority >= STAFF.priority; }

    public Component prefix() {
        if (prefixText.isEmpty()) return Component.empty();
        return MM.deserialize(closeWrap(colorOpen, "<bold>" + prefixText + "</bold>"));
    }

    public Component coloredName(String name) {
        return MM.deserialize(closeWrap(colorOpen, name));
    }

    public Component fullName(String name) {
        return prefix().append(coloredName(name));
    }

    public String rawColor() {
        return colorOpen;
    }

    public static Rank fromId(String id) {
        if (id == null) return MEMBER;
        try {
            return Rank.valueOf(id.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MEMBER;
        }
    }

    private static String closeWrap(String openTag, String inner) {
        // <gradient:...> requires </gradient>, <#hex> requires </#hex>, etc.
        // Use a closing wildcard via MiniMessage: just append matching close.
        String close;
        if (openTag.startsWith("<gradient")) close = "</gradient>";
        else if (openTag.startsWith("<#")) close = "</" + openTag.substring(1);
        else if (openTag.startsWith("<")) close = "</" + openTag.substring(1);
        else close = "";
        return openTag + inner + close;
    }
}
