package com.rickandmorty.sanity;

import com.rickandmorty.ModConfig;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Set;

/**
 * "If you're not careful and you noclip out of reality in the wrong areas..." Every fall that
 * would hurt you has a tiny chance (config: noclip.chance) to drop you through the world into
 * the Backrooms instead. Can be switched off with noclip.enabled.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class NoclipHandler {
    private NoclipHandler() {
    }

    public static boolean rollsNoclip(double roll) {
        return ModConfig.NOCLIP_ENABLED.get() && roll < ModConfig.NOCLIP_CHANCE.get();
    }

    @SubscribeEvent
    public static void onFallDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getSource().is(DamageTypeTags.IS_FALL)) return;
        if (com.rickandmorty.worldgen.level.BackroomsLevel.isBackrooms(player.level().dimension()) || !rollsNoclip(player.getRandom().nextDouble())) return;
        ServerLevel backrooms = player.server.getLevel(ModDimensions.BACKROOMS);
        var destination = ModDimensions.randomDestination(player.server, ModDimensions.BACKROOMS, player.getRandom());
        if (backrooms == null || destination.isEmpty()) return;

        event.setCanceled(true);
        // Change dimension on the next tick, not in the middle of the fall-damage calculation.
        player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
            var dest = destination.get();
            player.teleportTo(backrooms, dest.x(), dest.y(), dest.z(), Set.of(), dest.yaw(), 0.0f);
            player.resetFallDistance();
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120));
            backrooms.playSound(null, dest.x(), dest.y(), dest.z(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.4f);
            player.displayClientMessage(Component.translatable("message.rickrooms.noclip").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC), false);
            ModAdvancements.award(player, "noclip");
        }));
    }
}
