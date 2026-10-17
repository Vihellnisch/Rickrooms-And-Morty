package com.rickandmorty.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.client.model.CruiserLayers;
import com.rickandmorty.entity.cruiser.SpaceCruiserEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws Rick's Space Cruiser: the hull and everything on it from the generated model, the
 * headlights and exhaust glowing on top, and the dark glass dome, drawn here as a hemisphere so
 * the seats (and whoever's in them) show through.
 */
public class SpaceCruiserRenderer extends EntityRenderer<SpaceCruiserEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(RickAndMortyMod.id("space_cruiser"), "main");
    private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/space_cruiser.png");
    private static final ResourceLocation GLOW = RickAndMortyMod.id("textures/entity/space_cruiser_glow.png");
    private static final ResourceLocation DOME = RickAndMortyMod.id("textures/entity/space_cruiser_dome.png");
    private static final float DOME_RADIUS = 12.6f / 16.0f, DOME_BASE = 3.8f / 16.0f;
    /** The model is built a little small; the cruiser is the size of a car. */
    public static final float SCALE = 1.2f;
    private static final int SEGMENTS = 20, RINGS = 8;
    /** How much of the glass's tint is left when you look out from inside (of 255). */
    private static final int INSIDE_ALPHA = 60;

    private final ModelPart model;

    public SpaceCruiserRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = context.bakeLayer(LAYER);
        this.shadowRadius = 1.3f;
    }

    public static net.minecraft.client.model.geom.builders.LayerDefinition layer() {
        return CruiserLayers.cruiser();
    }

    @Override
    public void render(SpaceCruiserEntity cruiser, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.scale(SCALE, SCALE, SCALE);
        float time = cruiser.tickCount + partialTick;
        if (!cruiser.onGround() && cruiser.power() > 0) pose.translate(0.0, Mth.sin(time * 0.08f) * 0.04, 0.0);   // hovering
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - Mth.rotLerp(partialTick, cruiser.yRotO, cruiser.getYRot())));
        float hurt = cruiser.hurtTime() - partialTick;
        if (hurt > 0) pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(hurt) * hurt * 0.6f));
        // tilt a little into the direction it's moving
        var v = cruiser.getDeltaMovement();
        var local = v.yRot(cruiser.getYRot() * Mth.DEG_TO_RAD);
        pose.mulPose(Axis.XP.rotationDegrees((float) Mth.clamp(local.z * 12.0, -10.0, 10.0)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) Mth.clamp(-local.x * 12.0, -10.0, 10.0)));
        pose.scale(-1.0f, -1.0f, 1.0f);
        pose.translate(0.0f, -1.501f, 0.0f);
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        model.render(pose, buffers.getBuffer(RenderType.eyes(GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        dome(pose, buffers.getBuffer(CruiserGlassRenderType.glass(DOME)), light, cameraInside(cruiser, partialTick));
        pose.popPose();
        super.render(cruiser, yaw, partialTick, pose, buffers, light);
    }

    /** Whether you're looking out from the seats (first person, under the dome). */
    private static boolean cameraInside(SpaceCruiserEntity cruiser, float partialTick) {
        var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera.isDetached()) return false;
        var centre = cruiser.getPosition(partialTick).add(0.0, (1.501 - DOME_BASE) * SCALE, 0.0);
        var offset = camera.getPosition().subtract(centre);
        return offset.y > -0.3 && offset.length() < DOME_RADIUS * SCALE + 0.1;
    }

    /**
     * A hemisphere of glass on the collar. Seen from outside, every quad is drawn twice, once facing
     * out and once in, each wound the right way for its side, so it looks right (and shader packs
     * light it right) from any angle. From the seats only the inner side is drawn, and only as a
     * faint tint, so it doesn't dim the view (shader packs made the panels dark and faceted).
     */
    private static void dome(PoseStack pose, VertexConsumer consumer, int light, boolean inside) {
        int alpha = inside ? INSIDE_ALPHA : 255;
        PoseStack.Pose last = pose.last();
        for (int ring = 0; ring < RINGS; ring++) {
            float a0 = ring / (float) RINGS * Mth.HALF_PI, a1 = (ring + 1) / (float) RINGS * Mth.HALF_PI;
            for (int seg = 0; seg < SEGMENTS; seg++) {
                float t0 = seg / (float) SEGMENTS * Mth.TWO_PI, t1 = (seg + 1) / (float) SEGMENTS * Mth.TWO_PI;
                float u0 = seg / (float) SEGMENTS, u1 = (seg + 1) / (float) SEGMENTS, v0 = 1.0f - ring / (float) RINGS, v1 = 1.0f - (ring + 1) / (float) RINGS;
                float[][] quad = {{t0, a0, u0, v0}, {t1, a0, u1, v0}, {t1, a1, u1, v1}, {t0, a1, u0, v1}};
                for (int side = inside ? 1 : 0; side < 2; side++) {
                    for (int k = 0; k < 4; k++) {
                        float[] p = quad[side == 0 ? k : 3 - k];
                        float nx = Mth.sin(p[0]) * Mth.cos(p[1]), ny = -Mth.sin(p[1]), nz = Mth.cos(p[0]) * Mth.cos(p[1]);
                        float s = side == 0 ? 1.0f : -1.0f;
                        consumer.addVertex(last, nx * DOME_RADIUS, DOME_BASE + ny * DOME_RADIUS, nz * DOME_RADIUS)
                                .setColor(255, 255, 255, alpha)
                                .setUv(p[2], p[3])
                                .setOverlay(OverlayTexture.NO_OVERLAY)
                                .setLight(light)
                                .setNormal(last, nx * s, ny * s, nz * s);
                    }
                }
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(SpaceCruiserEntity entity) {
        return TEXTURE;
    }
}
