package com.rickandmorty.effect;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * "I turned myself into a pickle, Morty!" You're tiny, quick, and can climb walls
 * (see PickleRickHandler). The renderer swaps your player model for a pickle.
 */
public class PickleRickEffect extends MobEffect {
    public PickleRickEffect() {
        super(MobEffectCategory.NEUTRAL, 0x5E9E2F);
        addAttributeModifier(Attributes.SCALE, RickAndMortyMod.id("pickle_rick_size"), -0.7, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, RickAndMortyMod.id("pickle_rick_speed"), 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addAttributeModifier(Attributes.SAFE_FALL_DISTANCE, RickAndMortyMod.id("pickle_rick_fall"), 6.0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 1.0f, 1.4f);
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.rickrooms.pickle_rick").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
        }
    }
}
