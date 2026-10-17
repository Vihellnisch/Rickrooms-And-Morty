package com.rickandmorty.entity;

/**
 * The kinds of Cronenberg: what's left of the people (and pets) of Cronenberg World after
 * Rick's "cure". Sizes are the hitboxes of their models.
 */
public enum CronenbergKind {
    /** Low and wide, scuttling on six human arms, a face stretched across the front. Fast and always hungry. */
    CRAWLER(18, 4, 0.32, 1.3f, 0.9f),
    /** A tall, lopsided thing: one huge clawed arm, a second head growing out of its shoulder. */
    STALKER(32, 7, 0.27, 0.8f, 2.5f),
    /** A heaving mound of eyes and mouths. Slow, and leaves you alone... unless you get too close. Bursts when it dies. */
    BLOB(44, 5, 0.19, 1.5f, 1.5f);

    public final double health;
    public final double damage;
    public final double speed;
    public final float width;
    public final float height;

    CronenbergKind(double health, double damage, double speed, float width, float height) {
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.width = width;
        this.height = height;
    }

    /** Registry name, e.g. "cronenberg_crawler". */
    public String id() {
        return "cronenberg_" + name().toLowerCase();
    }
}
