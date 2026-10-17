package com.rickandmorty.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rickandmorty.client.ShaderCompat;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shader packs draw vanilla's round blob shadow as a pale white disc in dimensions without a
 * sky, like the Backrooms levels. There, with a shader pack on, leave the blob out.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityShadowMixin {
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void rickrooms$noBlobInTheBackrooms(PoseStack poseStack, MultiBufferSource buffers, Entity entity, float weight,
                                                          float partialTick, LevelReader level, float size, CallbackInfo ci) {
        if (entity.level() instanceof Level in && BackroomsLevel.isBackrooms(in.dimension()) && ShaderCompat.shaderPackInUse()) ci.cancel();
    }
}
