package com.rickandmorty.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.cronenberg.Mutation;
import com.rickandmorty.cronenberg.MutationClientState;
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
 * Mutation on screen: a row of cells above the hunger bar (in Cronenberg World, or whenever
 * you're mutated at all), and veins creeping in from the edges of the screen as it rises.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MutationHud {
    private static final ResourceLocation ICONS = RickAndMortyMod.id("textures/gui/mutation.png");
    private static final ResourceLocation VEINS = RickAndMortyMod.id("textures/gui/mutation_vignette.png");

    private MutationHud() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, RickAndMortyMod.id("mutation_veins"), MutationHud::renderVeins);
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, RickAndMortyMod.id("mutation"), MutationHud::renderMeter);
    }

    private static boolean active(Minecraft mc) {
        LocalPlayer player = mc.player;
        return player != null && !mc.options.hideGui && !player.isSpectator();
    }

    private static float value(Minecraft mc) {
        return MutationClientState.get(mc.player.getId());
    }

    private static void renderMeter(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc) || mc.player.isCreative()) return;
        float mutation = value(mc);
        if (mutation <= 0.0f && mc.level.dimension() != ModDimensions.CRONENBERG) return;
        int right = g.guiWidth() / 2 + 91;
        int y = g.guiHeight() - mc.gui.rightHeight;
        mc.gui.rightHeight += 10;
        int points = Mth.ceil(mutation / 5.0f);    // 20 half-cells
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - 9;
            int wobble = mutation >= 75 && (mc.player.tickCount + i * 3) % 20 < 3 ? -1 : 0;
            g.blit(ICONS, x, y + wobble, 18, 0, 9, 9, 27, 9);
            if (i * 2 + 1 < points) g.blit(ICONS, x, y + wobble, 0, 0, 9, 9, 27, 9);
            else if (i * 2 + 1 == points) g.blit(ICONS, x, y + wobble, 9, 0, 9, 9, 27, 9);
        }
        RenderSystem.disableBlend();
    }

    private static void renderVeins(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc)) return;
        float mutation = value(mc);
        if (Mutation.stage(mutation) < 1) return;
        float pulse = 0.85f + 0.15f * Mth.sin(mc.player.tickCount * (mutation >= 75 ? 0.25f : 0.1f));
        float alpha = Mth.clamp((mutation - 20.0f) / 80.0f, 0.0f, 1.0f) * pulse;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1.0f, 1.0f, 1.0f, alpha);
        g.blit(VEINS, 0, 0, g.guiWidth(), g.guiHeight(), 0, 0, 256, 256, 256, 256);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
