package com.rickandmorty.gametest;

import com.mojang.logging.LogUtils;
import com.rickandmorty.entity.cruiser.CruiserWeapons;
import com.rickandmorty.entity.cruiser.SpaceCruiserEntity;
import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;

import java.util.Map;
import java.util.function.BiConsumer;

/** The cruiser fixes tour (./gradlew runTourClient -DtourV12=true): glass over water, the booster, portal jumps. */
final class V12Tour {
    private static final Logger LOGGER = LogUtils.getLogger();

    private V12Tour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> V10Tour.clean(p));
        stops.accept("cruiser_over_water_hand", V12Tour::overWater);
        stops.accept("cruiser_booster_hand", V12Tour::booster);
        stops.accept("cruiser_portal_jump_back", (p, l) -> jump(p, l, Level.OVERWORLD, 9));
        stops.accept("cruiser_portal_dimension_back", (p, l) -> jump(p, l, ModDimensions.CRONENBERG, 10));
    }

    /** From the pilot's seat, looking through the dome at a lake. */
    private static void overWater(ServerPlayer player, ServerLevel overworld) {
        SpaceCruiserEntity cruiser = V11Tour.ride(player, overworld, 11, false);
        BlockPos at = cruiser.blockPosition();
        for (int x = -14; x <= 14; x++) {
            for (int z = -22; z <= -3; z++) {
                BlockPos top = V10Tour.ground(overworld, at.getX() + x, at.getZ() + z).below();
                overworld.setBlock(top, Blocks.WATER.defaultBlockState(), 2);
                overworld.setBlock(top.below(), Blocks.WATER.defaultBlockState(), 2);
            }
        }
        for (int i = 0; i < 5; i++) overworld.setBlock(at.offset(-6 + i * 3, 0, -10).below(), Blocks.LILY_PAD.defaultBlockState(), 2);
        DevTour.LATER.add(Map.entry(DevTour.ticks + 8, () -> player.connection.teleport(player.getX(), player.getY(), player.getZ(), 180.0f, 28.0f)));
    }

    /** The HUD with a Mk III booster fitted. */
    private static void booster(ServerPlayer player, ServerLevel overworld) {
        SpaceCruiserEntity cruiser = V11Tour.ride(player, overworld, 12, true);
        cruiser.setBooster(SpaceCruiserEntity.MAX_BOOSTER);
        cruiser.setPower(15000);
    }

    /** Flying along, then the portal drive: does the cruiser (and the pilot in it) really end up there? */
    private static void jump(ServerPlayer player, ServerLevel overworld, ResourceKey<Level> dimension, int slot) {
        SpaceCruiserEntity cruiser = V11Tour.ride(player, overworld, slot, true);
        ServerLevel target = overworld.getServer().getLevel(dimension);
        int x = cruiser.getBlockX() + 900, z = cruiser.getBlockZ() + 300;
        BlockPos ground = V10Tour.ground(target, x, z);
        // a gold tower to show where it lands
        for (int y = 0; y < 12; y++) target.setBlock(ground.offset(0, y, -9), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        gun.set(ModDataComponents.PORTAL_DESTINATIONS.get(), PortalDestinations.EMPTY.withAdded(new PortalDestinations.Destination("tour",
                dimension, x + 0.5, ground.getY() + 4, z + 0.5, 180.0f)).withSelected(0));
        player.getInventory().add(gun);
        DevTour.LATER.add(Map.entry(DevTour.ticks + 30, () -> CruiserWeapons.portalJump(cruiser, player)));
        DevTour.LATER.add(Map.entry(DevTour.ticks + 115, () -> {
            var vehicle = player.getVehicle();
            LOGGER.info("TOUR portal jump to {}: player at {} in {}, riding {} at {}; wanted {} {}", dimension.location(), player.position(),
                    player.level().dimension().location(), vehicle, vehicle == null ? null : vehicle.position(), x, z);
        }));
    }
}
