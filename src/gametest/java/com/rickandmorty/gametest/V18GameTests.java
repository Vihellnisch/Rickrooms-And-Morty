package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.gadget.FreezeRayItem;
import com.rickandmorty.gadget.OreSight;
import com.rickandmorty.gadget.ShrinkRayBlock;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModEffects;
import com.rickandmorty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests for Rick's gadgets. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V18GameTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void theShrinkRayShrinksAndGrows(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rayPos = new BlockPos(2, 2, 4);
        helper.setBlock(rayPos, ModBlocks.SHRINK_RAY.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 3));
        zombie.setNoAi(true);
        BlockPos abs = helper.absolutePos(rayPos);
        helper.assertTrue(ShrinkRayBlock.fire(level, abs, level.getBlockState(abs)) == 1, "it missed");
        helper.assertTrue(zombie.hasEffect(ModEffects.SHRUNK), "not shrunk");
        helper.assertTrue(Math.abs(zombie.getAttributeValue(Attributes.SCALE) - 0.5) < 0.01, "scale " + zombie.getAttributeValue(Attributes.SCALE));
        helper.assertTrue(ShrinkRayBlock.fire(level, abs, level.getBlockState(abs)) == 0, "it fired again straight away");
        // cooled down, with the lens, set to grow: back to normal, then big
        BlockState grow = level.getBlockState(abs).setValue(ShrinkRayBlock.COOLING, false).setValue(ShrinkRayBlock.UPGRADED, true).setValue(ShrinkRayBlock.GROW, true);
        level.setBlock(abs, grow, 3);
        ShrinkRayBlock.fire(level, abs, grow);
        helper.assertFalse(zombie.hasEffect(ModEffects.SHRUNK), "growing didn't undo the shrinking");
        level.setBlock(abs, grow, 3);
        ShrinkRayBlock.fire(level, abs, grow);
        helper.assertTrue(Math.abs(zombie.getAttributeValue(Attributes.SCALE) - 2.0) < 0.01, "not enlarged: " + zombie.getAttributeValue(Attributes.SCALE));
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theFreezeRayFreezes(GameTestHelper helper) {
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        Zombie attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 2));
        helper.assertTrue(FreezeRayItem.freeze(target, attacker, ModItems.FREEZE_RAY.get()), "it didn't freeze");
        helper.assertTrue(target.hasEffect(ModEffects.FROZEN), "no frozen effect");
        helper.assertTrue(target.getAttributeValue(Attributes.MOVEMENT_SPEED) < 0.001, "it can still move: " + target.getAttributeValue(Attributes.MOVEMENT_SPEED));
        helper.succeed();
    }

    private static int cookTime(GameTestHelper helper, BlockPos pos) {
        var furnace = (AbstractFurnaceBlockEntity) helper.getBlockEntity(pos);
        var tag = furnace.saveWithoutMetadata(helper.getLevel().registryAccess());
        return tag.getShort("CookTime") + (furnace.getItem(2).isEmpty() ? 0 : 1000 * furnace.getItem(2).getCount());
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void theTimeCrystalSpeedsThingsUp(GameTestHelper helper) {
        BlockPos slow = new BlockPos(1, 2, 1), fast = new BlockPos(5, 2, 5);
        for (BlockPos p : new BlockPos[]{slow, fast}) {
            helper.setBlock(p, Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.NORTH));
            var furnace = (AbstractFurnaceBlockEntity) helper.getBlockEntity(p);
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 8));
            furnace.setItem(1, new ItemStack(Items.COAL, 8));
        }
        helper.setBlock(fast.east(), ModBlocks.TIME_CRYSTAL.get());
        helper.runAfterDelay(60, () -> {
            int a = cookTime(helper, slow), b = cookTime(helper, fast);
            helper.assertTrue(b > a * 3, "the crystal didn't speed it up: " + a + " vs " + b);
            helper.succeed();
        });
    }

    /** A battery-run furnace sped up by a crystal used to run out of burn between top-ups and never finish. */
    @GameTest(template = EMPTY, timeoutTicks = 260)
    public static void theTimeCrystalSpeedsUpBatteryFurnaces(GameTestHelper helper) {
        BlockPos furnacePos = new BlockPos(2, 2, 2);
        helper.setBlock(new BlockPos(1, 2, 2), ModBlocks.MICROVERSE_BATTERY.get());
        helper.setBlock(furnacePos, Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.NORTH));
        var furnace = (AbstractFurnaceBlockEntity) helper.getBlockEntity(furnacePos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 3));   // no fuel: the battery runs it
        helper.setBlock(new BlockPos(4, 2, 2), ModBlocks.TIME_CRYSTAL.get());
        // 3 smelts take 600 ticks at normal speed; sped up they're done well inside the timeout
        helper.succeedWhen(() -> helper.assertTrue(furnace.getItem(2).getCount() == 3, "only made " + furnace.getItem(2)));
    }

    @GameTest(template = EMPTY)
    public static void theCruiserDialFetchesTheGun(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, new ItemStack(Items.BREAD));
        player.getInventory().setItem(20, new ItemStack(ModItems.PORTAL_GUN.get()));
        com.rickandmorty.entity.cruiser.CruiserWeapons.gunToHand(player);
        helper.assertTrue(player.getMainHandItem().is(ModItems.PORTAL_GUN.get()), "the gun isn't in hand");
        helper.assertTrue(player.getInventory().getItem(20).is(Items.BREAD), "what was in hand went missing");
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theGogglesSeeOres(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.DIAMOND_ORE);
        helper.setBlock(new BlockPos(5, 1, 3), Blocks.DEEPSLATE_IRON_ORE);
        var found = OreSight.scan(helper.getLevel(), helper.absolutePos(new BlockPos(4, 3, 3)), 4);
        helper.assertTrue(found.stream().anyMatch(o -> o.pos().equals(helper.absolutePos(new BlockPos(3, 1, 3))) && o.color() == 0x5CF2F2), "no diamond");
        helper.assertTrue(found.stream().anyMatch(o -> o.pos().equals(helper.absolutePos(new BlockPos(5, 1, 3)))), "no iron");
        helper.assertTrue(OreSight.color(Blocks.STONE.defaultBlockState()) < 0, "stone isn't an ore");
        helper.succeed();
    }
}
