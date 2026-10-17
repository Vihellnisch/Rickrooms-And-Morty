package com.rickandmorty.client.model;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.BehemothEntity;
import com.rickandmorty.entity.CronenbergEntity;
import com.rickandmorty.entity.MantisEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Animations for the Cronenberg World creatures (their shapes are in {@link CronenbergLayers}).
 * Everything twitches, breathes and writhes; nothing moves quite like it should.
 */
public final class CronenbergModels {
    public static final ModelLayerLocation CRAWLER = layer("cronenberg_crawler");
    public static final ModelLayerLocation STALKER = layer("cronenberg_stalker");
    public static final ModelLayerLocation BLOB = layer("cronenberg_blob");
    public static final ModelLayerLocation BEHEMOTH = layer("behemoth");
    public static final ModelLayerLocation MANTIS = layer("mantis_person");
    public static final ModelLayerLocation MUTATION = layer("mutation");
    public static final ModelLayerLocation HATS = layer("council_hats");

    private CronenbergModels() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(RickAndMortyMod.id(name), "main");
    }

    /** Base: finds parts by name and resets the pose every frame. */
    abstract static class Base<T extends Entity> extends HierarchicalModel<T> {
        private final ModelPart root;

        Base(ModelPart root) {
            this.root = root;
        }

        @Override
        public ModelPart root() {
            return root;
        }

        ModelPart part(String name) {
            return root.getAllParts().filter(p -> p.hasChild(name)).findFirst().map(p -> p.getChild(name))
                    .orElseThrow(() -> new IllegalStateException("no model part " + name));
        }

        void reset() {
            root.getAllParts().forEach(ModelPart::resetPose);
        }

        /** A sudden jerk now and then, different for every part (seeded by {@code k}). */
        static float twitch(float age, float k) {
            return Mth.sin(age * (1.1f + k * 0.13f) + k * 7.0f) > 0.97f ? 0.35f : 0.0f;
        }
    }

    // ---------------------------------------------------------------------------- Crawler

    public static class Crawler extends Base<CronenbergEntity> {
        private final ModelPart body, jaw, backArm, backFore;
        private final ModelPart[] right = new ModelPart[3], left = new ModelPart[3];

        public Crawler(ModelPart root) {
            super(root);
            body = part("body");
            jaw = part("jaw");
            backArm = part("back_arm");
            backFore = part("back_fore");
            for (int i = 0; i < 3; i++) {
                right[i] = part("right_leg" + i);
                left[i] = part("left_leg" + i);
            }
        }

        @Override
        public void setupAnim(CronenbergEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            reset();
            float scuttle = Math.min(1.0f, amount * 1.5f);
            for (int i = 0; i < 3; i++) {
                float phase = limbSwing * 1.1f + i * 2.1f;
                right[i].yRot = Mth.cos(phase) * 0.55f * scuttle;
                right[i].zRot += Mth.sin(phase) * 0.3f * scuttle + twitch(age, i) * 0.4f;
                left[i].yRot = Mth.cos(phase + Mth.PI) * 0.55f * scuttle;
                left[i].zRot -= Mth.sin(phase + Mth.PI) * 0.3f * scuttle + twitch(age, i + 3) * 0.4f;
            }
            body.zRot = Mth.sin(limbSwing * 1.1f) * 0.05f * scuttle;
            body.y += Mth.sin(age * 0.08f) * 0.3f;
            // the jaw never stops working, and gapes when it bites
            jaw.xRot = 0.45f + Mth.sin(age * 0.35f) * 0.2f + (attackTime > 0 ? Mth.sin(attackTime * Mth.PI) * 0.8f : 0.0f);
            backArm.xRot += Mth.sin(age * 0.09f) * 0.35f + twitch(age, 11);
            backArm.zRot += Mth.cos(age * 0.07f) * 0.25f;
            backFore.xRot += Mth.sin(age * 0.13f + 1.0f) * 0.45f;
        }
    }

    // ---------------------------------------------------------------------------- Stalker

    public static class Stalker extends Base<CronenbergEntity> {
        private final ModelPart rightLeg, leftLeg, torso, head, secondHead, bigArm, bigFore, thinArm;
        private final ModelPart[][] tentacles = new ModelPart[2][3];

        public Stalker(ModelPart root) {
            super(root);
            rightLeg = part("right_leg");
            leftLeg = part("left_leg");
            torso = part("torso");
            head = part("head");
            secondHead = part("second_head");
            bigArm = part("big_arm");
            bigFore = part("big_fore");
            thinArm = part("thin_arm");
            for (int i = 0; i < 2; i++) {
                tentacles[i][0] = part("tentacle" + i + "a");
                tentacles[i][1] = part("tentacle" + i + "b");
                tentacles[i][2] = part("tentacle" + i + "c");
            }
        }

        @Override
        public void setupAnim(CronenbergEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            reset();
            float stride = Mth.cos(limbSwing * 0.55f) * 1.0f * amount;
            rightLeg.xRot = stride;
            leftLeg.xRot = -stride * 0.7f;                      // one leg drags
            torso.zRot += Mth.sin(limbSwing * 0.55f) * 0.08f * amount;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.8f;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            head.zRot += Mth.sin(age * 0.04f) * 0.15f + twitch(age, 1) * 1.2f;
            secondHead.zRot += Mth.sin(age * 0.06f + 2.0f) * 0.2f + twitch(age, 2);
            secondHead.xRot = Mth.sin(age * 0.05f) * 0.3f;
            bigArm.xRot = -stride * 0.3f + Mth.sin(age * 0.05f) * 0.05f;
            thinArm.xRot = stride * 0.5f + Mth.sin(age * 0.07f) * 0.1f;
            thinArm.zRot += twitch(age, 3);
            if (e.isAggressive()) bigArm.xRot -= 0.6f;
            if (attackTime > 0.0f) {
                float swing = Mth.sin(attackTime * Mth.PI);
                bigArm.xRot -= swing * 2.0f;
                bigFore.xRot -= swing * 0.6f;
            }
            for (int i = 0; i < 2; i++) {
                for (int s = 0; s < 3; s++) {
                    tentacles[i][s].xRot += Mth.sin(age * 0.12f + i * 1.7f + s * 0.9f) * 0.35f;
                    tentacles[i][s].zRot += Mth.cos(age * 0.1f + i * 2.3f + s * 0.7f) * 0.25f;
                }
            }
        }
    }

    // ---------------------------------------------------------------------------- Blob

    public static class Blob extends Base<CronenbergEntity> {
        private final ModelPart body, jaw, lumps;
        private final ModelPart[] legs = new ModelPart[6];
        private final ModelPart[][] tentacles = new ModelPart[4][3];

        public Blob(ModelPart root) {
            super(root);
            body = part("body");
            jaw = part("jaw");
            lumps = part("lumps");
            for (int i = 0; i < 6; i++) legs[i] = part("leg" + i);
            for (int i = 0; i < 4; i++) {
                tentacles[i][0] = part("tentacle" + i + "a");
                tentacles[i][1] = part("tentacle" + i + "b");
                tentacles[i][2] = part("tentacle" + i + "c");
            }
        }

        @Override
        public void setupAnim(CronenbergEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            reset();
            // it heaves as it breathes
            float breath = Mth.sin(age * 0.09f);
            body.yScale = 1.0f + breath * 0.04f;
            body.xScale = 1.0f - breath * 0.025f;
            body.zScale = body.xScale;
            lumps.yScale = 1.0f + Mth.sin(age * 0.13f + 1.0f) * 0.08f;
            body.zRot = Mth.sin(limbSwing * 0.8f) * 0.06f * amount;
            jaw.xRot = 0.25f + Mth.sin(age * 0.2f) * 0.2f + (attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0.0f);
            for (int i = 0; i < 6; i++) legs[i].xRot = Mth.cos(limbSwing * 1.3f + i * 1.1f) * 0.8f * amount;
            for (int i = 0; i < 4; i++) {
                for (int s = 0; s < 3; s++) {
                    tentacles[i][s].xRot += Mth.sin(age * 0.1f + i * 1.3f + s * 0.8f) * 0.4f;
                    tentacles[i][s].zRot += Mth.cos(age * 0.08f + i * 2.1f + s * 0.6f) * 0.4f;
                }
            }
        }
    }

    // ---------------------------------------------------------------------------- Behemoth

    public static class Behemoth extends Base<BehemothEntity> {
        private final ModelPart body, head, jaw;
        private final ModelPart[] legs = new ModelPart[6];
        private final ModelPart[][] arms = new ModelPart[4][2];
        private final ModelPart[][] tentacles = new ModelPart[3][3];

        public Behemoth(ModelPart root) {
            super(root);
            body = part("body");
            head = part("head");
            jaw = part("jaw");
            for (int i = 0; i < 6; i++) legs[i] = part("leg" + i);
            for (int i = 0; i < 4; i++) {
                arms[i][0] = part("arm" + i);
                arms[i][1] = part("fore" + i);
            }
            for (int i = 0; i < 3; i++) {
                tentacles[i][0] = part("tentacle" + i + "a");
                tentacles[i][1] = part("tentacle" + i + "b");
                tentacles[i][2] = part("tentacle" + i + "c");
            }
        }

        @Override
        public void setupAnim(BehemothEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            reset();
            boolean awake = e.isAwake();
            float breath = Mth.sin(age * (awake ? 0.1f : 0.04f));
            body.yScale = 1.0f + breath * 0.03f;
            for (int i = 0; i < 6; i++) legs[i].xRot = Mth.cos(limbSwing * 0.7f + (i % 2) * Mth.PI + i * 0.4f) * 0.6f * amount;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.4f;
            if (!awake) {
                // asleep: arms curled up, mouth shut, barely breathing
                for (ModelPart[] arm : arms) {
                    arm[0].xRot = 1.1f;
                    arm[1].xRot = 1.4f;
                }
                jaw.xRot = 0.05f;
                return;
            }
            for (int i = 0; i < 4; i++) {
                arms[i][0].xRot += Mth.sin(age * 0.07f + i * 1.9f) * 0.5f + twitch(age, i);
                arms[i][0].zRot += Mth.cos(age * 0.05f + i) * 0.3f;
                arms[i][1].xRot += Mth.sin(age * 0.11f + i * 0.7f) * 0.6f;
            }
            for (int i = 0; i < 3; i++) {
                for (int s = 0; s < 3; s++) {
                    tentacles[i][s].xRot += Mth.sin(age * 0.09f + i * 1.4f + s) * 0.35f;
                    tentacles[i][s].yRot += Mth.cos(age * 0.07f + i * 2.0f + s * 0.6f) * 0.4f;
                }
            }
            jaw.xRot = 0.35f + Mth.sin(age * 0.15f) * 0.15f + (attackTime > 0 ? Mth.sin(attackTime * Mth.PI) * 0.7f : 0.0f);
            // the slam: rear up, then crash down
            float sinceSlam = e.tickCount - e.lastSlamTick() + (age - Mth.floor(age));
            if (sinceSlam >= 0 && sinceSlam < 16) {
                float lift = sinceSlam < 10 ? sinceSlam / 10.0f : 1.0f - (sinceSlam - 10) / 6.0f;
                body.xRot -= lift * 0.45f;
                for (ModelPart leg : legs) leg.xRot += lift * 0.5f;
                jaw.xRot = 0.9f;
            }
        }
    }

    // ---------------------------------------------------------------------------- Mantis

    public static class Mantis extends Base<MantisEntity> {
        private final ModelPart rightLeg, leftLeg, head, rightArm, leftArm, rightScythe, leftScythe, rightAntenna, leftAntenna;

        public Mantis(ModelPart root) {
            super(root);
            rightLeg = part("right_leg");
            leftLeg = part("left_leg");
            head = part("head");
            rightArm = part("right_arm");
            leftArm = part("left_arm");
            rightScythe = part("right_scythe");
            leftScythe = part("left_scythe");
            rightAntenna = part("right_antenna");
            leftAntenna = part("left_antenna");
        }

        @Override
        public void setupAnim(MantisEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            reset();
            float stride = Mth.cos(limbSwing * 0.66f) * 1.3f * amount;
            rightLeg.xRot = stride;
            leftLeg.xRot = -stride;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            // that mantis head-tilt, in sudden snaps
            head.zRot = (Mth.sin(age * 0.05f) > 0.6f ? 0.45f : Mth.sin(age * 0.05f) < -0.6f ? -0.45f : 0.0f) + twitch(age, 4) * 0.5f;
            rightAntenna.xRot += Mth.sin(age * 0.3f) * 0.2f;
            leftAntenna.xRot += Mth.sin(age * 0.3f + 1.0f) * 0.2f;
            rightArm.xRot += Mth.sin(age * 0.06f) * 0.05f;
            leftArm.xRot += Mth.sin(age * 0.06f + 0.5f) * 0.05f;
            if (e.isAggressive()) {
                rightArm.xRot -= 0.4f;
                leftArm.xRot -= 0.4f;
                rightScythe.xRot += 0.6f + Mth.sin(age * 0.5f) * 0.15f;
                leftScythe.xRot += 0.6f + Mth.sin(age * 0.5f + 1.5f) * 0.15f;
            }
            if (attackTime > 0.0f) {
                float strike = Mth.sin(attackTime * Mth.PI);
                rightArm.xRot -= strike * 0.8f;
                leftArm.xRot -= strike * 0.6f;
                rightScythe.xRot += strike * 2.2f;
                leftScythe.xRot += strike * 1.8f;
            }
        }
    }
}
