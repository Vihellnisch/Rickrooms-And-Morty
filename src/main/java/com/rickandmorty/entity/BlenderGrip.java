package com.rickandmorty.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/** What the blender machinery (blender portals and evil Warp Gateways) does to whatever it catches. */
public final class BlenderGrip {
    private BlenderGrip() {
    }

    /** Whether something is fair game: alive, not its owner (or the owner's pets), not a creative/spectator player. */
    public static boolean canBlend(LivingEntity e, @Nullable UUID owner) {
        if (!e.isAlive() || e.isSpectator() || e.isRemoved() || e instanceof ArmorStand) return false;
        if (e.getUUID().equals(owner)) return false;
        if (e instanceof TamableAnimal pet && owner != null && owner.equals(pet.getOwnerUUID())) return false;
        return !(e instanceof Player player) || (!player.isCreative() && !player.isSpectator());
    }

    /** Where a victim is held: just out in front of the ring, on whichever side they came from. */
    public static Vec3 holdPoint(Vec3 center, Vec3 normal, boolean flat, Entity victim) {
        double side = victim.position().add(0, victim.getBbHeight() / 2, 0).subtract(center).dot(normal) >= 0 ? 1.0 : -1.0;
        double out = 0.9 + victim.getBbWidth() * 0.5;
        Vec3 held = center.add(normal.scale(side * out));
        return flat ? held.add(0, side > 0 ? 0.1 : -victim.getBbHeight(), 0) : held.subtract(0, victim.getBbHeight() * 0.45, 0);
    }

    /** Pull the victim towards {@code target} ({@code grip} 0..1) and keep them there, shaking if {@code shake}. */
    public static void hold(LivingEntity victim, Vec3 target, float grip, boolean shake, RandomSource random) {
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 9, false, false));
        victim.resetFallDistance();
        if (victim.getBbWidth() > 1.6f) return;     // too big to lift; the arms hold on where it stands
        if (shake) target = target.add((random.nextDouble() - 0.5) * 0.12, (random.nextDouble() - 0.5) * 0.12, (random.nextDouble() - 0.5) * 0.12);
        moveTowards(victim, target, 0.15f + 0.6f * grip);
    }

    /** Moves an entity part of the way to a point (players are teleported, so the client can't fight it). */
    public static void moveTowards(Entity victim, Vec3 target, float fraction) {
        Vec3 now = victim.position();
        Vec3 next = now.add(target.subtract(now).scale(fraction));
        victim.setDeltaMovement(Vec3.ZERO);
        if (victim instanceof ServerPlayer player) {
            player.connection.teleport(next.x, next.y, next.z, 0.0f, 0.0f, EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT));
        } else {
            victim.setPos(next.x, next.y, next.z);
            if (victim instanceof Mob mob) mob.getNavigation().stop();
        }
    }
}
