package com.rickandmorty.sanity;

import com.rickandmorty.ModConfig;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.FluorescentLightBlock;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Things that happen now and then, whatever your sanity: only you hear them, and there's never
 * anything there. Footsteps creeping up behind you, knocking in the walls, your name whispered,
 * the lights dying all around you in Level 0, a splash behind you in the Poolrooms, giggling in
 * Level Fun. Mostly in the Backrooms; very occasionally, on a dark night, at home.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class StrangeEvents {
    private static final int INTERVAL = 400;
    private record Later(long tick, Runnable task) {
    }
    private static final List<Later> LATER = new ArrayList<>();

    public enum Kind { FOOTSTEPS, KNOCKING, WHISPER, LIGHTS_OUT, SPLASH, GIGGLING }

    private StrangeEvents() {
    }

    public static double chance(ServerLevel level) {
        BackroomsLevel here = BackroomsLevel.of(level.dimension());
        if (here == BackroomsLevel.SUNLIT_LOBBY) return 0.0;
        if (here != null) return 0.07;
        return level.dimension() == Level.OVERWORLD && level.isNight() ? 0.01 : 0.0;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % INTERVAL != 0 || !ModConfig.STRANGE_EVENTS.get()) return;
        double chance = chance(level);
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && level.random.nextDouble() < chance) happen(player, pick(level, level.random));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().getTickCount();
        List<Later> due = new ArrayList<>();
        LATER.removeIf(later -> {
            if (later.tick() > now) return false;
            due.add(later);
            return true;
        });
        due.forEach(later -> later.task().run());
    }

    private static void later(MinecraftServer server, int delay, Runnable task) {
        LATER.add(new Later(server.getTickCount() + delay, task));
    }

    private static Kind pick(ServerLevel level, RandomSource random) {
        BackroomsLevel here = BackroomsLevel.of(level.dimension());
        List<Kind> kinds = new ArrayList<>(List.of(Kind.FOOTSTEPS, Kind.KNOCKING, Kind.WHISPER));
        if (here == BackroomsLevel.LEVEL_0) kinds.add(Kind.LIGHTS_OUT);
        if (here == BackroomsLevel.POOLROOMS) kinds.add(Kind.SPLASH);
        if (here == BackroomsLevel.LEVEL_FUN) kinds.add(Kind.GIGGLING);
        return kinds.get(random.nextInt(kinds.size()));
    }

    /** Somewhere behind the player, {@code distance} blocks away. */
    private static Vec3 behind(ServerPlayer player, double distance) {
        return player.position().add(Vec3.directionFromRotation(0.0f, player.getYRot()).scale(-distance));
    }

    /** A sound only this player hears. */
    private static void hear(ServerPlayer player, SoundEvent sound, Vec3 at, float volume, float pitch) {
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        player.connection.send(new ClientboundSoundPacket(holder, SoundSource.AMBIENT, at.x, at.y, at.z, volume, pitch, player.getRandom().nextLong()));
    }

    public static void happen(ServerPlayer player, Kind kind) {
        MinecraftServer server = player.server;
        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();
        switch (kind) {
            case FOOTSTEPS -> {
                // someone walking up behind you, closer each step, and then nothing
                boolean backrooms = BackroomsLevel.isBackrooms(level.dimension());
                SoundEvent step = backrooms ? SoundEvents.WOOL_STEP : SoundEvents.GRASS_STEP;
                for (int i = 0; i < 7; i++) {
                    double distance = 14 - i * 1.7;
                    later(server, i * 9, () -> {
                        if (!player.isRemoved()) hear(player, step, behind(player, distance), 0.9f, 0.8f);
                    });
                }
            }
            case KNOCKING -> {
                Vec3 at = player.position().add(Vec3.directionFromRotation(0.0f, player.getYRot() + (random.nextBoolean() ? 90 : -90)).scale(4));
                for (int i = 0; i < 3; i++) later(server, i * 7, () -> hear(player, SoundEvents.WOOD_HIT, at, 1.0f, 0.7f));
            }
            case WHISPER -> {
                hear(player, SoundEvents.AMBIENT_CAVE.value(), behind(player, 2), 0.5f, 1.4f);
                player.displayClientMessage(Component.literal("... " + player.getGameProfile().getName() + " ...")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
            }
            case LIGHTS_OUT -> {
                // every light around you goes out at once, and comes back a few seconds later
                hear(player, SoundEvents.BEACON_DEACTIVATE, player.position(), 0.8f, 0.6f);
                BlockPos center = player.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-14, -4, -14), center.offset(14, 6, 14))) {
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof FluorescentLightBlock light && state.getValue(FluorescentLightBlock.LIT)
                            && !state.getValue(FluorescentLightBlock.BROKEN)) {
                        level.setBlock(pos, state.setValue(FluorescentLightBlock.LIT, false), Block.UPDATE_ALL);
                        level.scheduleTick(pos.immutable(), light, 70 + random.nextInt(30));
                    }
                }
            }
            case SPLASH -> {
                Vec3 at = behind(player, 12);
                hear(player, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, at, 0.8f, 0.9f);
                level.sendParticles(player, ParticleTypes.SPLASH, true, at.x, player.getY() + 0.2, at.z, 30, 0.6, 0.1, 0.6, 0.2);
            }
            case GIGGLING -> hear(player, SoundEvents.WITCH_CELEBRATE, behind(player, 10), 0.7f, 1.8f);
        }
    }
}
