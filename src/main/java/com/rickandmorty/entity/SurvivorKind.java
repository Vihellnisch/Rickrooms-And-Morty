package com.rickandmorty.entity;

import com.rickandmorty.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * The people still living in Cronenberg World. Jerry, Beth and Summer hold out in their fortified
 * house (the Survivor Camp); Cronenberg Rick and Morty just pass through, now and then.
 */
public enum SurvivorKind {
    /** Turns out the apocalypse is the one thing Jerry is good at. Trades survival gear. */
    JERRY(false, false, () -> Items.IRON_AXE, 5),
    /** Horse surgeon turned field medic. Trades medicine, including the Cure Serum. */
    BETH(true, false, () -> Items.IRON_SWORD, 4),
    /** Handles the weapons. Trades weapons. */
    SUMMER(true, false, () -> Items.CROSSBOW, 4),
    /** From the dimension next door. Shows up, talks, trades something weird, leaves. */
    CRONENBERG_RICK(false, true, () -> ModItems.PORTAL_GUN.get(), 5),
    CRONENBERG_MORTY(false, true, () -> Items.AIR, 4);

    public final boolean slim;
    /** Wanderers roam in, stay a few minutes, and leave through a portal. */
    public final boolean wanderer;
    public final Supplier<Item> held;
    /** How many "npc.rickrooms.<id>.N" chat lines there are. */
    public final int lines;

    SurvivorKind(boolean slim, boolean wanderer, Supplier<Item> held, int lines) {
        this.slim = slim;
        this.wanderer = wanderer;
        this.held = held;
        this.lines = lines;
    }

    public String id() {
        return name().toLowerCase();
    }

    /** Whether they wear the Cronenberg look (lumps and extra eyes). */
    public boolean mutated() {
        return this == CRONENBERG_RICK || this == CRONENBERG_MORTY;
    }
}
