package com.rickandmorty.effect;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Pickles climb walls. Runs on both sides because the client moves its own player.
 * Hold sneak to stop climbing and just cling on.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public class PickleRickHandler {
    private static final double CLIMB_SPEED = 0.2;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.hasEffect(ModEffects.PICKLE_RICK) || !player.horizontalCollision || player.getAbilities().flying) return;
        Vec3 motion = player.getDeltaMovement();
        double y = player.isShiftKeyDown() ? Math.max(motion.y, 0.0) : Math.max(motion.y, CLIMB_SPEED);
        player.setDeltaMovement(motion.x, y, motion.z);
        player.resetFallDistance();
    }
}
