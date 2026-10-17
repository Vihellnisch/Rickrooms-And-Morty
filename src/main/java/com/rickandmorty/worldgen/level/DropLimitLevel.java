package com.rickandmorty.worldgen.level;

import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.worldgen.site.SiteBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.noise;
import static com.rickandmorty.worldgen.level.GenMath.unit;

/**
 * Drop Limit. High above a sea of cloud that goes on forever:
 * grassy islands with sheer cliffs, roads that wind across them and run off the edge, with a
 * sign where they do (DROP LIMIT ?????), long open-air concrete galleries hung across the sky,
 * grown over with meadow, and here and there a bare concrete tower of open floors rising out of
 * the clouds. The clouds hold nothing up: step off, and you sink through them, and fall out of
 * the bottom of the level.
 * <p>
 * The cloud sea's top is lumpy, y 58-67, and 8 deep; islands stand from inside it up to y 80-87.
 * Galleries float at y 89-97, so they pass over islands as well as over the clouds.
 */
public final class DropLimitLevel implements LevelGenerator {
    public static final int HEIGHT = 192, CLOUD = 58, ISLAND_TOP = 80, GALLERY = 89, GALLERY_TOP = 97;
    /** Spacing of the roads that run east-west (by z) and north-south (by x), and of the galleries. */
    public static final int ROAD_X_LANE = 190, ROAD_Z_LANE = 230, GALLERY_X_LANE = 300, GALLERY_Z_LANE = 340, SEGMENT = 420, TOWER_CELL = 380, TREE_CELL = 11;

    private static BlockState of(Block block) {
        return block.defaultBlockState();
    }

    private final BlockState grass = of(Blocks.GRASS_BLOCK), dirt = of(Blocks.DIRT), stone = of(Blocks.STONE), andesite = of(Blocks.ANDESITE),
            tuff = of(Blocks.TUFF), mossyCobble = of(Blocks.MOSSY_COBBLESTONE), moss = of(Blocks.MOSS_BLOCK), calcite = of(Blocks.CALCITE),
            asphalt = of(Blocks.GRAY_CONCRETE), whiteLine = of(Blocks.WHITE_CONCRETE), yellowLine = of(Blocks.YELLOW_CONCRETE),
            concrete = of(Blocks.WHITE_CONCRETE), smooth = of(Blocks.SMOOTH_STONE), grey = of(Blocks.LIGHT_GRAY_CONCRETE),
            pillar = of(Blocks.QUARTZ_PILLAR), lamp = of(Blocks.PEARLESCENT_FROGLIGHT), post = of(Blocks.IRON_BARS),
            shortGrass = of(Blocks.SHORT_GRASS), azalea = of(Blocks.AZALEA_LEAVES).setValue(LeavesBlock.PERSISTENT, true),
            flowering = of(Blocks.FLOWERING_AZALEA_LEAVES).setValue(LeavesBlock.PERSISTENT, true);
    private final BlockState[] flowers = {of(Blocks.DANDELION), of(Blocks.OXEYE_DAISY), of(Blocks.AZURE_BLUET), of(Blocks.CORNFLOWER), of(Blocks.WHITE_TULIP)};

    private static BlockState cloud() {
        return ModBlocks.CLOUD.get().defaultBlockState();
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public int minStandY() {
        return CLOUD + 10;
    }

    // ------------------------------------------------------------------ the shape of things

    /** The top of the cloud sea: billows on billows, each a dome, big ones with little ones on them. */
    public static int cloudTop(long seed, int x, int z) {
        double big = domes(seed, x, z, 15, 6.0, 11.0, 6.5, 9100), small = domes(seed, x, z, 6, 2.5, 4.5, 2.5, 9101);
        return CLOUD + (int) Math.round(Math.max(big, 0.6 + small * 0.4) + small * 0.6);
    }

    /** The tallest dome over (x, z): one per {@code cell}-block square, of radius {@code minR}..{@code maxR} and up to {@code height} tall. */
    private static double domes(long seed, int x, int z, int cell, double minR, double maxR, double height, int salt) {
        int cx = Math.floorDiv(x, cell), cz = Math.floorDiv(z, cell);
        double best = 0;
        for (int i = cx - 1; i <= cx + 1; i++) {
            for (int j = cz - 1; j <= cz + 1; j++) {
                long h = hash(seed, i, j, salt);
                double px = (i + unit(h)) * cell, pz = (j + unit(Long.rotateLeft(h, 21))) * cell;
                double radius = minR + unit(Long.rotateLeft(h, 42)) * (maxR - minR), d2 = ((x + 0.5 - px) * (x + 0.5 - px) + (z + 0.5 - pz) * (z + 0.5 - pz)) / (radius * radius);
                if (d2 < 1) best = Math.max(best, height * (0.55 + 0.45 * unit(Long.rotateLeft(h, 9))) * Math.sqrt(1 - d2));
            }
        }
        return best;
    }

    /** How "island" a spot is; an island where this passes 0.6. */
    public static double islandness(long seed, int x, int z) {
        return noise(seed, x, z, 150.0, 9110) * 0.7 + noise(seed, x, z, 40.0, 9111) * 0.22 + noise(seed, x, z, 13.0, 9112) * 0.08;
    }

    public static boolean island(long seed, int x, int z) {
        return islandness(seed, x, z) > 0.63;
    }

    /** The height of the grass on an island: gentle and smooth, so roads lie flat on it. */
    public static int islandTop(long seed, int x, int z) {
        return ISLAND_TOP + (int) Math.round(noise(seed, x, z, 320.0, 9113) * 4);
    }

    /** A loose puff of cloud floating above the sea, between the islands. Returns its {bottom, top} or null. */
    public static int @Nullable [] puff(long seed, int x, int z) {
        double n = noise(seed, x, z, 23.0, 9120) * 0.75 + noise(seed, x, z, 8.0, 9121) * 0.25;
        if (n < 0.79 || noise(seed, x, z, 120.0, 9122) < 0.55) return null;
        int middle = CLOUD + 16 + (int) Math.round(noise(seed, x, z, 90.0, 9123) * 10);
        int half = (int) Math.round((n - 0.79) * 26);
        return half <= 0 ? null : new int[]{middle - half / 2, middle + half};
    }

    // ------------------------------------------------------------------ roads

    /** A road running east-west: where its middle is at x (z), or MIN_VALUE if lane {@code lane} has no road. */
    public static int roadZ(long seed, int lane, int x) {
        long h = hash(seed, lane, 0, 9130);
        if (unit(h) > 0.72) return Integer.MIN_VALUE;
        double amplitude = 10 + unit(hash(seed, lane, 1, 9130)) * 22, period = 170 + unit(hash(seed, lane, 2, 9130)) * 170;
        double phase = unit(hash(seed, lane, 3, 9130)) * Math.PI * 2;
        int base = lane * ROAD_X_LANE + 40 + (int) Math.floorMod(h >>> 20, 110L);
        return base + (int) Math.round(amplitude * Math.sin(x / period * Math.PI * 2 + phase));
    }

    public static int roadX(long seed, int lane, int z) {
        long h = hash(seed, lane, 0, 9131);
        if (unit(h) > 0.6) return Integer.MIN_VALUE;
        double amplitude = 10 + unit(hash(seed, lane, 1, 9131)) * 22, period = 170 + unit(hash(seed, lane, 2, 9131)) * 170;
        double phase = unit(hash(seed, lane, 3, 9131)) * Math.PI * 2;
        int base = lane * ROAD_Z_LANE + 40 + (int) Math.floorMod(h >>> 20, 150L);
        return base + (int) Math.round(amplitude * Math.sin(z / period * Math.PI * 2 + phase));
    }

    /** Signed distance across the nearest east-west road at (x, z) (its middle is 0), or MIN_VALUE if none is near. */
    public static int acrossRoadZ(long seed, int x, int z) {
        int lane = Math.floorDiv(z, ROAD_X_LANE);
        for (int l = lane - 1; l <= lane + 1; l++) {
            int middle = roadZ(seed, l, x);
            if (middle != Integer.MIN_VALUE && Math.abs(z - middle) <= 6) return z - middle;
        }
        return Integer.MIN_VALUE;
    }

    public static int acrossRoadX(long seed, int x, int z) {
        int lane = Math.floorDiv(x, ROAD_Z_LANE);
        for (int l = lane - 1; l <= lane + 1; l++) {
            int middle = roadX(seed, l, z);
            if (middle != Integer.MIN_VALUE && Math.abs(x - middle) <= 6) return x - middle;
        }
        return Integer.MIN_VALUE;
    }

    /** The lane of the east-west (eastWest) or north-south road within 6 of (x, z), or MIN_VALUE. */
    public static int lane(long seed, int x, int z, boolean eastWest) {
        int lane = Math.floorDiv(eastWest ? z : x, eastWest ? ROAD_X_LANE : ROAD_Z_LANE);
        for (int l = lane - 1; l <= lane + 1; l++) {
            int middle = eastWest ? roadZ(seed, l, x) : roadX(seed, l, z);
            if (middle != Integer.MIN_VALUE && Math.abs((eastWest ? z : x) - middle) <= 6) return l;
        }
        return Integer.MIN_VALUE;
    }

    /** The road in {@code lane}, at point {@code along} along it, is on land. */
    private static boolean onLand(long seed, int lane, int along, boolean eastWest) {
        return eastWest ? island(seed, along, roadZ(seed, lane, along)) : island(seed, roadX(seed, lane, along), along);
    }

    /** Going {@code step} (+1 / -1) along the road in {@code lane}, does it stay on land for 5 more blocks from {@code along}, then run off the edge? */
    public static boolean endsAhead(long seed, int lane, int along, boolean eastWest, int step) {
        if (lane == Integer.MIN_VALUE) return false;
        for (int k = 0; k <= 4; k++) if (!onLand(seed, lane, along + k * step, eastWest)) return false;
        return !onLand(seed, lane, along + 5 * step, eastWest);
    }

    // ------------------------------------------------------------------ galleries

    /** A gallery: the line it runs along (z for east-west ones), its two ends along the run, and whether the far end has a door. */
    public record Gallery(boolean eastWest, int line, int from, int to, boolean door) {
    }

    public static @Nullable Gallery gallery(long seed, boolean eastWest, int lane, int segment) {
        long h = hash(seed, lane, segment, eastWest ? 9140 : 9141);
        if (unit(h) > 0.5) return null;
        int spacing = eastWest ? GALLERY_X_LANE : GALLERY_Z_LANE;
        int line = lane * spacing + spacing / 2 + (int) Math.floorMod(h >>> 16, 61L) - 30;
        int from = segment * SEGMENT + (int) Math.floorMod(h >>> 24, 70L), to = segment * SEGMENT + 260 + (int) Math.floorMod(h >>> 32, 150L);
        return new Gallery(eastWest, line, from, to, unit(hash(seed, lane, segment, 9142)) < 0.4);
    }

    /** The gallery covering (x, z) (within 6 of its line), if any. */
    public static @Nullable Gallery galleryAt(long seed, int x, int z) {
        for (boolean eastWest : new boolean[]{true, false}) {
            int across = eastWest ? z : x, along = eastWest ? x : z, spacing = eastWest ? GALLERY_X_LANE : GALLERY_Z_LANE;
            int lane = Math.floorDiv(across, spacing), segment = Math.floorDiv(along, SEGMENT);
            for (int l = lane - 1; l <= lane + 1; l++) {
                Gallery g = gallery(seed, eastWest, l, segment);
                if (g != null && Math.abs(across - g.line) <= 6 && along >= g.from && along <= g.to) return g;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ towers

    /** A bare concrete tower out of the clouds: its middle, and the height of its top floor. */
    public record Tower(int x, int z, int top) {
    }

    public static @Nullable Tower tower(long seed, int cx, int cz) {
        long h = hash(seed, cx, cz, 9150);
        if (unit(h) > 0.45) return null;
        int x = cx * TOWER_CELL + 40 + (int) Math.floorMod(h >>> 16, TOWER_CELL - 80L), z = cz * TOWER_CELL + 40 + (int) Math.floorMod(h >>> 28, TOWER_CELL - 80L);
        int floors = 8 + (int) Math.floorMod(h >>> 40, 12L);
        return new Tower(x, z, CLOUD + 2 + floors * 6);
    }

    public static @Nullable Tower towerAt(long seed, int x, int z) {
        Tower t = tower(seed, Math.floorDiv(x, TOWER_CELL), Math.floorDiv(z, TOWER_CELL));
        return t != null && Math.abs(x - t.x) <= 6 && Math.abs(z - t.z) <= 6 ? t : null;
    }

    // ------------------------------------------------------------------ the column

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[HEIGHT];
        int cloudTop = cloudTop(seed, x, z);
        for (int y = CLOUD - 8; y <= cloudTop; y++) c[y] = cloud();
        int[] puff = puff(seed, x, z);

        boolean land = island(seed, x, z);
        if (land) island(c, seed, x, z);
        else if (puff != null) for (int y = puff[0]; y <= puff[1]; y++) c[y] = cloud();

        Tower tower = towerAt(seed, x, z);
        if (tower != null) tower(c, seed, x, z, tower);
        Gallery gallery = galleryAt(seed, x, z);
        if (gallery != null) gallery(c, seed, x, z, gallery);
        return c;
    }

    private void island(BlockState[] c, long seed, int x, int z) {
        int top = islandTop(seed, x, z);
        boolean edge = !island(seed, x + 1, z) || !island(seed, x - 1, z) || !island(seed, x, z + 1) || !island(seed, x, z - 1);
        boolean nearEdge = edge || !island(seed, x + 2, z) || !island(seed, x - 2, z) || !island(seed, x, z + 2) || !island(seed, x, z - 2);
        int bottom = CLOUD - 6 + (int) Math.floorMod(hash(seed, x, z, 9160), 3L);
        for (int y = bottom; y < top; y++) {
            if (nearEdge) {
                // the cliff face: grey rock, banded, with moss and the odd tuft of grass
                double band = noise(seed, x + y * 7, z - y * 5, 5.0, 9161);
                long h = hash(seed, x, y, z, 9162);
                BlockState rock = band < 0.35 ? andesite : band > 0.75 ? tuff : stone;
                if (y >= top - 3) rock = unit(h) < 0.5 ? dirt : moss;
                else if (y >= top - 9 && unit(h) < 0.18) rock = unit(h) < 0.08 ? grass : mossyCobble;
                else if (unit(h) < 0.03) rock = calcite;
                c[y] = rock;
            } else {
                c[y] = y >= top - 3 ? dirt : stone;
            }
        }
        c[top] = edge && unit(hash(seed, x, z, 9163)) < 0.25 ? moss : grass;
        int across = acrossRoadZ(seed, x, z), acrossX = acrossRoadX(seed, x, z);
        boolean eastWestRoad = across != Integer.MIN_VALUE && Math.abs(across) <= 3, northSouthRoad = acrossX != Integer.MIN_VALUE && Math.abs(acrossX) <= 3;
        if ((eastWestRoad || northSouthRoad) && !edge) {
            c[top] = asphalt;
            if (eastWestRoad && !northSouthRoad) c[top] = lineOf(across, x);
            else if (northSouthRoad && !eastWestRoad) c[top] = lineOf(acrossX, z);
            return;
        }
        // DROP LIMIT ?????, on the right-hand verge, five blocks before the road runs out
        int laneZ = across == Integer.MIN_VALUE ? Integer.MIN_VALUE : lane(seed, x, z, true), laneX = acrossX == Integer.MIN_VALUE ? Integer.MIN_VALUE : lane(seed, x, z, false);
        if (across == 5 && endsAhead(seed, laneZ, x, true, 1) || across == -5 && endsAhead(seed, laneZ, x, true, -1)) {
            sign(c, top, across == 5 ? 4 : 12);
            return;
        }
        if (acrossX == -5 && endsAhead(seed, laneX, z, false, 1) || acrossX == 5 && endsAhead(seed, laneX, z, false, -1)) {
            sign(c, top, acrossX == -5 ? 8 : 0);
            return;
        }
        boolean verge = across != Integer.MIN_VALUE || acrossX != Integer.MIN_VALUE;
        if (!verge && !edge && tree(c, seed, x, z)) return;
        if (c[top + 1] != null) return;
        double r = unit(hash(seed, x, z, 9164));
        if (r < 0.32) c[top + 1] = shortGrass;
        else if (r < 0.36 && !verge) tall(c, top + 1, Blocks.TALL_GRASS);
        else if (r < 0.42) c[top + 1] = flowers[(int) (r * 1000) % flowers.length];
    }

    private BlockState lineOf(int across, int along) {
        if (Math.abs(across) == 3) return whiteLine;
        if (across == 0 && Math.floorMod(along, 8) < 4) return yellowLine;
        return asphalt;
    }

    private void sign(BlockState[] c, int top, int rotation) {
        c[top + 1] = post;
        c[top + 2] = post;
        c[top + 3] = of(Blocks.BIRCH_SIGN).setValue(StandingSignBlock.ROTATION, rotation);
    }

    @Override
    public void blockEntity(WorldGenRegion region, long seed, BlockPos pos, BlockState state) {
        if (region.getBlockEntity(pos) instanceof SignBlockEntity sign) SiteBuilder.writeSign(sign, region.registryAccess(), "DROP", "LIMIT", "?????", "");
    }

    private void tall(BlockState[] c, int y, Block block) {
        c[y] = of(block).setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
        c[y + 1] = of(block).setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
    }

    /** Small round trees, far apart, never at the cliff edge or by a road. */
    private boolean tree(BlockState[] c, long seed, int x, int z) {
        int gx = Math.floorDiv(x, TREE_CELL), gz = Math.floorDiv(z, TREE_CELL);
        boolean any = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long h = hash(seed, gx + dx, gz + dz, 9170);
                if (unit(h) > 0.045) continue;
                int tx = (gx + dx) * TREE_CELL + 2 + (int) Math.floorMod(h >>> 16, TREE_CELL - 4L), tz = (gz + dz) * TREE_CELL + 2 + (int) Math.floorMod(h >>> 24, TREE_CELL - 4L);
                if (!island(seed, tx, tz) || !island(seed, tx + 4, tz) || !island(seed, tx - 4, tz) || !island(seed, tx, tz + 4) || !island(seed, tx, tz - 4)) continue;
                if (acrossRoadZ(seed, tx, tz) != Integer.MIN_VALUE || acrossRoadX(seed, tx, tz) != Integer.MIN_VALUE) continue;
                if (galleryAt(seed, tx + 3, tz) != null || galleryAt(seed, tx - 3, tz) != null || galleryAt(seed, tx, tz + 3) != null || galleryAt(seed, tx, tz - 3) != null) continue;
                boolean birch = (h >>> 40 & 1) == 0;
                int base = islandTop(seed, tx, tz) + 1, trunk = 4 + (int) Math.floorMod(h >>> 44, 3L);
                int ox = x - tx, oz = z - tz;
                if (ox == 0 && oz == 0) {
                    for (int y = base; y < base + trunk; y++) c[y] = of(birch ? Blocks.BIRCH_LOG : Blocks.OAK_LOG);
                    any = true;
                }
                BlockState leaves = of(birch ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES).setValue(LeavesBlock.PERSISTENT, true);
                for (int y = base + trunk - 2; y <= base + trunk + 1; y++) {
                    int r = y >= base + trunk + 1 ? 1 : 2;
                    if (Math.abs(ox) <= r && Math.abs(oz) <= r && !(Math.abs(ox) == r && Math.abs(oz) == r && unit(hash(seed, x, y, z, 9171)) < 0.6)
                            && c[y] == null) {
                        c[y] = leaves;
                        any = true;
                    }
                }
            }
        }
        return any;
    }

    /** The open-air galleries: a meadow floor, a white ceiling with lights, big window frames on one side and pillars on the other. */
    private void gallery(BlockState[] c, long seed, int x, int z, Gallery g) {
        int across = (g.eastWest ? z : x) - g.line, along = g.eastWest ? x : z;
        // the window side faces +across; pillars on -across
        if (Math.abs(across) == 6) {
            // the ceiling's lip, and a kerb along the floor's edge hiding the soil
            c[GALLERY] = concrete;
            c[GALLERY + 1] = concrete;
            c[GALLERY_TOP] = concrete;
            return;
        }
        c[GALLERY] = concrete;
        c[GALLERY + 1] = grass;
        c[GALLERY_TOP] = across == 0 && Math.floorMod(along, 8) == 4 ? lamp : concrete;
        int bay = Math.floorMod(along, 8);
        boolean end = along == g.to && g.door;
        if (end) {
            // a blank wall closing the far end, and a red door in it
            for (int y = GALLERY + 2; y < GALLERY_TOP; y++) c[y] = concrete;
            if (across == 0) {
                Direction facing = g.eastWest ? Direction.WEST : Direction.NORTH;
                BlockState door = ModBlocks.EXIT_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, facing);
                c[GALLERY + 2] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
                c[GALLERY + 3] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            }
            return;
        }
        if (across == 5) {
            // window frames: a slim post every bay, and a low sill
            if (bay == 0) for (int y = GALLERY + 2; y < GALLERY_TOP; y++) c[y] = pillar;
            else c[GALLERY + 2] = of(Blocks.SMOOTH_QUARTZ_SLAB);
            return;
        }
        if (across <= -4) {
            // thick pillars, vines down them, bushes along their foot
            boolean column = bay <= 1;
            if (across == -5 && column) {
                for (int y = GALLERY + 2; y < GALLERY_TOP; y++) c[y] = smooth;
                return;
            }
            if (across == -5 && (bay == 2 || bay == 7)) {
                int length = 2 + (int) Math.floorMod(hash(seed, x, z, 9180), 4L);
                BlockState vine = of(Blocks.VINE).setValue(bay == 2 ? VineBlock.WEST : VineBlock.EAST, true);
                if (!g.eastWest) vine = of(Blocks.VINE).setValue(bay == 2 ? VineBlock.NORTH : VineBlock.SOUTH, true);
                for (int i = 1; i <= length; i++) c[GALLERY_TOP - i] = vine;
            }
            double b = unit(hash(seed, x, z, 9181));
            if (b < 0.55) {
                c[GALLERY + 2] = b < 0.12 ? flowering : azalea;
                if (b < 0.2 && c[GALLERY + 3] == null) c[GALLERY + 3] = azalea;
            }
            return;
        }
        // the meadow: grass, petals and flowers thick on the floor
        double r = unit(hash(seed, x, z, 9182));
        if (r < 0.3) c[GALLERY + 2] = shortGrass;
        else if (r < 0.55) c[GALLERY + 2] = of(Blocks.PINK_PETALS).setValue(PinkPetalsBlock.AMOUNT, 1 + (int) (r * 100) % 4)
                .setValue(PinkPetalsBlock.FACING, Direction.from2DDataValue((int) (r * 1000) % 4));
        else if (r < 0.68) c[GALLERY + 2] = flowers[(int) (r * 1000) % flowers.length];
    }

    /** Level 385's kind of tower: bare floors every six blocks, no walls, a core with a ladder up it. */
    private void tower(BlockState[] c, long seed, int x, int z, Tower t) {
        int dx = x - t.x, dz = z - t.z;
        boolean core = Math.abs(dx) <= 1 && Math.abs(dz) <= 1, corner = Math.abs(dx) >= 5 && Math.abs(dz) >= 5, ladder = dx == 0 && dz == 2;
        for (int y = CLOUD - 6; y <= t.top; y++) {
            int level = Math.floorMod(y - (CLOUD + 2), 6);
            if (core || corner) c[y] = grey;
            else if (ladder) c[y] = of(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH);
            else if (level == 0 && y > CLOUD) {
                // floors, a few slabs fallen out of them
                boolean hole = unit(hash(seed, x, y, z, 9190)) < 0.06 && Math.abs(dx) < 5 && Math.abs(dz) < 5 && Math.abs(dx) + Math.abs(dz) > 3;
                c[y] = hole ? null : (Math.abs(dx) == 6 || Math.abs(dz) == 6) ? smooth : grey;
            }
        }
        if (core) c[t.top + 1] = null;
        if (dx == 0 && dz == -3) {
            // on the roof, a door with nowhere round it
            BlockState door = ModBlocks.EXIT_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
            c[t.top + 1] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            c[t.top + 2] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        }
    }

    // ------------------------------------------------------------------ travel

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        double r = unit(hash(seed, pos.getX(), pos.getZ(), 9200));
        return r < 0.4 ? null : r < 0.65 ? BackroomsLevel.ENDLESS_FIELD : r < 0.8 ? BackroomsLevel.STAIRCASE : BackroomsLevel.LEVEL_0;
    }

    /** You arrive on an island, on the grass. */
    @Override
    public Vec3 landing(long seed, RandomSource random) {
        for (int attempt = 0; attempt < 400; attempt++) {
            int x = random.nextIntBetweenInclusive(-3000, 3000), z = random.nextIntBetweenInclusive(-3000, 3000);
            if (!island(seed, x, z) || !island(seed, x + 6, z) || !island(seed, x - 6, z) || !island(seed, x, z + 6) || !island(seed, x, z - 6)) continue;
            BlockState[] c = column(seed, x, z);
            int top = islandTop(seed, x, z);
            if (LevelGenerator.clear(c[top + 1]) && LevelGenerator.clear(c[top + 2])) return new Vec3(x + 0.5, top + 1, z + 0.5);
        }
        return new Vec3(0.5, ISLAND_TOP + 8, 0.5);
    }
}
