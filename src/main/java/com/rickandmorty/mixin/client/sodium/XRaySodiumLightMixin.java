package com.rickandmorty.mixin.client.sodium;

import com.rickandmorty.client.gadget.XRay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Under Sodium, the x-ray's ores would be lit and shaded like the inside of the rock round them.
 * While it's on, every block reads as fully lit, glowing, and not in the way of the light.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess", remap = false)
public abstract class XRaySodiumLightMixin {
    @Shadow
    public static int packBL(int light) {
        throw new AssertionError();
    }

    @Shadow
    public static int packSL(int light) {
        throw new AssertionError();
    }

    @Shadow
    public static int packAO(float ao) {
        throw new AssertionError();
    }

    @Shadow
    public static int packEM(boolean emissive) {
        throw new AssertionError();
    }

    @Inject(method = "compute", at = @At("HEAD"), cancellable = true, require = 0)
    private void rickrooms$fullyLitInXray(int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
        if (XRay.active()) cir.setReturnValue(packBL(15) | packSL(15) | packAO(1.0f) | packEM(true));
    }
}
