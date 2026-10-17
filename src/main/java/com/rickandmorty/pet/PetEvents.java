package com.rickandmorty.pet;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.network.PetAmplifierPayload;
import com.rickandmorty.pet.goal.AmplifiedBrainGoal;
import com.rickandmorty.pet.goal.AmplifiedCombatGoal;
import com.rickandmorty.pet.goal.AmplifiedFetchGoal;
import com.rickandmorty.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Putting the helmet on (use it on your pet), fitting modules (use them on it), taking it off (sneak
 * and use shears), and giving every pet that could wear one the goals that make it act clever once it
 * does.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class PetEvents {
    private static final Set<Mob> WIRED = Collections.newSetFromMap(new WeakHashMap<>());

    private PetEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !PetBrain.canWear(mob)) return;
        if (WIRED.add(mob)) {
            mob.goalSelector.addGoal(-1, new AmplifiedCombatGoal(mob));
            mob.goalSelector.addGoal(0, new AmplifiedBrainGoal(mob));
            mob.goalSelector.addGoal(1, new AmplifiedFetchGoal(mob));
            if (GoalUtils.hasGroundPathNavigation(mob)) {
                mob.goalSelector.addGoal(2, new OpenDoorGoal(mob, true) {
                    @Override
                    public boolean canUse() {
                        return PetBrain.isAmplified(mob) && super.canUse();
                    }
                });
            }
        }
        if (PetBrain.isAmplified(mob)) PetBrain.refresh(mob);
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!PetBrain.canWear(event.getTarget())) return;
        Mob mob = (Mob) event.getTarget();
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        boolean helmet = stack.is(ModItems.COGNITION_AMPLIFIER.get()), module = stack.getItem() instanceof AmplifierModuleItem,
                shears = stack.getItem() instanceof ShearsItem && player.isSecondaryUseActive();
        if (!helmet && !module && !shears) return;
        boolean client = event.getLevel().isClientSide;
        if (shears && !(client ? AmplifierClientState.get(mob.getId()) >= 0 : PetBrain.isAmplified(mob))) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(client));
        if (client) return;

        if (helmet) {
            if (!PetBrain.belongsTo(mob, player)) tell(player, "not_yours");
            else if (PetBrain.isAmplified(mob)) tell(player, "already");
            else {
                int modules = CognitionAmplifierItem.modules(stack);
                stack.consume(1, player);
                mob.level().playSound(null, mob, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.0f, 1.2f);
                PetBrain.fit(mob, player, modules);
            }
        } else if (module) {
            AmplifierModule which = ((AmplifierModuleItem) stack.getItem()).module();
            if (!PetBrain.isAmplified(mob)) tell(player, "needs_helmet");
            else if (!PetBrain.ownedBy(mob, player)) tell(player, "not_yours");
            else if (!PetBrain.addModule(mob, which)) tell(player, "has_module");
            else {
                stack.consume(1, player);
                mob.level().playSound(null, mob, SoundEvents.SMITHING_TABLE_USE, SoundSource.NEUTRAL, 0.8f, 1.4f);
                PetSpeech.say(mob, PetSpeech.Topic.MODULE, true);
            }
        } else {
            if (!PetBrain.ownedBy(mob, player)) {
                tell(player, "not_yours");
                return;
            }
            PetSpeech.say(mob, PetSpeech.Topic.REMOVED, true);
            ItemStack off = PetBrain.remove(mob);
            if (!player.addItem(off)) player.drop(off, false);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
            mob.level().playSound(null, mob, SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0f, 1.2f);
        }
    }

    private static void tell(Player player, String what) {
        player.displayClientMessage(Component.translatable("message.rickrooms.amplifier." + what).withStyle(ChatFormatting.GRAY), true);
    }

    /** Kills get a remark; a pet that dies drops its helmet (and what it carried), with some last words. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) return;
        if (event.getSource().getEntity() instanceof Mob killer && killer != dead && PetBrain.isAmplified(killer) && killer.getRandom().nextBoolean()) {
            PetSpeech.say(killer, PetSpeech.Topic.KILL, false);
        }
        if (dead instanceof Mob pet && PetBrain.isAmplified(pet)) {
            PetSpeech.say(pet, PetSpeech.Topic.DEATH, true);
            pet.spawnAtLocation(PetBrain.remove(pet));
        }
    }

    /** When you get hurt, one of your clever pets nearby has something to say about it. */
    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer || event.getNewDamage() <= 0) return;
        if (player.getHealth() > player.getMaxHealth() * 0.6f && player.getRandom().nextFloat() > 0.3f) return;
        List<Mob> pets = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24.0),
                pet -> PetBrain.isAmplified(pet) && PetBrain.ownedBy(pet, player));
        if (!pets.isEmpty()) PetSpeech.say(pets.get(player.getRandom().nextInt(pets.size())), PetSpeech.Topic.OWNER_HURT, false);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Mob pet && PetBrain.isAmplified(pet) && event.getEntity() instanceof ServerPlayer watcher
                && !(watcher instanceof FakePlayer) && watcher.connection != null && watcher.connection.hasChannel(PetAmplifierPayload.TYPE)) {
            PacketDistributor.sendToPlayer(watcher, new PetAmplifierPayload(pet.getId(), PetBrain.modules(pet)));
        }
    }
}
