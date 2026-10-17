package com.rickandmorty.entity;

import net.minecraft.util.RandomSource;

/**
 * How a Council member differs from the player they're a version of: a wrong body shape and
 * (usually) a hat. Stored in one int so it syncs cheaply.
 */
public record CouncilLook(Shape shape, Hat hat) {
    public enum Shape {
        /** x, y, z body scale; head scale; arm length. */
        TALL(0.85f, 1.35f, 0.85f, 1.0f, 1.0f),
        SHORT(1.1f, 0.62f, 1.1f, 1.15f, 1.0f),
        WIDE(1.55f, 0.95f, 1.35f, 1.0f, 1.0f),
        BIG_HEAD(0.95f, 0.95f, 0.95f, 1.9f, 1.0f),
        TINY_HEAD(1.05f, 1.1f, 1.05f, 0.5f, 1.0f),
        LONG_ARMS(0.9f, 1.05f, 0.9f, 1.0f, 1.65f),
        NOODLE(0.55f, 1.5f, 0.55f, 0.9f, 1.2f);

        public final float scaleX, scaleY, scaleZ, head, arms;

        Shape(float scaleX, float scaleY, float scaleZ, float head, float arms) {
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.head = head;
            this.arms = arms;
        }
    }

    public enum Hat {
        NONE, TOP_HAT, WIZARD, COWBOY, CROWN, BEANIE, PROPELLER, TRAFFIC_CONE
    }

    public static CouncilLook random(RandomSource random) {
        Shape shape = Shape.values()[random.nextInt(Shape.values().length)];
        Hat hat = random.nextFloat() < 0.15f ? Hat.NONE : Hat.values()[1 + random.nextInt(Hat.values().length - 1)];
        return new CouncilLook(shape, hat);
    }

    /** Never 0, so 0 can mean "not rolled yet". */
    public int pack() {
        return 1 + shape.ordinal() * 16 + hat.ordinal();
    }

    public static CouncilLook unpack(int packed) {
        if (packed <= 0) return new CouncilLook(Shape.TALL, Hat.NONE);
        int value = packed - 1;
        Shape[] shapes = Shape.values();
        Hat[] hats = Hat.values();
        return new CouncilLook(shapes[Math.min(shapes.length - 1, value / 16)], hats[Math.min(hats.length - 1, value % 16)]);
    }
}
