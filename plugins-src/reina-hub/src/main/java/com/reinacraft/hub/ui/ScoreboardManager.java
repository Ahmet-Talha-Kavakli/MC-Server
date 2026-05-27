package com.reinacraft.hub.ui;

import com.reinacraft.core.ReinaCore;
import com.reinacraft.core.player.PlayerData;
import com.reinacraft.core.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ScoreboardManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final String[] LINE_ENTRIES = new String[]{
            "§0", "§1", "§2", "§3", "§4",
            "§5", "§6", "§7", "§8", "§9",
            "§a", "§b", "§c", "§d", "§e"
    };

    private static final String[][] GRADIENT_FRAMES = new String[][]{
            {"#FF1744", "#FFD700"},
            {"#FF4D4D", "#FFC107"},
            {"#FF8A65", "#FFAB00"},
            {"#FFAB00", "#FF8A65"},
            {"#FFC107", "#FF4D4D"},
            {"#FFD700", "#FF1744"},
            {"#FF8A65", "#FF1744"},
            {"#FF4D4D", "#FF1744"}
    };

    private final Plugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private BukkitTask task;
    private int frame = 0;

    public ScoreboardManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 0L, 6L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        boards.clear();
    }

    public void register(Player p) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("reina", Criteria.DUMMY, animatedTitle());
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 0; i < LINE_ENTRIES.length; i++) {
            Team team = sb.registerNewTeam("line_" + i);
            team.addEntry(LINE_ENTRIES[i]);
            obj.getScore(LINE_ENTRIES[i]).setScore(LINE_ENTRIES.length - i);
        }

        boards.put(p.getUniqueId(), sb);
        p.setScoreboard(sb);
        updatePlayer(p);
    }

    public void unregister(Player p) {
        boards.remove(p.getUniqueId());
    }

    private void tickAll() {
        frame = (frame + 1) % GRADIENT_FRAMES.length;
        Component title = animatedTitle();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard sb = boards.get(p.getUniqueId());
            if (sb == null) continue;
            Objective obj = sb.getObjective("reina");
            if (obj == null) continue;
            obj.displayName(title);
            updatePlayer(p);
        }
    }

    private void updatePlayer(Player p) {
        Scoreboard sb = boards.get(p.getUniqueId());
        if (sb == null) return;

        List<Component> lines = buildLines(p);

        for (int i = 0; i < LINE_ENTRIES.length; i++) {
            Team team = sb.getTeam("line_" + i);
            if (team == null) continue;
            Component line = i < lines.size() ? lines.get(i) : Component.empty();
            team.prefix(line);
            team.suffix(Component.empty());
        }
    }

    private List<Component> buildLines(Player p) {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();

        PlayerData data = ReinaCore.get().playerData().getCached(p.getUniqueId());
        Rank rank = data != null ? data.rank() : Rank.MEMBER;
        long coins = data != null ? data.coins() : 0L;
        long gems = data != null ? data.gems() : 0L;
        int level = data != null ? data.level() : 1;

        Component rankComp = rank == Rank.MEMBER
                ? MM.deserialize("<#AAAAAA>Üye")
                : rank.prefix();

        List<Component> lines = new ArrayList<>();
        lines.add(MM.deserialize("<dark_gray><st>                    </st>"));
        lines.add(MM.deserialize("<gray>Oyuncu: <white>" + p.getName()));
        lines.add(MM.deserialize("<gray>Rank: ").append(rankComp));
        lines.add(Component.empty());
        lines.add(MM.deserialize("<gray>Coin: <gold>" + coins + " ⛁"));
        lines.add(MM.deserialize("<gray>Gem: <aqua>" + gems + " ❖"));
        lines.add(MM.deserialize("<gray>Level: <yellow>" + level));
        lines.add(Component.empty());
        lines.add(MM.deserialize("<gray>Sunucu: <gold>" + ReinaCore.get().serverDisplayName()));
        lines.add(MM.deserialize("<gray>Online: <green>" + online + "<dark_gray>/<green>" + max));
        lines.add(Component.empty());
        lines.add(MM.deserialize("<gradient:#FF1744:#FFD700><bold>reinacraft.com</bold></gradient>"));
        lines.add(MM.deserialize("<dark_gray><st>                    </st>"));
        return lines;
    }

    private Component animatedTitle() {
        String[] colors = GRADIENT_FRAMES[frame];
        return MM.deserialize("<gradient:" + colors[0] + ":" + colors[1] + "><bold>✦ REINACRAFT ✦</bold></gradient>");
    }
}
