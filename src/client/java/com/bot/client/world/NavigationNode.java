package com.bot.client.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Pathfinding node model for future A*.
 *
 * The current phase only creates and validates nodes. Later A* work can fill
 * gCost/hCost/parent while reusing the same position and neighbor semantics.
 */
public class NavigationNode {
    private final BlockPos position;
    private double movementCost;
    private double estimatedCost;
    private NavigationNode parent;

    public NavigationNode(BlockPos position) {
        this(position, 0.0D, 0.0D, null);
    }

    public NavigationNode(BlockPos position, double movementCost, double estimatedCost, NavigationNode parent) {
        this.position = position.toImmutable();
        this.movementCost = movementCost;
        this.estimatedCost = estimatedCost;
        this.parent = parent;
    }

    public BlockPos position() {
        return position;
    }

    public Vec3d center() {
        return new Vec3d(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
    }

    public double movementCost() {
        return movementCost;
    }

    public void setMovementCost(double movementCost) {
        this.movementCost = movementCost;
    }

    public double estimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public double totalCost() {
        return movementCost + estimatedCost;
    }

    public NavigationNode parent() {
        return parent;
    }

    public void setParent(NavigationNode parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "NavigationNode{" +
                "position=" + position +
                ", movementCost=" + movementCost +
                ", estimatedCost=" + estimatedCost +
                '}';
    }
}
