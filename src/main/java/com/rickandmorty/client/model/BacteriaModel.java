package com.rickandmorty.client.model;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.BacteriaEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A tall, hunched, stick-thin figure: long legs, a narrow torso, an elongated head and arms
 * that hang down to its knees. About 2.6 blocks tall, so it just fits the Backrooms' rooms.
 */
public class BacteriaModel extends HierarchicalModel<BacteriaEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(RickAndMortyMod.id("bacteria"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public BacteriaModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Model y grows downwards; the ground is at y = 24.
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(20, 0).addBox(-3.0f, -14.0f, -1.5f, 6, 14, 3), PartPose.offset(0.0f, 6.0f, 0.0f));
        body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -9.0f, -2.5f, 5, 9, 5), PartPose.offset(0.0f, -14.0f, 0.0f));
        body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 0).addBox(-1.0f, -1.0f, -1.0f, 2, 22, 2), PartPose.offset(-4.0f, -13.0f, 0.0f));
        body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(48, 0).addBox(-1.0f, -1.0f, -1.0f, 2, 22, 2), PartPose.offset(4.0f, -13.0f, 0.0f));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 20).addBox(-1.5f, 0.0f, -1.5f, 3, 18, 3), PartPose.offset(-1.7f, 6.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(12, 20).addBox(-1.5f, 0.0f, -1.5f, 3, 18, 3), PartPose.offset(1.7f, 6.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(BacteriaEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);

        // Hunched forward, head craned up to look at you.
        body.xRot = 0.25f;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD - 0.2f;
        // Twitchy idle: a slow sway with an occasional jerk of the head.
        head.zRot = Mth.sin(ageInTicks * 0.05f) * 0.08f + (Mth.sin(ageInTicks * 0.9f) > 0.97f ? 0.25f : 0.0f);

        // Long, loping stride.
        float stride = Mth.cos(limbSwing * 0.45f) * 1.1f * limbSwingAmount;
        rightLeg.xRot = stride;
        leftLeg.xRot = -stride;
        rightArm.xRot = -stride * 0.7f;
        leftArm.xRot = stride * 0.7f;
        rightArm.zRot = 0.1f + Mth.sin(ageInTicks * 0.07f) * 0.05f;
        leftArm.zRot = -rightArm.zRot;

        // Reaching for its target, and a swipe when attacking.
        if (entity.isAggressive()) {
            rightArm.xRot -= 1.1f;
            leftArm.xRot -= 1.1f;
        }
        if (attackTime > 0.0f) {
            float swipe = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot -= swipe * 1.4f;
            leftArm.xRot -= swipe * 0.8f;
        }
    }
}
