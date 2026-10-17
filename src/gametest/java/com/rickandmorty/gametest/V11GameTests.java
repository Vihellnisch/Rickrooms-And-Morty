package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.cruiser.CruiserRocket;
import com.rickandmorty.entity.cruiser.CruiserTier;
import com.rickandmorty.entity.cruiser.CruiserUpgrade;
import com.rickandmorty.entity.cruiser.CruiserWeapons;
import com.rickandmorty.entity.cruiser.SpaceCruiserEntity;
import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Tests for v0.11: Rick's Space Cruiser. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V11GameTests {
    private static final String EMPTY = "empty";

    private static SpaceCruiserEntity cruiser(GameTestHelper helper, int x, int z, CruiserUpgrade... upgrades) {
        SpaceCruiserEntity cruiser = helper.spawn(ModEntities.SPACE_CRUISER.get(), new BlockPos(x, 2, z));
        cruiser.setYRot(0.0f);                       // facing south, +z
        for (CruiserUpgrade upgrade : upgrades) cruiser.install(upgrade);
        return cruiser;
    }

    @GameTest(template = EMPTY)
    public static void upgradesAreFitted(GameTestHelper helper) {
        SpaceCruiserEntity cruiser = cruiser(helper, 3, 3);
        ServerPlayer player = survivalPlayer(helper, "test-mechanic");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CRUISER_UPGRADES.get(CruiserUpgrade.MINIGUN).get()));
        cruiser.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(cruiser.has(CruiserUpgrade.MINIGUN) && player.getMainHandItem().isEmpty(), "the minigun wasn't fitted");
        helper.assertTrue(cruiser.selectedWeapon() == CruiserUpgrade.MINIGUN, "a fitted weapon should be selected");
        cruiser.install(CruiserUpgrade.ROCKETS);
        cruiser.cycleWeapon();
        helper.assertTrue(cruiser.selectedWeapon() == CruiserUpgrade.ROCKETS, "cycling didn't reach the rockets");
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 120)
    public static void weaponsHit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SpaceCruiserEntity cruiser = cruiser(helper, 2, 1, CruiserUpgrade.values());
        Zombie first = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 7));
        first.setNoAi(true);
        Vec3 aim = first.getEyePosition().subtract(cruiser.muzzle()).normalize();
        helper.assertTrue(CruiserWeapons.shoot(cruiser, null, CruiserUpgrade.DISINTEGRATOR, aim), "the disintegrator didn't fire");
        helper.assertFalse(first.isAlive(), "the disintegrator left something behind");
        Zombie second = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 7));
        second.setNoAi(true);
        float health = second.getHealth();
        CruiserWeapons.shoot(cruiser, null, CruiserUpgrade.MINIGUN, second.getEyePosition().subtract(cruiser.muzzle()).normalize());
        helper.assertTrue(second.getHealth() < health, "the minigun missed");
        CruiserWeapons.shoot(cruiser, null, CruiserUpgrade.FLAMETHROWER, second.position().add(0, 1, 0).subtract(cruiser.muzzle()).normalize());
        helper.assertTrue(second.isOnFire(), "the flamethrower didn't set it alight");
        CruiserWeapons.shoot(cruiser, null, CruiserUpgrade.ROCKETS, cruiser.forward());
        List<CruiserRocket> rockets = level.getEntitiesOfClass(CruiserRocket.class, new AABB(cruiser.blockPosition()).inflate(8));
        helper.assertTrue(rockets.size() == 2, rockets.size() + " rockets");
        helper.assertFalse(CruiserWeapons.shoot(cruiser, null, CruiserUpgrade.ROCKETS, cruiser.forward()), "the rockets fired again without cooling down");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void repulsorThrowsThingsAway(GameTestHelper helper) {
        SpaceCruiserEntity cruiser = cruiser(helper, 3, 3, CruiserUpgrade.REPULSOR);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 6));   // (a no-AI mob can't be knocked about)
        double before = zombie.distanceTo(cruiser);
        CruiserWeapons.repulse(cruiser, null);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(zombie.distanceTo(cruiser) > before + 1.5, "it only moved " + (zombie.distanceTo(cruiser) - before));
            helper.succeed();
        });
    }

    /** Each mark of drill against a 7x7 wall with obsidian in it: how many blocks one shot cuts. */
    private static int drillWall(GameTestHelper helper, int mark, int z, double aimY) {
        SpaceCruiserEntity cruiser = cruiser(helper, 4, z - 6, CruiserUpgrade.LASER_DRILL);
        cruiser.setTier(CruiserTier.DRILL, mark);
        ServerPlayer pilot = survivalPlayer(helper, "test-driller-" + mark);
        pilot.startRiding(cruiser, true);
        for (int x = 1; x <= 7; x++) {
            for (int y = 1; y <= 7; y++) helper.setBlock(new BlockPos(x, y, z), (x + y) % 4 == 0 ? Blocks.OBSIDIAN : Blocks.STONE);
        }
        Vec3 target = helper.absoluteVec(new Vec3(4.5, aimY, z));
        CruiserWeapons.shoot(cruiser, pilot, CruiserUpgrade.LASER_DRILL, target.subtract(cruiser.muzzle()).normalize());
        int cut = 0;
        for (int x = 1; x <= 7; x++) {
            for (int y = 1; y <= 7; y++) if (helper.getBlockState(new BlockPos(x, y, z)).isAir()) cut++;
        }
        helper.getLevel().getServer().getPlayerList().remove(pilot);
        return cut;
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void laserDrillMarksCutMore(GameTestHelper helper) {
        int one = drillWall(helper, 1, 7, 5.5);                 // at the stone at (4,5)
        helper.assertTrue(one == 1, "Mk I should cut one block, cut " + one);
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 5, 7)).isAir(), "Mk I missed the block it was aimed at");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void laserDrillMkTwoCutsThreeByThree(GameTestHelper helper) {
        int cut = drillWall(helper, 2, 7, 4.5);
        // 3x3 around (4,4): the obsidian at (4,4), (3,5), (5,3) stays
        helper.assertTrue(cut == 6, "Mk II should cut the 6 stone of a 3x3, cut " + cut);
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 4, 7)).is(Blocks.OBSIDIAN), "Mk II cut obsidian");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void laserDrillMkThreeCutsObsidian(GameTestHelper helper) {
        int cut = drillWall(helper, 3, 7, 4.5);
        helper.assertTrue(cut == 25, "Mk III should cut all of a 5x5, obsidian too; cut " + cut);
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void thrustersFlyFaster(GameTestHelper helper) {
        SpaceCruiserEntity plain = cruiser(helper, 2, 2);
        SpaceCruiserEntity fast = cruiser(helper, 6, 2);
        fast.setTier(CruiserTier.THRUSTERS, 3);
        for (int i = 0; i < 120; i++) {
            plain.fly(1.0f, 0.0f, false, false, 0.0f);
            fast.fly(1.0f, 0.0f, false, false, 0.0f);
        }
        double slow = plain.getDeltaMovement().horizontalDistance(), quick = fast.getDeltaMovement().horizontalDistance();
        helper.assertTrue(slow < 1.2 && quick > 2.4, "top speeds: plain " + slow + ", Mk III " + quick);
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void batteryRechargesAndPowerRunsOut(GameTestHelper helper) {
        SpaceCruiserEntity cruiser = cruiser(helper, 3, 3, CruiserUpgrade.DISINTEGRATOR);
        cruiser.setPower(10);
        helper.assertFalse(CruiserWeapons.shoot(cruiser, null, CruiserUpgrade.DISINTEGRATOR, cruiser.forward()), "fired with no power");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(cruiser.power() > 60, "the battery didn't recharge it: " + cruiser.power());
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void breaksIntoItsParts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SpaceCruiserEntity cruiser = cruiser(helper, 3, 3, CruiserUpgrade.ROCKETS, CruiserUpgrade.AUTO_PARK);
        ServerPlayer player = survivalPlayer(helper, "test-wrecker");
        for (int i = 0; i < 20 && !cruiser.isRemoved(); i++) cruiser.hurt(level.damageSources().playerAttack(player), 5.0f);
        helper.assertTrue(cruiser.isRemoved(), "it wouldn't break");
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(cruiser.blockPosition()).inflate(4));
        helper.assertTrue(drops.stream().anyMatch(d -> d.getItem().is(ModItems.SPACE_CRUISER.get())), "the cruiser itself wasn't dropped");
        helper.assertTrue(drops.stream().anyMatch(d -> d.getItem().is(ModItems.CRUISER_UPGRADES.get(CruiserUpgrade.ROCKETS).get())), "its upgrades were lost");
        level.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void autoParkComesWhenCalled(GameTestHelper helper) {
        SpaceCruiserEntity cruiser = cruiser(helper, 2, 2, CruiserUpgrade.AUTO_PARK);
        Vec3 target = cruiser.position().add(10, 3, 10);
        helper.assertTrue(cruiser.summon(target), "it wouldn't come");
        double before = cruiser.position().distanceTo(target);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(cruiser.position().distanceTo(target) < before - 5, "it didn't come closer: " + cruiser.position().distanceTo(target));
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void portalDriveJumpsWithItsPilot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SpaceCruiserEntity cruiser = cruiser(helper, 3, 3, CruiserUpgrade.PORTAL_DRIVE);
        ServerPlayer pilot = survivalPlayer(helper, "test-traveller");
        pilot.startRiding(cruiser, true);
        BlockPos far = helper.absolutePos(new BlockPos(3, 2, 3)).offset(300, 0, 0);
        level.getChunk(far);
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        PortalDestinations dial = PortalDestinations.EMPTY.withAdded(new PortalDestinations.Destination("away", level.dimension(), far.getX() + 0.5,
                far.getY(), far.getZ() + 0.5, 0.0f)).withSelected(0);
        gun.set(ModDataComponents.PORTAL_DESTINATIONS.get(), dial);
        pilot.getInventory().add(gun);
        CruiserWeapons.portalJump(cruiser, pilot);
        helper.assertTrue(cruiser.portalJumpPending(), "the portal didn't open");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(cruiser.position().distanceTo(Vec3.atBottomCenterOf(far)) < 4, "the cruiser isn't at the destination: " + cruiser.position());
            helper.assertTrue(pilot.getVehicle() == cruiser, "the pilot didn't come along");
            level.getServer().getPlayerList().remove(pilot);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void boostersMakePowerLast(GameTestHelper helper) {
        SpaceCruiserEntity plain = cruiser(helper, 2, 2, CruiserUpgrade.PORTAL_DRIVE);
        SpaceCruiserEntity boosted = cruiser(helper, 6, 2, CruiserUpgrade.PORTAL_DRIVE);
        ServerPlayer player = survivalPlayer(helper, "test-engineer");
        // fit Mk I, then Mk III over it: the Mk I comes back
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CRUISER_BOOSTERS.get(0).get()));
        boosted.interact(player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CRUISER_BOOSTERS.get(2).get()));
        boosted.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(boosted.booster() == 3, "the Mk III wasn't fitted: " + boosted.booster());
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.CRUISER_BOOSTERS.get(0).get())), "the Mk I wasn't handed back");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CRUISER_BOOSTERS.get(1).get()));
        boosted.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(boosted.booster() == 3 && !player.getMainHandItem().isEmpty(), "a Mk II replaced the Mk III");
        helper.assertTrue(boosted.maxPower() > plain.maxPower() * 2, "the battery isn't any bigger");
        helper.getLevel().getServer().getPlayerList().remove(player);     // (other tests look for the nearest player)
        // the same shot costs much less
        plain.setPower(1000);
        boosted.setPower(1000);
        plain.usePower(CruiserUpgrade.PORTAL_DRIVE.cost);
        boosted.usePower(CruiserUpgrade.PORTAL_DRIVE.cost);
        helper.assertTrue(boosted.exactPower() > plain.exactPower() + 300, "the booster didn't cut the cost: " + boosted.exactPower() + " vs " + plain.exactPower());
        // and charging is quicker
        plain.setPower(0);
        boosted.setPower(0);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(boosted.power() > plain.power() * 3, "the booster didn't charge faster: " + boosted.power() + " vs " + plain.power());
            helper.succeed();
        });
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper, String name) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }
}
