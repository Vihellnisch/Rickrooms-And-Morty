package com.rickandmorty.gametest;

import com.mojang.authlib.GameProfile;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.cronenberg.Mutation;
import com.rickandmorty.entity.BehemothEntity;
import com.rickandmorty.entity.CouncilLook;
import com.rickandmorty.entity.CouncilMemberEntity;
import com.rickandmorty.entity.CronenbergEntity;
import com.rickandmorty.entity.CronenbergKind;
import com.rickandmorty.entity.MantisEntity;
import com.rickandmorty.entity.PortalEntity;
import com.rickandmorty.entity.SurvivorEntity;
import com.rickandmorty.entity.SurvivorKind;
import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModEffects;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.registry.ModWorldgen;
import com.rickandmorty.worldgen.ModDimensions;
import com.rickandmorty.worldgen.site.SiteKind;
import com.rickandmorty.worldgen.site.SitePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Tests for v0.7: Cronenberg World, mutation, the Blender Upgrade and the Council. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V7GameTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void cronenbergWorldLoads(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        helper.assertTrue(access.registryOrThrow(Registries.DIMENSION_TYPE).containsKey(RickAndMortyMod.id("cronenberg")), "no dimension type");
        helper.assertTrue(access.registryOrThrow(Registries.NOISE_SETTINGS).containsKey(RickAndMortyMod.id("cronenberg")), "no noise settings");
        var biomes = access.registryOrThrow(Registries.BIOME);
        for (String b : List.of("flesh_plains", "bone_forest", "blood_marsh", "tumor_wastes", "cronenberg_suburbs", "blood_sea")) {
            helper.assertTrue(biomes.containsKey(RickAndMortyMod.id(b)), "biome missing: " + b);
        }
        var structures = access.registryOrThrow(Registries.STRUCTURE);
        var sets = access.registryOrThrow(Registries.STRUCTURE_SET);
        for (SiteKind kind : SiteKind.values()) {
            helper.assertTrue(structures.containsKey(RickAndMortyMod.id(kind.getSerializedName())), "structure missing: " + kind);
            helper.assertTrue(sets.containsKey(RickAndMortyMod.id(kind.getSerializedName())), "structure set missing: " + kind);
        }
        var placed = access.registryOrThrow(Registries.PLACED_FEATURE);
        for (String f : List.of("ribcage_common", "tumor_mound_common", "flesh_grass_dense", "pustules", "teeth", "eyestalks")) {
            helper.assertTrue(placed.containsKey(RickAndMortyMod.id(f)), "placed feature missing: " + f);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void boneTreesAndTumorsGrow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = far(level, 0);
        level.setBlock(origin, ModBlocks.VEINED_FLESH.get().defaultBlockState(), 3);
        ModWorldgen.RIBCAGE.get().place(NoneFeatureConfiguration.INSTANCE, level, level.getChunkSource().getGenerator(), RandomSource.create(3), origin.above());
        int bone = count(level, origin, 16, ModBlocks.CALCIFIED_BONE.get());
        helper.assertTrue(bone > 8, "ribcage too small: " + bone);

        BlockPos mound = far(level, 1);
        level.setBlock(mound, ModBlocks.VEINED_FLESH.get().defaultBlockState(), 3);
        ModWorldgen.TUMOR_MOUND.get().place(NoneFeatureConfiguration.INSTANCE, level, level.getChunkSource().getGenerator(), RandomSource.create(4), mound.above());
        int tumor = count(level, mound, 8, ModBlocks.TUMOR.get());
        helper.assertTrue(tumor > 5, "tumor mound too small: " + tumor);
        helper.succeed();
    }

    private static BlockPos far(ServerLevel level, int slot) {
        // the test world is kept between runs: build somewhere new each time, not on top of last run's sites
        int x = 3000 + slot * 96, z = 3000 + (int) (System.currentTimeMillis() / 1000 % 20000) * 16;
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
    }

    private static int count(ServerLevel level, BlockPos center, int radius, net.minecraft.world.level.block.Block block) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 24, radius))) {
            if (level.getBlockState(p).is(block)) n++;
        }
        return n;
    }

    private static BlockPos buildSite(ServerLevel level, SiteKind kind, int slot) {
        BlockPos origin = far(level, 4 + slot);
        // the test world is a thin superflat: give the pit room to be dug below the surface
        if (kind == SiteKind.FLESH_PIT) origin = origin.above(24);
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) level.getChunk((origin.getX() >> 4) + cx, (origin.getZ() >> 4) + cz);
        }
        SitePiece piece = new SitePiece(kind, origin, 1234L + slot);
        BoundingBox everywhere = new BoundingBox(origin.getX() - 32, level.getMinBuildHeight(), origin.getZ() - 32,
                origin.getX() + 32, level.getMaxBuildHeight(), origin.getZ() + 32);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1),
                everywhere, new ChunkPos(origin), origin);
        return origin;
    }

    @GameTest(template = EMPTY)
    public static void sitesBuildAndArePopulated(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos camp = buildSite(level, SiteKind.SURVIVOR_CAMP, 0);
        var survivors = nearby(level, SurvivorEntity.class, camp, 20);
        helper.assertTrue(survivors.size() == 3, "camp survivors: " + survivors.size());
        helper.assertTrue(survivors.stream().allMatch(s -> !s.getOffers().isEmpty()), "a survivor has nothing to trade");
        helper.assertTrue(count(level, camp, 14, ModBlocks.TOOTH_SPIKE.get()) > 20, "no teeth round the camp");
        helper.assertTrue(level.getBlockState(camp.offset(0, 1, 2)).isAir(), "the house has no door");

        BlockPos pit = buildSite(level, SiteKind.FLESH_PIT, 1);
        helper.assertTrue(nearby(level, BehemothEntity.class, pit, 24).size() == 1, "no Behemoth in its pit");
        helper.assertTrue(level.getBlockState(pit.below(6)).isAir(), "the pit isn't dug");

        BlockPos school = buildSite(level, SiteKind.HIGH_SCHOOL, 2);
        helper.assertTrue(nearby(level, MantisEntity.class, school, 30).size() >= 5, "school has no Mantis People");
        helper.assertTrue(count(level, school, 22, Blocks.BRICKS) > 200, "school has no walls");

        BlockPos house = buildSite(level, SiteKind.RUINED_HOUSE, 3);
        helper.assertTrue(level.getBlockState(house.offset(3, 1, -4)).is(Blocks.CHEST), "ruined house has no chest");

        BlockPos council = buildSite(level, SiteKind.COUNCIL_CHAMBER, 4);
        var members = nearby(level, CouncilMemberEntity.class, council, 14);
        helper.assertTrue(members.size() == 5, "council members: " + members.size());
        helper.assertTrue(level.getBlockState(council.offset(0, 1, -10)).is(Blocks.CHEST), "no council vault");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void blenderPortalGrabsAndBlends(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 center = helper.absoluteVec(new Vec3(4.5, 2.2, 4.5));
        owner.moveTo(center.x + 1.0, center.y - 1.2, center.z, 0, 0);   // right next to it: the owner is never grabbed
        PortalEntity portal = PortalEntity.spawnInAir(level, center, 0.0f, null, owner, true);
        helper.assertTrue(portal.isBlender(), "not a blender portal");
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 6));
        zombie.setNoAi(true);
        helper.succeedWhen(() -> {
            helper.assertFalse(zombie.isAlive(), "zombie still alive, phase " + portal.getPhase() + ", health " + zombie.getHealth());
            helper.assertTrue(owner.isAlive() && owner.getHealth() == owner.getMaxHealth(), "the owner got blended");
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void attackModeShootsBlenderPortals(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 from = helper.absoluteVec(new Vec3(1.5, 1, 4.5));
        player.moveTo(from.x, from.y, from.z, -90.0f, 0.0f);
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        PortalGunItem.toggleMode(player, gun);
        helper.assertFalse(PortalGunItem.isAttackMode(gun), "attack mode without the upgrade");
        gun.set(ModDataComponents.BLENDER.get(), true);
        PortalGunItem.toggleMode(player, gun);
        helper.assertTrue(PortalGunItem.isAttackMode(gun), "mode didn't switch");
        helper.setBlock(new BlockPos(7, 1, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(7, 2, 4), Blocks.STONE);
        helper.runAfterDelay(2, () -> gun.use(level, player, InteractionHand.MAIN_HAND));
        helper.succeedWhen(() -> {
            var portals = level.getEntitiesOfClass(PortalEntity.class, new AABB(from, from).inflate(12));
            helper.assertTrue(portals.size() == 1 && portals.get(0).isBlender(), "no blender portal: " + portals.size());
            helper.assertTrue(PortalGunItem.getCharges(gun) == PortalGunItem.MAX_CHARGES - 1, "no charge used");
        });
    }

    @GameTest(template = EMPTY)
    public static void blenderUpgradeRecipe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        gun.set(ModDataComponents.TARGETING.get(), true);
        var input = new SmithingRecipeInput(new ItemStack(ModItems.BLENDER_CHIP.get()), gun, new ItemStack(ModItems.MANTIS_SCYTHE.get()));
        var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level);
        helper.assertTrue(recipe.isPresent(), "no smithing recipe");
        ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
        helper.assertTrue(PortalGunItem.hasBlender(result) && PortalGunItem.hasTargeting(result), "upgrades lost: " + result.getComponents());

        var notes = CraftingInput.of(2, 2, List.of(new ItemStack(ModItems.RICK_NOTES_1.get()), new ItemStack(ModItems.RICK_NOTES_2.get()),
                new ItemStack(ModItems.RICK_NOTES_3.get()), new ItemStack(ModItems.PORTAL_FLUID.get())));
        var crafted = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, notes, level);
        helper.assertTrue(crafted.isPresent() && crafted.get().value().assemble(notes, level.registryAccess()).is(ModItems.CRONENBERG_COORDINATES.get()),
                "notes don't make coordinates");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void coordinatesTeachTheGunCronenbergWorld(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        ItemStack coords = new ItemStack(ModItems.CRONENBERG_COORDINATES.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, coords);
        player.setItemInHand(InteractionHand.OFF_HAND, gun);
        coords.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        PortalDestinations dial = PortalGunItem.getDestinations(gun);
        helper.assertTrue(dial.knows(ModDimensions.CRONENBERG), "gun didn't learn Cronenberg World");
        helper.assertTrue(dial.selectedDimension().filter(ModDimensions.CRONENBERG::equals).isPresent(), "not selected");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void mutationStagesAndCure(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        new ItemStack(ModItems.CRONENBERG_FLESH.get()).finishUsingItem(helper.getLevel(), player);
        net.neoforged.neoforge.event.EventHooks.onItemUseFinish(player, new ItemStack(ModItems.CRONENBERG_FLESH.get()), 0, ItemStack.EMPTY);
        helper.assertTrue(Mutation.get(player) >= 8.0f, "eating flesh didn't mutate: " + Mutation.get(player));
        Mutation.set(player, 80.0f);
        helper.assertTrue(Mutation.stage(player) == 3, "stage " + Mutation.stage(player));
        helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) >= baseDamage + 4.0, "no strength at stage 3");
        helper.assertFalse(CronenbergEntity.looksHuman(player), "Cronenbergs still see a stage 3 player as food");
        new ItemStack(ModItems.CURE_SERUM.get()).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(Mutation.get(player) == 0.0f, "cure didn't cure: " + Mutation.get(player));
        // (the serum's Weakness lowers it further for a while, so just check the mutation bonus is gone)
        helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) <= baseDamage + 0.01, "strength stayed after the cure");
        helper.assertTrue(CronenbergEntity.looksHuman(player), "cured player still looks like family");

        // Survivor armour keeps the world out
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack piece = new ItemStack(switch (slot) {
                case HEAD -> ModItems.SURVIVOR_HELMET.get();
                case CHEST -> ModItems.SURVIVOR_CHESTPLATE.get();
                case LEGS -> ModItems.SURVIVOR_LEGGINGS.get();
                default -> ModItems.SURVIVOR_BOOTS.get();
            });
            player.setItemSlot(slot, piece);
        }
        helper.assertTrue(Mutation.survivorPieces(player) == 4, "armour pieces " + Mutation.survivorPieces(player));
        Mutation.expose(player, 10.0f);
        helper.assertTrue(Mutation.get(player) == 0.0f, "full Survivor Armor let mutation in: " + Mutation.get(player));
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void cronenbergsHuntAndSpreadMutation(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        CronenbergEntity stalker = helper.spawn(ModEntities.CRONENBERGS.get(CronenbergKind.STALKER).get(), new BlockPos(3, 1, 3));
        helper.succeedWhen(() -> {
            helper.assertTrue(player.getHealth() < player.getMaxHealth(), "stalker didn't attack");
            helper.assertTrue(Mutation.get(player) > 0.0f, "hits didn't mutate");
            helper.assertTrue(player.hasEffect(ModEffects.BLEEDING), "stalker claws didn't make you bleed");
        });
    }

    @GameTest(template = EMPTY)
    public static void pustulesBurst(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        helper.setBlock(new BlockPos(2, 0, 2), ModBlocks.FLESH.get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.PUSTULE.get());
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
        com.rickandmorty.block.PustuleBlock.burst(helper.getLevel(), at, player);
        helper.assertTrue(helper.getLevel().getBlockState(at).isAir(), "pustule didn't pop");
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "no poison");
        helper.assertTrue(Mutation.get(player) >= 5.0f, "no mutation from the pus");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void councilMembersLookLikeAlternateYous(GameTestHelper helper) {
        CouncilMemberEntity member = helper.spawn(ModEntities.COUNCIL_MEMBER.get(), new BlockPos(2, 1, 2));
        member.setProfile(new GameProfile(UUID.randomUUID(), "Ahmet"));
        helper.assertTrue(member.getName().getString().startsWith("Ahmet "), "name " + member.getName().getString());
        for (int packed = 1; packed < 7 * 16; packed++) {
            CouncilLook look = CouncilLook.unpack(packed);
            if (look.hat().ordinal() == (packed - 1) % 16) {
                helper.assertTrue(CouncilLook.unpack(look.pack()).equals(look), "look doesn't round trip: " + look);
            }
        }
        helper.assertFalse(member.getOffers().isEmpty(), "the Council has nothing to trade");
        helper.assertTrue(member.getOffers().stream().allMatch(o -> !o.getResult().isEmpty()), "an empty trade");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void survivorsTradeTheRightThings(GameTestHelper helper) {
        SurvivorEntity beth = helper.spawn(ModEntities.SURVIVORS.get(SurvivorKind.BETH).get(), new BlockPos(2, 1, 2));
        helper.assertTrue(beth.getOffers().stream().anyMatch(o -> o.getResult().is(ModItems.CURE_SERUM.get())), "Beth sells no cure");
        SurvivorEntity jerry = helper.spawn(ModEntities.SURVIVORS.get(SurvivorKind.JERRY).get(), new BlockPos(4, 1, 2));
        helper.assertTrue(jerry.getOffers().stream().anyMatch(o -> o.getResult().is(ModItems.SURVIVOR_CHESTPLATE.get())), "Jerry sells no gear");
        SurvivorEntity rick = helper.spawn(ModEntities.SURVIVORS.get(SurvivorKind.CRONENBERG_RICK).get(), new BlockPos(6, 1, 2));
        helper.assertTrue(rick.getMainHandItem().is(ModItems.PORTAL_GUN.get()), "Cronenberg Rick has no portal gun");
        helper.assertTrue(rick.getOffers().stream().anyMatch(o -> o.getResult().is(ModItems.BLENDER_CHIP.get())), "Rick doesn't sell the blender");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void cronenbergLootDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BehemothEntity behemoth = helper.spawn(ModEntities.BEHEMOTH.get(), new BlockPos(4, 1, 4));
        LootTable table = level.getServer().reloadableRegistries().getLootTable(behemoth.getLootTable());
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, behemoth)
                .withParameter(LootContextParams.ORIGIN, behemoth.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                .create(LootContextParamSets.ENTITY);
        List<ItemStack> drops = table.getRandomItems(params);
        helper.assertTrue(drops.stream().anyMatch(s -> s.is(ModItems.BEHEMOTH_HEART.get())), "Behemoth dropped no heart: " + drops);
        behemoth.discard();
        for (String chest : List.of("cronenberg_house", "survivor_camp", "high_school", "flesh_pit", "council_vault")) {
            LootTable loot = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/" + chest)));
            List<ItemStack> items = loot.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                    .create(LootContextParamSets.CHEST));
            helper.assertFalse(items.isEmpty(), "empty chest: " + chest);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void newAdvancementsLoad(GameTestHelper helper) {
        var advancements = helper.getLevel().getServer().getAdvancements();
        for (String name : List.of("cronenberg_research", "cronenberg", "survivors", "close_rick_counters", "cronenberged", "cured", "mantis",
                "high_school", "behemoth", "blender_upgrade", "blended", "council", "evil_gateway", "meltdown")) {
            helper.assertTrue(advancements.get(RickAndMortyMod.id(name)) != null, "advancement missing: " + name);
        }
        helper.succeed();
    }

    /** Regression: signs placed during world generation have no level yet (writing them crashed chunk generation). */
    @GameTest(template = EMPTY)
    public static void worldgenSignsTakeText(GameTestHelper helper) {
        var sign = new net.minecraft.world.level.block.entity.SignBlockEntity(BlockPos.ZERO, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState());
        com.rickandmorty.worldgen.site.SiteBuilder.writeSign(sign, helper.getLevel().registryAccess(), "Harry Herpson", "High School");
        helper.assertTrue(sign.getFrontText().getMessage(0, false).getString().equals("Harry Herpson"), "sign text not written");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void mantisScytheMakesYouBleed(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        new ItemStack(ModItems.MANTIS_SCYTHE.get()).hurtEnemy(zombie, player);
        helper.assertTrue(zombie.hasEffect(ModEffects.BLEEDING), "no bleeding");
        helper.succeed();
    }

    /** Every loaded entity of a type near a point (including ones in chunks nobody is ticking). */
    private static <T> List<T> nearby(ServerLevel level, Class<T> type, BlockPos center, double radius) {
        List<T> found = new java.util.ArrayList<>();
        for (var e : level.getAllEntities()) {
            if (type.isInstance(e) && e.position().distanceTo(Vec3.atCenterOf(center)) <= radius) found.add(type.cast(e));
        }
        return found;
    }
}
