package com.rickandmorty.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A plain item with a one-line flavour tooltip ("tooltip.rickrooms.<name>"). */
public class LoreItem extends Item {
    private final String tooltipKey;

    public LoreItem(Properties properties, String name) {
        super(properties);
        this.tooltipKey = "tooltip.rickrooms." + name;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
