package com.rickandmorty.worldgen.anomaly;

import net.minecraft.util.StringRepresentable;

/** The ways the Backrooms leaks into the Overworld. */
public enum AnomalyKind implements StringRepresentable {
    /** A small, ruined piece of Level 0 sitting in the landscape. */
    BACKROOMS_REMNANT,
    /** An oak tree upside down: canopy on the ground, trunk rising into a floating clump of earth. */
    INVERTED_TREE,
    /** A circle of the wrong biome, with a few of its blocks hanging in the air above. */
    GLITCHED_PATCH;

    public static final StringRepresentable.EnumCodec<AnomalyKind> CODEC = StringRepresentable.fromEnum(AnomalyKind::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
