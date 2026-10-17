package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.PortalTeleporter;
import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * How you get around the Backrooms, like in the stories: fall out of the bottom of one level
 * into the next, step through a red exit door, or walk into the Hole. Every level you reach is
 * recorded by any Portal Gun you carry that already knows the way to the Backrooms.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class LevelTravel {
    /** Where the Hole (and other true noclips) can take you. */
    private static final List<BackroomsLevel> NOCLIP = List.of(BackroomsLevel.LEVEL_1, BackroomsLevel.LEVEL_4, BackroomsLevel.LEVEL_188,
            BackroomsLevel.HARVEST, BackroomsLevel.WOODEN_HOTEL, BackroomsLevel.POOLROOMS, BackroomsLevel.LEVEL_FUN, BackroomsLevel.INFINITE_IKEA,
            BackroomsLevel.DEAD_MALL, BackroomsLevel.ENDLESS_FIELD, BackroomsLevel.ABANDONED_CITY, BackroomsLevel.DROP_LIMIT, BackroomsLevel.FACADE,
            BackroomsLevel.STAIRCASE, BackroomsLevel.FROOPYLAND, BackroomsLevel.RUN_FOR_YOUR_LIFE);

    private LevelTravel() {
    }

    /** Sends a player to a random spot in a level. Returns false if that level doesn't exist here. */
    public static boolean sendTo(ServerPlayer player, BackroomsLevel level) {
        ServerLevel target = player.server.getLevel(level.key());
        if (target == null) return false;
        var destination = ModDimensions.randomDestination(player.server, level.key(), player.getRandom());
        if (destination.isEmpty()) return false;
        var d = destination.get();
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        PortalTeleporter.teleport(player, player.serverLevel(), target, d.position(), d.yaw());
        player.displayClientMessage(Component.translatable("level.rickrooms." + level.getSerializedName()).withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** An exit door: to its level, or (null) home to your bed or the world spawn. */
    public static void throughExitDoor(ServerPlayer player, BlockPos door, @Nullable BackroomsLevel destination) {
        if (player.isOnPortalCooldown()) return;
        ServerLevel here = player.serverLevel();
        here.playSound(null, door, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 0.8f);
        if (destination != null) {
            sendTo(player, destination);
            return;
        }
        ServerLevel home = player.server.getLevel(player.getRespawnDimension());
        BlockPos spot = player.getRespawnPosition();
        if (home == null || spot == null) {
            home = player.server.overworld();
            spot = home.getSharedSpawnPos();
        }
        home.getChunkAt(spot);
        int y = home.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spot.getX(), spot.getZ());
        Vec3 arrive = spot.getY() > home.getMinBuildHeight() && home.getBlockState(spot).isAir() ? Vec3.atBottomCenterOf(spot)
                : new Vec3(spot.getX() + 0.5, y, spot.getZ() + 0.5);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        PortalTeleporter.teleport(player, here, home, arrive, player.getYRot());
        if (BackroomsLevel.isBackrooms(here.dimension())) ModAdvancements.award(player, "the_way_out");
        if (here.dimension().equals(BackroomsLevel.INFINITE_IKEA.key())) ModAdvancements.award(player, "thank_you_for_shopping");
        player.displayClientMessage(Component.translatable("message.rickrooms.exit_door.home").withStyle(ChatFormatting.GREEN), true);
    }

    /**
     * The red door in the Overworld's Monolith: into Level 0, and out of the door at the foot of
     * one of the monoliths there, as if you'd come through it.
     */
    public static void throughMonolithDoor(ServerPlayer player, BlockPos door) {
        if (player.isOnPortalCooldown()) return;
        ServerLevel target = player.server.getLevel(BackroomsLevel.LEVEL_0.key());
        if (target == null) return;
        player.level().playSound(null, door, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 0.6f);
        Vec3 arrive = com.rickandmorty.worldgen.BackroomsZones.monolithArrival(target.getSeed(), player.getRandom());
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        PortalTeleporter.teleport(player, player.serverLevel(), target, arrive, 180.0f);
        player.displayClientMessage(Component.translatable("level.rickrooms.level_0").withStyle(ChatFormatting.GOLD), true);
    }

    /** The drain at the bottom of the Poolrooms' abyss. */
    public static void throughTheDrain(ServerPlayer player) {
        if (player.isOnPortalCooldown()) return;
        if (sendTo(player, com.rickandmorty.block.PoolDrainBlock.destination())) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 1.0f, 0.6f);
        }
    }

    /** The Hole in the wall on Level 0. */
    public static void throughTheHole(ServerPlayer player) {
        if (player.isOnPortalCooldown()) return;
        BackroomsLevel level = NOCLIP.get(player.getRandom().nextInt(NOCLIP.size()));
        if (sendTo(player, level)) {
            ModAdvancements.award(player, "the_hole");
            player.level().playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, 0.4f);
        }
    }

    /** Falling out of the bottom of a level takes you to the next one down. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        Level level = player.level();
        BackroomsLevel here = BackroomsLevel.of(level.dimension());
        if (here == null || player.getY() >= level.getMinBuildHeight() - 8) return;
        player.resetFallDistance();
        if (!sendTo(player, here.fallTarget())) {
            // no such level on this server: back up on solid ground rather than into the void
            var back = ModDimensions.randomDestination(player.server, level.dimension(), player.getRandom());
            back.ifPresent(d -> player.teleportTo(d.x(), d.y(), d.z()));
        }
    }

    /** Guns that know the Backrooms record every level you reach, for next time. */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BackroomsLevel level = BackroomsLevel.of(event.getTo());
        if (level == null || level == BackroomsLevel.LEVEL_0) return;
        boolean learned = false;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!(stack.getItem() instanceof PortalGunItem)) continue;
            PortalDestinations dial = PortalGunItem.getDestinations(stack);
            if (!dial.knows(ModDimensions.BACKROOMS) || dial.knows(level.key())) continue;
            stack.set(ModDataComponents.PORTAL_DESTINATIONS.get(), dial.withLearnedDimension(level.key()));
            learned = true;
        }
        if (learned) {
            player.sendSystemMessage(Component.translatable("message.rickrooms.level_recorded",
                    Component.translatable("level.rickrooms." + level.getSerializedName())).withStyle(ChatFormatting.AQUA));
        }
    }
}
