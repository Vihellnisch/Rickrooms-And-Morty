package com.rickandmorty.client.renderer;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.client.model.CronenbergModels;
import com.rickandmorty.entity.CouncilLook;
import com.rickandmorty.entity.CouncilMemberEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/**
 * A Council member: a player model in the skin of the player they're an alternate version of,
 * stretched or squashed into their odd shape, with their hat on. Their name floats above them.
 */
public class CouncilMemberRenderer extends LivingEntityRenderer<CouncilMemberEntity, PlayerModel<CouncilMemberEntity>> {
    private final ShapedModel wide;
    private final ShapedModel slim;

    public CouncilMemberRenderer(EntityRendererProvider.Context context) {
        super(context, new ShapedModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.wide = (ShapedModel) model;
        this.slim = new ShapedModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HatLayer(this, context.bakeLayer(CronenbergModels.HATS)));
    }

    private static PlayerSkin skin(CouncilMemberEntity member) {
        GameProfile profile = member.profile();
        if (profile == null) return DefaultPlayerSkin.get(member.getUUID());
        return Minecraft.getInstance().getSkinManager().getInsecureSkin(profile);
    }

    @Override
    public void render(CouncilMemberEntity member, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        model = skin(member).model() == PlayerSkin.Model.SLIM ? slim : wide;
        ((ShapedModel) model).shape = member.look().shape();
        super.render(member, yaw, partialTick, pose, buffers, light);
    }

    @Override
    protected void scale(CouncilMemberEntity member, PoseStack pose, float partialTick) {
        CouncilLook.Shape shape = member.look().shape();
        float base = 0.9375f;                      // the same shrink the player renderer applies
        pose.scale(base * shape.scaleX, base * shape.scaleY, base * shape.scaleZ);
    }

    @Override
    public ResourceLocation getTextureLocation(CouncilMemberEntity member) {
        return skin(member).texture();
    }

    /** A player model with a big (or tiny) head and long arms, per the member's shape. */
    static class ShapedModel extends PlayerModel<CouncilMemberEntity> {
        CouncilLook.Shape shape = CouncilLook.Shape.TALL;

        ShapedModel(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void setupAnim(CouncilMemberEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, limbSwing, amount, age, yaw, pitch);
            for (ModelPart part : new ModelPart[]{head, hat}) {
                part.xScale = part.yScale = part.zScale = shape.head;
            }
            for (ModelPart part : new ModelPart[]{leftArm, rightArm, leftSleeve, rightSleeve}) {
                part.xScale = part.zScale = 1.0f;
                part.yScale = shape.arms;
            }
        }
    }

    /** The member's hat, sitting on (and scaling with) their head. */
    static class HatLayer extends RenderLayer<CouncilMemberEntity, PlayerModel<CouncilMemberEntity>> {
        private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/council_hats.png");
        private final Map<CouncilLook.Hat, ModelPart> hats = new EnumMap<>(CouncilLook.Hat.class);
        private final ModelPart blades;

        HatLayer(RenderLayerParent<CouncilMemberEntity, PlayerModel<CouncilMemberEntity>> parent, ModelPart root) {
            super(parent);
            for (CouncilLook.Hat hat : CouncilLook.Hat.values()) {
                if (hat != CouncilLook.Hat.NONE) hats.put(hat, root.getChild(hat.name().toLowerCase()));
            }
            blades = hats.get(CouncilLook.Hat.PROPELLER).getChild("propeller_blades");
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, CouncilMemberEntity member, float limbSwing, float amount,
                           float partialTick, float age, float yaw, float pitch) {
            ModelPart hat = hats.get(member.look().hat());
            if (hat == null || member.isInvisible()) return;
            blades.yRot = age * 0.9f;
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            pose.pushPose();
            getParentModel().head.translateAndRotate(pose);
            hat.render(pose, consumer, light, LivingEntityRenderer.getOverlayCoords(member, 0.0f));
            pose.popPose();
        }
    }
}
