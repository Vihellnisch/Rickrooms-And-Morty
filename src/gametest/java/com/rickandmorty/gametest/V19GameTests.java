package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.pet.AmplifierModule;
import com.rickandmorty.pet.CognitionAmplifierItem;
import com.rickandmorty.pet.PetBrain;
import com.rickandmorty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Tests for the Cognition Amplifier. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V19GameTests {
    private static final String EMPTY = "empty";

    private static ServerPlayer owner(GameTestHelper helper, BlockPos at) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(at));
        player.moveTo(pos.x, pos.y, pos.z);
        return player;
    }

    private static void done(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    private static <T extends Mob> T tamed(GameTestHelper helper, EntityType<T> type, BlockPos at, ServerPlayer player) {
        T mob = helper.spawn(type, at);
        if (mob instanceof TamableAnimal tame) tame.tame(player);
        if (mob instanceof AbstractHorse horse) horse.tameWithName(player);
        if (mob instanceof Fox fox) fox.addTrustedUUID(player.getUUID());
        return mob;
    }

    @GameTest(template = EMPTY)
    public static void theAmplifierFitsEveryKindOfPet(GameTestHelper helper) {
        ServerPlayer player = owner(helper, new BlockPos(1, 2, 1));
        List<EntityType<? extends Mob>> kinds = List.of(EntityType.WOLF, EntityType.CAT, EntityType.PARROT, EntityType.FOX,
                EntityType.HORSE, EntityType.LLAMA, EntityType.CAMEL, EntityType.DONKEY);
        int x = 1;
        for (EntityType<? extends Mob> kind : kinds) {
            Mob pet = tamed(helper, kind, new BlockPos(x++ % 7, 2, 3 + x / 7 * 2), player);
            pet.setNoAi(true);
            helper.assertTrue(PetBrain.canWear(pet) && PetBrain.belongsTo(pet, player), kind + " can't wear one");
            float before = pet.getMaxHealth();
            PetBrain.fit(pet, player, AmplifierModule.MEDIC.bit());
            helper.assertTrue(PetBrain.isAmplified(pet), kind + " isn't amplified");
            helper.assertTrue(Math.abs(pet.getMaxHealth() - before * 1.5f) < 0.01f, kind + " health " + before + " -> " + pet.getMaxHealth());
            helper.assertTrue(pet.getHealth() == pet.getMaxHealth(), kind + " not healed up");
            ItemStack off = PetBrain.remove(pet);
            helper.assertTrue(off.is(ModItems.COGNITION_AMPLIFIER.get()) && CognitionAmplifierItem.modules(off) == AmplifierModule.MEDIC.bit(),
                    kind + " gave back " + off);
            helper.assertTrue(!PetBrain.isAmplified(pet) && Math.abs(pet.getMaxHealth() - before) < 0.01f, kind + " kept its boost");
        }
        // only your own pets
        Wolf wild = helper.spawn(EntityType.WOLF, new BlockPos(6, 2, 6));
        helper.assertFalse(PetBrain.belongsTo(wild, player), "a wild wolf counts as yours");
        done(helper, player);
    }

    @GameTest(template = EMPTY)
    public static void modulesMakeItStronger(GameTestHelper helper) {
        ServerPlayer player = owner(helper, new BlockPos(1, 2, 1));
        Wolf wolf = tamed(helper, EntityType.WOLF, new BlockPos(3, 2, 3), player);
        wolf.setNoAi(true);
        PetBrain.fit(wolf, player, 0);
        helper.assertTrue(Math.abs(PetBrain.damage(wolf) - 6.0f) < 0.01f, "damage " + PetBrain.damage(wolf));
        double armor = wolf.getAttributeValue(Attributes.ARMOR);
        helper.assertTrue(PetBrain.addModule(wolf, AmplifierModule.COMBAT), "combat module refused");
        helper.assertFalse(PetBrain.addModule(wolf, AmplifierModule.COMBAT), "the same module twice");
        helper.assertTrue(Math.abs(PetBrain.damage(wolf) - 12.0f) < 0.01f, "combat damage " + PetBrain.damage(wolf));
        helper.assertTrue(wolf.getAttributeValue(Attributes.ARMOR) > armor + 4, "no armour from the combat module");
        done(helper, player);
    }

    /** A cat (no attack of its own) with the Guardian Module goes for a zombie near it. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void anAmplifiedCatFights(GameTestHelper helper) {
        ServerPlayer player = owner(helper, new BlockPos(0, 2, 0));
        Cat cat = tamed(helper, EntityType.CAT, new BlockPos(2, 2, 2), player);
        PetBrain.fit(cat, player, AmplifierModule.GUARDIAN.bit());
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 2, 5));
        zombie.setNoAi(true);
        float full = zombie.getHealth();
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.getHealth() < full || !zombie.isAlive(), "the cat didn't fight");
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    /** With the Fetch Module, a dropped item ends up in its owner's pockets. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void theFetchModuleBringsThingsBack(GameTestHelper helper) {
        ServerPlayer player = owner(helper, new BlockPos(1, 2, 1));
        player.getInventory().clearContent();
        Wolf wolf = tamed(helper, EntityType.WOLF, new BlockPos(2, 2, 2), player);
        PetBrain.fit(wolf, player, AmplifierModule.FETCH.bit());
        Vec3 drop = helper.absoluteVec(new Vec3(6.5, 2.0, 6.5));
        ItemEntity item = new ItemEntity(helper.getLevel(), drop.x, drop.y, drop.z, new ItemStack(Items.DIAMOND, 3));
        item.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(item);
        helper.succeedWhen(() -> {
            helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 3, "owner has " + player.getInventory().countItem(Items.DIAMOND)
                    + ", wolf carries " + PetBrain.carried(wolf));
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }
}
