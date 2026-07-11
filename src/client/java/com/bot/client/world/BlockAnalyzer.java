package com.bot.client.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Stage 2 world analysis utilities.
 *
 * These methods intentionally answer navigation-level questions instead of
 * exposing raw "is this air/stone" checks to the rest of the bot. Pathfinding,
 * obstacle avoidance, and future A* code should ask whether a position is safe,
 * standable, dangerous, or blocked, then let this class own the Minecraft block
 * and fluid details.
 */
public final class BlockAnalyzer {
    public static final double FLAT_MOVE_COST = 1.0D;
    public static final double STEP_UP_COST = 1.35D;
    public static final double STEP_DOWN_COST = 0.85D;
    public static final double WATER_MOVE_COST = 3.0D;
    public static final double HAZARD_MOVE_COST = 25.0D;
    public static final double BLOCKED_MOVE_COST = Double.POSITIVE_INFINITY;

    private BlockAnalyzer() {
    }

    public static boolean isWalkable(BlockPos pos) {
        ClientWorld world = getWorld();
        return world != null && isWalkable(world, pos);
    }

    public static boolean isWalkable(ClientWorld world, BlockPos pos) {
        return isStandable(world, pos);
    }

    public static boolean isSafe(BlockPos pos) {
        ClientWorld world = getWorld();
        return world != null && isSafe(world, pos);
    }

    public static boolean isSafe(ClientWorld world, BlockPos pos) {
        return canStandAt(world, pos);
    }

    public static boolean hasGroundBelow(BlockPos pos) {
        ClientWorld world = getWorld();
        return world != null && hasGroundBelow(world, pos);
    }

    public static boolean hasGroundBelow(ClientWorld world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }

        BlockPos groundPos = pos.down();
        BlockState ground = world.getBlockState(groundPos);
        return !isHazard(world, groundPos)
                && ground.isSideSolidFullSquare(world, groundPos, Direction.UP)
                && !ground.getCollisionShape(world, groundPos).isEmpty();
    }

    public static boolean isHazard(BlockPos pos) {
        ClientWorld world = getWorld();
        return world != null && isHazard(world, pos);
    }

    public static boolean isHazard(ClientWorld world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        return state.getFluidState().isIn(FluidTags.LAVA)
                || state.isOf(Blocks.LAVA)
                || state.isOf(Blocks.FIRE)
                || state.isOf(Blocks.SOUL_FIRE)
                || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.MAGMA_BLOCK)
                || state.isOf(Blocks.CAMPFIRE)
                || state.isOf(Blocks.SOUL_CAMPFIRE)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.POWDER_SNOW);
    }

    public static boolean isObstacle(BlockPos pos) {
        ClientWorld world = getWorld();
        return world != null && isObstacle(world, pos);
    }

    public static boolean isObstacle(ClientWorld world, BlockPos pos) {
        return !isPassable(world, pos);
    }

    public static boolean isStandable(ClientWorld world, BlockPos pos) {
        return canStandAt(world, pos);
    }

    public static boolean canStandAt(ClientWorld world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }

        return !isHazard(world, pos)
                && !isHazard(world, pos.down())
                && isPassable(world, pos)
                && isPassable(world, pos.up())
                && hasGroundBelow(world, pos);
    }

    public static boolean canStepTo(ClientWorld world, BlockPos from, BlockPos to) {
        if (world == null || from == null || to == null) {
            return false;
        }

        int dx = Math.abs(to.getX() - from.getX());
        int dz = Math.abs(to.getZ() - from.getZ());
        int dy = to.getY() - from.getY();

        if (dx + dz != 1) {
            return false;
        }

        // Allow stepping up 1 block or stepping down up to 3 blocks
        if (dy < -3 || dy > 1) {
            return false;
        }

        // For descents, validate that all intermediate levels have safe ground
        if (dy < 0) {
            return canDescendTo(world, from, to, dy);
        }

        return canStandAt(world, to);
    }

    private static boolean canDescendTo(ClientWorld world, BlockPos from, BlockPos to, int dySteps) {
        // dy is negative, so we're descending |dy| blocks
        // Validate that each step down has ground and no hazards
        BlockPos current = from;
        for (int i = 0; i < Math.abs(dySteps); i++) {
            BlockPos nextStep = current.down();
            if (!hasGroundBelow(world, nextStep) || isHazard(world, nextStep.down())) {
                return false;
            }
            current = nextStep;
        }
        // Final destination must be standable
        return canStandAt(world, to);
    }

    public static double movementCostBetween(ClientWorld world, BlockPos from, BlockPos to) {
        if (!canStepTo(world, from, to)) {
            return BLOCKED_MOVE_COST;
        }

        int dy = to.getY() - from.getY();
        double cost = FLAT_MOVE_COST;

        if (dy == 1) {
            cost += STEP_UP_COST;
        } else if (dy < 0) {
            // Multi-block descent: cost scales with descent distance
            // 1-block descent: STEP_DOWN_COST
            // 2-block descent: STEP_DOWN_COST + (FLAT_MOVE_COST * 0.5) bonus
            // 3-block descent: STEP_DOWN_COST + (FLAT_MOVE_COST * 1.0) bonus
            cost += STEP_DOWN_COST;
            if (dy < -1) {
                cost += FLAT_MOVE_COST * 0.5D * (-dy - 1);
            }
        }

        if (isWater(world, to)) {
            cost += WATER_MOVE_COST;
        }

        if (isHazard(world, to) || isHazard(world, to.down())) {
            cost += HAZARD_MOVE_COST;
        }

        return cost;
    }

    public static boolean isPassable(ClientWorld world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        return state.isAir()
                || (state.getFluidState().isEmpty() && state.getCollisionShape(world, pos).isEmpty());
    }

    public static boolean isWater(ClientWorld world, BlockPos pos) {
        return world != null && pos != null && world.getBlockState(pos).getFluidState().isIn(FluidTags.WATER);
    }

    private static ClientWorld getWorld() {
        return MinecraftClient.getInstance().world;
    }
}
