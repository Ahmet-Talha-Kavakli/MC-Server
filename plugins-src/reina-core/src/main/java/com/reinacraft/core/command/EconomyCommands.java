package com.reinacraft.core.command;

import com.reinacraft.core.ReinaCore;
import com.reinacraft.core.economy.EconomyService;
import com.reinacraft.core.economy.EconomyService.Currency;
import com.reinacraft.core.player.PlayerData;
import com.reinacraft.core.player.PlayerDataService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EconomyCommands implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ReinaCore core;
    private final EconomyService economy;

    public EconomyCommands(ReinaCore core, EconomyService economy) {
        this.core = core;
        this.economy = economy;
    }

    public void register() {
        var coinsCmd = core.getCommand("coins");
        if (coinsCmd != null) {
            coinsCmd.setExecutor(this);
            coinsCmd.setTabCompleter(this);
        }
        var gemsCmd = core.getCommand("gems");
        if (gemsCmd != null) {
            gemsCmd.setExecutor(this);
            gemsCmd.setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Currency currency = command.getName().equalsIgnoreCase("coins") ? Currency.COIN : Currency.GEM;
        String symbol = currency == Currency.COIN ? "<gold>⛁</gold>" : "<aqua>❖</aqua>";
        String name = currency == Currency.COIN ? "Coin" : "Gem";

        if (args.length < 1) {
            sender.sendMessage(MM.deserialize("<red>Kullanım: /" + label + " <oyuncu> [give|set|take <miktar>]"));
            return true;
        }

        String playerName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();
        if (uuid == null) {
            sender.sendMessage(MM.deserialize("<red>Oyuncu bulunamadı: " + playerName));
            return true;
        }

        if (args.length == 1) {
            PlayerData data = core.playerData().getCached(uuid);
            if (data == null) {
                sender.sendMessage(MM.deserialize("<red>" + playerName + " şu an online değil. (Offline sorgu Faz 3'te)"));
                return true;
            }
            long bal = economy.balance(uuid, currency);
            sender.sendMessage(MM.deserialize(
                    "<gray>" + playerName + " " + name + " bakiyesi: <white>" + bal + " " + symbol));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MM.deserialize("<red>Kullanım: /" + label + " " + playerName + " give|set|take <miktar>"));
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(MM.deserialize("<red>Geçersiz miktar: " + args[2]));
            return true;
        }

        String action = args[1].toLowerCase();
        String reason = "admin:" + sender.getName();
        switch (action) {
            case "give" -> economy.give(uuid, currency, amount, reason).thenAccept(bal ->
                    sender.sendMessage(MM.deserialize(
                            "<green>" + playerName + " +<white>" + amount + " " + name +
                                    " </white><dark_gray>(yeni: <white>" + bal + "</white>)")));
            case "take" -> economy.take(uuid, currency, amount, reason).thenAccept(bal ->
                    sender.sendMessage(MM.deserialize(
                            "<yellow>" + playerName + " -<white>" + amount + " " + name +
                                    " </white><dark_gray>(yeni: <white>" + bal + "</white>)")));
            case "set" -> economy.set(uuid, currency, amount, reason).thenAccept(bal ->
                    sender.sendMessage(MM.deserialize(
                            "<aqua>" + playerName + " " + name + " = <white>" + bal)));
            default -> sender.sendMessage(MM.deserialize("<red>Bilinmeyen işlem: " + action));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        if (args.length == 2) return List.of("give", "set", "take");
        return List.of();
    }
}
