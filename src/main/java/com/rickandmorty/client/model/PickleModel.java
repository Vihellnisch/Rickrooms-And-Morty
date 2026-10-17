package com.rickandmorty.client.model;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.player.Player;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Pickle Rick: a bumpy green pickle, one block tall before scaling, with Rick's face on the front. */
public class PickleModel extends EntityModel<Player> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(RickAndMortyMod.id("pickle"), "main");
    private final ModelPart pickle;

    public PickleModel(ModelPart root) {
        this.pickle = root.getChild("pickle");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // ground is at y = 24; a rounded 6x12x6 body with narrower tips, standing upright
        root.addOrReplaceChild("pickle", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0f, -14.0f, -3.0f, 6, 12, 6)
                .texOffs(0, 18).addBox(-2.0f, -16.0f, -2.0f, 4, 2, 4)
                .texOffs(16, 18).addBox(-2.0f, -2.0f, -2.0f, 4, 2, 4)
                .texOffs(24, 0).addBox(-3.5f, -11.0f, -1.0f, 1, 3, 2)     // bumps
                .texOffs(24, 5).addBox(2.5f, -7.0f, -1.0f, 1, 3, 2),
                PartPose.offset(0.0f, 24.0f, 0.0f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** Wobbles as it hops along. */
    @Override
    public void setupAnim(Player player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        pickle.zRot = net.minecraft.util.Mth.sin(limbSwing * 0.8f) * 0.25f * limbSwingAmount;
        pickle.y = 24.0f - Math.abs(net.minecraft.util.Mth.sin(limbSwing * 0.8f)) * 2.0f * limbSwingAmount;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        pickle.render(poseStack, buffer, light, overlay, color);
    }
}
