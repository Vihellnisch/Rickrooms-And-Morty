package com.rickandmorty.pet.goal;

import com.rickandmorty.pet.AmplifierModule;
import com.rickandmorty.pet.PetBrain;
import com.rickandmorty.pet.PetSpeech;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * Any amplified pet fights for its owner, cats, parrots and horses included: whatever hurts its owner,
 * whatever its owner hits, and whatever hurts it. With the Guardian Module it also hunts monsters
 * near it, and guards the spot where it's sitting (going back to sit there afterwards). It's smart
 * enough to leave creepers alone. Damage comes from {@link PetBrain#damage}, not the animal's own
 * attack, so pets that never had one can fight too.
 */
public class AmplifiedCombatGoal extends Goal {
    private static final double GIVE_UP = 32.0;
    private final Mob mob;
    private LivingEntity target;
    private BlockPos post;
    private boolean returning;
    private int cooldown, repath, checks;

    public AmplifiedCombatGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private static boolean sitting(Mob mob) {
        return mob instanceof TamableAnimal tame && tame.isOrderedToSit();
    }

    @Override
    public boolean canUse() {
        if (!PetBrain.isAmplified(mob) || mob.isVehicle() || mob.isPassenger()) return false;
        target = findTarget();
        return target != null;
    }

    private LivingEntity findTarget() {
        boolean guardian = PetBrain.has(mob, AmplifierModule.GUARDIAN), sitting = sitting(mob);
        if (sitting && !guardian) return null;
        Player owner = PetBrain.owner(mob);
        if (owner != null && !sitting && owner.level() == mob.level()) {
            LivingEntity attacker = owner.getLastHurtByMob();
            if (attacker != null && owner.tickCount - owner.getLastHurtByMobTimestamp() < 100 && valid(attacker, owner)) return attacker;
            LivingEntity victim = owner.getLastHurtMob();
            if (victim != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 100 && valid(victim, owner)) return victim;
        }
        LivingEntity mine = mob.getLastHurtByMob();
        if (mine != null && mob.tickCount - mob.getLastHurtByMobTimestamp() < 100 && valid(mine, owner)) return mine;
        if (guardian && ++checks % 5 == 0) {
            return mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(12.0, 4.0, 12.0),
                            e -> e instanceof Enemy && valid(e, owner) && mob.hasLineOfSight(e))
                    .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        }
        return null;
    }

    /** Worth fighting: alive, near, not a creeper, not its owner or one of its owner's other pets. */
    private boolean valid(LivingEntity other, Player owner) {
        if (other == null || !other.isAlive() || other == mob || other == owner || other instanceof Creeper || other instanceof ArmorStand) return false;
        if (other instanceof Player player && (player.isCreative() || player.isSpectator() || (owner != null && !owner.canHarmPlayer(player)))) return false;
        if (owner != null && other instanceof OwnableEntity ownable && owner.getUUID().equals(ownable.getOwnerUUID())) return false;
        if (owner != null && other instanceof Mob pet && PetBrain.isAmplified(pet) && PetBrain.ownedBy(pet, owner)) return false;
        return other.level() == mob.level() && mob.distanceToSqr(other) < GIVE_UP * GIVE_UP;
    }

    @Override
    public void start() {
        returning = false;
        cooldown = 0;
        repath = 0;
        post = null;
        if (sitting(mob)) {
            post = mob.blockPosition();
            ((TamableAnimal) mob).setInSittingPose(false);
        }
        if (mob.getRandom().nextFloat() < 0.35f) PetSpeech.say(mob, PetSpeech.Topic.ATTACK, false);
    }

    @Override
    public boolean canContinueToUse() {
        if (!PetBrain.isAmplified(mob) || mob.isVehicle()) return false;
        if (!returning && valid(target, PetBrain.owner(mob))) return true;
        if (post == null) return false;
        if (!returning) {
            returning = true;
            repath = 0;
        }
        return mob.blockPosition().distSqr(post) > 2 && sitting(mob);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (returning) {
            if (--repath <= 0) {
                repath = 20;
                mob.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.1);
            }
            return;
        }
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
        if (--repath <= 0) {
            repath = 10;
            mob.getNavigation().moveTo(target, 1.3);
        }
        if (cooldown > 0) cooldown--;
        if (cooldown <= 0 && mob.isWithinMeleeAttackRange(target) && mob.hasLineOfSight(target)) {
            strike();
            cooldown = PetBrain.has(mob, AmplifierModule.COMBAT) ? 12 : 20;
        }
    }

    private void strike() {
        mob.swing(InteractionHand.MAIN_HAND);
        if (target.hurt(mob.damageSources().mobAttack(mob), PetBrain.damage(mob))) {
            double knock = PetBrain.has(mob, AmplifierModule.COMBAT) ? 0.8 : 0.4;
            target.knockback(knock, Mth.sin(mob.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(mob.getYRot() * Mth.DEG_TO_RAD));
            mob.setLastHurtMob(target);
            mob.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.4f, 1.3f + mob.getRandom().nextFloat() * 0.3f);
        }
    }

    @Override
    public void stop() {
        target = null;
        returning = false;
        post = null;
        mob.getNavigation().stop();
        if (sitting(mob)) ((TamableAnimal) mob).setInSittingPose(true);
    }
}
