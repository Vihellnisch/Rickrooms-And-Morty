package com.rickandmorty.mixin.client.sodium;

import com.rickandmorty.client.gadget.XRay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No liquids through the x-ray under Sodium either. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.neoforge.render.FluidRendererImpl", remap = false)
public abstract class XRaySodiumFluidMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void rickrooms$xrayLiquid(CallbackInfo ci) {
        if (XRay.active()) ci.cancel();
    }
}
