package com.reinacraft.hub.world;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Ambient particles + sounds around hub spawn. Runs every few ticks while players are present.
 * No state, no per-player allocation — safe to call shutdown() on plugin disable.
 */
public final class HubAtmosphere {

    private static final List<Particle.DustOptions> DUST = List.of(
            new Particle.DustOptions(Color.fromRGB(0xFF, 0x17, 0x44), 1.0f),  // crimson
            new Particle.DustOptions(Color.fromRGB(0xFF, 0xD7, 0x00), 1.0f),  // gold
            new Particle.DustOptions(Color.fromRGB(0x00, 0xB0, 0xFF), 1.0f),  // sky
            new Particle.DustOptions(Color.fromRGB(0xD5, 0x00, 0xF9), 1.0f)   // magenta
    );

    private final Plugin plugin;
    private final Location spawn;
    private BukkitTask task;
    private int tick = 0;

    public HubAtmosphere(Plugin plugin, Location spawn) {
        this.plugin = plugin;
        this.spawn = spawn;
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::pulse, 5L, 5L); // every 4 Hz
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
    }

    /** Strong welcome sound combo for newly-joined players. */
    public static void playWelcome(Player p) {
        Location at = p.getLocation();
        p.playSound(at, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
        Bukkit.getScheduler().runTaskLater(p.getServer().getPluginManager().getPlugins()[0], () ->
                p.playSound(at, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 0.9f), 6L);
        Bukkit.getScheduler().runTaskLater(p.getServer().getPluginManager().getPlugins()[0], () ->
                p.playSound(at, Sound.BLOCK_BELL_USE, 0.4f, 1.2f), 14L);
    }

    private void pulse() {
        tick++;
        if (spawn == null || spawn.getWorld() == null) return;
        World w = spawn.getWorld();
        if (Bukkit.getOnlinePlayers().isEmpty()) return;

        spireFlames(w);
        towerBeams(w);
        scatterDust(w);

        if (tick % 4 == 0) brazierFlames(w); // 1 Hz braziers (less GPU spam)
        if (tick % 6 == 0) fountainSpray(w);
    }

    private void spireFlames(World w) {
        // Tall flame column around the central spire (y 9..18 from spawn floor)
        int baseY = spawn.getBlockY();
        for (int dy = 9; dy <= 18; dy++) {
            double angle = (tick * 0.4 + dy) % (Math.PI * 2);
            double radius = 1.5;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location p = new Location(w, spawn.getX() + x, baseY + dy, spawn.getZ() + z);
            w.spawnParticle(Particle.FLAME, p, 1, 0.05, 0.05, 0.05, 0.005);
            if ((dy & 1) == 0) {
                w.spawnParticle(Particle.END_ROD, p, 1, 0.02, 0.02, 0.02, 0.005);
            }
        }
        // Crown sparkle at the very top
        Location crown = new Location(w, spawn.getX(), baseY + 19, spawn.getZ());
        w.spawnParticle(Particle.FIREWORK, crown, 2, 0.4, 0.4, 0.4, 0.02);
    }

    private void towerBeams(World w) {
        // Light beam at the 4 corner towers — towers are at (±(OUTER-TOWER), ±(OUTER-TOWER)) of the castle.
        // CastleBuilder constants: OUTER_RADIUS=26, TOWER_RADIUS=5, TOWER_HEIGHT=18. So tower center at ±21.
        int tcx = spawn.getBlockX();
        int tcz = spawn.getBlockZ();
        int baseY = spawn.getBlockY();
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                double cx = tcx + sx * 21;
                double cz = tcz + sz * 21;
                // Beam from top of tower (baseY + 18 + roof) upward
                for (int dy = 0; dy < 8; dy++) {
                    Location p = new Location(w, cx, baseY + 26 + dy, cz);
                    w.spawnParticle(Particle.FIREWORK, p, 1, 0.1, 0.0, 0.1, 0.005);
                    if (dy % 2 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.0, 0.05, 0.0, 0.0);
                }
            }
        }
    }

    private void scatterDust(World w) {
        // Random colored dust drifting down from above spawn
        for (int i = 0; i < 4; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = 8 + Math.random() * 12;
            double dy = 10 + Math.random() * 10;
            Location p = new Location(w,
                    spawn.getX() + Math.cos(angle) * radius,
                    spawn.getBlockY() + dy,
                    spawn.getZ() + Math.sin(angle) * radius);
            Particle.DustOptions opts = DUST.get((int) (Math.random() * DUST.size()));
            w.spawnParticle(Particle.DUST, p, 2, 0.3, 0.3, 0.3, 0, opts);
        }
    }

    private void brazierFlames(World w) {
        // 8 braziers — 2 per gate × 4 gates. Coordinates from CastleBuilder.buildGateBraziers.
        // Gate south (+z=26): flank x ∈ {-4, +4}, z = OUTER-1 = 25
        // Gate north (-z=-26): flank x ∈ {-4, +4}, z = -OUTER+1 = -25
        // Gate east (+x=26): flank z ∈ {-4, +4}, x = OUTER-1 = 25
        // Gate west (-x=-26): flank z ∈ {-4, +4}, x = -OUTER+1 = -25
        int bx = spawn.getBlockX();
        int by = spawn.getBlockY() + 4;
        int bz = spawn.getBlockZ();
        int[][] braziers = {
                {-4,  25}, {4,  25},
                {-4, -25}, {4, -25},
                { 25, -4}, { 25, 4},
                {-25, -4}, {-25, 4},
        };
        for (int[] xz : braziers) {
            Location p = new Location(w, bx + xz[0] + 0.5, by, bz + xz[1] + 0.5);
            w.spawnParticle(Particle.FLAME, p, 3, 0.15, 0.2, 0.15, 0.01);
            w.spawnParticle(Particle.LAVA, p, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    private void fountainSpray(World w) {
        // 4 fountains at (0,6), (0,-6), (6,0), (-6,0) relative to spawn.
        int by = spawn.getBlockY() + 3;
        int[][] fountains = { {0, 6}, {0, -6}, {6, 0}, {-6, 0} };
        for (int[] xz : fountains) {
            Location p = new Location(w,
                    spawn.getX() + xz[0],
                    by + 0.5,
                    spawn.getZ() + xz[1]);
            w.spawnParticle(Particle.SPLASH, p, 6, 0.3, 0.4, 0.3, 0.1);
            w.spawnParticle(Particle.FALLING_WATER, p.add(0, 0.5, 0), 4, 0.4, 0.0, 0.4, 0);
        }
    }

    /** Per-NPC themed particle pulse (called by HubNpcs hologram task, lower frequency). */
    public static void pulseNpc(World w, Location at, String npcId) {
        switch (npcId) {
            case "bedwars" -> {
                w.spawnParticle(Particle.FLAME, at.clone().add(0, 2, 0), 4, 0.4, 0.3, 0.4, 0.005);
                w.spawnParticle(Particle.LAVA, at.clone().add(0, 1.5, 0), 1, 0.3, 0.3, 0.3, 0);
            }
            case "cosmetic" -> {
                w.spawnParticle(Particle.HEART, at.clone().add(0, 2.6, 0), 1, 0.3, 0.2, 0.3, 0);
                w.spawnParticle(Particle.END_ROD, at.clone().add(0, 1.8, 0), 2, 0.3, 0.3, 0.3, 0.01);
            }
            case "profile" -> {
                w.spawnParticle(Particle.ENCHANT, at.clone().add(0, 2.5, 0), 8, 0.5, 0.5, 0.5, 0.5);
            }
            case "tutorial" -> {
                w.spawnParticle(Particle.HAPPY_VILLAGER, at.clone().add(0, 2.4, 0), 3, 0.4, 0.4, 0.4, 0);
                w.spawnParticle(Particle.COMPOSTER, at.clone().add(0, 1.8, 0), 2, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    public static Vector noop() { return new Vector(); }
}
