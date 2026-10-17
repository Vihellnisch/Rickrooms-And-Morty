package com.rickandmorty.gadget;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Interdimensional Goggles: wear them, and the ores around you light up through the rock. */
public class GogglesItem extends ArmorItem {
    public static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/models/armor/interdimensional_goggles.png");

    public GogglesItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties);
    }

    /** On as a helmet, or (with Curios) in its head slot over one. */
    public static boolean wearing(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GogglesItem || com.rickandmorty.compat.Curios.wearingGoggles(entity);
    }

    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return TEXTURE;
    }

    /** Night vision while they're on, to see the ores by. */
    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof LivingEntity living && living.getItemBySlot(EquipmentSlot.HEAD) == stack) nightVision(living);
    }

    /** Keeps night vision topped up (server side), for as long as they're worn. */
    public static void nightVision(LivingEntity living) {
        if (living.level().isClientSide) return;
        var current = living.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        if (current == null || current.getDuration() < 220) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rickrooms.interdimensional_goggles").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rickrooms.interdimensional_goggles.toggle",
                Component.keybind("key.rickrooms.goggles_xray")).withStyle(ChatFormatting.DARK_GRAY));
    }
}
