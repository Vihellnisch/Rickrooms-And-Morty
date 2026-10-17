package com.rickandmorty.client;

import com.rickandmorty.ModConfig;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.BacteriaEntity;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModSounds;
import com.rickandmorty.sanity.SanityClientState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Everything low sanity does to you, all on your own client (nobody else sees it):
 * <ul>
 *   <li>below 60: sounds that aren't there (footsteps behind you, doors, clicking);</li>
 *   <li>below 40: a Bacteria at the end of the hall that vanishes when you look right at it;</li>
 *   <li>below 35: the lights "flicker" (the screen goes dark for a moment);</li>
 *   <li>below 30: whispers;</li>
 *   <li>below 20: jumpscares (can be turned off in the config).</li>
 * </ul>
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID, value = Dist.CLIENT)
public final class SanityClientEffects {
    private static final int WHISPERS = 8;
    public static final int JUMPSCARE_TICKS = 14;

    private static int soundTimer = 300;
    private static int phantomTimer = 600;
    private static int whisperTimer = 600;
    private static int jumpscareTimer = 1200;
    static int flickerTicks;
    static int jumpscareTicks;

    private static Entity phantom;
    private static int phantomAge;
    private static int nextFakeId = -7_000_000;

    private SanityClientEffects() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (flickerTicks > 0) flickerTicks--;
        if (jumpscareTicks > 0) jumpscareTicks--;
        if (player == null || mc.level == null || mc.isPaused()) return;
        tickPhantom(mc, player);
        if (!ModConfig.SANITY_ENABLED.get() || player.isCreative() || player.isSpectator()) return;

        float sanity = SanityClientState.value;
        RandomSource random = player.getRandom();
        if (sanity < 60) {
            if (--soundTimer <= 0) {
                playFakeSound(mc, player, random);
                soundTimer = (int) (300 + random.nextInt(600) * (sanity / 60.0f));
            }
        } else {
            soundTimer = 300;
        }
        if (sanity < 40) {
            if (--phantomTimer <= 0 && phantom == null) {
                spawnPhantom(mc, player, random);
                phantomTimer = 700 + random.nextInt(900);
            }
        } else {
            phantomTimer = 600;
        }
        if (sanity < 35 && random.nextInt(500) == 0) flickerTicks = 3 + random.nextInt(4);
        if (sanity < 30) {
            if (--whisperTimer <= 0) {
                player.displayClientMessage(Component.translatable("whisper.rickrooms." + random.nextInt(WHISPERS))
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
                whisperTimer = 500 + random.nextInt(700);
            }
        } else {
            whisperTimer = 600;
        }
        if (sanity < 20 && ModConfig.JUMPSCARES.get()) {
            if (--jumpscareTimer <= 0) {
                triggerJumpscare();
                jumpscareTimer = 1500 + random.nextInt(2000);
            }
        } else {
            jumpscareTimer = 1200;
        }
    }

    public static void triggerJumpscare() {
        Minecraft mc = Minecraft.getInstance();
        jumpscareTicks = JUMPSCARE_TICKS;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ENDERMAN_STARE, 1.0f, 1.6f));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BACTERIA_HURT.get(), 0.9f, 1.0f));
    }

    private static void playFakeSound(Minecraft mc, LocalPlayer player, RandomSource random) {
        SoundEvent[] sounds = {SoundEvents.STONE_STEP, SoundEvents.STONE_STEP, SoundEvents.WOODEN_DOOR_CLOSE,
                ModSounds.BACTERIA_AMBIENT.get(), ModSounds.BACTERIA_STEP.get(), SoundEvents.AMBIENT_CAVE.value()};
        SoundEvent sound = sounds[random.nextInt(sounds.length)];
        // somewhere behind you
        Vec3 behind = player.getLookAngle().multiply(-1, 0, -1).normalize().scale(3 + random.nextInt(5));
        Vec3 at = player.position().add(behind).add(random.nextGaussian(), 0, random.nextGaussian());
        mc.level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.AMBIENT, 0.8f, 0.8f + random.nextFloat() * 0.3f, false);
    }

    /** A still, staring Bacteria somewhere ahead of you. It only exists on your screen. */
    private static void spawnPhantom(Minecraft mc, LocalPlayer player, RandomSource random) {
        ClientLevel level = mc.level;
        for (int attempt = 0; attempt < 12; attempt++) {
            float yaw = player.getYRot() + (random.nextFloat() - 0.5f) * 100.0f;
            double distance = 9 + random.nextDouble() * 9;
            Vec3 dir = Vec3.directionFromRotation(0, yaw);
            BlockPos feet = BlockPos.containing(player.position().add(dir.scale(distance)));
            if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()
                    || level.getBlockState(feet.below()).isAir()) continue;
            BacteriaEntity fake = ModEntities.BACTERIA.get().create(level);
            if (fake == null) return;
            fake.setId(nextFakeId--);
            fake.setNoAi(true);
            float face = yaw + 180.0f;
            fake.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, face, 0.0f);
            fake.setYHeadRot(face);
            fake.yBodyRot = face;
            level.addEntity(fake);
            phantom = fake;
            phantomAge = 0;
            return;
        }
    }

    /** It disappears the moment you look straight at it, or after a few seconds anyway. */
    private static void tickPhantom(Minecraft mc, LocalPlayer player) {
        if (phantom == null) return;
        phantomAge++;
        Vec3 toPhantom = phantom.position().add(0, 1.5, 0).subtract(player.getEyePosition()).normalize();
        boolean lookedAt = player.getLookAngle().dot(toPhantom) > Mth.cos(10 * Mth.DEG_TO_RAD);
        if ((lookedAt && phantomAge > 8) || phantomAge > 140 || phantom.level() != mc.level) {
            if (lookedAt) mc.level.playLocalSound(phantom.getX(), phantom.getY(), phantom.getZ(),
                    ModSounds.BACTERIA_AMBIENT.get(), SoundSource.HOSTILE, 0.3f, 0.5f, false);
            if (phantom.level() == mc.level) mc.level.removeEntity(phantom.getId(), Entity.RemovalReason.DISCARDED);
            phantom = null;
        }
    }
}
