package com.rickandmorty.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/** The Endless Field's sky: the Overworld's, sun and all, with the clouds hanging lower, closer to the grass. */
public class FieldSky extends DimensionSpecialEffects {
    public FieldSky() {
        super(118.0f, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.multiply(brightness * 0.94f + 0.06f, brightness * 0.94f + 0.06f, brightness * 0.91f + 0.09f);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }
}
