package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * The Harvest: an indoor farm with no end. Fields of dead corn between fenced paths, under a
 * ceiling so full of lights it never gets dark. Scarecrows stand in the fields. They don't
 * move while anyone is looking. Here and there a farmhouse lies crushed and tilted, as if it
 * had been dropped from somewhere; the way out is inside.
 * <pre>
 *  y 17 bedrock   y 16 ceiling and lights   y 2-3 corn   y 1 soil / paths
 * </pre>
 */
final class HarvestLevel implements LevelGenerator {
    static final int FIELD_X = 26, FIELD_Z = 20;      // a field plus the path beside it
    private static final int PATH = 4;
    private static final int FARM = 4;                 // farmhouses are placed per 4x4 fields
    private static final ResourceKey<LootTable> FARMHOUSE = ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/harvest_farmhouse"));

    private final BlockState bedrock = of(Blocks.BEDROCK), path = of(Blocks.SMOOTH_SANDSTONE), soil = of(Blocks.COARSE_DIRT);
    private final BlockState ceiling = of(ModBlocks.BACKROOMS_CEILING.get()), light = of(ModBlocks.PANEL_LIGHT.get()), fence = of(Blocks.SPRUCE_FENCE);
    private final BlockState cornLow = ModBlocks.DRIED_CORN.get().defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
    private final BlockState cornHigh = ModBlocks.DRIED_CORN.get().defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
    private final BlockState plank = of(Blocks.SPRUCE_PLANKS), siding = of(Blocks.LIGHT_GRAY_TERRACOTTA), roof = of(Blocks.DARK_OAK_PLANKS);

    @Override
    public int height() {
        return 18;
    }

    @Override
    public int maxStandY() {
        return 4;
    }

    /** The farmhouse in this block of fields, as {field x, field z}, or null. */
    private int @Nullable [] farmhouse(long seed, int fx, int fz) {
        int rx = Math.floorDiv(fx, FARM), rz = Math.floorDiv(fz, FARM);
        if (unit(seed, rx, rz, 70) > 0.4) return null;
        return new int[]{rx * FARM + GenMath.pick(seed, rx, rz, 71, FARM), rz * FARM + GenMath.pick(seed, rx, rz, 72, FARM)};
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[height()];
        c[0] = bedrock;
        c[17] = bedrock;
        c[16] = Math.floorMod(x, 5) == 2 && Math.floorMod(z, 5) == 2 ? light : ceiling;
        int fx = Math.floorDiv(x, FIELD_X), fz = Math.floorDiv(z, FIELD_Z);
        int lx = Math.floorMod(x, FIELD_X), lz = Math.floorMod(z, FIELD_Z);
        c[1] = path;
        if (lx >= FIELD_X - PATH || lz >= FIELD_Z - PATH) return c;     // the path between fields

        int[] farm = farmhouse(seed, fx, fz);
        if (farm != null && farm[0] == fx && farm[1] == fz) {
            farmhouse(c, seed, fx, fz, lx, lz);
            return c;
        }
        c[1] = soil;
        int w = FIELD_X - PATH, d = FIELD_Z - PATH;
        boolean edge = lx == 0 || lz == 0 || lx == w - 1 || lz == d - 1;
        if (edge) {
            // a fence round most fields, with a way in on each side
            int side = lx == 0 ? 0 : lx == w - 1 ? 1 : lz == 0 ? 2 : 3;
            int along = side < 2 ? lz : lx;
            boolean fenced = unit(seed, fx, fz, 73 + side) < 0.7 && Math.abs(along - (side < 2 ? d : w) / 2) > 1;
            if (fenced) c[2] = fence;
            return c;
        }
        double u = unit(seed, x, z, 77);
        if (lx % 2 == 1 && u < 0.92) {                   // rows of corn, a few gaps
            c[2] = cornLow;
            c[3] = cornHigh;
        } else if (u < 0.08) {
            c[2] = cornLow;
            c[3] = cornHigh;
        }
        return c;
    }

    /**
     * A grey farmhouse, crushed and tipped over to one side: walls broken, roof sagging from
     * five blocks up down to two. The exit door is at the back.
     */
    private void farmhouse(BlockState[] c, long seed, int fx, int fz, int lx, int lz) {
        c[1] = soil;
        int hx = lx - 4, hz = lz - 3;                      // 14 x 10 house in the 22 x 16 plot
        if (hx < 0 || hz < 0 || hx > 13 || hz > 9) {
            if (unit(seed, fx * 31 + lx, fz * 17 + lz, 78) < 0.3) c[2] = of(Blocks.HAY_BLOCK);   // spilled bales
            return;
        }
        c[1] = plank;
        int roofY = 7 - hx * 4 / 13;                       // tilted: high at one end, crushed at the other
        boolean wallCol = hx == 0 || hx == 13 || hz == 0 || hz == 9;
        long h = hash(seed, fx * 16 + hx, fz * 16 + hz, 79);
        if (wallCol) {
            boolean frontDoor = hz == 0 && (hx == 6 || hx == 7);
            for (int y = 2; y < roofY; y++) {
                if (frontDoor && y <= 3) continue;
                if (unit(Long.rotateLeft(h, y)) < 0.15) continue;                      // broken boards
                c[y] = (y + hx) % 3 == 0 ? plank : siding;
            }
            if (hz == 9 && hx == 6) LevelParts.exitDoor(c, 2, Direction.NORTH);   // the way out
        } else if (hx == 11 && hz == 7) {
            c[2] = of(Blocks.CHEST).setValue(ChestBlock.FACING, Direction.WEST);
        } else if (hx == 2 && hz >= 2 && hz <= 4) {
            c[2] = of(Blocks.HAY_BLOCK);
        }
        if (unit(Long.rotateLeft(h, 40)) > 0.18) c[roofY] = hz == 0 || hz == 9 ? of(Blocks.DARK_OAK_STAIRS).setValue(StairBlock.FACING, hz == 0 ? Direction.SOUTH : Direction.NORTH) : roof;
    }

    /** Scarecrows in the fields: two to four per field. */
    @Override
    public void populate(WorldGenRegion region, long seed) {
        ChunkPos chunk = region.getCenter();
        int fx0 = Math.floorDiv(chunk.getMinBlockX(), FIELD_X), fx1 = Math.floorDiv(chunk.getMaxBlockX(), FIELD_X);
        int fz0 = Math.floorDiv(chunk.getMinBlockZ(), FIELD_Z), fz1 = Math.floorDiv(chunk.getMaxBlockZ(), FIELD_Z);
        for (int fx = fx0; fx <= fx1; fx++) {
            for (int fz = fz0; fz <= fz1; fz++) {
                int count = 1 + GenMath.pick(seed, fx, fz, 80, 2);
                for (int i = 0; i < count; i++) {
                    long h = hash(seed, fx, fz, 81 + i);
                    int x = fx * FIELD_X + 2 + (int) Math.floorMod(h, (long) (FIELD_X - PATH - 4));
                    int z = fz * FIELD_Z + 2 + (int) Math.floorMod(h >>> 20, (long) (FIELD_Z - PATH - 4));
                    if (x < chunk.getMinBlockX() || x > chunk.getMaxBlockX() || z < chunk.getMinBlockZ() || z > chunk.getMaxBlockZ()) continue;
                    var scarecrow = ModEntities.SCARECROW.get().create(region.getLevel());
                    if (scarecrow == null) continue;
                    scarecrow.moveTo(x + 0.5, 2, z + 0.5, (float) Math.floorMod(h >>> 40, 360L), 0.0f);
                    scarecrow.finalizeSpawn(region, region.getCurrentDifficultyAt(new BlockPos(x, 2, z)), MobSpawnType.STRUCTURE, null);
                    scarecrow.setPersistenceRequired();
                    region.addFreshEntity(scarecrow);
                }
            }
        }
    }

    @Override
    public @Nullable ResourceKey<LootTable> containerLoot(long seed, BlockPos pos) {
        return FARMHOUSE;
    }

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        return BackroomsLevel.SUNLIT_LOBBY;
    }
}
