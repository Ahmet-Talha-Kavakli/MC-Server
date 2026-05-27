package com.reinacraft.core.player;

import com.reinacraft.core.rank.Rank;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PlayerData {

    private final UUID uuid;
    private volatile String name;
    private volatile Rank rank;
    private volatile long coins;
    private volatile long gems;
    private volatile int level;
    private volatile long xp;
    private volatile Instant firstLogin;
    private volatile Instant lastLogin;
    private volatile String lastServer;

    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.rank = Rank.MEMBER;
        this.coins = 0;
        this.gems = 0;
        this.level = 1;
        this.xp = 0;
        Instant now = Instant.now();
        this.firstLogin = now;
        this.lastLogin = now;
        this.lastServer = "hub";
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public Rank rank() { return rank; }
    public long coins() { return coins; }
    public long gems() { return gems; }
    public int level() { return level; }
    public long xp() { return xp; }
    public Instant firstLogin() { return firstLogin; }
    public Instant lastLogin() { return lastLogin; }
    public String lastServer() { return lastServer; }

    public void setName(String name) { this.name = name; markDirty(); }
    public void setRank(Rank rank) { this.rank = rank; markDirty(); }
    public void setCoins(long coins) { this.coins = coins; markDirty(); }
    public void setGems(long gems) { this.gems = gems; markDirty(); }
    public void setLevel(int level) { this.level = level; markDirty(); }
    public void setXp(long xp) { this.xp = xp; markDirty(); }
    public void setFirstLogin(Instant firstLogin) { this.firstLogin = firstLogin; }
    public void setLastLogin(Instant lastLogin) { this.lastLogin = lastLogin; markDirty(); }
    public void setLastServer(String lastServer) { this.lastServer = lastServer; markDirty(); }

    public void markDirty() { dirty.set(true); }
    public boolean clearDirty() { return dirty.getAndSet(false); }
    public boolean isDirty() { return dirty.get(); }
}
