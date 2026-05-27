package com.reinacraft.core.listener;

import com.reinacraft.core.ReinaCore;
import com.reinacraft.core.player.PlayerData;
import com.reinacraft.core.player.PlayerDataService;
import com.reinacraft.core.rank.Rank;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerSessionListener implements Listener {

    private final ReinaCore core;
    private final PlayerDataService service;

    public PlayerSessionListener(ReinaCore core, PlayerDataService service) {
        this.core = core;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        try {
            service.loadOrCreate(e.getUniqueId(), e.getName()).join();
        } catch (Throwable t) {
            core.getLogger().warning("Pre-login data load failed for " + e.getName() + ": " + t.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerData data = service.getCached(p.getUniqueId());
        if (data == null) {
            // Fallback if pre-login failed
            data = service.loadOrCreate(p.getUniqueId(), p.getName()).join();
        }
        data.setLastServer(core.serverId());

        // Auto-promote: if the player is OP and currently MEMBER, give them OWNER
        if (p.isOp() && data.rank() == Rank.MEMBER) {
            service.setRank(p.getUniqueId(), Rank.OWNER);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        service.unload(e.getPlayer().getUniqueId());
    }
}
