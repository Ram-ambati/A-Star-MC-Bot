package com.bot.client.movement;

import com.bot.client.world.BlockAnalyzer;
import com.bot.client.world.EnvironmentScan;
import com.bot.client.world.NavigationNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Stage 2 movement controller.
 *
 * Responsibilities:
 * - Execute a sequence of navigation nodes one at a time.
 * - Keep Stage 1 straight-line movement working for a single target.
 * - React locally when the immediate path is blocked by a wall, hazard, or hole.
 * - Prefer all world-sensing rules from {@link BlockAnalyzer} and {@link EnvironmentScan}.
 */
public class MovementController {
    private static final double NODE_REACHED_DISTANCE = 0.75D;
    private static final double WALK_SPEED = 0.215D;
    private static final double SLOWDOWN_DISTANCE = 2.0D;
    private static final double JUMP_VELOCITY = 0.42D;

    private final Deque<NavigationNode> remainingNodes = new ArrayDeque<>();
    private NavigationNode currentNode;
    private boolean active;
    private boolean appliedMovement;

    public void setTarget(double x, double y, double z) {
        setPath(List.of(new NavigationNode(BlockPos.ofFloored(x, y, z))));
    }

    public void setPath(List<NavigationNode> nodes) {
        clearPathState(true);

        if (nodes == null || nodes.isEmpty()) {
            active = false;
            return;
        }

        for (NavigationNode node : nodes) {
            if (node != null) {
                remainingNodes.addLast(copyNode(node));
            }
        }

        active = !remainingNodes.isEmpty();
        advanceToNextNode();
    }

    public void setPath(NavigationNode... nodes) {
        clearPathState(true);

        if (nodes == null || nodes.length == 0) {
            active = false;
            return;
        }

        for (NavigationNode node : nodes) {
            if (node != null) {
                remainingNodes.addLast(copyNode(node));
            }
        }

        active = !remainingNodes.isEmpty();
        advanceToNextNode();
    }

    public void clearTarget() {
        active = false;
        clearPathState(true);
    }

    public boolean isActive() {
        return active;
    }

    public Vec3d getTarget() {
        return active && currentNode != null ? nodeCenter(currentNode.position()) : null;
    }

    public List<NavigationNode> getActivePathSnapshot() {
        List<NavigationNode> snapshot = new ArrayList<>();
        if (currentNode != null) {
            snapshot.add(copyNode(currentNode));
        }
        for (NavigationNode node : remainingNodes) {
            snapshot.add(copyNode(node));
        }
        return snapshot;
    }

    public void tick() {
        if (!active) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        if (currentNode == null) {
            advanceToNextNode();
            if (!active) {
                return;
            }
        }

        Vec3d target = nodeCenter(currentNode.position());
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double dx = target.x - px;
        double dy = target.y - py;
        double dz = target.z - pz;
        double distanceSq = dx * dx + dy * dy + dz * dz;

        if (distanceSq <= NODE_REACHED_DISTANCE * NODE_REACHED_DISTANCE) {
            advanceToNextNode();
            if (!active) {
                finishNavigation(client, player);
            }
            return;
        }

        movePlayerTowardCurrentNode(player, client.world, dx, dy, dz, Math.sqrt(distanceSq));
    }

    private void movePlayerTowardCurrentNode(ClientPlayerEntity player, ClientWorld world, double dx, double dy, double dz, double distance) {
        if (world == null) {
            stopControlledMovement(player);
            return;
        }

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance <= 0.0001D) {
            stopControlledMovement(player);
            return;
        }

        EnvironmentScan scan = EnvironmentScan.scan(world, player.getX(), player.getY(), player.getZ(), dx, dz, dy);
        LocalMovementChoice choice = chooseMovementChoice(scan, dx, dy, dz);
        if (choice.isBlocked()) {
            stopControlledMovement(player);
            return;
        }

        Vec3d velocity = player.getVelocity();
        double speed = WALK_SPEED * choice.speedMultiplier * Math.min(1.0D, distance / SLOWDOWN_DISTANCE);
        double velocityX = choice.direction.x * speed;
        double velocityZ = choice.direction.z * speed;
        double velocityY = velocity.y;

        if (choice.jumpNeeded && player.isOnGround()) {
            player.jump();
            velocityY = Math.max(velocityY, JUMP_VELOCITY);
        }

        player.setVelocity(velocityX, velocityY, velocityZ);
        appliedMovement = true;
    }

    private LocalMovementChoice chooseMovementChoice(EnvironmentScan scan, double dx, double dy, double dz) {
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double forwardX = dx / horizontalDistance;
        double forwardZ = dz / horizontalDistance;

        if (scan.forwardStandable()) {
            return LocalMovementChoice.forward(forwardX, forwardZ, 1.0D);
        }

        if (scan.forwardStepUpStandable()) {
            return LocalMovementChoice.stepUp(forwardX, forwardZ);
        }

        if (scan.forwardStepDownStandable()) {
            return LocalMovementChoice.stepDown(forwardX, forwardZ);
        }

        if (scan.leftStandable()) {
            return LocalMovementChoice.strafe(-forwardZ, forwardX, 0.95D);
        }

        if (scan.rightStandable()) {
            return LocalMovementChoice.strafe(forwardZ, -forwardX, 0.95D);
        }

        return LocalMovementChoice.blocked();
    }

    private void advanceToNextNode() {
        currentNode = remainingNodes.pollFirst();
        if (currentNode == null) {
            active = false;
        }
    }

    private void finishNavigation(MinecraftClient client, ClientPlayerEntity player) {
        active = false;
        currentNode = null;
        remainingNodes.clear();
        stopControlledMovement(player);

        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal("Arrived at target."));
        }
    }

    private void clearPathState(boolean stopMovement) {
        remainingNodes.clear();
        currentNode = null;

        if (stopMovement) {
            stopControlledMovement();
        }
    }

    private void stopControlledMovement() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            stopControlledMovement(client.player);
        }
    }

    private void stopControlledMovement(ClientPlayerEntity player) {
        if (!appliedMovement) {
            return;
        }

        Vec3d velocity = player.getVelocity();
        player.setVelocity(0.0D, velocity.y, 0.0D);
        appliedMovement = false;
    }

    private static NavigationNode copyNode(NavigationNode node) {
        return new NavigationNode(node.position(), node.movementCost(), node.estimatedCost(), node.parent());
    }

    private static Vec3d nodeCenter(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    }

    private static final class LocalMovementChoice {
        private final Vec3d direction;
        private final double speedMultiplier;
        private final boolean jumpNeeded;
        private final boolean isBlocked;

        private LocalMovementChoice(Vec3d direction, double speedMultiplier, boolean jumpNeeded, boolean isBlocked) {
            this.direction = direction;
            this.speedMultiplier = speedMultiplier;
            this.jumpNeeded = jumpNeeded;
            this.isBlocked = isBlocked;
        }

        private static LocalMovementChoice forward(double x, double z, double speedMultiplier) {
            return new LocalMovementChoice(new Vec3d(x, 0.0D, z), speedMultiplier, false, false);
        }

        private static LocalMovementChoice strafe(double x, double z, double speedMultiplier) {
            return new LocalMovementChoice(new Vec3d(x, 0.0D, z), speedMultiplier, false, false);
        }

        private static LocalMovementChoice stepUp(double x, double z) {
            return new LocalMovementChoice(new Vec3d(x, 0.0D, z), 0.90D, true, false);
        }

        private static LocalMovementChoice stepDown(double x, double z) {
            return new LocalMovementChoice(new Vec3d(x, 0.0D, z), 1.0D, false, false);
        }

        private static LocalMovementChoice blocked() {
            return new LocalMovementChoice(Vec3d.ZERO, 0.0D, false, true);
        }

        private boolean isBlocked() {
            return isBlocked;
        }
    }
}
