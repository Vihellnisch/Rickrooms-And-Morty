package com.rickandmorty.mixin.client.sodium;

import com.rickandmorty.client.gadget.XRay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** With the x-ray on under Sodium, an ore buried in stone still shows every face. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
public abstract class XRaySodiumFaceMixin {
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, require = 0)
    private void rickrooms$xrayFaces(BlockState state, BlockGetter level, BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (XRay.active() && XRay.shows(state) && !XRay.shows(level.getBlockState(pos.relative(face)))) cir.setReturnValue(true);
    }
}
