package com.rickandmorty.gametest;

import com.rickandmorty.entity.StillLifeEntity;
import com.rickandmorty.entity.StillLifeForm;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.DeadMallLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.function.BiConsumer;

/** The Still Lifes of people, lined up in the Dead Mall (./gradlew runTourClient -DtourV17=true). */
final class V17Tour {
    private V17Tour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> V10Tour.clean(p));
        stops.accept("still_lifes_lineup", (p, l) -> scene(p, l, null, false, 14.0));
        for (StillLifeForm form : StillLifeForm.values()) {
            stops.accept("still_life_" + form.name().toLowerCase(), (p, l) -> scene(p, l, form, false, form == StillLifeForm.CAPTAIN ? 5.0 : 3.6));
        }
        stops.accept("still_life_captain_awake", (p, l) -> scene(p, l, StillLifeForm.CAPTAIN, true, 5.5));
        stops.accept("still_life_many_hands_awake", (p, l) -> scene(p, l, StillLifeForm.MANY_HANDS, true, 3.6));
        stops.accept("still_life_noodle_arms_awake", (p, l) -> scene(p, l, StillLifeForm.NOODLE_ARMS, true, 4.0));
        stops.accept("still_life_gaping_awake", (p, l) -> scene(p, l, StillLifeForm.GAPING, true, 3.2));
    }

    /** In a wide stretch of mall concourse: one form (or all five in a row) a little way in front of you. */
    private static void scene(ServerPlayer player, ServerLevel overworld, StillLifeForm only, boolean awake, double distance) {
        V10Tour.clean(player);
        ServerLevel level = overworld.getServer().getLevel(BackroomsLevel.DEAD_MALL.key());
        long seed = level.getSeed();
        double[] p = null;
        for (int i = 70; i < 120 && p == null; i++) {
            if (!DeadMallLevel.edge(seed, i, 70, 0)) continue;
            double[] q = DeadMallLevel.edgePoint(seed, i, 70, 0, 0.5);
            if (q[4] >= 8) p = q;
        }
        if (p == null) return;
        double fx = p[2], fz = p[3], sx = -fz, sz = fx;                  // forward along the concourse, and across it
        double ex = p[0] - fx * distance, ez = p[1] - fz * distance;
        level.getEntitiesOfClass(StillLifeEntity.class, new AABB(p[0] - 20, 0, p[1] - 20, p[0] + 20, 32, p[1] + 20)).forEach(StillLifeEntity::discard);
        float face = (float) Math.toDegrees(Math.atan2(fx, -fz));          // turned towards the camera
        int y = DeadMallLevel.LOW;
        StillLifeForm[] forms = only == null ? StillLifeForm.values() : new StillLifeForm[]{only};
        for (int k = 0; k < forms.length; k++) {
            double off = (k - (forms.length - 1) / 2.0) * 1.6;
            StillLifeEntity e = ModEntities.STILL_LIVES.get(forms[k]).get().create(level);
            e.moveTo(p[0] + sx * off, y, p[1] + sz * off, face, 0.0f);
            e.setYHeadRot(face);
            e.setYBodyRot(face);
            e.setNoAi(true);
            if (awake) e.wake();
            e.setPersistenceRequired();
            level.addFreshEntity(e);
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-fx, fz));
        DevTour.go(player, level, ex, y, ez, yaw, only == StillLifeForm.CAPTAIN ? -12.0f : 2.0f);
    }
}
