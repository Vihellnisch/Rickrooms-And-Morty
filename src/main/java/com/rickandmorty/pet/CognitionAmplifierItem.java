package com.rickandmorty.pet;

import com.rickandmorty.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Snuffles' helmet: put it on a pet you own (or a fox that trusts you) and it gets smart, see
 * {@link PetBrain}. The modules fitted to it stay with it when it comes off.
 */
public class CognitionAmplifierItem extends Item {
    public CognitionAmplifierItem(Properties properties) {
        super(properties);
    }

    public static int modules(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.AMPLIFIER_MODULES.get(), 0);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rickrooms.cognition_amplifier").withStyle(ChatFormatting.GRAY));
        Component modules = AmplifierModule.list(modules(stack));
        if (modules != null) tooltip.add(Component.translatable("tooltip.rickrooms.cognition_amplifier.modules", modules).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.rickrooms.cognition_amplifier.remove").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return modules(stack) == AmplifierModule.ALL;
    }
}
