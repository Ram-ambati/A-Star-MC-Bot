package com.bot.client.mixin;

import com.bot.client.BotClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses player WASD / movement input while the bot is navigating a path.
 * Jump (space) is intentionally NOT suppressed so the user can still manually jump.
 * The bot's own movement is applied via direct velocity overrides in MovementController.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(CallbackInfo ci) {
        if (BotClient.getMovementController() != null && BotClient.getMovementController().isActive()) {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client.player == null) return;

            boolean isFreecam = client.getCameraEntity() != client.player;
            
            // To swim properly without bobbing, we must force jump while in water.
            // But if we force jump while on the ground (like at the shore), the bot does a weird
            // sprint-jump out of the water. So we only force jump if NOT on ground!
            boolean needsSwimJump = client.player.isTouchingWater() && !client.player.isOnGround();

            // Pass through the physical jump key ONLY if we are NOT in freecam.
            // If in freecam, the user pressing spacebar is meant to fly the camera up, not jump the player!
            boolean userJump = !isFreecam && this.playerInput.jump();

            // Zero all directional movement from the keyboard so user WASD
            // doesn't stack onto the bot's velocity override.
            this.playerInput = new PlayerInput(
                    false, false, false, false,
                    needsSwimJump || userJump,
                    this.playerInput.sneak(),
                    this.playerInput.sprint()
            );
            
            // Note: we clear movementVector for the player so they don't walk randomly,
            // but if Freecam is active, we shouldn't break the Freecam camera's movement.
            // However, Minecraft's Input object is usually per-player. 
            // If Freecam breaks because of this, Freecam is sharing the Input object.
            this.movementVector = Vec2f.ZERO;
        }
    }
}
