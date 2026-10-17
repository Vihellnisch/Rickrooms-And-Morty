package com.rickandmorty.registry;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.worldgen.anomaly.AnomalyPiece;
import com.rickandmorty.worldgen.anomaly.AnomalyStructure;
import com.rickandmorty.worldgen.site.SitePiece;
import com.rickandmorty.worldgen.site.SiteStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Overworld anomaly structures (the actual structures are data: data/rickrooms/worldgen/structure). */
public class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, RickAndMortyMod.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, RickAndMortyMod.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<AnomalyStructure>> ANOMALY =
            STRUCTURE_TYPES.register("anomaly", () -> () -> AnomalyStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> ANOMALY_PIECE =
            PIECES.register("anomaly", () -> AnomalyPiece::new);

    /** Cronenberg World's ruins and camps, and the Council chamber. */
    public static final DeferredHolder<StructureType<?>, StructureType<SiteStructure>> SITE =
            STRUCTURE_TYPES.register("site", () -> () -> SiteStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SITE_PIECE =
            PIECES.register("site", () -> SitePiece::new);
}
