package com.rickandmorty.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** An effect that's nothing but its attribute changes (Shrunk, Enlarged). */
public class PlainEffect extends MobEffect {
    public PlainEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
