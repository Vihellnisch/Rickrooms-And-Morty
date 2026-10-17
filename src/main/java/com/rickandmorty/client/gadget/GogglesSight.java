package com.rickandmorty.client.gadget;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.gadget.GogglesItem;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * What you see through the Interdimensional Goggles: put them on and the rock melts away, leaving
 * only the ores (and anything alive) hanging in the dark; the goggles give you night vision to see
 * them by. The edge of the view darkens a little, like looking through lenses. The x-ray key
 * switches the x-ray off and on again; the night vision stays either way.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID, value = Dist.CLIENT)
public final class GogglesSight {
    public static final KeyMapping TOGGLE = new KeyMapping("key.rickrooms.goggles_xray", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.rickrooms");
    /** The wearer's switch: x-ray on (the default) or just the night vision. */
    private static boolean switchedOn = true;

    private GogglesSight() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean wearing = mc.player != null && mc.level != null && GogglesItem.wearing(mc.player);
        while (TOGGLE.consumeClick()) {
            if (!wearing || mc.screen != null) continue;
            switchedOn = !switchedOn;
            mc.player.displayClientMessage(Component.translatable(switchedOn ? "message.rickrooms.goggles.xray_on"
                    : "message.rickrooms.goggles.xray_off").withStyle(switchedOn ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
        }
        boolean xray = wearing && switchedOn;
        if (xray != XRay.active()) {
            XRay.setActive(xray);
            if (mc.levelRenderer != null && mc.level != null) mc.levelRenderer.allChanged();    // rebuild the world with (or without) the rock
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !XRay.active() || !mc.options.getCameraType().isFirstPerson()) return;
        var g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight(), edge = Math.max(12, Math.min(w, h) / 10);
        for (int i = 0; i < edge; i++) {
            int a = (int) (110 * (1.0 - i / (double) edge));
            int c = a << 24 | 0x062818;
            g.fill(i, i, w - i, i + 1, c);
            g.fill(i, h - i - 1, w - i, h - i, c);
            g.fill(i, i + 1, i + 1, h - i - 1, c);
            g.fill(w - i - 1, i + 1, w - i, h - i - 1, c);
        }
    }
}
