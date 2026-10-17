package com.rickandmorty.registry;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RickAndMortyMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rickrooms"))
                    .icon(() -> new ItemStack(ModItems.PORTAL_GUN.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.PORTAL_GUN.get());
                        output.accept(ModItems.PORTAL_FLUID.get());
                        output.accept(ModItems.TARGETING_CHIP.get());
                        output.accept(ModItems.WARP_GATEWAY.get());
                        output.accept(ModItems.JOURNAL_PAGE_1.get());
                        output.accept(ModItems.JOURNAL_PAGE_2.get());
                        output.accept(ModItems.JOURNAL_PAGE_3.get());
                        output.accept(ModItems.BACKROOMS_COORDINATES.get());
                        output.accept(ModItems.PORTAL_FLUID_DROP.get());
                        output.accept(ModItems.BACTERIA_SPAWN_EGG.get());
                        ModItems.MIMIC_SPAWN_EGGS.values().forEach(egg -> output.accept(egg.get()));
                        ModItems.STILL_LIFE_SPAWN_EGGS.values().forEach(egg -> output.accept(egg.get()));
                        output.accept(ModItems.MEESEEKS_BOX.get());
                        output.accept(ModItems.PLUMBUS.get());
                        output.accept(ModItems.RICKS_FLASK.get());
                        output.accept(ModItems.SZECHUAN_SAUCE.get());
                        output.accept(ModItems.SLIDE.get());
                        output.accept(ModItems.ALMOND_WATER.get());
                        output.accept(ModItems.MICROVERSE_BATTERY.get());
                        output.accept(ModItems.MICROVERSE_WIRE.get());
                        for (var potion : java.util.List.of(ModEffects.PICKLE_RICK_POTION, ModEffects.LONG_PICKLE_RICK_POTION)) {
                            output.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, potion));
                            output.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.SPLASH_POTION, potion));
                        }
                        // Cronenberg World
                        output.accept(ModItems.BLENDER_CHIP.get());
                        output.accept(blenderGun());
                        output.accept(ModItems.DNA_LOCK_CHIP.get());
                        output.accept(com.rickandmorty.item.PortalGunItem.fullyUpgraded(null));
                        output.accept(ModItems.RICK_NOTES_1.get());
                        output.accept(ModItems.RICK_NOTES_2.get());
                        output.accept(ModItems.RICK_NOTES_3.get());
                        output.accept(ModItems.CRONENBERG_COORDINATES.get());
                        output.accept(ModItems.CRONENBERG_FLESH.get());
                        output.accept(ModItems.COOKED_CRONENBERG_FLESH.get());
                        output.accept(ModItems.MUTAGEN_SAC.get());
                        output.accept(ModItems.BONE_PLATE.get());
                        output.accept(ModItems.CURE_SERUM.get());
                        output.accept(ModItems.BEHEMOTH_HEART.get());
                        output.accept(ModItems.MANTIS_SCYTHE.get());
                        output.accept(ModItems.BONE_PLATE_HELMET.get());
                        output.accept(ModItems.BONE_PLATE_CHESTPLATE.get());
                        output.accept(ModItems.BONE_PLATE_LEGGINGS.get());
                        output.accept(ModItems.BONE_PLATE_BOOTS.get());
                        output.accept(ModItems.SURVIVOR_HELMET.get());
                        output.accept(ModItems.SURVIVOR_CHESTPLATE.get());
                        output.accept(ModItems.SURVIVOR_LEGGINGS.get());
                        output.accept(ModItems.SURVIVOR_BOOTS.get());
                        ModItems.CRONENBERG_SPAWN_EGGS.values().forEach(egg -> output.accept(egg.get()));
                        output.accept(ModItems.BEHEMOTH_SPAWN_EGG.get());
                        output.accept(ModItems.MANTIS_SPAWN_EGG.get());
                        ModItems.SURVIVOR_SPAWN_EGGS.values().forEach(egg -> output.accept(egg.get()));
                        output.accept(ModItems.COUNCIL_SPAWN_EGG.get());
                        for (var block : java.util.List.of(ModItems.FLESH, ModItems.VEINED_FLESH, ModItems.ROTTING_FLESH, ModItems.NECROTIC_FLESH,
                                ModItems.TUMOR, ModItems.WATCHING_FLESH, ModItems.BLOOD_CLOT, ModItems.CALCIFIED_BONE, ModItems.PUSTULE,
                                ModItems.TOOTH_SPIKE, ModItems.FLESH_GRASS, ModItems.EYESTALK)) {
                            output.accept(block.get());
                        }
                        // the other Backrooms levels
                        for (var block : java.util.List.of(ModItems.EXIT_DOOR, ModItems.PANEL_LIGHT, ModItems.PIPE, ModItems.HANGING_CABLE,
                                ModItems.DRIED_CORN, ModItems.OFFICE_CARPET, ModItems.OFFICE_WALL, ModItems.OFFICE_DEBRIS, ModItems.GARAGE_CONCRETE,
                                ModItems.CORRUGATED_METAL, ModItems.LOBBY_TILE, ModItems.SUNLIT_TILE, ModItems.MONOLITH, ModItems.CARPET_LEDGE,
                                ModItems.POOL_TILE, ModItems.POOL_TILE_STAIRS, ModItems.POOL_TILE_SLAB, ModItems.POOL_FLOOR_TILE, ModItems.POOL_LIGHT,
                                ModItems.SPACE_CRUISER, ModItems.CRUISER_REMOTE, ModItems.SHRINK_RAY, ModItems.GROWTH_LENS, ModItems.FREEZE_RAY,
                                ModItems.TIME_CRYSTAL, ModItems.INTERDIMENSIONAL_GOGGLES, ModItems.COGNITION_AMPLIFIER, ModItems.COMBAT_MODULE,
                                ModItems.GUARDIAN_MODULE, ModItems.MEDIC_MODULE, ModItems.FETCH_MODULE,
                                ModItems.POOL_DRAIN, ModItems.BALLOON, ModItems.PRESENT, ModItems.CONFETTI, ModItems.FIELD_GUIDE,
                                ModItems.FARM_SCARECROW_SPAWN_EGG, ModItems.PARTYGOER_SPAWN_EGG, ModItems.SCARECROW_HEAD, ModItems.SCARECROW_LANTERN, ModItems.SCARECROW_SPAWN_EGG, ModItems.WATCHER_SPAWN_EGG)) {
                            output.accept(block.get());
                        }
                        ModItems.CRUISER_UPGRADES.values().forEach(upgrade -> output.accept(upgrade.get()));
                        for (var item : java.util.List.of(ModItems.STORE_FLOOR, ModItems.STORE_WALKWAY, ModItems.STORE_PARTITION, ModItems.STORE_LIGHT,
                                ModItems.EXIT_SIGN, ModItems.SOFA, ModItems.STORE_CHAIR, ModItems.DINING_TABLE, ModItems.COFFEE_TABLE, ModItems.CUBE_SHELF,
                                ModItems.FLOOR_LAMP, ModItems.KITCHEN_CABINET, ModItems.STOCK_CRATE, ModItems.FOOD_COUNTER, ModItems.CHECKOUT_COUNTER,
                                ModItems.AISLE_MARKER, ModItems.STORE_PALLET, ModItems.FLAT_PACK_BOX, ModItems.PAPER_LAMP, ModItems.RACK_UPRIGHT,
                                ModItems.RACK_SHELF, ModItems.STORE_PILLAR, ModItems.FLOOR_ARROW, ModItems.DEPARTMENT_SIGN, ModItems.LOADED_PALLET,
                                ModItems.BALL_PIT, ModItems.SHARK_PLUSH, ModItems.TOILET, ModItems.BATHTUB)) {
                            output.accept(item.get());
                        }
                        output.accept(ModItems.STORE_DOOR.get());
                        output.accept(ModItems.LONE_DOOR.get());
                        for (var item : java.util.List.of(ModItems.MALL_TILE, ModItems.MALL_WALL, ModItems.MALL_CEILING, ModItems.MALL_SKYLIGHT,
                                ModItems.MALL_RAILING, ModItems.MALL_ESCALATOR, ModItems.MALL_BENCH, ModItems.MALL_TRASH_CAN, ModItems.GUMBALL_MACHINE,
                                ModItems.GUMBALL)) {
                            output.accept(item.get());
                        }
                        for (var item : java.util.List.of(ModItems.CLOUD, ModItems.AIR_CONDITIONER, ModItems.FROOPY_TURF_PINK, ModItems.FROOPY_TURF_MINT,
                                ModItems.FROOPY_TURF_LAVENDER, ModItems.FROOPY_TURF_BUTTER, ModItems.RED_EXIT_SIGN)) {
                            output.accept(item.get());
                        }
                        output.accept(ModItems.MEATBALL_PLATE.get());
                        output.accept(ModItems.LINGONBERRY_JUICE.get());
                        output.accept(ModItems.STAFF_BADGE.get());
                        output.accept(ModItems.STAFF_SPAWN_EGG.get());
                        ModItems.LASER_DRILLS.forEach(drill -> output.accept(drill.get()));
                        ModItems.CRUISER_BOOSTERS.forEach(booster -> output.accept(booster.get()));
                        ModItems.CRUISER_THRUSTERS.forEach(thrusters -> output.accept(thrusters.get()));
                        output.accept(ModItems.BACKROOMS_WALLPAPER.get());
                        output.accept(ModItems.BACKROOMS_CARPET.get());
                        output.accept(ModItems.BACKROOMS_CEILING.get());
                        output.accept(ModItems.FLUORESCENT_LIGHT.get());
                    })
                    .build());

    /** A Portal Gun with the Blender Upgrade already fitted, for creative mode. */
    private static ItemStack blenderGun() {
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        gun.set(ModDataComponents.BLENDER.get(), true);
        return gun;
    }

    /** Also list the items in the matching vanilla tabs. */
    public static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.PORTAL_GUN.get());
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.PORTAL_FLUID.get());
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.WARP_GATEWAY.get());
        } else if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.BACTERIA_SPAWN_EGG.get());
            ModItems.MIMIC_SPAWN_EGGS.values().forEach(egg -> event.accept(egg.get()));
            ModItems.STILL_LIFE_SPAWN_EGGS.values().forEach(egg -> event.accept(egg.get()));
            ModItems.CRONENBERG_SPAWN_EGGS.values().forEach(egg -> event.accept(egg.get()));
            ModItems.SURVIVOR_SPAWN_EGGS.values().forEach(egg -> event.accept(egg.get()));
            event.accept(ModItems.BEHEMOTH_SPAWN_EGG.get());
            event.accept(ModItems.MANTIS_SPAWN_EGG.get());
            event.accept(ModItems.COUNCIL_SPAWN_EGG.get());
            event.accept(ModItems.SCARECROW_SPAWN_EGG.get());
            event.accept(ModItems.WATCHER_SPAWN_EGG.get());
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.MANTIS_SCYTHE.get());
            event.accept(ModItems.BONE_PLATE_HELMET.get());
            event.accept(ModItems.BONE_PLATE_CHESTPLATE.get());
            event.accept(ModItems.BONE_PLATE_LEGGINGS.get());
            event.accept(ModItems.BONE_PLATE_BOOTS.get());
            event.accept(ModItems.SURVIVOR_HELMET.get());
            event.accept(ModItems.SURVIVOR_CHESTPLATE.get());
            event.accept(ModItems.SURVIVOR_LEGGINGS.get());
            event.accept(ModItems.SURVIVOR_BOOTS.get());
        }
    }
}
