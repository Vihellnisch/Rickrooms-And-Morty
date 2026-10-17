package com.rickandmorty.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.PortalEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the portal like the show's: a round green vortex with a bright rim.
 * It pops open and shrinks closed.
 */
public class PortalRenderer extends EntityRenderer<PortalEntity> {
    private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/portal.png");
    private static final Layers GREEN = new Layers("portal");
    private static final Layers GOLD = new Layers("portal_gold");
    private static final Layers RED = new Layers("portal_red");

    /**
     * The textures of one portal colour. The disc writes depth so things behind it stay hidden.
     * With a shader pack on, the disc is drawn alone as an ordinary cutout texture instead, since
     * shader packs turn the see-through glowing layers black.
     */
    private record Layers(RenderType base, RenderType swirl, RenderType glow, RenderType shaderBase) {
        Layers(String name) {
            this(glowType(RickAndMortyMod.id("textures/entity/" + name + ".png"), true),
                    glowType(RickAndMortyMod.id("textures/entity/" + name + "_swirl.png"), false),
                    glowType(RickAndMortyMod.id("textures/entity/" + name + "_glow.png"), false),
                    RenderType.entityCutout(RickAndMortyMod.id("textures/entity/" + name + ".png")));
        }
    }

    /**
     * Like entityTranslucentEmissive, but one-sided (every layer is drawn once per side, see
     * {@link #drawLayer}); the disc also writes to the depth buffer, so water, particles and other
     * see-through things drawn later can't show through the portal.
     */
    private static RenderType glowType(ResourceLocation texture, boolean writesDepth) {
        return RenderType.create(writesDepth ? "rickrooms_portal" : "rickrooms_portal_glow", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 1536, true, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.CULL)
                        .setWriteMaskState(writesDepth ? RenderStateShard.COLOR_DEPTH_WRITE : RenderStateShard.COLOR_WRITE)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .createCompositeState(false));
    }

    public PortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PortalEntity portal, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        float open = portal.getOpenness(partialTick);
        if (portal.isBlender() && portal.getPhase() != PortalEntity.IDLE) {
            BlenderArms.render(portal, partialTick, poseStack, buffers, packedLight);
        }
        if (open > 0.0f) {
            float scale = easeOutBack(open);
            poseStack.pushPose();
            // Local +Z becomes the portal's facing direction, local +Y its long axis.
            poseStack.mulPose(Axis.YP.rotationDegrees(-portal.getPortalYaw()));
            if (portal.isFlat()) {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            }
            poseStack.scale(PortalEntity.WIDTH / 2 * scale, PortalEntity.HEIGHT / 2 * scale, 1.0f);
            renderVortex(poseStack, buffers, portal.tickCount + partialTick, portal.isGold());
            poseStack.popPose();
        }
        super.render(portal, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    /**
     * Draws a portal on the -1..1 square of the XY plane: a soft glow halo, the green disc with
     * its bright wobbly rim, and a second spiral turning faster on top so it looks like it's
     * pulling inwards. The whole thing breathes slightly. Shared with the Warp Gateway.
     * Gold is the rare Evil Morty colour.
     */
    public static void renderVortex(PoseStack poseStack, MultiBufferSource buffers, float time, boolean gold) {
        renderVortex(poseStack, buffers, time, gold ? GOLD : GREEN);
    }

    /** The blood-red vortex of an evil Warp Gateway that has sprung. */
    public static void renderRedVortex(PoseStack poseStack, MultiBufferSource buffers, float time) {
        renderVortex(poseStack, buffers, time, RED);
    }

    private static void renderVortex(PoseStack poseStack, MultiBufferSource buffers, float time, Layers layers) {
        float pulse = 1.0f + 0.025f * Mth.sin(time * 0.25f);
        poseStack.pushPose();
        poseStack.scale(pulse, pulse, 1.0f);
        if (com.rickandmorty.client.ShaderCompat.shaderPackInUse()) {
            drawLayer(poseStack, buffers.getBuffer(layers.shaderBase()), time * 3.0f, 0.0f, 1.0f, 255);
            poseStack.popPose();
            return;
        }
        drawLayer(poseStack, buffers.getBuffer(layers.glow()), time * 0.5f, -0.01f, 1.3f, 120);
        drawLayer(poseStack, buffers.getBuffer(layers.base()), time * 3.0f, 0.0f, 1.0f, 255);
        drawLayer(poseStack, buffers.getBuffer(layers.swirl()), time * 7.0f, 0.01f, 0.92f, 255);
        poseStack.popPose();
    }

    /**
     * Draws one spinning swirl layer on a -1..1 quad in the XY plane: once facing each way, each
     * wound the right way round for its side (the render types cull whichever faces away). Shader
     * packs work out which way a face points from its winding, so a single two-sided quad came
     * out black from one side.
     */
    public static void drawLayer(PoseStack poseStack, VertexConsumer consumer, float rotation, float offset, float size, int alpha) {
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, offset);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        poseStack.scale(size, size, 1.0f);
        PoseStack.Pose pose = poseStack.last();
        // front (+Z)
        vertex(consumer, pose, -1, -1, 0, 1, alpha, 1.0f);
        vertex(consumer, pose, 1, -1, 1, 1, alpha, 1.0f);
        vertex(consumer, pose, 1, 1, 1, 0, alpha, 1.0f);
        vertex(consumer, pose, -1, 1, 0, 0, alpha, 1.0f);
        // back (-Z): the same texture, seen from behind
        vertex(consumer, pose, -1, 1, 0, 0, alpha, -1.0f);
        vertex(consumer, pose, 1, 1, 1, 0, alpha, -1.0f);
        vertex(consumer, pose, 1, -1, 1, 1, alpha, -1.0f);
        vertex(consumer, pose, -1, -1, 0, 1, alpha, -1.0f);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v, int alpha, float normalZ) {
        consumer.addVertex(pose, x, y, 0.0f)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0f, 0.0f, normalZ);
    }

    /** Overshoots slightly past 1 before settling, which gives the portal a "pop". */
    private static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float t = x - 1.0f;
        return 1.0f + c3 * t * t * t + c1 * t * t;
    }

    @Override
    public ResourceLocation getTextureLocation(PortalEntity portal) {
        return TEXTURE;
    }
}
