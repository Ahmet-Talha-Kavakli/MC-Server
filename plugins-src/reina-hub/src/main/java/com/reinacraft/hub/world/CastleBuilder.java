package com.reinacraft.hub.world;

import com.reinacraft.core.util.SchematicBuilder;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Wall;
import org.bukkit.block.data.type.Wall.Height;

/**
 * Builds the ReinaCraft "Kraliçenin Diyarı" castle around the hub spawn platform.
 *
 * Layout (top-down, centered at spawn):
 *
 *      NW tower            NE tower
 *           \              /
 *            +--bridge--+
 *            |          |
 *         wall         wall
 *            |          |
 *            +--bridge--+
 *           /              \
 *      SW tower            SE tower
 *
 * Center is the existing 7-radius spawn platform; the castle grows outward.
 */
public final class CastleBuilder {

    private static final int OUTER_RADIUS = 26;          // half-width of square perimeter
    private static final int WALL_HEIGHT  = 7;
    private static final int TOWER_RADIUS = 5;
    private static final int TOWER_HEIGHT = 18;

    public static void build(World world, int cx, int cy, int cz) {
        SchematicBuilder b = new SchematicBuilder(world, cx, cy, cz);
        int floorY = -1; // spawn block - 1

        // 1) Clear interior airspace so the player can fly around (cy..cy+25)
        b.clear(-OUTER_RADIUS - 2, 0, -OUTER_RADIUS - 2,
                OUTER_RADIUS + 2, 25, OUTER_RADIUS + 2);

        // 2) Castle floor: stone bricks with gold accent ring
        for (int x = -OUTER_RADIUS; x <= OUTER_RADIUS; x++) {
            for (int z = -OUTER_RADIUS; z <= OUTER_RADIUS; z++) {
                int dist = Math.max(Math.abs(x), Math.abs(z));
                if (dist > OUTER_RADIUS) continue;
                if (dist <= 7) continue; // leave the central spawn platform alone

                Material mat;
                if (dist == OUTER_RADIUS) {
                    mat = Material.POLISHED_DEEPSLATE;
                } else if (dist == OUTER_RADIUS - 1) {
                    mat = Material.DEEPSLATE_BRICKS;
                } else if (dist == 9 || dist == 10) {
                    mat = ((x + z) & 1) == 0 ? Material.GOLD_BLOCK : Material.LAPIS_BLOCK;
                } else if (dist == 8) {
                    mat = Material.DEEPSLATE_TILES;
                } else {
                    mat = ((x + z) & 1) == 0 ? Material.DEEPSLATE_BRICKS : Material.DEEPSLATE_TILES;
                }
                b.set(x, floorY, z, mat);
            }
        }

        // 3) Outer walls: 4 sides, with crenellations
        // South wall (z = +OUTER_RADIUS), North (z = -OUTER_RADIUS)
        // East (x = +OUTER_RADIUS), West (x = -OUTER_RADIUS)
        // Skip corners (towers cover them) — start wall TOWER_RADIUS+1 in
        int wallInset = TOWER_RADIUS + 1;
        for (int along = -OUTER_RADIUS + wallInset; along <= OUTER_RADIUS - wallInset; along++) {
            // South + North
            for (int side : new int[]{-1, 1}) {
                int zEdge = side * OUTER_RADIUS;
                buildWallColumn(b, along, floorY, zEdge);
            }
            // East + West
            for (int side : new int[]{-1, 1}) {
                int xEdge = side * OUTER_RADIUS;
                buildWallColumn(b, xEdge, floorY, along);
            }
        }

        // 4) Gates at the cardinal midpoints (opening in wall, 3 wide × 4 tall)
        for (int[] gate : new int[][]{ {0, OUTER_RADIUS}, {0, -OUTER_RADIUS}, {OUTER_RADIUS, 0}, {-OUTER_RADIUS, 0} }) {
            carveGate(b, gate[0], floorY, gate[1]);
        }

        // 5) 4 corner towers
        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                int tcx = dx * (OUTER_RADIUS - TOWER_RADIUS);
                int tcz = dz * (OUTER_RADIUS - TOWER_RADIUS);
                buildTower(b, tcx, floorY, tcz);
            }
        }

        // 6) Banner pillars around the center spawn (just outside the existing platform)
        for (int[] p : new int[][]{ {0, 9}, {0, -9}, {9, 0}, {-9, 0} }) {
            buildBannerPillar(b, p[0], floorY, p[1]);
        }

        // 7) Lighting: sea lanterns on the inner ring
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0) * Math.PI * 2;
            int lx = (int) Math.round(Math.cos(angle) * 14);
            int lz = (int) Math.round(Math.sin(angle) * 14);
            b.set(lx, floorY, lz, Material.SEA_LANTERN);
        }

        // 8) Center spire above the spawn (mark of the queen)
        buildCentralSpire(b, 0, floorY, 0);
    }

    private static void buildWallColumn(SchematicBuilder b, int x, int floorY, int z) {
        // wall body (deepslate bricks)
        for (int y = floorY + 1; y <= floorY + WALL_HEIGHT - 1; y++) {
            b.set(x, y, z, Material.DEEPSLATE_BRICKS);
        }
        // top course alternating crenellation
        int topY = floorY + WALL_HEIGHT;
        boolean merlon = ((x + z) & 1) == 0;
        b.set(x, topY, z, merlon ? Material.DEEPSLATE_TILES : Material.AIR);

        // gold band on row 3
        b.set(x, floorY + 3, z, Material.GOLD_BLOCK);
    }

    private static void carveGate(SchematicBuilder b, int x, int floorY, int z) {
        // Compute the wall axis. If z is the outer wall (|z|=OUTER_RADIUS), opening is on x-axis.
        boolean wallIsZ = Math.abs(z) == OUTER_RADIUS;
        for (int along = -1; along <= 1; along++) {
            int gx = wallIsZ ? along : x;
            int gz = wallIsZ ? z : along;
            for (int y = floorY + 1; y <= floorY + 4; y++) {
                b.set(gx, y, gz, Material.AIR);
            }
            // Lapis arch crown
            b.set(gx, floorY + 5, gz, Material.LAPIS_BLOCK);
        }
        // Side torch-pillars
        if (wallIsZ) {
            b.set(-2, floorY + 1, z, Material.GOLD_BLOCK);
            b.set(-2, floorY + 2, z, Material.SEA_LANTERN);
            b.set(2, floorY + 1, z, Material.GOLD_BLOCK);
            b.set(2, floorY + 2, z, Material.SEA_LANTERN);
        } else {
            b.set(x, floorY + 1, -2, Material.GOLD_BLOCK);
            b.set(x, floorY + 2, -2, Material.SEA_LANTERN);
            b.set(x, floorY + 1, 2, Material.GOLD_BLOCK);
            b.set(x, floorY + 2, 2, Material.SEA_LANTERN);
        }
    }

    private static void buildTower(SchematicBuilder b, int cx, int floorY, int cz) {
        int baseY = floorY + 1;
        int topY  = floorY + TOWER_HEIGHT;
        int r = TOWER_RADIUS;
        int rOuter = r;
        int rInner = r - 1;

        // Hollow stone column
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= rOuter * rOuter && d2 > rInner * rInner) {
                    for (int y = baseY; y <= topY; y++) {
                        Material mat = (y - baseY) % 4 == 0 ? Material.DEEPSLATE_TILES : Material.DEEPSLATE_BRICKS;
                        b.set(cx + dx, y, cz + dz, mat);
                    }
                }
            }
        }

        // Tower floor (interior) - keep open above for visual airiness
        for (int dx = -r + 1; dx <= r - 1; dx++) {
            for (int dz = -r + 1; dz <= r - 1; dz++) {
                if (dx * dx + dz * dz <= (rInner - 0) * (rInner - 0)) {
                    b.set(cx + dx, floorY, cz + dz, Material.POLISHED_DEEPSLATE);
                }
            }
        }

        // Battlements (crenellations on top)
        int crenY = topY + 1;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= rOuter * rOuter && d2 > rInner * rInner) {
                    boolean merlon = ((dx + dz) & 1) == 0;
                    if (merlon) b.set(cx + dx, crenY, cz + dz, Material.DEEPSLATE_TILES);
                }
            }
        }

        // Conical golden roof (4 layers)
        for (int layer = 0; layer < 4; layer++) {
            int rr = r - layer;
            int ry = topY + 2 + layer;
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 <= rr * rr) {
                        Material mat = layer == 3 ? Material.GOLD_BLOCK : (layer % 2 == 0 ? Material.LAPIS_BLOCK : Material.GOLD_BLOCK);
                        b.set(cx + dx, ry, cz + dz, mat);
                    }
                }
            }
        }
        // Spire tip
        b.set(cx, topY + 6, cz, Material.GOLD_BLOCK);
        b.set(cx, topY + 7, cz, Material.SEA_LANTERN);
        b.set(cx, topY + 8, cz, Material.BEACON);

        // Gold band ring at the base
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= rOuter * rOuter && d2 > rInner * rInner) {
                    b.set(cx + dx, baseY + 3, cz + dz, Material.GOLD_BLOCK);
                }
            }
        }
    }

    private static void buildBannerPillar(SchematicBuilder b, int x, int floorY, int z) {
        b.set(x, floorY + 1, z, Material.POLISHED_DEEPSLATE);
        b.set(x, floorY + 2, z, Material.GOLD_BLOCK);
        b.set(x, floorY + 3, z, Material.RED_WOOL);
        b.set(x, floorY + 4, z, Material.RED_WOOL);
        b.set(x, floorY + 5, z, Material.GOLD_BLOCK);
        b.set(x, floorY + 6, z, Material.SEA_LANTERN);
    }

    private static void buildCentralSpire(SchematicBuilder b, int x, int floorY, int z) {
        int sx = x, sz = z;
        // Above center, build a thin spire (the platform itself stays untouched)
        for (int dy = 8; dy <= 18; dy++) {
            int layer = dy - 8;
            Material mat = layer < 2 ? Material.LAPIS_BLOCK :
                           layer < 7 ? Material.GOLD_BLOCK :
                                       Material.SEA_LANTERN;
            // Spire shrinks as it goes up
            int spireR = layer < 4 ? 1 : 0;
            for (int dx = -spireR; dx <= spireR; dx++)
                for (int dz = -spireR; dz <= spireR; dz++)
                    if (Math.abs(dx) + Math.abs(dz) <= spireR)
                        b.set(sx + dx, floorY + dy, sz + dz, mat);
        }
        // Crown at the tip
        b.set(sx, floorY + 19, sz, Material.BEACON);
    }
}
