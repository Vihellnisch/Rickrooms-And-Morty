package com.rickandmorty.worldgen.site;

import com.rickandmorty.entity.CronenbergKind;
import com.rickandmorty.entity.SurvivorKind;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/** The buildings of Cronenberg World. All coordinates are relative to the site origin (y = 0 is the floor). */
final class CronenbergSites {
    private static final ResourceKey<LootTable> HOUSE_LOOT = SiteBuilder.loot("cronenberg_house");
    private static final ResourceKey<LootTable> CAMP_LOOT = SiteBuilder.loot("survivor_camp");
    private static final ResourceKey<LootTable> SCHOOL_LOOT = SiteBuilder.loot("high_school");
    private static final ResourceKey<LootTable> PIT_LOOT = SiteBuilder.loot("flesh_pit");

    private CronenbergSites() {
    }

    // ------------------------------------------------------------------ shared bits

    /** A random piece of Cronenberg World's meat. Always makes the same two rolls. */
    static BlockState flesh(SiteBuilder b) {
        float r = b.roll();
        return (r < 0.1f ? ModBlocks.WATCHING_FLESH : r < 0.45f ? ModBlocks.TUMOR : r < 0.8f ? ModBlocks.FLESH : ModBlocks.VEINED_FLESH).get().defaultBlockState();
    }

    /** {@code normal}, or (with the given chance) flesh that has grown over it. */
    static BlockState overgrown(SiteBuilder b, BlockState normal, double chance) {
        float r = b.roll();
        BlockState meat = flesh(b);
        return r < chance ? meat : normal;
    }

    /** How strongly the flesh has taken over at (x, z), given where it got in. */
    static double infestation(int x, int y, int z, int cx, int cy, int cz, double radius) {
        double d = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy) * 0.5 + (z - cz) * (z - cz));
        return Math.max(0.0, 1.0 - d / radius) * 0.95;
    }

    static Block pick(SiteBuilder b, Block... blocks) {
        return blocks[b.nextInt(blocks.length)];
    }

    /** Dried blood on the floor: red carpet stains and a smeared trail. */
    static void blood(SiteBuilder b, int x0, int z0, int x1, int z1, int y, int stains) {
        for (int i = 0; i < stains; i++) {
            int x = x0 + b.nextInt(x1 - x0 + 1), z = z0 + b.nextInt(z1 - z0 + 1);
            boolean trail = b.roll() < 0.4f;
            int length = 2 + b.nextInt(4);
            Direction dir = Direction.from2DDataValue(b.nextInt(4));
            for (int s = 0; s < (trail ? length : 1); s++) {
                int px = x + dir.getStepX() * s, pz = z + dir.getStepZ() * s;
                if (px < x0 || px > x1 || pz < z0 || pz > z1) break;
                if (b.get(px, y, pz).isAir()) b.set(px, y, pz, trail ? Blocks.REDSTONE_WIRE : Blocks.RED_CARPET);
            }
        }
    }

    // ------------------------------------------------------------------ ruined house

    /** A 9x11 suburban house with a gable roof, half eaten by flesh growing in from one corner. */
    static void ruinedHouse(SiteBuilder b) {
        Block wall = pick(b, Blocks.WHITE_TERRACOTTA, Blocks.BIRCH_PLANKS, Blocks.BRICKS, Blocks.SMOOTH_SANDSTONE, Blocks.LIGHT_GRAY_TERRACOTTA);
        Block[] roofs = {Blocks.DARK_OAK_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.BRICK_STAIRS};
        Block[] roofFull = {Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BRICKS};
        int roofIndex = b.nextInt(roofs.length);
        int cx = b.roll() < 0.5f ? -4 : 4, cz = b.roll() < 0.5f ? -5 : 5;
        double radius = 5.0 + b.roll() * 3.0;

        b.foundation(-5, -6, 5, 6, null, Blocks.COBBLESTONE.defaultBlockState(), 10);
        for (int x = -4; x <= 4; x++) {
            for (int z = -5; z <= 5; z++) {
                b.set(x, 0, z, overgrown(b, Blocks.OAK_PLANKS.defaultBlockState(), infestation(x, 0, z, cx, 0, cz, radius)));
            }
        }
        // walls, with windows (mostly smashed) and a doorway with no door
        for (int y = 1; y <= 4; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -5; z <= 5; z++) {
                    boolean edge = Math.abs(x) == 4 || Math.abs(z) == 5;
                    if (!edge) continue;
                    float crumble = b.roll();
                    boolean window = (y == 2 || y == 3) && ((Math.abs(z) == 5 && Math.abs(x) == 2) || (Math.abs(x) == 4 && (z == -3 || z == 2)));
                    boolean door = z == 5 && x == 0 && y <= 2;
                    BlockState state = overgrown(b, wall.defaultBlockState(), infestation(x, y, z, cx, 0, cz, radius));
                    if (door) continue;
                    if (window) state = crumble < 0.35f ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    else if (y == 4 && crumble < 0.2f) state = Blocks.AIR.defaultBlockState();
                    b.set(x, y, z, state);
                }
            }
        }
        // a wall between the bedroom and the living room
        for (int y = 1; y <= 4; y++) {
            for (int x = -3; x <= 3; x++) {
                if (x == 0 && y <= 2) continue;
                b.set(x, y, -1, overgrown(b, wall.defaultBlockState(), infestation(x, y, -1, cx, 0, cz, radius)));
            }
        }
        // gable roof, ridge running north-south, with holes where it fell in
        for (int k = 0; k <= 5; k++) {
            int y = 5 + k;
            for (int z = -6; z <= 6; z++) {
                float hole = b.roll();
                double rot = infestation(-5 + k, y - 4, z, cx, 0, cz, radius);
                boolean missing = hole < 0.12f + rot * 0.5f;
                if (k == 5) {
                    if (!missing) b.set(0, y, z, roofFull[roofIndex]);
                    continue;
                }
                if (!missing) b.stairs(-5 + k, y, z, roofs[roofIndex], Direction.EAST, false);
                if (b.roll() >= 0.12f + infestation(5 - k, y - 4, z, cx, 0, cz, radius) * 0.5f) {
                    b.stairs(5 - k, y, z, roofs[roofIndex], Direction.WEST, false);
                }
                if (Math.abs(z) == 5) {                         // close the gable ends
                    for (int x = -4 + k; x <= 4 - k; x++) b.set(x, y, z, overgrown(b, wall.defaultBlockState(), rot));
                }
            }
        }
        // what's left of the furniture
        b.bed(-3, 1, -3, pick(b, Blocks.RED_BED, Blocks.BLUE_BED, Blocks.WHITE_BED), Direction.NORTH);
        b.chest(3, 1, -4, Direction.SOUTH, HOUSE_LOOT);
        b.set(2, 1, 2, Blocks.OAK_FENCE);
        b.set(2, 2, 2, Blocks.OAK_PRESSURE_PLATE);
        b.stairs(-3, 1, 1, Blocks.RED_NETHER_BRICK_STAIRS, Direction.WEST, false);
        b.stairs(-3, 1, 2, Blocks.RED_NETHER_BRICK_STAIRS, Direction.WEST, false);
        b.set(3, 1, 1, Blocks.DARK_OAK_PLANKS);
        b.set(3, 2, 1, Blocks.BLACK_CONCRETE);                   // the TV
        b.set(3, 1, -2, Blocks.WHITE_CONCRETE);                  // the fridge
        b.set(3, 2, -2, Blocks.WHITE_CONCRETE);
        blood(b, -3, -4, 3, 4, 1, 6);
        for (int i = 0; i < 5; i++) {
            int x = cx + (cx > 0 ? -1 : 1) * b.nextInt(4), z = cz + (cz > 0 ? -1 : 1) * b.nextInt(4);
            Block growth = b.roll() < 0.5f ? ModBlocks.PUSTULE.get() : ModBlocks.EYESTALK.get();
            if (b.get(x, 1, z).isAir() && Math.abs(x) < 4 && Math.abs(z) < 5) b.set(x, 1, z, growth);
        }
        if (b.roll() < 0.45f) {
            CronenbergKind kind = b.roll() < 0.5f ? CronenbergKind.CRAWLER : CronenbergKind.STALKER;
            b.spawn(ModEntities.CRONENBERGS.get(kind).get(), 0.5, 1, 2.5, mob -> {
            });
        }
    }

    // ------------------------------------------------------------------ survivor camp

    /**
     * The Smith house, fortified: a wall of scrap with teeth planted outside it, a watchtower,
     * the garage, a campfire and a little farm. Jerry, Beth and Summer live here.
     */
    static void survivorCamp(SiteBuilder b) {
        b.foundation(-14, -14, 14, 14, null, Blocks.DIRT.defaultBlockState(), 16);
        for (int x = -13; x <= 13; x++) {
            for (int z = -13; z <= 13; z++) {
                float r = b.roll();
                int ring = Math.max(Math.abs(x), Math.abs(z));
                BlockState ground = ring >= 13 ? ModBlocks.ROTTING_FLESH.get().defaultBlockState()
                        : (r < 0.55f ? Blocks.COARSE_DIRT : r < 0.8f ? Blocks.GRAVEL : Blocks.PACKED_MUD).defaultBlockState();
                b.set(x, 0, z, ground);
            }
        }
        // the wall
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != 12) continue;
                boolean gate = z == 12 && Math.abs(x) <= 1;
                for (int y = 1; y <= 4; y++) {
                    float r = b.roll();
                    if (gate) {
                        if (y == 1) b.set(x, y, z, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.SOUTH));
                        continue;
                    }
                    BlockState scrap = (r < 0.35f ? Blocks.COBBLESTONE : r < 0.55f ? Blocks.MOSSY_COBBLESTONE : r < 0.8f ? Blocks.SPRUCE_PLANKS
                            : r < 0.9f ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.IRON_BLOCK).defaultBlockState();
                    if (y == 4) scrap = r < 0.55f ? Blocks.SPRUCE_FENCE.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    b.set(x, y, z, scrap);
                }
            }
        }
        // teeth planted all around the outside
        for (int x = -14; x <= 14; x++) {
            for (int z = -14; z <= 14; z++) {
                int ring = Math.max(Math.abs(x), Math.abs(z));
                if (ring < 13) continue;
                if (b.roll() < 0.45f && !(z > 12 && Math.abs(x) <= 2)) b.set(x, 1, z, ModBlocks.TOOTH_SPIKE.get());
            }
        }
        // watchtower
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        for (int y = 1; y <= 7; y++) {
            for (int[] c : new int[][]{{-10, -10}, {-8, -10}, {-10, -8}, {-8, -8}}) b.set(c[0], y, c[1], log);
            if (y <= 6) b.set(-9, y, -8, log);
            b.set(-9, y, -7, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        b.fill(-10, 7, -10, -8, 7, -8, Blocks.SPRUCE_PLANKS);
        for (int x = -10; x <= -8; x++) {
            for (int z = -10; z <= -8; z++) {
                if ((x == -10 || x == -8 || z == -10 || z == -8) && !(x == -9 && z == -8)) b.set(x, 8, z, Blocks.SPRUCE_FENCE);
            }
        }
        b.set(-9, 8, -9, Blocks.LANTERN);

        smithHouse(b);
        garage(b);

        // campfire with log seats, and the farm
        b.set(-6, 1, 7, Blocks.CAMPFIRE);
        BlockState seatX = Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState seatZ = Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        b.set(-8, 1, 7, seatZ);
        b.set(-4, 1, 7, seatZ);
        b.set(-7, 1, 9, seatX);
        b.set(-6, 1, 9, seatX);
        for (int x = 4; x <= 8; x++) {
            for (int z = 6; z <= 9; z++) {
                if (x == 6 && (z == 7 || z == 8)) {
                    b.set(x, 0, z, Blocks.WATER);
                    continue;
                }
                b.set(x, 0, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                Block crop = pick(b, Blocks.POTATOES, Blocks.CARROTS, Blocks.WHEAT, Blocks.BEETROOTS);
                int age = 2 + b.nextInt(6);
                BlockState plant = crop.defaultBlockState();
                if (crop instanceof CropBlock cropBlock) plant = cropBlock.getStateForAge(Math.min(age, cropBlock.getMaxAge()));
                b.set(x, 1, z, plant);
            }
        }
        for (int[] c : new int[][]{{-11, 11}, {11, 11}, {11, -11}}) {
            b.set(c[0], 1, c[1], Blocks.SPRUCE_FENCE);
            b.set(c[0], 2, c[1], Blocks.LANTERN);
        }
        b.barrel(-11, 1, 0, Direction.EAST, CAMP_LOOT);
        b.barrel(-11, 1, 1, Direction.EAST, null);
        b.barrel(10, 1, 3, Direction.WEST, null);

        BlockPos home = b.at(0, 1, 0);
        b.spawn(ModEntities.SURVIVORS.get(SurvivorKind.JERRY).get(), -6.5, 1, 5.5, s -> s.setHome(home, 11));
        b.spawn(ModEntities.SURVIVORS.get(SurvivorKind.BETH).get(), 0.5, 1, -3.5, s -> s.setHome(home, 11));
        b.spawn(ModEntities.SURVIVORS.get(SurvivorKind.SUMMER).get(), 5.5, 1, 4.5, s -> s.setHome(home, 11));
    }

    /** Two storeys, pale walls, boarded windows, a porch. Its 1st floor spans x -5..5, z -8..2. */
    private static void smithHouse(SiteBuilder b) {
        BlockState wall = Blocks.WHITE_TERRACOTTA.defaultBlockState();
        b.fill(-5, 0, -8, 5, 0, 2, Blocks.OAK_PLANKS);
        for (int y = 1; y <= 8; y++) {
            for (int x = -5; x <= 5; x++) {
                for (int z = -8; z <= 2; z++) {
                    if (Math.abs(x) != 5 && z != -8 && z != 2) continue;
                    boolean windowRow = y == 2 || y == 3 || y == 6 || y == 7;
                    boolean windowCol = (Math.abs(x) == 5 && (z == -6 || z == -2)) || ((z == -8 || z == 2) && Math.abs(x) == 3);
                    boolean door = z == 2 && x == 0 && y <= 2;
                    float r = b.roll();
                    if (door) continue;
                    if (windowRow && windowCol) b.set(x, y, z, r < 0.55f ? Blocks.SPRUCE_PLANKS : Blocks.GLASS);   // boarded up
                    else b.set(x, y, z, y == 1 ? Blocks.STONE_BRICKS.defaultBlockState() : wall);
                }
            }
        }
        // upstairs floor, with a hole for the ladder
        b.fill(-4, 5, -7, 4, 5, 1, Blocks.SPRUCE_PLANKS);
        for (int y = 1; y <= 5; y++) b.set(4, y, -7, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        // gable roof (ridge east-west)
        for (int k = 0; k <= 6; k++) {
            int y = 9 + k;
            for (int x = -6; x <= 6; x++) {
                if (k == 6) {
                    b.set(x, y, -3, Blocks.DARK_OAK_PLANKS);
                    continue;
                }
                b.stairs(x, y, -9 + k, Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false);
                b.stairs(x, y, 3 - k, Blocks.DARK_OAK_STAIRS, Direction.NORTH, false);
            }
            for (int z = -8 + k; z <= 2 - k; z++) {
                if (k < 6) {
                    b.set(-5, y, z, wall);
                    b.set(5, y, z, wall);
                }
            }
        }
        // porch
        b.fill(-2, 0, 3, 2, 0, 4, Blocks.SPRUCE_PLANKS);
        for (int y = 1; y <= 3; y++) {
            b.set(-2, y, 4, Blocks.SPRUCE_FENCE);
            b.set(2, y, 4, Blocks.SPRUCE_FENCE);
        }
        for (int x = -2; x <= 2; x++) {
            b.slab(x, 4, 3, Blocks.SPRUCE_SLAB, false);
            b.slab(x, 4, 4, Blocks.SPRUCE_SLAB, false);
        }
        // downstairs: kitchen, dining table, couch
        b.set(3, 1, -7, Blocks.SMOKER.defaultBlockState().setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING, Direction.SOUTH));
        b.set(2, 1, -7, Blocks.FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING, Direction.SOUTH));
        b.barrel(1, 1, -7, Direction.SOUTH, CAMP_LOOT);
        b.set(0, 1, -7, Blocks.CRAFTING_TABLE);
        for (int x = -3; x <= -1; x++) {
            b.set(x, 1, -5, Blocks.SPRUCE_FENCE);
            b.set(x, 2, -5, Blocks.SPRUCE_PRESSURE_PLATE);
            b.stairs(x, 1, -4, Blocks.SPRUCE_STAIRS, Direction.SOUTH, false);
            b.stairs(x, 1, -6, Blocks.SPRUCE_STAIRS, Direction.NORTH, false);
        }
        b.stairs(-4, 1, -1, Blocks.RED_NETHER_BRICK_STAIRS, Direction.WEST, false);
        b.stairs(-4, 1, 0, Blocks.RED_NETHER_BRICK_STAIRS, Direction.WEST, false);
        b.fill(-2, 1, -1, 0, 1, 0, Blocks.GREEN_CARPET);
        b.set(0, 4, -3, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // upstairs: beds and a chest
        b.bed(-4, 6, -6, Blocks.RED_BED, Direction.NORTH);
        b.bed(-2, 6, -6, Blocks.RED_BED, Direction.NORTH);
        b.bed(1, 6, -6, Blocks.PINK_BED, Direction.NORTH);
        b.chest(-4, 6, 0, Direction.EAST, CAMP_LOOT);
        b.set(0, 6, 1, Blocks.LANTERN);
    }

    /** Rick's old garage: workbenches, an anvil, a chest. */
    private static void garage(SiteBuilder b) {
        b.fill(6, 0, -8, 11, 0, -2, Blocks.SMOOTH_STONE);
        for (int y = 1; y <= 5; y++) {
            for (int x = 6; x <= 11; x++) {
                for (int z = -8; z <= -2; z++) {
                    if (x != 6 && x != 11 && z != -8 && z != -2) continue;
                    if (z == -2 && x >= 7 && x <= 10 && y <= 3) continue;   // the garage door is long gone
                    b.set(x, y, z, b.roll() < 0.8f ? Blocks.STONE_BRICKS : Blocks.CRACKED_STONE_BRICKS);
                }
            }
        }
        b.fill(6, 6, -8, 11, 6, -2, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        b.set(7, 1, -7, Blocks.ANVIL);
        b.set(8, 1, -7, Blocks.SMITHING_TABLE);
        b.set(9, 1, -7, Blocks.BREWING_STAND);
        b.chest(10, 1, -7, Direction.SOUTH, CAMP_LOOT);
        b.set(10, 1, -5, Blocks.REDSTONE_LAMP);
        b.set(8, 5, -5, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    // ------------------------------------------------------------------ high school

    /**
     * Harry Herpson High: brick walls, a corridor of lockers between two rows of classrooms, a
     * gym, the principal's and the nurse's offices. The Mantis People nest in it now.
     */
    static void highSchool(SiteBuilder b) {
        int[][] infestations = new int[3][];
        for (int i = 0; i < 3; i++) infestations[i] = new int[]{-18 + b.nextInt(37), -12 + b.nextInt(25)};
        int collapseX = -14 + b.nextInt(29), collapseZ = -10 + b.nextInt(21);

        b.foundation(-21, -15, 21, 15, null, Blocks.COBBLESTONE.defaultBlockState(), 12);
        for (int x = -20; x <= 20; x++) {
            for (int z = -14; z <= 14; z++) {
                boolean corridor = Math.abs(z) <= 2 && x > -8;
                BlockState floor = x <= -8 ? Blocks.BIRCH_PLANKS.defaultBlockState()
                        : corridor ? ((x + z) % 2 == 0 ? Blocks.POLISHED_ANDESITE : Blocks.POLISHED_DIORITE).defaultBlockState()
                        : Blocks.OAK_PLANKS.defaultBlockState();
                b.set(x, 0, z, overgrown(b, floor, rot(infestations, x, z)));
            }
        }
        // outer walls with windows, roof with a collapsed patch
        for (int y = 1; y <= 10; y++) {
            for (int x = -20; x <= 20; x++) {
                for (int z = -14; z <= 14; z++) {
                    boolean edge = Math.abs(x) == 20 || Math.abs(z) == 14;
                    if (y == 10) {
                        float r = b.roll();
                        double dc = Math.hypot(x - collapseX, z - collapseZ);
                        if (dc < 4.5 || r < 0.06f) continue;
                        b.set(x, y, z, edge ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.STONE_BRICKS);
                        continue;
                    }
                    if (!edge) continue;
                    boolean door = z == 14 && Math.abs(x) <= 1 && y <= 3;
                    boolean window = y >= 3 && y <= 6 && (Math.abs(z) == 14 ? Math.floorMod(x, 4) == 2 : Math.floorMod(z, 4) == 2);
                    float r = b.roll();
                    BlockState wall = overgrown(b, (y == 9 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.BRICKS).defaultBlockState(), rot(infestations, x, z) * 0.8);
                    if (door) continue;
                    if (window) b.set(x, y, z, r < 0.4f ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState());
                    else b.set(x, y, z, wall);
                }
            }
        }
        // rubble under the hole in the roof
        for (int i = 0; i < 14; i++) {
            int x = collapseX + b.nextInt(7) - 3, z = collapseZ + b.nextInt(7) - 3;
            if (Math.abs(x) < 20 && Math.abs(z) < 14) b.set(x, 1, z, b.roll() < 0.5f ? Blocks.GRAVEL : Blocks.COBBLESTONE);
        }
        BlockState inner = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        // gym wall, open to the corridor
        for (int y = 1; y <= 9; y++) {
            for (int z = -13; z <= 13; z++) {
                if (Math.abs(z) <= 1 && y <= 3) continue;
                b.set(-8, y, z, inner);
            }
        }
        // corridor walls with lockers, and classroom dividers
        int[] dividers = {-1, 6, 13};
        int[] doors = {-5, 2, 9, 16};
        for (int y = 1; y <= 9; y++) {
            for (int x = -7; x <= 19; x++) {
                for (int side = -1; side <= 1; side += 2) {
                    int z = side * 3;
                    boolean doorway = false;
                    for (int d : doors) doorway |= (x == d || x == d + 1) && y <= 3;
                    if (doorway) continue;
                    float r = b.roll();
                    if (y <= 2 && Math.floorMod(x, 2) == 0) {
                        b.barrel(x, y, z, side < 0 ? Direction.SOUTH : Direction.NORTH, r < 0.08f ? SCHOOL_LOOT : null);
                    } else {
                        b.set(x, y, z, overgrown(b, inner, rot(infestations, x, z) * 0.6));
                    }
                }
            }
            for (int d : dividers) {
                for (int z = -13; z <= 13; z++) {
                    if (Math.abs(z) <= 3) continue;
                    b.set(d, y, z, inner);
                }
            }
        }
        // classrooms: desks and chairs facing a chalkboard (with something scrawled on it in red)
        int[][] rooms = {{-7, -2}, {0, 5}, {7, 12}, {14, 19}};
        for (int side = -1; side <= 1; side += 2) {
            for (int room = 0; room < rooms.length; room++) {
                int x0 = rooms[room][0], x1 = rooms[room][1];
                int far = side * 13;
                boolean principal = side < 0 && room == 3, nurse = side > 0 && room == 0;
                for (int x = x0; x <= x1; x++) {
                    for (int y = 2; y <= 3; y++) {
                        b.set(x, y, far, b.roll() < 0.12f ? Blocks.RED_CONCRETE : (principal ? Blocks.BOOKSHELF : Blocks.BLACK_CONCRETE));
                    }
                }
                if (principal) {
                    b.set(x0 + 2, 1, far - side * 3, Blocks.DARK_OAK_PLANKS);
                    b.set(x0 + 3, 1, far - side * 3, Blocks.DARK_OAK_PLANKS);
                    b.chest(x0 + 1, 1, far - side, side < 0 ? Direction.SOUTH : Direction.NORTH, SCHOOL_LOOT);
                    continue;
                }
                if (nurse) {
                    b.fill(x0, 0, 4, x1, 0, 12, Blocks.WHITE_CONCRETE);
                    b.bed(x0, 1, 11, Blocks.WHITE_BED, Direction.SOUTH);
                    b.bed(x0 + 3, 1, 11, Blocks.WHITE_BED, Direction.SOUTH);
                    b.set(x1, 1, 12, Blocks.BREWING_STAND);
                    b.chest(x1, 1, 9, Direction.WEST, SCHOOL_LOOT);
                    continue;
                }
                for (int x = x0; x <= x1; x += 2) {
                    for (int row = 0; row < 4; row++) {
                        int z = far - side * (3 + row * 2);
                        if (b.roll() < 0.25f) continue;              // knocked over
                        b.slab(x, 1, z, Blocks.OAK_SLAB, true);
                        b.stairs(x, 1, z - side, Blocks.OAK_STAIRS, side < 0 ? Direction.SOUTH : Direction.NORTH, false);
                    }
                }
            }
        }
        // the gym: bleachers, cobwebs and a nest
        for (int x = -19; x <= -9; x++) {
            for (int row = 0; row < 3; row++) {
                int z = -13 + row;
                for (int y = 1; y < 3 - row; y++) b.set(x, y, z, Blocks.SPRUCE_PLANKS);
                b.stairs(x, 3 - row, z, Blocks.SPRUCE_STAIRS, Direction.NORTH, false);
            }
        }
        for (int i = 0; i < 30; i++) {
            int x = -19 + b.nextInt(11), z = -9 + b.nextInt(22), y = b.roll() < 0.5f ? 1 : 7 + b.nextInt(3);
            if (b.get(x, y, z).isAir()) b.set(x, y, z, i % 3 == 0 ? Blocks.COBWEB : i % 3 == 1 ? Blocks.WHITE_WOOL : ModBlocks.TUMOR.get());
        }
        b.chest(-9, 1, 12, Direction.WEST, SCHOOL_LOOT);
        blood(b, -7, -2, 19, 2, 1, 14);
        blood(b, -19, -9, -9, 13, 1, 8);
        for (int i = 0; i < 6; i++) {
            int x = -12 + b.nextInt(30);
            b.set(x, 9, 0, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        b.sign(0, 5, 15, Direction.SOUTH, "Harry Herpson", "High School", "", "Go Hornets!");
        // they're still in here
        double[][] mantises = {{-14.5, 0.5}, {-4.5, -8.5}, {9.5, 8.5}, {3.5, 0.5}, {16.5, -8.5}, {-12.5, 8.5}};
        for (double[] m : mantises) b.spawn(ModEntities.MANTIS.get(), m[0], 1, m[1], mob -> {
        });
    }

    private static double rot(int[][] infestations, int x, int z) {
        double best = 0;
        for (int[] c : infestations) best = Math.max(best, infestation(x, 0, z, c[0], 0, c[1], 4.5));
        return best;
    }

    // ------------------------------------------------------------------ flesh pit

    /**
     * A crater of living meat, a pool of blood at the bottom and the ribs of something huge
     * arching over it. The Behemoth sleeps in the pool.
     */
    static void fleshPit(SiteBuilder b) {
        final double radius = 14.0;
        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 16; z++) {
                double d = Math.sqrt(x * x + z * z);
                float jitter = b.roll();
                if (d <= radius) {
                    int bottom = -(int) Math.round(10.0 * (1.0 - (d / radius) * (d / radius)) + jitter);
                    boolean pool = d <= 3.5;
                    for (int y = bottom - 3; y <= bottom; y++) {
                        BlockState lining = flesh(b);
                        if (b.roll() < 0.2f) lining = ModBlocks.NECROTIC_FLESH.get().defaultBlockState();
                        if (pool && y >= bottom - 1) lining = Blocks.WATER.defaultBlockState();
                        b.set(x, y, z, lining);
                    }
                    for (int y = bottom + 1; y <= 14; y++) b.set(x, y, z, Blocks.AIR.defaultBlockState());
                } else if (d <= radius + 2.0) {
                    int lip = 1 + (int) (jitter * 2.5f);
                    for (int y = -1; y <= lip; y++) b.set(x, y, z, flesh(b));
                    for (int y = lip + 1; y <= 12; y++) b.set(x, y, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
        // the ribs of something enormous, arching in over the pit
        float spin = b.roll() * (float) Math.PI * 2;
        for (int rib = 0; rib < 5; rib++) {
            double angle = spin + rib * Math.PI * 2 / 5 + b.roll() * 0.3;
            for (int s = 0; s <= 24; s++) {
                double t = s / 24.0;
                double dist = 15.0 - t * 11.0;
                int y = (int) Math.round(Math.sin(t * Math.PI * 0.85) * 10.0) - 2;
                int x = (int) Math.round(Math.cos(angle) * dist), z = (int) Math.round(Math.sin(angle) * dist);
                b.set(x, y, z, ModBlocks.CALCIFIED_BONE.get());
            }
        }
        // bones, teeth and pustules down the slopes
        for (int i = 0; i < 40; i++) {
            double angle = b.roll() * Math.PI * 2, dist = 4.0 + b.roll() * 9.0;
            int x = (int) Math.round(Math.cos(angle) * dist), z = (int) Math.round(Math.sin(angle) * dist);
            int surface = -(int) Math.round(10.0 * (1.0 - (dist / radius) * (dist / radius))) + 1;
            float what = b.roll();
            Block block = what < 0.4f ? ModBlocks.CALCIFIED_BONE.get() : what < 0.6f ? ModBlocks.TOOTH_SPIKE.get()
                    : what < 0.85f ? ModBlocks.PUSTULE.get() : ModBlocks.EYESTALK.get();
            if (b.get(x, surface, z).isAir()) b.set(x, surface, z, block);
            if (block == ModBlocks.CALCIFIED_BONE.get() && b.roll() < 0.5f) b.set(x, surface + 1, z, block);
        }
        // two chests half-buried in bone
        for (int i = 0; i < 2; i++) {
            double angle = b.roll() * Math.PI * 2;
            int x = (int) Math.round(Math.cos(angle) * 7.5), z = (int) Math.round(Math.sin(angle) * 7.5);
            int surface = -(int) Math.round(10.0 * (1.0 - (7.5 / radius) * (7.5 / radius))) + 1;
            b.chest(x, surface, z, Direction.fromYRot(Math.toDegrees(angle) + 90.0), PIT_LOOT);
            b.set(x + 1, surface, z, ModBlocks.CALCIFIED_BONE.get());
            b.set(x, surface, z + 1, ModBlocks.CALCIFIED_BONE.get());
        }
        b.spawn(ModEntities.BEHEMOTH.get(), 0.5, -11, 0.5, mob -> {
        });
    }
}
