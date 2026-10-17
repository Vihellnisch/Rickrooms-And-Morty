package com.rickandmorty.client.renderer;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.client.model.StaffLayers;
import com.rickandmorty.entity.StaffEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** The Infinite IKEA's staff: three kinds of faceless, long-armed, yellow-shirted wrongness. */
public class StaffRenderer extends MobRenderer<StaffEntity, StaffRenderer.Model> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(RickAndMortyMod.id("staff"), "main");
    private static final ResourceLocation[] TEXTURES = {
            RickAndMortyMod.id("textures/entity/staff_0.png"), RickAndMortyMod.id("textures/entity/staff_1.png"),
            RickAndMortyMod.id("textures/entity/staff_2.png")};

    public StaffRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(LAYER)), 0.6f);
    }

    public static LayerDefinition layer() {
        return StaffLayers.staff();
    }

    @Override
    public ResourceLocation getTextureLocation(StaffEntity entity) {
        return TEXTURES[entity.variant()];
    }

    public static class Model extends HierarchicalModel<StaffEntity> {
        private final ModelPart root, body, neck, rightLeg, leftLeg;
        private final ModelPart[] heads = new ModelPart[3];
        private final ModelPart[] arms = new ModelPart[2], forearms = new ModelPart[2], hands = new ModelPart[2], armSpikes = new ModelPart[4];
        private final ModelPart[][] fingers = new ModelPart[2][5];

        public Model(ModelPart root) {
            this.root = root;
            this.body = root.getChild("body");
            this.neck = body.getChild("neck");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
            heads[0] = neck.getChild("head_lumpy");
            heads[1] = neck.getChild("head_smooth");
            heads[2] = neck.getChild("head_spiky");
            String[] sides = {"right", "left"};
            for (int s = 0; s < 2; s++) {
                arms[s] = body.getChild(sides[s] + "_arm");
                forearms[s] = arms[s].getChild(sides[s] + "_forearm");
                hands[s] = forearms[s].getChild(sides[s] + "_hand");
                armSpikes[s * 2] = arms[s].getChild(sides[s] + "_arm_spikes");
                armSpikes[s * 2 + 1] = forearms[s].getChild(sides[s] + "_forearm_spikes");
                for (int f = 0; f < 4; f++) fingers[s][f] = hands[s].getChild(sides[s] + "_finger" + f);
                fingers[s][4] = hands[s].getChild(sides[s] + "_thumb");
            }
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void prepareMobModel(StaffEntity entity, float limbSwing, float amount, float partialTick) {
            int variant = entity.variant();
            for (int i = 0; i < heads.length; i++) heads[i].visible = i == variant;
            for (ModelPart spikes : armSpikes) spikes.visible = variant == 2;
        }

        @Override
        public void setupAnim(StaffEntity entity, float limbSwing, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            boolean hunting = entity.isAggressive();
            float walk = limbSwing * 0.55f;
            rightLeg.xRot = Mth.cos(walk) * 0.9f * amount;
            leftLeg.xRot = Mth.cos(walk + Mth.PI) * 0.9f * amount;
            body.xRot = (hunting ? 0.32f : 0.14f) + Mth.sin(age * 0.04f) * 0.02f;
            neck.yRot = yaw * Mth.DEG_TO_RAD * 0.8f;
            neck.xRot = pitch * Mth.DEG_TO_RAD * 0.5f - body.xRot * 0.5f;
            if (entity.variant() == 1) neck.zRot = 0.28f;                    // the head tilted, like it's listening
            if (entity.variant() == 0) neck.zRot = -0.12f;
            for (int s = 0; s < 2; s++) {
                float side = s == 0 ? 1.0f : -1.0f;
                float swing = Mth.cos(walk + (s == 0 ? Mth.PI : 0.0f)) * 0.45f * amount;
                if (hunting) {
                    // reaching out for you, swaying
                    arms[s].xRot = -1.35f + Mth.sin(age * 0.25f + s) * 0.12f - attackTime * 1.2f;
                    arms[s].zRot = side * 0.12f;
                    forearms[s].xRot = -0.25f;
                } else {
                    arms[s].xRot = swing - 0.05f + Mth.sin(age * 0.05f + s * 1.3f) * 0.04f;
                    arms[s].zRot = side * (0.08f + Mth.sin(age * 0.03f) * 0.02f);
                    forearms[s].xRot = -0.18f;
                }
                for (int f = 0; f < 4; f++) {
                    fingers[s][f].xRot = hunting ? -0.05f : -0.35f + f * 0.05f;
                    fingers[s][f].zRot = side * (f - 1.5f) * (hunting ? 0.22f : 0.08f);
                }
                fingers[s][4].zRot = side * (hunting ? 0.9f : 0.5f);
            }
        }
    }
}
