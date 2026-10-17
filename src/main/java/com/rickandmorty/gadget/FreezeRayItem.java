package com.rickandmorty.gadget;

import com.rickandmorty.registry.ModEffects;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * The Freeze Ray. Rick being Rick, it doesn't shoot anything: you hit someone with it, and they
 * freeze solid for a few seconds. Then it needs a moment to cool back down. Well, warm up.
 */
public class FreezeRayItem extends Item {
    public static final int FREEZE_TICKS = 100, COOLDOWN = 40;

    public FreezeRayItem(Properties properties) {
        super(properties.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 3.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build()));
    }

    /** Freezes {@code target}, if the ray's ready. */
    public static boolean freeze(LivingEntity target, LivingEntity attacker, Item item) {
        if (attacker instanceof Player player && player.getCooldowns().isOnCooldown(item)) return false;
        target.addEffect(new MobEffectInstance(ModEffects.FROZEN, FREEZE_TICKS, 0), attacker);
        if (attacker instanceof Player player) player.getCooldowns().addCooldown(item, COOLDOWN);
        if (target.level() instanceof ServerLevel level) {
            level.playSound(null, target.blockPosition(), ModSounds.FREEZE_RAY_HIT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.5), target.getZ(), 30, target.getBbWidth() * 0.5,
                    target.getBbHeight() * 0.4, target.getBbWidth() * 0.5, 0.05);
        }
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        freeze(target, attacker, this);
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rickrooms.freeze_ray").withStyle(ChatFormatting.GRAY));
    }
}
