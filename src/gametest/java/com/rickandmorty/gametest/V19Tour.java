package com.rickandmorty.gametest;

import com.rickandmorty.pet.AmplifierModule;
import com.rickandmorty.pet.PetBrain;
import com.rickandmorty.pet.PetSpeech;
import com.rickandmorty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** The Cognition Amplifier on all sorts of pets (./gradlew runTourClient -DtourV19=true). */
final class V19Tour {
    private V19Tour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> V10Tour.clean(p));
        stops.accept("amp_wolf", (p, l) -> one(p, l, 0, EntityType.WOLF, AmplifierModule.ALL, false));
        stops.accept("amp_cat", (p, l) -> one(p, l, 1, EntityType.CAT, 0, false));
        stops.accept("amp_parrot", (p, l) -> one(p, l, 2, EntityType.PARROT, AmplifierModule.MEDIC.bit(), false));
        stops.accept("amp_fox", (p, l) -> one(p, l, 3, EntityType.FOX, AmplifierModule.COMBAT.bit(), false));
        stops.accept("amp_horse", (p, l) -> one(p, l, 4, EntityType.HORSE, AmplifierModule.GUARDIAN.bit(), false));
        stops.accept("amp_llama", (p, l) -> one(p, l, 5, EntityType.LLAMA, 0, false));
        stops.accept("amp_camel", (p, l) -> one(p, l, 6, EntityType.CAMEL, 0, false));
        stops.accept("amp_puppy", (p, l) -> one(p, l, 7, EntityType.WOLF, AmplifierModule.FETCH.bit(), true));
        stops.accept("amp_wolf_from_behind", V19Tour::back);
        stops.accept("amp_lineup", V19Tour::lineup);
        stops.accept("amp_talking_hand", V19Tour::talking);
    }

    private static BlockPos spot(ServerLevel level, int slot) {
        BlockPos at = V10Tour.ground(level, 7000 + slot * 30, 5000 + (int) (System.currentTimeMillis() / 1000 % 3000) * 48);
        level.getEntitiesOfClass(Mob.class, new AABB(at).inflate(20)).forEach(Mob::discard);
        return at;
    }

    /** A pet of this kind, tamed by the player, helmet on, standing still facing south (+z). */
    private static Mob pet(ServerPlayer player, ServerLevel level, EntityType<? extends Mob> type, double x, double y, double z, int modules, boolean baby) {
        Mob mob = type.create(level);
        mob.moveTo(x, y, z, 180.0f, 0.0f);
        mob.setYHeadRot(180.0f);
        mob.setYBodyRot(180.0f);
        if (baby && mob instanceof net.minecraft.world.entity.AgeableMob ageable) ageable.setBaby(true);
        if (mob instanceof TamableAnimal tame) tame.tame(player);
        if (mob instanceof AbstractHorse horse) horse.tameWithName(player);
        if (mob instanceof Fox fox) fox.addTrustedUUID(player.getUUID());
        mob.setNoAi(true);
        level.addFreshEntity(mob);
        PetBrain.fit(mob, player, modules);
        return mob;
    }

    private static void one(ServerPlayer player, ServerLevel level, int slot, EntityType<? extends Mob> type, int modules, boolean baby) {
        V10Tour.clean(player);
        BlockPos at = spot(level, slot);
        Mob mob = pet(player, level, type, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, modules, baby);
        double size = Math.max(mob.getBbHeight(), mob.getBbWidth());
        double head = mob.getBbHeight() * 0.85, back = 0.7 + size * 1.25;
        // from the front left, a little above the head
        look(player, level, at.getX() + 0.5 + back * 0.55, at.getY() + head + 0.35 + size * 0.25, at.getZ() + 0.5 - back * 0.85,
                at.getX() + 0.5, at.getY() + head, at.getZ() + 0.5);
    }

    /** From behind, where the module lights are (all four on). */
    private static void back(ServerPlayer player, ServerLevel level) {
        V10Tour.clean(player);
        BlockPos at = spot(level, 8);
        pet(player, level, EntityType.WOLF, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, AmplifierModule.ALL, false);
        look(player, level, at.getX() - 0.6, at.getY() + 1.6, at.getZ() + 2.0, at.getX() + 0.5, at.getY() + 0.75, at.getZ() + 0.2);
    }

    private static void lineup(ServerPlayer player, ServerLevel level) {
        V10Tour.clean(player);
        BlockPos at = spot(level, 9);
        List<EntityType<? extends Mob>> types = List.of(EntityType.PARROT, EntityType.CAT, EntityType.WOLF, EntityType.FOX, EntityType.LLAMA, EntityType.HORSE);
        for (int i = 0; i < types.size(); i++) {
            pet(player, level, types.get(i), at.getX() + 0.5 + (i - 2.5) * 1.8, at.getY(), at.getZ() + 0.5, i * 3 % 16, false);
        }
        look(player, level, at.getX() + 0.5, at.getY() + 2.2, at.getZ() - 6.0, at.getX() + 0.5, at.getY() + 0.9, at.getZ() + 0.5);
    }

    /** Holding a spare helmet, while a cat in one tells you what it thinks. */
    private static void talking(ServerPlayer player, ServerLevel level) {
        V10Tour.clean(player);
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(ModItems.COGNITION_AMPLIFIER.get()));
        BlockPos at = spot(level, 10);
        Mob cat = pet(player, level, EntityType.CAT, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, AmplifierModule.GUARDIAN.bit(), false);
        look(player, level, at.getX() + 0.5, at.getY() + 1.5, at.getZ() - 2.6, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5);
        DevTour.LATER.add(Map.entry(DevTour.ticks + 70, () -> PetSpeech.say(cat, PetSpeech.Topic.IDLE, true)));
    }

    private static void look(ServerPlayer player, ServerLevel level, double ex, double ey, double ez, double tx, double ty, double tz) {
        double dx = tx - ex, dy = ty - ey, dz = tz - ez;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        DevTour.go(player, level, ex, ey - 1.62, ez, yaw, pitch);
    }
}
