package com.bot.client.pathfinding;

import com.bot.client.world.BlockAnalyzer;
import com.bot.client.world.NavigationNode;
import com.bot.client.world.NeighborGenerator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Standard A* route planner for Stage 3.
 *
 * This layer only decides which nodes form the route. Movement execution is
 * still handled elsewhere, and rendering can visualize the final route.
 */
public final class LocalRoutePlanner {
    private static final int MAX_EXPANSIONS = 4096;
    // Weighted A* weight: 1.0 = standard A* (optimal), > 1.0 = aggressively goal-directed (faster but suboptimal)
    private static final double DEFAULT_HEURISTIC_WEIGHT = 1.5D;

    private LocalRoutePlanner() {
    }

    public static List<NavigationNode> findRoute(BlockPos start, BlockPos goal) {
        ClientWorld world = MinecraftClient.getInstance().world;
        return world == null ? List.of() : findRoute(world, start, goal);
    }

    public static List<NavigationNode> findRoute(ClientWorld world, BlockPos start, BlockPos goal) {
        return findRoute(world, start, goal, DEFAULT_HEURISTIC_WEIGHT);
    }

    public static List<NavigationNode> findRoute(ClientWorld world, BlockPos start, BlockPos goal, double heuristicWeight) {
        if (world == null || start == null || goal == null) {
            return List.of();
        }

        NavigationNode startNode = new NavigationNode(start, 0.0D, weightedHeuristic(start, goal, heuristicWeight), null);
        PriorityQueue<NavigationNode> openSet = new PriorityQueue<>(Comparator
                .comparingDouble(NavigationNode::totalCost)
                .thenComparingDouble(NavigationNode::estimatedCost));
        Map<BlockPos, Double> bestCosts = new HashMap<>();
        Map<BlockPos, NavigationNode> bestNodes = new HashMap<>();

        openSet.add(startNode);
        bestCosts.put(startNode.position(), 0.0D);
        bestNodes.put(startNode.position(), startNode);

        // Track the best node found during search for partial path fallback
        NavigationNode bestNodeFound = startNode;
        double bestDistanceToGoal = heuristic(start, goal);

        int expansions = 0;
        while (!openSet.isEmpty() && expansions++ < MAX_EXPANSIONS) {
            NavigationNode current = openSet.poll();
            Double knownBest = bestCosts.get(current.position());
            if (knownBest != null && current.movementCost() > knownBest) {
                continue;
            }

            if (isGoal(current.position(), goal)) {
                return reconstructPath(current);
            }

            // Track best partial path (closest node to goal found so far)
            double distToGoal = heuristic(current.position(), goal);
            if (distToGoal < bestDistanceToGoal) {
                bestDistanceToGoal = distToGoal;
                bestNodeFound = current;
            }

            for (NavigationNode neighbor : NeighborGenerator.getLocalNeighbors(world, current, goal)) {
                double tentativeCost = neighbor.movementCost();
                Double bestCost = bestCosts.get(neighbor.position());
                if (bestCost != null && tentativeCost >= bestCost) {
                    continue;
                }

                neighbor.setEstimatedCost(weightedHeuristic(neighbor.position(), goal, heuristicWeight));
                bestCosts.put(neighbor.position(), tentativeCost);
                bestNodes.put(neighbor.position(), neighbor);
                openSet.add(neighbor);
            }
        }

        // If we didn't find the goal, return the partial path to the best node we found
        if (bestNodeFound != startNode) {
            return reconstructPath(bestNodeFound);
        }

        return List.of();
    }

    public static boolean isRouteStandable(ClientWorld world, List<NavigationNode> route) {
        if (world == null || route == null || route.isEmpty()) {
            return false;
        }

        for (NavigationNode node : route) {
            if (!BlockAnalyzer.canStandAt(world, node.position())) {
                return false;
            }
        }

        return true;
    }

    private static List<NavigationNode> reconstructPath(NavigationNode goalNode) {
        List<NavigationNode> path = new ArrayList<>();
        NavigationNode cursor = goalNode;
        while (cursor != null) {
            path.add(0, copyNode(cursor));
            cursor = cursor.parent();
        }
        return path;
    }

    private static NavigationNode copyNode(NavigationNode node) {
        return new NavigationNode(node.position(), node.movementCost(), node.estimatedCost(), node.parent());
    }

    private static boolean isGoal(BlockPos position, BlockPos goal) {
        return position.getX() == goal.getX()
                && position.getY() == goal.getY()
                && position.getZ() == goal.getZ();
    }

    private static double heuristic(BlockPos from, BlockPos to) {
        return Math.abs(from.getX() - to.getX())
                + Math.abs(from.getY() - to.getY())
                + Math.abs(from.getZ() - to.getZ());
    }

    private static double weightedHeuristic(BlockPos from, BlockPos to, double weight) {
        // Multiply heuristic by weight to favor goal-directed exploration (Weighted A*)
        // weight = 1.0 → standard A* (optimal)
        // weight > 1.0 → aggressively goal-directed (faster but may be suboptimal)
        return heuristic(from, to) * weight;
    }
}

