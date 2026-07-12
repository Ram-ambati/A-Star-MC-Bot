package com.bot.client.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Produces locally reachable navigation nodes.
 *
 * This is still not full pathfinding. It is the local expansion rule used by
 * later A* code and by tests that want to verify which nearby positions the
 * bot considers reachable.
 */
public final class NeighborGenerator {
    private static final Direction[] CARDINAL_DIRECTIONS = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };


    private NeighborGenerator() {
    }

    public static List<NavigationNode> getCardinalNeighbors(ClientWorld world, NavigationNode node, BlockPos target) {
        return getLocalNeighbors(world, node, target);
    }

    public static List<NavigationNode> getLocalNeighbors(ClientWorld world, NavigationNode node, BlockPos target) {
        List<NavigationNode> neighbors = new ArrayList<>();
        BlockPos originPos = node.position();

        // Cardinal directions
        for (Direction direction : CARDINAL_DIRECTIONS) {
            BlockPos base = originPos.offset(direction);
            addIfReachable(world, node, target, neighbors, base);
            addIfReachable(world, node, target, neighbors, base.up());
            addIfReachable(world, node, target, neighbors, base.down());
        }



        return neighbors;
    }

    private static void addIfReachable(
            ClientWorld world,
            NavigationNode origin,
            BlockPos target,
            List<NavigationNode> neighbors,
            BlockPos candidate
    ) {
        if (!BlockAnalyzer.canStepTo(world, origin.position(), candidate)) {
            return;
        }

        NavigationNode neighbor = new NavigationNode(candidate);
        neighbor.setParent(origin);
        neighbor.setMovementCost(origin.movementCost() + BlockAnalyzer.movementCostBetween(world, origin.position(), candidate));
        neighbor.setEstimatedCost(manhattanDistance(candidate, target));
        neighbors.add(neighbor);
    }

    private static int manhattanDistance(BlockPos from, BlockPos to) {
        return Math.abs(from.getX() - to.getX())
                + Math.abs(from.getY() - to.getY())
                + Math.abs(from.getZ() - to.getZ());
    }
}
