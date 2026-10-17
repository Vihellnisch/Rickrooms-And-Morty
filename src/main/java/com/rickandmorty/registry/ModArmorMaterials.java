package com.rickandmorty.registry;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/** Armour from Cronenberg World. */
public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, RickAndMortyMod.MOD_ID);

    /** Plates of calcified Cronenberg bone lashed together with sinew. Tough, and ugly. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BONE_PLATE = MATERIALS.register("bone_plate",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 2);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 7);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 7);
            }), 12, SoundEvents.ARMOR_EQUIP_TURTLE, () -> Ingredient.of(ModItems.BONE_PLATE.get()),
                    List.of(new ArmorMaterial.Layer(RickAndMortyMod.id("bone_plate"))), 1.5f, 0.05f));

    /** The Interdimensional Goggles: they're for seeing, not for protection. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GOGGLES = MATERIALS.register("interdimensional_goggles",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 1);
                map.put(ArmorItem.Type.LEGGINGS, 1);
                map.put(ArmorItem.Type.CHESTPLATE, 1);
                map.put(ArmorItem.Type.HELMET, 1);
                map.put(ArmorItem.Type.BODY, 1);
            }), 10, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT),
                    List.of(new ArmorMaterial.Layer(RickAndMortyMod.id("interdimensional_goggles"))), 0.0f, 0.0f));

    /** Jerry's scavenged wasteland gear: not much protection, but it keeps Cronenberg World off your skin. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SURVIVOR = MATERIALS.register("survivor",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 2);
                map.put(ArmorItem.Type.LEGGINGS, 4);
                map.put(ArmorItem.Type.CHESTPLATE, 5);
                map.put(ArmorItem.Type.HELMET, 2);
                map.put(ArmorItem.Type.BODY, 5);
            }), 15, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER),
                    List.of(new ArmorMaterial.Layer(RickAndMortyMod.id("survivor"))), 0.0f, 0.0f));
}
