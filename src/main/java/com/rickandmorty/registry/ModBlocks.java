package com.rickandmorty.registry;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.ExitDoorBlock;
import com.rickandmorty.block.FleshPlantBlock;
import com.rickandmorty.block.LevelBlocks;
import com.rickandmorty.block.FluorescentLightBlock;
import com.rickandmorty.block.PustuleBlock;
import com.rickandmorty.block.ToothSpikeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import com.rickandmorty.block.GatewayFrameBlock;
import com.rickandmorty.block.MicroverseBatteryBlock;
import com.rickandmorty.block.MicroverseWireBlock;
import com.rickandmorty.block.SlideBlock;
import com.rickandmorty.block.WarpGatewayBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RickAndMortyMod.MOD_ID);

    // --- Warp Gateway
    public static final DeferredBlock<WarpGatewayBlock> WARP_GATEWAY = BLOCKS.register("warp_gateway",
            () -> new WarpGatewayBlock(gatewayProperties()
                    .lightLevel(state -> state.getValue(WarpGatewayBlock.ACTIVE) ? 13 : 4)));

    public static final DeferredBlock<GatewayFrameBlock> GATEWAY_FRAME = BLOCKS.register("gateway_frame",
            () -> new GatewayFrameBlock(gatewayProperties()
                    .noLootTable()
                    .lightLevel(state -> 3)
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)));

    // --- The Backrooms
    public static final DeferredBlock<Block> BACKROOMS_WALLPAPER = BLOCKS.registerSimpleBlock("backrooms_wallpaper",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5f, 6.0f).sound(SoundType.WOOD));

    public static final DeferredBlock<Block> BACKROOMS_CARPET = BLOCKS.registerSimpleBlock("backrooms_carpet",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW).strength(0.8f).sound(SoundType.WOOL));

    public static final DeferredBlock<Block> BACKROOMS_CEILING = BLOCKS.registerSimpleBlock("backrooms_ceiling",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.0f).sound(SoundType.CALCITE));

    public static final DeferredBlock<FluorescentLightBlock> FLUORESCENT_LIGHT = BLOCKS.register("fluorescent_light",
            () -> new FluorescentLightBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.5f)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> state.getValue(FluorescentLightBlock.LIT) ? 15 : 0)));

    /** Slippery yellow plastic slide between the Backrooms' floors. */
    public static final DeferredBlock<SlideBlock> SLIDE = BLOCKS.register("slide",
            () -> new SlideBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.YELLOW_CONCRETE).friction(0.989f)));

    /** Infinite fuel for connected furnaces. There's a tiny civilisation in there. */
    public static final DeferredBlock<MicroverseBatteryBlock> MICROVERSE_BATTERY = BLOCKS.register("microverse_battery",
            () -> new MicroverseBatteryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)
                    .sound(SoundType.METAL).lightLevel(state -> 7).noOcclusion()));

    public static final DeferredBlock<MicroverseWireBlock> MICROVERSE_WIRE = BLOCKS.register("microverse_wire",
            () -> new MicroverseWireBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.5f)
                    .sound(SoundType.COPPER).noOcclusion().lightLevel(state -> 2)));

    // --- Cronenberg World: the ground is meat
    public static final DeferredBlock<Block> FLESH = BLOCKS.registerSimpleBlock("flesh", flesh(MapColor.COLOR_PINK, 1.5f));
    public static final DeferredBlock<Block> VEINED_FLESH = BLOCKS.registerSimpleBlock("veined_flesh", flesh(MapColor.COLOR_RED, 1.0f));
    public static final DeferredBlock<Block> ROTTING_FLESH = BLOCKS.registerSimpleBlock("rotting_flesh", flesh(MapColor.TERRACOTTA_BROWN, 0.8f));
    public static final DeferredBlock<Block> NECROTIC_FLESH = BLOCKS.registerSimpleBlock("necrotic_flesh",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_PURPLE).strength(3.0f, 6.0f).sound(SoundType.MUD));
    public static final DeferredBlock<Block> TUMOR = BLOCKS.registerSimpleBlock("tumor", flesh(MapColor.TERRACOTTA_PINK, 0.8f));
    /** Flesh full of eyes. They glow a little in the dark. */
    public static final DeferredBlock<Block> WATCHING_FLESH = BLOCKS.registerSimpleBlock("watching_flesh",
            flesh(MapColor.COLOR_PINK, 1.2f).lightLevel(state -> 4));
    /** Congealed blood: the Blood Marsh's sticky floor slows you down. */
    public static final DeferredBlock<Block> BLOOD_CLOT = BLOCKS.registerSimpleBlock("blood_clot",
            BlockBehaviour.Properties.of().mapColor(MapColor.CRIMSON_NYLIUM).strength(0.6f).sound(SoundType.HONEY_BLOCK)
                    .speedFactor(0.45f).jumpFactor(0.6f));
    public static final DeferredBlock<RotatedPillarBlock> CALCIFIED_BONE = BLOCKS.register("calcified_bone",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BONE_BLOCK).mapColor(MapColor.TERRACOTTA_WHITE)));
    public static final DeferredBlock<PustuleBlock> PUSTULE = BLOCKS.register("pustule",
            () -> new PustuleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).instabreak().noCollission()
                    .sound(SoundType.SLIME_BLOCK).lightLevel(state -> 2).pushReaction(PushReaction.DESTROY).noLootTable()));
    public static final DeferredBlock<ToothSpikeBlock> TOOTH_SPIKE = BLOCKS.register("tooth_spike",
            () -> new ToothSpikeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(1.5f)
                    .noCollission().noOcclusion().sound(SoundType.BONE_BLOCK).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<FleshPlantBlock> FLESH_GRASS = BLOCKS.register("flesh_grass",
            () -> new FleshPlantBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).replaceable().noCollission()
                    .instabreak().sound(SoundType.ROOTS).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY), false));
    public static final DeferredBlock<FleshPlantBlock> EYESTALK = BLOCKS.register("eyestalk",
            () -> new FleshPlantBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollission().instabreak()
                    .sound(SoundType.SLIME_BLOCK).lightLevel(state -> 3).offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY), true));

    // --- the other Backrooms levels
    /** Drop Limit's cloud sea: looks solid, holds nothing up. */
    public static final DeferredBlock<com.rickandmorty.block.SkyBlocks.Cloud> CLOUD = BLOCKS.register("cloud",
            () -> new com.rickandmorty.block.SkyBlocks.Cloud(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.2f).sound(SoundType.WOOL)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
    public static final DeferredBlock<com.rickandmorty.block.SkyBlocks.AirConditioner> AIR_CONDITIONER = BLOCKS.register("air_conditioner",
            () -> new com.rickandmorty.block.SkyBlocks.AirConditioner(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.5f).sound(SoundType.METAL)));
    public static final DeferredBlock<com.rickandmorty.block.SkyBlocks.FroopyTurf> FROOPY_TURF_PINK = turf("pink"), FROOPY_TURF_MINT = turf("mint"),
            FROOPY_TURF_LAVENDER = turf("lavender"), FROOPY_TURF_BUTTER = turf("butter");
    public static final DeferredBlock<com.rickandmorty.block.SkyBlocks.RedExitSign> RED_EXIT_SIGN = BLOCKS.register("red_exit_sign",
            () -> new com.rickandmorty.block.SkyBlocks.RedExitSign(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.5f).sound(SoundType.METAL)
                    .noOcclusion().lightLevel(s -> 9)));

    private static DeferredBlock<com.rickandmorty.block.SkyBlocks.FroopyTurf> turf(String colour) {
        return BLOCKS.register("froopy_turf_" + colour, () -> new com.rickandmorty.block.SkyBlocks.FroopyTurf(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK).strength(0.5f).sound(SoundType.WOOL)));
    }

    /** Froopyland's turf, in its four colours (pink, mint, lavender, butter). */
    public static BlockState turf(int colour) {
        return (switch (colour) {
            case 1 -> FROOPY_TURF_MINT;
            case 2 -> FROOPY_TURF_LAVENDER;
            case 3 -> FROOPY_TURF_BUTTER;
            default -> FROOPY_TURF_PINK;
        }).get().defaultBlockState();
    }

    public static final DeferredBlock<ExitDoorBlock> EXIT_DOOR = BLOCKS.register("exit_door",
            () -> new ExitDoorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(-1.0f, 3600000.0f)
                    .sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<LevelBlocks.NoclipVoid> NOCLIP_VOID = BLOCKS.register("noclip_void",
            () -> new LevelBlocks.NoclipVoid(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0f, 3600000.0f)
                    .noCollission().noLootTable().sound(SoundType.WOOL)));
    public static final DeferredBlock<Block> PANEL_LIGHT = BLOCKS.registerSimpleBlock("panel_light",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.5f).sound(SoundType.GLASS).lightLevel(state -> 15));
    public static final DeferredBlock<LevelBlocks.Pipe> PIPE = BLOCKS.register("pipe",
            () -> new LevelBlocks.Pipe(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<LevelBlocks.HangingCable> HANGING_CABLE = BLOCKS.register("hanging_cable",
            () -> new LevelBlocks.HangingCable(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instabreak().noCollission()
                    .noOcclusion().sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<LevelBlocks.DriedCorn> DRIED_CORN = BLOCKS.register("dried_corn",
            () -> new LevelBlocks.DriedCorn(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).instabreak().noCollission()
                    .sound(SoundType.CROP).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> OFFICE_CARPET = BLOCKS.registerSimpleBlock("office_carpet",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.8f).sound(SoundType.WOOL));
    public static final DeferredBlock<Block> OFFICE_WALL = BLOCKS.registerSimpleBlock("office_wall",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5f, 6.0f).sound(SoundType.CALCITE));
    public static final DeferredBlock<net.minecraft.world.level.block.CarpetBlock> OFFICE_DEBRIS = BLOCKS.register("office_debris",
            () -> new net.minecraft.world.level.block.CarpetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instabreak()
                    .noOcclusion().sound(SoundType.WOOL).replaceable()));
    public static final DeferredBlock<Block> GARAGE_CONCRETE = BLOCKS.registerSimpleBlock("garage_concrete",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f, 6.0f).sound(SoundType.STONE));
    public static final DeferredBlock<Block> CORRUGATED_METAL = BLOCKS.registerSimpleBlock("corrugated_metal",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL));
    public static final DeferredBlock<Block> LOBBY_TILE = BLOCKS.registerSimpleBlock("lobby_tile",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5f, 6.0f).sound(SoundType.STONE));
    public static final DeferredBlock<Block> SUNLIT_TILE = BLOCKS.registerSimpleBlock("sunlit_tile",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5f, 6.0f).sound(SoundType.STONE).lightLevel(state -> 11));
    public static final DeferredBlock<com.rickandmorty.block.MonolithBlock> MONOLITH = BLOCKS.register("monolith",
            () -> new com.rickandmorty.block.MonolithBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(-1.0f, 3600000.0f).sound(SoundType.STONE)));
    // --- Level Fun
    public static final DeferredBlock<com.rickandmorty.block.PartyBlocks.Balloon> BALLOON = BLOCKS.register("balloon",
            () -> new com.rickandmorty.block.PartyBlocks.Balloon(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instabreak()
                    .noOcclusion().noCollission().sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<com.rickandmorty.block.PartyBlocks.Present> PRESENT = BLOCKS.register("present",
            () -> new com.rickandmorty.block.PartyBlocks.Present(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.5f)
                    .noOcclusion().sound(SoundType.WOOL)));
    public static final DeferredBlock<net.minecraft.world.level.block.CarpetBlock> CONFETTI = BLOCKS.register("confetti",
            () -> new net.minecraft.world.level.block.CarpetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).instabreak()
                    .noOcclusion().sound(SoundType.WOOL).replaceable()));

    // --- the Poolrooms
    public static final DeferredBlock<Block> POOL_TILE = BLOCKS.registerSimpleBlock("pool_tile",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.5f, 6.0f).sound(SoundType.STONE));
    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> POOL_TILE_STAIRS = BLOCKS.register("pool_tile_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(POOL_TILE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(POOL_TILE.get())));
    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> POOL_TILE_SLAB = BLOCKS.register("pool_tile_slab",
            () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.ofFullCopy(POOL_TILE.get())));
    public static final DeferredBlock<Block> POOL_FLOOR_TILE = BLOCKS.registerSimpleBlock("pool_floor_tile",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5f, 6.0f).sound(SoundType.STONE));
    public static final DeferredBlock<Block> POOL_LIGHT = BLOCKS.registerSimpleBlock("pool_light",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.5f).sound(SoundType.GLASS).lightLevel(state -> 15));
    public static final DeferredBlock<com.rickandmorty.block.PoolDrainBlock> POOL_DRAIN = BLOCKS.register("pool_drain",
            () -> new com.rickandmorty.block.PoolDrainBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0f, 3600000.0f)
                    .sound(SoundType.METAL)));
    public static final DeferredBlock<com.rickandmorty.block.ScarecrowHeadBlock> SCARECROW_HEAD = BLOCKS.register("scarecrow_head",
            () -> new com.rickandmorty.block.ScarecrowHeadBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(1.0f)
                    .sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> CARPET_LEDGE = BLOCKS.register("carpet_ledge",
            () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.0f)
                    .sound(SoundType.WOOL)));

    private static BlockBehaviour.Properties flesh(MapColor color, float strength) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(strength, 3.0f).sound(SoundType.SLIME_BLOCK);
    }

    private static BlockBehaviour.Properties gatewayProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0f, 1200.0f)
                .sound(SoundType.NETHERITE_BLOCK)
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK);
    }

    // --- the Infinite IKEA
    public static final DeferredBlock<Block> STORE_FLOOR = BLOCKS.registerSimpleBlock("store_floor",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.5f, 6.0f).sound(SoundType.STONE));
    public static final DeferredBlock<Block> STORE_WALKWAY = BLOCKS.registerSimpleBlock("store_walkway",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.5f, 6.0f).sound(SoundType.STONE));
    public static final DeferredBlock<Block> STORE_PARTITION = BLOCKS.registerSimpleBlock("store_partition",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.0f, 3.0f).sound(SoundType.WOOD));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Shaped> STORE_LIGHT = BLOCKS.register("store_light",
            () -> new com.rickandmorty.block.StoreBlocks.Shaped(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5f).sound(SoundType.LANTERN).noOcclusion()
                    .lightLevel(state -> 14), true, new double[]{5, 0, 4, 11, 5, 12}));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.ExitSign> EXIT_SIGN = BLOCKS.register("exit_sign",
            () -> new com.rickandmorty.block.StoreBlocks.ExitSign(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.5f).sound(SoundType.METAL).noOcclusion().lightLevel(state -> 6)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Sofa> SOFA = BLOCKS.register("sofa",
            () -> new com.rickandmorty.block.StoreBlocks.Sofa(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.8f).sound(SoundType.WOOL).noOcclusion()));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Chair> STORE_CHAIR = BLOCKS.register("store_chair",
            () -> new com.rickandmorty.block.StoreBlocks.Chair(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.8f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Fixed> DINING_TABLE = BLOCKS.register("dining_table",
            () -> new com.rickandmorty.block.StoreBlocks.Fixed(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.8f).sound(SoundType.WOOD).noOcclusion(),
                    net.minecraft.world.phys.shapes.Shapes.or(Block.box(0, 13, 0, 16, 16, 16), Block.box(1, 0, 1, 3, 13, 3), Block.box(13, 0, 1, 15, 13, 3),
                            Block.box(1, 0, 13, 3, 13, 15), Block.box(13, 0, 13, 15, 13, 15))));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Fixed> COFFEE_TABLE = BLOCKS.register("coffee_table",
            () -> new com.rickandmorty.block.StoreBlocks.Fixed(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.8f).sound(SoundType.WOOD).noOcclusion(), Block.box(1, 0, 1, 15, 7, 15)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Facing> CUBE_SHELF = BLOCKS.register("cube_shelf",
            () -> new com.rickandmorty.block.StoreBlocks.Facing(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.0f).sound(SoundType.WOOD)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Fixed> FLOOR_LAMP = BLOCKS.register("floor_lamp",
            () -> new com.rickandmorty.block.StoreBlocks.Fixed(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.5f).sound(SoundType.LANTERN).noOcclusion().lightLevel(state -> 13),
                    Block.box(4, 0, 4, 12, 16, 12)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Facing> KITCHEN_CABINET = BLOCKS.register("kitchen_cabinet",
            () -> new com.rickandmorty.block.StoreBlocks.Facing(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.0f).sound(SoundType.WOOD)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.StockCrate> STOCK_CRATE = BLOCKS.register("stock_crate",
            () -> new com.rickandmorty.block.StoreBlocks.StockCrate(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.8f).sound(SoundType.WOOD)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.FoodCounter> FOOD_COUNTER = BLOCKS.register("food_counter",
            () -> new com.rickandmorty.block.StoreBlocks.FoodCounter(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Facing> CHECKOUT_COUNTER = BLOCKS.register("checkout_counter",
            () -> new com.rickandmorty.block.StoreBlocks.Facing(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> AISLE_MARKER = BLOCKS.registerSimpleBlock("aisle_marker",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0f).sound(SoundType.METAL));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Fixed> STORE_PALLET = BLOCKS.register("store_pallet",
            () -> new com.rickandmorty.block.StoreBlocks.Fixed(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.8f).sound(SoundType.WOOD).noOcclusion(), Block.box(0, 0, 0, 16, 4, 16)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Fixed> FLAT_PACK_BOX = BLOCKS.register("flat_pack_box",
            () -> new com.rickandmorty.block.StoreBlocks.Fixed(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.6f).sound(SoundType.WOOD),
                    net.minecraft.world.phys.shapes.Shapes.block()));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Fixed> PAPER_LAMP = BLOCKS.register("paper_lamp",
            () -> new com.rickandmorty.block.StoreBlocks.Fixed(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.3f).sound(SoundType.WOOL).noOcclusion().lightLevel(state -> 15),
                    Block.box(3, 2, 3, 13, 12, 13)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Fixed> RACK_UPRIGHT = BLOCKS.register("rack_upright",
            () -> new com.rickandmorty.block.StoreBlocks.Fixed(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion(), Block.box(5, 0, 5, 11, 16, 11)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Fixed> RACK_SHELF = BLOCKS.register("rack_shelf",
            () -> new com.rickandmorty.block.StoreBlocks.Fixed(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion(), Block.box(0, 13, 0, 16, 16, 16)));
    public static final DeferredBlock<com.rickandmorty.block.StoreDoorBlock> STORE_DOOR = BLOCKS.register("store_door",
            () -> new com.rickandmorty.block.StoreDoorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(-1.0f, 3600000.0f).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<Block> STORE_PILLAR = BLOCKS.registerSimpleBlock("store_pillar",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(-1.0f, 3600000.0f).sound(SoundType.STONE));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Shaped> FLOOR_ARROW = BLOCKS.register("floor_arrow",
            () -> new com.rickandmorty.block.StoreBlocks.Shaped(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.2f).sound(SoundType.WOOL).noOcclusion().noCollission(),
                    false, new double[]{0, 0, 0, 16, 1, 16}));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.DepartmentSign> DEPARTMENT_SIGN = BLOCKS.register("department_sign",
            () -> new com.rickandmorty.block.StoreBlocks.DepartmentSign(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5f).sound(SoundType.METAL).noOcclusion().lightLevel(state -> 5)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Fixed> LOADED_PALLET = BLOCKS.register("loaded_pallet",
            () -> new com.rickandmorty.block.StoreBlocks.Fixed(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.8f).sound(SoundType.WOOD), net.minecraft.world.phys.shapes.Shapes.block()));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Fixed> BALL_PIT = BLOCKS.register("ball_pit",
            () -> new com.rickandmorty.block.StoreBlocks.Fixed(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.3f).sound(SoundType.WOOL).noOcclusion().speedFactor(0.5f),
                    Block.box(0, 0, 0, 16, 8, 16)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Shaped> SHARK_PLUSH = BLOCKS.register("shark_plush",
            () -> new com.rickandmorty.block.StoreBlocks.Shaped(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.2f).sound(SoundType.WOOL).noOcclusion(), true,
                    new double[]{5, 0, 1, 11, 6, 15}));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Sittable> TOILET = BLOCKS.register("toilet",
            () -> new com.rickandmorty.block.StoreBlocks.Sittable(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.0f).sound(SoundType.STONE).noOcclusion(), 0.4,
                    new double[]{3, 0, 2, 13, 8, 12}, new double[]{2, 8, 11, 14, 18, 15}));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Shaped> BATHTUB = BLOCKS.register("bathtub",
            () -> new com.rickandmorty.block.StoreBlocks.Shaped(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.0f).sound(SoundType.STONE).noOcclusion(), true,
                    new double[]{0, 0, 0, 16, 10, 16}));

    /** The Infinite IKEA's roof: looks solid, lets the daylight through, and can't be broken. */
    public static final DeferredBlock<net.minecraft.world.level.block.TransparentBlock> STORE_CEILING = BLOCKS.register("store_ceiling",
            () -> new net.minecraft.world.level.block.TransparentBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(-1.0f, 3600000.0f).sound(SoundType.METAL).noOcclusion().isValidSpawn((s, l, p, e) -> false)));
    public static final DeferredBlock<net.minecraft.world.level.block.TransparentBlock> STORE_CEILING_LIGHT = BLOCKS.register("store_ceiling_light",
            () -> new net.minecraft.world.level.block.TransparentBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
                    .strength(-1.0f, 3600000.0f).sound(SoundType.GLASS).noOcclusion().lightLevel(state -> 15).isValidSpawn((s, l, p, e) -> false)));

    // --- the Dead Mall
    public static final DeferredBlock<com.rickandmorty.block.MallBlocks.Tile> MALL_TILE = BLOCKS.register("mall_tile",
            () -> new com.rickandmorty.block.MallBlocks.Tile(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5f, 6.0f).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> MALL_WALL = BLOCKS.registerSimpleBlock("mall_wall",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5f, 6.0f).sound(SoundType.STONE));
    public static final DeferredBlock<Block> MALL_CEILING = BLOCKS.registerSimpleBlock("mall_ceiling",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6f).sound(SoundType.WOOL));
    public static final DeferredBlock<Block> MALL_SKYLIGHT = BLOCKS.registerSimpleBlock("mall_skylight",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.5f).sound(SoundType.GLASS).lightLevel(state -> 15));
    public static final DeferredBlock<com.rickandmorty.block.MallBlocks.Railing> MALL_RAILING = BLOCKS.register("mall_railing",
            () -> new com.rickandmorty.block.MallBlocks.Railing(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.8f).sound(SoundType.GLASS).noOcclusion()));
    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> MALL_ESCALATOR = BLOCKS.register("mall_escalator",
            () -> new net.minecraft.world.level.block.StairBlock(Blocks.IRON_BLOCK.defaultBlockState(),
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Sittable> MALL_BENCH = BLOCKS.register("mall_bench",
            () -> new com.rickandmorty.block.StoreBlocks.Sittable(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.WOOD).noOcclusion(), 0.4,
                    new double[]{0, 0, 3, 16, 7, 13}));
    public static final DeferredBlock<com.rickandmorty.block.StoreBlocks.Fixed> MALL_TRASH_CAN = BLOCKS.register("mall_trash_can",
            () -> new com.rickandmorty.block.StoreBlocks.Fixed(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.0f).sound(SoundType.METAL).noOcclusion(),
                    Block.box(4, 0, 4, 12, 14, 12)));
    public static final DeferredBlock<com.rickandmorty.block.MallBlocks.GumballMachine> GUMBALL_MACHINE = BLOCKS.register("gumball_machine",
            () -> new com.rickandmorty.block.MallBlocks.GumballMachine(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0f).sound(SoundType.GLASS).noOcclusion()));

    /** A plain white door standing on its own in the Endless Field. Like an exit door, it opens somewhere else. */
    public static final DeferredBlock<com.rickandmorty.block.ExitDoorBlock> LONE_DOOR = BLOCKS.register("lone_door",
            () -> new com.rickandmorty.block.ExitDoorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(-1.0f, 3600000.0f)
                    .sound(SoundType.WOOD).noOcclusion()));

    // --- Rick's gadgets
    public static final DeferredBlock<com.rickandmorty.gadget.ShrinkRayBlock> SHRINK_RAY = BLOCKS.register("shrink_ray",
            () -> new com.rickandmorty.gadget.ShrinkRayBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(2.5f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> 6)));
    public static final DeferredBlock<com.rickandmorty.gadget.TimeCrystalBlock> TIME_CRYSTAL = BLOCKS.register("time_crystal",
            () -> new com.rickandmorty.gadget.TimeCrystalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f).sound(SoundType.AMETHYST).noOcclusion()
                    .lightLevel(state -> 10)));
}
