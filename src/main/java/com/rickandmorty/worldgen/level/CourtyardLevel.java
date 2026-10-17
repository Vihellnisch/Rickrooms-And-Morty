package com.rickandmorty.worldgen.level;

import com.rickandmorty.entity.MimicKind;
import com.rickandmorty.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * Level 188, the Courtyard of Windows: enormous indoor courtyards, 48 blocks across and 60
 * high, walled on every side by row upon row of identical windows. Most are dark. A few are
 * lit. In a very few, something is standing there, watching. Lawns and paths below, a flat
 * white ceiling far above, tunnels at ground level to the next courtyard.
 */
final class CourtyardLevel implements LevelGenerator {
    static final int PERIOD = 72;
    static final int OPEN_MIN = 12, OPEN_MAX = 59;     // the courtyard, in local coordinates
    static final int TOP = 62;
    /** Where the exit door is on the west wall (clear of the tunnel, which is at 34..36). */
    static final int DOOR_Z = 24;
    private static final int FIRST_ROW = 5, ROW = 5, ROWS = 11;

    enum Pane { WALL, DARK, WARM, COOL, FIGURE }

    private final BlockState bedrock = of(Blocks.BEDROCK), concrete = of(Blocks.WHITE_CONCRETE), glass = of(Blocks.GRAY_STAINED_GLASS);
    private final BlockState path = of(Blocks.POLISHED_DIORITE), lawn = of(Blocks.GRASS_BLOCK), wood = of(Blocks.SPRUCE_PLANKS);
    private final BlockState frame = of(Blocks.BLACK_CONCRETE), warm = of(Blocks.GLOWSTONE), cool = of(Blocks.SEA_LANTERN);

    @Override
    public int height() {
        return TOP + 2;
    }

    @Override
    public int maxStandY() {
        return 2;
    }

    private static boolean open(int local) {
        return local >= OPEN_MIN && local <= OPEN_MAX;
    }

    /** What's in the window at position {@code along} on a face, on row {@code row} (or WALL). */
    Pane pane(long seed, int courtyardX, int courtyardZ, int face, int along, int row) {
        int a = Math.floorMod(along, 6);
        if (a != 2 && a != 3) return Pane.WALL;
        int window = Math.floorDiv(along, 6);
        double u = unit(hash(seed, courtyardX * 4 + face, courtyardZ, window * 16 + row, 60));
        return u < 0.012 ? Pane.FIGURE : u < 0.1 ? Pane.WARM : u < 0.15 ? Pane.COOL : Pane.DARK;
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[height()];
        c[0] = bedrock;
        c[TOP + 1] = bedrock;
        int lx = Math.floorMod(x, PERIOD), lz = Math.floorMod(z, PERIOD);
        int cx = Math.floorDiv(x, PERIOD), cz = Math.floorDiv(z, PERIOD);
        if (open(lx) && open(lz)) {
            courtyardFloor(c, lx - OPEN_MIN, lz - OPEN_MIN);
            c[TOP] = concrete;
            return c;
        }
        // the buildings: solid, except for window faces and ground-floor tunnels
        LevelParts.fill(c, 1, TOP, concrete);
        int[] face = faceOf(lx, lz);
        if (face != null) {
            int depth = face[1], along = face[0] == 0 || face[0] == 1 ? z : x;
            for (int row = 0; row < ROWS; row++) {
                int y = FIRST_ROW + row * ROW;
                Pane pane = pane(seed, cx, cz, face[0], along, row);
                if (pane == Pane.WALL) continue;
                BlockState fill = switch (depth) {
                    case 0 -> glass;
                    case 1 -> pane == Pane.WARM ? warm : pane == Pane.COOL ? cool : pane == Pane.FIGURE ? null : frame;
                    case 2 -> pane == Pane.FIGURE ? null : concrete;
                    default -> pane == Pane.FIGURE ? warm : concrete;
                };
                c[y] = fill;
                c[y + 1] = fill;
                if (pane == Pane.FIGURE && depth > 0) c[y + 2] = depth == 3 ? warm : null;   // head room for whoever it is
            }
            // one exit door per courtyard, in its west wall
            if (face[0] == 0 && depth == 0 && Math.floorMod(z, PERIOD) == DOOR_Z && unit(seed, cx, cz, 61) < 0.6) {
                c[1] = path;
                LevelParts.exitDoor(c, 2, Direction.EAST);
            }
        }
        // tunnels through to the neighbouring courtyards
        boolean tunnelZ = lx >= 34 && lx <= 36 && !open(lz);
        boolean tunnelX = lz >= 34 && lz <= 36 && !open(lx);
        if (tunnelX || tunnelZ) {
            c[1] = path;
            LevelParts.fill(c, 2, 4, null);
        }
        return c;
    }

    /** {face, depth} if this building column is one of the (up to 4) columns behind a courtyard wall. */
    private static int @Nullable [] faceOf(int lx, int lz) {
        if (open(lz)) {
            if (lx < OPEN_MIN && lx >= OPEN_MIN - 4) return new int[]{0, OPEN_MIN - 1 - lx};          // west wall
            if (lx > OPEN_MAX && lx <= OPEN_MAX + 4) return new int[]{1, lx - OPEN_MAX - 1};          // east wall
        }
        if (open(lx)) {
            if (lz < OPEN_MIN && lz >= OPEN_MIN - 4) return new int[]{2, OPEN_MIN - 1 - lz};          // north wall
            if (lz > OPEN_MAX && lz <= OPEN_MAX + 4) return new int[]{3, lz - OPEN_MAX - 1};          // south wall
        }
        return null;
    }

    /** Lawns, stone paths and a wooden circle in the middle, like a hotel atrium. */
    private void courtyardFloor(BlockState[] c, int u, int v) {
        double du = u - 23.5, dv = v - 23.5, r = Math.sqrt(du * du + dv * dv);
        BlockState floor;
        if (r < 6) floor = wood;
        else if (r < 7.5 || u < 3 || u > 44 || v < 3 || v > 44 || Math.abs(du) < 2 || Math.abs(dv) < 2) floor = path;
        else floor = lawn;
        c[1] = floor;
    }

    /** Whatever is standing in the lit windows. */
    @Override
    public void populate(WorldGenRegion region, long seed) {
        ChunkPos chunk = region.getCenter();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                int lx = Math.floorMod(x, PERIOD), lz = Math.floorMod(z, PERIOD);
                int[] face = faceOf(lx, lz);
                if (face == null || face[1] != 1) continue;
                int along = face[0] <= 1 ? z : x;
                if (Math.floorMod(along, 6) != 2) continue;           // one figure per window
                for (int row = 0; row < ROWS; row++) {
                    if (pane(seed, Math.floorDiv(x, PERIOD), Math.floorDiv(z, PERIOD), face[0], along, row) != Pane.FIGURE) continue;
                    var mimic = ModEntities.MIMICS.get(MimicKind.VILLAGER).get().create(region.getLevel());
                    if (mimic == null) continue;
                    float yaw = switch (face[0]) {
                        case 0 -> -90.0f;
                        case 1 -> 90.0f;
                        case 2 -> 0.0f;
                        default -> 180.0f;
                    };
                    mimic.moveTo(x + 0.5, FIRST_ROW + row * ROW, z + 0.5, yaw, 0.0f);
                    mimic.finalizeSpawn(region, region.getCurrentDifficultyAt(BlockPos.containing(x, FIRST_ROW + row * ROW, z)), MobSpawnType.STRUCTURE, null);
                    mimic.setPersistenceRequired();
                    region.addFreshEntity(mimic);
                }
            }
        }
    }

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        // most lead on to the Wooden Hotel; a few, to the Poolrooms
        double roll = unit(seed, pos.getX(), pos.getZ(), 62);
        return roll < 0.15 ? BackroomsLevel.STAIRCASE : roll < 0.3 ? BackroomsLevel.POOLROOMS : roll < 0.45 ? BackroomsLevel.DEAD_MALL : BackroomsLevel.WOODEN_HOTEL;
    }
}
