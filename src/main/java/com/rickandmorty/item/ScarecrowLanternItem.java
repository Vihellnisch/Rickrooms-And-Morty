package com.rickandmorty.item;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A lantern with a scarecrow's head bound inside it. Hold it (in either hand) and the scarecrows
 * of the Harvest won't come for you: they back away from its light instead.
 */
public class ScarecrowLanternItem extends Item {
    /** How far the lantern keeps them off. */
    public static final double RANGE = 12.0;

    public ScarecrowLanternItem(Properties properties) {
        super(properties);
    }

    public static boolean isHeldBy(Player player) {
        return player.getMainHandItem().getItem() instanceof ScarecrowLanternItem || player.getOffhandItem().getItem() instanceof ScarecrowLanternItem;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.rickrooms.scarecrow_lantern.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
