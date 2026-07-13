package com.bot.client.movement.movements;

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

        boolean airborne = !player.isOnGround() && !player.isTouchingWater();

        // Relax the Y tolerance when airborne so the bot advances nodes during a jump
        // instead of oscillating around the node waiting for the player to land.
        // Also relax horizontal slightly when falling so descent nodes complete cleanly.
        double tolXZ = airborne ? 0.6D : 0.35D;
        double tolY  = airborne ? 3.0D : 0.35D; // free-fall up to 3 blocks is fine

        if (horizontalDist <= tolXZ && Math.abs(dy) <= tolY) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        boolean sprint = shouldSprint(player);
        double speed = WALK_SPEED * (sprint ? SPRINT_SPEED_MULTIPLIER : 1.0D);

        // XZ velocity is always applied — even when airborne — to maintain forward motion.
        double velX = horizontalDist > 0.0001D ? (dx / horizontalDist) * speed : 0;
        double velZ = horizontalDist > 0.0001D ? (dz / horizontalDist) * speed : 0;

        // Water swimming is handled entirely by KeyboardInputMixin (playerInput.jump = true).
        // On land: jump only when we need to step up a block.
        if (!player.isTouchingWater() && dy > 0.5D && player.isOnGround()) {
            if (player.horizontalCollision || horizontalDist < 0.85D) {
                state.setJump(true);
            }
        }
        // Free fall (airborne, no jump needed): just maintain XZ and let gravity do its thing.

        return state.setTargetVelocity(new Vec3d(velX, player.getVelocity().y, velZ)).setSprint(sprint);
    }
}
