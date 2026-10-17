package com.rickandmorty.mixin.client;

import com.rickandmorty.client.gadget.XRay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The ores the x-ray shows are buried in rock that isn't drawn, so they'd be shaded as dark as the
 * inside of a wall. While it's on, no ambient-occlusion shading (and see {@link XRayLightLevelMixin}
 * for the light itself).
 */
@Mixin(ModelBlockRenderer.class)
public abstract class XRayLightMixin {
    @Redirect(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useAmbientOcclusion()Z"))
    private boolean rickrooms$noShadingInXray() {
        return !XRay.active() && Minecraft.useAmbientOcclusion();
    }
}
