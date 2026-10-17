package com.rickandmorty.item;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.client.ClientHooks;
import com.rickandmorty.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Rick's Field Guide: a battered notebook explaining (in his words) everything in this mod.
 * Everyone gets one the first time they join a world; you can also make one from a book and
 * some lime dye.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public class FieldGuideItem extends Item {
    /** How many pages there are (guide.rickrooms.page.0 .. PAGES-1). */
    public static final int PAGES = 32;
    private static final String GIVEN = RickAndMortyMod.MOD_ID + ":got_field_guide";

    public FieldGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) ClientHooks.openFieldGuide();
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @SubscribeEvent
    public static void onFirstJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(GIVEN)) return;
        data.putBoolean(GIVEN, true);
        ItemStack guide = new ItemStack(ModItems.FIELD_GUIDE.get());
        if (!player.getInventory().add(guide)) player.drop(guide, false);
    }
}
