package com.rickandmorty.entity.cruiser;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** An upgrade for the Space Cruiser: use it on the cruiser to fit it. */
public class CruiserUpgradeItem extends Item {
    private final CruiserUpgrade upgrade;

    public CruiserUpgradeItem(CruiserUpgrade upgrade, Properties properties) {
        super(properties);
        this.upgrade = upgrade;
    }

    public CruiserUpgrade upgrade() {
        return upgrade;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.rickrooms." + upgrade.itemId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rickrooms.cruiser_upgrade").withStyle(ChatFormatting.DARK_GRAY));
    }
}
