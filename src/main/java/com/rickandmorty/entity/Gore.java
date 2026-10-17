package com.rickandmorty.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Blood and guts, as particles. */
public final class Gore {
    public static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.5f, 0.0f, 0.02f), 1.3f);
    public static final DustParticleOptions DARK_BLOOD = new DustParticleOptions(new Vector3f(0.25f, 0.0f, 0.0f), 1.8f);
    private static final BlockParticleOption CHUNKS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState());
    private static final BlockParticleOption MEAT = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHER_WART_BLOCK.defaultBlockState());
    private static final ItemParticleOption FLESH = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ROTTEN_FLESH));
    private static final ItemParticleOption BONE = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.BONE));

    private Gore() {
    }

    /** A spray of blood (server side). */
    public static void splash(ServerLevel level, Vec3 at, int count, double spread) {
        level.sendParticles(BLOOD, at.x, at.y, at.z, count, spread, spread, spread, 0.0);
        level.sendParticles(CHUNKS, at.x, at.y, at.z, count / 2, spread, spread, spread, 0.15);
    }

    /** Blood from a wound, scaled to how hard the hit was. */
    public static void wound(Entity entity, float damage) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        Vec3 at = entity.position().add(0, entity.getBbHeight() * 0.6, 0);
        int count = Math.min(40, 6 + (int) (damage * 3));
        splash(level, at, count, entity.getBbWidth() * 0.35);
    }

    /** Something just came apart: blood, chunks of meat and bone everywhere. */
    public static void burst(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        Vec3 at = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        double spread = entity.getBbWidth() * 0.5 + 0.2;
        int size = (int) Math.ceil(entity.getBbWidth() * entity.getBbHeight() * 20);
        level.sendParticles(DARK_BLOOD, at.x, at.y, at.z, 40 + size, spread, spread, spread, 0.0);
        level.sendParticles(CHUNKS, at.x, at.y, at.z, 30 + size, spread, spread, spread, 0.3);
        level.sendParticles(MEAT, at.x, at.y, at.z, 20 + size / 2, spread, spread, spread, 0.3);
        level.sendParticles(FLESH, at.x, at.y, at.z, 12 + size / 4, spread, spread, spread, 0.25);
        level.sendParticles(BONE, at.x, at.y, at.z, 4 + size / 8, spread, spread, spread, 0.25);
    }

    /** Client-side: a few drops of blood around a point (used while something is being blended). */
    public static void clientSpray(Level level, Vec3 at, int count, double spread) {
        for (int i = 0; i < count; i++) {
            double vx = (level.random.nextDouble() - 0.5) * 0.6, vy = level.random.nextDouble() * 0.4, vz = (level.random.nextDouble() - 0.5) * 0.6;
            double x = at.x + (level.random.nextDouble() - 0.5) * spread;
            double y = at.y + (level.random.nextDouble() - 0.5) * spread;
            double z = at.z + (level.random.nextDouble() - 0.5) * spread;
            level.addParticle(i % 3 == 0 ? MEAT : CHUNKS, x, y, z, vx, vy, vz);
            level.addParticle(BLOOD, x, y, z, 0, 0, 0);
        }
        if (level.random.nextInt(3) == 0) {
            level.addParticle(FLESH, at.x, at.y, at.z, (level.random.nextDouble() - 0.5) * 0.5, 0.3, (level.random.nextDouble() - 0.5) * 0.5);
        }
    }
}
