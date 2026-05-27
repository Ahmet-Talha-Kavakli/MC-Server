package com.reinacraft.hub.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class TabListManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Plugin plugin;
    private BukkitTask task;
    private int frame = 0;

    private static final String[][] FRAMES = new String[][]{
            {"#FF1744", "#FFD700"},
            {"#FF4D4D", "#FFC107"},
            {"#FF8A65", "#FFAB00"},
            {"#FFAB00", "#FF8A65"},
            {"#FFC107", "#FF4D4D"},
            {"#FFD700", "#FF1744"}
    };

    public TabListManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 0L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void apply(Player p) {
        p.sendPlayerListHeaderAndFooter(header(), footer());
    }

    private void tickAll() {
        frame = (frame + 1) % FRAMES.length;
        Component header = header();
        Component footer = footer();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendPlayerListHeaderAndFooter(header, footer);
        }
    }

    private Component header() {
        String[] c = FRAMES[frame];
        return MM.deserialize(
                "\n" +
                        "<gradient:" + c[0] + ":" + c[1] + "><bold>            ✦  R E I N A C R A F T  ✦            </bold></gradient>\n" +
                        "<gray>Kraliçenin Diyarı <dark_gray>•</dark_gray> <white>Multi-Version <gray>(1.8 - 1.21.x)\n"
        );
    }

    private Component footer() {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        return MM.deserialize(
                "\n" +
                        "<gray>Online: <green>" + online + "<dark_gray>/<green>" + max +
                        "  <dark_gray>•</dark_gray>  <gray>Sunucu: <gold>HUB\n" +
                        "<gradient:#FF1744:#FFD700><bold>reinacraft.com</bold></gradient>\n"
        );
    }
}
