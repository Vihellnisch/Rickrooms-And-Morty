package com.rickandmorty.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Fuel for the portal gun. Automatically used from the inventory when the gun runs dry. */
public class PortalFluidItem extends Item {
    public PortalFluidItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rickrooms.portal_fluid", PortalGunItem.MAX_CHARGES)
                .withStyle(ChatFormatting.GRAY));
    }
}
