package com.rickandmorty.gametest;

import com.rickandmorty.ModConfig;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.MicroverseWireBlock;
import com.rickandmorty.item.AlmondWaterItem;
import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModEffects;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.sanity.NoclipHandler;
import com.rickandmorty.sanity.Sanity;
import com.rickandmorty.worldgen.anomaly.AnomalyKind;
import com.rickandmorty.worldgen.anomaly.AnomalyPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Tests for v0.5: targeting upgrade, Pickle Rick, Microverse Battery, anomalies, sanity, noclip, advancements. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V5GameTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void targetingShotTeleportsCreature(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 from = helper.absoluteVec(new Vec3(2.5, 1, 8.5));
        player.moveTo(from.x, from.y, from.z, -90.0f, 0.0f);    // looking towards +x
        Vec3 home = helper.absoluteVec(new Vec3(2.5, 1, 1.5));
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        gun.set(ModDataComponents.TARGETING.get(), true);
        gun.set(ModDataComponents.PORTAL_DESTINATIONS.get(), PortalDestinations.EMPTY.withAdded(
                new PortalDestinations.Destination("Home", level.dimension(), home.x, home.y, home.z, 0.0f)));
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(12, 1, 8));
        zombie.setNoAi(true);
        helper.runAfterDelay(2, () -> gun.use(level, player, InteractionHand.MAIN_HAND));
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.position().distanceTo(home) < 1.0, "zombie at " + zombie.position() + " not sent home to " + home);
            // a Targeting shot sends things away without opening any portal
            helper.assertTrue(level.getEntitiesOfClass(com.rickandmorty.entity.PortalEntity.class,
                    new net.minecraft.world.phys.AABB(from, from).inflate(40)).isEmpty(), "a portal was opened");
        });
    }

    @GameTest(template = EMPTY)
    public static void pickleRickShrinksAndBrews(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.addEffect(new MobEffectInstance(ModEffects.PICKLE_RICK, 200));
        helper.assertTrue(Math.abs(player.getScale() - 0.3f) < 0.01f, "pickle scale " + player.getScale());
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        ItemStack seaPickle = new ItemStack(Items.SEA_PICKLE);
        helper.assertTrue(level.potionBrewing().hasMix(awkward, seaPickle), "no brewing recipe");
        ItemStack brewed = level.potionBrewing().mix(seaPickle, awkward);
        helper.assertTrue(brewed.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).is(ModEffects.PICKLE_RICK_POTION),
                "brewed " + brewed);
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void microverseBatteryRunsFurnaces(GameTestHelper helper) {
        BlockPos battery = new BlockPos(2, 1, 2);
        helper.setBlock(battery, ModBlocks.MICROVERSE_BATTERY.get());
        BlockPos wire1 = new BlockPos(3, 1, 2), wire2 = new BlockPos(4, 1, 2);
        helper.setBlock(wire1, ModBlocks.MICROVERSE_WIRE.get());
        helper.setBlock(wire2, ModBlocks.MICROVERSE_WIRE.get());
        BlockPos farFurnace = new BlockPos(5, 1, 2), nearFurnace = new BlockPos(2, 1, 3);
        helper.setBlock(farFurnace, Blocks.FURNACE);
        helper.setBlock(nearFurnace, Blocks.FURNACE);
        // wires placed with setBlock don't run placement logic, so let them recompute their connections
        for (BlockPos p : List.of(wire1, wire2)) {
            BlockPos abs = helper.absolutePos(p);
            ServerLevel level = helper.getLevel();
            var state = level.getBlockState(abs);
            for (var dir : net.minecraft.core.Direction.values()) {
                state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), MicroverseWireBlock.connectsTo(level.getBlockState(abs.relative(dir))));
            }
            level.setBlock(abs, state, 3);
        }
        helper.assertTrue(helper.getBlockState(wire1).getValue(PipeBlock.WEST) && helper.getBlockState(wire1).getValue(PipeBlock.EAST), "wire not connected");
        for (BlockPos f : List.of(farFurnace, nearFurnace)) {
            FurnaceBlockEntity furnace = (FurnaceBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(f));
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 2));   // no fuel at all
        }
        helper.succeedWhen(() -> {
            for (BlockPos f : List.of(farFurnace, nearFurnace)) {
                FurnaceBlockEntity furnace = (FurnaceBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(f));
                helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT), "furnace at " + f + " made " + furnace.getItem(2));
                helper.assertTrue(furnace.getItem(1).isEmpty(), "fuel slot touched");
                helper.assertTrue(helper.getBlockState(f).getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT),
                        "furnace at " + f + " cooks but doesn't look lit");
            }
        });
    }

    private static BlockPos groundAt(ServerLevel level, BlockPos column) {
        return new BlockPos(column.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ()) - 1, column.getZ());
    }

    private static void build(ServerLevel level, AnomalyKind kind, BlockPos origin, long seed) {
        AnomalyPiece piece = new AnomalyPiece(kind, origin, seed);
        BoundingBox everywhere = new BoundingBox(origin.getX() - 16, level.getMinBuildHeight(), origin.getZ() - 16,
                origin.getX() + 16, level.getMaxBuildHeight(), origin.getZ() + 16);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1),
                everywhere, new ChunkPos(origin), origin);
    }

    @GameTest(template = EMPTY)
    public static void anomaliesBuild(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var sets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        for (AnomalyKind kind : AnomalyKind.values()) {
            helper.assertTrue(structures.containsKey(RickAndMortyMod.id(kind.getSerializedName())), "structure missing: " + kind);
            helper.assertTrue(sets.containsKey(RickAndMortyMod.id(kind.getSerializedName())), "structure set missing: " + kind);
        }

        BlockPos remnant = groundAt(level, helper.absolutePos(new BlockPos(8, 0, 8)));
        build(level, AnomalyKind.BACKROOMS_REMNANT, remnant, 42L);
        helper.assertTrue(level.getBlockState(remnant.above(4)).is(ModBlocks.FLUORESCENT_LIGHT.get()), "remnant has no light");
        int walls = 0;
        for (BlockPos p : BlockPos.betweenClosed(remnant.offset(-4, 1, -4), remnant.offset(4, 3, 4))) {
            if (level.getBlockState(p).is(ModBlocks.BACKROOMS_WALLPAPER.get())) walls++;
        }
        helper.assertTrue(walls > 20, "remnant walls: " + walls);
        helper.assertTrue(level.getBlockState(remnant.offset(1, 0, 1)).is(ModBlocks.BACKROOMS_CARPET.get())
                || level.getBlockState(remnant.offset(1, 0, 1)).is(Blocks.COARSE_DIRT), "remnant floor");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void invertedTreeAndGlitchBuild(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos tree = groundAt(level, helper.absolutePos(new BlockPos(4, 0, 4)));
        build(level, AnomalyKind.INVERTED_TREE, tree, 7L);
        for (int y = 1; y <= 8; y++) helper.assertTrue(level.getBlockState(tree.above(y)).is(Blocks.OAK_LOG), "trunk missing at " + y);
        helper.assertTrue(level.getBlockState(tree.above(10)).is(Blocks.GRASS_BLOCK), "no ground up top");
        helper.assertTrue(level.getBlockState(tree.offset(1, 1, 0)).is(Blocks.OAK_LEAVES), "no leaves on the ground");

        BlockPos glitch = groundAt(level, helper.absolutePos(new BlockPos(12, 0, 12)));
        var before = level.getBlockState(glitch.offset(1, 0, 1));
        build(level, AnomalyKind.GLITCHED_PATCH, glitch, 99L);
        helper.assertFalse(level.getBlockState(glitch.offset(1, 0, 1)) == before, "glitch changed nothing (still " + before + ")");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void sanityDrainsAndAlmondWaterHelps(GameTestHelper helper) {
        helper.assertTrue(Sanity.ratePerSecond(false, false, false, false, 1.0) > 0, "doesn't recover outside");
        float lit = Sanity.ratePerSecond(true, false, false, false, 1.0);
        float dark = Sanity.ratePerSecond(true, true, false, false, 1.0);
        float hunted = Sanity.ratePerSecond(true, true, true, false, 1.0);
        float company = Sanity.ratePerSecond(true, false, false, true, 1.0);
        helper.assertTrue(lit < 0 && dark < lit && hunted < dark && company > lit, "rates " + lit + " " + dark + " " + hunted + " " + company);
        helper.assertTrue(Math.abs(Sanity.MAX / -lit - 600) < 1, "should take 10 minutes to empty");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Sanity.set(player, 10.0f);
        ItemStack water = new ItemStack(ModItems.ALMOND_WATER.get());
        ItemStack left = water.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(Math.abs(Sanity.get(player) - (10.0f + AlmondWaterItem.RESTORES)) < 0.01f, "sanity " + Sanity.get(player));
        helper.assertTrue(left.is(Items.GLASS_BOTTLE), "no bottle back: " + left);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void noclipFollowsConfig(GameTestHelper helper) {
        boolean enabled = ModConfig.NOCLIP_ENABLED.get();
        double chance = ModConfig.NOCLIP_CHANCE.get();
        try {
            helper.assertTrue(ModConfig.NOCLIP_CHANCE.getDefault() <= 0.005, "default noclip chance should be tiny");
            ModConfig.NOCLIP_ENABLED.set(true);
            ModConfig.NOCLIP_CHANCE.set(0.5);
            helper.assertTrue(NoclipHandler.rollsNoclip(0.1) && !NoclipHandler.rollsNoclip(0.9), "chance ignored");
            ModConfig.NOCLIP_ENABLED.set(false);
            helper.assertFalse(NoclipHandler.rollsNoclip(0.0), "noclip happened while disabled");
        } finally {
            ModConfig.NOCLIP_ENABLED.set(enabled);
            ModConfig.NOCLIP_CHANCE.set(chance);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void advancementsLoad(GameTestHelper helper) {
        var advancements = helper.getLevel().getServer().getAdvancements();
        for (String name : List.of("root", "portal_fluid", "warp_gateway", "targeting", "microverse", "meeseeks", "pickle_rick",
                "szechuan", "anomaly", "research", "backrooms", "noclip", "going_up", "almond_water", "still_life", "bacteria",
                "escape", "lost_your_mind")) {
            helper.assertTrue(advancements.get(RickAndMortyMod.id(name)) != null, "advancement missing: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void targetingUpgradeRecipe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        gun.set(ModDataComponents.PORTAL_CHARGES.get(), 3);
        var input = new net.minecraft.world.item.crafting.SmithingRecipeInput(new ItemStack(ModItems.TARGETING_CHIP.get()), gun, new ItemStack(Items.DIAMOND));
        var recipe = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING, input, level);
        helper.assertTrue(recipe.isPresent(), "no smithing recipe");
        ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
        helper.assertTrue(PortalGunItem.hasTargeting(result), "result not upgraded");
        helper.assertTrue(PortalGunItem.getCharges(result) == 3, "upgrade lost the gun's data");
        helper.succeed();
    }
}
