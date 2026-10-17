package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.item.DnaLock;
import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** v0.22: the DNA Lock Upgrade and /rickrooms gun. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V22GameTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void aDnaLockedGunComesBackAfterDeath(GameTestHelper helper) {
        ServerPlayer dead = helper.makeMockServerPlayerInLevel();
        ServerPlayer respawned = helper.makeMockServerPlayerInLevel();
        ItemStack locked = new ItemStack(ModItems.PORTAL_GUN.get());
        locked.set(ModDataComponents.DNA_LOCK.get(), true);
        locked.set(ModDataComponents.PORTAL_CHARGES.get(), 3);
        locked.set(ModDataComponents.PORTAL_DESTINATIONS.get(), PortalDestinations.EMPTY.withAdded(
                new PortalDestinations.Destination("Home", Level.OVERWORLD, 1, 70, 2, 0.0f)));
        ItemStack plain = new ItemStack(ModItems.PORTAL_GUN.get());
        List<ItemEntity> drops = new ArrayList<>();
        for (ItemStack stack : List.of(locked, plain, new ItemStack(Items.DIRT, 5))) {
            drops.add(new ItemEntity(helper.getLevel(), dead.getX(), dead.getY(), dead.getZ(), stack));
        }
        helper.assertValueEqual(DnaLock.keep(dead, drops), 1, "locked guns kept");
        helper.assertValueEqual(drops.size(), 2, "drops left");
        helper.assertTrue(drops.stream().noneMatch(d -> PortalGunItem.hasDnaLock(d.getItem())), "the locked gun was still dropped");
        respawned.getInventory().clearContent();
        DnaLock.giveBack(dead, respawned);
        ItemStack back = respawned.getInventory().items.stream().filter(s -> s.is(ModItems.PORTAL_GUN.get())).findFirst().orElse(ItemStack.EMPTY);
        helper.assertFalse(back.isEmpty(), "the gun didn't come back");
        helper.assertTrue(PortalGunItem.hasDnaLock(back), "it lost its DNA lock");
        helper.assertValueEqual(PortalGunItem.getCharges(back), 3, "charges");
        helper.assertValueEqual(PortalGunItem.getDestinations(back).destinations().size(), 1, "saved places");
        // only once
        respawned.getInventory().clearContent();
        DnaLock.giveBack(dead, respawned);
        helper.assertTrue(respawned.getInventory().isEmpty(), "the gun came back twice");
        helper.getLevel().getServer().getPlayerList().remove(dead);
        helper.getLevel().getServer().getPlayerList().remove(respawned);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theGunCommandGivesAFullyUpgradedGun(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        var server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(2).withSuppressedOutput(), "rickrooms gun");
        ItemStack gun = player.getInventory().items.stream().filter(s -> s.is(ModItems.PORTAL_GUN.get())).findFirst().orElse(ItemStack.EMPTY);
        helper.assertFalse(gun.isEmpty(), "no gun given");
        helper.assertTrue(PortalGunItem.hasTargeting(gun) && PortalGunItem.hasBlender(gun) && PortalGunItem.hasDnaLock(gun), "missing an upgrade");
        helper.assertValueEqual(PortalGunItem.getCharges(gun), PortalGunItem.MAX_CHARGES, "charges");
        // every level this server has is on the dial (the test server may only have the overworld)
        PortalDestinations dial = PortalGunItem.getDestinations(gun);
        for (var level : com.rickandmorty.worldgen.level.BackroomsLevel.values()) {
            helper.assertValueEqual(dial.knows(level.key()), server.getLevel(level.key()) != null, "dial knows " + level.getSerializedName());
        }
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theDnaLockRecipesExist(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String id : List.of("dna_lock_chip", "portal_gun_dna_lock")) {
            helper.assertTrue(recipes.byKey(RickAndMortyMod.id(id)).isPresent(), "missing recipe " + id);
        }
        helper.succeed();
    }

    /** A dial that knows every level must still fit through the network (it was capped at 16 and kicked the player). */
    @GameTest(template = EMPTY)
    public static void aDialKnowingEveryLevelCanBeSent(GameTestHelper helper) {
        PortalDestinations dial = PortalDestinations.EMPTY;
        for (var level : com.rickandmorty.worldgen.level.BackroomsLevel.values()) dial = dial.withLearnedDimension(level.key());
        dial = dial.withLearnedDimension(com.rickandmorty.worldgen.ModDimensions.CRONENBERG);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        PortalDestinations.STREAM_CODEC.encode(buffer, dial);
        PortalDestinations back = PortalDestinations.STREAM_CODEC.decode(buffer);
        helper.assertValueEqual(back.knownDimensions(), dial.knownDimensions(), "dimensions after a round trip");
        helper.succeed();
    }

    /** Every level and Cronenberg World fit on the dial, with room for more. */
    @GameTest(template = EMPTY)
    public static void theDialHasRoomForEveryLevel(GameTestHelper helper) {
        PortalDestinations dial = PortalDestinations.EMPTY;
        for (var level : com.rickandmorty.worldgen.level.BackroomsLevel.values()) dial = dial.withLearnedDimension(level.key());
        dial = dial.withLearnedDimension(com.rickandmorty.worldgen.ModDimensions.CRONENBERG);
        helper.assertValueEqual(dial.knownDimensions().size(), com.rickandmorty.worldgen.level.BackroomsLevel.values().length + 1, "learned");
        helper.succeed();
    }
}
