package com.rickandmorty.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.client.model.CronenbergModels;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.ToIntFunction;

/**
 * Lumps of flesh growing on a humanoid as the mutation takes hold: tumours on the back and arm
 * from stage 2, a bulging extra eye and a growth on the head from stage 3, bone spines from stage 4.
 * They pulse gently. Used on players (their synced mutation) and on Cronenberg Rick and Morty.
 */
public class MutationLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/mutation.png");
    private final ModelPart body, head, arm, spikes;
    private final ToIntFunction<T> stage;

    public MutationLayer(RenderLayerParent<T, M> parent, EntityModelSet models, ToIntFunction<T> stage) {
        super(parent);
        ModelPart root = models.bakeLayer(CronenbergModels.MUTATION);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.arm = root.getChild("arm");
        this.spikes = root.getChild("spikes");
        this.stage = stage;
    }

    /** Adds the layer to a renderer that draws a humanoid model. */
    @SuppressWarnings("unchecked")
    public static <T extends LivingEntity, M extends HumanoidModel<T>> void attach(LivingEntityRenderer<T, M> renderer, EntityModelSet models,
                                                                                ToIntFunction<T> stage) {
        renderer.addLayer(new MutationLayer<>(renderer, models, stage));
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        int level = stage.applyAsInt(entity);
        if (level < 2 || entity.isInvisible()) return;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0f);
        float pulse = 1.0f + Mth.sin(ageInTicks * 0.15f) * 0.06f;
        M model = getParentModel();
        draw(pose, consumer, light, overlay, model.body, body, pulse);
        draw(pose, consumer, light, overlay, model.rightArm, arm, pulse);
        if (level >= 3) draw(pose, consumer, light, overlay, model.head, head, pulse);
        if (level >= 4) draw(pose, consumer, light, overlay, model.body, spikes, 1.0f);
    }

    private static void draw(PoseStack pose, VertexConsumer consumer, int light, int overlay, ModelPart parent, ModelPart growth, float pulse) {
        pose.pushPose();
        parent.translateAndRotate(pose);
        growth.xScale = growth.yScale = growth.zScale = pulse;
        growth.render(pose, consumer, light, overlay);
        pose.popPose();
    }
}
