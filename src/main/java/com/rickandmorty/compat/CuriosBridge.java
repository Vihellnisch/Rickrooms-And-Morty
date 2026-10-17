package com.rickandmorty.compat;

import com.rickandmorty.gadget.GogglesItem;
import com.rickandmorty.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** The parts that call Curios. Only loaded when Curios is installed (see {@link Curios}). */
final class CuriosBridge {
    private CuriosBridge() {
    }

    static boolean wearingGoggles(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).map(curios -> curios.isEquipped(stack -> stack.getItem() instanceof GogglesItem)).orElse(false);
    }

    static void register() {
        CuriosApi.registerCurio(ModItems.INTERDIMENSIONAL_GOGGLES.get(), new ICurioItem() {
            /** The same night vision as wearing them as a helmet. */
            @Override
            public void curioTick(SlotContext slot, ItemStack stack) {
                GogglesItem.nightVision(slot.entity());
            }

            @Override
            public ICurio.SoundInfo getEquipSound(SlotContext slot, ItemStack stack) {
                return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0f, 1.2f);
            }
        });
    }
}
