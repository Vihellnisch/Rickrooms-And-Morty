package com.rickandmorty.effect;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.Gore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** An open wound from a Mantis scythe or a Stalker's claw: you lose health and leave a trail of blood. */
public class BleedingEffect extends MobEffect {
    public static final ResourceKey<DamageType> BLEEDING = ResourceKey.create(Registries.DAMAGE_TYPE, RickAndMortyMod.id("bleeding"));

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A0303, Gore.BLOOD);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            entity.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BLEEDING)), 1.0f);
            level.sendParticles(Gore.BLOOD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    6, entity.getBbWidth() * 0.3, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.3, 0.0);
        }
        return true;
    }

    /** Once a second and a half, faster when it's worse. */
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = Math.max(8, 30 >> amplifier);
        return duration % interval == 0;
    }
}
