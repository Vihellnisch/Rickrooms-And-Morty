package com.rickandmorty.registry;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.effect.PickleRickEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Status effects and potions: Pickle Rick. */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, RickAndMortyMod.MOD_ID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, RickAndMortyMod.MOD_ID);

    public static final DeferredHolder<MobEffect, PickleRickEffect> PICKLE_RICK = EFFECTS.register("pickle_rick", PickleRickEffect::new);
    public static final DeferredHolder<MobEffect, com.rickandmorty.effect.BleedingEffect> BLEEDING =
            EFFECTS.register("bleeding", com.rickandmorty.effect.BleedingEffect::new);

    /** The Shrink Ray's work: half size, or (with the Growth Lens) twice it. */
    public static final DeferredHolder<MobEffect, MobEffect> SHRUNK = EFFECTS.register("shrunk",
            () -> new com.rickandmorty.effect.PlainEffect(net.minecraft.world.effect.MobEffectCategory.NEUTRAL, 0x6CE04A)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.SCALE, RickAndMortyMod.id("shrunk"), -0.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> ENLARGED = EFFECTS.register("enlarged",
            () -> new com.rickandmorty.effect.PlainEffect(net.minecraft.world.effect.MobEffectCategory.NEUTRAL, 0xFF8A2A)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.SCALE, RickAndMortyMod.id("enlarged"), 1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    /** The Freeze Ray's: frozen solid, can't move or jump. */
    public static final DeferredHolder<MobEffect, MobEffect> FROZEN = EFFECTS.register("frozen",
            () -> new com.rickandmorty.effect.FrozenEffect()
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, RickAndMortyMod.id("frozen"), -1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH, RickAndMortyMod.id("frozen_jump"), -1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, RickAndMortyMod.id("frozen_attack"), -0.9, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<Potion, Potion> PICKLE_RICK_POTION = POTIONS.register("pickle_rick",
            () -> new Potion("pickle_rick", new MobEffectInstance(PICKLE_RICK, 1200)));
    public static final DeferredHolder<Potion, Potion> LONG_PICKLE_RICK_POTION = POTIONS.register("long_pickle_rick",
            () -> new Potion("pickle_rick", new MobEffectInstance(PICKLE_RICK, 3600)));

    /** Awkward Potion + Sea Pickle = Pickle Rick. Add redstone to make it last longer. */
    @SubscribeEvent
    public static void registerBrewing(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addMix(Potions.AWKWARD, Items.SEA_PICKLE, PICKLE_RICK_POTION);
        event.getBuilder().addMix(PICKLE_RICK_POTION, Items.REDSTONE, LONG_PICKLE_RICK_POTION);
    }
}
