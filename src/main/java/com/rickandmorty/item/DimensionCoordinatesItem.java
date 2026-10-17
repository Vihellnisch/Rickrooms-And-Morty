package com.rickandmorty.item;

import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Deciphered coordinates for one of the gun's special dimensions (the Backrooms, Cronenberg
 * World). Use it while holding a Portal Gun in the other hand to permanently add that dimension
 * to the gun's dial.
 */
public class DimensionCoordinatesItem extends Item {
    private final ResourceKey<Level> dimension;
    private final String name;

    public DimensionCoordinatesItem(Properties properties, ResourceKey<Level> dimension, String name) {
        super(properties);
        this.dimension = dimension;
        this.name = name;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack coordinates = player.getItemInHand(hand);
        ItemStack gun = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(gun.getItem() instanceof PortalGunItem)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.rickrooms.coordinates.need_gun").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.fail(coordinates);
        }
        if (!level.isClientSide) {
            PortalDestinations dial = PortalGunItem.getDestinations(gun);
            Component where = PortalDestinations.dimensionName(dimension);
            if (dial.knows(dimension)) {
                player.displayClientMessage(Component.translatable("message.rickrooms.coordinates.already_known", where).withStyle(ChatFormatting.YELLOW), true);
                return InteractionResultHolder.fail(coordinates);
            }
            gun.set(ModDataComponents.PORTAL_DESTINATIONS.get(), dial.withKnownDimension(dimension));
            coordinates.consume(1, player);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PORTAL_GUN_RELOAD.get(), SoundSource.PLAYERS, 1.0f, 0.6f);
            player.displayClientMessage(Component.translatable("message.rickrooms.coordinates.programmed", where).withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResultHolder.sidedSuccess(coordinates, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rickrooms." + name + ".1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.rickrooms." + name + ".2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
