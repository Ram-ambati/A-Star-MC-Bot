package com.bot.client.world;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Snapshot of the immediate area around the player for one movement tick.
 *
 * This is deliberately small and local. It is not a path; it is the sensory
 * layer that lets movement code know whether the direct step is blocked,
 * dangerous, or unsupported before future pathfinding chooses better routes.
 */
public final class EnvironmentScan {
    private final BlockPos current;
    private final BlockPos ahead;
    private final BlockPos left;
    private final BlockPos right;
    private final BlockPos aheadUp;
    private final BlockPos aheadDown;
    private final boolean obstacleAhead;
    private final boolean hazardAhead;
    private final boolean holeAhead;
    private final boolean waterAhead;
    private final boolean forwardStandable;
    private final boolean forwardStepUpStandable;
    private final boolean forwardStepDownStandable;
    private final boolean leftStandable;
    private final boolean rightStandable;

    private EnvironmentScan(
            BlockPos current,
            BlockPos ahead,
            BlockPos left,
            BlockPos right,
            BlockPos aheadUp,
            BlockPos aheadDown,
            boolean obstacleAhead,
            boolean hazardAhead,
            boolean holeAhead,
            boolean waterAhead,
            boolean forwardStandable,
            boolean forwardStepUpStandable,
            boolean forwardStepDownStandable,
            boolean leftStandable,
            boolean rightStandable
    ) {
        this.current = current;
        this.ahead = ahead;
        this.left = left;
        this.right = right;
        this.aheadUp = aheadUp;
        this.aheadDown = aheadDown;
        this.obstacleAhead = obstacleAhead;
        this.hazardAhead = hazardAhead;
        this.holeAhead = holeAhead;
        this.waterAhead = waterAhead;
        this.forwardStandable = forwardStandable;
        this.forwardStepUpStandable = forwardStepUpStandable;
        this.forwardStepDownStandable = forwardStepDownStandable;
        this.leftStandable = leftStandable;
        this.rightStandable = rightStandable;
    }

    public static EnvironmentScan scan(ClientWorld world, double x, double y, double z, double directionX, double directionZ) {
        return scan(world, x, y, z, directionX, directionZ, 0.0D);
    }

    public static EnvironmentScan scan(ClientWorld world, double x, double y, double z, double directionX, double directionZ, double directionY) {
        double horizontalLength = Math.sqrt(directionX * directionX + directionZ * directionZ);
        double forwardX = horizontalLength > 0.0001D ? directionX / horizontalLength : 0.0D;
        double forwardZ = horizontalLength > 0.0001D ? directionZ / horizontalLength : 0.0D;
        double leftX = -forwardZ;
        double leftZ = forwardX;

        BlockPos current = BlockPos.ofFloored(x, y, z);
        BlockPos ahead = BlockPos.ofFloored(x + forwardX, y, z + forwardZ);
        BlockPos aheadUp = ahead.up();
        BlockPos aheadDown = ahead.down();
        BlockPos left = BlockPos.ofFloored(x + leftX, y, z + leftZ);
        BlockPos right = BlockPos.ofFloored(x - leftX, y, z - leftZ);

        boolean forwardStandable = BlockAnalyzer.canStepTo(world, current, ahead);
        boolean forwardStepUpStandable = BlockAnalyzer.canStepTo(world, current, aheadUp);
        boolean forwardStepDownStandable = BlockAnalyzer.canStepTo(world, current, aheadDown);
        boolean obstacleAhead = BlockAnalyzer.isObstacle(world, ahead) || BlockAnalyzer.isObstacle(world, ahead.up());
        boolean hazardAhead = BlockAnalyzer.isHazard(world, ahead) || BlockAnalyzer.isHazard(world, ahead.down());
        boolean holeAhead = !BlockAnalyzer.hasGroundBelow(world, ahead);
        boolean waterAhead = BlockAnalyzer.isWater(world, ahead) || BlockAnalyzer.isWater(world, ahead.down());

        return new EnvironmentScan(
                current,
                ahead,
                left,
                right,
                aheadUp,
                aheadDown,
                obstacleAhead,
                hazardAhead,
                holeAhead,
                waterAhead,
                forwardStandable,
                forwardStepUpStandable,
                forwardStepDownStandable,
                BlockAnalyzer.canStepTo(world, current, left),
                BlockAnalyzer.canStepTo(world, current, right)
        );
    }

    public boolean directPathUnsafe() {
        return obstacleAhead || hazardAhead || holeAhead || waterAhead;
    }

    public BlockPos current() {
        return current;
    }

    public BlockPos ahead() {
        return ahead;
    }

    public BlockPos aheadUp() {
        return aheadUp;
    }

    public BlockPos aheadDown() {
        return aheadDown;
    }

    public BlockPos left() {
        return left;
    }

    public BlockPos right() {
        return right;
    }

    public boolean obstacleAhead() {
        return obstacleAhead;
    }

    public boolean hazardAhead() {
        return hazardAhead;
    }

    public boolean holeAhead() {
        return holeAhead;
    }

    public boolean waterAhead() {
        return waterAhead;
    }

    public boolean forwardStandable() {
        return forwardStandable;
    }

    public boolean forwardStepUpStandable() {
        return forwardStepUpStandable;
    }

    public boolean forwardStepDownStandable() {
        return forwardStepDownStandable;
    }

    public boolean leftStandable() {
        return leftStandable;
    }

    public boolean rightStandable() {
        return rightStandable;
    }
}
