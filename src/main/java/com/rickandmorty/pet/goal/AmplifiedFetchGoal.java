package com.rickandmorty.pet.goal;

import com.rickandmorty.pet.AmplifierModule;
import com.rickandmorty.pet.PetBrain;
import com.rickandmorty.pet.PetSpeech;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * The Fetch Module: the pet goes round picking up items lying near it (not ones its owner threw
 * away on purpose), up to {@link PetBrain#MAX_CARRIED} stacks, and brings them back to its owner.
 */
public class AmplifiedFetchGoal extends Goal {
    private final Mob mob;
    private ItemEntity item;
    private boolean delivering;
    private int repath, time, checks;

    public AmplifiedFetchGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean able() {
        return PetBrain.has(mob, AmplifierModule.FETCH) && !mob.isVehicle() && !mob.isPassenger()
                && !(mob instanceof TamableAnimal tame && tame.isOrderedToSit());
    }

    private Player owner() {
        Player owner = PetBrain.owner(mob);
        return owner != null && owner.level() == mob.level() && owner.isAlive() && owner.distanceToSqr(mob) < 32 * 32 ? owner : null;
    }

    @Override
    public boolean canUse() {
        if (++checks % 5 != 0 || !able()) return false;              // looking round every so often is plenty
        Player owner = owner();
        if (owner == null) return false;
        item = mob.level().getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(10.0, 4.0, 10.0),
                        e -> e.isAlive() && !e.hasPickUpDelay() && e.getOwner() != owner && e.distanceToSqr(owner) < 24 * 24
                                && PetBrain.hasRoomFor(mob, e.getItem()))
                .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        delivering = item == null && !PetBrain.carried(mob).isEmpty();
        return item != null || delivering;
    }

    @Override
    public void start() {
        repath = 0;
        time = 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (!able() || time > 400) return false;
        if (delivering) return owner() != null && !PetBrain.carried(mob).isEmpty();
        return item != null && item.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        time++;
        if (delivering) {
            Player owner = owner();
            if (owner == null) return;
            mob.getLookControl().setLookAt(owner, 30.0f, 30.0f);
            if (--repath <= 0) {
                repath = 10;
                mob.getNavigation().moveTo(owner, 1.2);
            }
            if (mob.distanceToSqr(owner) < 2.5 * 2.5) {
                PetBrain.deliver(mob, owner);
                PetSpeech.say(mob, PetSpeech.Topic.FETCH, false);
            }
            return;
        }
        if (item == null || !item.isAlive()) return;                // picked up; the goal ends next check
        mob.getLookControl().setLookAt(item, 30.0f, 30.0f);
        if (--repath <= 0) {
            repath = 10;
            mob.getNavigation().moveTo(item.getX(), item.getY(), item.getZ(), 1.2);
        }
        if (mob.getBoundingBox().inflate(1.0, 0.5, 1.0).intersects(item.getBoundingBox())) {
            PetBrain.pickUp(mob, item);
            item = null;
        }
    }

    @Override
    public void stop() {
        item = null;
        mob.getNavigation().stop();
    }
}
