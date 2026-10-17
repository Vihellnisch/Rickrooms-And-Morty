package com.rickandmorty.mixin.client;

import com.rickandmorty.client.gadget.XRay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** With the x-ray on, an ore buried in stone still shows every face (the stone round it isn't drawn). */
@Mixin(Block.class)
public abstract class XRayFaceMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void rickrooms$xrayFaces(BlockState state, BlockGetter level, BlockPos pos, Direction face, BlockPos neighbor,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (XRay.active() && XRay.shows(state) && !XRay.shows(level.getBlockState(neighbor))) cir.setReturnValue(true);
    }
}
