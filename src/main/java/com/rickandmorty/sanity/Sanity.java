package com.rickandmorty.sanity;

import com.rickandmorty.ModConfig;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.BacteriaEntity;
import com.rickandmorty.entity.MeeseeksEntity;
import com.rickandmorty.entity.MimicEntity;
import com.rickandmorty.network.SanityPayload;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModAttachments;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The sanity meter. It drains while you're in the Backrooms (faster in the dark and with
 * something hostile nearby, slower with a Meeseeks for company) and recovers anywhere else.
 * Low sanity brings hallucinations (client side, see SanityClientEffects), nausea, and at
 * zero it starts to hurt. Almond Water restores it.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class Sanity {
    public static final float MAX = 100.0f;
    /** Seconds to go from full to empty in a lit, quiet room, at drain multiplier 1. */
    private static final float SECONDS_TO_EMPTY = 600.0f;
    private static final float RECOVERY_PER_SECOND = 1.0f;
    public static final ResourceKey<DamageType> INSANITY = ResourceKey.create(Registries.DAMAGE_TYPE, RickAndMortyMod.id("insanity"));

    private Sanity() {
    }

    public static float get(Player player) {
        return player.getData(ModAttachments.SANITY);
    }

    public static void set(ServerPlayer player, float value) {
        player.setData(ModAttachments.SANITY, Mth.clamp(value, 0.0f, MAX));
        // fake players (from other mods) and connections without our channel can't receive it
        if (!(player instanceof FakePlayer) && player.connection != null && player.connection.hasChannel(SanityPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new SanityPayload(get(player)));
        }
    }

    public static void add(ServerPlayer player, float amount) {
        set(player, get(player) + amount);
    }

    /** Change per second (negative drains). */
    public static float ratePerSecond(boolean inBackrooms, boolean dark, boolean threatNearby, boolean friendNearby, double drainMultiplier) {
        if (!inBackrooms) return RECOVERY_PER_SECOND;
        float drain = MAX / SECONDS_TO_EMPTY;
        if (dark) drain *= 2.5f;
        if (threatNearby) drain *= 2.0f;
        if (friendNearby) drain *= 0.5f;
        return (float) (-drain * drainMultiplier);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        float before = get(player);
        if (!ModConfig.SANITY_ENABLED.get() || player.isCreative() || player.isSpectator()) {
            if (before < MAX) set(player, Math.min(MAX, before + RECOVERY_PER_SECOND * 5));
            return;
        }
        ServerLevel level = player.serverLevel();
        var backrooms = com.rickandmorty.worldgen.level.BackroomsLevel.of(level.dimension());
        // every level drains (some faster), except the Sunlit Lobby, where you recover
        if (backrooms != null && backrooms.sanity == 0.0f) return;              // somewhere your mind just... holds still
        boolean inBackrooms = backrooms != null && backrooms.sanity > 0;
        double levelDrain = backrooms == null ? 1.0 : Math.abs(backrooms.sanity);
        boolean dark = level.getMaxLocalRawBrightness(player.blockPosition()) < 5;
        boolean threat = inBackrooms && !level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(12),
                m -> m instanceof BacteriaEntity || (m instanceof MimicEntity mimic && mimic.isAwake())
                        || m instanceof com.rickandmorty.entity.ScarecrowEntity || m instanceof com.rickandmorty.entity.WatcherEntity
                        || m instanceof com.rickandmorty.entity.PartygoerEntity partygoer && partygoer.isHostile()).isEmpty();
        boolean friend = inBackrooms && !level.getEntitiesOfClass(MeeseeksEntity.class, player.getBoundingBox().inflate(8)).isEmpty();
        float after = Mth.clamp(before + ratePerSecond(inBackrooms, dark, threat, friend, ModConfig.SANITY_DRAIN.get() * levelDrain), 0.0f, MAX);
        if (after != before) set(player, after);

        if (after < 20 && player.getRandom().nextInt(15) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120));
        }
        if (after <= 0) {
            ModAdvancements.award(player, "lost_your_mind");
            if (player.tickCount % 100 == 0) {
                player.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(INSANITY)), 2.0f);
                player.displayClientMessage(Component.translatable("message.rickrooms.sanity.breaking").withStyle(ChatFormatting.DARK_RED), true);
            }
        }
    }

    /** Szechuan Sauce is good for the soul. */
    @SubscribeEvent
    public static void onItemEaten(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().is(ModItems.SZECHUAN_SAUCE.get())) {
            add(player, 15.0f);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) set(player, get(player));
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) set(player, get(player));
    }
}
