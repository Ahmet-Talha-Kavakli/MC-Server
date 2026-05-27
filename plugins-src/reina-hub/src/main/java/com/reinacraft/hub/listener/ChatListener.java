package com.reinacraft.hub.listener;

import com.reinacraft.core.ReinaCore;
import com.reinacraft.core.player.PlayerData;
import com.reinacraft.core.rank.Rank;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class ChatListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player p = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        PlayerData data = ReinaCore.get().playerData().getCached(p.getUniqueId());
        Rank rank = data != null ? data.rank() : Rank.MEMBER;

        Component prefix = rank.prefix();
        Component nameColor = rank.coloredName(p.getName());
        Component msg = MM.deserialize("<gray>" + escape(message));

        Component formatted = Component.empty()
                .append(prefix)
                .append(nameColor)
                .append(MM.deserialize(" <dark_gray>»</dark_gray> "))
                .append(msg);

        event.renderer((source, sourceDisplayName, sourceMessage, viewer) -> formatted);
    }

    private String escape(String input) {
        return input.replace("<", "&lt;").replace(">", "&gt;");
    }
}
