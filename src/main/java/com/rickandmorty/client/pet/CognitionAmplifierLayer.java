package com.rickandmorty.client.pet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rickandmorty.pet.AmplifierClientState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

/** The helmet on any pet the server says is wearing one. Added to every living thing's renderer. */
public class CognitionAmplifierLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public CognitionAmplifierLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void attach(LivingEntityRenderer<?, ?> renderer) {
        ((LivingEntityRenderer) renderer).addLayer(new CognitionAmplifierLayer(renderer));
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        int modules = AmplifierClientState.get(entity.getId());
        if (modules < 0 || entity.isInvisible()) return;
        AmplifierHelmet.drawOn(getParentModel(), entity, pose, buffers, light, LivingEntityRenderer.getOverlayCoords(entity, 0.0f), modules);
    }
}
