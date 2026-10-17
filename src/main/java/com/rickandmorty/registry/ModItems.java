package com.rickandmorty.registry;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.item.AlmondWaterItem;
import com.rickandmorty.item.CureSerumItem;
import com.rickandmorty.item.DimensionCoordinatesItem;
import com.rickandmorty.item.MantisScytheItem;
import com.rickandmorty.entity.CronenbergKind;
import com.rickandmorty.entity.SurvivorKind;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import com.rickandmorty.entity.MimicKind;
import com.rickandmorty.item.JournalPageItem;
import com.rickandmorty.item.LoreItem;
import com.rickandmorty.item.MeeseeksBoxItem;
import com.rickandmorty.item.PlumbusItem;
import com.rickandmorty.item.RicksFlaskItem;
import com.rickandmorty.item.PortalFluidDropItem;
import com.rickandmorty.item.PortalFluidItem;
import com.rickandmorty.item.PortalGunItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RickAndMortyMod.MOD_ID);

    public static final DeferredItem<PortalGunItem> PORTAL_GUN = ITEMS.register("portal_gun",
            () -> new PortalGunItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<PortalFluidItem> PORTAL_FLUID = ITEMS.register("portal_fluid",
            () -> new PortalFluidItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    // --- Warp Gateway
    public static final DeferredItem<BlockItem> WARP_GATEWAY = ITEMS.register("warp_gateway",
            () -> new BlockItem(ModBlocks.WARP_GATEWAY.get(), new Item.Properties().rarity(Rarity.RARE)));

    // --- Backrooms research
    public static final DeferredItem<JournalPageItem> JOURNAL_PAGE_1 = page("journal_page", 1);
    public static final DeferredItem<JournalPageItem> JOURNAL_PAGE_2 = page("journal_page", 2);
    public static final DeferredItem<JournalPageItem> JOURNAL_PAGE_3 = page("journal_page", 3);
    public static final DeferredItem<DimensionCoordinatesItem> BACKROOMS_COORDINATES = ITEMS.register("backrooms_coordinates",
            () -> new DimensionCoordinatesItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), ModDimensions.BACKROOMS, "backrooms_coordinates"));

    // --- Cronenberg World research: Rick's notes on Potion #9
    public static final DeferredItem<JournalPageItem> RICK_NOTES_1 = page("rick_notes", 1);
    public static final DeferredItem<JournalPageItem> RICK_NOTES_2 = page("rick_notes", 2);
    public static final DeferredItem<JournalPageItem> RICK_NOTES_3 = page("rick_notes", 3);
    public static final DeferredItem<DimensionCoordinatesItem> CRONENBERG_COORDINATES = ITEMS.register("cronenberg_coordinates",
            () -> new DimensionCoordinatesItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), ModDimensions.CRONENBERG, "cronenberg_coordinates"));

    public static final DeferredItem<PortalFluidDropItem> PORTAL_FLUID_DROP = ITEMS.register("portal_fluid_drop",
            () -> new PortalFluidDropItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> BACTERIA_SPAWN_EGG = ITEMS.register("bacteria_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BACTERIA, 0x121214, 0xE8E6DA, new Item.Properties()));

    // --- Rick and Morty gadgets
    public static final DeferredItem<MeeseeksBoxItem> MEESEEKS_BOX = ITEMS.register("meeseeks_box",
            () -> new MeeseeksBoxItem(new Item.Properties().durability(5).rarity(Rarity.RARE)));
    public static final DeferredItem<PlumbusItem> PLUMBUS = ITEMS.register("plumbus",
            () -> new PlumbusItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<RicksFlaskItem> RICKS_FLASK = ITEMS.register("ricks_flask",
            () -> new RicksFlaskItem(new Item.Properties().durability(3).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<LoreItem> SZECHUAN_SAUCE = ITEMS.register("szechuan_sauce",
            () -> new LoreItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE).food(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.6f).alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200), 1.0f)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200), 1.0f)
                    .build()), "szechuan_sauce"));

    /** Smithing template for the Portal Gun's Targeting Upgrade. */
    public static final DeferredItem<LoreItem> TARGETING_CHIP = ITEMS.register("targeting_chip",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.RARE), "targeting_chip"));

    public static final Map<MimicKind, DeferredItem<DeferredSpawnEggItem>> MIMIC_SPAWN_EGGS = new EnumMap<>(MimicKind.class);

    static {
        int[][] colors = {{0x6B7266, 0xB5C2AC}, {0x9C998F, 0x5E5B55}, {0xA88482, 0x5C4A48}, {0x524A44, 0x8A8078}, {0x9C9892, 0x7A2A20}, {0x3E4A3C, 0x74836F}};
        for (MimicKind kind : MimicKind.values()) {
            int[] c = colors[kind.ordinal()];
            MIMIC_SPAWN_EGGS.put(kind, ITEMS.register(kind.id() + "_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.MIMICS.get(kind), c[0], c[1], new Item.Properties())));
        }
    }

    public static final Map<com.rickandmorty.entity.StillLifeForm, DeferredItem<DeferredSpawnEggItem>> STILL_LIFE_SPAWN_EGGS = new EnumMap<>(com.rickandmorty.entity.StillLifeForm.class);

    static {
        int[][] colors = {{0xC9B98A, 0x6B8F7E}, {0x8E3A2C, 0xD8A890}, {0x8A8A84, 0xE4E0D4}, {0x5A4A3E, 0xC8A888}, {0xE6DDCC, 0x9A8670},
                {0xCFC9BC, 0x6E5A3C}, {0x2A2A2C, 0x3E5E8C}, {0xD8E85A, 0x8A8A8A}, {0xE08A3C, 0xB08A5A}, {0x1A1A1A, 0xE8E2D0}};
        for (com.rickandmorty.entity.StillLifeForm form : com.rickandmorty.entity.StillLifeForm.values()) {
            int[] c = colors[form.ordinal()];
            STILL_LIFE_SPAWN_EGGS.put(form, ITEMS.register(form.id() + "_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.STILL_LIVES.get(form), c[0], c[1], new Item.Properties())));
        }
    }

    // --- Backrooms blocks
    public static final DeferredItem<BlockItem> BACKROOMS_WALLPAPER = ITEMS.registerSimpleBlockItem(ModBlocks.BACKROOMS_WALLPAPER);
    public static final DeferredItem<BlockItem> BACKROOMS_CARPET = ITEMS.registerSimpleBlockItem(ModBlocks.BACKROOMS_CARPET);
    public static final DeferredItem<BlockItem> BACKROOMS_CEILING = ITEMS.registerSimpleBlockItem(ModBlocks.BACKROOMS_CEILING);
    public static final DeferredItem<BlockItem> FLUORESCENT_LIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.FLUORESCENT_LIGHT);
    public static final DeferredItem<BlockItem> SLIDE = ITEMS.registerSimpleBlockItem(ModBlocks.SLIDE);
    public static final DeferredItem<AlmondWaterItem> ALMOND_WATER = ITEMS.register("almond_water",
            () -> new AlmondWaterItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BlockItem> MICROVERSE_BATTERY = ITEMS.register("microverse_battery",
            () -> new BlockItem(ModBlocks.MICROVERSE_BATTERY.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> MICROVERSE_WIRE = ITEMS.registerSimpleBlockItem(ModBlocks.MICROVERSE_WIRE);

    // --- Cronenberg World
    public static final DeferredItem<Item> CRONENBERG_FLESH = ITEMS.register("cronenberg_flesh",
            () -> new LoreItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.2f)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600), 0.8f)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200), 0.3f).build()), "cronenberg_flesh"));
    public static final DeferredItem<Item> COOKED_CRONENBERG_FLESH = ITEMS.register("cooked_cronenberg_flesh",
            () -> new LoreItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.5f).build()),
                    "cooked_cronenberg_flesh"));
    public static final DeferredItem<Item> MUTAGEN_SAC = ITEMS.register("mutagen_sac",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.UNCOMMON), "mutagen_sac"));
    public static final DeferredItem<Item> BONE_PLATE = ITEMS.register("bone_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<MantisScytheItem> MANTIS_SCYTHE = ITEMS.register("mantis_scythe",
            () -> new MantisScytheItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<CureSerumItem> CURE_SERUM = ITEMS.register("cure_serum",
            () -> new CureSerumItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> BEHEMOTH_HEART = ITEMS.register("behemoth_heart",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.EPIC).food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.0f)
                    .alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 1), 1.0f)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 400, 1), 1.0f)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3600), 1.0f).build()), "behemoth_heart"));
    /** Smithing template for the Portal Gun's Blender Upgrade (attack mode). */
    public static final DeferredItem<LoreItem> BLENDER_CHIP = ITEMS.register("blender_chip",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.EPIC), "blender_chip"));
    /** Smithing template for the Portal Gun's DNA Lock Upgrade (it comes back with you when you die). */
    public static final DeferredItem<LoreItem> DNA_LOCK_CHIP = ITEMS.register("dna_lock_chip",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.EPIC), "dna_lock_chip"));

    public static final DeferredItem<ArmorItem> BONE_PLATE_HELMET = armor("bone_plate_helmet", ModArmorMaterials.BONE_PLATE, ArmorItem.Type.HELMET, 20);
    public static final DeferredItem<ArmorItem> BONE_PLATE_CHESTPLATE = armor("bone_plate_chestplate", ModArmorMaterials.BONE_PLATE, ArmorItem.Type.CHESTPLATE, 20);
    public static final DeferredItem<ArmorItem> BONE_PLATE_LEGGINGS = armor("bone_plate_leggings", ModArmorMaterials.BONE_PLATE, ArmorItem.Type.LEGGINGS, 20);
    public static final DeferredItem<ArmorItem> BONE_PLATE_BOOTS = armor("bone_plate_boots", ModArmorMaterials.BONE_PLATE, ArmorItem.Type.BOOTS, 20);
    public static final DeferredItem<ArmorItem> SURVIVOR_HELMET = armor("survivor_helmet", ModArmorMaterials.SURVIVOR, ArmorItem.Type.HELMET, 18);
    public static final DeferredItem<ArmorItem> SURVIVOR_CHESTPLATE = armor("survivor_chestplate", ModArmorMaterials.SURVIVOR, ArmorItem.Type.CHESTPLATE, 18);
    public static final DeferredItem<ArmorItem> SURVIVOR_LEGGINGS = armor("survivor_leggings", ModArmorMaterials.SURVIVOR, ArmorItem.Type.LEGGINGS, 18);
    public static final DeferredItem<ArmorItem> SURVIVOR_BOOTS = armor("survivor_boots", ModArmorMaterials.SURVIVOR, ArmorItem.Type.BOOTS, 18);

    public static final DeferredItem<BlockItem> FLESH = ITEMS.registerSimpleBlockItem(ModBlocks.FLESH);
    public static final DeferredItem<BlockItem> VEINED_FLESH = ITEMS.registerSimpleBlockItem(ModBlocks.VEINED_FLESH);
    public static final DeferredItem<BlockItem> ROTTING_FLESH = ITEMS.registerSimpleBlockItem(ModBlocks.ROTTING_FLESH);
    public static final DeferredItem<BlockItem> NECROTIC_FLESH = ITEMS.registerSimpleBlockItem(ModBlocks.NECROTIC_FLESH);
    public static final DeferredItem<BlockItem> TUMOR = ITEMS.registerSimpleBlockItem(ModBlocks.TUMOR);
    public static final DeferredItem<BlockItem> WATCHING_FLESH = ITEMS.registerSimpleBlockItem(ModBlocks.WATCHING_FLESH);
    public static final DeferredItem<BlockItem> BLOOD_CLOT = ITEMS.registerSimpleBlockItem(ModBlocks.BLOOD_CLOT);
    public static final DeferredItem<BlockItem> CALCIFIED_BONE = ITEMS.registerSimpleBlockItem(ModBlocks.CALCIFIED_BONE);
    public static final DeferredItem<BlockItem> PUSTULE = ITEMS.registerSimpleBlockItem(ModBlocks.PUSTULE);
    public static final DeferredItem<BlockItem> TOOTH_SPIKE = ITEMS.registerSimpleBlockItem(ModBlocks.TOOTH_SPIKE);
    public static final DeferredItem<BlockItem> FLESH_GRASS = ITEMS.registerSimpleBlockItem(ModBlocks.FLESH_GRASS);
    public static final DeferredItem<BlockItem> EYESTALK = ITEMS.registerSimpleBlockItem(ModBlocks.EYESTALK);

    // --- the other Backrooms levels
    public static final DeferredItem<net.minecraft.world.item.DoubleHighBlockItem> EXIT_DOOR = ITEMS.register("exit_door",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.EXIT_DOOR.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> PANEL_LIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.PANEL_LIGHT);
    public static final DeferredItem<BlockItem> PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.PIPE);
    public static final DeferredItem<BlockItem> HANGING_CABLE = ITEMS.registerSimpleBlockItem(ModBlocks.HANGING_CABLE);
    public static final DeferredItem<net.minecraft.world.item.DoubleHighBlockItem> DRIED_CORN = ITEMS.register("dried_corn",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.DRIED_CORN.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> OFFICE_CARPET = ITEMS.registerSimpleBlockItem(ModBlocks.OFFICE_CARPET);
    public static final DeferredItem<BlockItem> OFFICE_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.OFFICE_WALL);
    public static final DeferredItem<BlockItem> OFFICE_DEBRIS = ITEMS.registerSimpleBlockItem(ModBlocks.OFFICE_DEBRIS);
    public static final DeferredItem<BlockItem> GARAGE_CONCRETE = ITEMS.registerSimpleBlockItem(ModBlocks.GARAGE_CONCRETE);
    public static final DeferredItem<BlockItem> CORRUGATED_METAL = ITEMS.registerSimpleBlockItem(ModBlocks.CORRUGATED_METAL);
    public static final DeferredItem<BlockItem> LOBBY_TILE = ITEMS.registerSimpleBlockItem(ModBlocks.LOBBY_TILE);
    public static final DeferredItem<BlockItem> SUNLIT_TILE = ITEMS.registerSimpleBlockItem(ModBlocks.SUNLIT_TILE);
    public static final DeferredItem<BlockItem> MONOLITH = ITEMS.registerSimpleBlockItem(ModBlocks.MONOLITH);
    public static final DeferredItem<BlockItem> CARPET_LEDGE = ITEMS.registerSimpleBlockItem(ModBlocks.CARPET_LEDGE);
    public static final DeferredItem<BlockItem> SCARECROW_HEAD = ITEMS.registerSimpleBlockItem(ModBlocks.SCARECROW_HEAD);
    // --- Rick's Space Cruiser
    public static final DeferredItem<com.rickandmorty.entity.cruiser.SpaceCruiserItem> SPACE_CRUISER = ITEMS.register("space_cruiser",
            () -> new com.rickandmorty.entity.cruiser.SpaceCruiserItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<com.rickandmorty.entity.cruiser.CruiserRemoteItem> CRUISER_REMOTE = ITEMS.register("cruiser_remote",
            () -> new com.rickandmorty.entity.cruiser.CruiserRemoteItem(new Item.Properties().stacksTo(1)));
    public static final Map<com.rickandmorty.entity.cruiser.CruiserUpgrade, DeferredItem<com.rickandmorty.entity.cruiser.CruiserUpgradeItem>> CRUISER_UPGRADES =
            new EnumMap<>(com.rickandmorty.entity.cruiser.CruiserUpgrade.class);

    static {
        for (var upgrade : com.rickandmorty.entity.cruiser.CruiserUpgrade.values()) {
            CRUISER_UPGRADES.put(upgrade, ITEMS.register(upgrade.itemId(),
                    () -> new com.rickandmorty.entity.cruiser.CruiserUpgradeItem(upgrade, new Item.Properties().stacksTo(1).rarity(Rarity.RARE))));
        }
    }

    /** Microverse Boosters and Speed Thrusters Mk I, II and III, and Laser Drills Mk II and III, in that order. */
    public static final java.util.List<DeferredItem<com.rickandmorty.entity.cruiser.CruiserTierItem>> CRUISER_BOOSTERS = java.util.List.of(
            tierItem("cruiser_booster_mk", com.rickandmorty.entity.cruiser.CruiserTier.BOOSTER, 1),
            tierItem("cruiser_booster_mk", com.rickandmorty.entity.cruiser.CruiserTier.BOOSTER, 2),
            tierItem("cruiser_booster_mk", com.rickandmorty.entity.cruiser.CruiserTier.BOOSTER, 3));
    public static final java.util.List<DeferredItem<com.rickandmorty.entity.cruiser.CruiserTierItem>> CRUISER_THRUSTERS = java.util.List.of(
            tierItem("cruiser_thrusters_mk", com.rickandmorty.entity.cruiser.CruiserTier.THRUSTERS, 1),
            tierItem("cruiser_thrusters_mk", com.rickandmorty.entity.cruiser.CruiserTier.THRUSTERS, 2),
            tierItem("cruiser_thrusters_mk", com.rickandmorty.entity.cruiser.CruiserTier.THRUSTERS, 3));
    public static final java.util.List<DeferredItem<com.rickandmorty.entity.cruiser.CruiserTierItem>> LASER_DRILLS = java.util.List.of(
            tierItem("laser_drill_mk", com.rickandmorty.entity.cruiser.CruiserTier.DRILL, 2),
            tierItem("laser_drill_mk", com.rickandmorty.entity.cruiser.CruiserTier.DRILL, 3));

    private static DeferredItem<com.rickandmorty.entity.cruiser.CruiserTierItem> tierItem(String prefix, com.rickandmorty.entity.cruiser.CruiserTier line, int tier) {
        Rarity rarity = tier == 1 ? Rarity.UNCOMMON : tier == 2 ? Rarity.RARE : Rarity.EPIC;
        return ITEMS.register(prefix + tier,
                () -> new com.rickandmorty.entity.cruiser.CruiserTierItem(line, tier, new Item.Properties().stacksTo(1).rarity(rarity)));
    }

    public static final DeferredItem<BlockItem> BALLOON = ITEMS.registerSimpleBlockItem(ModBlocks.BALLOON);
    public static final DeferredItem<BlockItem> PRESENT = ITEMS.registerSimpleBlockItem(ModBlocks.PRESENT);
    public static final DeferredItem<BlockItem> CONFETTI = ITEMS.registerSimpleBlockItem(ModBlocks.CONFETTI);
    public static final DeferredItem<com.rickandmorty.item.FieldGuideItem> FIELD_GUIDE = ITEMS.register("field_guide",
            () -> new com.rickandmorty.item.FieldGuideItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DeferredSpawnEggItem> FARM_SCARECROW_SPAWN_EGG = ITEMS.register("farm_scarecrow_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FARM_SCARECROW, 0xB08A4E, 0xB33A2E, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> PARTYGOER_SPAWN_EGG = ITEMS.register("partygoer_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.PARTYGOER, 0xF2D13A, 0x1A1A1A, new Item.Properties()));
    public static final DeferredItem<BlockItem> POOL_TILE = ITEMS.registerSimpleBlockItem(ModBlocks.POOL_TILE);
    public static final DeferredItem<BlockItem> POOL_TILE_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.POOL_TILE_STAIRS);
    public static final DeferredItem<BlockItem> POOL_TILE_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.POOL_TILE_SLAB);
    public static final DeferredItem<BlockItem> POOL_FLOOR_TILE = ITEMS.registerSimpleBlockItem(ModBlocks.POOL_FLOOR_TILE);
    public static final DeferredItem<BlockItem> POOL_LIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.POOL_LIGHT);
    public static final DeferredItem<BlockItem> POOL_DRAIN = ITEMS.registerSimpleBlockItem(ModBlocks.POOL_DRAIN);
    public static final DeferredItem<com.rickandmorty.item.ScarecrowLanternItem> SCARECROW_LANTERN = ITEMS.register("scarecrow_lantern",
            () -> new com.rickandmorty.item.ScarecrowLanternItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DeferredSpawnEggItem> SCARECROW_SPAWN_EGG = ITEMS.register("scarecrow_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SCARECROW, 0x2A2420, 0xC9A55A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> WATCHER_SPAWN_EGG = ITEMS.register("watcher_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WATCHER, 0x3AA7A7, 0xF4F4F4, new Item.Properties()));

    public static final Map<CronenbergKind, DeferredItem<DeferredSpawnEggItem>> CRONENBERG_SPAWN_EGGS = new EnumMap<>(CronenbergKind.class);
    public static final Map<SurvivorKind, DeferredItem<DeferredSpawnEggItem>> SURVIVOR_SPAWN_EGGS = new EnumMap<>(SurvivorKind.class);

    static {
        int[][] cronenberg = {{0xC76F6A, 0x5A1414}, {0xB5605A, 0xE8D8B0}, {0xD98A80, 0x7A2A40}};
        for (CronenbergKind kind : CronenbergKind.values()) {
            int[] c = cronenberg[kind.ordinal()];
            CRONENBERG_SPAWN_EGGS.put(kind, ITEMS.register(kind.id() + "_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.CRONENBERGS.get(kind), c[0], c[1], new Item.Properties())));
        }
        int[][] survivors = {{0x6B4A2B, 0xE0B48A}, {0xC9A15A, 0x7A1E1E}, {0xE05A2A, 0xF0A0B0}, {0x8FB8C8, 0xC76F6A}, {0xF0D040, 0xC76F6A}};
        for (SurvivorKind kind : SurvivorKind.values()) {
            int[] c = survivors[kind.ordinal()];
            SURVIVOR_SPAWN_EGGS.put(kind, ITEMS.register(kind.id() + "_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.SURVIVORS.get(kind), c[0], c[1], new Item.Properties())));
        }
    }

    public static final DeferredItem<DeferredSpawnEggItem> BEHEMOTH_SPAWN_EGG = ITEMS.register("behemoth_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BEHEMOTH, 0x8C2F2F, 0xE8D8B0, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> MANTIS_SPAWN_EGG = ITEMS.register("mantis_person_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MANTIS, 0x6E8C3A, 0x3A4A6A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> COUNCIL_SPAWN_EGG = ITEMS.register("council_member_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.COUNCIL_MEMBER, 0xE8EEF2, 0x5CFF3B, new Item.Properties()));

    private static DeferredItem<JournalPageItem> page(String name, int page) {
        return ITEMS.register(name + "_" + page,
                () -> new JournalPageItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), name, page));
    }

    private static DeferredItem<ArmorItem> armor(String name, Holder<ArmorMaterial> material, ArmorItem.Type type, int durability) {
        return ITEMS.register(name, () -> new ArmorItem(material, type, new Item.Properties().durability(type.getDurability(durability))));
    }

    // --- the Infinite IKEA
    public static final DeferredItem<BlockItem> STORE_FLOOR = ITEMS.registerSimpleBlockItem(ModBlocks.STORE_FLOOR);
    public static final DeferredItem<BlockItem> STORE_WALKWAY = ITEMS.registerSimpleBlockItem(ModBlocks.STORE_WALKWAY);
    public static final DeferredItem<BlockItem> STORE_PARTITION = ITEMS.registerSimpleBlockItem(ModBlocks.STORE_PARTITION);
    public static final DeferredItem<BlockItem> STORE_LIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.STORE_LIGHT);
    public static final DeferredItem<BlockItem> EXIT_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.EXIT_SIGN);
    public static final DeferredItem<BlockItem> SOFA = ITEMS.registerSimpleBlockItem(ModBlocks.SOFA);
    public static final DeferredItem<BlockItem> STORE_CHAIR = ITEMS.registerSimpleBlockItem(ModBlocks.STORE_CHAIR);
    public static final DeferredItem<BlockItem> DINING_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.DINING_TABLE);
    public static final DeferredItem<BlockItem> COFFEE_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.COFFEE_TABLE);
    public static final DeferredItem<BlockItem> CUBE_SHELF = ITEMS.registerSimpleBlockItem(ModBlocks.CUBE_SHELF);
    public static final DeferredItem<BlockItem> FLOOR_LAMP = ITEMS.registerSimpleBlockItem(ModBlocks.FLOOR_LAMP);
    public static final DeferredItem<BlockItem> KITCHEN_CABINET = ITEMS.registerSimpleBlockItem(ModBlocks.KITCHEN_CABINET);
    public static final DeferredItem<BlockItem> STOCK_CRATE = ITEMS.registerSimpleBlockItem(ModBlocks.STOCK_CRATE);
    public static final DeferredItem<BlockItem> FOOD_COUNTER = ITEMS.registerSimpleBlockItem(ModBlocks.FOOD_COUNTER);
    public static final DeferredItem<BlockItem> CHECKOUT_COUNTER = ITEMS.registerSimpleBlockItem(ModBlocks.CHECKOUT_COUNTER);
    public static final DeferredItem<BlockItem> AISLE_MARKER = ITEMS.registerSimpleBlockItem(ModBlocks.AISLE_MARKER);
    public static final DeferredItem<BlockItem> STORE_PALLET = ITEMS.registerSimpleBlockItem(ModBlocks.STORE_PALLET);
    public static final DeferredItem<BlockItem> FLAT_PACK_BOX = ITEMS.registerSimpleBlockItem(ModBlocks.FLAT_PACK_BOX);
    public static final DeferredItem<BlockItem> PAPER_LAMP = ITEMS.registerSimpleBlockItem(ModBlocks.PAPER_LAMP);
    public static final DeferredItem<BlockItem> RACK_UPRIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.RACK_UPRIGHT);
    public static final DeferredItem<BlockItem> RACK_SHELF = ITEMS.registerSimpleBlockItem(ModBlocks.RACK_SHELF);
    public static final DeferredItem<net.minecraft.world.item.DoubleHighBlockItem> STORE_DOOR = ITEMS.register("store_door",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.STORE_DOOR.get(), new Item.Properties()));
    public static final DeferredItem<Item> MEATBALL_PLATE = ITEMS.register("meatball_plate",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build())));
    public static final DeferredItem<com.rickandmorty.item.LingonberryJuiceItem> LINGONBERRY_JUICE = ITEMS.register("lingonberry_juice",
            () -> new com.rickandmorty.item.LingonberryJuiceItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<com.rickandmorty.item.LoreItem> STAFF_BADGE = ITEMS.register("staff_badge",
            () -> new com.rickandmorty.item.LoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "staff_badge"));
    public static final DeferredItem<DeferredSpawnEggItem> STAFF_SPAWN_EGG = ITEMS.register("staff_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.STAFF, 0xE8BE22, 0x22242C, new Item.Properties()));

    public static final DeferredItem<BlockItem> STORE_PILLAR = ITEMS.registerSimpleBlockItem(ModBlocks.STORE_PILLAR);
    public static final DeferredItem<BlockItem> FLOOR_ARROW = ITEMS.registerSimpleBlockItem(ModBlocks.FLOOR_ARROW);
    public static final DeferredItem<BlockItem> DEPARTMENT_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.DEPARTMENT_SIGN);
    public static final DeferredItem<BlockItem> LOADED_PALLET = ITEMS.registerSimpleBlockItem(ModBlocks.LOADED_PALLET);
    public static final DeferredItem<BlockItem> BALL_PIT = ITEMS.registerSimpleBlockItem(ModBlocks.BALL_PIT);
    public static final DeferredItem<BlockItem> SHARK_PLUSH = ITEMS.registerSimpleBlockItem(ModBlocks.SHARK_PLUSH);
    public static final DeferredItem<BlockItem> TOILET = ITEMS.registerSimpleBlockItem(ModBlocks.TOILET);
    public static final DeferredItem<BlockItem> BATHTUB = ITEMS.registerSimpleBlockItem(ModBlocks.BATHTUB);

    // --- the Dead Mall
    public static final DeferredItem<BlockItem> MALL_TILE = ITEMS.registerSimpleBlockItem(ModBlocks.MALL_TILE);
    public static final DeferredItem<BlockItem> MALL_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.MALL_WALL);
    public static final DeferredItem<BlockItem> MALL_CEILING = ITEMS.registerSimpleBlockItem(ModBlocks.MALL_CEILING);
    public static final DeferredItem<BlockItem> MALL_SKYLIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.MALL_SKYLIGHT);
    public static final DeferredItem<BlockItem> MALL_RAILING = ITEMS.registerSimpleBlockItem(ModBlocks.MALL_RAILING);
    public static final DeferredItem<BlockItem> MALL_ESCALATOR = ITEMS.registerSimpleBlockItem(ModBlocks.MALL_ESCALATOR);
    public static final DeferredItem<BlockItem> MALL_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.MALL_BENCH);
    public static final DeferredItem<BlockItem> MALL_TRASH_CAN = ITEMS.registerSimpleBlockItem(ModBlocks.MALL_TRASH_CAN);
    public static final DeferredItem<BlockItem> GUMBALL_MACHINE = ITEMS.registerSimpleBlockItem(ModBlocks.GUMBALL_MACHINE);
    // --- v0.23 levels
    public static final DeferredItem<BlockItem> CLOUD = ITEMS.registerSimpleBlockItem(ModBlocks.CLOUD);
    public static final DeferredItem<BlockItem> AIR_CONDITIONER = ITEMS.registerSimpleBlockItem(ModBlocks.AIR_CONDITIONER);
    public static final DeferredItem<BlockItem> FROOPY_TURF_PINK = ITEMS.registerSimpleBlockItem(ModBlocks.FROOPY_TURF_PINK);
    public static final DeferredItem<BlockItem> FROOPY_TURF_MINT = ITEMS.registerSimpleBlockItem(ModBlocks.FROOPY_TURF_MINT);
    public static final DeferredItem<BlockItem> FROOPY_TURF_LAVENDER = ITEMS.registerSimpleBlockItem(ModBlocks.FROOPY_TURF_LAVENDER);
    public static final DeferredItem<BlockItem> FROOPY_TURF_BUTTER = ITEMS.registerSimpleBlockItem(ModBlocks.FROOPY_TURF_BUTTER);
    public static final DeferredItem<BlockItem> RED_EXIT_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.RED_EXIT_SIGN);
    public static final DeferredItem<Item> GUMBALL = ITEMS.register("gumball",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.2f).fast().alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0), 0.4f).build())));

    public static final DeferredItem<net.minecraft.world.item.DoubleHighBlockItem> LONE_DOOR = ITEMS.register("lone_door",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.LONE_DOOR.get(), new Item.Properties()));

    // --- Rick's gadgets
    public static final DeferredItem<BlockItem> SHRINK_RAY = ITEMS.register("shrink_ray",
            () -> new BlockItem(ModBlocks.SHRINK_RAY.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> TIME_CRYSTAL = ITEMS.register("time_crystal",
            () -> new BlockItem(ModBlocks.TIME_CRYSTAL.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<com.rickandmorty.item.LoreItem> GROWTH_LENS = ITEMS.register("growth_lens",
            () -> new com.rickandmorty.item.LoreItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), "growth_lens"));
    public static final DeferredItem<com.rickandmorty.gadget.FreezeRayItem> FREEZE_RAY = ITEMS.register("freeze_ray",
            () -> new com.rickandmorty.gadget.FreezeRayItem(new Item.Properties().durability(250).rarity(Rarity.RARE)));
    public static final DeferredItem<com.rickandmorty.gadget.GogglesItem> INTERDIMENSIONAL_GOGGLES = ITEMS.register("interdimensional_goggles",
            () -> new com.rickandmorty.gadget.GogglesItem(ModArmorMaterials.GOGGLES,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(20)).rarity(Rarity.EPIC)));

    // --- the Cognition Amplifier for pets, and its modules
    public static final DeferredItem<com.rickandmorty.pet.CognitionAmplifierItem> COGNITION_AMPLIFIER = ITEMS.register("cognition_amplifier",
            () -> new com.rickandmorty.pet.CognitionAmplifierItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<com.rickandmorty.pet.AmplifierModuleItem> COMBAT_MODULE = module("combat");
    public static final DeferredItem<com.rickandmorty.pet.AmplifierModuleItem> GUARDIAN_MODULE = module("guardian");
    public static final DeferredItem<com.rickandmorty.pet.AmplifierModuleItem> MEDIC_MODULE = module("medic");
    public static final DeferredItem<com.rickandmorty.pet.AmplifierModuleItem> FETCH_MODULE = module("fetch");

    private static DeferredItem<com.rickandmorty.pet.AmplifierModuleItem> module(String name) {
        return ITEMS.register(name + "_module", () -> new com.rickandmorty.pet.AmplifierModuleItem(
                new Item.Properties().stacksTo(16).rarity(Rarity.RARE), com.rickandmorty.pet.AmplifierModule.valueOf(name.toUpperCase(java.util.Locale.ROOT))));
    }
}
