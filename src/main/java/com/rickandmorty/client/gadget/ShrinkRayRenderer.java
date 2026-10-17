package com.rickandmorty.client.gadget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.client.model.ShrinkRayLayers;
import com.rickandmorty.gadget.ShrinkRayBlock;
import com.rickandmorty.gadget.ShrinkRayBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the Shrink Ray at its real size, about three and a half blocks tall, with its emitter over
 * the block in front: the base, the jointed arm, the head with its glowing core and cone, and,
 * while it fires, a beam of light down to the floor. Green when it shrinks, orange when it grows.
 */
public class ShrinkRayRenderer implements BlockEntityRenderer<ShrinkRayBlockEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(RickAndMortyMod.id("shrink_ray"), "main");
    private static final ResourceLocation SHRINK = RickAndMortyMod.id("textures/entity/shrink_ray.png"),
            SHRINK_GLOW = RickAndMortyMod.id("textures/entity/shrink_ray_glow.png"),
            GROW = RickAndMortyMod.id("textures/entity/shrink_ray_grow.png"),
            GROW_GLOW = RickAndMortyMod.id("textures/entity/shrink_ray_grow_glow.png");
    private static ModelPart model;

    public ShrinkRayRenderer(BlockEntityRendererProvider.Context context) {
        model = context.bakeLayer(LAYER);
    }

    public static net.minecraft.client.model.geom.builders.LayerDefinition layer() {
        return ShrinkRayLayers.shrinkRay();
    }

    private static ModelPart model() {
        if (model == null) model = Minecraft.getInstance().getEntityModels().bakeLayer(LAYER);
        return model;
    }

    /** Draws it standing at the middle of the current pose's floor, facing {@code facing}. */
    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, Direction facing, boolean grow, boolean firing, float time) {
        ModelPart root = model();
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - facing.toYRot()));
        ModelPart beam = root.getChild("beam");
        beam.visible = false;
        root.render(pose, buffers.getBuffer(RenderType.entityCutout(grow ? GROW : SHRINK)), light, overlay);
        beam.visible = firing;
        float pulse = 0.75f + 0.25f * (float) Math.sin(time * 0.15);
        int glow = (int) (255 * pulse);
        root.render(pose, buffers.getBuffer(RenderType.eyes(grow ? GROW_GLOW : SHRINK_GLOW)), LightTexture.FULL_BRIGHT, overlay,
                0xFF000000 | glow << 16 | glow << 8 | glow);
        beam.visible = false;
        pose.popPose();
    }

    @Override
    public void render(ShrinkRayBlockEntity ray, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = ray.getBlockState();
        if (!(state.getBlock() instanceof ShrinkRayBlock)) return;
        float time = ray.getLevel() == null ? 0 : ray.getLevel().getGameTime() + partialTick;
        draw(pose, buffers, light, overlay, state.getValue(ShrinkRayBlock.FACING),
                state.getValue(ShrinkRayBlock.GROW) && state.getValue(ShrinkRayBlock.UPGRADED), state.getValue(ShrinkRayBlock.COOLING), time);
    }

    @Override
    public AABB getRenderBoundingBox(ShrinkRayBlockEntity ray) {
        return new AABB(ray.getBlockPos()).inflate(1.5, 0.0, 1.5).expandTowards(0.0, 3.0, 0.0);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    /** The same model in your hand and inventory, shrunk to fit. */
    public static class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            pose.pushPose();
            pose.translate(0.5, 0.0, 0.5);
            pose.scale(0.3f, 0.3f, 0.3f);
            pose.translate(-0.5, 0.0, -0.5);
            draw(pose, buffers, light, overlay, Direction.NORTH, false, false, 0);
            pose.popPose();
        }
    }
}
