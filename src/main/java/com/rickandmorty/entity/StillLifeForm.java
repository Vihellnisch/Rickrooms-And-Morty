package com.rickandmorty.entity;

/**
 * The shapes the Backrooms gets people wrong in. Each wears a real player's skin (often the
 * skin of whoever it saw first). Sizes are the hitboxes; how they're bent is in StillLifeRenderer.
 */
public enum StillLifeForm {
    /** Like Captain Clark: far too tall and thin, a long stretched face, and it moves like stop-motion. */
    CAPTAIN(0.6f, 3.0f, 40, 6, 0.34),
    /** Three faces pressed into one head. */
    THREE_FACES(0.75f, 2.0f, 36, 5, 0.26),
    /** Short, wide, heavy, with one face sliding down over another. */
    TWICE(0.95f, 1.9f, 50, 8, 0.22),
    /** Folded forward at the waist, walking on its hands and feet, its head on upside down. */
    FOLDED(0.9f, 1.3f, 26, 4, 0.36),
    /** Too many arms, holding on to each other. And then to you. */
    MANY_HANDS(0.7f, 1.95f, 34, 5, 0.27),
    /** Hunched under a hump, its head sunk sideways into its chest, the hat still on the empty neck, dragging one long arm and a briefcase. */
    COMMUTER(0.8f, 1.6f, 38, 7, 0.24),
    /** Nearly right. One arm hangs to the floor and the other barely reaches the hip, one boot is enormous, the face is too wide. */
    LOPSIDED(0.75f, 2.05f, 34, 6, 0.28),
    /** Its arms go on and on, coiling down to the floor, and its head is on backwards: it walks at you with its back to you. */
    NOODLE_ARMS(0.6f, 2.0f, 30, 5, 0.3),
    /** Its face has grown into the plank it was carrying. */
    PLANK_FACE(0.7f, 1.95f, 44, 7, 0.24),
    /** Its face splits open, top from bottom, onto a mouth that isn't a mouth. */
    GAPING(0.65f, 1.95f, 28, 5, 0.32);

    public final float width, height;
    public final double health, damage, speed;

    StillLifeForm(float width, float height, double health, double damage, double speed) {
        this.width = width;
        this.height = height;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
    }

    /** Registry name, e.g. "still_life_captain". */
    public String id() {
        return "still_life_" + name().toLowerCase();
    }
}
