package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.compat.Curios;
import com.rickandmorty.gadget.GogglesItem;
import com.rickandmorty.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

/** The goggles in Curios' head slot (the second test only runs with Curios installed). */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V21GameTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void theGogglesFitCuriosHeadSlot(GameTestHelper helper) {
        TagKey<Item> head = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "head"));
        helper.assertTrue(new ItemStack(ModItems.INTERDIMENSIONAL_GOGGLES.get()).is(head), "the goggles aren't tagged for the head slot");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void gogglesInTheCurioSlotWork(GameTestHelper helper) {
        if (!Curios.loaded()) {
            helper.succeed();                                   // nothing to test without Curios
            return;
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        player.removeEffect(MobEffects.NIGHT_VISION);
        helper.assertFalse(GogglesItem.wearing(player), "wearing goggles before putting them on");
        WithCurios.putOn(player, new ItemStack(ModItems.INTERDIMENSIONAL_GOGGLES.get()));
        helper.assertTrue(GogglesItem.wearing(player), "goggles in the curio head slot don't count");
        helper.succeedWhen(() -> {
            helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "no night vision from the curio slot");
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    /** Kept apart so the class loads without Curios. */
    private static final class WithCurios {
        static void putOn(ServerPlayer player, ItemStack stack) {
            var handler = CuriosApi.getCuriosInventory(player).orElseThrow().getStacksHandler("head").orElseThrow();
            handler.getStacks().setStackInSlot(0, stack);
        }
    }
}
