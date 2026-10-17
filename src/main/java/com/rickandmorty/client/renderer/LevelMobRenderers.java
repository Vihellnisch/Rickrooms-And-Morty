package com.rickandmorty.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.client.model.CronenbergModels;
import com.rickandmorty.entity.ScarecrowEntity;
import com.rickandmorty.entity.WatcherEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renderers for the scarecrows of the Harvest and the Watcher of the wooden levels. */
public final class LevelMobRenderers {
    private LevelMobRenderers() {
    }

    /** Arms out, legs together, head on one side, and a black hat. When it moves, it doesn't walk: it glides. */
    public static class Scarecrow extends HumanoidMobRenderer<ScarecrowEntity, PlayerModel<ScarecrowEntity>> {
        private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/scarecrow.png");
        private static final ResourceLocation HAT = RickAndMortyMod.id("textures/entity/council_hats.png");

        public Scarecrow(EntityRendererProvider.Context context) {
            super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false) {
                @Override
                public void setupAnim(ScarecrowEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
                    super.setupAnim(e, 0.0f, 0.0f, age, e.isWatched() ? 0.0f : yaw, 0.0f);
                    rightArm.xRot = 0.0f;
                    leftArm.xRot = 0.0f;
                    rightArm.zRot = Mth.HALF_PI - 0.08f;
                    leftArm.zRot = -Mth.HALF_PI + 0.08f;
                    rightLeg.xRot = 0.0f;
                    leftLeg.xRot = 0.0f;
                    head.zRot = 0.35f;
                    if (!e.isWatched() && attackTime > 0) {
                        rightArm.zRot -= Mth.sin(attackTime * Mth.PI) * 1.2f;
                        leftArm.zRot += Mth.sin(attackTime * Mth.PI) * 1.2f;
                    }
                    for (ModelPart[] pair : new ModelPart[][]{{rightSleeve, rightArm}, {leftSleeve, leftArm}, {rightPants, rightLeg}, {leftPants, leftLeg}, {hat, head}}) {
                        pair[0].copyFrom(pair[1]);
                    }
                }
            }, 0.5f);
            ModelPart topHat = context.bakeLayer(CronenbergModels.HATS).getChild("top_hat");
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, ScarecrowEntity e, float limbSwing, float amount,
                                   float partialTick, float age, float yaw, float pitch) {
                    pose.pushPose();
                    getParentModel().head.translateAndRotate(pose);
                    topHat.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(HAT)), light, LivingEntityRenderer.getOverlayCoords(e, 0.0f));
                    pose.popPose();
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(ScarecrowEntity entity) {
            return TEXTURE;
        }
    }

    /** A plain figure with blank, glowing white eyes. */
    public static class Watcher extends HumanoidMobRenderer<WatcherEntity, PlayerModel<WatcherEntity>> {
        private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/watcher.png");
        private static final RenderType EYES = RenderType.eyes(RickAndMortyMod.id("textures/entity/watcher_eyes.png"));

        public Watcher(EntityRendererProvider.Context context) {
            super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return EYES;
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(WatcherEntity entity) {
            return TEXTURE;
        }
    }
    /** The friendly one: arms out on its invisible post, swaying a little, a stitched smile. */
    public static class FarmScarecrow extends HumanoidMobRenderer<com.rickandmorty.entity.FarmScarecrowEntity, PlayerModel<com.rickandmorty.entity.FarmScarecrowEntity>> {
        private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/farm_scarecrow.png");

        public FarmScarecrow(EntityRendererProvider.Context context) {
            super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false) {
                @Override
                public void setupAnim(com.rickandmorty.entity.FarmScarecrowEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
                    super.setupAnim(e, limbSwing, amount, age, yaw, pitch);
                    float sway = Mth.sin(age * 0.05f) * 0.06f;
                    rightArm.xRot = 0.0f;
                    leftArm.xRot = 0.0f;
                    rightArm.zRot = Mth.HALF_PI - 0.12f + sway;
                    leftArm.zRot = -Mth.HALF_PI + 0.12f + sway;
                    head.zRot = 0.18f + sway;
                    for (ModelPart[] pair : new ModelPart[][]{{rightSleeve, rightArm}, {leftSleeve, leftArm}, {rightPants, rightLeg}, {leftPants, leftLeg}, {hat, head}}) {
                        pair[0].copyFrom(pair[1]);
                    }
                }
            }, 0.5f);
        }

        @Override
        public ResourceLocation getTextureLocation(com.rickandmorty.entity.FarmScarecrowEntity entity) {
            return TEXTURE;
        }
    }

    /** A Partygoer: a big yellow smile, and a different one once it stops pretending. */
    public static class Partygoer extends HumanoidMobRenderer<com.rickandmorty.entity.PartygoerEntity, PlayerModel<com.rickandmorty.entity.PartygoerEntity>> {
        private static final ResourceLocation FRIENDLY = RickAndMortyMod.id("textures/entity/partygoer.png");
        private static final ResourceLocation HOSTILE = RickAndMortyMod.id("textures/entity/partygoer_hostile.png");

        public Partygoer(EntityRendererProvider.Context context) {
            super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false) {
                @Override
                public void setupAnim(com.rickandmorty.entity.PartygoerEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
                    super.setupAnim(e, limbSwing, amount, age, yaw, pitch);
                    if (e.isHostile()) {
                        // arms out, reaching for you
                        rightArm.xRot = -Mth.HALF_PI + Mth.sin(age * 0.6f) * 0.08f;
                        leftArm.xRot = -Mth.HALF_PI - Mth.sin(age * 0.6f) * 0.08f;
                        head.zRot = Mth.sin(age * 0.9f) * 0.12f;
                    } else {
                        // a little wave now and then
                        if (Mth.sin(age * 0.08f) > 0.6f) rightArm.zRot = 2.6f + Mth.sin(age * 0.8f) * 0.3f;
                        head.zRot = Mth.sin(age * 0.1f) * 0.08f;
                    }
                    for (ModelPart[] pair : new ModelPart[][]{{rightSleeve, rightArm}, {leftSleeve, leftArm}, {hat, head}}) pair[0].copyFrom(pair[1]);
                }
            }, 0.5f);
        }

        @Override
        public ResourceLocation getTextureLocation(com.rickandmorty.entity.PartygoerEntity entity) {
            return entity.isHostile() ? HOSTILE : FRIENDLY;
        }
    }
}
