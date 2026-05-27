package com.reinacraft.hub.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class ChatListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final String RAINBOW_GRADIENT = "<gradient:#FF1744:#FF9100:#FFEA00:#00E676:#00B0FF:#D500F9>";

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player p = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        Component rank;
        Component nameColor;
        if (p.isOp()) {
            rank = MM.deserialize(RAINBOW_GRADIENT + "<bold>★ YÖNETİCİ</bold></gradient> ");
            nameColor = MM.deserialize(RAINBOW_GRADIENT + "<bold>" + p.getName() + "</bold></gradient>");
        } else {
            rank = MM.deserialize("<#AAAAAA>Oyuncu </#AAAAAA>");
            nameColor = MM.deserialize("<white>" + p.getName());
        }

        Component msg = MM.deserialize("<gray>" + escape(message));

        Component formatted = Component.empty()
                .append(rank)
                .append(nameColor)
                .append(MM.deserialize(" <dark_gray>»</dark_gray> "))
                .append(msg);

        event.renderer((source, sourceDisplayName, sourceMessage, viewer) -> formatted);
    }

    private String escape(String input) {
        return input.replace("<", "&lt;").replace(">", "&gt;");
    }
}
