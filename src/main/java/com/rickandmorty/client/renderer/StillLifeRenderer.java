package com.rickandmorty.client.renderer;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rickandmorty.entity.StillLifeEntity;
import com.rickandmorty.entity.StillLifeForm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A Still Life of a person: a player model in a player's skin, bent out of true. The Captain is
 * stretched tall and thin with a long face and moves like stop-motion; Three Faces has two more
 * heads pressed into the sides of its own; Twice is squat and wide with a second face sliding over
 * the first; the Folded one walks bent double on hands and feet, head on upside down; Many Hands
 * has arms growing out of its sides, holding on.
 */
public class StillLifeRenderer extends LivingEntityRenderer<StillLifeEntity, PlayerModel<StillLifeEntity>> {
    private final FormModel wide, slim;

    public StillLifeRenderer(EntityRendererProvider.Context context) {
        super(context, new FormModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.wide = (FormModel) model;
        this.slim = new FormModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new ExtraParts(this));
    }

    private static PlayerSkin skin(StillLifeEntity entity) {
        GameProfile profile = entity.profile();
        if (profile == null) return DefaultPlayerSkin.get(entity.getUUID());
        return Minecraft.getInstance().getSkinManager().getInsecureSkin(profile);
    }

    @Override
    public void render(StillLifeEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        model = skin(entity).model() == PlayerSkin.Model.SLIM ? slim : wide;
        ((FormModel) model).form = entity.form();
        if (entity.form() == StillLifeForm.CAPTAIN) {
            // stop-motion: no smoothing between ticks, it just jumps from one pose to the next
            pose.pushPose();
            pose.translate(entity.getX() - Mth.lerp(partialTick, entity.xo, entity.getX()), entity.getY() - Mth.lerp(partialTick, entity.yo, entity.getY()),
                    entity.getZ() - Mth.lerp(partialTick, entity.zo, entity.getZ()));
            super.render(entity, entity.getYRot(), 1.0f, pose, buffers, light);
            pose.popPose();
            return;
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    protected void scale(StillLifeEntity entity, PoseStack pose, float partialTick) {
        float base = 0.9375f;                   // the same shrink the player renderer applies
        switch (entity.form()) {
            case CAPTAIN -> pose.scale(base * 0.82f, base * 1.5f, base * 0.82f);
            case TWICE -> pose.scale(base * 1.25f, base * 0.82f, base * 1.2f);
            default -> pose.scale(base, base, base);
        }
    }

    @Override
    protected boolean isShaking(StillLifeEntity entity) {
        return entity.isAwake() && entity.form() != StillLifeForm.CAPTAIN;
    }

    @Override
    protected boolean shouldShowName(StillLifeEntity entity) {
        return entity.hasCustomName() && super.shouldShowName(entity);
    }

    @Override
    public ResourceLocation getTextureLocation(StillLifeEntity entity) {
        return skin(entity).texture();
    }

    // ------------------------------------------------------------------ the bent model

    static class FormModel extends PlayerModel<StillLifeEntity> {
        StillLifeForm form = StillLifeForm.THREE_FACES;

        FormModel(ModelPart root, boolean slim) {
            super(root, slim);
        }

        private Iterable<ModelPart> everything() {
            return java.util.List.of(head, hat, body, jacket, rightArm, leftArm, rightSleeve, leftSleeve, rightLeg, leftLeg, rightPants, leftPants);
        }

        @Override
        public void setupAnim(StillLifeEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            for (ModelPart part : everything()) {
                part.resetPose();
                part.xScale = part.yScale = part.zScale = 1.0f;
                part.visible = true;
            }
            if (form == StillLifeForm.CAPTAIN) {
                age = Mth.floor(age / 3.0f) * 3.0f;               // fewer frames
                limbSwing = Mth.floor(limbSwing * 1.5f) / 1.5f;
            }
            super.setupAnim(e, limbSwing, amount, age, yaw, pitch);
            boolean awake = e.isAwake();
            int step = Mth.floor(age / 7.0f) + e.getId() * 13;
            switch (form) {
                case CAPTAIN -> captain(awake, step, age);
                case THREE_FACES -> {
                    head.xScale = 1.2f;
                    head.zRot = 0.12f + (awake ? Mth.sin(age * 0.4f) * 0.08f : 0.0f);
                }
                case TWICE -> {
                    head.xScale = head.zScale = 1.15f;
                    rightArm.zRot += 0.15f;
                    leftArm.zRot -= 0.15f;
                }
                case FOLDED -> folded(limbSwing, amount, age, awake);
                case COMMUTER -> commuter(age, awake);
                case LOPSIDED -> {
                    head.xScale = 1.35f;
                    head.yScale = 0.85f;
                    head.zRot = -0.1f;
                    body.zRot = 0.08f;
                    rightArm.yScale = 1.8f;
                    rightArm.xScale = rightArm.zScale = 1.15f;
                    rightArm.zRot = 0.1f;
                    rightArm.xRot *= 0.3f;
                    leftArm.yScale = 0.7f;
                    if (awake) rightArm.xRot = -0.5f + Mth.sin(age * 0.2f) * 0.3f;
                }
                case NOODLE_ARMS -> {
                    // seen from the front you get its back, and its face looking at you over it
                    for (ModelPart part : new ModelPart[]{body, rightLeg, leftLeg}) part.yRot += Mth.PI;
                    rightLeg.x = 1.9f;
                    leftLeg.x = -1.9f;
                    rightArm.visible = leftArm.visible = false;
                    head.zRot = 0.15f;
                }
                case PLANK_FACE -> {
                    head.xScale = 2.6f;
                    head.yScale = 0.9f;
                    head.zRot = 0.22f;
                    rightArm.setRotation(-2.5f, -0.2f, 0.35f);
                    leftArm.setRotation(-2.3f, 0.2f, -0.25f);
                }
                case GAPING -> head.visible = false;           // two halves of it are drawn instead
                case MANY_HANDS -> {
                    head.zRot = 0.4f + Mth.sin(age * 0.03f) * 0.05f;
                    if (!awake) {
                        // its own arms folded across itself, holding on
                        rightArm.xRot = -0.9f;
                        rightArm.yRot = -0.6f;
                        leftArm.xRot = -0.9f;
                        leftArm.yRot = 0.6f;
                    }
                }
            }
            hat.copyFrom(head);
            hat.visible = head.visible;
            rightSleeve.visible = rightArm.visible;
            leftSleeve.visible = leftArm.visible;
            jacket.copyFrom(body);
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
            rightPants.copyFrom(rightLeg);
            leftPants.copyFrom(leftLeg);
        }

        /** Hunched under a hump, its head lying sideways in its chest, one long arm dragging. */
        private void commuter(float age, boolean awake) {
            body.xRot = 0.55f;
            body.y = 3.2f;
            head.setPos(-2.5f, 8.5f, -4.2f);
            head.setRotation(0.25f, head.yRot * 0.3f, 1.35f + (awake ? Mth.sin(age * 0.3f) * 0.1f : 0.0f));
            rightArm.setPos(-5, 5.2f, -1);
            leftArm.setPos(5, 5.2f, -1);
            rightArm.yScale = 1.7f;
            rightArm.xRot = -0.25f + rightArm.xRot * 0.3f;
            leftArm.xRot = -0.1f + leftArm.xRot * 0.3f;
            for (ModelPart leg : new ModelPart[]{rightLeg, leftLeg}) {
                leg.z = 4.0f;
                leg.y = 12.2f;
            }
        }

        /** Tall, thin, long-faced, its head snapping from one angle to the next. */
        private void captain(boolean awake, int step, float age) {
            head.xScale = 0.78f;
            head.yScale = 1.6f;
            head.zScale = 0.85f;
            body.xScale = body.zScale = 0.8f;
            for (ModelPart arm : new ModelPart[]{rightArm, leftArm}) {
                arm.xScale = arm.zScale = 0.7f;
                arm.yScale = 1.35f;
            }
            for (ModelPart leg : new ModelPart[]{rightLeg, leftLeg}) leg.xScale = leg.zScale = 0.75f;
            head.zRot = (Math.floorMod(step * 7919, 5) - 2) * 0.13f;
            head.xRot += (Math.floorMod(step * 104729, 3) - 1) * 0.15f;
            body.xRot = 0.12f;
            if (awake) {
                // arms flung about, a different pose every few frames
                rightArm.xRot = -0.4f + (Math.floorMod(step * 31, 7) - 3) * 0.35f;
                leftArm.xRot = -0.4f + (Math.floorMod(step * 17, 7) - 3) * 0.35f;
                rightArm.zRot = 0.3f + Math.floorMod(step * 13, 3) * 0.25f;
                leftArm.zRot = -0.3f - Math.floorMod(step * 11, 3) * 0.25f;
            } else {
                rightArm.zRot = 0.18f;
                leftArm.zRot = -0.18f;
            }
        }

        /** Bent forward at the hips, walking on its hands, its head hanging upside down in front. */
        private void folded(float limbSwing, float amount, float age, boolean awake) {
            body.setPos(0, 12, 0);
            body.xRot = -Mth.HALF_PI;
            body.yRot = 0;
            float headYaw = head.yRot;
            head.setPos(0, 12.5f, -12);
            head.setRotation(head.xRot * -0.5f, headYaw * 0.6f, Mth.PI + (awake ? Mth.sin(age * 0.5f) * 0.15f : 0.0f));
            float walk = Mth.cos(limbSwing * 0.6662f) * 1.1f * amount;
            rightArm.setPos(-5, 13.5f, -11);
            leftArm.setPos(5, 13.5f, -11);
            rightArm.setRotation(-walk, 0, 0.05f);
            leftArm.setRotation(walk, 0, -0.05f);
        }
    }

    // ------------------------------------------------------------------ extra faces and arms

    static class ExtraParts extends RenderLayer<StillLifeEntity, PlayerModel<StillLifeEntity>> {
        ExtraParts(RenderLayerParent<StillLifeEntity, PlayerModel<StillLifeEntity>> parent) {
            super(parent);
        }

        /** Renders {@code part} (and its overlay) once more, moved by {@code adjust}, then puts it back. */
        private static void again(ModelPart part, @Nullable ModelPart overlay, PoseStack pose, VertexConsumer consumer, int light, int overlayCoords,
                                  Consumer<ModelPart> adjust) {
            PartPose saved = part.storePose();
            float xs = part.xScale, ys = part.yScale, zs = part.zScale;
            boolean visible = part.visible;
            part.visible = true;
            adjust.accept(part);
            part.render(pose, consumer, light, overlayCoords);
            part.visible = visible;
            if (overlay != null) {
                PartPose savedOverlay = overlay.storePose();
                float oxs = overlay.xScale, oys = overlay.yScale, ozs = overlay.zScale;
                boolean overlayVisible = overlay.visible;
                overlay.copyFrom(part);
                overlay.visible = true;
                overlay.render(pose, consumer, light, overlayCoords);
                overlay.visible = overlayVisible;
                overlay.loadPose(savedOverlay);
                overlay.xScale = oxs;
                overlay.yScale = oys;
                overlay.zScale = ozs;
            }
            part.loadPose(saved);
            part.xScale = xs;
            part.yScale = ys;
            part.zScale = zs;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, StillLifeEntity e, float limbSwing, float amount,
                           float partialTick, float age, float yaw, float pitch) {
            if (e.isInvisible()) return;
            PlayerModel<StillLifeEntity> m = getParentModel();
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(e)));
            int overlay = LivingEntityRenderer.getOverlayCoords(e, 0.0f);
            boolean awake = e.isAwake();
            switch (e.form()) {
                case THREE_FACES -> {
                    for (int side : new int[]{-1, 1}) {
                        float look = awake ? Mth.sin(age * 0.23f + side * 2.0f) * 0.35f : 0.0f;    // the other two look about on their own
                        again(m.head, m.hat, pose, consumer, light, overlay, p -> {
                            p.x += side * 2.4f;
                            p.y += 0.8f;
                            p.z -= 0.35f;
                            p.yRot += side * 0.32f + look;
                            p.zRot += side * 0.22f;
                            p.xScale *= 0.85f;
                        });
                    }
                }
                case TWICE -> {
                    again(m.head, m.hat, pose, consumer, light, overlay, p -> {
                        p.y -= 3.5f;
                        p.z -= 0.45f;
                        p.zRot += 0.07f;
                        p.yScale *= 0.92f;
                    });
                    again(m.head, null, pose, consumer, light, overlay, p -> {
                        p.y += 2.2f;
                        p.x += 0.8f;
                        p.z -= 0.3f;
                        p.zRot -= 0.05f;
                        p.xScale *= 0.92f;
                        p.yScale *= 0.85f;
                    });
                }
                case MANY_HANDS -> {
                    for (int i = 0; i < 2; i++) {
                        int row = i;
                        for (int side : new int[]{-1, 1}) {
                            ModelPart arm = side < 0 ? m.rightArm : m.leftArm, sleeve = side < 0 ? m.rightSleeve : m.leftSleeve;
                            float reach = awake ? -1.5f + Mth.sin(age * 0.35f + row + side) * 0.35f : -0.9f - row * 0.35f;
                            again(arm, sleeve, pose, consumer, light, overlay, p -> {
                                p.setPos(side * (4.4f - row * 0.4f), 4.5f + row * 3.5f, -0.5f);
                                p.setRotation(reach, side * -0.7f + (awake ? 0.0f : side * -0.2f), side * 0.25f);
                                p.yScale *= 0.85f;
                            });
                        }
                    }
                }
                case COMMUTER -> {
                    // the hump on its back
                    again(m.body, m.jacket, pose, consumer, light, overlay, p -> {
                        p.y -= 0.5f;
                        p.z += 2.2f;
                        p.xRot += 0.5f;
                        p.xScale *= 1.15f;
                        p.yScale *= 0.55f;
                        p.zScale *= 1.7f;
                    });
                    // the boater, still on the neck where the head should be
                    pose.pushPose();
                    m.body.translateAndRotate(pose);
                    block(pose, buffers, light, Blocks.HAY_BLOCK.defaultBlockState(), 0, -0.6f, 0, 0.55f, 0.06f, 0.55f);
                    block(pose, buffers, light, Blocks.HAY_BLOCK.defaultBlockState(), 0, -1.8f, 0, 0.33f, 0.16f, 0.33f);
                    block(pose, buffers, light, Blocks.BLACK_WOOL.defaultBlockState(), 0, -1.0f, 0, 0.34f, 0.04f, 0.34f);
                    pose.popPose();
                    // and the briefcase
                    pose.pushPose();
                    m.leftArm.translateAndRotate(pose);
                    block(pose, buffers, light, Blocks.BROWN_TERRACOTTA.defaultBlockState(), 1, 15, 0, 0.13f, 0.34f, 0.46f);
                    block(pose, buffers, light, Blocks.DARK_OAK_PLANKS.defaultBlockState(), 1, 10.6f, 0, 0.05f, 0.05f, 0.18f);
                    pose.popPose();
                }
                case LOPSIDED -> {
                    // one enormous boot
                    again(m.leftLeg, m.leftPants, pose, consumer, light, overlay, p -> {
                        p.y += 8.5f;
                        p.z -= 1.2f;
                        p.xScale *= 1.8f;
                        p.yScale *= 0.42f;
                        p.zScale *= 2.3f;
                    });
                }
                case NOODLE_ARMS -> {
                    noodle(pose, m.rightArm, m.rightSleeve, consumer, light, overlay, -1, age, awake);
                    noodle(pose, m.leftArm, m.leftSleeve, consumer, light, overlay, 1, age, awake);
                }
                case PLANK_FACE -> {
                    // the plank, grown right through its face
                    pose.pushPose();
                    m.head.translateAndRotate(pose);
                    block(pose, buffers, light, Blocks.OAK_PLANKS.defaultBlockState(), 2.5f, -4.0f, 0.5f, 3.4f, 0.3f, 0.7f);
                    pose.popPose();
                }
                case GAPING -> {
                    float open = awake ? 6.0f + Mth.sin(age * 0.25f) * 1.5f : 3.0f;
                    float half = open / 2.0f;
                    again(m.head, m.hat, pose, consumer, light, overlay, p -> {
                        p.yScale *= 0.55f;
                        p.y -= 3.6f + half;
                        p.xRot -= open * 0.04f;
                    });
                    again(m.head, m.hat, pose, consumer, light, overlay, p -> {
                        p.yScale *= 0.55f;
                        p.y += half;
                        p.xRot += open * 0.025f;
                    });
                    // the dark between, and a row of teeth along each lip
                    pose.pushPose();
                    m.head.translateAndRotate(pose);
                    block(pose, buffers, light, Blocks.BLACK_CONCRETE.defaultBlockState(), 0, -4.0f + 0.4f, -2.4f, 0.46f, (open + 0.6f) / 16.0f, 0.3f);
                    block(pose, buffers, light, Blocks.BONE_BLOCK.defaultBlockState(), 0, -4.0f + 0.4f - half + 0.4f, -3.9f, 0.44f, 0.05f, 0.05f);
                    block(pose, buffers, light, Blocks.BONE_BLOCK.defaultBlockState(), 0, -4.0f + 0.4f + half - 0.4f, -3.9f, 0.44f, 0.05f, 0.05f);
                    pose.popPose();
                }
                default -> {
                }
            }
        }

        /** A block, centred on (x, y, z) in the current model frame (sixteenths, y down), {@code sx} by {@code sy} by {@code sz} blocks. */
        private static void block(PoseStack pose, MultiBufferSource buffers, int light, net.minecraft.world.level.block.state.BlockState state,
                                  float x, float y, float z, float sx, float sy, float sz) {
            pose.pushPose();
            pose.translate(x / 16.0f, y / 16.0f, z / 16.0f);
            pose.scale(-sx, -sy, sz);
            pose.translate(-0.5f, -0.5f, -0.5f);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }

        /** An arm that goes on and on: five bending lengths of it, writhing, the last one with the hand. */
        private static void noodle(PoseStack pose, ModelPart arm, ModelPart sleeve, VertexConsumer consumer, int light, int overlay, int side,
                                   float age, boolean awake) {
            pose.pushPose();
            pose.translate(arm.x / 16.0f, arm.y / 16.0f, arm.z / 16.0f);
            float speed = awake ? 0.22f : 0.06f;
            for (int k = 0; k < 5; k++) {
                // each length bends back the other way, so the arm coils on down to the floor
                float bend = k == 0 ? side * -0.25f : side * 0.75f * Mth.sin(age * speed + k * 1.9f) + (k % 2 == 0 ? 0.3f : -0.3f) * side;
                pose.mulPose(com.mojang.math.Axis.ZP.rotation(bend));
                pose.mulPose(com.mojang.math.Axis.XP.rotation(0.45f * Mth.sin(age * speed * 1.3f + k * 1.1f) - (awake && k == 0 ? 0.6f : 0.0f)));
                pose.pushPose();
                pose.scale(0.7f, 0.75f, 0.7f);
                again(arm, k == 0 ? sleeve : null, pose, consumer, light, overlay, p -> {
                    p.setPos(0, 0, 0);
                    p.setRotation(0, 0, 0);
                    p.xScale = p.yScale = p.zScale = 1.0f;
                });
                pose.popPose();
                pose.translate(0, 7.0f * 0.75f / 16.0f * 1.25f, 0);
            }
            pose.popPose();
        }
    }
}
