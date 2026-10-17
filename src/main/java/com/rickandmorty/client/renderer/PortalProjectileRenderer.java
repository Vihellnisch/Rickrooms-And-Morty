package com.rickandmorty.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.PortalProjectile;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** A glowing green orb that always faces the camera. */
public class PortalProjectileRenderer extends EntityRenderer<PortalProjectile> {
    private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/portal_projectile.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

    public PortalProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PortalProjectile projectile, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0f, projectile.getBbHeight() / 2, 0.0f);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.6f, 0.6f, 0.6f);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffers.getBuffer(RENDER_TYPE);
        vertex(consumer, pose, -0.5f, -0.5f, 0, 1);
        vertex(consumer, pose, 0.5f, -0.5f, 1, 1);
        vertex(consumer, pose, 0.5f, 0.5f, 1, 0);
        vertex(consumer, pose, -0.5f, 0.5f, 0, 0);
        poseStack.popPose();
        super.render(projectile, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v) {
        consumer.addVertex(pose, x, y, 0.0f)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(PortalProjectile projectile) {
        return TEXTURE;
    }
}
