package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.worldgen.site.SiteBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.noise;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * The Endless Field: grass, rolling on forever under a still morning sky. Long, smooth hills,
 * flat stretches between them, and nothing else, except, far apart, the odd thing someone put
 * there, like a child adding details to a drawing: a lone tree, a little pastel house with a path
 * that wanders off and stops, a door standing on its own with a chair beside it, a swing set, a
 * picnic table, a swimming pool in the lawn, a pond with a white shed, a black rectangle, a hole
 * with stairs going down, a water tower, power lines that run from nowhere to nowhere.
 * <p>
 * The things stand in 96-block squares, at most one each, flattening the ground under them.
 * Each is built once as a "stamp" (in all four turns) and read back column by column.
 */
public final class EndlessFieldLevel implements LevelGenerator {
    public static final int HEIGHT = 96, CELL = 96, BASE = 32;
    private static final int LINE_BAND = 400, POLE = 24;
    private static final ResourceKey<LootTable> HOUSE_LOOT = ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/field_house"));

    /** What can stand in a square: how common, how far round it the ground is levelled (and blended), and how far it's sunk. */
    public enum Feature {
        TREE(18, 1, 3, 0), HOUSE(14, 6, 8, 0), SUNKEN_HOUSE(4, 6, 16, -6), WHITE_BOX(8, 8, 8, 0), DOOR(6, 2, 4, 0),
        PLAYGROUND(6, 9, 6, 0), PICNIC(6, 2, 4, 0), POOL(6, 6, 6, 0), POND(7, 14, 6, 0), VOID_DOOR(3, 2, 4, 0),
        STAIRWELL(3, 11, 8, 0), WATER_TOWER(4, 4, 6, 0), LADDER(3, 1, 2, 0), BARN(5, 9, 8, 0);

        public final int weight, flat, blend, sink;

        Feature(int weight, int flat, int blend, int sink) {
            this.weight = weight;
            this.flat = flat;
            this.blend = blend;
            this.sink = sink;
        }
    }

    /** A thing in the field: what, where its middle is, the level it stands at, which way it's turned, which version. */
    public record Placed(Feature feature, int x, int z, int y0, int rotation, int variant) {
    }

    private static final int TOTAL;

    static {
        int total = 0;
        for (Feature f : Feature.values()) total += f.weight;
        TOTAL = total;
    }

    private final BlockState bedrock = of(Blocks.BEDROCK), stone = of(Blocks.STONE), dirt = of(Blocks.DIRT), grass = of(Blocks.GRASS_BLOCK);

    // ------------------------------------------------------------------ the land

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** The bare ground height: long smooth hills, with whole regions gone almost flat. */
    public static int ground(long seed, int x, int z) {
        double hilly = smooth(noise(seed, x, z, 520.0, 5000));
        double h = noise(seed, x, z, 170.0, 5001) * 16 + noise(seed, x, z, 64.0, 5002) * 7 + noise(seed, x, z, 28.0, 5003) * 1.5;
        return BASE + (int) Math.round(h * (0.2 + 0.8 * hilly) + 4 * (1 - hilly));
    }

    public static @Nullable Placed placed(long seed, int cx, int cz) {
        long h = hash(seed, cx, cz, 5010);
        if (unit(h) >= 0.45) return null;
        int roll = (int) Math.floorMod(Long.rotateLeft(h, 17), (long) TOTAL);
        Feature feature = Feature.TREE;
        for (Feature f : Feature.values()) {
            roll -= f.weight;
            if (roll < 0) {
                feature = f;
                break;
            }
        }
        int x = cx * CELL + 36 + (int) Math.floorMod(h >>> 20, 25L), z = cz * CELL + 36 + (int) Math.floorMod(h >>> 28, 25L);
        int rotation = (int) Math.floorMod(h >>> 36, 4L), variant = (int) Math.floorMod(h >>> 42, 4L);
        return new Placed(feature, x, z, ground(seed, x, z) + feature.sink, rotation, variant);
    }

    private static int flattened(int raw, Placed p, int x, int z) {
        double d = Math.hypot(x - p.x, z - p.z);
        if (d <= p.feature.flat) return p.y0;
        if (d >= p.feature.flat + p.feature.blend) return raw;
        return (int) Math.round(p.y0 + (raw - p.y0) * smooth((d - p.feature.flat) / p.feature.blend));
    }

    /** The ground height, levelled round whatever stands nearby. */
    public static int groundAt(long seed, int x, int z) {
        int raw = ground(seed, x, z);
        Placed p = placed(seed, Math.floorDiv(x, CELL), Math.floorDiv(z, CELL));
        return p == null ? raw : flattened(raw, p, x, z);
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public @Nullable ResourceKey<LootTable> containerLoot(long seed, BlockPos pos) {
        return HOUSE_LOOT;
    }

    /** A door standing in the grass goes home; the one at the bottom of the stairwell goes back down to Level 0. */
    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        if (pos.getY() < ground(seed, pos.getX(), pos.getZ()) - 8) return BackroomsLevel.LEVEL_0;
        // one lone door in four opens onto a road high above the clouds
        return unit(hash(seed, pos.getX(), pos.getZ(), 5300)) < 0.25 ? BackroomsLevel.DROP_LIMIT : null;
    }

    @Override
    public void blockEntity(WorldGenRegion region, long seed, BlockPos pos, BlockState state) {
        if (region.getBlockEntity(pos) instanceof SignBlockEntity sign) SiteBuilder.writeSign(sign, region.registryAccess(), "", "087", "", "");
    }

    /** Arrive standing in the open grass. */
    @Override
    public Vec3 landing(long seed, RandomSource random) {
        for (int attempt = 0; attempt < 200; attempt++) {
            int x = random.nextIntBetweenInclusive(-3000, 3000), z = random.nextIntBetweenInclusive(-3000, 3000);
            int g = groundAt(seed, x, z);
            BlockState[] c = column(seed, x, z);
            if (c[g] != null && c[g].is(Blocks.GRASS_BLOCK) && LevelGenerator.clear(c[g + 1]) && c[g + 2] == null) return new Vec3(x + 0.5, g + 1, z + 0.5);
        }
        return new Vec3(0.5, groundAt(seed, 0, 0) + 1, 0.5);
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[HEIGHT];
        Placed p = placed(seed, Math.floorDiv(x, CELL), Math.floorDiv(z, CELL));
        int raw = ground(seed, x, z), g = p == null ? raw : flattened(raw, p, x, z);
        c[0] = bedrock;
        LevelParts.fill(c, 1, g - 4, stone);
        LevelParts.fill(c, Math.max(1, g - 3), g - 1, dirt);
        c[g] = grass;
        Stamp stamp = p == null ? null : stamp(p);
        long key = p == null ? 0 : key(x - p.x, z - p.z);
        BlockState[] surface = stamp == null ? null : stamp.surface.get(key);
        Stamp.Col col = stamp == null ? null : stamp.cols.get(key);
        boolean near = p != null && Math.hypot(x - p.x, z - p.z) <= p.feature.flat + 1;
        if (surface != null) {
            c[g] = surface[0];
            if (surface[1] != null) c[g + 1] = surface[1];
        } else if (!near) {
            decorate(c, seed, x, z, g);
        }
        if (col != null) {
            for (int i = 0; i < col.states.length; i++) {
                int y = p.y0 + col.min + i;
                BlockState state = col.states[i];
                if (state == null || y < 1 || y >= HEIGHT - 1) continue;
                c[y] = state.isAir() ? null : state;
            }
        }
        powerLines(c, seed, x, z);
        return c;
    }

    /** A clean lawn, a blade of tall grass here and there, and meadows of dandelions. */
    private void decorate(BlockState[] c, long seed, int x, int z, int g) {
        long h = hash(seed, x, z, 5020);
        double u = unit(h);
        boolean meadow = noise(seed, x, z, 36.0, 5021) > 0.74;
        if (meadow && u < 0.09) c[g + 1] = of(Blocks.DANDELION);
        else if (meadow && u < 0.105) c[g + 1] = of(Blocks.OXEYE_DAISY);
        else if (u < 0.018) c[g + 1] = of(Blocks.SHORT_GRASS);
    }

    /** Lines of wooden poles marching across the field along x, wires sagging between them. */
    private static void powerLines(BlockState[] c, long seed, int x, int z) {
        int band = Math.floorDiv(z, LINE_BAND);
        if (unit(seed, band, 1, 5031) >= 0.3) return;
        int zl = band * LINE_BAND + 40 + (int) Math.floorMod(hash(seed, band, 0, 5030), 320L);
        int off = z - zl;
        if (Math.abs(off) > 1) return;
        int px = Math.floorDiv(x, POLE) * POLE;
        int topA = groundAt(seed, px, zl) + 10;
        if (x == px) {
            Placed q = placed(seed, Math.floorDiv(px, CELL), Math.floorDiv(zl, CELL));
            if (q != null && Math.hypot(px - q.x, zl - q.z) <= q.feature.flat + 3) return;
            if (off == 0) {
                for (int y = topA - 10 + 1; y <= topA + 1 && y < HEIGHT - 1; y++) c[y] = of(Blocks.SPRUCE_LOG);
            } else if (topA < HEIGHT - 1) {
                c[topA] = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
            }
            return;
        }
        if (off == 0) return;
        int topB = groundAt(seed, px + POLE, zl) + 10;
        double t = (x - px) / (double) POLE;
        int y = (int) Math.round(topA + (topB - topA) * t - 10.0 * t * (1 - t));
        if (y > 0 && y < HEIGHT - 1 && c[y] == null) c[y] = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.X);
    }

    // ------------------------------------------------------------------ stamps

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    static final class Stamp {
        record Col(int min, BlockState[] states) {
        }

        final Map<Long, Col> cols = new HashMap<>();
        final Map<Long, BlockState[]> surface = new HashMap<>();
    }

    /** Builds a stamp facing south (+z), then turns it. Air means "dig out". */
    static final class B {
        private static final BlockState AIR = Blocks.AIR.defaultBlockState();
        final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        final Map<BlockPos, BlockState[]> surface = new LinkedHashMap<>();

        void set(int x, int y, int z, BlockState state) {
            blocks.put(new BlockPos(x, y, z), state);
        }

        void set(int x, int y, int z, Block block) {
            set(x, y, z, block.defaultBlockState());
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
            for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) set(x, y, z, state);
        }

        void carve(int x0, int y0, int z0, int x1, int y1, int z1) {
            fill(x0, y0, z0, x1, y1, z1, AIR);
        }

        void surface(int x, int z, BlockState ground, @Nullable BlockState above) {
            surface.put(new BlockPos(x, 0, z), new BlockState[]{ground, above});
        }

        /** Joins fences and panes up to what's next to them. */
        void connect() {
            for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
                BlockState state = e.getValue();
                if (!(state.getBlock() instanceof CrossCollisionBlock)) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockState n = blocks.get(e.getKey().relative(d));
                    boolean join = n != null && !n.isAir() && (n.getBlock() instanceof CrossCollisionBlock || n.canOcclude());
                    state = state.setValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(d), join);
                }
                e.setValue(state);
            }
        }

        Stamp stamp(Rotation rotation) {
            Map<Long, TreeMap<Integer, BlockState>> columns = new HashMap<>();
            for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
                BlockPos q = e.getKey().rotate(rotation);
                columns.computeIfAbsent(key(q.getX(), q.getZ()), k -> new TreeMap<>()).put(q.getY(), e.getValue().rotate(rotation));
            }
            Stamp stamp = new Stamp();
            for (Map.Entry<Long, TreeMap<Integer, BlockState>> e : columns.entrySet()) {
                int min = e.getValue().firstKey(), max = e.getValue().lastKey();
                BlockState[] states = new BlockState[max - min + 1];
                e.getValue().forEach((y, s) -> states[y - min] = s);
                stamp.cols.put(e.getKey(), new Stamp.Col(min, states));
            }
            for (Map.Entry<BlockPos, BlockState[]> e : surface.entrySet()) {
                BlockPos q = e.getKey().rotate(rotation);
                BlockState[] s = e.getValue();
                stamp.surface.put(key(q.getX(), q.getZ()), new BlockState[]{s[0], s[1] == null ? null : s[1].rotate(rotation)});
            }
            return stamp;
        }
    }

    private static final Map<Long, Stamp> STAMPS = new ConcurrentHashMap<>();

    static Stamp stamp(Placed p) {
        long k = p.feature.ordinal() * 64L + p.variant * 4L + p.rotation;
        return STAMPS.computeIfAbsent(k, kk -> {
            B b = build(p.feature, p.variant);
            b.connect();
            return b.stamp(Rotation.values()[p.rotation]);
        });
    }

    // ------------------------------------------------------------------ the things in the field

    private static BlockState stairs(Block block, Direction facing) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, Half.BOTTOM);
    }

    private static BlockState facing(Block block, Direction facing) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static BlockState log(Block block, Direction.Axis axis) {
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState leaves(Block block) {
        return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static void door(B b, int x, int y, int z, Block door, Direction facing) {
        BlockState state = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        b.set(x, y, z, state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        b.set(x, y + 1, z, state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    static B build(Feature feature, int v) {
        B b = new B();
        switch (feature) {
            case TREE -> tree(b, 0, 0, 4 + v / 2, 3.2 + (v % 2) * 0.8);
            case HOUSE -> {
                switch (v) {
                    case 0 -> house(b, of(Blocks.WHITE_CONCRETE), Blocks.DEEPSLATE_TILE_STAIRS, of(Blocks.DEEPSLATE_TILES), Blocks.SPRUCE_DOOR, true);
                    case 1 -> house(b, of(Blocks.PINK_CONCRETE), Blocks.DARK_PRISMARINE_STAIRS, of(Blocks.DARK_PRISMARINE), Blocks.MANGROVE_DOOR, false);
                    case 2 -> house(b, of(Blocks.LIGHT_BLUE_CONCRETE), Blocks.SMOOTH_QUARTZ_STAIRS, of(Blocks.SMOOTH_QUARTZ), Blocks.BIRCH_DOOR, false);
                    default -> house(b, of(Blocks.YELLOW_CONCRETE), Blocks.SMOOTH_QUARTZ_STAIRS, of(Blocks.SMOOTH_QUARTZ), Blocks.BIRCH_DOOR, true);
                }
                furnish(b);
                windingPath(b, 26, v * 1.3);
            }
            case SUNKEN_HOUSE -> {
                house(b, of(Blocks.PINK_CONCRETE), Blocks.DARK_PRISMARINE_STAIRS, of(Blocks.DARK_PRISMARINE), Blocks.MANGROVE_DOOR, true);
                furnish(b);
                for (int x : new int[]{-3, -2, 2, 3}) b.set(x, 1, 4, Blocks.FLOWERING_AZALEA);
                for (int z = 4; z <= 28; z++) b.surface(0, z, of(Blocks.LIGHT_GRAY_CONCRETE), null);     // a straight path up out of the hollow
            }
            case WHITE_BOX -> whiteBox(b);
            case DOOR -> {
                door(b, 0, 1, 0, ModBlocks.LONE_DOOR.get(), Direction.SOUTH);
                if (v != 3) b.set(-2, 1, 1, facing(ModBlocks.STORE_CHAIR.get(), Direction.SOUTH));
            }
            case PLAYGROUND -> playground(b);
            case PICNIC -> {
                b.set(0, 1, 0, ModBlocks.DINING_TABLE.get());
                b.set(0, 2, 0, Blocks.PINK_CARPET);
                b.set(-1, 1, 0, facing(ModBlocks.STORE_CHAIR.get(), Direction.EAST));
                b.set(1, 1, 0, facing(ModBlocks.STORE_CHAIR.get(), Direction.WEST));
                if (v % 2 == 0) b.set(0, 3, 0, Blocks.DECORATED_POT);
            }
            case POOL -> pool(b);
            case POND -> pond(b, v);
            case VOID_DOOR -> b.fill(-1, 1, 0, 1, 5, 0, of(ModBlocks.NOCLIP_VOID.get()));
            case STAIRWELL -> stairwell(b);
            case WATER_TOWER -> waterTower(b);
            case LADDER -> {
                for (int y = 1; y <= 10 + v * 2; y++) b.set(0, y, 0, facing(Blocks.LADDER, Direction.SOUTH));
            }
            case BARN -> barn(b);
        }
        return b;
    }

    /** A round tree, alone. */
    private static void tree(B b, int x, int z, int trunk, double r) {
        int cy = trunk + 2;
        int ri = (int) Math.ceil(r);
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -ri; dy <= ri; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (dx * dx + dy * dy * 1.25 + dz * dz <= r * r) b.set(x + dx, cy + dy, z + dz, leaves(Blocks.OAK_LEAVES));
                }
            }
        }
        for (int y = 1; y <= trunk + 1; y++) b.set(x, y, z, log(Blocks.OAK_LOG, Direction.Axis.Y));
    }

    /** A bare tree: a trunk and a few crooked branches, no leaves. */
    private static void bareTree(B b, int x, int z) {
        for (int y = 1; y <= 6; y++) b.set(x, y, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        b.set(x + 1, 4, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
        b.set(x + 2, 5, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
        b.set(x - 1, 5, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
        b.set(x - 2, 6, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
        b.set(x, 5, z + 1, log(Blocks.DARK_OAK_LOG, Direction.Axis.Z));
        b.set(x, 6, z + 2, log(Blocks.DARK_OAK_LOG, Direction.Axis.Z));
        b.set(x, 6, z - 1, log(Blocks.DARK_OAK_LOG, Direction.Axis.Z));
        for (int[] t : new int[][]{{0, 7, 0}, {2, 6, 0}, {-2, 7, 0}, {0, 7, 2}, {0, 7, -1}}) b.set(x + t[0], t[1], z + t[2], Blocks.DARK_OAK_FENCE);
    }

    /** A little gabled house, 7x7, its door on the south side. */
    private static void house(B b, BlockState wall, Block roof, BlockState ridge, Block doorBlock, boolean chimney) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) b.set(dx, 0, dz, Blocks.BIRCH_PLANKS);
        for (int dy = 1; dy <= 8; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int ax = Math.abs(dx), az = Math.abs(dz);
                    if (dy > 4 && az >= 9 - dy) continue;                    // above the eaves: only what's under the roof
                    boolean edge = ax == 3 || az == 3;
                    b.set(dx, dy, dz, edge ? wall : B.AIR);
                }
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = 5; dy <= 8; dy++) {
                b.set(dx, dy, 9 - dy, stairs(roof, Direction.NORTH));
                b.set(dx, dy, dy - 9, stairs(roof, Direction.SOUTH));
            }
            b.set(dx, 9, 0, ridge);
        }
        door(b, 0, 1, 3, doorBlock, Direction.SOUTH);
        for (int[] w : new int[][]{{-2, 2, 3}, {2, 2, 3}, {-3, 2, 0}, {3, 2, 0}, {-1, 2, -3}, {1, 2, -3}, {-3, 6, 0}, {3, 6, 0}}) {
            b.set(w[0], w[1], w[2], Blocks.GLASS_PANE);
        }
        if (chimney) for (int dy = 6; dy <= 10; dy++) b.set(2, dy, -1, Blocks.BRICKS);
    }

    /** Inside: a chest, and a chair facing the wall. Nothing else. */
    private static void furnish(B b) {
        b.set(-2, 1, -2, facing(Blocks.CHEST, Direction.SOUTH).setValue(ChestBlock.FACING, Direction.SOUTH));
        b.set(2, 1, -2, facing(ModBlocks.STORE_CHAIR.get(), Direction.NORTH));
    }

    /** A dirt path from the front door, wandering off and then just stopping. */
    private static void windingPath(B b, int length, double phase) {
        int last = 0;
        for (int z = 4; z <= 4 + length; z++) {
            int x = (int) Math.round(Math.sin(z * 0.17 + phase) * 4 * Math.min(1.0, (z - 4) / 10.0));
            for (int xi = Math.min(last, x); xi <= Math.max(last, x); xi++) b.surface(xi, z, of(Blocks.DIRT_PATH), null);
            last = x;
        }
    }

    /** A plain white box with a dark doorway and a row of tiny windows. Dark inside. */
    private static void whiteBox(B b) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = 0; dy <= 8; dy++) {
                    boolean edge = Math.abs(dx) == 6 || Math.abs(dz) == 4 || dy == 0 || dy == 8;
                    b.set(dx, dy, dz, edge ? (dy == 0 ? of(Blocks.SMOOTH_STONE) : of(Blocks.WHITE_CONCRETE)) : B.AIR);
                }
            }
        }
        b.carve(0, 1, 4, 0, 2, 4);
        for (int dx : new int[]{2, 3, 4, 5}) b.set(dx, 6, 4, Blocks.BLACK_STAINED_GLASS);
        b.set(-6, 6, -1, Blocks.BLACK_STAINED_GLASS);
        b.set(-6, 6, 1, Blocks.BLACK_STAINED_GLASS);
        b.set(-5, 1, -3, facing(Blocks.CHEST, Direction.EAST).setValue(ChestBlock.FACING, Direction.EAST));
        b.set(0, 1, -3, facing(ModBlocks.STORE_CHAIR.get(), Direction.NORTH));
    }

    /** A swing set and a slide. */
    private static void playground(B b) {
        for (int y = 1; y <= 3; y++) {
            b.set(-3, y, 0, Blocks.WARPED_FENCE);
            b.set(3, y, 0, Blocks.WARPED_FENCE);
        }
        for (int x = -3; x <= 3; x++) b.set(x, 4, 0, log(Blocks.STRIPPED_WARPED_STEM, Direction.Axis.X));
        for (int x : new int[]{-1, 1}) {
            b.set(x, 3, 0, Blocks.CHAIN);
            b.set(x, 2, 0, Blocks.CHAIN);
            b.set(x, 1, 0, Blocks.WARPED_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP));
        }
        b.set(7, 1, 0, log(Blocks.STRIPPED_WARPED_STEM, Direction.Axis.Y));
        b.set(7, 2, 0, log(Blocks.STRIPPED_WARPED_STEM, Direction.Axis.Y));
        b.set(8, 1, 0, facing(Blocks.LADDER, Direction.EAST));
        b.set(8, 2, 0, facing(Blocks.LADDER, Direction.EAST));
        b.set(7, 3, 1, Blocks.IRON_BARS);
        b.set(7, 3, -1, Blocks.IRON_BARS);
        b.set(6, 2, 0, stairs(ModBlocks.SLIDE.get(), Direction.EAST));
        b.set(5, 1, 0, stairs(ModBlocks.SLIDE.get(), Direction.EAST));
    }

    /** A swimming pool set into the lawn. */
    private static void pool(B b) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean rim = Math.abs(dx) == 5 || Math.abs(dz) == 3;
                if (rim) {
                    b.fill(dx, -3, dz, dx, 0, dz, of(ModBlocks.POOL_TILE.get()));
                } else {
                    b.set(dx, -3, dz, ModBlocks.POOL_FLOOR_TILE.get());
                    b.fill(dx, -2, dz, dx, 0, dz, of(Blocks.WATER));
                }
            }
        }
        for (int y = -2; y <= 0; y++) b.set(4, y, 0, facing(Blocks.LADDER, Direction.WEST).setValue(LadderBlock.WATERLOGGED, true));
    }

    /** A pond, and a white shed beside it. */
    private static void pond(B b, int v) {
        double phase = v * 1.7;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                double a = Math.atan2(dz, dx), d = Math.hypot(dx, dz);
                double r = 5.5 + 1.4 * Math.sin(3 * a + phase) + 0.8 * Math.sin(5 * a + phase * 2);
                if (d < r) {
                    b.set(dx, 0, dz, Blocks.WATER);
                    b.set(dx, -1, dz, d < r - 1.5 ? Blocks.WATER : Blocks.DIRT);
                    b.set(dx, -2, dz, Blocks.CLAY);
                    if (d < r - 2 && Math.floorMod(dx * 7 + dz * 13 + v, 17) == 0) b.set(dx, 1, dz, Blocks.LILY_PAD);
                } else if (d < r + 1 && Math.floorMod(dx * 5 + dz * 11 + v, 9) == 0) {
                    b.set(dx, 1, dz, Blocks.SUGAR_CANE);
                    b.set(dx, 2, dz, Blocks.SUGAR_CANE);
                }
            }
        }
        for (int x = 9; x <= 13; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean edge = x == 9 || x == 13 || Math.abs(z) == 2;
                b.set(x, 0, z, Blocks.SMOOTH_STONE);
                for (int y = 1; y <= 3; y++) b.set(x, y, z, edge ? of(Blocks.WHITE_CONCRETE) : B.AIR);
                b.set(x, 4, z, Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            }
        }
        b.carve(11, 1, 2, 11, 2, 2);
    }

    /**
     * A rectangular hole in the grass, white inside, with "087" on the lip, and stairs going down:
     * a flight, a landing, a flight back, and at the bottom, in the dark, a red door.
     */
    private static void stairwell(B b) {
        b.fill(-2, -26, -8, 5, -1, 9, of(Blocks.SMOOTH_QUARTZ));
        for (int i = 0; i < 12; i++) {
            int z = 6 - i, y = -1 - i;
            for (int x = -1; x <= 0; x++) {
                b.set(x, y, z, stairs(Blocks.QUARTZ_STAIRS, Direction.SOUTH));
                b.carve(x, y + 1, z, x, i < 5 ? 0 : y + 4, z);
            }
        }
        b.carve(-1, -11, -7, 4, -8, -6);                                        // the landing
        for (int j = 0; j < 12; j++) {
            int z = -5 + j, y = -13 - j;
            for (int x = 3; x <= 4; x++) {
                b.set(x, y, z, stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH));
                b.carve(x, y + 1, z, x, y + 4, z);
            }
        }
        b.carve(3, -23, 7, 4, -20, 8);
        b.set(4, -23, 8, Blocks.SMOOTH_QUARTZ);
        b.set(4, -22, 8, Blocks.SMOOTH_QUARTZ);
        door(b, 3, -23, 8, ModBlocks.EXIT_DOOR.get(), Direction.NORTH);
        // just enough light to see by, on the landing and at the bottom
        b.set(1, -9, -6, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 6));
        b.set(4, -20, 7, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 5));
        // a white lip round the opening, and the number
        for (int z = 1; z <= 7; z++) {
            b.set(-2, 0, z, Blocks.SMOOTH_QUARTZ);
            b.set(1, 0, z, Blocks.SMOOTH_QUARTZ);
        }
        for (int x = -2; x <= 1; x++) {
            b.set(x, 0, 1, Blocks.SMOOTH_QUARTZ);
            b.set(x, 0, 7, Blocks.SMOOTH_QUARTZ);
        }
        b.set(-1, 0, 1, B.AIR);
        b.set(0, 0, 1, B.AIR);
        b.set(0, -1, 7, Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH));
    }

    private static void waterTower(B b) {
        for (int sx : new int[]{-2, 2}) for (int sz : new int[]{-2, 2}) for (int y = 1; y <= 9; y++) b.set(sx, y, sz, Blocks.DARK_OAK_FENCE);
        for (int i = -1; i <= 1; i++) {
            b.set(i, 5, -2, Blocks.DARK_OAK_FENCE);
            b.set(i, 5, 2, Blocks.DARK_OAK_FENCE);
            b.set(-2, 5, i, Blocks.DARK_OAK_FENCE);
            b.set(2, 5, i, Blocks.DARK_OAK_FENCE);
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int r2 = dx * dx + dz * dz;
                if (r2 <= 10) b.fill(dx, 10, dz, dx, 14, dz, of(Blocks.WHITE_CONCRETE));
                if (r2 <= 5) b.set(dx, 15, dz, Blocks.WHITE_CONCRETE);
                if (r2 <= 1) b.set(dx, 16, dz, Blocks.WHITE_CONCRETE);
            }
        }
        b.set(0, 17, 0, Blocks.LIGHTNING_ROD);
    }

    /** An old barn with a rust-red roof, two bare trees, and a field of wheat round it. */
    private static void barn(B b) {
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                if (Math.hypot(dx, dz) > 12 || (Math.abs(dx) <= 4 && Math.abs(dz) <= 4) || (Math.abs(dx) <= 1 && dz > 4 && dz <= 7)) continue;
                b.surface(dx, dz, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7),
                        Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
            }
        }
        house(b, of(Blocks.WHITE_TERRACOTTA), Blocks.BRICK_STAIRS, of(Blocks.BRICKS), Blocks.SPRUCE_DOOR, false);
        b.set(-2, 1, -2, Blocks.HAY_BLOCK);
        b.set(-2, 2, -2, Blocks.HAY_BLOCK);
        b.set(-1, 1, -2, Blocks.HAY_BLOCK);
        b.set(2, 1, -2, facing(Blocks.CHEST, Direction.SOUTH).setValue(ChestBlock.FACING, Direction.SOUTH));
        for (int z = 5; z <= 7; z++) for (int x = -1; x <= 1; x++) b.surface(x, z, of(Blocks.DIRT_PATH), null);
        bareTree(b, -7, 1);
        bareTree(b, 7, -1);
    }
}
