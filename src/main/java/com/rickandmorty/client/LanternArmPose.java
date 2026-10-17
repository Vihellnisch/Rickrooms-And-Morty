package com.rickandmorty.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * The arm pose for holding a Scarecrow Lantern (seen in third person, by you and by others): arm
 * held out in front, the lantern hanging from the fist. Added to vanilla's ArmPose enum through
 * META-INF/enumextensions.json.
 */
public final class LanternArmPose {
    public static final EnumProxy<HumanoidModel.ArmPose> PARAMETERS = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) LanternArmPose::apply);

    private LanternArmPose() {
    }

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float side = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
        part.xRot = -1.3f + model.head.xRot * 0.35f;
        part.yRot = -side * 0.12f + model.head.yRot * 0.35f;
        part.zRot = 0.0f;
    }

    public static HumanoidModel.ArmPose pose() {
        return PARAMETERS.getValue();
    }
}
