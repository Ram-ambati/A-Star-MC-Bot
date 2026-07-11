package com.bot.client.movement;

import com.bot.client.movement.movements.IMovement;
import com.bot.client.movement.movements.MovementHelper;
import com.bot.client.movement.movements.MovementState;
import com.bot.client.movement.movements.MovementStatus;
import com.bot.client.pathfinding.LocalRoutePlanner;
import com.bot.client.world.NavigationNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MovementController {
    private static final double VELOCITY_SMOOTHING_FACTOR = 0.20D;
    private static final double DIRECTION_CHANGE_THRESHOLD = 0.4D;
    private static final int MAX_TICKS_AT_NODE = 60; // 3 seconds

    public static boolean isSpoofing = false;
    public static float spoofedYaw = 0.0f;
    public static float spoofedPitch = 0.0f;

    private final Deque<NavigationNode> remainingNodes = new ArrayDeque<>();
    private NavigationNode previousNode;
    private NavigationNode currentNode;
    private IMovement currentMovement;

    private boolean active;
    private boolean appliedMovement;
    private Vec3d smoothedVelocity = Vec3d.ZERO;
    private BlockPos originalGoal;
    private int recalculationAttempts = 0;
    private static final int MAX_RECALCULATION_ATTEMPTS = 3;
    private Vec3d lastRecalculationPos = null;
    private static final double RECALCULATION_DISTANCE = 30.0D;
    
    private int ticksAtCurrentNode = 0;
    private boolean enableVelocitySmoothing = false; // Disabled as per user request

    public void setTarget(double x, double y, double z) {
        setPath(List.of(new NavigationNode(BlockPos.ofFloored(x, y, z))));
    }

    public void setPath(List<NavigationNode> nodes) {
        clearPathState();
        if (nodes == null || nodes.isEmpty()) { active = false; return; }
        for (NavigationNode node : nodes) { if (node != null) { remainingNodes.addLast(copyNode(node)); } }
        active = !remainingNodes.isEmpty();
        advanceToNextNode(MinecraftClient.getInstance().player);
    }

    public void setPath(NavigationNode... nodes) {
        clearPathState();
        if (nodes == null || nodes.length == 0) { active = false; return; }
        for (NavigationNode node : nodes) { if (node != null) { remainingNodes.addLast(copyNode(node)); } }
        active = !remainingNodes.isEmpty();
        advanceToNextNode(MinecraftClient.getInstance().player);
    }

    public void setPlannedRoute(List<NavigationNode> route) {
        setPath(route);
        if (route != null && !route.isEmpty()) {
            originalGoal = route.getLast().position();
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                lastRecalculationPos = new Vec3d(player.getX(), player.getY(), player.getZ());
            }
            recalculationAttempts = 0;
        }
    }

    public void clearTarget() {
        active = false;
        originalGoal = null;
        recalculationAttempts = 0;
        lastRecalculationPos = null;
        clearPathState();
    }

    public boolean isActive() { return active; }

    public Vec3d getTarget() {
        return active && currentNode != null ? nodeCenter(currentNode.position()) : null;
    }

    public List<NavigationNode> getActivePathSnapshot() {
        List<NavigationNode> snapshot = new ArrayList<>();
        if (currentNode != null) { snapshot.add(copyNode(currentNode)); }
        for (NavigationNode node : remainingNodes) { snapshot.add(copyNode(node)); }
        return snapshot;
    }

    public void tick() {
        if (!active) return;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (originalGoal != null && lastRecalculationPos != null) {
            Vec3d currentPos = new Vec3d(player.getX(), player.getY(), player.getZ());
            double distTraveled = currentPos.distanceTo(lastRecalculationPos);
            if (distTraveled >= RECALCULATION_DISTANCE && recalculationAttempts < MAX_RECALCULATION_ATTEMPTS) {
                recalculateRoute(client.world, player.getBlockPos(), originalGoal);
                return;
            }
        }

        while (active) {
            if (currentNode == null) {
                advanceToNextNode(player);
                if (!active) {
                    player.sendMessage(net.minecraft.text.Text.literal("§aReached destination!"), false);
                    return;
                }
            }
            
            ticksAtCurrentNode++;
            if (ticksAtCurrentNode > MAX_TICKS_AT_NODE) {
                recalculateRoute(client.world, player.getBlockPos(), originalGoal);
                return;
            }

            if (currentMovement == null) {
                NavigationNode prev = previousNode != null ? previousNode : new NavigationNode(player.getBlockPos());
                currentMovement = MovementHelper.createMovement(prev, currentNode);
                if (currentMovement == null) {
                    advanceToNextNode(player);
                    if (!active) {
                        finishNavigation(client, player);
                        player.sendMessage(net.minecraft.text.Text.literal("§aReached destination!"), false);
                    }
                    continue;
                }
            }

            MovementState state = currentMovement.tick(client, player, client.world);

            if (state.getStatus() == MovementStatus.SUCCESS) {
                advanceToNextNode(player);
                if (!active) {
                    finishNavigation(client, player);
                    player.sendMessage(net.minecraft.text.Text.literal("§aReached destination!"), false);
                    return;
                }
                continue; // Process next node immediately in the same tick
            } else if (state.getStatus() == MovementStatus.UNREACHABLE) {
                recalculateRoute(client.world, player.getBlockPos(), originalGoal);
                return;
            }

            applyMovementState(player, state);
            break;
        }
    }
    
    private void recalculateRoute(ClientWorld world, BlockPos start, BlockPos goal) {
        if (world != null && recalculationAttempts < MAX_RECALCULATION_ATTEMPTS) {
            List<NavigationNode> newRoute = LocalRoutePlanner.findRoute(world, start, goal);
            if (!newRoute.isEmpty()) {
                recalculationAttempts++;
                setPlannedRoute(newRoute);
            } else {
                finishNavigation(MinecraftClient.getInstance(), MinecraftClient.getInstance().player);
            }
        } else {
            finishNavigation(MinecraftClient.getInstance(), MinecraftClient.getInstance().player);
        }
    }

    private void applyMovementState(ClientPlayerEntity player, MovementState state) {
        player.setSneaking(state.isSneak());
        player.setSprinting(state.isSprint());
        


        if (state.isJump() && player.isOnGround()) {
            player.jump();
        }

        Vec3d targetVel = state.getTargetVelocity();
        double targetVelocityX = targetVel.x;
        double targetVelocityY = targetVel.y;
        double targetVelocityZ = targetVel.z;
        
        if (!enableVelocitySmoothing) {
            smoothedVelocity = new Vec3d(targetVelocityX, targetVelocityY, targetVelocityZ);
        } else {
            double adaptiveSmoothingFactor = VELOCITY_SMOOTHING_FACTOR;
            if (!player.isOnGround()) {
                adaptiveSmoothingFactor = 0.12D;
            } else {
                Vec3d targetDir = new Vec3d(targetVelocityX, 0.0D, targetVelocityZ);
                Vec3d prevDir = new Vec3d(smoothedVelocity.x, 0.0D, smoothedVelocity.z);
                double targetMag = targetDir.length();
                double prevMag = prevDir.length();
                if (targetMag > 0.0001D && prevMag > 0.0001D) {
                    double dot = (targetDir.x * prevDir.x + targetDir.z * prevDir.z) / (targetMag * prevMag);
                    if (dot < DIRECTION_CHANGE_THRESHOLD) adaptiveSmoothingFactor = 0.12D;
                }
            }
    
            if (state.isSneak()) adaptiveSmoothingFactor = Math.min(adaptiveSmoothingFactor, 0.15D);
    
            double smoothedVelX = smoothedVelocity.x * (1.0D - adaptiveSmoothingFactor) + targetVelocityX * adaptiveSmoothingFactor;
            double smoothedVelZ = smoothedVelocity.z * (1.0D - adaptiveSmoothingFactor) + targetVelocityZ * adaptiveSmoothingFactor;
            smoothedVelocity = new Vec3d(smoothedVelX, targetVelocityY, smoothedVelZ);
        }

        player.setVelocity(smoothedVelocity);
        
        if (Math.abs(smoothedVelocity.x) > 0.001D || Math.abs(smoothedVelocity.z) > 0.001D) {
            spoofedYaw = (float) Math.toDegrees(Math.atan2(-smoothedVelocity.x, smoothedVelocity.z));
            isSpoofing = true;
        } else {
            isSpoofing = false;
        }
        
        appliedMovement = true;
    }

    private void advanceToNextNode(ClientPlayerEntity player) { 
        previousNode = currentNode;
        if (previousNode == null && player != null) {
            previousNode = new NavigationNode(player.getBlockPos());
        }
        currentNode = remainingNodes.pollFirst(); 
        currentMovement = null;
        ticksAtCurrentNode = 0;
        if (currentNode == null) active = false; 
    }

    private void finishNavigation(MinecraftClient client, ClientPlayerEntity player) {
        active = false; currentNode = null; previousNode = null; currentMovement = null; remainingNodes.clear(); originalGoal = null; recalculationAttempts = 0; stopControlledMovement(player);
        isSpoofing = false;
    }

    private void clearPathState() { remainingNodes.clear(); currentNode = null; previousNode = null; currentMovement = null; smoothedVelocity = Vec3d.ZERO; originalGoal = null; recalculationAttempts = 0; lastRecalculationPos = null; stopControlledMovement(); isSpoofing = false; }
    private void stopControlledMovement() { if (MinecraftClient.getInstance().player != null) stopControlledMovement(MinecraftClient.getInstance().player); }
    private void stopControlledMovement(ClientPlayerEntity player) { if (!appliedMovement) return; player.setVelocity(0.0D, player.getVelocity().y, 0.0D); smoothedVelocity = Vec3d.ZERO; appliedMovement = false; }
    private static NavigationNode copyNode(NavigationNode node) { return new NavigationNode(node.position(), node.movementCost(), node.estimatedCost(), node.parent()); }
    private static Vec3d nodeCenter(BlockPos pos) { return new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D); }
}

