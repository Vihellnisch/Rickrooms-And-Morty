package com.rickandmorty.worldgen.level;

import com.rickandmorty.block.PartyBlocks;
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
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.noise;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * Level Fun =) : the Backrooms dressed for a party nobody left. The same damp yellow halls,
 * but strung with bunting, confetti all over the carpet, balloons against the ceiling,
 * presents piled in the corners, and long tables of cake in the bigger rooms. The Partygoers
 * are very glad you came.
 */
public final class FunLevel implements LevelGenerator {
    static final int CELL = 12;
    private static final int FLOOR = 1, CEIL = 6, ROOF = 7;
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, com.rickandmorty.RickAndMortyMod.id("chests/party_present"));

    public enum Room { HALL, ROOM, CAKE }

    private final BlockState bedrock = of(Blocks.BEDROCK), carpet = of(ModBlocks.BACKROOMS_CARPET.get());
    private final BlockState wallpaper = of(ModBlocks.BACKROOMS_WALLPAPER.get()), ceiling = of(ModBlocks.BACKROOMS_CEILING.get());
    private final BlockState confetti = of(ModBlocks.CONFETTI.get());
    private static final BlockState[] BUNTING = {of(Blocks.RED_WOOL), of(Blocks.YELLOW_WOOL), of(Blocks.LIGHT_BLUE_WOOL), of(Blocks.LIME_WOOL),
            of(Blocks.PINK_WOOL), of(Blocks.ORANGE_WOOL)};

    public static Room room(long seed, int cx, int cz) {
        double u = unit(seed, cx, cz, 900);
        return u < 0.35 ? Room.HALL : u < 0.8 ? Room.ROOM : Room.CAKE;
    }

    @Override
    public int height() {
        return ROOF + 1;
    }

    @Override
    public int maxStandY() {
        return FLOOR + 1;
    }

    @Override
    public @Nullable ResourceKey<LootTable> containerLoot(long seed, BlockPos pos) {
        return LOOT;
    }

    /** The way out of the party leads right back to where it all started. */
    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        return BackroomsLevel.LEVEL_0;
    }

    private BlockState balloon(long h) {
        return ModBlocks.BALLOON.get().defaultBlockState().setValue(PartyBlocks.COLOR, (int) Math.floorMod(h, 4L));
    }

    private BlockState present(long h) {
        return ModBlocks.PRESENT.get().defaultBlockState().setValue(PartyBlocks.COLOR, (int) Math.floorMod(h, 4L));
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[height()];
        c[0] = bedrock;
        c[ROOF] = bedrock;
        c[FLOOR] = carpet;
        c[CEIL] = ceiling;
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL), lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
        Room room = room(seed, cx, cz);
        if (lx == 0 || lz == 0) {
            boolean alongX = lz == 0;
            Room other = alongX ? room(seed, cx, cz - 1) : room(seed, cx - 1, cz);
            boolean open = room == Room.HALL && other == Room.HALL && !(lx == 0 && lz == 0) && unit(seed, x, z, 901) < 0.85;
            if (!open) {
                LevelParts.fill(c, FLOOR + 1, CEIL - 1, wallpaper);
                int along = alongX ? lx : lz;
                c[CEIL - 1] = BUNTING[Math.floorMod(along + (alongX ? cz : cx) * 3, BUNTING.length)];   // bunting round the top of the walls
                if (lx == 0 && lz == 0) return c;
                long door = hash(seed, cx, cz, alongX ? 1 : 2, 902);
                if (along >= 5 && along <= 6 && unit(door) < 0.8) {
                    c[FLOOR + 1] = null;
                    c[FLOOR + 2] = null;
                    c[FLOOR + 3] = null;
                } else if (along == 9 && unit(Long.rotateLeft(door, 1)) < 0.14 && !alongX) {
                    LevelParts.exitDoor(c, FLOOR + 1, Direction.EAST);
                }
                return c;
            }
        }
        long h = hash(seed, x, z, 903);
        if (lx % 4 == 2 && lz % 4 == 2) c[CEIL] = LevelParts.fluorescent(unit(h) < 0.08);
        // confetti scattered in drifts
        if (noise(seed, x, z, 3.0, 904) > 0.45 && unit(Long.rotateLeft(h, 3)) < 0.7) c[FLOOR + 1] = confetti;
        // balloons bunched against the ceiling, mostly in the corners
        boolean corner = (lx <= 2 || lx >= CELL - 2) && (lz <= 2 || lz >= CELL - 2);
        if (unit(Long.rotateLeft(h, 5)) < (corner ? 0.45 : 0.04)) c[CEIL - 1] = balloon(h >>> 9);
        // presents in the corners
        if (corner && unit(Long.rotateLeft(h, 13)) < 0.12) c[FLOOR + 1] = present(h >>> 17);
        if (room == Room.CAKE) cakeTable(c, h, lx, lz);
        return c;
    }

    /** A long table down the middle, set with cake, and chairs along both sides. */
    private void cakeTable(BlockState[] c, long h, int lx, int lz) {
        if (lx >= 3 && lx <= 8 && lz >= 5 && lz <= 6) {
            c[FLOOR + 1] = of(Blocks.WHITE_WOOL);                                       // the tablecloth
            c[FLOOR + 2] = lx % 2 == 1 ? Blocks.CAKE.defaultBlockState() : unit(Long.rotateLeft(h, 21)) < 0.4 ? present(h >>> 23) : null;
            if (c[FLOOR + 2] != null && c[FLOOR + 2].is(Blocks.CAKE) && unit(Long.rotateLeft(h, 25)) < 0.5) {
                c[FLOOR + 2] = Blocks.CAKE.defaultBlockState().setValue(net.minecraft.world.level.block.CakeBlock.BITES, 1 + (int) Math.floorMod(h >>> 27, 4L));
            }
        } else if (lx >= 3 && lx <= 8 && (lz == 4 || lz == 7) && lx % 2 == 1) {
            c[FLOOR + 1] = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, lz == 4 ? Direction.NORTH : Direction.SOUTH);
        }
    }

    @Override
    public void populate(WorldGenRegion region, long seed) {
        ChunkPos chunk = region.getCenter();
        for (int cx = Math.floorDiv(chunk.getMinBlockX(), CELL); cx <= Math.floorDiv(chunk.getMaxBlockX(), CELL); cx++) {
            for (int cz = Math.floorDiv(chunk.getMinBlockZ(), CELL); cz <= Math.floorDiv(chunk.getMaxBlockZ(), CELL); cz++) {
                if (room(seed, cx, cz) != Room.CAKE || unit(seed, cx, cz, 905) > 0.5) continue;
                int x = cx * CELL + 5, z = cz * CELL + 3;          // standing at the head of the table
                if (x < chunk.getMinBlockX() || x > chunk.getMaxBlockX() || z < chunk.getMinBlockZ() || z > chunk.getMaxBlockZ()) continue;
                var partygoer = ModEntities.PARTYGOER.get().create(region.getLevel());
                if (partygoer == null) continue;
                partygoer.moveTo(x + 0.5, FLOOR + 1, z + 0.5, 0.0f, 0.0f);
                partygoer.finalizeSpawn(region, region.getCurrentDifficultyAt(new BlockPos(x, FLOOR + 1, z)), MobSpawnType.STRUCTURE, null);
                region.addFreshEntity(partygoer);
            }
        }
    }
}
