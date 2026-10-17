package com.rickandmorty.gametest;

import com.rickandmorty.gadget.FreezeRayItem;
import com.rickandmorty.gadget.ShrinkRayBlock;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.function.BiConsumer;

/** Rick's gadgets (./gradlew runTourClient -DtourV18=true). */
final class V18Tour {
    private V18Tour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> V10Tour.clean(p));
        stops.accept("shrink_ray", (p, l) -> ray(p, l, 0, false, 0));
        stops.accept("shrink_ray_with_lens", (p, l) -> ray(p, l, 1, true, 0));
        stops.accept("shrink_ray_villager_underneath", (p, l) -> ray(p, l, 8, false, 2));
        stops.accept("shrink_ray_firing", (p, l) -> ray(p, l, 9, false, 3));
        stops.accept("shrink_ray_shrunk_front", (p, l) -> ray(p, l, 2, false, 1));
        stops.accept("shrink_ray_enlarged_front", (p, l) -> ray(p, l, 3, true, 1));
        stops.accept("freeze_ray_hand", V18Tour::freeze);
        stops.accept("time_crystal", V18Tour::crystal);
        stops.accept("goggles_worn_front", (p, l) -> goggles(p, l, false));
        stops.accept("goggles_xray", (p, l) -> goggles(p, l, true));
    }

    private static void look(ServerPlayer player, ServerLevel level, double ex, double ey, double ez, double tx, double ty, double tz) {
        double dx = tx - ex, dy = ty - ey, dz = tz - ez;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        DevTour.go(player, level, ex, ey - 1.62, ez, yaw, pitch);
    }

    private static BlockPos spot(ServerLevel level, int slot) {
        // fresh ground every run (the tour world keeps what earlier runs built)
        BlockPos at = V10Tour.ground(level, 5000 + slot * 30, 5000 + (int) (System.currentTimeMillis() / 1000 % 3000) * 48);
        level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(at).inflate(16)).forEach(net.minecraft.world.entity.Mob::discard);
        return at;
    }

    /** A shrink ray facing north; {@code use} 1: the player stands under it and it fires. */
    private static void ray(ServerPlayer player, ServerLevel level, int slot, boolean lens, int use) {
        V10Tour.clean(player);
        level.setDayTime(6000);
        BlockPos at = spot(level, slot);
        BlockState ray = ModBlocks.SHRINK_RAY.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(ShrinkRayBlock.UPGRADED, lens).setValue(ShrinkRayBlock.GROW, lens);
        level.setBlock(at, ray, 3);
        if (use >= 2) {
            // someone full size under the emitter, seen from the side; and (3) the beam as it fires
            var villager = EntityType.VILLAGER.create(level);
            villager.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 0.5, 90.0f, 0.0f);
            villager.setNoAi(true);
            level.addFreshEntity(villager);
            look(player, level, at.getX() + 6.5, at.getY() + 2.2, at.getZ() - 0.2, at.getX() + 0.5, at.getY() + 1.6, at.getZ() - 0.2);
            if (use == 3) {
                for (int t = 60; t <= 115; t += 31) DevTour.LATER.add(Map.entry(DevTour.ticks + t, () -> ShrinkRayBlock.fire(level, at, level.getBlockState(at))));
            }
            return;
        }
        if (use == 0) {
            look(player, level, at.getX() + 3.6, at.getY() + 1.9, at.getZ() - 2.6, at.getX() + 0.5, at.getY() + 0.9, at.getZ());
            return;
        }
        BlockPos under = at.north();
        DevTour.go(player, level, under.getX() + 0.5, under.getY(), under.getZ() + 0.5, 180.0f, 0.0f);
        DevTour.LATER.add(Map.entry(DevTour.ticks + 20, () -> {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }));
        DevTour.LATER.add(Map.entry(DevTour.ticks + 40, () -> ShrinkRayBlock.fire(level, at, level.getBlockState(at))));
        // a zombie beside, for scale
        var zombie = EntityType.VILLAGER.create(level);
        zombie.moveTo(under.getX() - 1.5, under.getY(), under.getZ() + 0.5, 180.0f, 0.0f);
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
    }

    private static void freeze(ServerPlayer player, ServerLevel level) {
        V10Tour.clean(player);
        level.setDayTime(6000);
        BlockPos at = spot(level, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FREEZE_RAY.get()));
        var zombie = EntityType.HUSK.create(level);
        zombie.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 2.5, 0.0f, 0.0f);
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
        DevTour.go(player, level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180.0f, 8.0f);
        DevTour.LATER.add(Map.entry(DevTour.ticks + 30, () -> FreezeRayItem.freeze(zombie, player, Items.AIR)));
        for (int t = 30; t < 130; t += 20) DevTour.LATER.add(Map.entry(DevTour.ticks + t, () -> {
            if (!zombie.hasEffect(com.rickandmorty.registry.ModEffects.FROZEN)) FreezeRayItem.freeze(zombie, player, Items.AIR);
        }));
    }

    /** A time crystal among furnaces and a little wheat field. */
    private static void crystal(ServerPlayer player, ServerLevel level) {
        V10Tour.clean(player);
        level.setDayTime(6000);
        BlockPos at = spot(level, 5);
        level.setBlock(at, ModBlocks.TIME_CRYSTAL.get().defaultBlockState(), 3);
        for (int i = -1; i <= 1; i++) {
            BlockPos furnace = at.offset(i, 0, -2);
            level.setBlock(furnace, Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.SOUTH)
                    .setValue(AbstractFurnaceBlock.LIT, true), 3);
            if (level.getBlockEntity(furnace) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity f) {
                f.setItem(0, new ItemStack(Items.RAW_IRON, 64));
                f.setItem(1, new ItemStack(Items.COAL, 64));
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = 2; z <= 3; z++) {
                level.setBlock(at.offset(x, -1, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
                level.setBlock(at.offset(x, 0, z), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3), 3);
            }
        }
        look(player, level, at.getX() + 1.6, at.getY() + 1.0, at.getZ() + 1.4, at.getX() + 0.5, at.getY() + 0.35, at.getZ() + 0.5);
    }

    private static void goggles(ServerPlayer player, ServerLevel level, boolean xray) {
        V10Tour.clean(player);
        level.setDayTime(6000);
        BlockPos at = spot(level, xray ? 7 : 6);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.INTERDIMENSIONAL_GOGGLES.get()));
        if (!xray) {
            DevTour.go(player, level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 0.0f);
            DevTour.LATER.add(Map.entry(DevTour.ticks + 20, () -> {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }));
            return;
        }
        // ores buried in a slab of stone, seen from above through it
        var ores = new net.minecraft.world.level.block.Block[]{Blocks.DIAMOND_ORE, Blocks.GOLD_ORE, Blocks.IRON_ORE, Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE,
                Blocks.EMERALD_ORE, Blocks.COAL_ORE, Blocks.COPPER_ORE};
        var random = level.random;
        for (int x = -7; x <= 7; x++) {
            for (int z = -10; z <= 4; z++) {
                for (int y = -6; y <= 0; y++) {
                    BlockPos p = at.offset(x, y, z);
                    level.setBlock(p, y < -2 && random.nextFloat() < 0.07f ? ores[random.nextInt(ores.length)].defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        DevTour.go(player, level, at.getX() + 0.5, at.getY() + 4, at.getZ() + 4.5, 180.0f, 40.0f);
    }
}
