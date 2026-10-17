package com.rickandmorty.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * The vanilla mobs the Backrooms has tried to copy. Stats are for the copy; sizes match the
 * stretched-out way they're rendered (see MimicRenderer).
 */
public enum MimicKind {
    VILLAGER(24, 4, 0.30, 0.55f, 2.2f, SoundEvents.VILLAGER_AMBIENT, SoundEvents.VILLAGER_HURT, SoundEvents.VILLAGER_DEATH),
    SHEEP(16, 3, 0.32, 0.8f, 1.6f, SoundEvents.SHEEP_AMBIENT, SoundEvents.SHEEP_HURT, SoundEvents.SHEEP_DEATH),
    PIG(20, 4, 0.30, 0.9f, 0.8f, SoundEvents.PIG_AMBIENT, SoundEvents.PIG_HURT, SoundEvents.PIG_DEATH),
    COW(30, 6, 0.25, 0.8f, 1.75f, SoundEvents.COW_AMBIENT, SoundEvents.COW_HURT, SoundEvents.COW_DEATH),
    CHICKEN(6, 2, 0.38, 0.3f, 1.1f, SoundEvents.CHICKEN_AMBIENT, SoundEvents.CHICKEN_HURT, SoundEvents.CHICKEN_DEATH),
    ZOMBIE(30, 5, 0.30, 0.55f, 2.35f, SoundEvents.ZOMBIE_AMBIENT, SoundEvents.ZOMBIE_HURT, SoundEvents.ZOMBIE_DEATH);

    public final double health;
    public final double damage;
    public final double speed;
    public final float width;
    public final float height;
    final SoundEvent ambient;
    final SoundEvent hurt;
    final SoundEvent death;

    MimicKind(double health, double damage, double speed, float width, float height, SoundEvent ambient, SoundEvent hurt, SoundEvent death) {
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.width = width;
        this.height = height;
        this.ambient = ambient;
        this.hurt = hurt;
        this.death = death;
    }

    /** Registry name, e.g. "mimic_villager". */
    public String id() {
        return "mimic_" + name().toLowerCase();
    }
}
