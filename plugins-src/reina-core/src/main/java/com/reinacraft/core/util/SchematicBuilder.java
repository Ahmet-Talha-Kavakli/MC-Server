package com.reinacraft.core.util;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;

import java.util.function.BiFunction;

public final class SchematicBuilder {

    private final World world;
    private final int originX;
    private final int originY;
    private final int originZ;

    public SchematicBuilder(World world, int originX, int originY, int originZ) {
        this.world = world;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
    }

    public SchematicBuilder set(int dx, int dy, int dz, Material material) {
        Block b = world.getBlockAt(originX + dx, originY + dy, originZ + dz);
        b.setType(material, false);
        return this;
    }

    public SchematicBuilder set(int dx, int dy, int dz, BlockData data) {
        Block b = world.getBlockAt(originX + dx, originY + dy, originZ + dz);
        b.setBlockData(data, false);
        return this;
    }

    public SchematicBuilder setDirectional(int dx, int dy, int dz, Material material, BlockFace face) {
        Block b = world.getBlockAt(originX + dx, originY + dy, originZ + dz);
        b.setType(material, false);
        BlockData data = b.getBlockData();
        if (data instanceof Directional dir) {
            dir.setFacing(face);
            b.setBlockData(dir, false);
        }
        return this;
    }

    public SchematicBuilder fillCuboid(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    set(x, y, z, material);
        return this;
    }

    public SchematicBuilder fillCuboid(int x1, int y1, int z1, int x2, int y2, int z2, BiFunction<int[], Integer, Material> chooser) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    set(x, y, z, chooser.apply(new int[]{x, y, z}, 0));
        return this;
    }

    /** Hollow box (walls + floor + roof). Interior left untouched. */
    public SchematicBuilder hollowBox(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ) {
                        set(x, y, z, material);
                    }
                }
        return this;
    }

    /** Hollow vertical cylinder (no floor/roof). */
    public SchematicBuilder hollowCylinder(int cx, int cz, int y1, int y2, int radius, Material material) {
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int r2 = radius * radius;
        int rInner2 = (radius - 1) * (radius - 1);
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) {
                int dist2 = dx * dx + dz * dz;
                if (dist2 <= r2 && dist2 >= rInner2) {
                    for (int y = minY; y <= maxY; y++) set(cx + dx, y, cz + dz, material);
                }
            }
        return this;
    }

    public SchematicBuilder solidDisc(int cx, int cz, int y, int radius, Material material) {
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= r2) set(cx + dx, y, cz + dz, material);
            }
        return this;
    }

    /** Clear (set to AIR) an inclusive cuboid. */
    public SchematicBuilder clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        return fillCuboid(x1, y1, z1, x2, y2, z2, Material.AIR);
    }
}
