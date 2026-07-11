package com.bot.client.movement.movements;

import com.bot.client.world.NavigationNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

public class MovementDescend extends MovementBase {
    public MovementDescend(NavigationNode startNode, NavigationNode endNode) {
        super(startNode, endNode);
    }

    @Override
    public MovementState tick(MinecraftClient client, ClientPlayerEntity player, ClientWorld world) {
        MovementState state = new MovementState();
        Vec3d target = getTargetCenter();
        
        double dx = target.x - player.getX();
        double dy = target.y - player.getY();
        double dz = target.z - player.getZ();
        double horizontalDist = getHorizontalDistance(player, target);

        if (horizontalDist <= 0.35D && dy >= -0.25D && dy <= 0.35D) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        double speed = WALK_SPEED * DESCEND_SPEED_MULTIPLIER;
        boolean sneak = false;
        
        // Removed sneak and slowdown logic here to prevent getting stuck at block edges


        double velX = horizontalDist > 0.0001D ? (dx / horizontalDist) * speed : 0;
        double velZ = horizontalDist > 0.0001D ? (dz / horizontalDist) * speed : 0;
        double velY = player.getVelocity().y;

        return state.setTargetVelocity(new Vec3d(velX, velY, velZ)).setSneak(sneak);
    }
}
