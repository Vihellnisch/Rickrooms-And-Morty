package com.rickandmorty.gametest;

import com.rickandmorty.entity.MimicKind;
import com.rickandmorty.entity.PortalEntity;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

/**
 * Close-ups of the things shader packs tend to break (./gradlew runTourClient -DtourShaders=true,
 * with Iris, Sodium and a shader pack in run-tour/mods and run-tour/shaderpacks).
 * Stops ending in "_hand" keep the HUD so the held gun is drawn.
 */
final class ShaderTour {
    private ShaderTour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        // shader packs compile for a while on the first frames; give the client time to catch up
        stops.accept("shader_warmup", (p, l) -> clean(p));
        stops.accept("shader_warmup_2", (p, l) -> clean(p));
        stops.accept("shader_portal", (p, l) -> portal(p, l, false));
        stops.accept("shader_portal_behind", (p, l) -> portal(p, l, true));
        stops.accept("shader_still_life", ShaderTour::stillLifeOverworld);
        stops.accept("shader_still_life_backrooms", ShaderTour::stillLifeBackrooms);
        stops.accept("shader_gun_hand", (p, l) -> gun(p, l, false));
        stops.accept("shader_gun_attack_hand", (p, l) -> gun(p, l, true));
        stops.accept("dial_every_level_screen", ShaderTour::dial);
        stops.accept("shader_gateway", ShaderTour::gateway);
    }

    private static BlockPos ground(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    private static void clean(ServerPlayer player) {
        com.rickandmorty.cronenberg.Mutation.set(player, 0.0f);
        player.removeAllEffects();
        player.getInventory().clearContent();
    }

    private static void portal(ServerPlayer player, ServerLevel overworld, boolean behind) {
        clean(player);
        BlockPos at = ground(overworld, behind ? 440 : 400, 400);
        Vec3 center = Vec3.atBottomCenterOf(at).add(0, 1.2, 0);
        if (behind) DevTour.go(player, overworld, center.x, at.getY(), center.z - 4.5, 0.0f, 0.0f);
        else DevTour.go(player, overworld, center.x, at.getY(), center.z + 4.5, 180.0f, 0.0f);
        later(30, () -> PortalEntity.spawnInAir(overworld, center, 0.0f, null, player, false));
    }

    private static void later(int delay, Runnable task) {
        DevTour.LATER.add(java.util.Map.entry(DevTour.ticks + delay, task));
    }

    private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, double x, double y, double z, float yaw) {
        T mob = type.create(level);
        mob.moveTo(x, y, z, yaw, 0.0f);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        return mob;
    }

    private static void stillLifeOverworld(ServerPlayer player, ServerLevel overworld) {
        clean(player);
        BlockPos at = ground(overworld, 480, 400);
        DevTour.go(player, overworld, at.getX() + 1.5, at.getY() + 1.5, at.getZ() + 4.5, 180.0f, 25.0f);
        later(30, () -> {
            spawn(overworld, ModEntities.MIMICS.get(MimicKind.VILLAGER).get(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f);
            spawn(overworld, EntityType.VILLAGER, at.getX() + 2.5, at.getY(), at.getZ() + 0.5, 0.0f);   // a real one for comparison
        });
    }

    private static void stillLifeBackrooms(ServerPlayer player, ServerLevel overworld) {
        clean(player);
        ServerLevel level = overworld.getServer().getLevel(ModDimensions.BACKROOMS);
        var d = ModDimensions.randomDestination(level.getServer(), ModDimensions.BACKROOMS, level.random).orElseThrow();
        BlockPos feet = BlockPos.containing(d.position());
        // a small cleared room so both fit in view
        for (int dx = -1; dx <= 3; dx++) {
            for (int dz = -1; dz <= 5; dz++) {
                for (int y = 0; y <= 2; y++) level.setBlock(feet.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(feet.offset(dx, -1, dz), com.rickandmorty.registry.ModBlocks.BACKROOMS_CARPET.get().defaultBlockState(), 3);
            }
        }
        spawn(level, ModEntities.MIMICS.get(MimicKind.VILLAGER).get(), feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0f);
        spawn(level, EntityType.VILLAGER, feet.getX() + 2.5, feet.getY(), feet.getZ() + 0.5, 0.0f);
        DevTour.go(player, level, feet.getX() + 1.5, feet.getY() + 0.8, feet.getZ() + 4.0, 180.0f, 30.0f);
    }

    /** A linked, active Warp Gateway (it draws the same vortex as the portals). */
    private static void gateway(ServerPlayer player, ServerLevel overworld) {
        clean(player);
        BlockPos at = ground(overworld, 640, 400);
        BlockPos a = at, b = at.east(12);
        for (BlockPos pos : new BlockPos[]{a, b}) {
            var state = com.rickandmorty.registry.ModBlocks.WARP_GATEWAY.get().defaultBlockState()
                    .setValue(com.rickandmorty.block.WarpGatewayBlock.FACING, net.minecraft.core.Direction.SOUTH);
            overworld.setBlock(pos, state, 3);
            state.getBlock().setPlacedBy(overworld, pos, state, null, new ItemStack(ModItems.WARP_GATEWAY.get()));
        }
        com.rickandmorty.block.entity.WarpGatewayBlockEntity.link(overworld.getServer(), net.minecraft.core.GlobalPos.of(overworld.dimension(), a),
                net.minecraft.core.GlobalPos.of(overworld.dimension(), b));
        DevTour.go(player, overworld, at.getX() + 0.5, at.getY(), at.getZ() + 6.5, 180.0f, -8.0f);
    }

    /** A gun that knows every level, with the dial open (DevTourClient opens it for "_screen" stops). */
    private static void dial(ServerPlayer player, ServerLevel overworld) {
        clean(player);
        BlockPos at = ground(overworld, 600, 400);
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        var dial = com.rickandmorty.item.PortalDestinations.EMPTY.withKnownDimension(ModDimensions.CRONENBERG).withKnownDimension(ModDimensions.BACKROOMS);
        for (var level : com.rickandmorty.worldgen.level.BackroomsLevel.values()) {
            if (level != com.rickandmorty.worldgen.level.BackroomsLevel.LEVEL_0) dial = dial.withLearnedDimension(level.key());
        }
        gun.set(ModDataComponents.PORTAL_DESTINATIONS.get(), dial.withSelectedDimension(1));
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        DevTour.go(player, overworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 20.0f);
    }

    private static void gun(ServerPlayer player, ServerLevel overworld, boolean attack) {
        clean(player);
        BlockPos at = ground(overworld, 560, 400);
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        gun.set(ModDataComponents.PORTAL_CHARGES.get(), PortalGunItem.MAX_CHARGES);
        if (attack) {
            gun.set(ModDataComponents.BLENDER.get(), true);
            gun.set(ModDataComponents.ATTACK_MODE.get(), true);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        DevTour.go(player, overworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 20.0f);
    }
}
