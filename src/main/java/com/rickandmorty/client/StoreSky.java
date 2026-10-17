package com.rickandmorty.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * The Infinite IKEA's sky: a flat grey overcast, no clouds, and a fog so close you can never see
 * how far the store goes. It darkens with the evening like any sky, and at night it's black.
 */
public class StoreSky extends DimensionSpecialEffects {
    public StoreSky() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        float f = brightness * 0.92f + 0.04f;
        return fogColor.multiply(f, f, f);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }
}
