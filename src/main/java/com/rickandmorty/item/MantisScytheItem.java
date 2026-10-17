package com.rickandmorty.item;

import com.rickandmorty.entity.Gore;
import com.rickandmorty.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A Mantis Person's forearm, serrated edge and all. Hits open wounds that keep bleeding. */
public class MantisScytheItem extends SwordItem {
    public MantisScytheItem(Properties properties) {
        super(Tiers.IRON, properties.attributes(SwordItem.createAttributes(Tiers.IRON, 4, -2.6f)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 80), attacker);
        Gore.wound(target, 4.0f);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rickrooms.mantis_scythe").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
