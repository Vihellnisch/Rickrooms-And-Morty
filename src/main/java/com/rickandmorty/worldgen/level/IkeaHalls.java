package com.rickandmorty.worldgen.level;

import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.worldgen.level.InfiniteIkeaLevel.Department;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.noise;
import static com.rickandmorty.worldgen.level.GenMath.pick;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * The Infinite IKEA's big themed halls: each takes up four departments (the walkways between
 * them are gone), 59 blocks square. A backyard with a patio, a shed, an orchard and a vegetable
 * garden; an indoor forest round a pond, with boulders worth mining; and a winter wonderland with
 * an ice rink and a Christmas market. Coordinates here are inside the hall, 0 to 58.
 */
final class IkeaHalls {
    static final int SIZE = 2 * InfiniteIkeaLevel.CELL - InfiniteIkeaLevel.WALK;
    private static final int F = InfiniteIkeaLevel.FLOOR;

    private final BlockState grass = of(Blocks.GRASS_BLOCK), dirt = of(Blocks.DIRT), stone = of(Blocks.STONE), water = of(Blocks.WATER);
    private final BlockState sand = of(Blocks.SAND), crate = of(ModBlocks.STOCK_CRATE.get()), lantern = of(Blocks.LANTERN);
    private final BlockState chair = of(ModBlocks.STORE_CHAIR.get()), table = of(ModBlocks.DINING_TABLE.get());
    private final BlockState snowBlock = of(Blocks.SNOW_BLOCK), snow = of(Blocks.SNOW);
    private final BlockState fence = of(Blocks.BIRCH_FENCE), spruceFence = of(Blocks.SPRUCE_FENCE);
    private static final Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY,
            Blocks.CORNFLOWER, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.LILY_OF_THE_VALLEY};
    private static final Block[] GIFTS = {Blocks.RED_WOOL, Blocks.GREEN_WOOL, Blocks.BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.WHITE_WOOL};

    /** A tree: where (hall coordinates), how tall, and what kind (0 oak, 1 birch, 2 spruce, 3 cherry). */
    private record Tree(int x, int z, int height, int kind) {
    }

    void column(BlockState[] c, long seed, Department theme, int sx, int sz, int hx, int hz, int x, int z) {
        switch (theme) {
            case BACKYARD -> backyard(c, seed, sx, sz, hx, hz, x, z);
            case NATURE -> nature(c, seed, sx, sz, hx, hz, x, z);
            default -> seasonal(c, seed, sx, sz, hx, hz, x, z);
        }
    }

    // ------------------------------------------------------------------ trees

    private static BlockState leaves(Block block) {
        return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static Block log(int kind) {
        return switch (kind) {
            case 1 -> Blocks.BIRCH_LOG;
            case 2 -> Blocks.SPRUCE_LOG;
            case 3 -> Blocks.CHERRY_LOG;
            default -> Blocks.OAK_LOG;
        };
    }

    private static Block leafBlock(int kind) {
        return switch (kind) {
            case 1 -> Blocks.BIRCH_LEAVES;
            case 2 -> Blocks.SPRUCE_LEAVES;
            case 3 -> Blocks.CHERRY_LEAVES;
            default -> Blocks.OAK_LEAVES;
        };
    }

    /** The tree in grid square (gx, gz), if it has one. */
    private static @Nullable Tree tree(long seed, int sx, int sz, int gx, int gz, int spacing, int salt, int[] kinds) {
        long h = hash(seed ^ ((long) sx * 341873128712L + sz), gx, gz, salt);
        if (unit(h) < 0.2) return null;
        int x = gx * spacing + 2 + (int) Math.floorMod(h >>> 8, (long) Math.max(1, spacing - 4));
        int z = gz * spacing + 2 + (int) Math.floorMod(h >>> 16, (long) Math.max(1, spacing - 4));
        int kind = kinds[(int) Math.floorMod(h >>> 24, (long) kinds.length)];
        int height = (kind == 2 ? 7 : 4) + (int) Math.floorMod(h >>> 32, 3L);
        return new Tree(x, z, height, kind);
    }

    /** Draws any tree trunk or leaves at (hx, hz). Returns true if there's a trunk here. */
    private static boolean trees(BlockState[] c, long seed, int sx, int sz, int hx, int hz, int spacing, int salt, int[] kinds,
                                 java.util.function.Predicate<Tree> allowed, @Nullable BlockState decoration) {
        int gx0 = Math.floorDiv(hx, spacing), gz0 = Math.floorDiv(hz, spacing);
        boolean trunk = false;
        for (int gx = gx0 - 1; gx <= gx0 + 1; gx++) {
            for (int gz = gz0 - 1; gz <= gz0 + 1; gz++) {
                Tree t = tree(seed, sx, sz, gx, gz, spacing, salt, kinds);
                if (t == null || t.x < 3 || t.z < 3 || t.x > SIZE - 4 || t.z > SIZE - 4 || !allowed.test(t)) continue;
                int dx = Math.abs(hx - t.x), dz = Math.abs(hz - t.z);
                int top = F + t.height;
                if (dx == 0 && dz == 0) {
                    for (int y = F + 1; y <= top; y++) c[y] = of(log(t.kind));
                    trunk = true;
                }
                BlockState leaf = leaves(leafBlock(t.kind));
                for (int y = F + 2; y <= top + 1; y++) {
                    int r;
                    if (t.kind == 2) {                                         // spruce: a cone
                        int k = top + 1 - y;
                        r = k == 0 ? 0 : k <= 2 ? 1 : k % 2 == 0 ? 2 : 1;
                        if (y < F + 3) r = -1;
                    } else {
                        r = y >= top ? 1 : y >= top - 2 ? 2 : -1;
                        if (y == top + 1 && dx + dz > 1) r = -1;
                    }
                    if (r < 0 || dx > r || dz > r || (r == 2 && dx == 2 && dz == 2)) continue;
                    if (c[y] == null) {
                        long lh = hash(seed, hx, hz, y, salt + 1);
                        c[y] = decoration != null && unit(lh) < 0.08 ? decoration : leaf;
                    }
                }
                if (decoration != null && dx == 0 && dz == 0) c[top + 2] = of(Blocks.LANTERN);     // a light on top
            }
        }
        return trunk;
    }

    // ------------------------------------------------------------------ the backyard

    private static boolean fenceAt(int hx, int hz) {
        if (hx < 1 || hz < 1 || hx > SIZE - 2 || hz > SIZE - 2) return false;
        boolean ring = hx == 1 || hz == 1 || hx == SIZE - 2 || hz == SIZE - 2;
        boolean gate = Math.abs(hx - SIZE / 2) <= 1 || Math.abs(hz - SIZE / 2) <= 1;
        return ring && !gate;
    }

    private void backyard(BlockState[] c, long seed, int sx, int sz, int hx, int hz, int x, int z) {
        c[1] = dirt;
        c[2] = dirt;
        c[F] = grass;
        int mid = SIZE / 2;
        long h = hash(seed, x, z, 3060);
        if (fenceAt(hx, hz)) {
            c[F + 1] = fence.setValue(FenceBlock.NORTH, fenceAt(hx, hz - 1)).setValue(FenceBlock.SOUTH, fenceAt(hx, hz + 1))
                    .setValue(FenceBlock.EAST, fenceAt(hx + 1, hz)).setValue(FenceBlock.WEST, fenceAt(hx - 1, hz));
            if ((hx + hz) % 8 == 0) c[F + 2] = lantern;
            return;
        }
        if (Math.abs(hx - mid) <= 1 || Math.abs(hz - mid) <= 1) {
            c[F] = of(Blocks.DIRT_PATH);                                        // the garden paths
            return;
        }
        if (hx < mid && hz < mid) patio(c, hx, hz, h);
        else if (hx > mid && hz < mid) orchard(c, seed, sx, sz, hx, hz, h);
        else if (hx < mid) shed(c, hx, hz, h);
        else vegetables(c, hx, hz);
    }

    /** Stone patio with umbrella tables and a barbecue. */
    private void patio(BlockState[] c, int hx, int hz, long h) {
        if (hx < 4 || hz < 4 || hx > 24 || hz > 24) {
            if (unit(h) < 0.12) c[F + 1] = of(FLOWERS[(int) Math.floorMod(h >>> 8, (long) FLOWERS.length)]);
            return;
        }
        c[F] = of(Blocks.STONE_BRICKS);
        for (int[] t : new int[][]{{9, 9}, {9, 19}, {19, 9}, {19, 19}}) {
            int dx = hx - t[0], dz = hz - t[1];
            if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
                c[F + 4] = Math.floorMod(dx + dz, 2) == 0 ? of(Blocks.WHITE_WOOL) : of(t[0] == t[1] ? Blocks.RED_WOOL : Blocks.BLUE_WOOL);
            }
            if (dx == 0 && dz == 0) {
                c[F + 1] = table;
                c[F + 2] = fence;
                c[F + 3] = fence;
            } else if (Math.abs(dx) + Math.abs(dz) == 1) {
                Direction toTable = dx == 1 ? Direction.WEST : dx == -1 ? Direction.EAST : dz == 1 ? Direction.NORTH : Direction.SOUTH;
                c[F + 1] = chair.setValue(HorizontalDirectionalBlock.FACING, toTable);
            }
        }
        if (hz == 5 && hx == 23) c[F + 1] = of(Blocks.CAMPFIRE);                // the barbecue
        if (hz == 5 && hx == 22) c[F + 1] = of(Blocks.SMOKER).setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
        if (hz == 5 && hx == 21) c[F + 1] = crate;
    }

    private void orchard(BlockState[] c, long seed, int sx, int sz, int hx, int hz, long h) {
        boolean trunk = trees(c, seed, sx, sz, hx, hz, 7, 3061, new int[]{0, 3, 0, 1}, t -> t.x > SIZE / 2 + 2 && t.z < SIZE / 2 - 2, null);
        if (!trunk && c[F + 1] == null && unit(h) < 0.1) c[F + 1] = of(FLOWERS[(int) Math.floorMod(h >>> 8, (long) FLOWERS.length)]);
    }

    /** A garden shed with the tools in it, and a hedge and a little pond. */
    private void shed(BlockState[] c, int hx, int hz, long h) {
        int x0 = 6, x1 = 14, z0 = 36, z1 = 42;
        if (hx >= x0 && hx <= x1 && hz >= z0 && hz <= z1) {
            c[F] = of(Blocks.SPRUCE_PLANKS);
            c[F + 4] = of(Blocks.SPRUCE_SLAB);
            boolean wall = hx == x0 || hx == x1 || hz == z0 || hz == z1;
            if (wall) {
                boolean door = hz == z0 && hx == 10;
                if (!door) LevelParts.fill(c, F + 1, F + 3, of(Blocks.SPRUCE_PLANKS));
                else c[F + 3] = of(Blocks.SPRUCE_PLANKS);
                if (hz == z1 && hx == 10) c[F + 2] = of(Blocks.GLASS_PANE);
            } else if (hz == z1 - 1 && (hx == 8 || hx == 12)) {
                c[F + 1] = crate;                                            // the garden supplies
            } else if (hz == z1 - 1 && hx == 7) {
                c[F + 1] = of(Blocks.CRAFTING_TABLE);
            } else if (hz == z1 - 1 && hx == 13) {
                c[F + 1] = of(Blocks.COMPOSTER);
            } else if (hz == z0 + 1 && hx == 13) {
                c[F + 1] = of(Blocks.BARREL);
            }
            return;
        }
        if ((hz == 48 || hz == 52) && hx >= 4 && hx <= 25 && hx % 7 != 3) {
            c[F + 1] = leaves(Blocks.OAK_LEAVES);                              // hedges
            c[F + 2] = leaves(Blocks.OAK_LEAVES);
            return;
        }
        int dx = hx - 21, dz = hz - 40;
        if (dx * dx + dz * dz <= 5) {
            c[F] = water;
            c[F - 1] = water;
            if (unit(h) < 0.25) c[F + 1] = of(Blocks.LILY_PAD);
            return;
        }
        if (unit(h) < 0.08) c[F + 1] = of(FLOWERS[(int) Math.floorMod(h >>> 8, (long) FLOWERS.length)]);
    }

    /** Rows of ripe crops with water between them, ready to pick and plant again. */
    private void vegetables(BlockState[] c, int hx, int hz) {
        int x0 = SIZE / 2 + 3, z0 = SIZE / 2 + 3;
        if (hx < x0 || hz < z0 || hx > SIZE - 4 || hz > SIZE - 4) {
            if (hx == SIZE - 3 && hz % 3 == 0) c[F + 1] = of(Blocks.HAY_BLOCK);
            return;
        }
        if ((hx - x0) % 5 == 2) {
            c[F] = water;
            return;
        }
        c[F] = of(Blocks.FARMLAND).setValue(FarmBlock.MOISTURE, 7);
        int row = (hz - z0) / 6;
        c[F + 1] = switch (row % 4) {
            case 0 -> Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
            case 1 -> Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, 7);
            case 2 -> Blocks.POTATOES.defaultBlockState().setValue(CropBlock.AGE, 7);
            default -> Blocks.BEETROOTS.defaultBlockState().setValue(BlockStateProperties.AGE_3, 3);
        };
    }

    // ------------------------------------------------------------------ the indoor forest

    private void nature(BlockState[] c, long seed, int sx, int sz, int hx, int hz, int x, int z) {
        c[1] = stone;
        c[2] = dirt;
        double n = noise(seed, x, z, 9.0, 3070);
        c[F] = n > 0.74 ? of(Blocks.PODZOL) : n > 0.64 ? of(Blocks.MOSS_BLOCK) : n < 0.14 ? of(Blocks.COARSE_DIRT) : grass;
        long h = hash(seed, x, z, 3071);
        // the pond
        double px = SIZE / 2.0 + pick(seed, sx, sz, 3072, 9) - 4, pz = SIZE / 2.0 + pick(seed, sx, sz, 3073, 9) - 4;
        double d = Math.hypot(hx - px, hz - pz), r = 7 + noise(seed, x, z, 6.0, 3074) * 3;
        if (d < r - 1.5) {
            c[F] = water;
            c[F - 1] = water;
            c[1] = of(Blocks.CLAY);
            if (unit(h) < 0.07) c[F + 1] = of(Blocks.LILY_PAD);
            return;
        }
        if (d < r + 0.5) {
            c[F] = sand;
            if (d < r - 0.6 && unit(h) < 0.3) {
                c[F + 1] = of(Blocks.SUGAR_CANE);
                c[F + 2] = of(Blocks.SUGAR_CANE);
            }
            return;
        }
        // boulders, with ore in them
        for (int i = 0; i < 4; i++) {
            int bx = 5 + pick(seed, sx, sz, 3080 + i, SIZE - 10), bz = 5 + pick(seed, sx, sz, 3090 + i, SIZE - 10);
            if (Math.hypot(bx - px, bz - pz) < 14) continue;
            double bd = Math.hypot(hx - bx, hz - bz);
            if (bd <= 2.4) {
                c[F + 1] = rock(hash(seed, x, z, 1, 3081));
                if (bd <= 1.6) c[F + 2] = rock(hash(seed, x, z, 2, 3081));
                if (bd <= 0.8) c[F + 3] = of(Blocks.MOSSY_COBBLESTONE);
                return;
            }
        }
        boolean trunk = trees(c, seed, sx, sz, hx, hz, 8, 3075, new int[]{0, 0, 1, 1, 2},
                t -> Math.hypot(t.x - px, t.z - pz) > 13, null);
        if (trunk || c[F + 1] != null) return;
        double u = unit(Long.rotateLeft(h, 7));
        if (u < 0.18) c[F + 1] = of(Blocks.SHORT_GRASS);
        else if (u < 0.24) c[F + 1] = of(Blocks.FERN);
        else if (u < 0.29) c[F + 1] = of(FLOWERS[(int) Math.floorMod(h >>> 8, (long) FLOWERS.length)]);
        else if (u < 0.31) c[F + 1] = Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3);
        else if (u < 0.32) c[F + 1] = of(Blocks.BROWN_MUSHROOM);
        else if (u < 0.325) c[F + 1] = of(Blocks.PUMPKIN);
    }

    /** Stone and cobble, with coal, iron, copper, and once in a while something better. */
    private BlockState rock(long h) {
        double u = unit(h);
        if (u < 0.08) return of(Blocks.COAL_ORE);
        if (u < 0.13) return of(Blocks.IRON_ORE);
        if (u < 0.17) return of(Blocks.COPPER_ORE);
        if (u < 0.185) return of(Blocks.LAPIS_ORE);
        if (u < 0.195) return of(Blocks.REDSTONE_ORE);
        if (u < 0.202) return of(Blocks.GOLD_ORE);
        if (u < 0.205) return of(Blocks.DIAMOND_ORE);
        if (u < 0.45) return of(Blocks.COBBLESTONE);
        if (u < 0.6) return of(Blocks.MOSSY_COBBLESTONE);
        if (u < 0.7) return of(Blocks.ANDESITE);
        return stone;
    }

    // ------------------------------------------------------------------ the winter wonderland

    private void seasonal(BlockState[] c, long seed, int sx, int sz, int hx, int hz, int x, int z) {
        c[1] = stone;
        c[2] = dirt;
        c[F] = snowBlock;
        long h = hash(seed, x, z, 3100);
        int mid = SIZE / 2;
        double d = Math.hypot(hx - mid, hz - mid);
        // the ice rink, with a snow wall round it and four ways in
        if (d < 9) {
            c[F] = of(Blocks.PACKED_ICE);
            return;
        }
        if (d < 10) {
            if (Math.abs(hx - mid) > 1 && Math.abs(hz - mid) > 1) c[F + 1] = snowBlock;
            return;
        }
        // the Christmas market: stalls along the far side
        if (hz >= 50 && hz <= 53 && hx >= 3 && hx <= SIZE - 4) {
            int stall = Math.floorMod(hx - 3, 7);
            if (stall < 5) {
                c[F + 4] = Math.floorMod(hx, 2) == 0 ? of(Blocks.RED_WOOL) : of(Blocks.WHITE_WOOL);   // the awning
                if (hz == 52) c[F + 1] = stall == 2 ? crate : of(Blocks.SPRUCE_PLANKS);
                if (hz == 52 && stall != 2) c[F + 2] = stall % 2 == 0 ? lantern : of(Blocks.CANDLE);
                if (hz == 53 && (stall == 0 || stall == 4)) LevelParts.fill(c, F + 1, F + 3, spruceFence);
                return;
            }
        }
        // snowmen
        for (int i = 0; i < 4; i++) {
            int mx = 4 + pick(seed, sx, sz, 3110 + i, SIZE - 8), mz = 4 + pick(seed, sx, sz, 3120 + i, 40);
            if (hx == mx && hz == mz && Math.hypot(mx - mid, mz - mid) > 12) {
                c[F + 1] = snowBlock;
                c[F + 2] = snowBlock;
                c[F + 3] = Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, Direction.from2DDataValue(i));
                return;
            }
        }
        // lamp posts
        if (hx % 10 == 5 && hz % 10 == 5 && d > 12) {
            c[F + 1] = spruceFence;
            c[F + 2] = spruceFence;
            c[F + 3] = lantern;
            return;
        }
        boolean trunk = trees(c, seed, sx, sz, hx, hz, 9, 3101, new int[]{2},
                t -> Math.hypot(t.x - mid, t.z - mid) > 13 && t.z < 46, of(Blocks.GLOWSTONE));
        if (trunk) return;
        if (c[F + 1] == null) {
            // presents under the trees, and snow on everything else
            c[F + 1] = unit(h) < 0.015 ? of(GIFTS[(int) Math.floorMod(h >>> 8, (long) GIFTS.length)]) : snow.setValue(SnowLayerBlock.LAYERS, 1);
        }
    }

    // ------------------------------------------------------------------ animals for the backyard and the forest

    void populate(WorldGenRegion region, Department theme, int sx, int sz) {
        int ox = sx * 2 * InfiniteIkeaLevel.CELL + InfiniteIkeaLevel.WALK, oz = sz * 2 * InfiniteIkeaLevel.CELL + InfiniteIkeaLevel.WALK;
        Object[][] spots = switch (theme) {
            case BACKYARD -> new Object[][]{{EntityType.SHEEP, 40, 25}, {EntityType.SHEEP, 46, 25}, {EntityType.COW, 52, 26},
                    {EntityType.CHICKEN, 10, 46}, {EntityType.CHICKEN, 15, 46}, {EntityType.CHICKEN, 20, 46}};
            case NATURE -> new Object[][]{{EntityType.PIG, 6, 6}, {EntityType.PIG, 52, 8}, {EntityType.RABBIT, 8, 50},
                    {EntityType.RABBIT, 50, 52}, {EntityType.CHICKEN, 22, 6}, {EntityType.SHEEP, 6, 30}};
            default -> new Object[0][];
        };
        ChunkPos chunk = region.getCenter();
        for (Object[] spot : spots) {
            int x = ox + (int) spot[1], z = oz + (int) spot[2];
            if (x < chunk.getMinBlockX() || x > chunk.getMaxBlockX() || z < chunk.getMinBlockZ() || z > chunk.getMaxBlockZ()) continue;
            BlockPos feet = new BlockPos(x, F + 1, z);
            if (!region.getBlockState(feet).isAir() || !region.getBlockState(feet.above()).isAir()) continue;
            Mob mob = (Mob) ((EntityType<?>) spot[0]).create(region.getLevel());
            if (mob == null) continue;
            mob.moveTo(x + 0.5, F + 1, z + 0.5, region.getRandom().nextFloat() * 360.0f, 0.0f);
            mob.finalizeSpawn(region, region.getCurrentDifficultyAt(feet), MobSpawnType.STRUCTURE, null);
            mob.setPersistenceRequired();
            region.addFreshEntity(mob);
        }
    }
}
