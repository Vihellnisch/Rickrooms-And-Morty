package com.rickandmorty.entity.cruiser;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** One of the Space Cruiser's rockets: straight and fast, and it goes off on whatever it hits. */
public class CruiserRocket extends AbstractHurtingProjectile implements ItemSupplier {
    private static final ItemStack LOOK = new ItemStack(Items.FIREWORK_ROCKET);

    public CruiserRocket(EntityType<? extends CruiserRocket> type, Level level) {
        super(type, level);
        this.accelerationPower = 0.02;
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 100 && !level().isClientSide) discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof SpaceCruiserEntity) && !(target.getVehicle() instanceof SpaceCruiserEntity);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().explode(this, getX(), getY(), getZ(), 2.5f, false, Level.ExplosionInteraction.MOB);
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(damageSources().explosion(this, getOwner()), 8.0f);
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.FIREWORK;
    }

    @Override
    public ItemStack getItem() {
        return LOOK;
    }
}
