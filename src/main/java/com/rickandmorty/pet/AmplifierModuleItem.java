package com.rickandmorty.pet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A module for the Cognition Amplifier: use it on a pet wearing one (see {@link PetEvents}). */
public class AmplifierModuleItem extends Item {
    private final AmplifierModule module;

    public AmplifierModuleItem(Properties properties, AmplifierModule module) {
        super(properties);
        this.module = module;
    }

    public AmplifierModule module() {
        return module;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rickrooms." + module.id() + "_module").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rickrooms.module_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
