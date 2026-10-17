package com.rickandmorty.item;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The DNA Lock Upgrade: a locked Portal Gun isn't dropped when its owner dies. It's taken out of
 * the death drops (first, before grave mods see them), kept on the dead player, and handed back
 * to the respawned one. Kept in the player's saved data, so quitting on the death screen is safe.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class DnaLock {
    private static final String KEY = "rickrooms:dna_locked";

    private DnaLock() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) keep(player, event.getDrops());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.Clone event) {
        if (event.isWasDeath()) giveBack(event.getOriginal(), event.getEntity());
    }

    /** Takes the locked guns out of {@code drops} and stores them on the player. Returns how many. */
    public static int keep(Player player, Collection<ItemEntity> drops) {
        List<ItemStack> kept = new ArrayList<>();
        drops.removeIf(drop -> {
            ItemStack stack = drop.getItem();
            if (!stack.is(ModItems.PORTAL_GUN.get()) || !PortalGunItem.hasDnaLock(stack)) return false;
            kept.add(stack.copy());
            drop.discard();
            return true;
        });
        if (kept.isEmpty()) return 0;
        HolderLookup.Provider registries = player.registryAccess();
        CompoundTag data = player.getPersistentData();
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        for (ItemStack stack : kept) list.add(stack.save(registries));
        data.put(KEY, list);
        return kept.size();
    }

    /** Hands the stored guns from the dead player to the respawned one. */
    public static void giveBack(Player original, Player respawned) {
        CompoundTag data = original.getPersistentData();
        if (!data.contains(KEY)) return;
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        data.remove(KEY);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(respawned.registryAccess(), list.getCompound(i));
            if (stack.isEmpty()) continue;
            if (!respawned.getInventory().add(stack)) respawned.drop(stack, false);
        }
    }
}
