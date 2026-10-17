package com.rickandmorty.block;

import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Layout and geometry of the 3x3 Warp Gateway.
 * <p>
 * The controller ({@link WarpGatewayBlock}) sits at the bottom middle. Seven invisible
 * {@link GatewayFrameBlock}s fill the rest of the ring so it has proper collision.
 * All geometry is written for a gateway facing north, in "local" block units relative to
 * the controller block's corner; {@link #toWorld} rotates it for the real facing.
 * <p>
 * The ring numbers must match tools/generate_gateway_model.py.
 */
public final class GatewayStructure {
    /** Frame parts as {sideways offset (towards facing.getClockWise()), height}. */
    public static final int[][] PARTS = {{-1, 0}, {1, 0}, {-1, 1}, {1, 1}, {-1, 2}, {1, 2}, {0, 2}};
    /** The frame part that renders the top half of the ring. */
    public static final int TOP_PART = 6;

    public static final double RING_X = 0.5;
    public static final double RING_Y = 21.6 / 16;
    public static final double RING_INNER = 18.5 / 16;
    public static final double RING_OUTER = 23.5 / 16;
    public static final double PLATFORM_TOP = 4.0 / 16;

    private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> CONTROLLER_SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape[] parts = new VoxelShape[PARTS.length];
            for (int i = 0; i < PARTS.length; i++) {
                parts[i] = buildShape(facing, PARTS[i][0], PARTS[i][1]);
            }
            SHAPES.put(facing, parts);
            CONTROLLER_SHAPES.put(facing, buildShape(facing, 0, 0));
        }
    }

    private GatewayStructure() {
    }

    public static BlockPos partPos(BlockPos controller, Direction facing, int part) {
        return controller.relative(facing.getClockWise(), PARTS[part][0]).above(PARTS[part][1]);
    }

    public static BlockPos controllerPos(BlockPos partPos, Direction facing, int part) {
        return partPos.relative(facing.getClockWise(), -PARTS[part][0]).below(PARTS[part][1]);
    }

    /** Finds the controller of a gateway from any of its blocks. */
    public static Optional<BlockPos> findController(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.WARP_GATEWAY.get())) {
            return Optional.of(pos);
        }
        if (state.is(ModBlocks.GATEWAY_FRAME.get())) {
            BlockPos controller = controllerPos(pos, state.getValue(GatewayFrameBlock.FACING), state.getValue(GatewayFrameBlock.PART));
            if (level.getBlockState(controller).is(ModBlocks.WARP_GATEWAY.get())) {
                return Optional.of(controller);
            }
        }
        return Optional.empty();
    }

    public static VoxelShape frameShape(Direction facing, int part) {
        return SHAPES.get(facing)[part];
    }

    public static VoxelShape controllerShape(Direction facing) {
        return CONTROLLER_SHAPES.get(facing);
    }

    /** Clockwise rotation (seen from above) that turns the north-facing layout into {@code facing}. */
    public static float yRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    /** Converts a local (north-facing) point to a world position. */
    public static Vec3 toWorld(BlockPos controller, Direction facing, double x, double y, double z) {
        double wx, wz;
        switch (facing) {
            case SOUTH -> { wx = 1 - x; wz = 1 - z; }
            case EAST -> { wx = 1 - z; wz = x; }
            case WEST -> { wx = z; wz = 1 - x; }
            default -> { wx = x; wz = z; }
        }
        return new Vec3(controller.getX() + wx, controller.getY() + y, controller.getZ() + wz);
    }

    /** The area inside the ring. Anything touching it gets teleported. */
    public static AABB portalZone(BlockPos controller, Direction facing) {
        Vec3 a = toWorld(controller, facing, RING_X - 1.0, PLATFORM_TOP, 0.3);
        Vec3 b = toWorld(controller, facing, RING_X + 1.0, RING_Y + 1.05, 0.7);
        return new AABB(a, b);
    }

    /** Where travellers step out: on the platform, just in front of the ring, walking away from it. */
    public static Vec3 arrivalPoint(BlockPos controller, Direction facing) {
        return new Vec3(controller.getX() + 0.5 + facing.getStepX() * 1.3,
                controller.getY() + PLATFORM_TOP + 0.05,
                controller.getZ() + 0.5 + facing.getStepZ() * 1.3);
    }

    /** A point on the inner edge of the ring at the given angle, for particles. */
    public static Vec3 rimPoint(BlockPos controller, Direction facing, double angle) {
        return toWorld(controller, facing, RING_X + Math.cos(angle) * RING_INNER, RING_Y + Math.sin(angle) * RING_INNER, 0.5);
    }

    // ------------------------------------------------------------------ collision shapes

    private static boolean inRing(double x, double y) {
        double r = Math.hypot(x - RING_X, y - RING_Y);
        return r >= RING_INNER - 0.05 && r <= RING_OUTER;
    }

    private static boolean inPylon(double x, double y) {
        return y < 14.0 / 16 && (x < -0.5 || x > 1.5);
    }

    /**
     * Samples the gateway's shape on a 2-pixel grid inside the block at (sideways, height)
     * and merges each row into boxes.
     */
    private static VoxelShape buildShape(Direction facing, int sideways, int height) {
        VoxelShape shape = Shapes.empty();
        int cells = 8;
        double cell = 16.0 / cells;
        for (int j = 0; j < cells; j++) {
            double y = height + (j + 0.5) / cells;
            int runStart = -1;
            int runType = 0;
            for (int i = 0; i <= cells; i++) {
                int type = 0;
                if (i < cells) {
                    double x = sideways + (i + 0.5) / cells;
                    if (y < PLATFORM_TOP) type = 1;
                    else if (inPylon(x, y)) type = 2;
                    else if (inRing(x, y)) type = 3;
                }
                if (type != runType) {
                    if (runType != 0) {
                        double z0 = runType == 1 ? 0 : runType == 2 ? 2 : 4;
                        double z1 = 16 - z0;
                        shape = Shapes.or(shape, box(facing, runStart * cell, j * cell, z0, i * cell, (j + 1) * cell, z1));
                    }
                    runStart = i;
                    runType = type;
                }
            }
        }
        return shape.optimize();
    }

    /** Block.box for a north-facing box (pixels), rotated to {@code facing}. */
    private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        return switch (facing) {
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }
}
