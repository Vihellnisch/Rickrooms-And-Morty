package com.rickandmorty.client;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** The v0.23 levels' looks: their skies, the white-out inside a cloud, and Run For Your Life's flickering red. */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID, value = Dist.CLIENT)
public final class SkyLevelClient {
    private SkyLevelClient() {
    }

    private static boolean inCloud() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        var camera = mc.gameRenderer.getMainCamera();
        return mc.level.getBlockState(camera.getBlockPosition()).is(com.rickandmorty.registry.ModBlocks.CLOUD.get());
    }

    /** Run For Your Life: everything washed in the red of the EXIT signs, and the red flickers. Drawn over the world, under the HUD. */
    @SubscribeEvent
    public static void redWash(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != com.rickandmorty.worldgen.level.BackroomsLevel.RUN_FOR_YOUR_LIFE.key()) return;
        long t = mc.level.getGameTime();
        float alpha = (t / 3 * 2654435761L & 0xFF) < 28 ? 0.22f : 0.36f;
        var window = mc.getWindow();
        float w = window.getGuiScaledWidth(), h = window.getGuiScaledHeight();
        com.mojang.blaze3d.systems.RenderSystem.backupProjectionMatrix();
        com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0, w, h, 0, 1000, 3000),
                com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
        var modelView = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        modelView.translate(0, 0, -2000);
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        var buffer = com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(0, h, 0).setColor(0.75f, 0.0f, 0.04f, alpha);
        buffer.addVertex(w, h, 0).setColor(0.75f, 0.0f, 0.04f, alpha);
        buffer.addVertex(w, 0, 0).setColor(0.75f, 0.0f, 0.04f, alpha);
        buffer.addVertex(0, 0, 0).setColor(0.75f, 0.0f, 0.04f, alpha);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.buildOrThrow());
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        modelView.popMatrix();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        com.mojang.blaze3d.systems.RenderSystem.restoreProjectionMatrix();
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog event) {
        if (!inCloud()) return;
        event.setNearPlaneDistance(0.0f);
        event.setFarPlaneDistance(2.5f);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor event) {
        if (!inCloud()) return;
        event.setRed(0.93f);
        event.setGreen(0.95f);
        event.setBlue(0.98f);
    }

    /** The Facade's sky: overcast, no sun to see, and fog close in, so the walls fade out up, down and along. */
    public static class GreySky extends DimensionSpecialEffects {
        public GreySky() {
            super(Float.NaN, false, SkyType.NONE, false, true);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
            return fogColor;
        }

        @Override
        public boolean isFoggyAt(int x, int z) {
            return true;
        }
    }

    /** Drop Limit's sky: the Overworld's sun in a deep blue sky, and no clouds up there: they're all below you. */
    public static class HighSky extends DimensionSpecialEffects {
        public HighSky() {
            this(Float.NaN);
        }

        /** The same, with clouds at {@code clouds} (NaN: none). */
        public HighSky(float clouds) {
            super(clouds, true, SkyType.NORMAL, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
            return fogColor.multiply(brightness * 0.94f + 0.06f, brightness * 0.94f + 0.06f, brightness * 0.91f + 0.09f);
        }

        @Override
        public boolean isFoggyAt(int x, int z) {
            return false;
        }
    }
}
