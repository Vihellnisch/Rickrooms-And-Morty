package com.rickandmorty.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.MeeseeksEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Mr. Meeseeks: a tall, skinny, very blue humanoid. */
public class MeeseeksRenderer extends HumanoidMobRenderer<MeeseeksEntity, HumanoidModel<MeeseeksEntity>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(RickAndMortyMod.id("meeseeks"), "main");
    private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/meeseeks.png");

    public MeeseeksRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(LAYER)), 0.4f);
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f), 64, 32);
    }

    @Override
    protected void scale(MeeseeksEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.85f, 1.08f, 0.85f);
    }

    /** Meeseeks are always bright and cheerful, even in a dark room. */
    @Override
    protected int getBlockLightLevel(MeeseeksEntity entity, BlockPos pos) {
        return Math.max(12, super.getBlockLightLevel(entity, pos));
    }

    @Override
    public ResourceLocation getTextureLocation(MeeseeksEntity entity) {
        return TEXTURE;
    }
}
