package com.bot.client.mixin;

import com.bot.client.movement.MovementController;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Unique
    private float originalYaw;

    @Unique
    private float originalPitch;

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPacketsHead(CallbackInfo ci) {
        if (MovementController.isSpoofing) {
            ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
            this.originalYaw = player.getYaw();
            this.originalPitch = player.getPitch();

            player.setYaw(MovementController.spoofedYaw);
            player.setPitch(MovementController.spoofedPitch);
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void onSendMovementPacketsReturn(CallbackInfo ci) {
        if (MovementController.isSpoofing) {
            ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
            player.setYaw(this.originalYaw);
            player.setPitch(this.originalPitch);
        }
    }
}
