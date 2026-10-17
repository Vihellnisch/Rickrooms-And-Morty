package com.rickandmorty.pet.goal;

import com.rickandmorty.pet.AmplifierModule;
import com.rickandmorty.pet.PetBrain;
import com.rickandmorty.pet.PetSpeech;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * The thinking an amplified pet does alongside whatever else it's up to (it takes no control): it
 * heals slowly (fast, with the Medic Module, which also patches its owner up when they're badly
 * hurt), warns its owner about monsters with the Guardian Module, and chats now and then.
 */
public class AmplifiedBrainGoal extends Goal {
    private static final int MEDIC_COOLDOWN = 1200;
    private final Mob mob;

    public AmplifiedBrainGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return PetBrain.isAmplified(mob);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (mob.tickCount % 20 != 0 || !(mob.level() instanceof ServerLevel level)) return;
        boolean medic = PetBrain.has(mob, AmplifierModule.MEDIC);
        if (mob.getHealth() < mob.getMaxHealth() && (medic || mob.tickCount % 100 == 0)) mob.heal(1.0f);
        if (mob.getHealth() < mob.getMaxHealth() * 0.3f) PetSpeech.say(mob, PetSpeech.Topic.LOW_HEALTH, false);

        Player owner = PetBrain.owner(mob);
        if (owner == null || owner.level() != level || !owner.isAlive()) return;
        double near = mob.distanceToSqr(owner);
        if (medic && near < 16 * 16 && owner.getHealth() <= owner.getMaxHealth() * 0.4f) {
            long now = level.getGameTime(), last = PetBrain.medicAt(mob);
            if (now - last > MEDIC_COOLDOWN || last > now) {
                owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                level.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 1.8, owner.getZ(), 6, 0.4, 0.3, 0.4, 0.0);
                PetBrain.setMedicAt(mob, now);
                PetSpeech.say(mob, PetSpeech.Topic.HEAL_OWNER, true);
            }
        }
        if (PetBrain.has(mob, AmplifierModule.GUARDIAN) && near < 32 * 32 && mob.tickCount % 40 == 0) {
            var around = owner.getBoundingBox().inflate(14.0, 6.0, 14.0);
            if (!level.getEntitiesOfClass(Creeper.class, owner.getBoundingBox().inflate(9.0, 4.0, 9.0), Creeper::isAlive).isEmpty()) {
                PetSpeech.say(mob, PetSpeech.Topic.CREEPER, false);
            } else if (!level.getEntitiesOfClass(Mob.class, around, e -> e instanceof Enemy && e.isAlive() && e.getTarget() == owner).isEmpty()) {
                PetSpeech.say(mob, PetSpeech.Topic.WARNING, false);
            }
        }
        if (near < 16 * 16 && mob.getRandom().nextInt(90) == 0) {
            boolean night = level.dimensionType().hasSkyLight() && level.isNight();
            PetSpeech.say(mob, night && mob.getRandom().nextInt(3) == 0 ? PetSpeech.Topic.NIGHT : PetSpeech.Topic.IDLE, false);
        }
    }
}
