package com.rickandmorty.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rickandmorty.entity.MimicEntity;
import com.rickandmorty.entity.MimicKind;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renders a "Still Life" with the real vanilla model and texture, but wrong: stretched out of
 * proportion, drained of colour, heads bent or turned backwards, and trembling once awake.
 */
public class MimicRenderer extends MobRenderer<MimicEntity, EntityModel<MimicEntity>> {
    private final ResourceLocation texture;
    private final float scaleX, scaleY, scaleZ;

    private MimicRenderer(EntityRendererProvider.Context context, EntityModel<MimicEntity> model, float shadow,
                          String texture, float scaleX, float scaleY, float scaleZ) {
        super(context, model, shadow);
        this.texture = ResourceLocation.withDefaultNamespace("textures/entity/" + texture + ".png");
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }

    @Override
    protected void scale(MimicEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(scaleX, scaleY, scaleZ);
    }

    @Override
    protected boolean isShaking(MimicEntity entity) {
        return entity.isAwake();
    }

    @Override
    public ResourceLocation getTextureLocation(MimicEntity entity) {
        return texture;
    }

    /** A creepy little twitch, the same for every model. */
    private static float twitch(float ageInTicks) {
        return Mth.sin(ageInTicks * 1.3f) > 0.96f ? 0.4f : 0.0f;
    }

    public static EntityRendererProvider<MimicEntity> provider(MimicKind kind) {
        return context -> switch (kind) {
            case VILLAGER -> new MimicRenderer(context, new VillagerModel<MimicEntity>(context.bakeLayer(ModelLayers.VILLAGER)) {
                @Override
                public void setupAnim(MimicEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
                    super.setupAnim(e, limbSwing, amount, age, yaw, pitch);
                    getHead().zRot = 0.55f + twitch(age);                    // head lolling on its shoulder
                    if (e.isAwake() && Mth.sin(age * 0.05f) > 0.7f) getHead().yRot += Mth.PI;  // ...and sometimes spun round
                }

                @Override
                public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
                    super.renderToBuffer(pose, buffer, light, overlay, 0xFFB5C2AC);
                }
            }, 0.4f, "villager/villager", 0.85f, 1.15f, 0.85f);
            case SHEEP -> new MimicRenderer(context, new QuadrupedModel<MimicEntity>(context.bakeLayer(ModelLayers.SHEEP), false, 8.0f, 4.0f, 2.0f, 2.0f, 24) {
                @Override
                public void setupAnim(MimicEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
                    super.setupAnim(e, limbSwing, amount, age, yaw, pitch);
                    head.zRot = 1.1f + twitch(age);                          // broken neck
                }

                @Override
                public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
                    super.renderToBuffer(pose, buffer, light, overlay, 0xFFA9A69E);
                }
            }, 0.5f, "sheep/sheep", 0.85f, 1.25f, 1.3f);
            case PIG -> new MimicRenderer(context, new PigModel<MimicEntity>(context.bakeLayer(ModelLayers.PIG)) {
                @Override
                public void setupAnim(MimicEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
                    super.setupAnim(e, limbSwing, amount, age, yaw, pitch);
                    head.yRot += Mth.PI;                                     // face on backwards
                    head.zRot = twitch(age);
                }

                @Override
                public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
                    super.renderToBuffer(pose, buffer, light, overlay, 0xFFC9A3A0);
                }
            }, 0.5f, "pig/pig", 1.1f, 0.85f, 1.5f);
            case COW -> new MimicRenderer(context, new CowModel<MimicEntity>(context.bakeLayer(ModelLayers.COW)) {
                @Override
                public void setupAnim(MimicEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
                    super.setupAnim(e, limbSwing, amount, age, yaw, pitch);
                    head.xRot = -1.0f;                                       // staring at the ceiling
                    head.zRot = twitch(age);
                }

                @Override
                public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
                    super.renderToBuffer(pose, buffer, light, overlay, 0xFF8A8078);
                }
            }, 0.6f, "cow/cow", 0.85f, 1.25f, 1.0f);
            case CHICKEN -> new MimicRenderer(context, new ChickenModel<MimicEntity>(context.bakeLayer(ModelLayers.CHICKEN)) {
                @Override
                public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
                    super.renderToBuffer(pose, buffer, light, overlay, 0xFFB4B0AA);
                }
            }, 0.3f, "chicken", 0.75f, 1.6f, 0.75f);                         // an unsettlingly tall chicken
            case ZOMBIE -> new MimicRenderer(context, new HumanoidModel<MimicEntity>(context.bakeLayer(ModelLayers.ZOMBIE)) {
                @Override
                public void setupAnim(MimicEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
                    super.setupAnim(e, limbSwing, amount, age, yaw, pitch);
                    AnimationUtils.animateZombieArms(leftArm, rightArm, e.isAwake(), attackTime, age);
                    head.zRot = -0.45f + twitch(age);
                }

                @Override
                public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
                    super.renderToBuffer(pose, buffer, light, overlay, 0xFF74836F);
                }
            }, 0.4f, "zombie/zombie", 0.85f, 1.2f, 0.85f);
        };
    }
}
