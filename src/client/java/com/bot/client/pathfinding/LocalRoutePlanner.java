package com.bot.client.pathfinding;

import com.bot.client.world.BlockAnalyzer;
import com.bot.client.world.NavigationNode;
import com.bot.client.world.NeighborGenerator;
import java.util.ArrayList;
import java.util.List;
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

    public static PathfinderState beginRoute(ClientWorld world, BlockPos start, BlockPos goal, double heuristicWeight) {
        if (world == null || start == null || goal == null) {
            return null;
        }

        PathfinderState state = new PathfinderState(start, goal, heuristicWeight);
        NavigationNode startNode = new NavigationNode(start, 0.0D, weightedHeuristic(start, goal, heuristicWeight), null);
        state.openSet.add(startNode);
        state.bestCosts.put(startNode.position(), 0.0D);
        state.bestNodes.put(startNode.position(), startNode);
        
        state.bestNodeFound = startNode;
        state.bestDistanceToGoal = heuristic(start, goal);
        
        return state;
    }
    
    public static void tick(ClientWorld world, PathfinderState state, int maxExpansions) {
        if (state == null || state.status != PathfinderState.Status.RUNNING) {
            return;
        }

        int expansionsThisTick = 0;
        while (!state.openSet.isEmpty() && expansionsThisTick++ < maxExpansions && state.expansions++ < MAX_EXPANSIONS) {
            NavigationNode current = state.openSet.poll();
            Double knownBest = state.bestCosts.get(current.position());
            if (knownBest != null && current.movementCost() > knownBest) {
                continue;
            }

            if (isGoal(current.position(), state.goal)) {
                state.finalRoute = reconstructPath(current);
                state.status = PathfinderState.Status.SUCCESS;
                return;
            }

            // Track best partial path (closest node to goal found so far)
            double distToGoal = heuristic(current.position(), state.goal);
            if (distToGoal < state.bestDistanceToGoal) {
                state.bestDistanceToGoal = distToGoal;
                state.bestNodeFound = current;
            }

            for (NavigationNode neighbor : NeighborGenerator.getLocalNeighbors(world, current, state.goal)) {
                double tentativeCost = neighbor.movementCost();
                Double bestCost = state.bestCosts.get(neighbor.position());
                if (bestCost != null && tentativeCost >= bestCost) {
                    continue;
                }

                neighbor.setEstimatedCost(weightedHeuristic(neighbor.position(), state.goal, state.heuristicWeight));
                state.bestCosts.put(neighbor.position(), tentativeCost);
                state.bestNodes.put(neighbor.position(), neighbor);
                state.openSet.add(neighbor);
            }
        }

        if (state.status == PathfinderState.Status.RUNNING) {
            if (state.openSet.isEmpty() || state.expansions >= MAX_EXPANSIONS) {
                // Search finished without finding goal
                if (state.bestNodeFound != null && !state.bestNodeFound.position().equals(state.start)) {
                    state.finalRoute = reconstructPath(state.bestNodeFound);
                    state.status = PathfinderState.Status.SUCCESS; // Partial path is considered success here
                } else {
                    state.finalRoute = List.of();
                    state.status = PathfinderState.Status.FAILED;
                }
            }
        }
    }
    
    public static List<NavigationNode> findRoute(ClientWorld world, BlockPos start, BlockPos goal, double heuristicWeight) {
        PathfinderState state = beginRoute(world, start, goal, heuristicWeight);
        if (state == null) return List.of();
        tick(world, state, MAX_EXPANSIONS);
        return state.finalRoute != null ? state.finalRoute : List.of();
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

    public static boolean isGoal(BlockPos position, BlockPos goal) {
        return position.getX() == goal.getX()
                && Math.abs(position.getY() - goal.getY()) <= 2
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

