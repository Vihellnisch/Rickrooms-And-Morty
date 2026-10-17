package com.rickandmorty.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.item.ScarecrowLanternItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * In first person the Scarecrow Lantern isn't a flat picture: your arm is raised in front of you
 * and the lantern hangs from your fist by its chain, swaying a little as you walk.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID, value = Dist.CLIENT)
public final class LanternHandRenderer {
    private LanternHandRenderer() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ScarecrowLanternItem)) return;
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || player.isInvisible()) return;
        event.setCanceled(true);
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        render(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getEquipProgress(), event.getSwingProgress(),
                event.getPartialTick(), arm, player, stack);
    }

    private static void render(PoseStack pose, MultiBufferSource buffers, int light, float equip, float swing, float partialTick,
                               HumanoidArm arm, AbstractClientPlayer player, ItemStack stack) {
        boolean right = arm == HumanoidArm.RIGHT;
        float side = right ? 1.0f : -1.0f;
        float root = Mth.sqrt(swing);
        org.joml.Matrix4f view = new org.joml.Matrix4f(pose.last().pose());
        // the arm: vanilla's empty hand, lifted up into view and tilted in a little
        pose.pushPose();
        pose.translate(0.0f, ARM_LIFT, 0.0f);
        pose.mulPose(Axis.ZP.rotationDegrees(side * ARM_TILT));
        pose.translate(side * (-0.3f * Mth.sin(root * Mth.PI) + 0.64f), 0.4f * Mth.sin(root * Mth.TWO_PI) - 0.6f + equip * -0.6f,
                -0.4f * Mth.sin(swing * Mth.PI) - 0.72f);
        pose.mulPose(Axis.YP.rotationDegrees(side * 45.0f));
        pose.mulPose(Axis.YP.rotationDegrees(side * Mth.sin(root * Mth.PI) * 70.0f));
        pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.sin(swing * swing * Mth.PI) * -20.0f));
        pose.translate(side * -1.0f, 3.6f, 3.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(side * 120.0f));
        pose.mulPose(Axis.XP.rotationDegrees(200.0f));
        pose.mulPose(Axis.YP.rotationDegrees(side * -135.0f));
        pose.translate(side * 5.6f, 0.0f, 0.0f);
        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (right) renderer.renderRightHand(pose, buffers, light, player);
        else renderer.renderLeftHand(pose, buffers, light, player);
        // where the fist ended up: the bottom of the arm, in the middle of its width
        var model = renderer.getModel();
        (right ? model.rightArm : model.leftArm).translateAndRotate(pose);
        boolean slim = player.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
        float middle = (slim ? 0.5f : 1.0f) * -side;
        org.joml.Vector3f fist = pose.last().pose().transformPosition(middle / 16.0f, FIST_DEPTH / 16.0f, 0.0f, new org.joml.Vector3f());
        view.invert().transformPosition(fist);
        // slide it along the line of sight towards the eye: it stays exactly on the fist on screen,
        // but in front of the forearm instead of behind it (and smaller, to look the same size)
        fist.mul(NEARER);
        pose.popPose();

        // the lantern hangs straight down from that fist by its chain, swinging a little
        pose.pushPose();
        float time = player.tickCount + partialTick;
        float walk = Math.min(1.0f, player.walkAnimation.speed(partialTick) * 2.0f);
        float sway = Mth.sin(time * 0.1f) * 3.0f + Mth.sin(player.walkAnimation.position(partialTick) * 0.66f) * 9.0f * walk;
        pose.translate(fist.x, fist.y, fist.z);
        pose.mulPose(Axis.ZP.rotationDegrees(sway));
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - side * 12.0f));      // its stitched face towards you
        pose.scale(SCALE * NEARER, SCALE * NEARER, SCALE * NEARER);
        pose.translate(0.0f, -CHAIN_TOP, 0.0f);                    // the top of the chain is in the fist
        Minecraft.getInstance().getItemRenderer().renderStatic(player, stack, ItemDisplayContext.NONE, !right, pose, buffers, player.level(),
                light, OverlayTexture.NO_OVERLAY, player.getId());
        pose.popPose();
    }

    private static final float ARM_LIFT = 0.42f, ARM_TILT = 14.0f;
    /** How far down the arm (in model pixels) the grip is, and how high the chain's top is above the lantern's middle. */
    private static final float FIST_DEPTH = 9.5f, CHAIN_TOP = 0.36f, SCALE = 0.46f, NEARER = 0.7f;
}
