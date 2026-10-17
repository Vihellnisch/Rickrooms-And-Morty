package com.rickandmorty.gametest;

import com.rickandmorty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.function.BiConsumer;

/** The v0.10 tour (./gradlew runTourClient -DtourV10=true). */
final class V10Tour {
    private V10Tour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> clean(p));
        stops.accept("lantern_hand", (p, l) -> lantern(p, l, false));
        stops.accept("lantern_walking_hand", (p, l) -> lantern(p, l, true));
        stops.accept("lantern_third_front", (p, l) -> lantern(p, l, false));
        stops.accept("farm_scarecrow", V10Tour::farm);
        stops.accept("level_fun_hall", (p, l) -> fun(p, l, com.rickandmorty.worldgen.level.FunLevel.Room.HALL));
        stops.accept("level_fun_cake_room", (p, l) -> fun(p, l, com.rickandmorty.worldgen.level.FunLevel.Room.CAKE));
        stops.accept("partygoers", V10Tour::partygoers);
        stops.accept("field_guide_screen", (p, l) -> {
            clean(p);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIELD_GUIDE.get()));
        });
    }

    static void clean(ServerPlayer player) {
        player.stopRiding();                                     // the world may have been saved with us sat in something
        com.rickandmorty.cronenberg.Mutation.set(player, 0.0f);
        player.removeAllEffects();
        player.getInventory().clearContent();
    }

    static BlockPos ground(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
    }

    private static void farm(ServerPlayer player, ServerLevel overworld) {
        clean(player);
        overworld.setDayTime(6000);
        BlockPos at = ground(overworld, 2700, 2600).below();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos soil = at.offset(x, 0, z);
                if (x == 0 && z == 0) {
                    overworld.setBlock(soil, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
                    continue;
                }
                overworld.setBlock(soil, net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7), 3);
                overworld.setBlock(soil.above(), net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.CropBlock.AGE, Math.floorMod(x * 3 + z, 8)), 3);
            }
        }
        BlockPos post = at.offset(0, 1, -6);
        overworld.setBlock(post, net.minecraft.world.level.block.Blocks.HAY_BLOCK.defaultBlockState(), 3);
        overworld.setBlock(post.above(), net.minecraft.world.level.block.Blocks.HAY_BLOCK.defaultBlockState(), 3);
        overworld.setBlock(post.above(2), com.rickandmorty.registry.ModBlocks.SCARECROW_HEAD.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
        var scarecrow = com.rickandmorty.entity.FarmScarecrowEntity.tryBuild(overworld, post.above(2), null);
        if (scarecrow != null) scarecrow.setNoAi(true);
        DevTour.go(player, overworld, at.getX() + 0.5, at.getY() + 3, at.getZ() + 6.5, 180.0f, 22.0f);
    }

    private static void fun(ServerPlayer player, ServerLevel overworld, com.rickandmorty.worldgen.level.FunLevel.Room room) {
        clean(player);
        ServerLevel level = overworld.getServer().getLevel(com.rickandmorty.worldgen.level.BackroomsLevel.LEVEL_FUN.key());
        long seed = level.getSeed();
        int cx = 5, cz = 5;
        search:
        for (int r = 0; r < 20; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (com.rickandmorty.worldgen.level.FunLevel.room(seed, 5 + x, 5 + z) == room
                            && (room != com.rickandmorty.worldgen.level.FunLevel.Room.HALL || com.rickandmorty.worldgen.level.FunLevel.room(seed, 6 + x, 5 + z) == room)) {
                        cx = 5 + x;
                        cz = 5 + z;
                        break search;
                    }
                }
            }
        }
        double x0 = cx * 12, z0 = cz * 12;
        if (room == com.rickandmorty.worldgen.level.FunLevel.Room.CAKE) DevTour.go(player, level, x0 + 1.5, 2, z0 + 11.5, -135.0f, 12.0f);
        else DevTour.go(player, level, x0 + 1.5, 2, z0 + 1.5, -50.0f, 4.0f);
    }

    private static void partygoers(ServerPlayer player, ServerLevel overworld) {
        clean(player);
        overworld.setDayTime(6000);
        BlockPos at = ground(overworld, 2800, 2600);
        DevTour.go(player, overworld, at.getX() + 0.5, at.getY(), at.getZ() + 4.5, 180.0f, 0.0f);
        DevTour.LATER.add(java.util.Map.entry(DevTour.ticks + 20, () -> {
            for (int i = 0; i < 2; i++) {
                var partygoer = com.rickandmorty.registry.ModEntities.PARTYGOER.get().create(overworld);
                partygoer.moveTo(at.getX() + (i == 0 ? -0.7 : 1.7), at.getY(), at.getZ() + 0.5, 0.0f, 0.0f);
                partygoer.setNoAi(true);
                overworld.addFreshEntity(partygoer);
                if (i == 1) partygoer.turn(false);
            }
        }));
    }

    private static void lantern(ServerPlayer player, ServerLevel overworld, boolean walking) {
        clean(player);
        overworld.setDayTime(14000);   // dusk, so the glow shows
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCARECROW_LANTERN.get()));
        BlockPos at = ground(overworld, 2600, 2600);
        DevTour.go(player, overworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180.0f, 10.0f);
        player.getAbilities().flying = false;
        if (walking) player.setDeltaMovement(0, 0, -0.2);
    }
}
