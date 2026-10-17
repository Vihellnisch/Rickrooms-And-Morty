package com.rickandmorty.mixin;

import com.mojang.serialization.Lifecycle;
import com.rickandmorty.RickAndMortyMod;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Vanilla calls any world with a dimension it doesn't know "experimental" and warns about it
 * on creation. This mod's dimensions (the Backrooms levels, Cronenberg World) are ordinary
 * datapack dimensions, so count them as stable. If anything else makes the world experimental
 * (another mod's dimension, experimental features), nothing changes and the warning still shows.
 */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMixin {
    @Inject(method = "bake", at = @At("RETURN"), cancellable = true)
    private void rickrooms$ourDimensionsAreStable(Registry<LevelStem> datapackDimensions, CallbackInfoReturnable<WorldDimensions.Complete> cir) {
        WorldDimensions.Complete complete = cir.getReturnValue();
        Registry<LevelStem> dimensions = complete.dimensions();
        if (dimensions.registryLifecycle() == Lifecycle.stable()) return;
        boolean anyOfOurs = false;
        for (ResourceKey<LevelStem> key : dimensions.registryKeySet()) {
            if (key.location().getNamespace().equals(RickAndMortyMod.MOD_ID)) {
                anyOfOurs = true;
                continue;
            }
            // anything else must be one of the three vanilla dimensions, and vanilla-stable itself
            boolean vanilla = key == LevelStem.OVERWORLD || key == LevelStem.NETHER || key == LevelStem.END;
            Lifecycle lifecycle = dimensions.registrationInfo(key).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental());
            if (!vanilla || lifecycle != Lifecycle.stable()) return;
        }
        if (!anyOfOurs) return;
        MappedRegistry<LevelStem> stable = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable());
        dimensions.holders().forEach(holder -> {                  // in registration order
            ResourceKey<LevelStem> key = holder.key();
            Optional<net.minecraft.server.packs.repository.KnownPack> pack = dimensions.registrationInfo(key).flatMap(RegistrationInfo::knownPackInfo);
            stable.register(key, holder.value(), new RegistrationInfo(pack, Lifecycle.stable()));
        });
        cir.setReturnValue(new WorldDimensions.Complete(stable.freeze(), complete.specialWorldProperty()));
    }
}
