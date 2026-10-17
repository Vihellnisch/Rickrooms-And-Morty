package com.rickandmorty.worldgen.site;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Block-placing helpers for {@link SitePiece}, in coordinates relative to the site's origin
 * (y = 0 is the floor). Only blocks inside the chunk being generated are written, but callers
 * make every random roll regardless, so each chunk sees the same site.
 */
public final class SiteBuilder {
    final WorldGenLevel level;
    final BoundingBox box;
    final RandomSource random;
    final BlockPos origin;

    SiteBuilder(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos origin) {
        this.level = level;
        this.box = box;
        this.random = random;
        this.origin = origin;
    }

    static ResourceKey<LootTable> loot(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/" + name));
    }

    float roll() {
        return random.nextFloat();
    }

    int nextInt(int bound) {
        return random.nextInt(bound);
    }

    BlockPos at(int x, int y, int z) {
        return origin.offset(x, y, z);
    }

    boolean inChunk(int x, int y, int z) {
        return box.isInside(at(x, y, z));
    }

    void set(int x, int y, int z, BlockState state) {
        BlockPos pos = at(x, y, z);
        if (box.isInside(pos)) level.setBlock(pos, state, 2);
    }

    void set(int x, int y, int z, Block block) {
        set(x, y, z, block.defaultBlockState());
    }

    BlockState get(int x, int y, int z) {
        return level.getBlockState(at(x, y, z));
    }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, state);
            }
        }
    }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        fill(x0, y0, z0, x1, y1, z1, block.defaultBlockState());
    }

    /** Hollow box walls (no floor or ceiling). */
    void walls(int x0, int y0, int z0, int x1, int y1, int z1, java.util.function.Supplier<BlockState> material) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                set(x, y, z0, material.get());
                set(x, y, z1, material.get());
            }
            for (int z = z0 + 1; z < z1; z++) {
                set(x0, y, z, material.get());
                set(x1, y, z, material.get());
            }
        }
    }

    /**
     * Levels a rectangle: the floor layer (y = 0) becomes {@code floor}, the ground under it is
     * filled down to solid earth with {@code under}, and everything up to {@code clear} is air.
     */
    void foundation(int x0, int z0, int x1, int z1, @Nullable BlockState floor, BlockState under, int clear) {
        Heightmap.Types type = level instanceof WorldGenRegion ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.OCEAN_FLOOR;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                BlockPos column = at(x, 0, z);
                if (!box.isInside(column.getX(), box.minY(), column.getZ())) continue;
                int top = level.getHeight(type, column.getX(), column.getZ()) - 1;
                for (int y = -1; y >= -12 && origin.getY() + y > top - 1; y--) set(x, y, z, under);
                if (floor != null) set(x, 0, z, floor);
                for (int y = 1; y <= clear; y++) set(x, y, z, Blocks.AIR.defaultBlockState());
            }
        }
    }

    void chest(int x, int y, int z, Direction facing, ResourceKey<LootTable> table) {
        BlockPos pos = at(x, y, z);
        long seed = random.nextLong();     // drawn even when the chest is in another chunk
        if (!box.isInside(pos)) return;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(seed), pos, table);
    }

    void barrel(int x, int y, int z, Direction facing, @Nullable ResourceKey<LootTable> table) {
        BlockPos pos = at(x, y, z);
        long seed = random.nextLong();
        if (!box.isInside(pos)) return;
        level.setBlock(pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing), 2);
        if (table != null) RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(seed), pos, table);
    }

    void stairs(int x, int y, int z, Block block, Direction facing, boolean upsideDown) {
        set(x, y, z, block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM));
    }

    void slab(int x, int y, int z, Block block, boolean top) {
        set(x, y, z, block.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM));
    }

    /** A bed whose foot is at (x, z) and head one step towards {@code facing}. */
    void bed(int x, int y, int z, Block bed, Direction facing) {
        set(x, y, z, bed.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT));
        set(x + facing.getStepX(), y, z + facing.getStepZ(), bed.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.HEAD));
    }

    /** A wall sign with up to four lines of text. */
    void sign(int x, int y, int z, Direction facing, String... lines) {
        BlockPos pos = at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.WallSignBlock.FACING, facing), 2);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) writeSign(sign, level.registryAccess(), lines);
    }

    /**
     * Puts text on a sign's front. During world generation the sign isn't in a level yet, and
     * setText() would try to send a block update (and crash), so the text goes in as saved data.
     */
    public static void writeSign(SignBlockEntity sign, net.minecraft.core.RegistryAccess registries, String... lines) {
        Component[] text = new Component[4];
        for (int i = 0; i < 4; i++) text[i] = Component.literal(i < lines.length ? lines[i] : "");
        var ops = registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var front = SignText.DIRECT_CODEC.encodeStart(ops, new SignText(text, text, net.minecraft.world.item.DyeColor.WHITE, true)).getOrThrow();
        var tag = sign.saveWithoutMetadata(registries);
        tag.put("front_text", front);
        sign.loadWithComponents(tag, registries);
    }

    /** Spawns a mob standing at (x, y, z), once (in whichever chunk contains that spot). */
    <T extends Mob> void spawn(EntityType<T> type, double x, int y, double z, Consumer<T> setup) {
        BlockPos pos = at((int) Math.floor(x), y, (int) Math.floor(z));
        float yaw = random.nextFloat() * 360.0f;
        if (!box.isInside(pos)) return;
        T mob = type.create(level.getLevel());
        if (mob == null) return;
        mob.moveTo(origin.getX() + x, origin.getY() + y, origin.getZ() + z, yaw, 0.0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        setup.accept(mob);
        level.addFreshEntityWithPassengers(mob);
    }
}
