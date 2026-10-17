package com.rickandmorty.client.gadget;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Interdimensional Goggles as worn: a deep green visor across the eyes with a grey cap at each
 * side (an orange button on the right one), held on by a dark strap round the head. Only the head
 * has anything on it. The texture's laid out to match tools/generate_gadgets.py.
 */
public class GogglesModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(RickAndMortyMod.id("interdimensional_goggles"), "main");

    public GogglesModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5f, -6.6f, -5.4f, 9, 4, 2)                                     // the visor
                .texOffs(0, 8).addBox(-4.0f, -5.6f, -4.0f, 8, 2, 8, new CubeDeformation(0.6f))          // the strap
                .texOffs(24, 0).addBox(-5.6f, -6.2f, -4.8f, 2, 3, 3)                                     // the side caps
                .texOffs(24, 0).addBox(3.6f, -6.2f, -4.8f, 2, 3, 3)
                .texOffs(36, 0).addBox(5.5f, -5.2f, -3.9f, 1, 1, 1),                                     // the button
                PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
}
