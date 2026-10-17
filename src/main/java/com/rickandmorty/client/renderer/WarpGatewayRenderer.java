package com.rickandmorty.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rickandmorty.block.GatewayStructure;
import com.rickandmorty.block.WarpGatewayBlock;
import com.rickandmorty.block.entity.EvilGateway;
import com.rickandmorty.block.entity.WarpGatewayBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Draws the swirling portal inside a linked Warp Gateway's ring. The ring itself is a block model. */
public class WarpGatewayRenderer implements BlockEntityRenderer<WarpGatewayBlockEntity> {
    private static final float OPEN_TICKS = 20.0f;

    public WarpGatewayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WarpGatewayBlockEntity gateway, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        BlockState state = gateway.getBlockState();
        if (!state.hasProperty(WarpGatewayBlock.ACTIVE)) return;
        boolean red = state.getValue(WarpGatewayBlock.RED);
        Direction facing = state.getValue(WarpGatewayBlock.FACING);
        long time = gateway.getLevel().getGameTime();
        float spin = time + partialTick;
        // a sprung evil gateway opens a red vortex, linked or not
        boolean busy = gateway.isEvil() && gateway.evilPhase() != EvilGateway.IDLE;   // e.g. flickering in a meltdown
        if (state.getValue(WarpGatewayBlock.ACTIVE) || red || busy) {
            if (gateway.clientOpenedAt < 0) gateway.clientOpenedAt = time;
            float open = Mth.clamp((time - gateway.clientOpenedAt + partialTick) / OPEN_TICKS, 0.0f, 1.0f);
            float radius = (float) GatewayStructure.RING_INNER * 0.97f * (1.0f - (1.0f - open) * (1.0f - open));
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.0f, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(-GatewayStructure.yRotation(facing)));
            poseStack.translate(-0.5f + GatewayStructure.RING_X, GatewayStructure.RING_Y, 0.0f);
            poseStack.scale(radius, radius, 1.0f);
            if (red) PortalRenderer.renderRedVortex(poseStack, buffers, spin * 1.6f);
            else PortalRenderer.renderVortex(poseStack, buffers, spin, false);
            poseStack.popPose();
        } else {
            gateway.clientOpenedAt = -1;
        }
        if (busy) renderArms(gateway, facing, partialTick, spin, poseStack, buffers, packedLight);
    }

    /** The evil gateway's tentacles and saws, reaching out of the ring. */
    private static void renderArms(WarpGatewayBlockEntity gateway, Direction facing, float partialTick, float time, PoseStack poseStack,
                                   MultiBufferSource buffers, int light) {
        float age = gateway.evilPhaseAge(partialTick);
        float reach, saw;
        boolean thrash = false;
        switch (gateway.evilPhase()) {
            case EvilGateway.GRAB -> {
                reach = Mth.clamp(age / (EvilGateway.GRAB_TICKS * 0.7f), 0.0f, 1.0f);
                saw = 0.0f;
            }
            case EvilGateway.BLEND -> {
                reach = 1.0f;
                saw = Mth.clamp(age / 10.0f, 0.0f, 1.0f);
            }
            case EvilGateway.DEVOUR -> {
                reach = 1.0f;                                    // dragging the corpse in
                saw = 1.0f - Mth.clamp(age / 10.0f, 0.0f, 1.0f);
            }
            case EvilGateway.RETRACT -> {
                reach = 1.0f - Mth.clamp(age / EvilGateway.RETRACT_TICKS, 0.0f, 1.0f);
                saw = 0.0f;
            }
            case EvilGateway.MELTDOWN -> {
                reach = 1.0f;
                saw = 1.0f;
                thrash = true;
            }
            default -> {
                return;
            }
        }
        BlockPos pos = gateway.getBlockPos();
        Vec3 center = EvilGateway.center(pos, facing);
        Vec3 n = EvilGateway.normal(facing);
        Vec3 r = new Vec3(-n.z, 0, n.x);
        poseStack.pushPose();
        poseStack.translate(center.x - pos.getX(), center.y - pos.getY(), center.z - pos.getZ());
        BlenderArms.render(gateway, reach, saw, thrash, time, center, n, r, new Vec3(0, 1, 0), gateway.evilVictim(), partialTick, 1.2f,
                poseStack, buffers, light);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(WarpGatewayBlockEntity gateway) {
        BlockPos pos = gateway.getBlockPos();
        AABB ring = new AABB(pos.getX() - 1.5, pos.getY(), pos.getZ() - 1.5, pos.getX() + 2.5, pos.getY() + 3.0, pos.getZ() + 2.5);
        return gateway.isEvil() ? ring.inflate(4.0) : ring;     // the arms reach a long way out
    }
}
