package com.bot.client.movement.movements;

import net.minecraft.util.math.Vec3d;

public class MovementState {
    private Vec3d targetVelocity = Vec3d.ZERO;
    private boolean jump = false;
    private boolean sprint = false;
    private boolean sneak = false;
    private MovementStatus status = MovementStatus.RUNNING;

    public MovementState setTargetVelocity(Vec3d velocity) { this.targetVelocity = velocity; return this; }
    public MovementState setJump(boolean jump) { this.jump = jump; return this; }
    public MovementState setSprint(boolean sprint) { this.sprint = sprint; return this; }
    public MovementState setSneak(boolean sneak) { this.sneak = sneak; return this; }
    public MovementState setStatus(MovementStatus status) { this.status = status; return this; }

    public Vec3d getTargetVelocity() { return targetVelocity; }
    public boolean isJump() { return jump; }
    public boolean isSprint() { return sprint; }
    public boolean isSneak() { return sneak; }
    public MovementStatus getStatus() { return status; }
}
