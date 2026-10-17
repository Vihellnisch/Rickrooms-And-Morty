package com.rickandmorty.worldgen;

import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.worldgen.level.GenMath;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.unit;

/**
 * Level 0's special zones, each a 72x72 block area (12x12 cells) walled off from the classic
 * two-storey maze, with doorways through on the ground floor. Unlike the maze, their ceilings
 * go up and down:
 * <ul>
 *   <li>KANE: low and high ceilings in blocks, dropped soffits, fat pillars, soft low benches;</li>
 *   <li>OPEN_HALL: a vast empty room under a dense grid of lights, as far as you can see;</li>
 *   <li>PITS: a room whose floor is a grid of square pits sixty blocks deep. At the bottom is Level 1;</li>
 *   <li>MONOLITH: a hall forty blocks high around two dark towers; one of them has a door.</li>
 * </ul>
 * Plus, in the classic maze, the rare Hole: a wall burst open around a patch of pure black.
 */
public final class BackroomsZones {
    public static final int ZONE = 72;
    /** Pit walls reach down to here; below them is the void. */
    public static final int PIT_BOTTOM = -60;

    public enum Zone { CLASSIC, KANE, OPEN_HALL, PITS, MONOLITH }

    private BackroomsZones() {
    }

    public static Zone zoneAt(long seed, int x, int z) {
        int zx = Math.floorDiv(x, ZONE), zz = Math.floorDiv(z, ZONE);
        if ((zx == 0 || zx == -1) && (zz == 0 || zz == -1)) return Zone.CLASSIC;    // the maze around the origin stays classic
        double u = unit(seed, zx, zz, 200);
        if (u < 0.12) return Zone.KANE;
        if (u < 0.2) return Zone.OPEN_HALL;
        if (u < 0.25) return Zone.PITS;
        if (u < 0.28) return Zone.MONOLITH;
        return Zone.CLASSIC;
    }

    /** y of a zone's bedrock roof. */
    public static int roof(Zone zone) {
        return switch (zone) {
            case KANE -> 10;
            case OPEN_HALL -> 8;
            case PITS -> 9;
            case MONOLITH -> 45;
            case CLASSIC -> BackroomsLayout.ROOF_Y;
        };
    }

    private static BlockState carpet() {
        return ModBlocks.BACKROOMS_CARPET.get().defaultBlockState();
    }

    private static BlockState wallpaper() {
        return ModBlocks.BACKROOMS_WALLPAPER.get().defaultBlockState();
    }

    private static BlockState ceiling() {
        return ModBlocks.BACKROOMS_CEILING.get().defaultBlockState();
    }

    private static BlockState light(boolean broken) {
        BlockState light = ModBlocks.FLUORESCENT_LIGHT.get().defaultBlockState();
        return broken ? com.rickandmorty.block.FluorescentLightBlock.broken(light) : light;
    }

    private static void fill(BlockState[] c, int from, int to, @Nullable BlockState state) {
        for (int y = from; y <= to; y++) c[y] = state;
    }

    /** The column at (x, z) in a special zone (index = y, from 0; null = air). */
    public static BlockState[] column(long seed, int x, int z, Zone zone) {
        int roof = roof(zone);
        BlockState[] c = new BlockState[Math.max(roof, BackroomsLayout.ROOF_Y) + 1];
        c[0] = Blocks.BEDROCK.defaultBlockState();
        c[roof] = Blocks.BEDROCK.defaultBlockState();
        c[1] = carpet();
        int lx = Math.floorMod(x, ZONE), lz = Math.floorMod(z, ZONE);
        int zx = Math.floorDiv(x, ZONE), zz = Math.floorDiv(z, ZONE);
        if (lx == 0 || lz == 0 || lx == ZONE - 1 || lz == ZONE - 1) {
            // walled off from the maze, doorways at ground level
            fill(c, 2, c.length - 2, wallpaper());
            c[roof] = Blocks.BEDROCK.defaultBlockState();
            int along = lx == 0 || lx == ZONE - 1 ? lz : lx;
            if (Math.floorMod(along, 12) == 5 || Math.floorMod(along, 12) == 6) {
                c[2] = null;
                c[3] = null;
            }
            return c;
        }
        switch (zone) {
            case KANE -> kane(c, seed, zx, zz, lx, lz);
            case OPEN_HALL -> openHall(c, seed, x, z, lx, lz);
            case PITS -> pits(c, lx, lz);
            case MONOLITH -> monolith(c, seed, x, z, lx, lz);
            case CLASSIC -> {
            }
        }
        return c;
    }

    /**
     * The found-footage Level 0: the ceiling height changes block by block (dropped soffits,
     * lowered ducts, tall voids with lights recessed in them), fat square pillars, and low
     * padded benches you could almost sit on.
     */
    private static void kane(BlockState[] c, long seed, int zx, int zz, int lx, int lz) {
        int ax = lx / 6, az = lz / 6;
        long h = hash(seed, zx * 16 + ax, zz * 16 + az, 201);
        int[] heights = {5, 6, 6, 7, 7, 9};
        int ceilingY = heights[(int) Math.floorMod(h, (long) heights.length)];
        if ((lx % 6 == 0 || lz % 6 == 0) && unit(Long.rotateLeft(h, 3)) < 0.35) ceilingY = Math.max(4, ceilingY - 1);   // a duct
        c[ceilingY] = ceiling();
        fill(c, ceilingY + 1, 9, wallpaper());
        if (lx % 6 >= 2 && lx % 6 <= 3 && lz % 6 == 3 && unit(Long.rotateLeft(h, 5)) < 0.8) c[ceilingY] = light(unit(Long.rotateLeft(h, 7)) < 0.1);
        // square pillars
        if (lx % 12 <= 1 && lz % 12 <= 1 && unit(seed, zx * 8 + lx / 12, zz * 8 + lz / 12, 202) < 0.45) {
            fill(c, 2, ceilingY - 1, wallpaper());
            return;
        }
        // stretches of wall that don't quite make rooms
        if (lx % 6 == 0 && lz % 6 != 2 && lz % 6 != 3 && unit(seed, zx * 16 + ax, zz * 16 + az, 203) < 0.18) {
            fill(c, 2, ceilingY - 1, wallpaper());
            return;
        }
        // soft low benches: a padded back and a lower seat, rounded off at the ends
        if (unit(Long.rotateLeft(h, 12)) < 0.3) {
            boolean turned = unit(Long.rotateLeft(h, 20)) < 0.5;
            int along = turned ? lz % 6 : lx % 6, across = turned ? lx % 6 : lz % 6;
            BlockState seat = ModBlocks.CARPET_LEDGE.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            if (along >= 1 && along <= 4) {
                if (across == 2) c[2] = along == 1 || along == 4 ? seat : carpet();
                if (across == 3) c[2] = seat;
            }
        }
    }

    /** Endless carpet under an endless grid of lights. */
    private static void openHall(BlockState[] c, long seed, int x, int z, int lx, int lz) {
        c[7] = ceiling();
        if (lx % 4 == 2 && lz % 4 == 2) c[7] = light(unit(seed, x, z, 204) < 0.04);
        if (lx % 24 == 12 && lz % 24 == 12) fill(c, 2, 6, wallpaper());
    }

    /** A grid of 4x4 pits with narrow carpet walkways between them; the pits go down forever. */
    private static void pits(BlockState[] c, int lx, int lz) {
        c[8] = ceiling();
        if (lx % 5 == 2 && lz % 5 == 2) c[8] = light(false);
        if (isPit(lx, lz)) {
            c[0] = null;
            c[1] = null;
        } else if (lx >= 6 && lx <= 65 && lz >= 6 && lz <= 65) {
            c[0] = carpet();
        }
    }

    private static boolean isPit(int lx, int lz) {
        boolean inside = lx >= 6 && lx <= 65 && lz >= 6 && lz <= 65;
        return inside && (lx - 6) % 5 != 0 && (lz - 6) % 5 != 0;
    }

    /** What fills the column below y 0 (pit walls reach far down), or null. */
    public static @Nullable BlockState below(long seed, int x, int z) {
        if (zoneAt(seed, x, z) != Zone.PITS) return null;
        int lx = Math.floorMod(x, ZONE), lz = Math.floorMod(z, ZONE);
        boolean inside = lx >= 6 && lx <= 65 && lz >= 6 && lz <= 65;
        return inside && !isPit(lx, lz) ? carpet() : null;
    }

    /** A hall forty blocks high, two dark towers rising into the ceiling. One has a door in it. */
    private static void monolith(BlockState[] c, long seed, int x, int z, int lx, int lz) {
        c[44] = ceiling();
        if (lx % 8 == 4 && lz % 8 == 4 && unit(seed, x, z, 205) < 0.6) c[44] = light(false);
        boolean towerA = lx >= 24 && lx <= 33 && lz >= 30 && lz <= 39;
        boolean towerB = lx >= 38 && lx <= 47 && lz >= 30 && lz <= 39;
        if (towerA || towerB) {
            fill(c, 2, 43, ModBlocks.MONOLITH.get().defaultBlockState());
            if (towerA && lx == 28 && lz == 39) {
                BlockState door = ModBlocks.EXIT_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
                c[2] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
                c[3] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            }
        }
    }

    /**
     * Somewhere just outside the door of one of Level 0's monoliths (facing it), for anyone
     * coming through from the Overworld's Monolith.
     */
    public static net.minecraft.world.phys.Vec3 monolithArrival(long seed, net.minecraft.util.RandomSource random) {
        int startX = random.nextIntBetweenInclusive(-40, 40), startZ = random.nextIntBetweenInclusive(-40, 40);
        for (int ring = 0; ring < 80; ring++) {
            for (int zx = startX - ring; zx <= startX + ring; zx++) {
                for (int zz = startZ - ring; zz <= startZ + ring; zz++) {
                    if (Math.max(Math.abs(zx - startX), Math.abs(zz - startZ)) != ring) continue;
                    if (zoneAt(seed, zx * ZONE + 36, zz * ZONE + 36) != Zone.MONOLITH) continue;
                    return new net.minecraft.world.phys.Vec3(zx * ZONE + 28.5, 2.0, zz * ZONE + 41.5);
                }
            }
        }
        return new net.minecraft.world.phys.Vec3(0.5, BackroomsLayout.standY(0), 0.5);
    }

    /** Whether someone can stand at (x, z) in a zone: floor, then two blocks of air. */
    public static boolean standable(long seed, int x, int z) {
        Zone zone = zoneAt(seed, x, z);
        BlockState[] c = column(seed, x, z, zone);
        return c[1] != null && c[2] == null && c[3] == null;
    }

    // ------------------------------------------------------------------ the Hole

    private static final int HOLE_REGION = 8;      // in cells

    /** The Hole in this 8x8-cell region, as the cell whose west wall it's in, or null. */
    public static int @Nullable [] hole(long seed, int rx, int rz) {
        if (unit(seed, rx, rz, 210) > 0.1) return null;
        int cx = rx * HOLE_REGION + 1 + GenMath.pick(seed, rx, rz, 211, HOLE_REGION - 2);
        int cz = rz * HOLE_REGION + 1 + GenMath.pick(seed, rx, rz, 212, HOLE_REGION - 2);
        int x = cx * BackroomsLayout.CELL, z = cz * BackroomsLayout.CELL + 3;
        if (zoneAt(seed, x - 4, z) != Zone.CLASSIC || zoneAt(seed, x + 4, z) != Zone.CLASSIC) return null;
        for (int level = 0; level < BackroomsLayout.LEVELS; level++) {
            for (int dx = -2; dx <= 2; dx += 2) {
                for (int dz = -3; dz <= 3; dz += 3) {
                    if (BackroomsRooms.roomAt(seed, level, x + dx, z + dz) != null) return null;    // not through a room's wall
                }
            }
        }
        return new int[]{x, z};
    }

    /**
     * Bursts the wall open around the Hole: black in the middle, the wall intact around it, and
     * cubes of wallpaper pushed out of both sides like something came through.
     */
    public static void applyHole(BlockState[] c, long seed, int x, int z) {
        int cell = BackroomsLayout.CELL;
        int rx = Math.floorDiv(Math.floorDiv(x, cell), HOLE_REGION), rz = Math.floorDiv(Math.floorDiv(z, cell), HOLE_REGION);
        for (int dxr = -1; dxr <= 1; dxr++) {
            for (int dzr = -1; dzr <= 1; dzr++) {
                int[] hole = hole(seed, rx + dxr, rz + dzr);
                if (hole == null) continue;
                int dx = x - hole[0], dz = z - hole[1];
                if (Math.abs(dx) > 2 || Math.abs(dz) > 3) continue;
                for (int y = BackroomsLayout.ROOM_MIN_Y; y <= BackroomsLayout.ROOM_MAX_Y; y++) {
                    double d = Math.sqrt(dz * dz + (y - 3) * (y - 3));
                    double jag = unit(hash(seed, x, z, y, 213));
                    if (dx == 0) {
                        if (d < 1.5 || d < 2.1 && jag < 0.5) c[y] = ModBlocks.NOCLIP_VOID.get().defaultBlockState();
                        else if (d < 3.4) c[y] = wallpaper();
                    } else if (Math.abs(dx) == 1 && d > 1.6 && d < 3.4 && jag < 0.5) {
                        c[y] = wallpaper();
                    } else if (Math.abs(dx) == 2 && d > 1.8 && d < 3.0 && jag < 0.18) {
                        c[y] = wallpaper();
                    }
                }
                return;
            }
        }
    }
}
