package com.bot.client.movement.movements;

import com.bot.client.world.EnvironmentScan;
import com.bot.client.world.NavigationNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

public class MovementTraverse extends MovementBase {
    public MovementTraverse(NavigationNode startNode, NavigationNode endNode) {
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

        if (horizontalDist <= 0.35D && Math.abs(dy) <= 0.35D) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        EnvironmentScan scan = EnvironmentScan.scan(world, player.getX(), player.getY(), player.getZ(), dx, dz, dy);
        if (!scan.forwardStandable() && !scan.forwardStepUpStandable() && !scan.forwardStepDownStandable() 
            && !scan.leftStandable() && !scan.rightStandable()) {
            // Give up if blocked
            return state.setStatus(MovementStatus.UNREACHABLE);
        }

        boolean sprint = shouldSprint(player);
        double speed = WALK_SPEED * (sprint ? SPRINT_SPEED_MULTIPLIER : 1.0D);
        


        double velX = horizontalDist > 0.0001D ? (dx / horizontalDist) * speed : 0;
        double velZ = horizontalDist > 0.0001D ? (dz / horizontalDist) * speed : 0;
        double velY = player.getVelocity().y;

        if (scan.forwardStepUpStandable() && player.isOnGround()) {
            state.setJump(true);
            velY = JUMP_VELOCITY;
        }

        return state.setTargetVelocity(new Vec3d(velX, velY, velZ)).setSprint(sprint);
    }
}

