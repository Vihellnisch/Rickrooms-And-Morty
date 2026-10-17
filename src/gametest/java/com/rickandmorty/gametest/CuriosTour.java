package com.rickandmorty.gametest;

import com.rickandmorty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The goggles in Curios' head slot, over a helmet (./gradlew runTourClient -DtourCurios=true, with the
 * Curios jar from libs/ copied into run-tour/mods).
 */
final class CuriosTour {
    private CuriosTour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> V10Tour.clean(p));
        stops.accept("curios_goggles_over_helmet_front", (p, l) -> wear(p, l, 0, true));
        stops.accept("curios_goggles_over_iron_front", (p, l) -> wear(p, l, 1, false));
        stops.accept("curios_goggles_no_helmet_front", (p, l) -> wear(p, l, 2, null));
        stops.accept("curios_goggles_xray", CuriosTour::xray);
    }

    private static BlockPos spot(ServerLevel level, int slot) {
        return V10Tour.ground(level, 9000 + slot * 30, 5000 + (int) (System.currentTimeMillis() / 1000 % 3000) * 48);
    }

    private static void putOn(ServerPlayer player) {
        var handler = CuriosApi.getCuriosInventory(player).orElseThrow().getStacksHandler("head").orElseThrow();
        handler.getStacks().setStackInSlot(0, new ItemStack(ModItems.INTERDIMENSIONAL_GOGGLES.get()));
    }

    /** Standing on the ground in a helmet (diamond, iron, or none: null), goggles in the curio slot. */
    private static void wear(ServerPlayer player, ServerLevel level, int slot, Boolean diamond) {
        V10Tour.clean(player);
        level.setDayTime(6000);
        BlockPos at = spot(level, slot);
        if (diamond != null) player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(diamond ? Items.DIAMOND_HELMET : Items.IRON_HELMET));
        putOn(player);
        DevTour.go(player, level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 0.0f);
        DevTour.LATER.add(Map.entry(DevTour.ticks + 20, () -> {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }));
    }

    /** Ores buried in stone, seen from above: the x-ray works from the curio slot too. */
    private static void xray(ServerPlayer player, ServerLevel level) {
        V10Tour.clean(player);
        level.setDayTime(6000);
        BlockPos at = spot(level, 3);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        putOn(player);
        var ores = new net.minecraft.world.level.block.Block[]{Blocks.DIAMOND_ORE, Blocks.GOLD_ORE, Blocks.IRON_ORE, Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE,
                Blocks.EMERALD_ORE, Blocks.COAL_ORE, Blocks.COPPER_ORE};
        var random = level.random;
        for (int x = -7; x <= 7; x++) {
            for (int z = -10; z <= 4; z++) {
                for (int y = -6; y <= 0; y++) {
                    level.setBlock(at.offset(x, y, z), y < -2 && random.nextFloat() < 0.07f ? ores[random.nextInt(ores.length)].defaultBlockState()
                            : Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        DevTour.go(player, level, at.getX() + 0.5, at.getY() + 4, at.getZ() + 4.5, 180.0f, 40.0f);
        DevTour.LATER.add(Map.entry(DevTour.ticks + 100, () -> DevTour.LOGGER.info("TOUR curios night vision: {}",
                player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))));
    }
}
