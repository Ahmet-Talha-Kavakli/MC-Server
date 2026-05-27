package com.reinacraft.core.command;

import com.reinacraft.core.ReinaCore;
import com.reinacraft.core.player.PlayerData;
import com.reinacraft.core.player.PlayerDataService;
import com.reinacraft.core.rank.Rank;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class RankCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ReinaCore core;
    private final PlayerDataService players;

    public RankCommand(ReinaCore core, PlayerDataService players) {
        this.core = core;
        this.players = players;
    }

    public void register() {
        var cmd = core.getCommand("rank");
        if (cmd == null) return;
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(MM.deserialize("<red>Kullanım: /rank <oyuncu> [set <rank>]"));
            return true;
        }

        String name = args[0];
        if (args.length == 1) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(name);
            if (target.getUniqueId() == null) {
                sender.sendMessage(MM.deserialize("<red>Oyuncu bulunamadı: " + name));
                return true;
            }
            players.loadOfflineByName(name).thenAccept(data -> {
                if (data == null) {
                    sender.sendMessage(MM.deserialize("<red>" + name + " hiç giriş yapmamış."));
                    return;
                }
                sender.sendMessage(MM.deserialize(
                        "<gray>" + data.name() + " rank: ")
                        .append(data.rank().fullName(data.name())));
            });
            return true;
        }

        if (args.length >= 3 && args[1].equalsIgnoreCase("set")) {
            Rank rank;
            try {
                rank = Rank.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(MM.deserialize("<red>Geçersiz rank: " + args[2]));
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(name);
            UUID uuid = target.getUniqueId();
            if (uuid == null) {
                sender.sendMessage(MM.deserialize("<red>Oyuncu bulunamadı: " + name));
                return true;
            }
            players.setRank(uuid, rank).thenRun(() ->
                    sender.sendMessage(MM.deserialize(
                            "<green>" + name + " rank'i ayarlandı: <white>" + rank.name())));
            return true;
        }

        sender.sendMessage(MM.deserialize("<red>Kullanım: /rank <oyuncu> [set <rank>]"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        if (args.length == 2) {
            return List.of("set");
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
            return Arrays.stream(Rank.values()).map(Enum::name).toList();
        }
        return List.of();
    }
}
