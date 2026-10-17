package com.rickandmorty.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Everyone has a plumbus in their home. Squeeze it and it does... something. Usually
 * something good. Sometimes fleeb juice gets everywhere.
 */
public class PlumbusItem extends Item {
    private static final int COOLDOWN = 600;
    private static final List<Holder<MobEffect>> GOOD = List.of(MobEffects.REGENERATION, MobEffects.MOVEMENT_SPEED,
            MobEffects.NIGHT_VISION, MobEffects.JUMP, MobEffects.LUCK, MobEffects.DIG_SPEED, MobEffects.DAMAGE_RESISTANCE);

    public PlumbusItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack plumbus = player.getItemInHand(hand);
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0f, 0.8f);
            if (level.random.nextInt(10) == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160));
                player.displayClientMessage(Component.translatable("message.rickrooms.plumbus.fleeb").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            } else {
                Holder<MobEffect> effect = GOOD.get(level.random.nextInt(GOOD.size()));
                int duration = effect == MobEffects.NIGHT_VISION ? 1800 : 400;
                player.addEffect(new MobEffectInstance(effect, duration));
                player.displayClientMessage(Component.translatable("message.rickrooms.plumbus.used").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            player.getCooldowns().addCooldown(this, COOLDOWN);
        }
        return InteractionResultHolder.sidedSuccess(plumbus, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rickrooms.plumbus.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rickrooms.plumbus.2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
