package com.bot.client.movement.movements;

import com.bot.client.world.NavigationNode;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public abstract class MovementBase implements IMovement {
    protected final NavigationNode startNode;
    protected final NavigationNode endNode;

    protected static final double WALK_SPEED = 0.216D;
    protected static final double SPRINT_SPEED_MULTIPLIER = 1.30D;
    protected static final double DESCEND_SPEED_MULTIPLIER = 1.0D;
    protected static final double JUMP_VELOCITY = 0.42D;

    public MovementBase(NavigationNode startNode, NavigationNode endNode) {
        this.startNode = startNode;
        this.endNode = endNode;
    }

    protected boolean shouldSprint(ClientPlayerEntity player) {
        // Don't sprint in water — vanilla swim speed is walk speed.
        // Sprinting while jumping out of water looks like a cheat/exploit.
        if (player.isTouchingWater()) return false;
        try {
            return player.isCreative()
                    || (player.getHungerManager() != null && player.getHungerManager().getFoodLevel() > 6);
        } catch (Throwable ignored) {
            return false;
        }
    }

    protected Vec3d getTargetCenter() {
        return new Vec3d(endNode.position().getX() + 0.5D, endNode.position().getY(), endNode.position().getZ() + 0.5D);
    }

    protected double getHorizontalDistance(ClientPlayerEntity player, Vec3d target) {
        double dx = target.x - player.getX();
        double dz = target.z - player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
