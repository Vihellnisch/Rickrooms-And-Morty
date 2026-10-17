package com.rickandmorty.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.client.model.PickleModel;
import com.rickandmorty.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** Draws players under the Pickle Rick effect as a pickle, and hides their (non-existent) hands. */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID, value = Dist.CLIENT)
public class PickleRickRenderer {
    private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/pickle_rick.png");
    private static PickleModel model;

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.hasEffect(ModEffects.PICKLE_RICK)) return;
        event.setCanceled(true);
        if (model == null) {
            model = new PickleModel(Minecraft.getInstance().getEntityModels().bakeLayer(PickleModel.LAYER));
        }
        float partialTick = event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        // the pickle model is one block tall; make it as tall as the shrunken player
        float size = player.getBbHeight();
        pose.scale(-size, -size, size);
        pose.translate(0.0f, -1.501f, 0.0f);
        float limbSwing = player.walkAnimation.position(partialTick);
        float limbAmount = player.walkAnimation.speed(partialTick);
        model.setupAnim(player, limbSwing, limbAmount, player.tickCount + partialTick, 0, 0);
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0f);
        model.renderToBuffer(pose, event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                event.getPackedLight(), overlay, -1);
        pose.popPose();
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.hasEffect(ModEffects.PICKLE_RICK)) {
            event.setCanceled(true);
        }
    }
}
