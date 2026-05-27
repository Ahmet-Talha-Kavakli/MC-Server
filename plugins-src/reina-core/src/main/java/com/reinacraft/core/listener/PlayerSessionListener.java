package com.reinacraft.core.listener;

import com.reinacraft.core.ReinaCore;
import com.reinacraft.core.companion.Companion;
import com.reinacraft.core.companion.CompanionManager;
import com.reinacraft.core.companion.CompanionService;
import com.reinacraft.core.cosmetic.CosmeticEffectManager;
import com.reinacraft.core.cosmetic.CosmeticService;
import com.reinacraft.core.player.PlayerData;
import com.reinacraft.core.player.PlayerDataService;
import com.reinacraft.core.rank.Rank;
import org.bukkit.Bukkit;
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
    private final CosmeticService cosmetics;
    private final CosmeticEffectManager cosmeticEffects;
    private final CompanionService companionService;
    private final CompanionManager companionManager;

    public PlayerSessionListener(ReinaCore core, PlayerDataService service,
                                 CosmeticService cosmetics, CosmeticEffectManager cosmeticEffects,
                                 CompanionService companionService, CompanionManager companionManager) {
        this.core = core;
        this.service = service;
        this.cosmetics = cosmetics;
        this.cosmeticEffects = cosmeticEffects;
        this.companionService = companionService;
        this.companionManager = companionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        try {
            service.loadOrCreate(e.getUniqueId(), e.getName()).join();
            cosmetics.load(e.getUniqueId());
            companionService.load(e.getUniqueId());
        } catch (Throwable t) {
            core.getLogger().warning("Pre-login data load failed for " + e.getName() + ": " + t.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerData data = service.getCached(p.getUniqueId());
        if (data == null) {
            data = service.loadOrCreate(p.getUniqueId(), p.getName()).join();
        }
        data.setLastServer(core.serverId());

        if (p.isOp() && data.rank() == Rank.MEMBER) {
            service.setRank(p.getUniqueId(), Rank.OWNER);
        }

        // Cosmetics + companions only apply on hub for now
        if ("hub".equalsIgnoreCase(core.serverId())) {
            Bukkit.getScheduler().runTaskLater(core, () -> {
                if (!p.isOnline()) return;
                cosmeticEffects.applyAll(p);
                // Re-summon previously equipped companion
                Companion eq = companionService.equipped(p.getUniqueId());
                if (eq != null) companionManager.summon(p, eq);
            }, 30L); // small delay so the player has finished loading their world view
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        companionManager.dismiss(e.getPlayer().getUniqueId());
        cosmeticEffects.clearAll(e.getPlayer().getUniqueId());
        companionService.unload(e.getPlayer().getUniqueId());
        cosmetics.unload(e.getPlayer().getUniqueId());
        service.unload(e.getPlayer().getUniqueId());
    }
}
