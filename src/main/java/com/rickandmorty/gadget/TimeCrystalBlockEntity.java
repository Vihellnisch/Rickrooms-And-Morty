package com.rickandmorty.gadget;

import com.rickandmorty.registry.ModBlockEntities;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Speeds time up around the crystal, 4 blocks every way: every machine there (anything with a
 * ticking block entity: furnaces, smokers, brewing stands, hoppers...) gets {@link #EXTRA_TICKS}
 * more ticks each tick, and plants and the like get extra random ticks, so crops grow several
 * times as fast.
 */
public class TimeCrystalBlockEntity extends BlockEntity {
    public static final int RADIUS = 4, EXTRA_TICKS = 3, RANDOM_TICKS = 4;
    private static final DustParticleOptions SPARKLE = new DustParticleOptions(new Vector3f(1.0f, 0.45f, 0.85f), 0.8f);
    private final List<BlockPos> machines = new ArrayList<>();
    private int age;

    public TimeCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIME_CRYSTAL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TimeCrystalBlockEntity crystal) {
        if (!(level instanceof ServerLevel server)) return;
        if (crystal.age++ % 40 == 0) crystal.findMachines(server);
        for (BlockPos at : crystal.machines) speedUp(server, at);
        RandomSource random = server.random;
        for (int i = 0; i < RANDOM_TICKS; i++) {
            BlockPos at = pos.offset(random.nextInt(2 * RADIUS + 1) - RADIUS, random.nextInt(2 * RADIUS + 1) - RADIUS, random.nextInt(2 * RADIUS + 1) - RADIUS);
            BlockState there = server.getBlockState(at);
            if (there.isRandomlyTicking()) there.randomTick(server, at, random);
        }
        if (crystal.age % 10 == 0) {
            server.sendParticles(SPARKLE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 2, 0.25, 0.3, 0.25, 0.0);
        }
        if (crystal.age % 120 == 0) server.playSound(null, pos, ModSounds.TIME_CRYSTAL_HUM.get(), SoundSource.BLOCKS, 0.4f, 1.0f);
    }

    /** Which nearby blocks have a ticking machine in them (not other crystals: they'd only speed each other up). */
    private void findMachines(ServerLevel level) {
        machines.clear();
        for (BlockPos at : BlockPos.betweenClosed(worldPosition.offset(-RADIUS, -RADIUS, -RADIUS), worldPosition.offset(RADIUS, RADIUS, RADIUS))) {
            if (!level.isLoaded(at)) continue;
            BlockEntity be = level.getBlockEntity(at);
            if (be == null || be instanceof TimeCrystalBlockEntity) continue;
            if (be.getBlockState().getTicker(level, be.getType()) != null) machines.add(at.immutable());
        }
    }

    @SuppressWarnings("unchecked")
    private static void speedUp(ServerLevel level, BlockPos at) {
        BlockEntity be = level.getBlockEntity(at);
        if (be == null || be.isRemoved() || be instanceof TimeCrystalBlockEntity) return;
        BlockState state = be.getBlockState();
        BlockEntityTicker<BlockEntity> ticker = state.getTicker(level, (BlockEntityType<BlockEntity>) be.getType());
        if (ticker == null) return;
        for (int i = 0; i < EXTRA_TICKS; i++) ticker.tick(level, at, state, be);
    }
}
