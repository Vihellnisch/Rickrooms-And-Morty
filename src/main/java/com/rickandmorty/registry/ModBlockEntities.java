package com.rickandmorty.registry;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.entity.MicroverseBatteryBlockEntity;
import com.rickandmorty.block.entity.WarpGatewayBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RickAndMortyMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarpGatewayBlockEntity>> WARP_GATEWAY =
            BLOCK_ENTITIES.register("warp_gateway", () -> BlockEntityType.Builder
                    .of(WarpGatewayBlockEntity::new, ModBlocks.WARP_GATEWAY.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.rickandmorty.block.entity.ExitDoorBlockEntity>> EXIT_DOOR =
            BLOCK_ENTITIES.register("exit_door", () -> BlockEntityType.Builder
                    .of(com.rickandmorty.block.entity.ExitDoorBlockEntity::new, ModBlocks.EXIT_DOOR.get(), ModBlocks.STORE_DOOR.get(), ModBlocks.LONE_DOOR.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MicroverseBatteryBlockEntity>> MICROVERSE_BATTERY =
            BLOCK_ENTITIES.register("microverse_battery", () -> BlockEntityType.Builder
                    .of(MicroverseBatteryBlockEntity::new, ModBlocks.MICROVERSE_BATTERY.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.rickandmorty.block.entity.StockCrateBlockEntity>> STOCK_CRATE =
            BLOCK_ENTITIES.register("stock_crate", () -> BlockEntityType.Builder
                    .of(com.rickandmorty.block.entity.StockCrateBlockEntity::new, ModBlocks.STOCK_CRATE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.rickandmorty.block.entity.FoodCounterBlockEntity>> FOOD_COUNTER =
            BLOCK_ENTITIES.register("food_counter", () -> BlockEntityType.Builder
                    .of(com.rickandmorty.block.entity.FoodCounterBlockEntity::new, ModBlocks.FOOD_COUNTER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.rickandmorty.gadget.TimeCrystalBlockEntity>> TIME_CRYSTAL =
            BLOCK_ENTITIES.register("time_crystal", () -> BlockEntityType.Builder
                    .of(com.rickandmorty.gadget.TimeCrystalBlockEntity::new, ModBlocks.TIME_CRYSTAL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.rickandmorty.gadget.ShrinkRayBlockEntity>> SHRINK_RAY =
            BLOCK_ENTITIES.register("shrink_ray", () -> BlockEntityType.Builder
                    .of(com.rickandmorty.gadget.ShrinkRayBlockEntity::new, ModBlocks.SHRINK_RAY.get())
                    .build(null));
}
