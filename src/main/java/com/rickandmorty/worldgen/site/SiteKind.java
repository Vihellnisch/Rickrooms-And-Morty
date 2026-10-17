package com.rickandmorty.worldgen.site;

import net.minecraft.util.StringRepresentable;

/**
 * Built places: the ruins and camps of Cronenberg World, and the Council's chamber in the
 * Overworld. Half-sizes are the footprint around the origin; height is above the floor.
 */
public enum SiteKind implements StringRepresentable {
    /** A suburban house the flesh is swallowing. */
    RUINED_HOUSE(6, 9, 3),
    /** Jerry, Beth and Summer's fortified home: the Smith house, the garage, a wall of scrap and teeth. */
    SURVIVOR_CAMP(14, 16, 3),
    /** Harry Herpson High School, now a Mantis nest. */
    HIGH_SCHOOL(22, 12, 3),
    /** A crater of living meat with the Behemoth asleep at the bottom. */
    FLESH_PIT(16, 16, 15),
    /** The Council of Yous: alternate versions of this world's players, sitting in judgement. */
    COUNCIL_CHAMBER(13, 16, 3),
    /** One of Level 0's monoliths, come through into the Overworld broken, leaning and far taller. */
    MONOLITH(28, MonolithSite.HEIGHT + 2, 4),
    /** A perfectly normal IKEA. Its doors open into the Infinite IKEA. */
    STORE(17, StoreSite.TOP + 3, 13);

    public static final StringRepresentable.EnumCodec<SiteKind> CODEC = StringRepresentable.fromEnum(SiteKind::values);

    public final int half;
    public final int height;
    public final int depth;

    SiteKind(int half, int height, int depth) {
        this.half = half;
        this.height = height;
        this.depth = depth;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
