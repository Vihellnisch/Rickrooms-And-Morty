package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.noise;
import static com.rickandmorty.worldgen.level.GenMath.pick;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * The Glowstone Cavern: a cave with no walls, only a far-off ceiling of stone studded with
 * thousands of glowstone lights like a warm, wrong sky. Rolling grass, trees, slow rivers,
 * stone pillars holding the roof up, abandoned rail lines lit by redstone torches, and old
 * cobblestone castles that nobody built. A castle's keep holds the way out.
 */
final class CavernLevel implements LevelGenerator {
    static final int WATER = 13;
    private static final int TREE = 11;
    static final int CASTLE_REGION = 144;
    static final int CASTLE_W = 30, CASTLE_D = 22;
    private static final ResourceKey<LootTable> CASTLE_LOOT = ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/cavern_castle"));

    private final BlockState bedrock = of(Blocks.BEDROCK), stone = of(Blocks.STONE), dirt = of(Blocks.DIRT), grass = of(Blocks.GRASS_BLOCK);
    private final BlockState water = of(Blocks.WATER), sand = of(Blocks.SAND), glow = of(Blocks.GLOWSTONE), log = of(Blocks.OAK_LOG);
    private final BlockState leaves = of(Blocks.OAK_LEAVES).setValue(LeavesBlock.PERSISTENT, true);
    private final BlockState cobble = of(Blocks.COBBLESTONE), bricks = of(Blocks.STONE_BRICKS), mossy = of(Blocks.MOSSY_COBBLESTONE);

    @Override
    public int height() {
        return 72;
    }

    int ground(long seed, int x, int z) {
        return 14 + (int) Math.round(7 * noise(seed, x, z, 44, 1) + 3 * noise(seed, x, z, 15, 2) - 5);
    }

    private int ceilingBottom(long seed, int x, int z) {
        return 50 + (int) Math.round(8 * noise(seed, x, z, 38, 3) + 4 * noise(seed, x, z, 11, 4));
    }

    private boolean pillar(long seed, int x, int z) {
        return noise(seed, x, z, 26, 5) > 0.84;
    }

    /** The castle in this region: {x0, z0, base y}, or null. */
    int @Nullable [] castle(long seed, int rx, int rz) {
        if (unit(seed, rx, rz, 120) > 0.3) return null;
        int x0 = rx * CASTLE_REGION + 30 + pick(seed, rx, rz, 121, CASTLE_REGION - 60 - CASTLE_W);
        int z0 = rz * CASTLE_REGION + 30 + pick(seed, rx, rz, 122, CASTLE_REGION - 60 - CASTLE_D);
        int base = Math.max(WATER + 1, ground(seed, x0 + CASTLE_W / 2, z0 + CASTLE_D / 2));
        return new int[]{x0, z0, base};
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[height()];
        c[0] = bedrock;
        c[71] = bedrock;
        int g = ground(seed, x, z);
        int cb = ceilingBottom(seed, x, z);
        LevelParts.fill(c, 1, g - 4, stone);
        LevelParts.fill(c, g - 3, g - 1, dirt);
        boolean wet = g < WATER;
        c[g] = wet ? (g < WATER - 2 ? dirt : sand) : grass;
        if (wet) LevelParts.fill(c, g + 1, WATER, water);
        // the ceiling, and its lights
        LevelParts.fill(c, cb, 70, stone);
        long h = hash(seed, x, z, 123);
        if (unit(h) < 0.16) c[cb] = glow;
        if (unit(Long.rotateLeft(h, 13)) < 0.04) c[cb - 1] = glow;
        if (pillar(seed, x, z)) {
            LevelParts.fill(c, 1, cb, stone);
            if (unit(Long.rotateLeft(h, 30)) < 0.05) c[Math.min(cb - 1, g + 4)] = glow;
            return c;
        }
        int[] castle = castle(seed, Math.floorDiv(x, CASTLE_REGION), Math.floorDiv(z, CASTLE_REGION));
        if (castle != null && x >= castle[0] - 2 && x < castle[0] + CASTLE_W + 2 && z >= castle[1] - 2 && z < castle[1] + CASTLE_D + 5) {
            castle(c, x - castle[0], z - castle[1], castle[2], g, h);
            return c;
        }
        if (wet) return c;
        trees(c, seed, x, z);
        if (c[g + 1] == null) {
            double u = unit(Long.rotateLeft(h, 40));
            if (u < 0.1) c[g + 1] = of(Blocks.SHORT_GRASS);
            else if (u < 0.115) c[g + 1] = of(Blocks.RED_MUSHROOM);
            else if (u < 0.125) c[g + 1] = of(Blocks.BROWN_MUSHROOM);
        }
        rails(c, seed, x, z, g);
        return c;
    }

    /** Oak trees: leaves may reach into this column from trunks up to two cells away. */
    private void trees(BlockState[] c, long seed, int x, int z) {
        int tx = Math.floorDiv(x, TREE), tz = Math.floorDiv(z, TREE);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = tx + dx, cz = tz + dz;
                if (unit(seed, cx, cz, 124) > 0.3) continue;
                int trunkX = cx * TREE + 2 + pick(seed, cx, cz, 125, TREE - 4);
                int trunkZ = cz * TREE + 2 + pick(seed, cx, cz, 126, TREE - 4);
                int base = ground(seed, trunkX, trunkZ);
                if (base < WATER || pillar(seed, trunkX, trunkZ)) continue;
                int heightOf = 4 + pick(seed, cx, cz, 127, 3);
                if (x == trunkX && z == trunkZ) LevelParts.fill(c, base + 1, base + heightOf, log);
                double r = Math.hypot(x - trunkX, z - trunkZ);
                for (int y = base + heightOf - 2; y <= base + heightOf + 1; y++) {
                    double rr = y > base + heightOf ? 1.3 : 2.6;
                    if (r <= rr && c[y] == null && !(x == trunkX && z == trunkZ && y <= base + heightOf)) c[y] = leaves;
                }
            }
        }
    }

    /** Old rail lines running north-south, a redstone torch beside them every few blocks. */
    private void rails(BlockState[] c, long seed, int x, int z, int g) {
        int line = Math.floorDiv(x, 64);
        if (unit(seed, line, 0, 128) > 0.6) return;
        int lineX = line * 64 + 20 + pick(seed, line, 0, 129, 24);
        if (x == lineX && ground(seed, x, z - 1) == g && ground(seed, x, z + 1) == g && c[g + 1] == null) c[g + 1] = of(Blocks.RAIL);
        if (x == lineX + 1 && Math.floorMod(z, 8) == 0 && c[g + 1] == null) c[g + 1] = of(Blocks.REDSTONE_TORCH);
    }

    /**
     * A cobblestone castle: curtain walls with battlements, a tower at each corner, a gate to the
     * south with steps, and a stone keep in the courtyard with the exit door and a chest.
     */
    private void castle(BlockState[] c, int lx, int lz, int base, int g, long h) {
        int roof = c.length - 2;
        while (roof > g && c[roof] != null) roof--;                      // the ceiling stays
        LevelParts.fill(c, g + 1, roof, null);                           // clear the ground's plants and trees
        for (int y = Math.min(g, base); y < base; y++) c[y] = cobble;    // foundations
        for (int y = base + 1; y <= Math.max(g, base) + 1 && y <= roof; y++) c[y] = null;
        if (lx < 0 || lx >= CASTLE_W || lz < 0 || lz >= CASTLE_D) {
            // the steps down from the gate
            if (lz >= CASTLE_D && lz < CASTLE_D + 3 && lx >= 13 && lx <= 16) {
                int top = base - (lz - CASTLE_D);
                for (int y = g + 1; y < top; y++) c[y] = cobble;
                if (top > g) c[top] = of(Blocks.COBBLESTONE_STAIRS).setValue(StairBlock.FACING, Direction.NORTH);
            }
            return;
        }
        c[base] = of(Blocks.GRAVEL);
        boolean towerX = lx < 5 || lx >= CASTLE_W - 5, towerZ = lz < 5 || lz >= CASTLE_D - 5;
        boolean tower = towerX && towerZ;
        boolean edge = lx == 0 || lz == 0 || lx == CASTLE_W - 1 || lz == CASTLE_D - 1;
        if (tower) {
            boolean shell = lx == 0 || lz == 0 || lx == CASTLE_W - 1 || lz == CASTLE_D - 1 || lx == 4 || lx == CASTLE_W - 5 || lz == 4 || lz == CASTLE_D - 5;
            int top = base + 13;
            if (shell) {
                LevelParts.fill(c, base + 1, top, cobble);
                if ((lx + lz) % 2 == 0) c[top + 1] = cobble;                  // battlements
                if (Math.floorMod(h, 7) == 0) c[base + 3 + Math.floorMod(h >>> 5, 8)] = mossy;
            } else {
                c[top] = of(Blocks.SPRUCE_PLANKS);
            }
            return;
        }
        if (edge) {
            int top = base + 8;
            boolean gate = lz == CASTLE_D - 1 && lx >= 13 && lx <= 16;
            for (int y = base + 1; y <= top; y++) {
                if (gate && y <= base + 4) continue;
                c[y] = y == base + 4 ? bricks : cobble;
            }
            if ((lx + lz) % 2 == 0) c[top + 1] = cobble;
            if (Math.floorMod(h, 9) == 0) c[base + 2 + Math.floorMod(h >>> 5, 6)] = mossy;
            return;
        }
        // the keep, in the middle of the courtyard
        boolean inKeep = lx >= 9 && lx <= 20 && lz >= 5 && lz <= 13;
        if (inKeep) {
            boolean keepWall = lx == 9 || lx == 20 || lz == 5 || lz == 13;
            int top = base + 11;
            if (keepWall) {
                LevelParts.fill(c, base + 1, top, bricks);
                if ((lx + lz) % 2 == 0) c[top + 1] = bricks;
                if (lz == 13 && (lx == 12 || lx == 17)) {
                    c[base + 6] = of(Blocks.GLASS_PANE);
                    c[base + 7] = of(Blocks.GLASS_PANE);
                }
                if (lz == 13 && lx == 14) LevelParts.exitDoor(c, base + 1, Direction.SOUTH);
                if (lz == 13 && lx == 15) {
                    c[base + 1] = null;
                    c[base + 2] = null;
                }
            } else {
                c[top] = of(Blocks.SPRUCE_PLANKS);
                if (lx == 18 && lz == 7) c[base + 1] = of(Blocks.CHEST).setValue(ChestBlock.FACING, Direction.WEST);
                if (lx == 11 && lz == 7) c[base + 1] = of(Blocks.TORCH);
            }
        } else if (Math.floorMod(lx * 7 + lz * 3, 17) == 0) {
            c[base + 1] = of(Blocks.SHORT_GRASS);
        }
        if (!inKeep && lx == 14 && lz == 16) c[base + 1] = of(Blocks.TORCH);
    }

    @Override
    public int maxStandY() {
        return 48;
    }

    @Override
    public @Nullable ResourceKey<LootTable> containerLoot(long seed, BlockPos pos) {
        return CASTLE_LOOT;
    }

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        return BackroomsLevel.SUNLIT_LOBBY;
    }
}
