package com.rickandmorty.worldgen.site;

import com.rickandmorty.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The Council chamber: a white domed rotunda, like a tiny Citadel. A round table with a glowing
 * green core in the middle, a ring of thrones, and alternate versions of this world's players
 * standing in front of them.
 */
final class CouncilSite {
    private static final ResourceKey<LootTable> VAULT_LOOT = SiteBuilder.loot("council_vault");
    private static final int RADIUS = 11;
    private static final int THRONES = 7;
    private static final int MEMBERS = 5;

    private CouncilSite() {
    }

    static void build(SiteBuilder b) {
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState quartz = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        b.foundation(-12, -12, 12, 12, null, Blocks.STONE.defaultBlockState(), 16);
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > RADIUS + 0.5) continue;
                // floor: quartz with a blue ring, a glowing ring of lights further out
                BlockState floor = Math.abs(d - 7.0) < 0.5 ? Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()
                        : Math.abs(d - 9.0) < 0.5 && Math.floorMod(x * 7 + z * 3, 5) == 0 ? Blocks.SEA_LANTERN.defaultBlockState() : quartz;
                if (d < 1.6) floor = Blocks.SEA_LANTERN.defaultBlockState();
                b.set(x, 0, z, floor);
                // walls with tall windows, and the entrance to the south
                if (d > RADIUS - 0.5) {
                    for (int y = 1; y <= 8; y++) {
                        boolean entrance = z > 0 && Math.abs(x) <= 1 && y <= 4;
                        double angle = Math.atan2(z, x);
                        boolean window = y >= 3 && y <= 6 && Math.floorMod((int) Math.round(angle / (Math.PI * 2) * 24), 3) == 0;
                        b.set(x, y, z, entrance ? air : window ? Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState() : white);
                    }
                }
            }
        }
        // the dome: rings closing in above the walls
        for (int y = 9; y <= 15; y++) {
            double ring = RADIUS * Math.cos((y - 8) / 8.0 * Math.PI / 2);
            for (int x = -12; x <= 12; x++) {
                for (int z = -12; z <= 12; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d <= ring + 0.5 && d > ring - 1.2) {
                        b.set(x, y, z, Math.floorMod(x + z, 4) == 0 ? Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState() : white);
                    } else if (d <= ring - 1.2 && y == 15) {
                        b.set(x, y, z, white);
                    }
                }
            }
        }
        // pillars at the entrance
        for (int y = 1; y <= 5; y++) {
            b.set(-2, y, RADIUS, Blocks.QUARTZ_PILLAR.defaultBlockState());
            b.set(2, y, RADIUS, Blocks.QUARTZ_PILLAR.defaultBlockState());
        }
        // round table around a glowing green core
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d >= 2.5 && d <= 4.0) b.slab(x, 1, z, Blocks.SMOOTH_QUARTZ_SLAB, true);
                else if (d < 1.6) b.set(x, 1, z, Blocks.LIME_STAINED_GLASS);
            }
        }
        b.set(0, 2, 0, Blocks.LIME_STAINED_GLASS);
        // thrones facing the table, one of them (north) grander than the rest
        for (int i = 0; i < THRONES; i++) {
            double angle = -Math.PI / 2 + i * Math.PI * 2 / (THRONES + 1);
            int x = (int) Math.round(Math.cos(angle) * 6.5), z = (int) Math.round(Math.sin(angle) * 6.5);
            int bx = (int) Math.round(Math.cos(angle) * 7.6), bz = (int) Math.round(Math.sin(angle) * 7.6);
            Direction back = Direction.fromYRot(Math.toDegrees(angle) - 90.0);
            b.stairs(x, 1, z, Blocks.QUARTZ_STAIRS, back, false);
            int height = i == 0 ? 5 : 3;
            for (int y = 1; y <= height; y++) b.set(bx, y, bz, Blocks.QUARTZ_PILLAR.defaultBlockState());
            if (i == 0) b.set(bx, height + 1, bz, Blocks.SEA_LANTERN);
        }
        b.chest(0, 1, -10, Direction.SOUTH, VAULT_LOOT);
        // the Council itself
        BlockPos home = b.at(0, 1, 0);
        for (int i = 0; i < MEMBERS; i++) {
            double angle = -Math.PI / 2 + (i * 3 % THRONES) * Math.PI * 2 / (THRONES + 1);
            double x = Mth.cos((float) angle) * 5.3 + 0.5, z = Mth.sin((float) angle) * 5.3 + 0.5;
            b.spawn(ModEntities.COUNCIL_MEMBER.get(), x, 1, z, member -> member.setHome(home, 8));
        }
    }
}
