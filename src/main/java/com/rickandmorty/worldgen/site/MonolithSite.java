package com.rickandmorty.worldgen.site;

import com.rickandmorty.block.FluorescentLightBlock;
import com.rickandmorty.block.entity.ExitDoorBlockEntity;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.GenMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The Monolith: one of the towers from Level 0's great hall, come through into the Overworld and
 * broken on the way. It's far taller than it was down there, leaning, cracked and crumbling at
 * the top, with a snapped-off twin beside it and chunks of it strewn about. Around its foot the
 * Backrooms are leaking out: damp carpet in the grass, scraps of yellow wallpaper, a dead
 * fluorescent light half buried. There's a way into a small room inside, and a red door in its
 * face that leads back to where it came from.
 */
final class MonolithSite {
    private static final ResourceKey<LootTable> VAULT = SiteBuilder.loot("monolith_vault");
    static final int HEIGHT = 112;
    private static final int X0 = -5, X1 = 4, Z0 = -5, Z1 = 4;

    private MonolithSite() {
    }

    private static BlockState monolith() {
        return ModBlocks.MONOLITH.get().defaultBlockState();
    }

    static void build(SiteBuilder b) {
        long seed = b.random.nextLong();
        BlockState air = Blocks.AIR.defaultBlockState();
        b.foundation(-7, -7, 6, 6, null, Blocks.STONE.defaultBlockState(), 0);

        // --- the tower: 10x10, leaning east and wobbling, cracked, its top broken off unevenly
        for (int y = 0; y <= HEIGHT; y++) {
            int lean = Math.round(y * 0.075f);
            int wobble = Math.round((float) Math.sin(y / 17.0) * 1.4f);
            for (int x = X0; x <= X1; x++) {
                for (int z = Z0; z <= Z1; z++) {
                    int top = HEIGHT - (int) (GenMath.unit(seed, x, z, 1) * 26) - (edge(x, z) ? 6 : 0);
                    if (y > top) continue;
                    boolean surface = edge(x, z);
                    // crumbling: more of the outside is missing the higher you go
                    double rot = GenMath.unit(GenMath.hash(seed, x, z, y, 2));
                    if (surface && y > 12 && rot < (y - 12) / (double) HEIGHT * 0.45) continue;
                    // cracks: thin vertical splits running down two of the faces
                    if (surface && (x == -2 && z == Z1 || z == 1 && x == X0) && y > 20 && y % 23 < 11) continue;
                    b.set(x + lean, y, z + wobble, monolith());
                }
            }
        }

        // --- its twin, snapped off a third of the way up and leaning the other way
        for (int y = 0; y <= 38; y++) {
            int lean = -Math.round(y * 0.12f);
            for (int x = 10; x <= 17; x++) {
                for (int z = -3; z <= 4; z++) {
                    int top = 24 + (int) (GenMath.unit(seed, x, z, 3) * 14);
                    if (y > top) continue;
                    if (edge2(x, z) && GenMath.unit(GenMath.hash(seed, x, z, y, 4)) < y / 60.0) continue;
                    b.set(x, y, z + lean, monolith());
                }
            }
        }

        // --- a room inside the tower's foot, reached through a breach in the north face
        b.fill(-3, 1, -3, 2, 4, 2, air);
        for (int x = -3; x <= 2; x++) {
            for (int z = -3; z <= 2; z++) b.set(x, 0, z, ModBlocks.BACKROOMS_CARPET.get());
        }
        for (int y = 1; y <= 4; y++) {
            for (int i = -3; i <= 2; i++) {
                b.set(i, y, -4, ModBlocks.BACKROOMS_WALLPAPER.get());
                b.set(i, y, 3, ModBlocks.BACKROOMS_WALLPAPER.get());
                b.set(-4, y, i, ModBlocks.BACKROOMS_WALLPAPER.get());
                b.set(3, y, i, ModBlocks.BACKROOMS_WALLPAPER.get());
            }
        }
        b.fill(-1, 1, -5, 0, 3, -4, air);                                  // the breach
        b.set(-2, 2, -5, air);
        b.set(1, 1, -5, air);
        b.set(-1, 5, -3, FluorescentLightBlock.broken(ModBlocks.FLUORESCENT_LIGHT.get().defaultBlockState()));
        b.chest(1, 1, 1, Direction.WEST, VAULT);

        // --- the red door in its south face, back to the hall it came from
        BlockState door = ModBlocks.EXIT_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        b.set(0, 0, Z1 + 1, Blocks.STONE_BRICKS.defaultBlockState());
        b.set(0, 1, Z1, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        b.set(0, 2, Z1, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        BlockPos doorPos = b.at(0, 1, Z1);
        if (b.box.isInside(doorPos) && b.level.getBlockEntity(doorPos) instanceof ExitDoorBlockEntity exit) {
            exit.setDestination(BackroomsLevel.LEVEL_0);
            exit.setArrival(ExitDoorBlockEntity.Arrival.MONOLITH);
        }

        // --- chunks that fell off it, half sunk into the ground
        int chunks = 12 + b.nextInt(6);
        for (int i = 0; i < chunks; i++) {
            double angle = b.roll() * Math.PI * 2, distance = 8 + b.roll() * 16;
            int cx = (int) Math.round(Math.cos(angle) * distance), cz = (int) Math.round(Math.sin(angle) * distance);
            int sx = 1 + b.nextInt(3), sy = 1 + b.nextInt(3), sz = 1 + b.nextInt(3), sink = b.nextInt(2);
            for (int x = 0; x <= sx; x++) {
                for (int y = 0; y <= sy; y++) {
                    for (int z = 0; z <= sz; z++) {
                        if (b.roll() < 0.18f) continue;
                        groundSet(b, cx + x, y - sink, cz + z, monolith());
                    }
                }
            }
        }

        // --- the Backrooms leaking out round its foot: carpet in the grass, torn wallpaper, a dead light
        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 16; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d < 7 || d > 16) continue;
                double n = GenMath.noise(seed, x, z, 5.0, 6);
                if (n > 0.3 + (d - 7) / 9 * 0.45) groundSet(b, x, 0, z, ModBlocks.BACKROOMS_CARPET.get().defaultBlockState());
            }
        }
        for (int w = 0; w < 4; w++) {
            double angle = b.roll() * Math.PI * 2, distance = 9 + b.roll() * 6;
            int wx = (int) Math.round(Math.cos(angle) * distance), wz = (int) Math.round(Math.sin(angle) * distance);
            boolean alongX = b.roll() < 0.5f;
            int length = 3 + b.nextInt(3);
            for (int i = 0; i < length; i++) {
                int height = 1 + b.nextInt(3);
                for (int y = 1; y <= height; y++) {
                    groundSet(b, wx + (alongX ? i : 0), y, wz + (alongX ? 0 : i), ModBlocks.BACKROOMS_WALLPAPER.get().defaultBlockState());
                }
            }
        }
        double angle = b.roll() * Math.PI * 2;
        int lx = (int) Math.round(Math.cos(angle) * 11), lz = (int) Math.round(Math.sin(angle) * 11);
        groundSet(b, lx, 0, lz, FluorescentLightBlock.broken(ModBlocks.FLUORESCENT_LIGHT.get().defaultBlockState()));
        groundSet(b, lx + 1, 0, lz, ModBlocks.BACKROOMS_CEILING.get().defaultBlockState());
    }

    private static boolean edge(int x, int z) {
        return x == X0 || x == X1 || z == Z0 || z == Z1;
    }

    private static boolean edge2(int x, int z) {
        return x == 10 || x == 17 || z == -3 || z == 4;
    }

    private static final java.util.Map<Long, Integer> GROUND = java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<>(512, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Long, Integer> eldest) {
            return size() > 4096;
        }
    });

    /**
     * Sets a block relative to the natural ground at (x, z) rather than the site's floor, so
     * debris sits on hills. The ground comes from the terrain generator, not the world, so it
     * ignores the tower and the rubble already placed.
     */
    private static void groundSet(SiteBuilder b, int x, int y, int z, BlockState state) {
        BlockPos column = b.at(x, 0, z);
        if (!b.box.isInside(column.getX(), b.box.minY(), column.getZ())) return;
        net.minecraft.server.level.ServerLevel server = b.level.getLevel();
        long key = column.asLong() ^ ((long) server.dimension().location().hashCode() << 40);
        int top = GROUND.computeIfAbsent(key, k -> server.getChunkSource().getGenerator().getBaseHeight(column.getX(), column.getZ(),
                net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, b.level, server.getChunkSource().randomState()));
        b.set(x, top - 1 - b.origin.getY() + y, z, state);
    }
}
