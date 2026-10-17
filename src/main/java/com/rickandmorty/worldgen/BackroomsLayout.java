package com.rickandmorty.worldgen;

import com.rickandmorty.block.FluorescentLightBlock;
import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Backrooms floor plan. Everything is a pure function of (seed, x, z), so the chunk
 * generator, the random-landing code, the mob spawner and the tests always agree.
 * <p>
 * There are two floors stacked on top of each other, each with its own maze. Each floor is
 * split into 6x6 cells; walls only stand on cell edges (x or z divisible by 6), so cell
 * interiors, and especially cell centres, are open floor. Each edge randomly has a wall,
 * often with a 2-wide doorway, and wall density changes between regions (cramped mazes
 * vs. wide empty halls).
 * <p>
 * On top of that: rooms ({@link BackroomsRooms}: halls, closets, kitchens...); round patches
 * imitating real biomes ({@link BackroomsTheme}), including rare huge ones; connectors between
 * the floors (stairs, slides, ladders, pits); and rare cells hiding a chest.
 * <pre>
 *  y 11  bedrock roof
 *  y 10  upper ceiling / lights
 *  y 7-9 upper floor's rooms
 *  y  6  upper floor
 *  y  5  lower ceiling / lights
 *  y 2-4 lower floor's rooms
 *  y  1  lower floor
 *  y  0  bedrock
 * </pre>
 */
public final class BackroomsLayout {
    public static final int CELL = 6;
    /** Region size in cells, for varying the wall density. */
    private static final int REGION = 8;

    public static final int LEVELS = 2;
    /** Vertical distance between the two floors. */
    public static final int LEVEL_HEIGHT = 5;
    public static final int FLOOR_Y = 1;      // lower floor (carpet)
    public static final int ROOM_MIN_Y = 2;   // first air block of the lower floor
    public static final int ROOM_MAX_Y = 4;
    public static final int CEILING_Y = 5;
    public static final int ROOF_Y = FLOOR_Y + LEVELS * LEVEL_HEIGHT;  // 11, bedrock

    public enum Light { NONE, ON, BROKEN }

    private BackroomsLayout() {
    }

    public static int floorY(int level) {
        return FLOOR_Y + level * LEVEL_HEIGHT;
    }

    /** y where a mob or player standing on the given floor has its feet. */
    public static int standY(int level) {
        return floorY(level) + 1;
    }

    /** Which floor someone at height y is on. */
    public static int levelAt(double y) {
        return Mth.clamp((int) Math.floor((y - FLOOR_Y) / LEVEL_HEIGHT), 0, LEVELS - 1);
    }

    /** Each floor gets its own maze by scrambling the seed. */
    private static long levelSeed(long seed, int level) {
        return level == 0 ? seed : seed ^ (0x5DEECE66DL * (level + 7));
    }

    public static boolean isWall(long seed, int x, int z) {
        return isWall(seed, 0, x, z);
    }

    public static boolean isWall(long seed, int level, int x, int z) {
        BackroomsRooms.Room room = BackroomsRooms.roomAt(seed, level, x, z);
        if (room != null) return BackroomsRooms.wallInRoom(room, x, z);
        long s = levelSeed(seed, level);
        int lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        if (lx == 0 && lz == 0) return hasPillar(s, cx, cz);
        if (lx == 0) return segmentBlocks(s, cx, cz, 0, lz);
        if (lz == 0) return segmentBlocks(s, cx, cz, 1, lx);
        return false;
    }

    public static Light light(long seed, int x, int z) {
        return light(seed, 0, x, z);
    }

    /** Ceiling lights: a 3-long fluorescent strip across the middle of every cell. */
    public static Light light(long seed, int level, int x, int z) {
        long s = levelSeed(seed, level);
        int lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
        if (lz != CELL / 2 || lx < 2 || lx > 4) return Light.NONE;
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        boolean darkRegion = unit(hash(s, Math.floorDiv(cx, REGION), Math.floorDiv(cz, REGION), 56)) < 0.07;
        boolean broken = unit(hash(s, cx, cz, 55)) < 0.12;
        return darkRegion || broken ? Light.BROKEN : Light.ON;
    }

    /** Centre of a cell (x or z), which is open floor unless the cell holds a connector. */
    public static double cellCenter(int cell) {
        return cell * CELL + CELL / 2 + 0.5;
    }

    /** Cells whose middle is free on both floors. */
    public static boolean isOpenCell(long seed, int cx, int cz) {
        return isOpenCell(seed, 0, cx, cz) && isOpenCell(seed, 1, cx, cz);
    }

    /** Cells whose middle is free to land or spawn in on a floor: no connector, no furniture, not over an atrium. */
    public static boolean isOpenCell(long seed, int level, int cx, int cz) {
        int mx = (int) Math.floor(cellCenter(cx)), mz = (int) Math.floor(cellCenter(cz));
        if (BackroomsZones.zoneAt(seed, mx, mz) != BackroomsZones.Zone.CLASSIC) {
            // the special zones only have a ground floor
            return level == 0 && BackroomsZones.standable(seed, mx, mz) && BackroomsZones.standable(seed, mx + 1, mz)
                    && BackroomsZones.standable(seed, mx, mz + 1);
        }
        if (connector(seed, cx, cz).kind() != ConnectorKind.NONE) return false;
        BackroomsRooms.Room room = BackroomsRooms.roomOfCell(seed, level, cx, cz);
        if (room == null) return true;
        return room.type() == BackroomsRooms.Type.HALL && !(room.atrium() && level == 1);
    }

    // ------------------------------------------------------------------ full columns

    /**
     * Every block of the column at (x, z), from y = 0 to {@link #ROOF_Y}. Null means air.
     * The middle 3x3 of each cell never gets decorations, so landing and spawn spots stay clear.
     */
    public static BlockState[] column(long seed, int x, int z) {
        BackroomsZones.Zone zone = BackroomsZones.zoneAt(seed, x, z);
        if (zone != BackroomsZones.Zone.CLASSIC) return BackroomsZones.column(seed, x, z, zone);
        BlockState[] out = new BlockState[ROOF_Y + 1];
        out[0] = Blocks.BEDROCK.defaultBlockState();
        out[ROOF_Y] = Blocks.BEDROCK.defaultBlockState();

        ThemeHit hit = themeAt(seed, x, z);
        Connector connector = connector(seed, Math.floorDiv(x, CELL), Math.floorDiv(z, CELL));
        BackroomsRooms.Room[] rooms = new BackroomsRooms.Room[LEVELS];
        for (int level = 0; level < LEVELS; level++) {
            rooms[level] = BackroomsRooms.roomAt(seed, level, x, z);
            fillLevel(out, seed, level, x, z, hit, connector.kind() == ConnectorKind.NONE && rooms[level] == null);
        }
        for (int level = 0; level < LEVELS; level++) {
            if (rooms[level] != null) BackroomsRooms.furnish(out, rooms[level], level, x, z);
        }
        applyConnector(out, connector, Math.floorMod(x, CELL), Math.floorMod(z, CELL));
        BackroomsZones.applyHole(out, seed, x, z);
        return out;
    }

    private static void fillLevel(BlockState[] out, long seed, int level, int x, int z, @Nullable ThemeHit hit, boolean mayDecorate) {
        BackroomsTheme theme = hit == null ? null : hit.theme();
        double strength = hit == null ? 0 : hit.strength();
        int floorY = floorY(level), roomMin = floorY + 1, roomMax = floorY + 3, ceilingY = floorY + 4;

        boolean floorThemed = theme != null && chance(seed, x, floorY, z, 11, strength * 2.2);
        out[floorY] = floorThemed ? BackroomsTheme.pick(theme.floors, hash(seed, x, z, 21 + floorY))
                : ModBlocks.BACKROOMS_CARPET.get().defaultBlockState();

        boolean wall = isWall(seed, level, x, z);
        if (wall) {
            for (int y = roomMin; y <= roomMax; y++) {
                boolean themed = theme != null && chance(seed, x, y, z, 12, strength * 1.6);
                out[y] = themed ? BackroomsTheme.pick(theme.walls, hash(seed, x, z, 22 + y))
                        : ModBlocks.BACKROOMS_WALLPAPER.get().defaultBlockState();
            }
        }

        boolean ceilingThemed = theme != null && chance(seed, x, ceilingY, z, 13, strength * 1.4);
        Light light = light(seed, level, x, z);
        BlockState ceilingTile = ModBlocks.BACKROOMS_CEILING.get().defaultBlockState();
        if (light != Light.NONE) {
            if (ceilingThemed && theme.light != null) {
                out[ceilingY] = theme.light;
            } else {
                boolean broken = light == Light.BROKEN
                        || (theme != null && chance(seed, x, ceilingY, z, 14, strength * theme.extraBroken * 2));
                BlockState fluorescent = ModBlocks.FLUORESCENT_LIGHT.get().defaultBlockState();
                out[ceilingY] = broken ? FluorescentLightBlock.broken(fluorescent) : fluorescent;
            }
        } else {
            BlockState picked = ceilingThemed ? BackroomsTheme.pick(theme.ceilings, hash(seed, x, z, 23 + ceilingY)) : ceilingTile;
            out[ceilingY] = picked.isAir() ? ceilingTile : picked;  // air in a palette means "normal tile"
        }

        if (mayDecorate && !wall && isDecorationSpot(x, z) && theme != null && !(level == 0 && isChestSpot(seed, x, z))) {
            if (floorThemed && theme.floorDecoration != null && unit(hash(seed, x, z, 15 + floorY)) < theme.decorationChance) {
                BlockState[] plant = theme.floorDecoration.place(out[floorY], hash(seed, x, z, 16 + floorY));
                if (plant != null) {
                    for (int i = 0; i < plant.length; i++) out[roomMin + i] = plant[i];
                }
            }
            if (ceilingThemed && light == Light.NONE && theme.ceilingDecoration != null && out[roomMax] == null
                    && unit(hash(seed, x, z, 17 + floorY)) < theme.decorationChance) {
                out[roomMax] = theme.ceilingDecoration.place(out[ceilingY], hash(seed, x, z, 18 + floorY));
            }
        }
    }

    /** Next to the cell's edges, but never in its middle 3x3 or on a wall line. */
    private static boolean isDecorationSpot(int x, int z) {
        int lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
        if (lx == 0 || lz == 0) return false;
        return !(lx >= 2 && lx <= 4 && lz >= 2 && lz <= 4);
    }

    // ------------------------------------------------------------------ connectors between floors

    public enum ConnectorKind { NONE, STAIRS, SLIDE, LADDER, PIT }

    /** {@code direction} is the way up (stairs/slides climb towards it). */
    public record Connector(ConnectorKind kind, Direction direction) {
    }

    private static final Connector NO_CONNECTOR = new Connector(ConnectorKind.NONE, Direction.NORTH);
    /** Roughly one cell in this many links the two floors. */
    private static final int CONNECTOR_RARITY = 14;

    public static Connector connector(long seed, int cx, int cz) {
        if (unit(hash(seed, cx, cz, 500)) >= 1.0 / CONNECTOR_RARITY) return NO_CONNECTOR;
        if (BackroomsZones.zoneAt(seed, cx * CELL + 3, cz * CELL + 3) != BackroomsZones.Zone.CLASSIC) return NO_CONNECTOR;
        if (nearHole(seed, cx, cz)) return NO_CONNECTOR;
        if (BackroomsRooms.roomOfCell(seed, 0, cx, cz) != null || BackroomsRooms.roomOfCell(seed, 1, cx, cz) != null) return NO_CONNECTOR;
        long h = hash(seed, cx, cz, 501);
        double u = unit(h);
        ConnectorKind kind = u < 0.35 ? ConnectorKind.STAIRS : u < 0.6 ? ConnectorKind.SLIDE : u < 0.85 ? ConnectorKind.LADDER : ConnectorKind.PIT;
        return new Connector(kind, Direction.from2DDataValue((int) Math.floorMod(h >>> 20, 4L)));
    }

    /**
     * Carves a connector into a column. Stairs and slides are a 5-step run through the middle
     * row of the cell, from the lower floor up to the upper floor, with the ceiling and upper
     * floor opened above them. Ladders climb through a 1x1 hole beside a pillar; pits are a
     * 2x2 hole in the upper floor.
     */
    private static void applyConnector(BlockState[] out, Connector connector, int lx, int lz) {
        int upperFloor = floorY(1), lowerCeiling = CEILING_Y;
        switch (connector.kind()) {
            case STAIRS, SLIDE -> {
                int step = runStep(connector.direction(), lx, lz);
                if (step == 0) return;
                boolean slide = connector.kind() == ConnectorKind.SLIDE;
                BlockState support = slide ? Blocks.YELLOW_CONCRETE.defaultBlockState() : Blocks.SMOOTH_SANDSTONE.defaultBlockState();
                BlockState stair = (slide ? ModBlocks.SLIDE.get() : Blocks.SMOOTH_SANDSTONE_STAIRS).defaultBlockState()
                        .setValue(StairBlock.FACING, connector.direction());
                for (int y = ROOM_MIN_Y; y <= step; y++) out[y] = support;
                out[FLOOR_Y + step] = stair;                               // step tops rise from y 2 to y 6
                if (step == 2 || step == 3) out[lowerCeiling] = null;       // head room while climbing
                if (step == 3 || step == 4) out[upperFloor] = null;
            }
            case LADDER -> {
                if (lx == 2 && lz == 2) {                                   // the pillar the ladder hangs on
                    BlockState pillar = ModBlocks.BACKROOMS_WALLPAPER.get().defaultBlockState();
                    for (int y = ROOM_MIN_Y; y <= ROOM_MAX_Y; y++) out[y] = pillar;
                    for (int y = upperFloor + 1; y <= upperFloor + 3; y++) out[y] = pillar;
                } else if (lx == 3 && lz == 2) {
                    BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
                    for (int y = ROOM_MIN_Y; y <= upperFloor + 2; y++) out[y] = ladder;
                }
            }
            case PIT -> {
                if (lx >= 3 && lx <= 4 && lz >= 3 && lz <= 4) {
                    out[lowerCeiling] = null;
                    out[upperFloor] = null;
                }
            }
            case NONE -> {
            }
        }
    }

    /** Position (1..5) along a stair/slide run climbing towards {@code up}, or 0 if not on it. */
    private static int runStep(Direction up, int lx, int lz) {
        int middle = CELL / 2;
        return switch (up) {
            case EAST -> lz == middle && lx >= 1 ? lx : 0;
            case WEST -> lz == middle && lx >= 1 ? CELL - lx : 0;
            case SOUTH -> lx == middle && lz >= 1 ? lz : 0;
            default -> lx == middle && lz >= 1 ? CELL - lz : 0;   // NORTH
        };
    }

    // ------------------------------------------------------------------ biome patches

    /** Size of the grid that small patches are scattered on (at most one per grid square). */
    public static final int PATCH_GRID = 64;
    private static final double PATCH_CHANCE = 0.45;
    /** Huge patches are rare and sit on a much coarser grid. */
    public static final int MEGA_GRID = 400;
    private static final double MEGA_CHANCE = 0.3;

    public record Patch(int x, int z, int radius, BackroomsTheme theme) {
    }

    public record ThemeHit(BackroomsTheme theme, double strength) {
    }

    public static @Nullable Patch patch(long seed, int gx, int gz) {
        if (unit(hash(seed, gx, gz, 300)) >= PATCH_CHANCE) return null;
        long h = hash(seed, gx, gz, 301);
        int x = gx * PATCH_GRID + 12 + (int) Math.floorMod(h >>> 8, (long) (PATCH_GRID - 24));
        int z = gz * PATCH_GRID + 12 + (int) Math.floorMod(h >>> 24, (long) (PATCH_GRID - 24));
        int radius = 10 + (int) Math.floorMod(h >>> 40, 13L);
        return new Patch(x, z, radius, theme(hash(seed, gx, gz, 302)));
    }

    /** A rare huge patch, 50 to 110 blocks across... in radius. */
    public static @Nullable Patch megaPatch(long seed, int gx, int gz) {
        if (unit(hash(seed, gx, gz, 310)) >= MEGA_CHANCE) return null;
        long h = hash(seed, gx, gz, 311);
        int radius = 50 + (int) Math.floorMod(h >>> 40, 61L);
        int margin = radius;
        int x = gx * MEGA_GRID + margin + (int) Math.floorMod(h >>> 8, (long) (MEGA_GRID - 2 * margin));
        int z = gz * MEGA_GRID + margin + (int) Math.floorMod(h >>> 24, (long) (MEGA_GRID - 2 * margin));
        return new Patch(x, z, radius, theme(hash(seed, gx, gz, 312)));
    }

    private static BackroomsTheme theme(long hash) {
        BackroomsTheme[] themes = BackroomsTheme.values();
        return themes[(int) Math.floorMod(hash, (long) themes.length)];
    }

    /** The strongest patch covering (x, z); strength is 1 in its core and fades to 0 at its edge. */
    public static @Nullable ThemeHit themeAt(long seed, int x, int z) {
        ThemeHit best = null;
        int gx = Math.floorDiv(x, PATCH_GRID), gz = Math.floorDiv(z, PATCH_GRID);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                best = stronger(best, patch(seed, gx + dx, gz + dz), x, z, 1.0);
            }
        }
        // Mega patches have a big solid core and a ~25 block fuzzy edge.
        Patch mega = megaPatch(seed, Math.floorDiv(x, MEGA_GRID), Math.floorDiv(z, MEGA_GRID));
        return stronger(best, mega, x, z, mega == null ? 1.0 : mega.radius / 25.0);
    }

    private static @Nullable ThemeHit stronger(@Nullable ThemeHit best, @Nullable Patch patch, int x, int z, double sharpness) {
        if (patch == null) return best;
        double strength = Math.min(1.0, (1.0 - Math.hypot(x - patch.x, z - patch.z) / patch.radius) * sharpness);
        if (strength <= 0 || (best != null && strength <= best.strength())) return best;
        return new ThemeHit(patch.theme, strength);
    }

    private static boolean chance(long seed, int x, int y, int z, int salt, double probability) {
        return unit(hash(seed, x, z, salt * 131 + y)) < probability;
    }

    // ------------------------------------------------------------------ hidden chests

    public enum ChestKind { NONE, LOOT, PORTAL_DROP }

    /** Roughly one cell in this many has a chest (on the lower floor). */
    private static final int CHEST_RARITY = 50;

    public static ChestKind chest(long seed, int cx, int cz) {
        if (unit(hash(seed, cx, cz, 400)) >= 1.0 / CHEST_RARITY) return ChestKind.NONE;
        if (BackroomsZones.zoneAt(seed, cx * CELL + 1, cz * CELL + 1) != BackroomsZones.Zone.CLASSIC) return ChestKind.NONE;
        if (BackroomsRooms.roomOfCell(seed, 0, cx, cz) != null) return ChestKind.NONE;   // rooms have their own cupboards
        return unit(hash(seed, cx, cz, 401)) < 0.25 ? ChestKind.PORTAL_DROP : ChestKind.LOOT;
    }

    private static boolean isChestSpot(long seed, int x, int z) {
        return Math.floorMod(x, CELL) == 1 && Math.floorMod(z, CELL) == 1
                && chest(seed, Math.floorDiv(x, CELL), Math.floorDiv(z, CELL)) != ChestKind.NONE;
    }

    /** Chests sit in the corner of their cell, out of the way of the middle. */
    public static BlockPos chestPos(int cx, int cz) {
        return new BlockPos(cx * CELL + 1, ROOM_MIN_Y, cz * CELL + 1);
    }

    public static long chestHash(long seed, int cx, int cz) {
        return hash(seed, cx, cz, 402);
    }

    /** Cells next to the Hole don't get stairs or ladders (they'd cut through it). */
    private static boolean nearHole(long seed, int cx, int cz) {
        int[] hole = BackroomsZones.hole(seed, Math.floorDiv(cx, 8), Math.floorDiv(cz, 8));
        if (hole == null) return false;
        int hx = Math.floorDiv(hole[0], CELL), hz = Math.floorDiv(hole[1], CELL);
        return (cx == hx || cx == hx - 1) && cz == hz;
    }

    // ------------------------------------------------------------------ internals

    /** dir 0 = wall line along z (at x = cx*CELL), dir 1 = along x (at z = cz*CELL). */
    private static boolean segmentExists(long seed, int cx, int cz, int dir) {
        return unit(hash(seed, cx, cz, dir)) < density(seed, cx, cz);
    }

    private static boolean segmentBlocks(long seed, int cx, int cz, int dir, int along) {
        if (!segmentExists(seed, cx, cz, dir)) return false;
        long door = hash(seed, cx, cz, dir + 10);
        if (unit(door) < 0.55) {
            int gapStart = 1 + (int) Math.floorMod(door >>> 24, (long) (CELL - 3));
            if (along == gapStart || along == gapStart + 1) return false;
        }
        return true;
    }

    private static boolean hasPillar(long seed, int cx, int cz) {
        return segmentExists(seed, cx, cz, 0) || segmentExists(seed, cx, cz - 1, 0)
                || segmentExists(seed, cx, cz, 1) || segmentExists(seed, cx - 1, cz, 1)
                || unit(hash(seed, cx, cz, 20)) < 0.12;
    }

    private static double density(long seed, int cx, int cz) {
        double u = unit(hash(seed, Math.floorDiv(cx, REGION), Math.floorDiv(cz, REGION), 99));
        if (u < 0.15) return 0.08;          // big open hall
        if (u > 0.85) return 0.75;          // tight maze
        return Mth.lerp((u - 0.15) / 0.7, 0.3, 0.55);
    }

    private static long hash(long seed, int a, int b, int salt) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }
}
