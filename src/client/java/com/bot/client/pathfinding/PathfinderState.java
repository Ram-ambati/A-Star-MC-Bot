package com.bot.client.pathfinding;

import com.bot.client.world.NavigationNode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.util.math.BlockPos;

public class PathfinderState {
    public final BlockPos start;
    public final BlockPos goal;
    public final double heuristicWeight;
    
    public final PriorityQueue<NavigationNode> openSet;
    public final Map<BlockPos, Double> bestCosts;
    public final Map<BlockPos, NavigationNode> bestNodes;
    
    public NavigationNode bestNodeFound;
    public double bestDistanceToGoal;
    public int expansions = 0;
    
    public enum Status {
        RUNNING, SUCCESS, FAILED
    }
    
    public Status status = Status.RUNNING;
    public java.util.List<NavigationNode> finalRoute = null;

    public PathfinderState(BlockPos start, BlockPos goal, double heuristicWeight) {
        this.start = start;
        this.goal = goal;
        this.heuristicWeight = heuristicWeight;
        this.openSet = new PriorityQueue<>(Comparator
                .comparingDouble(NavigationNode::totalCost)
                .thenComparingDouble(NavigationNode::estimatedCost));
        this.bestCosts = new HashMap<>();
        this.bestNodes = new HashMap<>();
    }
}
