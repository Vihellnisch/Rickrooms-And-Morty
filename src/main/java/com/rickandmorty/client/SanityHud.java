package com.rickandmorty.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rickandmorty.ModConfig;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.sanity.Sanity;
import com.rickandmorty.sanity.SanityClientState;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Sanity on screen: a row of eyes above the hunger bar (shown in the Backrooms, or whenever
 * you're not at full sanity), a vignette that closes in as it drops, flickers, and jumpscares.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SanityHud {
    private static final ResourceLocation ICONS = RickAndMortyMod.id("textures/gui/sanity.png");
    private static final ResourceLocation VIGNETTE = RickAndMortyMod.id("textures/gui/sanity_vignette.png");
    private static final ResourceLocation JUMPSCARE = RickAndMortyMod.id("textures/gui/jumpscare.png");

    private SanityHud() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, RickAndMortyMod.id("sanity_vignette"), SanityHud::renderVignette);
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, RickAndMortyMod.id("sanity"), SanityHud::renderMeter);
        event.registerAboveAll(RickAndMortyMod.id("jumpscare"), SanityHud::renderJumpscare);
    }

    private static boolean active(Minecraft mc) {
        LocalPlayer player = mc.player;
        return player != null && !mc.options.hideGui && ModConfig.SANITY_ENABLED.get() && !player.isCreative() && !player.isSpectator();
    }

    private static void renderMeter(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc)) return;
        float sanity = SanityClientState.value;
        boolean inBackrooms = com.rickandmorty.worldgen.level.BackroomsLevel.isBackrooms(mc.level.dimension());
        if (!inBackrooms && sanity >= Sanity.MAX) return;
        int right = g.guiWidth() / 2 + 91;
        int y = g.guiHeight() - mc.gui.rightHeight;
        mc.gui.rightHeight += 10;
        int points = Mth.ceil(sanity / 5.0f);  // 20 half-eyes
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - 9;
            int shake = sanity < 20 && mc.player.tickCount % 3 == 0 ? mc.player.getRandom().nextInt(2) : 0;
            g.blit(ICONS, x, y + shake, 18, 0, 9, 9, 27, 9);                       // empty eye
            if (i * 2 + 1 < points) g.blit(ICONS, x, y + shake, 0, 0, 9, 9, 27, 9);       // full
            else if (i * 2 + 1 == points) g.blit(ICONS, x, y + shake, 9, 0, 9, 9, 27, 9); // half
        }
        RenderSystem.disableBlend();
    }

    private static void renderVignette(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc)) return;
        float sanity = SanityClientState.value;
        int w = g.guiWidth(), h = g.guiHeight();
        if (sanity < 50) {
            float pulse = 0.85f + 0.15f * Mth.sin(mc.player.tickCount * 0.1f);
            float alpha = Mth.clamp((50 - sanity) / 50.0f, 0.0f, 1.0f) * 0.85f * pulse;
            float red = sanity < 20 ? 0.6f : 0.0f;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();   // the vanilla vignette before us leaves an odd blend mode on
            g.setColor(1.0f, 1.0f - red, 1.0f - red, alpha);
            g.blit(VIGNETTE, 0, 0, w, h, 0, 0, 256, 256, 256, 256);
            g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
        if (SanityClientEffects.flickerTicks > 0) {
            g.fill(0, 0, w, h, 0xC8000000);
        }
    }

    private static void renderJumpscare(GuiGraphics g, DeltaTracker delta) {
        if (SanityClientEffects.jumpscareTicks <= 0) return;
        float t = SanityClientEffects.jumpscareTicks / (float) SanityClientEffects.JUMPSCARE_TICKS;
        int w = g.guiWidth(), h = g.guiHeight();
        // lunges at the screen: starts big and fades
        float grow = 1.0f + (1.0f - t) * 0.3f;
        int size = (int) (Math.max(w, h) * grow);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.fill(0, 0, w, h, ((int) (t * 200) << 24));
        g.setColor(1.0f, 1.0f, 1.0f, Mth.clamp(t * 1.5f, 0.0f, 1.0f));
        g.blit(JUMPSCARE, (w - size) / 2, (h - size) / 2, size, size, 0, 0, 256, 256, 256, 256);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
