package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client half of {@link DevTour}: a screenshot near the end of each stop (GUI hidden, like F1),
 * third person from the front for stops ending in "_front", then quits.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID, value = Dist.CLIENT)
public class DevTourClient {
    private static final int SHOT_DELAY = 115;
    /** The last stop we took a screenshot of, and the last one we set the camera up for. */
    private static int shot = -1, prepared = -1, screenTicks;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!DevTour.ENABLED || mc.player == null || mc.level == null) return;
        // follow the (integrated) server's clock, so a slow client (shader compiling, dimension
        // changes) can't drift out of step with the stops
        int sinceFirst = DevTour.ticks - DevTour.FIRST_STOP;
        if (sinceFirst < 0) return;
        int stop = sinceFirst / DevTour.STOP_LENGTH, within = sinceFirst % DevTour.STOP_LENGTH;
        if (stop >= DevTour.STOP_NAMES.size()) {
            mc.stop();
            return;
        }
        String name = DevTour.STOP_NAMES.get(stop);
        mc.options.hideGui = !name.endsWith("_hand");   // like pressing F1: no HUD or hand in the screenshots (unless the hand is the point)
        if (prepared != stop && within >= 5) {
            prepared = stop;
            boolean front = name.endsWith("_front");
            mc.options.setCameraType(front ? CameraType.THIRD_PERSON_FRONT : name.endsWith("_back") ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);
            mc.options.fov().set(front ? 40 : 70);                     // zoom in on yourself
        }
        // screens like books pause the game (and the server's clock with it), so count client ticks while one is open
        if (name.endsWith("_screen") && mc.screen != null && shot != stop && ++screenTicks >= 25) {
            shot = stop;
            screenTicks = 0;
            Screenshot.grab(mc.gameDirectory, "tour_" + String.format("%02d", stop) + "_" + name + ".png", mc.getMainRenderTarget(), message -> {});
            mc.setScreen(null);
            return;
        }
        if (name.endsWith("_screen") && within >= 60 && mc.screen == null && shot != stop) {
            if (name.contains("guide")) com.rickandmorty.client.ClientHooks.openFieldGuide();
            else mc.setScreen(new com.rickandmorty.client.screen.PortalGunScreen(net.minecraft.world.InteractionHand.MAIN_HAND));
        }
        if (shot != stop && within >= SHOT_DELAY) {
            shot = stop;
            Screenshot.grab(mc.gameDirectory, "tour_" + String.format("%02d", stop) + "_" + name + ".png", mc.getMainRenderTarget(), message -> {});
            if (name.endsWith("_screen")) mc.setScreen(null);
        }
    }
}
