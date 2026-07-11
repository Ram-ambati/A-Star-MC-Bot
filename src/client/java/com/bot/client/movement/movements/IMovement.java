package com.bot.client.movement.movements;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

public interface IMovement {
    MovementState tick(MinecraftClient client, ClientPlayerEntity player, ClientWorld world);
}
